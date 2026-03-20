package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
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

    @Override
    public void publishFailed(Payment payment) {
        System.out.println("Evento fake: pagamento falhado - " + payment.getId());
    }
}
