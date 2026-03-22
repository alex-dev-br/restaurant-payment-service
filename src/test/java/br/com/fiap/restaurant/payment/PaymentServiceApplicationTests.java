package br.com.fiap.restaurant.payment;

import br.com.fiap.restaurant.payment.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"app.payment.retry.scheduler.enabled=false",
		"app.payment.outbox.publisher.enabled=false"
})
@ActiveProfiles("test")
class PaymentServiceApplicationTests extends AbstractPostgresIntegrationTest {

	@Test
	void contextLoads() {
	}
}
