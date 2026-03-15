package br.com.fiap.restaurant.payment.infra.client.processor.dto;

public record ProcpagPaymentSubmissionResponse(
        String status
) {
    public boolean isAccepted() {
        return status != null && status.equalsIgnoreCase("accepted");
    }
}
