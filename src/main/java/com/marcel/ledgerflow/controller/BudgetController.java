package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.BudgetRequest;
import com.marcel.ledgerflow.dto.BudgetResponse;
import com.marcel.ledgerflow.dto.UpdateBudgetRequest;
import com.marcel.ledgerflow.service.BudgetService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BudgetRequest request
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        BudgetResponse response =
                budgetService.createBudget(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        List<BudgetResponse> budgets =
                budgetService.getBudgetsByMonth(userId, year, month);

        return ResponseEntity.ok(budgets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        BudgetResponse response =
                budgetService.getBudgetById(id, userId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        BudgetResponse response =
                budgetService.updateBudget(id, userId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        budgetService.deleteBudget(id, userId);

        return ResponseEntity.noContent().build();
    }
}