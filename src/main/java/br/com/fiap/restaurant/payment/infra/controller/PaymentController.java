package br.com.fiap.restaurant.payment.infra.controller;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.usecase.FindPaymentByOrderIdUseCase;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.infra.controller.mapper.PaymentControllerMapper;
import br.com.fiap.restaurant.payment.infra.controller.request.ProcessPaymentRequest;
import br.com.fiap.restaurant.payment.infra.controller.response.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final FindPaymentByOrderIdUseCase findPaymentByOrderIdUseCase;
    private final PaymentControllerMapper paymentControllerMapper;

    public PaymentController(
            ProcessPaymentUseCase processPaymentUseCase,
            FindPaymentByOrderIdUseCase findPaymentByOrderIdUseCase,
            PaymentControllerMapper paymentControllerMapper
    ) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.findPaymentByOrderIdUseCase = findPaymentByOrderIdUseCase;
        this.paymentControllerMapper = paymentControllerMapper;
    }

    @PostMapping("/process")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        Payment payment = processPaymentUseCase.execute(
                request.orderId(),
                request.clientId(),
                request.amount()
        );

        return paymentControllerMapper.toResponse(payment);
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse findByOrderId(@PathVariable Long orderId) {
        Payment payment = findPaymentByOrderIdUseCase.execute(orderId);
        return paymentControllerMapper.toResponse(payment);
    }
}
