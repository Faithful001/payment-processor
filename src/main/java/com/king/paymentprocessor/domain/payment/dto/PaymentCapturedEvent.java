package com.king.paymentprocessor.domain.payment.dto;

import java.math.BigDecimal;

public record PaymentCapturedEvent(
        String paymentId,
        BigDecimal amount,
        String currency,
        String sourceAccountId,
        String destinationAccountId
) {}