package br.com.fiap.restaurant.payment.infra.messaging.inbound.dto;

import java.math.BigDecimal;

public record OrderItemDTO(BigDecimal quantity, BigDecimal unitPrice) {

    public BigDecimal total() {
        return unitPrice.multiply(quantity);
    }
}
