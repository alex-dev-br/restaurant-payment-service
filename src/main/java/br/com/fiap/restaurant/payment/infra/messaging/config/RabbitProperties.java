package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbit")
public class RabbitProperties {

    private String orderExchange;
    private String paymentExchange;
    private String orderCreatedQueue;
    private String orderCreatedRoutingKey;
    private String paymentApprovedRoutingKey;
    private String paymentPendingRoutingKey;
    private String approvedDebugQueue;
    private String pendingDebugQueue;

    public String getOrderExchange() {
        return orderExchange;
    }

    public void setOrderExchange(String orderExchange) {
        this.orderExchange = orderExchange;
    }

    public String getPaymentExchange() {
        return paymentExchange;
    }

    public void setPaymentExchange(String paymentExchange) {
        this.paymentExchange = paymentExchange;
    }

    public String getOrderCreatedQueue() {
        return orderCreatedQueue;
    }

    public void setOrderCreatedQueue(String orderCreatedQueue) {
        this.orderCreatedQueue = orderCreatedQueue;
    }

    public String getOrderCreatedRoutingKey() {
        return orderCreatedRoutingKey;
    }

    public void setOrderCreatedRoutingKey(String orderCreatedRoutingKey) {
        this.orderCreatedRoutingKey = orderCreatedRoutingKey;
    }

    public String getPaymentApprovedRoutingKey() {
        return paymentApprovedRoutingKey;
    }

    public void setPaymentApprovedRoutingKey(String paymentApprovedRoutingKey) {
        this.paymentApprovedRoutingKey = paymentApprovedRoutingKey;
    }

    public String getPaymentPendingRoutingKey() {
        return paymentPendingRoutingKey;
    }

    public void setPaymentPendingRoutingKey(String paymentPendingRoutingKey) {
        this.paymentPendingRoutingKey = paymentPendingRoutingKey;
    }

    public String getApprovedDebugQueue() {
        return approvedDebugQueue;
    }

    public void setApprovedDebugQueue(String approvedDebugQueue) {
        this.approvedDebugQueue = approvedDebugQueue;
    }

    public String getPendingDebugQueue() {
        return pendingDebugQueue;
    }

    public void setPendingDebugQueue(String pendingDebugQueue) {
        this.pendingDebugQueue = pendingDebugQueue;
    }
}