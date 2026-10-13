
package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.AccountResponse;
import com.marcel.ledgerflow.dto.CreateAccountRequest;
import com.marcel.ledgerflow.dto.UpdateAccountRequest;
import com.marcel.ledgerflow.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAccountRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        AccountResponse response =
                accountService.createAccount(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        List<AccountResponse> accounts =
                accountService.getAccountsByUserId(userId);

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        AccountResponse account =
                accountService.getAccountById(id, userId);

        return ResponseEntity.ok(account);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        AccountResponse updatedAccount =
                accountService.updateAccount(id, userId, request);

        return ResponseEntity.ok(updatedAccount);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        accountService.deleteAccount(id, userId);

        return ResponseEntity.noContent().build();
    }
}