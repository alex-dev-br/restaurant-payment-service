package br.com.fiap.restaurant.payment.infra.scheduler;

import br.com.fiap.restaurant.payment.core.usecase.RetryPendingPaymentsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentRetryScheduler.class);

    private final RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;

    public PaymentRetryScheduler(RetryPendingPaymentsUseCase retryPendingPaymentsUseCase) {
        this.retryPendingPaymentsUseCase = retryPendingPaymentsUseCase;
    }

    @Scheduled(fixedDelayString = "${app.payment.retry.scheduler.fixed-delay-ms:30000}")
    public void retryPendingPayments() {
        log.info("Iniciando rotina agendada de retry de pagamentos pendentes");
        retryPendingPaymentsUseCase.execute();
        log.info("Finalizando rotina agendada de retry de pagamentos pendentes");
    }
}
