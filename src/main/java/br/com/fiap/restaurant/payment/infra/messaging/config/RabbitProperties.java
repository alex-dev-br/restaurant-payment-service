package br.com.fiap.restaurant.payment.infra.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbit")
public class RabbitProperties {

    private String exchange;
    private String paymentApprovedRoutingKey;
    private String paymentPendingRoutingKey;

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
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
}
