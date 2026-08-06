package com.king.paymentprocessor.domain.refund;

import com.king.paymentprocessor.domain.refund.dto.CreateRefundDto;
import com.king.paymentprocessor.domain.refund.entity.Refund;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments/{paymentId}/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Full and partial refunds against captured payments")
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "Create a refund for a captured payment")
    @PostMapping
    public ResponseEntity<Refund> create(
            @PathVariable String paymentId,
            @Valid @RequestBody CreateRefundDto payload
    ) {
        Refund refund = refundService.create(paymentId, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(refund);
    }

    @Operation(summary = "List all refunds for a payment")
    @GetMapping
    public List<Refund> getAll(@PathVariable String paymentId) {
        return refundService.getByPaymentId(paymentId);
    }
}