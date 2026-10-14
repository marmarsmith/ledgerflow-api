package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.CreateTransactionRequest;
import com.marcel.ledgerflow.dto.TransactionResponse;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.exception.AccountNotFoundException;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import com.marcel.ledgerflow.exception.TransactionNotFoundException;
import com.marcel.ledgerflow.dto.UpdateTransactionRequest;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(
            Long userId,
            Long accountId,
            CreateTransactionRequest request
    ) {

        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description().trim());
        transaction.setTransactionDate(request.transactionDate());

        Transaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return new TransactionResponse(
                savedTransaction.getId(),
                savedTransaction.getAccount().getId(),
                savedTransaction.getType(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription(),
                savedTransaction.getTransactionDate(),
                savedTransaction.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccountId(
            Long userId,
            Long accountId
    ) {

        accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        return transactionRepository
                .findAllByAccountIdOrderByTransactionDateDesc(accountId)
                .stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getAccount().getId(),
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getDescription(),
                        transaction.getTransactionDate(),
                        transaction.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(
            Long userId,
            Long accountId,
            Long transactionId
    ) {

        accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        Transaction transaction = transactionRepository
                .findByIdAndAccountId(transactionId, accountId)
                .orElseThrow(TransactionNotFoundException::new);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }

    @Transactional
    public TransactionResponse updateTransaction(
            Long userId,
            Long accountId,
            Long transactionId,
            UpdateTransactionRequest request
    ) {

        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        Transaction transaction = transactionRepository
                .findByIdAndAccountId(transactionId, accountId)
                .orElseThrow(TransactionNotFoundException::new);

        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description().trim());
        transaction.setTransactionDate(request.transactionDate());

        Transaction updatedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return new TransactionResponse(
                updatedTransaction.getId(),
                updatedTransaction.getAccount().getId(),
                updatedTransaction.getType(),
                updatedTransaction.getAmount(),
                updatedTransaction.getDescription(),
                updatedTransaction.getTransactionDate(),
                updatedTransaction.getCreatedAt()
        );
    }

    @Transactional
    public void deleteTransaction(
            Long userId,
            Long accountId,
            Long transactionId
    ) {

        accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        Transaction transaction = transactionRepository
                .findByIdAndAccountId(transactionId, accountId)
                .orElseThrow(TransactionNotFoundException::new);

        transactionRepository.delete(transaction);
    }
}