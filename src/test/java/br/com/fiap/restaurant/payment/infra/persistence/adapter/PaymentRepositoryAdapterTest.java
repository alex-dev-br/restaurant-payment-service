package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryAdapterTest {

    @Autowired
    private SpringDataPaymentRepository springDataPaymentRepository;

    @Test
    void shouldSaveAndFindPaymentByOrderId() {
        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(springDataPaymentRepository);

        UUID orderId = UUID.randomUUID();

        Payment payment = new Payment(
                UUID.randomUUID(),
                orderId,
                new BigDecimal("99.90"),
                PaymentStatus.PENDING,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        Payment saved = adapter.save(payment);
        Optional<Payment> found = adapter.findByOrderId(orderId);

        assertNotNull(saved);
        assertEquals(orderId, saved.getOrderId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(PaymentStatus.PENDING, found.get().getStatus());
    }
}