package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventTransportGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentOutboxRepositoryGateway;

import java.time.OffsetDateTime;
import java.util.List;

public class PublishPendingPaymentOutboxUseCase {

    private final PaymentOutboxRepositoryGateway paymentOutboxRepositoryGateway;
    private final PaymentEventTransportGateway paymentEventTransportGateway;
    private final int batchSize;

    public PublishPendingPaymentOutboxUseCase(
            PaymentOutboxRepositoryGateway paymentOutboxRepositoryGateway,
            PaymentEventTransportGateway paymentEventTransportGateway,
            int batchSize
    ) {
        this.paymentOutboxRepositoryGateway = paymentOutboxRepositoryGateway;
        this.paymentEventTransportGateway = paymentEventTransportGateway;
        this.batchSize = batchSize;
    }

    public void execute() {
        List<PaymentOutboxEvent> pendingEvents =
                paymentOutboxRepositoryGateway.findPending(batchSize);

        for (PaymentOutboxEvent event : pendingEvents) {
            paymentEventTransportGateway.publish(event);
            paymentOutboxRepositoryGateway.markPublished(event.getId(), OffsetDateTime.now());
        }
    }
}
