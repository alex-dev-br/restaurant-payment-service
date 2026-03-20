package br.com.fiap.restaurant.payment.core.usecase.command;

import java.math.BigDecimal;
import java.util.UUID;

public record HandleOrderCreatedEventCommand(
        UUID messageId,
        Long orderId,
        UUID clientId,
        BigDecimal amount
) {
}
