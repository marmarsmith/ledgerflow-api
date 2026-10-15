package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.SavingsContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsContributionRepository
        extends JpaRepository<SavingsContribution, Long> {

    List<SavingsContribution> findBySavingsGoalIdOrderByContributedAtDesc(
            Long savingsGoalId
    );
}