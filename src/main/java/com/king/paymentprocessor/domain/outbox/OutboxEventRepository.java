package com.king.paymentprocessor.domain.outbox;

import com.king.paymentprocessor.domain.outbox.entity.OutboxEvent;
import com.king.paymentprocessor.domain.outbox.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByStatus(OutboxStatus status);
}