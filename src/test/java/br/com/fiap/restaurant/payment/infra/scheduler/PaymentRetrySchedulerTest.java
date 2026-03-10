package br.com.fiap.restaurant.payment.infra.scheduler;

import br.com.fiap.restaurant.payment.core.usecase.RetryPendingPaymentsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PaymentRetrySchedulerTest {

    private RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;
    private PaymentRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        retryPendingPaymentsUseCase = mock(RetryPendingPaymentsUseCase.class);
        scheduler = new PaymentRetryScheduler(retryPendingPaymentsUseCase);
    }

    @Test
    void shouldExecuteRetryUseCaseWhenSchedulerRuns() {
        scheduler.retryPendingPayments();

        verify(retryPendingPaymentsUseCase).execute();
    }
}