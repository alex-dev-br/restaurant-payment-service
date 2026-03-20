package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ProcessedMessageRepositoryGateway;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataProcessedMessageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ProcessedMessagePersistenceAdapter implements ProcessedMessageRepositoryGateway {

    private final SpringDataProcessedMessageRepository repository;

    public ProcessedMessagePersistenceAdapter(SpringDataProcessedMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean registerIfAbsent(UUID messageId, String messageType, String aggregateKey) {
        int affectedRows = repository.insertIfAbsent(
                messageId,
                messageType,
                aggregateKey,
                OffsetDateTime.now()
        );

        return affectedRows > 0;
    }
}
