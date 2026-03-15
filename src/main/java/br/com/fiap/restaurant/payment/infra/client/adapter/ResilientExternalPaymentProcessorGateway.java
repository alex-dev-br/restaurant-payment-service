package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.infra.client.processor.ExternalPaymentProcessorClient;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

@Component
@Primary
public class ResilientExternalPaymentProcessorGateway implements ExternalPaymentProcessorGateway {

    private final ExternalPaymentProcessorClient client;
    private final ExecutorService executorService;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Bulkhead bulkhead;

    public ResilientExternalPaymentProcessorGateway(
            ExternalPaymentProcessorClient client,
            ExecutorService externalPaymentExecutorService,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            BulkheadRegistry bulkheadRegistry
    ) {
        this.client = client;
        this.executorService = externalPaymentExecutorService;
        this.retry = retryRegistry.retry("externalPaymentProcessor");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("externalPaymentProcessor");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("externalPaymentProcessor");
        this.bulkhead = bulkheadRegistry.bulkhead("externalPaymentProcessor");
    }

    @Override
    public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {
        try {
            Callable<Boolean> callable = () -> client.process(paymentId, clientId, amount);

            Callable<Boolean> bulkheadCallable =
                    Bulkhead.decorateCallable(bulkhead, callable);

            Callable<Boolean> circuitBreakerCallable =
                    CircuitBreaker.decorateCallable(circuitBreaker, bulkheadCallable);

            Callable<Boolean> retryCallable =
                    Retry.decorateCallable(retry, circuitBreakerCallable);

            Boolean result = timeLimiter.executeFutureSupplier(
                    () -> executorService.submit(retryCallable)
            );

            return Boolean.TRUE.equals(result);

        } catch (Exception exception) {
            return false;
        }
    }
}
