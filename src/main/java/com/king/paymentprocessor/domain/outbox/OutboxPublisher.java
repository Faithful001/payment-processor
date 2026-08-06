package com.king.paymentprocessor.domain.outbox;

import com.king.paymentprocessor.infrastructure.messaging.rabbitmq.RabbitMQConfig;
import com.king.paymentprocessor.domain.outbox.entity.OutboxEvent;
import com.king.paymentprocessor.domain.outbox.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    // polls every 2 seconds for unpublished events and sends them to RabbitMQ
    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent event : pending) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.PAYMENT_EXCHANGE,
                        RabbitMQConfig.PAYMENT_ROUTING_KEY,
                        event.getPayload()
                );
                event.setStatus(OutboxStatus.PUBLISHED);
            } catch (Exception e) {
                event.setStatus(OutboxStatus.FAILED);
            }
            outboxEventRepository.save(event);
        }
    }
}