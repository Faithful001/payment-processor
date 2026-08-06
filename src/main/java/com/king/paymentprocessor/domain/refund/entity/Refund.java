package com.king.paymentprocessor.domain.refund.entity;

import com.king.paymentprocessor.domain.payment.entity.Payment;
import com.king.paymentprocessor.domain.refund.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JoinColumn(name = "payment_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Payment payment;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}