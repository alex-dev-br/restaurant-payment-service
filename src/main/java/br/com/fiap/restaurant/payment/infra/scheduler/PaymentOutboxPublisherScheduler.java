package br.com.fiap.restaurant.payment.infra.scheduler;

import br.com.fiap.restaurant.payment.core.usecase.PublishPendingPaymentOutboxUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.payment.outbox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentOutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisherScheduler.class);

    private final PublishPendingPaymentOutboxUseCase publishPendingPaymentOutboxUseCase;

    public PaymentOutboxPublisherScheduler(
            PublishPendingPaymentOutboxUseCase publishPendingPaymentOutboxUseCase
    ) {
        this.publishPendingPaymentOutboxUseCase = publishPendingPaymentOutboxUseCase;
    }

    @Scheduled(fixedDelayString = "${app.payment.outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        log.info("Iniciando publicação de eventos pendentes do outbox");
        publishPendingPaymentOutboxUseCase.execute();
        log.info("Finalizando publicação de eventos pendentes do outbox");
    }
}
