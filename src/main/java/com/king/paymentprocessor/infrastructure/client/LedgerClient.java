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
    private final String apiKey;

    public LedgerClient(
            @Value("${ledger.engine.base-url}") String baseUrl,
            @Value("${ledger.engine.api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
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
                .header("X-Api-Key", apiKey)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}