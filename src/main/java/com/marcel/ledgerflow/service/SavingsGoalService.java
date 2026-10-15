package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.SavingsGoalRequest;
import com.marcel.ledgerflow.dto.SavingsGoalResponse;
import com.marcel.ledgerflow.entity.SavingsGoal;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.SavingsGoalRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import com.marcel.ledgerflow.dto.SavingsContributionRequest;
import com.marcel.ledgerflow.dto.SavingsContributionResponse;
import com.marcel.ledgerflow.entity.SavingsContribution;
import com.marcel.ledgerflow.repository.SavingsContributionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final SavingsContributionRepository savingsContributionRepository;

    public SavingsGoalService(
            SavingsGoalRepository savingsGoalRepository,
            UserRepository userRepository,
            SavingsContributionRepository savingsContributionRepository
    ) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
        this.savingsContributionRepository = savingsContributionRepository;
    }

    @Transactional
    public SavingsGoalResponse createGoal(
            Long userId,
            SavingsGoalRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        SavingsGoal goal = new SavingsGoal();

        goal.setUser(user);
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setSavedAmount(BigDecimal.ZERO);
        goal.setTargetDate(request.targetDate());

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);

        return toResponse(savedGoal);
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getGoals(Long userId) {

        return savingsGoalRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getGoal(Long userId, Long goalId) {

        SavingsGoal goal = savingsGoalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Savings goal not found"
                ));

        return toResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse updateGoal(
            Long userId,
            Long goalId,
            SavingsGoalRequest request
    ) {

        SavingsGoal goal = savingsGoalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Savings goal not found"
                ));

        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());

        SavingsGoal updatedGoal = savingsGoalRepository.save(goal);

        return toResponse(updatedGoal);
    }

    @Transactional
    public void deleteGoal(Long userId, Long goalId) {

        SavingsGoal goal = savingsGoalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Savings goal not found"
                ));

        savingsGoalRepository.delete(goal);
    }

    @Transactional
    public SavingsContributionResponse addContribution(
            Long userId,
            Long goalId,
            SavingsContributionRequest request
    ) {

        SavingsGoal goal = savingsGoalRepository
                .findByIdAndUserIdForUpdate(goalId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Savings goal not found"
                ));

        BigDecimal newSavedAmount = goal.getSavedAmount()
                .add(request.amount());

        goal.setSavedAmount(newSavedAmount);

        savingsGoalRepository.save(goal);

        SavingsContribution contribution = new SavingsContribution();
        contribution.setSavingsGoal(goal);
        contribution.setAmount(request.amount());

        SavingsContribution savedContribution =
                savingsContributionRepository.saveAndFlush(contribution);

        return new SavingsContributionResponse(
                savedContribution.getId(),
                goal.getId(),
                savedContribution.getAmount(),
                savedContribution.getContributedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<SavingsContributionResponse> getContributions(
            Long userId,
            Long goalId
    ) {

        SavingsGoal goal = savingsGoalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Savings goal not found"
                ));

        return savingsContributionRepository
                .findBySavingsGoalIdOrderByContributedAtDesc(goal.getId())
                .stream()
                .map(contribution -> new SavingsContributionResponse(
                        contribution.getId(),
                        goal.getId(),
                        contribution.getAmount(),
                        contribution.getContributedAt()
                ))
                .toList();
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {

        BigDecimal remainingAmount = goal.getTargetAmount()
                .subtract(goal.getSavedAmount())
                .max(BigDecimal.ZERO);

        BigDecimal progressPercentage = goal.getSavedAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        goal.getTargetAmount(),
                        2,
                        RoundingMode.HALF_UP
                );

        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getSavedAmount(),
                remainingAmount,
                progressPercentage,
                goal.getTargetDate()
        );
    }
}