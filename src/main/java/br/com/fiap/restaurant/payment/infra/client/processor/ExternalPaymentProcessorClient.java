package br.com.fiap.restaurant.payment.infra.client.processor;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalPaymentProcessorClient {
    boolean process(UUID paymentId, UUID clientId, BigDecimal amount);
}
