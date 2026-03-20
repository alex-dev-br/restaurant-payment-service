package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.gateway.ProcessedMessageRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.command.HandleOrderCreatedEventCommand;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;

public class HandleOrderCreatedEventUseCase {

    private static final String MESSAGE_TYPE = "ORDER_CREATED";

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ProcessedMessageRepositoryGateway processedMessageRepositoryGateway;

    public HandleOrderCreatedEventUseCase(
            ProcessPaymentUseCase processPaymentUseCase,
            ProcessedMessageRepositoryGateway processedMessageRepositoryGateway
    ) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.processedMessageRepositoryGateway = processedMessageRepositoryGateway;
    }

    public void execute(HandleOrderCreatedEventCommand command) {
        boolean registered = processedMessageRepositoryGateway.registerIfAbsent(
                command.messageId(),
                MESSAGE_TYPE,
                String.valueOf(command.orderId())
        );

        if (!registered) {
            return;
        }

        ProcessPaymentCommand processPaymentCommand = new ProcessPaymentCommand(
                command.orderId(),
                command.clientId(),
                command.amount()
        );

        processPaymentUseCase.execute(processPaymentCommand);
    }
}