
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

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
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

        return new AccountResponse(
                savedAccount.getId(),
                savedAccount.getName(),
                savedAccount.getType(),
                savedAccount.getInitialBalance(),
                savedAccount.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(Long userId) {

        return accountRepository.findAllByUserId(userId)
                .stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getName(),
                        account.getType(),
                        account.getInitialBalance(),
                        account.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId, Long userId) {

        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getInitialBalance(),
                account.getCreatedAt()
        );
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
        return new AccountResponse(
                updatedAccount.getId(),
                updatedAccount.getName(),
                updatedAccount.getType(),
                updatedAccount.getInitialBalance(),
                updatedAccount.getCreatedAt()
        );
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


}
