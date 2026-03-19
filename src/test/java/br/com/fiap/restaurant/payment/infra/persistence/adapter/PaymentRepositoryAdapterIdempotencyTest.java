package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentEntity;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PaymentRepositoryAdapterIdempotencyTest {

    private SpringDataPaymentRepository repository;
    private PaymentPersistenceMapper mapper;
    private PaymentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataPaymentRepository.class);
        mapper = new PaymentPersistenceMapper();
        adapter = new PaymentRepositoryAdapter(repository, mapper);
    }

    @Test
    void shouldReturnExistingPaymentWhenSaveFailsDueToUniqueConstraint() {
        UUID paymentId = UUID.randomUUID();
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Payment payment = new Payment(
                paymentId,
                orderId,
                clientId,
                new BigDecimal("99.90"),
                PaymentStatus.PENDING,
                now,
                now
        );

        PaymentEntity existingEntity = new PaymentEntity(
                paymentId,
                orderId,
                clientId,
                new BigDecimal("99.90"),
                PaymentStatus.PENDING.name(),
                now,
                now
        );

        when(repository.save(any(PaymentEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violation"));

        when(repository.findByOrderId(orderId))
                .thenReturn(Optional.of(existingEntity));

        Payment result = adapter.save(payment);

        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(paymentId, result.getId());

        verify(repository, times(1)).save(any(PaymentEntity.class));
        verify(repository, times(1)).findByOrderId(orderId);
    }
}
