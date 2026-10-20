
package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.RecurringTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserId(Long userId);

    Optional<RecurringTransaction> findByIdAndUserId(
            Long id,
            Long userId
    );

    List<RecurringTransaction>
    findByActiveTrueAndNextOccurrenceDateLessThanEqual(
            LocalDate date
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM RecurringTransaction r
        WHERE r.id = :recurringTransactionId
        """)
    Optional<RecurringTransaction> findByIdForUpdate(
            @Param("recurringTransactionId") Long recurringTransactionId
    );

}
