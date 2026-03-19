package br.com.fiap.restaurant.payment.core.domain.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long orderId) {
        super("Pagamento não encontrado para o orderId: " + orderId);
    }
}
