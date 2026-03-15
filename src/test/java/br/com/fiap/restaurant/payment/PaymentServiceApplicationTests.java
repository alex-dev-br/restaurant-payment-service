package br.com.fiap.restaurant.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"app.payment.retry.scheduler.enabled=false"
})
@ActiveProfiles("test")
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
