package br.com.fiap.restaurant.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.task.scheduling.enabled=false"
})
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
