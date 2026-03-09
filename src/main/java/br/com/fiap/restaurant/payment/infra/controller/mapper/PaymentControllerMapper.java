package br.com.fiap.restaurant.payment.infra.controller.mapper;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.infra.controller.response.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentControllerMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getClientId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}

