package br.com.fiap.restaurant.payment.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ResilienceConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService externalPaymentExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
