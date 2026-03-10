package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.infra.client.processor.ExternalPaymentProcessorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResilientExternalPaymentProcessorGatewayTest {

    private ExternalPaymentProcessorClient client;
    private ResilientExternalPaymentProcessorGateway gateway;

    @BeforeEach
    void setUp() {
        client = mock(ExternalPaymentProcessorClient.class);
        gateway = new ResilientExternalPaymentProcessorGateway(client);
    }

    @Test
    void shouldReturnTrueWhenClientSucceeds() {
        when(client.process(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenReturn(true);

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenClientThrowsException() {
        when(client.process(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("processor unavailable"));

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertFalse(result);
    }
}
