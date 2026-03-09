package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitMessagingConfig {

    @Bean
    public TopicExchange paymentExchange(RabbitProperties rabbitProperties) {
        return new TopicExchange(rabbitProperties.getExchange(), true, false);
    }

    @Bean
    public Queue approvedDebugQueue(RabbitProperties rabbitProperties) {
        return new Queue(rabbitProperties.getApprovedDebugQueue(), true);
    }

    @Bean
    public Queue pendingDebugQueue(RabbitProperties rabbitProperties) {
        return new Queue(rabbitProperties.getPendingDebugQueue(), true);
    }

    @Bean
    public Binding approvedDebugBinding(
            Queue approvedDebugQueue,
            TopicExchange paymentExchange,
            RabbitProperties rabbitProperties
    ) {
        return BindingBuilder
                .bind(approvedDebugQueue)
                .to(paymentExchange)
                .with(rabbitProperties.getPaymentApprovedRoutingKey());
    }

    @Bean
    public Binding pendingDebugBinding(
            Queue pendingDebugQueue,
            TopicExchange paymentExchange,
            RabbitProperties rabbitProperties
    ) {
        return BindingBuilder
                .bind(pendingDebugQueue)
                .to(paymentExchange)
                .with(rabbitProperties.getPaymentPendingRoutingKey());
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jacksonJsonMessageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonJsonMessageConverter);
        return rabbitTemplate;
    }
}
