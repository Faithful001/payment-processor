package com.king.paymentprocessor.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreatePaymentDto {
    @Schema(description = "Account the payment is drawn from", example = "acc-source-id")
    @NotBlank
    private String sourceAccountId;

    @Schema(description = "Account the payment is sent to", example = "acc-dest-id")
    @NotBlank
    private String destinationAccountId;

    @Schema(description = "Payment amount", example = "5000.00")
    @NotNull
    @Positive
    private BigDecimal amount;

    @Schema(description = "ISO 4217 currency code", example = "NGN")
    @NotBlank
    private String currency;
}