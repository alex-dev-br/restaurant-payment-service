package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataProcessedMessageRepository;
import br.com.fiap.restaurant.payment.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@Import(ProcessedMessagePersistenceAdapter.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProcessedMessagePersistenceAdapterTest extends AbstractPostgresIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ProcessedMessagePersistenceAdapter adapter;

    @org.springframework.beans.factory.annotation.Autowired
    private SpringDataProcessedMessageRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldRegisterMessageWhenItDoesNotExist() {
        UUID messageId = UUID.randomUUID();

        boolean registered = adapter.registerIfAbsent(
                messageId,
                "ORDER_CREATED",
                "123"
        );

        assertTrue(registered);
    }

    @Test
    void shouldReturnFalseWhenMessageAlreadyExists() {
        UUID messageId = UUID.randomUUID();

        boolean first = adapter.registerIfAbsent(
                messageId,
                "ORDER_CREATED",
                "123"
        );

        boolean second = adapter.registerIfAbsent(
                messageId,
                "ORDER_CREATED",
                "123"
        );

        assertTrue(first);
        assertFalse(second);
    }
}
