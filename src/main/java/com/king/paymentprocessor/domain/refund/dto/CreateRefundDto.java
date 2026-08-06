package com.king.paymentprocessor.domain.refund.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateRefundDto {
    @Schema(description = "Amount to refund, must not exceed captured amount", example = "1000.00")
    @NotNull
    @Positive
    private BigDecimal amount;

    @Schema(description = "Reason for the refund", example = "Customer requested cancellation")
    private String reason;
}