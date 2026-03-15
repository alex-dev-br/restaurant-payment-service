package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbit")
public class RabbitProperties {

    private Exchange exchange = new Exchange();
    private RoutingKey routingKey = new RoutingKey();
    private QueueConfig queue = new QueueConfig();
    private Dlq dlq = new Dlq();

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public RoutingKey getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(RoutingKey routingKey) {
        this.routingKey = routingKey;
    }

    public QueueConfig getQueue() {
        return queue;
    }

    public void setQueue(QueueConfig queue) {
        this.queue = queue;
    }

    public Dlq getDlq() {
        return dlq;
    }

    public void setDlq(Dlq dlq) {
        this.dlq = dlq;
    }

    public static class Exchange {
        private String order;
        private String payment;

        public String getOrder() {
            return order;
        }

        public void setOrder(String order) {
            this.order = order;
        }

        public String getPayment() {
            return payment;
        }

        public void setPayment(String payment) {
            this.payment = payment;
        }
    }

    public static class RoutingKey {
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

    public static class QueueConfig {
        private String paymentOrderCreated;
        private String paymentApprovedDebug;
        private String paymentPendingDebug;

        public String getPaymentOrderCreated() {
            return paymentOrderCreated;
        }

        public void setPaymentOrderCreated(String paymentOrderCreated) {
            this.paymentOrderCreated = paymentOrderCreated;
        }

        public String getPaymentApprovedDebug() {
            return paymentApprovedDebug;
        }

        public void setPaymentApprovedDebug(String paymentApprovedDebug) {
            this.paymentApprovedDebug = paymentApprovedDebug;
        }

        public String getPaymentPendingDebug() {
            return paymentPendingDebug;
        }

        public void setPaymentPendingDebug(String paymentPendingDebug) {
            this.paymentPendingDebug = paymentPendingDebug;
        }
    }

    public static class Dlq {
        private String paymentOrderCreated;
        private String paymentApprovedDebug;
        private String paymentPendingDebug;

        public String getPaymentOrderCreated() {
            return paymentOrderCreated;
        }

        public void setPaymentOrderCreated(String paymentOrderCreated) {
            this.paymentOrderCreated = paymentOrderCreated;
        }

        public String getPaymentApprovedDebug() {
            return paymentApprovedDebug;
        }

        public void setPaymentApprovedDebug(String paymentApprovedDebug) {
            this.paymentApprovedDebug = paymentApprovedDebug;
        }

        public String getPaymentPendingDebug() {
            return paymentPendingDebug;
        }

        public void setPaymentPendingDebug(String paymentPendingDebug) {
            this.paymentPendingDebug = paymentPendingDebug;
        }
    }
}