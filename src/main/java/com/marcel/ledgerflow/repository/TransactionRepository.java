package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByAccountIdOrderByTransactionDateDesc(Long accountId);

    Optional<Transaction> findByIdAndAccountId(Long transactionId, Long accountId);

    @Query("""
            SELECT COALESCE(
                SUM(
                    CASE
                        WHEN t.type = :incomeType THEN t.amount
                        ELSE -t.amount
                    END
                ),
                0
            )
            FROM Transaction t
            WHERE t.account.id = :accountId
            """)
    BigDecimal calculateNetTransactionAmount(
            @Param("accountId") Long accountId,
            @Param("incomeType") TransactionType incomeType
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.account.user.id = :userId
          AND t.type = :type
          AND t.transactionDate >= :startDate
          AND t.transactionDate < :endDate
        """)
    BigDecimal calculateTotalByUserAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT
            c.id AS categoryId,
            COALESCE(c.name, 'Uncategorized') AS categoryName,
            SUM(t.amount) AS amount
        FROM Transaction t
        LEFT JOIN t.category c
        WHERE t.account.user.id = :userId
          AND t.type = :type
          AND t.transactionDate >= :startDate
          AND t.transactionDate < :endDate
        GROUP BY c.id, c.name
        ORDER BY SUM(t.amount) DESC
        """)
    List<CategoryExpenseProjection> calculateExpensesByCategory(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM Transaction t
    WHERE t.account.user.id = :userId
      AND t.category.id = :categoryId
      AND t.type = :type
      AND t.transactionDate >= :startDate
      AND t.transactionDate < :endDate
    """)
    BigDecimal calculateCategorySpending(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}