package com.king.paymentprocessor.domain.refund;

import com.king.paymentprocessor.domain.payment.PaymentRepository;
import com.king.paymentprocessor.domain.payment.entity.Payment;
import com.king.paymentprocessor.domain.payment.enums.PaymentStatus;
import com.king.paymentprocessor.domain.refund.dto.CreateRefundDto;
import com.king.paymentprocessor.domain.refund.entity.Refund;
import com.king.paymentprocessor.domain.refund.enums.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Refund create(String paymentId, CreateRefundDto payload) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CAPTURED payments can be refunded");
        }

        BigDecimal alreadyRefunded = refundRepository.findByPaymentId(paymentId).stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (alreadyRefunded.add(payload.getAmount()).compareTo(payment.getAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Refund amount exceeds captured payment amount");
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(payload.getAmount())
                .reason(payload.getReason())
                .status(RefundStatus.COMPLETED)
                .build();

        refundRepository.save(refund);

        if (alreadyRefunded.add(payload.getAmount()).compareTo(payment.getAmount()) == 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        return refund;
    }

    public List<Refund> getByPaymentId(String paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }
}