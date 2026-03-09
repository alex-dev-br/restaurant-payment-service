package br.com.fiap.restaurant.payment.infra.controller.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        UUID clientId,
        BigDecimal amount,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
