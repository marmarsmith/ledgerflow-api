
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.AccountResponse;
import com.marcel.ledgerflow.dto.CreateAccountRequest;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.AccountType;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;

import com.marcel.ledgerflow.exception.AccountNotFoundException;

import com.marcel.ledgerflow.dto.UpdateAccountRequest;
import org.mockito.ArgumentCaptor;



@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountForAuthenticatedUser() {
        // Arrange
        Long userId = 5L;

        CreateAccountRequest request = new CreateAccountRequest(
                "  My Checking Account  ",
                AccountType.CHECKING,
                new BigDecimal("1500.00")
        );

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(invocation -> {
                    Account account = invocation.getArgument(0);
                    account.setId(10L);
                    account.setCreatedAt(
                            OffsetDateTime.parse("2026-10-13T12:00:00Z")
                    );
                    return account;
                });

        // Act
        AccountResponse response =
                accountService.createAccount(userId, request);

        // Assert
        assertEquals(10L, response.id());
        assertEquals("My Checking Account", response.name());
        assertEquals(AccountType.CHECKING, response.type());
        assertEquals(
                new BigDecimal("1500.00"),
                response.initialBalance()
        );
        assertNotNull(response.createdAt());

        verify(userRepository).findById(userId);

        verify(accountRepository).saveAndFlush(argThat(account ->
                account.getUser().getId().equals(userId)
                        && account.getName().equals("My Checking Account")
                        && account.getType() == AccountType.CHECKING
                        && account.getInitialBalance()
                        .compareTo(new BigDecimal("1500.00")) == 0
        ));
    }


    @Test
    void shouldNotCreateAccountWhenUserDoesNotExist() {
        // Arrange
        Long userId = 999L;

        CreateAccountRequest request = new CreateAccountRequest(
                "My Savings",
                AccountType.SAVINGS,
                new BigDecimal("500.00")
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(userId, request)
        );

        assertEquals(
                "Authenticated user not found",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);
        verifyNoInteractions(accountRepository);
    }


    @Test
    void shouldReturnAccountsBelongingToUser() {
        // Arrange
        Long userId = 5L;

        Account checkingAccount = new Account();
        checkingAccount.setId(10L);
        checkingAccount.setName("My Checking Account");
        checkingAccount.setType(AccountType.CHECKING);
        checkingAccount.setInitialBalance(new BigDecimal("1500.00"));

        Account savingsAccount = new Account();
        savingsAccount.setId(11L);
        savingsAccount.setName("Emergency Savings");
        savingsAccount.setType(AccountType.SAVINGS);
        savingsAccount.setInitialBalance(new BigDecimal("5000.00"));

        when(accountRepository.findAllByUserId(userId))
                .thenReturn(List.of(checkingAccount, savingsAccount));

        // Act
        List<AccountResponse> responses =
                accountService.getAccountsByUserId(userId);

        // Assert
        assertEquals(2, responses.size());

        assertEquals(10L, responses.get(0).id());
        assertEquals("My Checking Account", responses.get(0).name());
        assertEquals(AccountType.CHECKING, responses.get(0).type());
        assertEquals(new BigDecimal("1500.00"),
                responses.get(0).initialBalance());

        assertEquals(11L, responses.get(1).id());
        assertEquals("Emergency Savings", responses.get(1).name());
        assertEquals(AccountType.SAVINGS, responses.get(1).type());
        assertEquals(new BigDecimal("5000.00"),
                responses.get(1).initialBalance());

        verify(accountRepository).findAllByUserId(userId);
        verifyNoInteractions(userRepository);
    }


    @Test
    void shouldReturnAccountWhenItBelongsToUser() {
        // Arrange
        Long accountId = 10L;
        Long userId = 5L;

        Account account = new Account();
        account.setId(accountId);
        account.setName("My Checking Account");
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(new BigDecimal("1500.00"));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        // Act
        AccountResponse response =
                accountService.getAccountById(accountId, userId);

        // Assert
        assertEquals(accountId, response.id());
        assertEquals("My Checking Account", response.name());
        assertEquals(AccountType.CHECKING, response.type());
        assertEquals(new BigDecimal("1500.00"), response.initialBalance());

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verifyNoInteractions(userRepository);
    }


    @Test
    void shouldThrowAccountNotFoundWhenAccountDoesNotBelongToUser() {
        // Arrange
        Long accountId = 10L;
        Long userId = 8L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getAccountById(accountId, userId)
        );

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldUpdateAccountWhenItBelongsToUser() {
        // Arrange
        Long accountId = 10L;
        Long userId = 5L;

        Account account = new Account();
        account.setId(accountId);
        account.setName("Old Checking Account");
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(new BigDecimal("1000.00"));

        UpdateAccountRequest request = new UpdateAccountRequest(
                "  Emergency Savings  ",
                AccountType.SAVINGS,
                new BigDecimal("2500.00")
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountResponse response =
                accountService.updateAccount(accountId, userId, request);

        // Assert
        assertEquals(accountId, response.id());
        assertEquals("Emergency Savings", response.name());
        assertEquals(AccountType.SAVINGS, response.type());
        assertEquals(new BigDecimal("2500.00"), response.initialBalance());

        ArgumentCaptor<Account> accountCaptor =
                ArgumentCaptor.forClass(Account.class);

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verify(accountRepository).saveAndFlush(accountCaptor.capture());

        Account savedAccount = accountCaptor.getValue();

        assertEquals("Emergency Savings", savedAccount.getName());
        assertEquals(AccountType.SAVINGS, savedAccount.getType());
        assertEquals(new BigDecimal("2500.00"), savedAccount.getInitialBalance());
    }

    @Test
    void shouldThrowAccountNotFoundWhenUpdatingAnotherUsersAccount() {
        // Arrange
        Long accountId = 10L;
        Long userId = 8L;

        UpdateAccountRequest request = new UpdateAccountRequest(
                "Unauthorized Update",
                AccountType.CHECKING,
                new BigDecimal("9999.00")
        );

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.updateAccount(accountId, userId, request)
        );

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verify(accountRepository, never()).saveAndFlush(any(Account.class));
    }

    @Test
    void shouldDeleteAccountWhenItBelongsToUser() {
        // Arrange
        Long accountId = 10L;
        Long userId = 5L;

        Account account = new Account();
        account.setId(accountId);
        account.setName("My Checking Account");
        account.setType(AccountType.CHECKING);
        account.setInitialBalance(new BigDecimal("1500.00"));

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(account));

        // Act
        accountService.deleteAccount(accountId, userId);

        // Assert
        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verify(accountRepository).delete(account);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowAccountNotFoundWhenDeletingAnotherUsersAccount() {
        // Arrange
        Long accountId = 10L;
        Long userId = 8L;

        when(accountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.deleteAccount(accountId, userId)
        );

        verify(accountRepository).findByIdAndUserId(accountId, userId);
        verify(accountRepository, never()).delete(any(Account.class));
        verifyNoInteractions(userRepository);
    }


}
