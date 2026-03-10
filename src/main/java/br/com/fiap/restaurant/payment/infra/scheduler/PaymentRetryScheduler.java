package br.com.fiap.restaurant.payment.infra.scheduler;

import br.com.fiap.restaurant.payment.core.usecase.RetryPendingPaymentsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.payment.retry.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentRetryScheduler {

    private final RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;

    public PaymentRetryScheduler(RetryPendingPaymentsUseCase retryPendingPaymentsUseCase) {
        this.retryPendingPaymentsUseCase = retryPendingPaymentsUseCase;
    }

    @Scheduled(fixedDelayString = "${app.payment.retry.scheduler.fixed-delay-ms:30000}")
    public void retryPendingPayments() {
        retryPendingPaymentsUseCase.execute();
    }
}
