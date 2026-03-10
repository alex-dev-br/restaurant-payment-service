package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class FakeExternalPaymentProcessorGateway implements ExternalPaymentProcessorGateway {

    @Override
    public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {

        return true;
    }
}
