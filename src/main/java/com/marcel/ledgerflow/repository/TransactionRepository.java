package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
}