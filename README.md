# fiap-tech-challenge-billing-service

Microsserviço de **Cobrança** (orçamento e pagamento) do Tech Challenge — FIAP Pós-Tech, Arquitetura de Software, turma 15SOAT, **Fase 4** (microsserviços + Saga).

Faz parte do sistema de gestão de oficina mecânica, que na Fase 4 foi dividido em quatro serviços independentes (OS, Billing, Execution e Inventory). Cada serviço tem repositório, infraestrutura e banco próprios.

| Repositório | Papel |
|---|---|
| **fiap-tech-challenge-billing-service** (este) | Código do serviço de cobrança |
| `fiap-tech-challenge-billing-database-infrastructure` | RDS PostgreSQL do serviço (Terraform) |
| `fiap-tech-challenge-billing-infrastructure` | Namespace `billing` e filas SQS do serviço (Terraform) |
| `fiap-tech-challenge-os-service` | Serviço de OS (orquestrador da saga) |
| `fiap-tech-challenge-os-infrastructure` | VPC, EKS compartilhado, New Relic e tópico SNS |

> **Estado atual:** o serviço executa os comandos da saga que chegam pela fila SQS `billing-commands` e responde pelo tópico SNS `saga-events` (seção "Mensageria").

## Responsabilidade

Dono dos dados de `Budget` (orçamento da OS, com um snapshot dos itens) e `Payment` (tentativa de pagamento da OS). Nenhum outro serviço acessa o banco deste.

Na saga, o Billing executa os passos:

| Comando (SQS `billing-commands`) | Caso de uso | Efeito |
|---|---|---|
| `GenerateBudget` | `GenerateBudgetUseCase` | Calcula o total e grava o orçamento (`GENERATED`). Repetir o comando recalcula o mesmo orçamento. |
| `ProcessPayment` | `ProcessPaymentUseCase` | Cobra o total do orçamento no gateway. Aprovado: `Payment=APPROVED` e `Budget=PAID`. Recusado: `Payment=DECLINED`. |
| `CancelBudget` (compensação) | `CancelBudgetUseCase` | `Budget=CANCELLED`. |
| `RefundPayment` (compensação) | `RefundPaymentUseCase` | Estorna no gateway: `Payment=REFUNDED` e `Budget=CANCELLED`. |

Regras de consistência:

- **Um orçamento e um pagamento por OS** (`UNIQUE service_order_id`). Um novo diagnóstico da mesma OS substitui o orçamento, desde que ele não esteja pago.
- **Idempotência:** `ProcessPayment` devolve o pagamento existente (aprovado ou recusado) em vez de cobrar de novo, e `RefundPayment` repetido não estorna duas vezes. Isso torna seguro o reenvio de mensagens SQS.
- **Recusa não é erro:** o gateway recusar uma cobrança gera um `Payment` com status `DECLINED`, que a saga usa para disparar as compensações.
- Orçamento pago não pode ser cancelado nem regenerado; o caminho é o estorno. Esses casos viram `409 Conflict`.

## Mensageria

Comandos chegam pela fila `billing-commands`; as respostas saem para o tópico `saga-events`, que alimenta a fila do OS. Todas as mensagens usam o mesmo envelope JSON:

```json
{ "messageId": "…", "sagaId": "…", "serviceOrderId": "…", "type": "GenerateBudget", "payload": { … } }
```

| Comando recebido (`type`) | `payload` | Resposta publicada |
|---|---|---|
| `GenerateBudget` | `items: [{ description, unitPrice, quantity }]` | `BudgetGenerated` `{ totalAmount }` |
| `ProcessPayment` | `paymentMethod` | `PaymentApproved`, ou `PaymentDeclined` `{ reason }` |
| `CancelBudget` | (vazio) | `BudgetCancelled` |
| `RefundPayment` | (vazio) | `PaymentRefunded` |

- **Recusa não é falha:** `PaymentDeclined` é uma resposta normal; é ela que faz o OS disparar as compensações.
- **Reentrega segura:** os casos de uso são idempotentes, e o `messageId` da resposta é derivado do `messageId` do comando. Se o comando chega duas vezes, o OS recebe duas respostas com o mesmo id e descarta a repetida.
- **Falha vai para a DLQ:** se o comando não pode ser tratado (JSON inválido, tipo desconhecido, orçamento inexistente), a mensagem não é confirmada; o SQS a reentrega e, ao passar do máximo de tentativas da fila, a move para `billing-commands-dlq`.
- **Filas são do Terraform:** o serviço não cria filas. Se `billing-commands` não existir, ele falha na subida, o que denuncia um `billing-infrastructure` não aplicado.
- Variáveis: `COMMANDS_QUEUE` (padrão `billing-commands`), `EVENTS_TOPIC_ARN` (ARN do tópico; o `deploy-aws.yml` monta a partir da conta e da região), `MESSAGING_ENABLED` (`false` desliga o listener, usado no `docker compose` isolado e em testes que não usam filas).
- Sem outbox: a resposta é publicada depois de gravar. Se a publicação falha, o comando volta para a fila e é refeito, e a idempotência garante que nada é cobrado duas vezes.

## Arquitetura

Clean Architecture em quatro camadas, com a regra de dependência `domain ← application ← infrastructure / interfaces` **verificada no build** por ArchUnit (`ArchitectureTest`).

```
src/main/java/com/fiap/workshop/billing/
├── domain/            Budget, BudgetItem, Payment, Money, BudgetCalculationService,
│                      PaymentGateway (port), repositórios (interfaces). Sem Spring/JPA.
├── application/       Use cases (um por operação), ports de entrada, DTOs.
├── infrastructure/    Adaptadores: JPA/PostgreSQL, gateway de pagamento mock, segurança JWT, OpenAPI.
└── interfaces/rest/   BudgetController, PaymentController (finos) e GlobalExceptionHandler.
```

Decisões:

- **Banco:** PostgreSQL, migrations com Flyway (`V1` orçamentos e itens, `V2` pagamentos).
- **Gateway de pagamento atrás de uma port** (`PaymentGateway`): hoje o adaptador é `MockPaymentGateway`; trocar pelo Mercado Pago real não toca em domínio nem em casos de uso.
- **Mock determinístico**, para demonstrar a compensação da saga sob demanda: a cobrança é **recusada** quando o método de pagamento é `mock-decline` ou quando o valor passa de `app.payment.mock.decline-above` (variável `PAYMENT_MOCK_DECLINE_ABOVE`, padrão `50000.00`).
- **Snapshot dos itens:** o OS envia descrição, preço e quantidade; o Billing nunca consulta o catálogo nem o estoque.
- **Autenticação:** o token JWT é emitido pelo OS Service; aqui ele só é **validado** (HMAC com o `JWT_SECRET` compartilhado, o mesmo do OS). O token precisa das claims `sub` e `role`; sem `role` a requisição é tratada como não autenticada (401). Não há banco de usuários neste serviço.
- **Money** é duplicado em cada serviço de propósito (sem biblioteca compartilhada entre serviços).
- **Observabilidade:** agente New Relic anexado no `Dockerfile` (`NEW_RELIC_APP_NAME=workshop-billing-service`), logs em JSON com `trace.id`/`span.id`.

## API

Swagger UI: `http://<host>/swagger-ui.html` · OpenAPI: `/v3/api-docs`. As rotas de `/api/v1` exigem `Authorization: Bearer <jwt>`.

A API HTTP é **somente de consulta**: orçamentos e pagamentos são criados e alterados por comandos da saga, nunca por HTTP, para que ninguém contorne o orquestrador.

| Método | Rota | Perfis |
|---|---|---|
| `GET` | `/api/v1/budgets/{serviceOrderId}` | CONSULTANT, ADMIN |
| `GET` | `/api/v1/payments/{serviceOrderId}` | CONSULTANT, ADMIN |
| `GET` | `/actuator/health` | público |

## Como rodar localmente

```bash
docker compose up --build        # PostgreSQL + serviço em http://localhost:8082 (sem filas)
```

Para chamar a API é preciso um JWT assinado com o mesmo `JWT_SECRET`, contendo a claim `role`. Enquanto o OS Service não emitir essa claim, gere um token de teste (o segredo de desenvolvimento está em `application.yml`). Sem as filas (`MESSAGING_ENABLED=false` neste compose) não há como criar um orçamento; o ambiente completo com LocalStack fica no repositório do OS.

## Testes e qualidade

```bash
mvn verify        # testes unitários + integração (Testcontainers/PostgreSQL, requer Docker) + gate JaCoCo >= 80%
```

- Cobertura atual: ~97% de instruções (gate do build: 80%). Relatório em `target/site/jacoco/`.
- `ArchitectureTest` falha o build se uma camada violar a regra de dependência.
- `SagaMessagingIntegrationTest` roda o serviço contra SQS e SNS reais (LocalStack) e PostgreSQL: fluxo completo, recusa, estorno, comando repetido e mensagem que vai para a DLQ.
- `BillingApiIntegrationTest` sobe o serviço inteiro contra um PostgreSQL real (migrations, segurança, casos de uso e REST juntos) e cobre o fluxo completo: gerar orçamento, pagar, recusar, estornar e regenerar.
- Análise estática e quality gate no **SonarQube Cloud** (step do CI, ativo quando o secret `SONAR_TOKEN` existe).

## CI/CD

Workflows em [`.github/workflows/`](.github/workflows/):

- `ci-cd.yml` — job `build-and-test` (`mvn verify` + Sonar) em todo push/PR; em `develop`/`main` também publica a imagem `adrianameyer/workshop-billing-service` no Docker Hub.
- `deploy-aws.yml` — deploy/destroy no EKS compartilhado (namespace `billing`), disparado manualmente (`workflow_dispatch`). Também roda após um CI verde na `main` **se** a variável de repositório `AUTO_DEPLOY` for `true`.

Secrets do repositório: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` (renovados a cada sessão do Lab), `JWT_SECRET` (igual ao do OS), `NEW_RELIC_LICENSE_KEY`, `SONAR_TOKEN`, e `RDS_DATASOURCE_URL` / `RDS_USERNAME` / `RDS_PASSWORD` (enviados automaticamente pelo `billing-database-infrastructure`).

Ordem de subida no Lab: `os-infrastructure` → `billing-infrastructure` → `billing-database-infrastructure` → deploy deste serviço. Na destruição, a ordem inversa (rodar `destroy` aqui primeiro libera o NLB).
