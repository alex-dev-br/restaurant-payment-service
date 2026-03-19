package br.com.fiap.restaurant.payment.infra.observability.adapter;

import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

public class NoOpPaymentObservabilityAdapter implements PaymentObservabilityGateway {

    @Override
    public void logProcessingStarted(Long orderId, UUID clientId, BigDecimal amount) {}

    @Override
    public void logIdempotentReuse(Payment payment) {}

    @Override
    public void logConcurrentClaimReuse(Payment payment) {}

    @Override
    public void logExternalProcessingStarted(Payment payment) {}

    @Override
    public void logApproved(Payment payment) {}

    @Override
    public void logPending(Payment payment) {}

    @Override
    public void logExternalError(Payment payment, Exception exception) {}

    @Override
    public <T> T measureProcessing(Supplier<T> supplier) {
        return supplier.get();
    }
}
