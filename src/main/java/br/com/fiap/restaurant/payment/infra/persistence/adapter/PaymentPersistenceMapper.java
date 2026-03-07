package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceMapper {

    public PaymentEntity toEntity(Payment payment) {
        return new PaymentEntity(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public Payment toDomain(PaymentEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getOrderId(),
                entity.getAmount(),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
