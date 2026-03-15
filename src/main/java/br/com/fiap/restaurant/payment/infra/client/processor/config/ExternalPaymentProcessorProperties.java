package br.com.fiap.restaurant.payment.infra.client.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.external-payment")
public record ExternalPaymentProcessorProperties(

        boolean fakeEnabled,
        String baseUrl,
        String requestPath,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
