package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange orderExchange(RabbitProperties properties) {
        return new DirectExchange(properties.getExchange().getOrder(), true, false);
    }

    @Bean
    public DirectExchange paymentExchange(RabbitProperties properties) {
        return new DirectExchange(properties.getExchange().getPayment(), true, false);
    }

    @Bean
    public Queue paymentOrderCreatedQueue(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getQueue().getPaymentOrderCreated())
                .deadLetterExchange(properties.getExchange().getOrder())
                .deadLetterRoutingKey(properties.getDlq().getPaymentOrderCreated())
                .build();
    }

    @Bean
    public Queue paymentOrderCreatedDlq(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getDlq().getPaymentOrderCreated())
                .build();
    }

    @Bean
    public Binding paymentOrderCreatedBinding(
            Queue paymentOrderCreatedQueue,
            DirectExchange orderExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentOrderCreatedQueue)
                .to(orderExchange)
                .with(properties.getRoutingKey().getOrderCreated());
    }

    @Bean
    public Binding paymentOrderCreatedDlqBinding(
            Queue paymentOrderCreatedDlq,
            DirectExchange orderExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentOrderCreatedDlq)
                .to(orderExchange)
                .with(properties.getDlq().getPaymentOrderCreated());
    }

    @Bean
    public Queue paymentApprovedDebugQueue(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getQueue().getPaymentApprovedDebug())
                .deadLetterExchange(properties.getExchange().getPayment())
                .deadLetterRoutingKey(properties.getDlq().getPaymentApprovedDebug())
                .build();
    }

    @Bean
    public Queue paymentApprovedDebugDlq(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getDlq().getPaymentApprovedDebug())
                .build();
    }

    @Bean
    public Binding paymentApprovedDebugBinding(
            Queue paymentApprovedDebugQueue,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentApprovedDebugQueue)
                .to(paymentExchange)
                .with(properties.getRoutingKey().getPaymentApproved());
    }

    @Bean
    public Binding paymentApprovedDebugDlqBinding(
            Queue paymentApprovedDebugDlq,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentApprovedDebugDlq)
                .to(paymentExchange)
                .with(properties.getDlq().getPaymentApprovedDebug());
    }

    @Bean
    public Queue paymentPendingDebugQueue(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getQueue().getPaymentPendingDebug())
                .deadLetterExchange(properties.getExchange().getPayment())
                .deadLetterRoutingKey(properties.getDlq().getPaymentPendingDebug())
                .build();
    }

    @Bean
    public Queue paymentPendingDebugDlq(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getDlq().getPaymentPendingDebug())
                .build();
    }

    @Bean
    public Binding paymentPendingDebugBinding(
            Queue paymentPendingDebugQueue,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentPendingDebugQueue)
                .to(paymentExchange)
                .with(properties.getRoutingKey().getPaymentPending());
    }

    @Bean
    public Binding paymentPendingDebugDlqBinding(
            Queue paymentPendingDebugDlq,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentPendingDebugDlq)
                .to(paymentExchange)
                .with(properties.getDlq().getPaymentPendingDebug());
    }
}