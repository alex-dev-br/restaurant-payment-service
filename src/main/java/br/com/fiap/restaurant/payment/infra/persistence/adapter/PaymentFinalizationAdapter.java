package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.OutboxStatus;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentEventType;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;
import br.com.fiap.restaurant.payment.core.gateway.PaymentFinalizationGateway;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentEntity;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentOutboxRepository;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class PaymentFinalizationAdapter implements PaymentFinalizationGateway {

    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataPaymentOutboxRepository outboxRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;
    private final PaymentOutboxPersistenceMapper paymentOutboxPersistenceMapper;
    private final RabbitProperties rabbitProperties;
    private final JsonMapper objectMapper;

    public PaymentFinalizationAdapter(
            SpringDataPaymentRepository paymentRepository,
            SpringDataPaymentOutboxRepository outboxRepository,
            PaymentPersistenceMapper paymentPersistenceMapper,
            PaymentOutboxPersistenceMapper paymentOutboxPersistenceMapper,
            RabbitProperties rabbitProperties,
            JsonMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.paymentPersistenceMapper = paymentPersistenceMapper;
        this.paymentOutboxPersistenceMapper = paymentOutboxPersistenceMapper;
        this.rabbitProperties = rabbitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Payment saveApprovedAndEnqueue(Payment payment) {
        return saveAndEnqueue(
                payment,
                PaymentEventType.PAYMENT_APPROVED,
                rabbitProperties.getRoutingKey().getPaymentApproved()
        );
    }

    @Override
    @Transactional
    public Payment savePendingAndEnqueue(Payment payment) {
        return saveAndEnqueue(
                payment,
                PaymentEventType.PAYMENT_PENDING,
                rabbitProperties.getRoutingKey().getPaymentPending()
        );
    }

    @Override
    @Transactional
    public Payment saveFailedAndEnqueue(Payment payment) {
        return saveAndEnqueue(
                payment,
                PaymentEventType.PAYMENT_FAILED,
                rabbitProperties.getRoutingKey().getPaymentFailed()
        );
    }

    private Payment saveAndEnqueue(Payment payment, PaymentEventType eventType, String routingKey) {
        PaymentEntity savedPaymentEntity = paymentRepository.save(paymentPersistenceMapper.toEntity(payment));
        Payment savedPayment = paymentPersistenceMapper.toDomain(savedPaymentEntity);

        PaymentEventMessage message = new PaymentEventMessage(
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getClientId(),
                savedPayment.getAmount(),
                savedPayment.getStatus().name(),
                OffsetDateTime.now()
        );

        PaymentOutboxEvent outboxEvent = new PaymentOutboxEvent(
                UUID.randomUUID(),
                savedPayment.getId(),
                eventType,
                rabbitProperties.getExchange().getPayment(),
                routingKey,
                toJson(message),
                OutboxStatus.PENDING,
                OffsetDateTime.now(),
                null
        );

        outboxRepository.save(paymentOutboxPersistenceMapper.toEntity(outboxEvent));

        return savedPayment;
    }

    private String toJson(PaymentEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Erro ao serializar evento de pagamento para o outbox", exception);
        }
    }
}