package com.king.paymentprocessor.infrastructure.client.dto;

import java.math.BigDecimal;

public record EntryLineRequest(
        String accountId,
        BigDecimal amount,
        String type
) {}