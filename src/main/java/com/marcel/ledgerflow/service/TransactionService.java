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

import com.marcel.ledgerflow.repository.CategoryRepository;

import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.exception.CategoryNotFoundException;
import com.marcel.ledgerflow.exception.CategoryTypeMismatchException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository) {

        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
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

        if (request.categoryId() != null) {

            Category category = categoryRepository
                    .findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(CategoryNotFoundException::new);

            if (category.getType() != request.type()) {
                throw new CategoryTypeMismatchException();
            }

            transaction.setCategory(category);
        }

        Transaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getCategory() != null
                        ? transaction.getCategory().getId()
                        : null
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
                        transaction.getCreatedAt(),
                        transaction.getCategory() != null
                            ? transaction.getCategory().getId()
                                : null
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
                transaction.getCreatedAt(),
                transaction.getCategory() != null
                        ? transaction.getCategory().getId()
                        : null
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

        // Validate category before modifying the transaction
        Category category = null;

        if (request.categoryId() != null) {

            category = categoryRepository
                    .findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(CategoryNotFoundException::new);

            if (category.getType() != request.type()) {
                throw new CategoryTypeMismatchException();
            }
        }

        // Apply changes after validation succeeds
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setCategory(category);

        Transaction updatedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return new TransactionResponse(
                updatedTransaction.getId(),
                updatedTransaction.getAccount().getId(),
                updatedTransaction.getType(),
                updatedTransaction.getAmount(),
                updatedTransaction.getDescription(),
                updatedTransaction.getTransactionDate(),
                updatedTransaction.getCreatedAt(),
                updatedTransaction.getCategory() != null
                        ? updatedTransaction.getCategory().getId()
                        : null
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