package br.com.fiap.restaurant.payment.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.retry.policy")
public class PaymentRetryPolicyProperties {

    private int maxAttempts = 3;
    private boolean publishPendingOnRetryFailure = false;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isPublishPendingOnRetryFailure() {
        return publishPendingOnRetryFailure;
    }

    public void setPublishPendingOnRetryFailure(boolean publishPendingOnRetryFailure) {
        this.publishPendingOnRetryFailure = publishPendingOnRetryFailure;
    }
}
