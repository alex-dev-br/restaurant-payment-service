package br.com.fiap.restaurant.payment.infra.persistence.repository;

import br.com.fiap.restaurant.payment.infra.persistence.entity.ProcessedMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SpringDataProcessedMessageRepository extends JpaRepository<ProcessedMessageEntity, UUID> {

    @Modifying
    @Query(value = """
            insert into processed_messages (message_id, message_type, aggregate_key, processed_at)
            values (:messageId, :messageType, :aggregateKey, :processedAt)
            on conflict (message_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("messageId") UUID messageId,
            @Param("messageType") String messageType,
            @Param("aggregateKey") String aggregateKey,
            @Param("processedAt") OffsetDateTime processedAt
    );
}