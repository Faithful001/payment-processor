package com.king.paymentprocessor.domain.refund;

import com.king.paymentprocessor.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, String> {
    List<Refund> findByPaymentId(String paymentId);
}