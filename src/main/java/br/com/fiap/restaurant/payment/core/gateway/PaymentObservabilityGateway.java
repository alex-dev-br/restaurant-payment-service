package br.com.fiap.restaurant.payment.core.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

public interface PaymentObservabilityGateway {

    void logProcessingStarted(UUID orderId, UUID clientId, BigDecimal amount);

    void logIdempotentReuse(Payment payment);

    void logConcurrentClaimReuse(Payment payment);

    void logExternalProcessingStarted(Payment payment);

    void logApproved(Payment payment);

    void logPending(Payment payment);

    void logExternalError(Payment payment, Exception exception);

    <T> T measureProcessing(Supplier<T> supplier);
}
