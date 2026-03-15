package br.com.fiap.restaurant.payment.infra.client.processor;

import br.com.fiap.restaurant.payment.infra.client.processor.config.ExternalPaymentProcessorProperties;
import br.com.fiap.restaurant.payment.infra.client.processor.dto.ProcpagPaymentRequest;
import br.com.fiap.restaurant.payment.infra.client.processor.dto.ProcpagPaymentStatusResponse;
import br.com.fiap.restaurant.payment.infra.client.processor.dto.ProcpagPaymentSubmissionResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "app.external-payment",
        name = "fake-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class HttpExternalPaymentProcessorClient implements ExternalPaymentProcessorClient {

    private final RestClient restClient;
    private final ExternalPaymentProcessorProperties properties;

    public HttpExternalPaymentProcessorClient(
            RestClient externalPaymentRestClient,
            ExternalPaymentProcessorProperties properties
    ) {
        this.restClient = externalPaymentRestClient;
        this.properties = properties;
    }

    @Override
    public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {
        int valor = amount.intValueExact();

        if (valor <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be a positive integer for procpag integration"
            );
        }

        ProcpagPaymentRequest request = new ProcpagPaymentRequest(
                valor,
                paymentId.toString(),
                clientId.toString()
        );

        ProcpagPaymentSubmissionResponse submissionResponse = restClient.post()
                .uri(properties.requestPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ProcpagPaymentSubmissionResponse.class);

        if (submissionResponse == null || !submissionResponse.isAccepted()) {
            return false;
        }

        ProcpagPaymentStatusResponse statusResponse = restClient.get()
                .uri(properties.requestPath() + "/{paymentId}", paymentId.toString())
                .retrieve()
                .body(ProcpagPaymentStatusResponse.class);

        return statusResponse != null && statusResponse.isPaid();
    }
}

