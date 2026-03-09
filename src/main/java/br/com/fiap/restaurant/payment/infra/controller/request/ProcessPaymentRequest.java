package br.com.fiap.restaurant.payment.infra.controller.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentRequest(

        @NotNull(message = "orderId é obrigatório")
        UUID orderId,

        @NotNull(message = "clientId é obrigatório")
        UUID clientId,

        @NotNull(message = "amount é obrigatório")
        @DecimalMin(value = "0.01", message = "amount deve ser maior que zero")
        BigDecimal amount
) {
}
