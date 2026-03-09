package br.com.fiap.restaurant.payment.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.retry.scheduler")
public class PaymentRetrySchedulerProperties {

    private boolean enabled = true;
    private long fixedDelayMs = 30000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }
}
