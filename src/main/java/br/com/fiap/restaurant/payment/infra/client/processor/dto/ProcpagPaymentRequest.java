package br.com.fiap.restaurant.payment.infra.client.processor.dto;

public record ProcpagPaymentRequest(

        int valor,
        String pagamento_id,
        String cliente_id
) {
}
