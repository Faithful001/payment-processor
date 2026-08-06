package com.king.paymentprocessor.infrastructure.client;

import com.king.paymentprocessor.infrastructure.client.dto.CreateTransactionRequest;
import com.king.paymentprocessor.infrastructure.client.dto.EntryLineRequest;
import com.king.paymentprocessor.domain.payment.dto.PaymentCapturedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class LedgerClient {

    private final RestClient restClient;

    public LedgerClient(@Value("${ledger.engine.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void postTransaction(PaymentCapturedEvent event) {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "Payment captured: " + event.paymentId(),
                List.of(
                        new EntryLineRequest(event.destinationAccountId(), event.amount(), "DEBIT"),
                        new EntryLineRequest(event.sourceAccountId(), event.amount(), "CREDIT")
                )
        );

        restClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "payment-" + event.paymentId())
                .header("X-User-Id", "system") // placeholder, matches ledger engine's current auth stub
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}