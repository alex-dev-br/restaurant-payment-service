package br.com.fiap.restaurant.payment.infra.messaging.inbound.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedMessage(
        Long orderId,
        UUID clientId,
        BigDecimal amount
) {
}
