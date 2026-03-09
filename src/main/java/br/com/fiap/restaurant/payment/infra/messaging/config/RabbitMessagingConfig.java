package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitMessagingConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange orderExchange(RabbitProperties properties) {
        return new DirectExchange(properties.getOrderExchange(), true, false);
    }

    @Bean
    public DirectExchange paymentExchange(RabbitProperties properties) {
        return new DirectExchange(properties.getPaymentExchange(), true, false);
    }

    @Bean
    public Queue orderCreatedQueue(RabbitProperties properties) {
        return new Queue(properties.getOrderCreatedQueue(), true);
    }

    @Bean
    public Binding orderCreatedBinding(
            Queue orderCreatedQueue,
            DirectExchange orderExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderExchange)
                .with(properties.getOrderCreatedRoutingKey());
    }

    @Bean
    public Queue approvedDebugQueue(RabbitProperties properties) {
        return new Queue(properties.getApprovedDebugQueue(), true);
    }

    @Bean
    public Queue pendingDebugQueue(RabbitProperties properties) {
        return new Queue(properties.getPendingDebugQueue(), true);
    }

    @Bean
    public Binding approvedDebugBinding(
            Queue approvedDebugQueue,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(approvedDebugQueue)
                .to(paymentExchange)
                .with(properties.getPaymentApprovedRoutingKey());
    }

    @Bean
    public Binding pendingDebugBinding(
            Queue pendingDebugQueue,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(pendingDebugQueue)
                .to(paymentExchange)
                .with(properties.getPaymentPendingRoutingKey());
    }
}