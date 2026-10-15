package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.SavingsGoalRequest;
import com.marcel.ledgerflow.dto.SavingsGoalResponse;
import com.marcel.ledgerflow.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.marcel.ledgerflow.dto.SavingsContributionRequest;
import com.marcel.ledgerflow.dto.SavingsContributionResponse;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsGoalResponse createGoal(
            @Valid @RequestBody SavingsGoalRequest request,
            Authentication authentication
    ) {

        Long userId = Long.valueOf(authentication.getName());

        return savingsGoalService.createGoal(userId, request);
    }

    @GetMapping
    public List<SavingsGoalResponse> getGoals(
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return savingsGoalService.getGoals(userId);
    }

    @GetMapping("/{id}")
    public SavingsGoalResponse getGoal(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return savingsGoalService.getGoal(userId, id);
    }

    @PutMapping("/{id}")
    public SavingsGoalResponse updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalRequest request,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return savingsGoalService.updateGoal(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        savingsGoalService.deleteGoal(userId, id);
    }

    @PostMapping("/{id}/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsContributionResponse addContribution(
            @PathVariable Long id,
            @Valid @RequestBody SavingsContributionRequest request,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return savingsGoalService.addContribution(userId, id, request);
    }

    @GetMapping("/{id}/contributions")
    public List<SavingsContributionResponse> getContributions(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return savingsGoalService.getContributions(userId, id);
    }
}