package br.com.fiap.restaurant.payment.infra.scheduler;

import br.com.fiap.restaurant.payment.core.usecase.PublishPendingPaymentOutboxUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PaymentOutboxPublisherSchedulerTest {

    @Test
    void shouldExecuteOutboxPublisherUseCaseWhenSchedulerRuns() {
        PublishPendingPaymentOutboxUseCase useCase = mock(PublishPendingPaymentOutboxUseCase.class);

        PaymentOutboxPublisherScheduler scheduler = new PaymentOutboxPublisherScheduler(useCase);

        scheduler.publishPendingEvents();

        verify(useCase).execute();
    }
}