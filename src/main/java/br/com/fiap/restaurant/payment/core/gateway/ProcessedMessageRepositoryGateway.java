package br.com.fiap.restaurant.payment.core.gateway;

import java.util.UUID;

public interface ProcessedMessageRepositoryGateway {

    boolean registerIfAbsent(UUID messageId, String messageType, String aggregateKey);
}
