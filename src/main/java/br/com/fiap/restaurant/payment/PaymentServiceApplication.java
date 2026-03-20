package br.com.fiap.restaurant.payment;

import br.com.fiap.restaurant.payment.infra.config.PaymentOutboxPublisherProperties;
import br.com.fiap.restaurant.payment.infra.config.PaymentRetryPolicyProperties;
import br.com.fiap.restaurant.payment.infra.config.PaymentRetrySchedulerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
		PaymentRetrySchedulerProperties.class,
		PaymentRetryPolicyProperties.class,
		PaymentOutboxPublisherProperties.class
})
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}
}
