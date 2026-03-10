package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Primary
public class ResilientExternalPaymentProcessorGateway implements ExternalPaymentProcessorGateway {

    private final ResilientExternalPaymentProcessorExecutor executor;

    public ResilientExternalPaymentProcessorGateway(ResilientExternalPaymentProcessorExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {
        try {
            return executor.processAsync(paymentId, clientId, amount)
                    .toCompletableFuture()
                    .join();
        } catch (Exception exception) {
            return false;
        }
    }
}
