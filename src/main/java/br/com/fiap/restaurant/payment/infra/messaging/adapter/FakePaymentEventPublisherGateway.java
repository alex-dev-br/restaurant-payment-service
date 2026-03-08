package br.com.fiap.restaurant.payment.infra.messaging.adapter;

import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentEventPublisherGateway implements PaymentEventPublisherGateway {

    @Override
    public void publishApproved(Payment payment) {
        System.out.println("Evento fake: pagamento aprovado - " + payment.getId());
    }

    @Override
    public void publishPending(Payment payment) {
        System.out.println("Evento fake: pagamento pendente - " + payment.getId());
    }
}
