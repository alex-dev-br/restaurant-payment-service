package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.gateway.ProcessedMessageRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.command.HandleOrderCreatedEventCommand;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HandleOrderCreatedEventUseCaseTest {

    private ProcessPaymentUseCase processPaymentUseCase;
    private ProcessedMessageRepositoryGateway processedMessageRepositoryGateway;
    private HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase;

    @BeforeEach
    void setUp() {
        processPaymentUseCase = mock(ProcessPaymentUseCase.class);
        processedMessageRepositoryGateway = mock(ProcessedMessageRepositoryGateway.class);
        handleOrderCreatedEventUseCase = new HandleOrderCreatedEventUseCase(
                processPaymentUseCase,
                processedMessageRepositoryGateway
        );
    }

    @Test
    void shouldProcessPaymentWhenMessageIsNew() {
        UUID messageId = UUID.randomUUID();
        Long orderId = 10L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        HandleOrderCreatedEventCommand command = new HandleOrderCreatedEventCommand(
                messageId,
                orderId,
                clientId,
                amount
        );

        when(processedMessageRepositoryGateway.registerIfAbsent(
                messageId,
                "ORDER_CREATED",
                String.valueOf(orderId)
        )).thenReturn(true);

        handleOrderCreatedEventUseCase.execute(command);

        ArgumentCaptor<ProcessPaymentCommand> captor =
                ArgumentCaptor.forClass(ProcessPaymentCommand.class);

        verify(processPaymentUseCase).execute(captor.capture());

        ProcessPaymentCommand captured = captor.getValue();

        assertEquals(orderId, captured.orderId());
        assertEquals(clientId, captured.clientId());
        assertEquals(amount, captured.amount());
    }

    @Test
    void shouldIgnoreMessageWhenItWasAlreadyProcessed() {
        UUID messageId = UUID.randomUUID();
        Long orderId = 10L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        HandleOrderCreatedEventCommand command = new HandleOrderCreatedEventCommand(
                messageId,
                orderId,
                clientId,
                amount
        );

        when(processedMessageRepositoryGateway.registerIfAbsent(
                messageId,
                "ORDER_CREATED",
                String.valueOf(orderId)
        )).thenReturn(false);

        handleOrderCreatedEventUseCase.execute(command);

        verify(processPaymentUseCase, never()).execute(any(ProcessPaymentCommand.class));
    }
}
