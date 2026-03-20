package br.com.fiap.restaurant.payment.infra.controller;

import br.com.fiap.restaurant.payment.core.domain.exception.PaymentNotFoundException;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.usecase.FindPaymentByOrderIdUseCase;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import br.com.fiap.restaurant.payment.infra.controller.mapper.PaymentControllerMapper;
import br.com.fiap.restaurant.payment.infra.controller.response.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
@Import(br.com.fiap.restaurant.payment.infra.controller.handler.GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessPaymentUseCase processPaymentUseCase;

    @MockitoBean
    private FindPaymentByOrderIdUseCase findPaymentByOrderIdUseCase;

    @MockitoBean
    private PaymentControllerMapper paymentControllerMapper;

    @Test
    void shouldProcessPaymentAndReturnCreated() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Long orderId = 1L;
        UUID clientId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        BigDecimal amount = new BigDecimal("120.00");
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime updatedAt = createdAt.plusSeconds(1);

        Payment payment = new Payment(
                paymentId,
                orderId,
                clientId,
                amount,
                PaymentStatus.APPROVED,
                createdAt,
                updatedAt
        );

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                clientId,
                amount,
                "APPROVED",
                createdAt,
                updatedAt
        );

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        when(processPaymentUseCase.execute(command)).thenReturn(payment);
        when(paymentControllerMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "clientId": "22222222-2222-2222-2222-222222222222",
                                  "amount": 120.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.amount").value(120.00))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldFindPaymentByOrderIdAndReturnOk() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Long orderId = 1L;
        UUID clientId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        BigDecimal amount = new BigDecimal("120.00");
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime updatedAt = createdAt.plusSeconds(1);

        Payment payment = new Payment(
                paymentId,
                orderId,
                clientId,
                amount,
                PaymentStatus.APPROVED,
                createdAt,
                updatedAt
        );

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                clientId,
                amount,
                "APPROVED",
                createdAt,
                updatedAt
        );

        when(findPaymentByOrderIdUseCase.execute(orderId)).thenReturn(payment);
        when(paymentControllerMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(get("/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.amount").value(120.00))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturnBadRequestWhenProcessPaymentRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": null,
                                  "clientId": null,
                                  "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.fields.orderId").exists())
                .andExpect(jsonPath("$.fields.clientId").exists())
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
        Long orderId = 999L;

        when(findPaymentByOrderIdUseCase.execute(orderId))
                .thenThrow(new PaymentNotFoundException(orderId));

        mockMvc.perform(get("/payments/order/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Pagamento não encontrado para o orderId: " + orderId
                ));
    }
}
