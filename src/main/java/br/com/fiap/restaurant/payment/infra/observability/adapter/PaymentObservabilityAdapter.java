package br.com.fiap.restaurant.payment.infra.observability.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class PaymentObservabilityAdapter implements PaymentObservabilityGateway {

    private static final Logger log = LoggerFactory.getLogger(PaymentObservabilityAdapter.class);

    private final Counter approvedPaymentsCounter;
    private final Counter pendingPaymentsCounter;
    private final Counter failedPaymentsCounter;
    private final Counter idempotentReuseCounter;
    private final Timer paymentProcessingTimer;

    public PaymentObservabilityAdapter(MeterRegistry meterRegistry) {
        this.approvedPaymentsCounter = Counter.builder("payment.approved.total")
                .description("Total de pagamentos aprovados")
                .register(meterRegistry);

        this.pendingPaymentsCounter = Counter.builder("payment.pending.total")
                .description("Total de pagamentos pendentes")
                .register(meterRegistry);

        this.failedPaymentsCounter = Counter.builder("payment.failed.total")
                .description("Total de pagamentos falhados")
                .register(meterRegistry);

        this.idempotentReuseCounter = Counter.builder("payment.idempotent.reused.total")
                .description("Total de pagamentos reaproveitados por idempotência")
                .register(meterRegistry);

        this.paymentProcessingTimer = Timer.builder("payment.processing.duration")
                .description("Duração do processamento de pagamentos")
                .register(meterRegistry);
    }

    @Override
    public void logProcessingStarted(Long orderId, UUID clientId, BigDecimal amount) {
        log.info("Iniciando processamento do pagamento. orderId={}, clientId={}, amount={}",
                orderId, clientId, amount);
    }

    @Override
    public void logIdempotentReuse(Payment payment) {
        idempotentReuseCounter.increment();
        log.info("Pagamento já existente encontrado. Reutilizando por idempotência. orderId={}, paymentId={}, status={}",
                payment.getOrderId(), payment.getId(), payment.getStatus());
    }

    @Override
    public void logConcurrentClaimReuse(Payment payment) {
        idempotentReuseCounter.increment();
        log.warn("Pagamento já havia sido persistido por outro fluxo concorrente. orderId={}, paymentId={}, status={}",
                payment.getOrderId(), payment.getId(), payment.getStatus());
    }

    @Override
    public void logExternalProcessingStarted(Payment payment) {
        log.info("Chamando processador externo de pagamentos. orderId={}, paymentId={}, clientId={}, amount={}",
                payment.getOrderId(), payment.getId(), payment.getClientId(), payment.getAmount());
    }

    @Override
    public void logApproved(Payment payment) {
        approvedPaymentsCounter.increment();
        log.info("Pagamento aprovado com sucesso. orderId={}, paymentId={}, status={}",
                payment.getOrderId(), payment.getId(), payment.getStatus());
    }

    @Override
    public void logPending(Payment payment) {
        pendingPaymentsCounter.increment();
        log.warn("Pagamento marcado como pendente. orderId={}, paymentId={}, status={}",
                payment.getOrderId(), payment.getId(), payment.getStatus());
    }

    @Override
    public void logFailed(Payment payment) {
        failedPaymentsCounter.increment();
        log.error("Pagamento marcado como falhado. orderId={}, paymentId={}, status={}, retryCount={}",
                payment.getOrderId(), payment.getId(), payment.getStatus(), payment.getRetryCount());
    }

    @Override
    public void logExternalError(Payment payment, Exception exception) {
        log.error("Erro ao processar pagamento externamente. orderId={}, paymentId={}, motivo={}",
                payment.getOrderId(), payment.getId(), exception.getMessage(), exception);
    }

    @Override
    public <T> T measureProcessing(Supplier<T> supplier) {
        return paymentProcessingTimer.record(supplier);
    }
}
