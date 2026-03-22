package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.amqp.core.*;
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
                .quorum()
                .deadLetterExchange(properties.getExchange().getOrderDl())
                .deadLetterRoutingKey(properties.getQueue().getPaymentOrderCreated())
                .build();
    }

    @Bean
    public Queue paymentOrderCreatedDlq(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getDlq().getPaymentOrderCreated())
                .quorum()
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
                .with(properties.getQueue().getPaymentOrderCreated());
    }

    @Bean
    public Queue paymentApprovedDebugQueue(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getQueue().getPaymentApprovedDebug())
                .quorum()
                .deadLetterExchange(properties.getExchange().getPaymentDl())
                .deadLetterRoutingKey(properties.getQueue().getPaymentApprovedDebug())
                .build();
    }

    @Bean
    public Queue paymentApprovedDebugDlq(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getDlq().getPaymentApprovedDebug())
                .quorum()
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
                .with(properties.getQueue().getPaymentApprovedDebug());
    }

    @Bean
    public Queue paymentPendingDebugQueue(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getQueue().getPaymentPendingDebug())
                .quorum()
                .deadLetterExchange(properties.getExchange().getPaymentDl())
                .deadLetterRoutingKey(properties.getQueue().getPaymentPendingDebug())
                .build();
    }

    @Bean
    public Queue paymentPendingDebugDlq(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getDlq().getPaymentPendingDebug())
                .quorum()
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
                .with(properties.getQueue().getPaymentPendingDebug());
    }

    @Bean
    public Queue paymentFailedDebugQueue(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getQueue().getPaymentFailedDebug())
                .quorum()
                .deadLetterExchange(properties.getExchange().getPaymentDl())
                .deadLetterRoutingKey(properties.getQueue().getPaymentFailedDebug())
                .build();
    }

    @Bean
    public Queue paymentFailedDebugDlq(RabbitProperties properties) {
        return QueueBuilder
                .durable(properties.getDlq().getPaymentFailedDebug())
                .quorum()
                .build();
    }

    @Bean
    public Binding paymentFailedDebugBinding(
            Queue paymentFailedDebugQueue,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentFailedDebugQueue)
                .to(paymentExchange)
                .with(properties.getRoutingKey().getPaymentFailed());
    }

    @Bean
    public Binding paymentFailedDebugDlqBinding(
            Queue paymentFailedDebugDlq,
            DirectExchange paymentExchange,
            RabbitProperties properties
    ) {
        return BindingBuilder
                .bind(paymentFailedDebugDlq)
                .to(paymentExchange)
                .with(properties.getQueue().getPaymentFailedDebug());
    }
}