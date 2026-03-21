package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import br.com.fiap.restaurant.payment.infra.client.processor.ExternalPaymentProcessorClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@TestConfiguration
public class ControlledExternalPaymentProcessorTestConfig {

    @Bean
    @Primary
    ControlledExternalPaymentProcessorClient controlledExternalPaymentProcessorClient() {
        return new ControlledExternalPaymentProcessorClient();
    }

    public static class ControlledExternalPaymentProcessorClient implements ExternalPaymentProcessorClient {

        private final Queue<ProcessorResult> results = new ConcurrentLinkedQueue<>();

        public void reset() {
            results.clear();
        }

        public void enqueueApproved() {
            results.add(ProcessorResult.approvedResult());
        }

        public void enqueuePending() {
            results.add(ProcessorResult.pendingResult());
        }

        public void enqueueFailure(RuntimeException exception) {
            results.add(ProcessorResult.failureResult(exception));
        }

        @Override
        public boolean process(UUID paymentId, UUID clientId, BigDecimal amount) {
            ProcessorResult result = results.poll();

            if (result == null) {
                return true;
            }

            if (result.exception() != null) {
                throw result.exception();
            }

            return Boolean.TRUE.equals(result.shouldApprove());
        }

        private record ProcessorResult(Boolean shouldApprove, RuntimeException exception) {

            static ProcessorResult approvedResult() {
                return new ProcessorResult(true, null);
            }

            static ProcessorResult pendingResult() {
                return new ProcessorResult(false, null);
            }

            static ProcessorResult failureResult(RuntimeException exception) {
                return new ProcessorResult(null, exception);
            }
        }
    }
}
