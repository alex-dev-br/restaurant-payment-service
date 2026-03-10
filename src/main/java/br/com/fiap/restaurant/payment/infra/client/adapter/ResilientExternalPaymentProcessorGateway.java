package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.infra.client.processor.ExternalPaymentProcessorClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@Primary
public class ResilientExternalPaymentProcessorGateway implements ExternalPaymentProcessorGateway {

    private final ExternalPaymentProcessorClient client;

    public ResilientExternalPaymentProcessorGateway(ExternalPaymentProcessorClient client) {
        this.client = client;
    }

    @Override
    public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {
        try {
            return processAsync(paymentId, clientId, amount)
                    .toCompletableFuture()
                    .join();
        } catch (Exception exception) {
            return false;
        }
    }

    @Retry(name = "externalPaymentProcessor", fallbackMethod = "fallbackAsync")
    @CircuitBreaker(name = "externalPaymentProcessor", fallbackMethod = "fallbackAsync")
    @TimeLimiter(name = "externalPaymentProcessor", fallbackMethod = "fallbackAsync")
    public CompletionStage<Boolean> processAsync(UUID paymentId, UUID clientId, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() ->
                client.process(paymentId, clientId, amount)
        );
    }

    private CompletionStage<Boolean> fallbackAsync(
            UUID paymentId,
            UUID clientId,
            BigDecimal amount,
            Throwable throwable
    ) {
        return CompletableFuture.completedFuture(false);
    }
}
