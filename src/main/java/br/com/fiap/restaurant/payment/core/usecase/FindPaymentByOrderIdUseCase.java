package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;

import java.util.UUID;

public class FindPaymentByOrderIdUseCase {

    private final PaymentRepositoryGateway paymentRepositoryGateway;

    public FindPaymentByOrderIdUseCase(PaymentRepositoryGateway paymentRepositoryGateway) {
        this.paymentRepositoryGateway = paymentRepositoryGateway;
    }

    public Payment execute(UUID orderId) {
        return paymentRepositoryGateway.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pagamento não encontrado para o orderId: " + orderId
                ));
    }
}
