package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.OutboxStatus;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;
import br.com.fiap.restaurant.payment.core.gateway.PaymentOutboxRepositoryGateway;
import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentOutboxEntity;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentOutboxRepositoryAdapter implements PaymentOutboxRepositoryGateway {

    private final SpringDataPaymentOutboxRepository repository;
    private final PaymentOutboxPersistenceMapper mapper;

    public PaymentOutboxRepositoryAdapter(
            SpringDataPaymentOutboxRepository repository,
            PaymentOutboxPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PaymentOutboxEvent save(PaymentOutboxEvent event) {
        PaymentOutboxEntity saved = repository.save(mapper.toEntity(event));
        return mapper.toDomain(saved);
    }

    @Override
    public List<PaymentOutboxEvent> findPending(int limit) {
        return repository.findByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING.name(),
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId, OffsetDateTime publishedAt) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.setStatus(OutboxStatus.PUBLISHED.name());
            entity.setPublishedAt(publishedAt);
            repository.save(entity);
        });
    }
}
