package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserId(Long userId);

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT g
        FROM SavingsGoal g
        WHERE g.id = :goalId
          AND g.user.id = :userId
        """)
    Optional<SavingsGoal> findByIdAndUserIdForUpdate(
            @Param("goalId") Long goalId,
            @Param("userId") Long userId
    );
}