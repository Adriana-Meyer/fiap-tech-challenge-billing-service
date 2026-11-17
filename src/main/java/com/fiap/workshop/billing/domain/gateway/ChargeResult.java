package com.fiap.workshop.billing.domain.gateway;

/** Outcome of a charge. A decline is a business result, not an error. */
public record ChargeResult(boolean approved, String externalReference, String declineReason) {

    public static ChargeResult approved(String externalReference) {
        return new ChargeResult(true, externalReference, null);
    }

    public static ChargeResult declined(String declineReason) {
        return new ChargeResult(false, null, declineReason);
    }
}
