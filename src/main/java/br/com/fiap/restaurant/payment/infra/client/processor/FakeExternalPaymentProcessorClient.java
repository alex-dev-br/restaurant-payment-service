package br.com.fiap.restaurant.payment.infra.client.processor;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class FakeExternalPaymentProcessorClient implements ExternalPaymentProcessorClient {

    @Override
    public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {
        return true;
    }
}
