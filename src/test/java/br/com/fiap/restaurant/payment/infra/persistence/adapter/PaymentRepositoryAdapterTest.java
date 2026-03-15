package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PaymentRepositoryAdapterTest {

    @Autowired
    private SpringDataPaymentRepository springDataPaymentRepository;

    @Test
    void shouldSaveAndFindPaymentByOrderId() {

        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(
                        springDataPaymentRepository,
                        new PaymentPersistenceMapper()
                );

        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Payment payment = new Payment(
                UUID.randomUUID(),
                orderId,
                clientId,
                new BigDecimal("99.90"),
                PaymentStatus.PENDING,
                now,
                now
        );

        Payment saved = adapter.save(payment);
        Optional<Payment> found = adapter.findByOrderId(orderId);

        assertNotNull(saved);
        assertEquals(orderId, saved.getOrderId());
        assertEquals(clientId, saved.getClientId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(PaymentStatus.PENDING, found.get().getStatus());
        assertEquals(clientId, found.get().getClientId());
    }
}
