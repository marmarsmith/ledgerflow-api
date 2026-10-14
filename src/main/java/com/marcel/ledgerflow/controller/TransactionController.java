package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.CreateTransactionRequest;
import com.marcel.ledgerflow.dto.TransactionResponse;
import com.marcel.ledgerflow.dto.UpdateTransactionRequest;
import com.marcel.ledgerflow.service.TransactionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable Long accountId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        TransactionResponse response = transactionService.createTransaction(
                userId,
                accountId,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable Long accountId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                transactionService.getTransactionsByAccountId(userId, accountId)
        );
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long accountId,
            @PathVariable Long transactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                transactionService.getTransactionById(
                        userId,
                        accountId,
                        transactionId
                )
        );
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long accountId,
            @PathVariable Long transactionId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                transactionService.updateTransaction(
                        userId,
                        accountId,
                        transactionId,
                        request
                )
        );
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long accountId,
            @PathVariable Long transactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        transactionService.deleteTransaction(
                userId,
                accountId,
                transactionId
        );

        return ResponseEntity.noContent().build();
    }
}