package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndYearAndMonth(
            Long userId,
            Integer year,
            Integer month
    );

    Optional<Budget> findByIdAndUserId(
            Long budgetId,
            Long userId
    );

    boolean existsByUserIdAndCategoryIdAndYearAndMonth(
            Long userId,
            Long categoryId,
            Integer year,
            Integer month
    );
}