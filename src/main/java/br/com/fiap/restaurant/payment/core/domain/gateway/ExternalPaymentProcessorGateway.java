package br.com.fiap.restaurant.payment.core.domain.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalPaymentProcessorGateway {

    boolean process(UUID paymentId, UUID clientId, BigDecimal amount);
}
