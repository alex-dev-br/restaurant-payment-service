package br.com.fiap.restaurant.payment.infra.client.processor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "app.external-payment",
        name = "fake-enabled",
        havingValue = "true"
)
public class FakeExternalPaymentProcessorClient implements ExternalPaymentProcessorClient {

    @Override
    public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {
        return true;
    }
}
