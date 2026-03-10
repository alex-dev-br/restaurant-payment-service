package br.com.fiap.restaurant.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"app.payment.retry.scheduler.enabled=false"
})
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
