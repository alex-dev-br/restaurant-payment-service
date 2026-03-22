package br.com.fiap.restaurant.payment.infra.messaging.inbound.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderDTO (Long id, UUID customerUuid, List<OrderItemDTO> items) {
    public BigDecimal total() {
        return items.stream().map(OrderItemDTO::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
