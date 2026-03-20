package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentEntity;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryGateway {

    private final SpringDataPaymentRepository repository;
    private final PaymentPersistenceMapper mapper;

    public PaymentRepositoryAdapter(
            SpringDataPaymentRepository repository,
            PaymentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = mapper.toEntity(payment);

        try {
            PaymentEntity savedEntity = repository.save(entity);
            return mapper.toDomain(savedEntity);

        } catch (DataIntegrityViolationException exception) {
            return repository.findByOrderId(payment.getOrderId())
                    .map(mapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException(
                            "Falha ao salvar pagamento e nenhum pagamento existente foi encontrado para o orderId: "
                                    + payment.getOrderId(),
                            exception
                    ));
        }
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return repository.findByStatus(status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findRetryablePendingPayments(OffsetDateTime referenceTime) {
        return repository.findRetryablePendingPayments(referenceTime)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
