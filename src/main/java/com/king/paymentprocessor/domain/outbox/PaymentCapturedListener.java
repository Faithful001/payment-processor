package com.king.paymentprocessor.domain.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king.paymentprocessor.domain.payment.dto.PaymentCapturedEvent;
import com.king.paymentprocessor.infrastructure.client.LedgerClient;
import com.king.paymentprocessor.infrastructure.messaging.rabbitmq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCapturedListener {

    private final LedgerClient ledgerClient;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handlePaymentCaptured(String payload) {
        try {
            PaymentCapturedEvent event = objectMapper.readValue(payload, PaymentCapturedEvent.class);
            log.info("Processing PaymentCaptured event for payment {}", event.paymentId());

            ledgerClient.postTransaction(event);

        } catch (Exception e) {
            log.error("Failed to process PaymentCaptured event: {}", payload, e);
            throw new RuntimeException(e); // rethrow so rabbitmq retries / eventually dead-letters it
        }
    }
}