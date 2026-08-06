package com.king.paymentprocessor.domain.payment;

import com.king.paymentprocessor.domain.payment.dto.CreatePaymentDto;
import com.king.paymentprocessor.domain.payment.entity.Payment;
import com.king.paymentprocessor.domain.payment.entity.PaymentAttempt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Create, capture, and inspect payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentAttemptRepository paymentAttemptRepository;

    @Operation(summary = "Create and authorize a new payment")
    @PostMapping
    public ResponseEntity<Payment> create(
            @Valid @RequestBody CreatePaymentDto payload,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        Payment payment = paymentService.create(payload, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @Operation(summary = "Capture a previously authorized payment")
    @PostMapping("/{id}/capture")
    public Payment capture(@PathVariable String id) {
        return paymentService.capture(id);
    }

    @Operation(summary = "Get a payment by ID")
    @GetMapping("/{id}")
    public Payment getById(@PathVariable String id) {
        return paymentService.getById(id);
    }

    @Operation(summary = "Get the processing attempt history for a payment")
    @GetMapping("/{id}/attempts")
    public List<PaymentAttempt> getAttempts(@PathVariable String id) {
        return paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(id);
    }
}