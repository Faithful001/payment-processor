package com.king.paymentprocessor.infrastructure.client.dto;

import java.util.List;

public record CreateTransactionRequest(
        String description,
        List<EntryLineRequest> entries
) {}