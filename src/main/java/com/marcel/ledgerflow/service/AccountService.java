
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.AccountResponse;
import com.marcel.ledgerflow.dto.CreateAccountRequest;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.marcel.ledgerflow.exception.AccountNotFoundException;
import com.marcel.ledgerflow.dto.UpdateAccountRequest;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.repository.TransactionRepository;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse createAccount(
            Long userId,
            CreateAccountRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authenticated user not found"
                ));

        Account account = new Account();
        account.setUser(user);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setInitialBalance(request.initialBalance());

        Account savedAccount = accountRepository.saveAndFlush(account);

        return toAccountResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(Long userId) {

        return accountRepository.findAllByUserId(userId)
                .stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId, Long userId) {

        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        return toAccountResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(
            Long accountId,
            Long userId,
            UpdateAccountRequest request
    ) {
        // Find the account and verify ownership
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        // Update editable fields
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setInitialBalance(request.initialBalance());

        // Persist changes
        Account updatedAccount = accountRepository.saveAndFlush(account);

        // Return the updated account
        return toAccountResponse(updatedAccount);
    }

    @Transactional
    public void deleteAccount(Long accountId, Long userId) {

        // Find the account and verify ownership
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        // Delete the account
        accountRepository.delete(account);
    }

    private AccountResponse toAccountResponse(Account account) {

        BigDecimal netTransactionAmount =
                transactionRepository.calculateNetTransactionAmount(
                        account.getId(),
                        TransactionType.INCOME
                );

        BigDecimal currentBalance = account.getInitialBalance()
                .add(netTransactionAmount);

        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getInitialBalance(),
                currentBalance,
                account.getCreatedAt()
        );
    }

}
