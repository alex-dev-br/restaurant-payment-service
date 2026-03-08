package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicsProperties {

    private String orderCreated;
    private String paymentApproved;
    private String paymentPending;

    public String getOrderCreated() {
        return orderCreated;
    }

    public void setOrderCreated(String orderCreated) {
        this.orderCreated = orderCreated;
    }

    public String getPaymentApproved() {
        return paymentApproved;
    }

    public void setPaymentApproved(String paymentApproved) {
        this.paymentApproved = paymentApproved;
    }

    public String getPaymentPending() {
        return paymentPending;
    }

    public void setPaymentPending(String paymentPending) {
        this.paymentPending = paymentPending;
    }
}
