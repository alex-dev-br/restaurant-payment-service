package br.com.fiap.restaurant.payment.infra.client.processor;

import br.com.fiap.restaurant.payment.infra.client.processor.config.ExternalPaymentProcessorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpExternalPaymentProcessorClientTest {

    private static final String BASE_URL = "http://localhost:8089";
    private static final String REQUEST_PATH = "/requisicao";

    private MockRestServiceServer server;
    private HttpExternalPaymentProcessorClient client;

    private UUID paymentId;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder
                .baseUrl(BASE_URL)
                .build();

        ExternalPaymentProcessorProperties properties =
                new ExternalPaymentProcessorProperties(
                        false,
                        BASE_URL,
                        REQUEST_PATH,
                        1000,
                        2000
                );

        client = new HttpExternalPaymentProcessorClient(restClient, properties);

        paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        clientId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    }

    @Test
    void shouldReturnTrueWhenPostIsAcceptedAndStatusIsPago() {
        server.expect(requestTo(BASE_URL + REQUEST_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "valor": 100,
                          "pagamento_id": "11111111-1111-1111-1111-111111111111",
                          "cliente_id": "22222222-2222-2222-2222-222222222222"
                        }
                        """))
                .andRespond(withCreatedEntity(null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"status":"accepted"}
                              """));

        server.expect(requestTo(BASE_URL + REQUEST_PATH + "/" + paymentId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "pagamento_id": "11111111-1111-1111-1111-111111111111",
                          "status": "pago"
                        }
                        """, MediaType.APPLICATION_JSON));

        boolean result = client.process(paymentId, clientId, BigDecimal.valueOf(100));

        assertTrue(result);
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenPostIsAcceptedButStatusIsNotPago() {
        server.expect(requestTo(BASE_URL + REQUEST_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"status":"accepted"}
                              """));

        server.expect(requestTo(BASE_URL + REQUEST_PATH + "/" + paymentId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "pagamento_id": "11111111-1111-1111-1111-111111111111",
                          "status": "pendente"
                        }
                        """, MediaType.APPLICATION_JSON));

        boolean result = client.process(paymentId, clientId, BigDecimal.valueOf(100));

        assertFalse(result);
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenPostResponseIsNotAccepted() {
        server.expect(requestTo(BASE_URL + REQUEST_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"status":"rejected"}
                              """));

        boolean result = client.process(paymentId, clientId, BigDecimal.valueOf(100));

        assertFalse(result);
        server.verify();
    }

    @Test
    void shouldThrowExceptionWhenPostReturnsServerError() {
        server.expect(requestTo(BASE_URL + REQUEST_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(RestClientResponseException.class, () ->
                client.process(paymentId, clientId, BigDecimal.valueOf(100))
        );

        server.verify();
    }

    @Test
    void shouldThrowArithmeticExceptionWhenAmountHasDecimalPlaces() {
        assertThrows(ArithmeticException.class, () ->
                client.process(paymentId, clientId, BigDecimal.valueOf(10.50))
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAmountIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                client.process(paymentId, clientId, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                client.process(paymentId, clientId, BigDecimal.valueOf(-5))
        );
    }
}

