package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.exception.PaymentNotFoundException;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;

public class FindPaymentByOrderIdUseCase {

    private final PaymentRepositoryGateway paymentRepositoryGateway;

    public FindPaymentByOrderIdUseCase(PaymentRepositoryGateway paymentRepositoryGateway) {
        this.paymentRepositoryGateway = paymentRepositoryGateway;
    }

    public Payment execute(Long orderId) {
        return paymentRepositoryGateway.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }
}
