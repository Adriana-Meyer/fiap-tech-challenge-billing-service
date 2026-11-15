package com.fiap.workshop.billing.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Workshop Billing API",
                version = "1.0.0",
                description = "Microsserviço de Cobrança (orçamento e pagamento) — FIAP Tech Challenge"
        ),
        servers = @Server(url = "/", description = "Default server"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Informe o token JWT obtido no OS Service (POST /api/v1/auth/login)"
)
public class OpenApiConfig {
}
