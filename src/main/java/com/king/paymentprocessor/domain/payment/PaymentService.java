package com.king.paymentprocessor.domain.payment;

import com.king.paymentprocessor.domain.outbox.OutboxEventRepository;
import com.king.paymentprocessor.domain.outbox.entity.OutboxEvent;
import com.king.paymentprocessor.domain.payment.dto.CreatePaymentDto;
import com.king.paymentprocessor.domain.payment.entity.Payment;
import com.king.paymentprocessor.domain.payment.entity.PaymentAttempt;
import com.king.paymentprocessor.domain.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Payment create(CreatePaymentDto payload, String idempotencyKey) {
        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Payment payment = Payment.builder()
                .amount(payload.getAmount())
                .currency(payload.getCurrency())
                .sourceAccountId(payload.getSourceAccountId())
                .destinationAccountId(payload.getDestinationAccountId())
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.CREATED)
                .build();

        paymentRepository.save(payment);

        // Simulate authorization happening immediately for simplicity.
        return authorize(payment.getId());
    }

    @Transactional
    public Payment authorize(String paymentId) {
        Payment payment = getById(paymentId);

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CREATED payments can be authorized");
        }

        payment.setStatus(PaymentStatus.AUTHORIZED);
        paymentRepository.save(payment);
        recordAttempt(payment, PaymentStatus.AUTHORIZED, null);

        return payment;
    }

    @Transactional
    public Payment capture(String paymentId) {
        Payment payment = getById(paymentId);

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only AUTHORIZED payments can be captured");
        }

        payment.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);
        recordAttempt(payment, PaymentStatus.CAPTURED, null);

        // write the event to the outbox in the SAME transaction as the state change.
        // this guarantees the event is never lost even if the app crashes right after.
        OutboxEvent event = OutboxEvent.builder()
                .eventType("PaymentCaptured")
                .payload(buildCapturedEventPayload(payment))
                .build();
        outboxEventRepository.save(event);

        return payment;
    }

    @Transactional
    public Payment fail(String paymentId, String reason) {
        Payment payment = getById(paymentId);
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        recordAttempt(payment, PaymentStatus.FAILED, reason);
        return payment;
    }

    public Payment getById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private void recordAttempt(Payment payment, PaymentStatus resultingStatus, String failureReason) {
        int nextAttemptNumber = paymentAttemptRepository
                .findByPaymentIdOrderByAttemptNumberAsc(payment.getId())
                .size() + 1;

        PaymentAttempt attempt = PaymentAttempt.builder()
                .payment(payment)
                .attemptNumber(nextAttemptNumber)
                .resultingStatus(resultingStatus)
                .failureReason(failureReason)
                .build();

        paymentAttemptRepository.save(attempt);
    }

    private String buildCapturedEventPayload(Payment payment) {
        // Simple manual JSON for now; swap for a proper ObjectMapper-based serializer as this grows.
        return String.format(
                "{\"paymentId\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"sourceAccountId\":\"%s\",\"destinationAccountId\":\"%s\"}",
                payment.getId(), payment.getAmount(), payment.getCurrency(),
                payment.getSourceAccountId(), payment.getDestinationAccountId()
        );
    }
}