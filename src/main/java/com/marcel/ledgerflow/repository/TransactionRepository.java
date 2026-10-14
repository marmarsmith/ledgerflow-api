package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByAccountIdOrderByTransactionDateDesc(Long accountId);

    Optional<Transaction> findByIdAndAccountId(Long transactionId, Long accountId);

}