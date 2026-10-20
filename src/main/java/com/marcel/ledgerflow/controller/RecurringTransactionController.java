
package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.RecurringTransactionRequest;
import com.marcel.ledgerflow.dto.RecurringTransactionResponse;
import com.marcel.ledgerflow.service.RecurringTransactionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionController(
            RecurringTransactionService recurringTransactionService
    ) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecurringTransactionRequest request
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        RecurringTransactionResponse response =
                recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>> getRecurringTransactions(
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        List<RecurringTransactionResponse> responses =
                recurringTransactionService.getRecurringTransactions(userId);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{recurringTransactionId}")
    public ResponseEntity<RecurringTransactionResponse> getRecurringTransactionById(
            @PathVariable Long recurringTransactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        RecurringTransactionResponse response =
                recurringTransactionService.getRecurringTransactionById(
                        userId,
                        recurringTransactionId
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{recurringTransactionId}")
    public ResponseEntity<RecurringTransactionResponse> updateRecurringTransaction(
            @PathVariable Long recurringTransactionId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecurringTransactionRequest request
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        RecurringTransactionResponse response =
                recurringTransactionService.updateRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{recurringTransactionId}/pause")
    public ResponseEntity<RecurringTransactionResponse> pauseRecurringTransaction(
            @PathVariable Long recurringTransactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        RecurringTransactionResponse response =
                recurringTransactionService.pauseRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{recurringTransactionId}/resume")
    public ResponseEntity<RecurringTransactionResponse> resumeRecurringTransaction(
            @PathVariable Long recurringTransactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        RecurringTransactionResponse response =
                recurringTransactionService.resumeRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recurringTransactionId}")
    public ResponseEntity<Void> deleteRecurringTransaction(
            @PathVariable Long recurringTransactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        recurringTransactionService.deleteRecurringTransaction(
                userId,
                recurringTransactionId
        );

        return ResponseEntity.noContent().build();
    }




}
