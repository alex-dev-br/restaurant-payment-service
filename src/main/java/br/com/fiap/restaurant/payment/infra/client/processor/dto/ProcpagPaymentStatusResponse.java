package br.com.fiap.restaurant.payment.infra.client.processor.dto;

public record ProcpagPaymentStatusResponse(
        String pagamento_id,
        String status
) {
    public boolean isPaid() {
        return status != null && status.equalsIgnoreCase("pago");
    }
}

