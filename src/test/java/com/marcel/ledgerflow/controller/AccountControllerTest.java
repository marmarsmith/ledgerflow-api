package com.marcel.ledgerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcel.ledgerflow.dto.AccountResponse;
import com.marcel.ledgerflow.dto.CreateAccountRequest;
import com.marcel.ledgerflow.entity.AccountType;
import com.marcel.ledgerflow.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.junit.jupiter.api.AfterEach;
import java.util.List;
import com.marcel.ledgerflow.exception.AccountNotFoundException;
import com.marcel.ledgerflow.exception.GlobalExceptionHandler;
import com.marcel.ledgerflow.dto.UpdateAccountRequest;


class AccountControllerTest {

    private MockMvc mockMvc;
    private AccountService accountService;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;


    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);

        AccountController controller =
                new AccountController(accountService);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setValidator(validator)
                .build();


        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }



    @Test
    void shouldReturn201WhenAccountIsCreated() throws Exception {
        // Arrange
        CreateAccountRequest request = new CreateAccountRequest(
                "My Checking Account",
                AccountType.CHECKING,
                new BigDecimal("1500.00")
        );

        AccountResponse response = new AccountResponse(
                10L,
                "My Checking Account",
                AccountType.CHECKING,
                new BigDecimal("1500.00"),
                new BigDecimal("1500.00"),
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(accountService.createAccount(eq(5L), any(CreateAccountRequest.class)))
                .thenReturn(response);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        var authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);


        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("My Checking Account"))
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.initialBalance").value(1500.00))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(accountService).createAccount(eq(5L), any(CreateAccountRequest.class));
    }


    @Test
    void shouldReturn400WhenAccountNameIsBlank() throws Exception {
        // Arrange
        CreateAccountRequest request = new CreateAccountRequest(
                "",
                AccountType.CHECKING,
                new BigDecimal("1500.00")
        );

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }


    @Test
    void shouldReturn400WhenInitialBalanceHasTooManyDecimalPlaces()
            throws Exception {

        // Arrange
        CreateAccountRequest request = new CreateAccountRequest(
                "My Checking Account",
                AccountType.CHECKING,
                new BigDecimal("1500.256")
        );

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }


    @Test
    void shouldReturnAccountsForAuthenticatedUser() throws Exception {
        // Arrange
        Long userId = 5L;

        AccountResponse checkingAccount = new AccountResponse(
                10L,
                "My Checking Account",
                AccountType.CHECKING,
                new BigDecimal("1500.00"),
                new BigDecimal("1500.00"),
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        AccountResponse savingsAccount = new AccountResponse(
                11L,
                "Emergency Savings",
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                new BigDecimal("1500.00"),
                OffsetDateTime.parse("2026-10-13T12:30:00Z")
        );

        when(accountService.getAccountsByUserId(userId))
                .thenReturn(List.of(checkingAccount, savingsAccount));

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("My Checking Account"))
                .andExpect(jsonPath("$[0].type").value("CHECKING"))
                .andExpect(jsonPath("$[0].initialBalance").value(1500.00))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].name").value("Emergency Savings"))
                .andExpect(jsonPath("$[1].type").value("SAVINGS"))
                .andExpect(jsonPath("$[1].initialBalance").value(5000.00));

        verify(accountService).getAccountsByUserId(userId);
    }


    @Test
    void shouldReturnEmptyListWhenUserHasNoAccounts() throws Exception {
        // Arrange
        Long userId = 5L;

        when(accountService.getAccountsByUserId(userId))
                .thenReturn(List.of());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(accountService).getAccountsByUserId(userId);
    }

    @Test
    void shouldReturnAccountWhenItBelongsToAuthenticatedUser() throws Exception {
        // Arrange
        Long accountId = 10L;
        Long userId = 5L;

        AccountResponse response = new AccountResponse(
                accountId,
                "My Checking Account",
                AccountType.CHECKING,
                new BigDecimal("1500.00"),
                new BigDecimal("1500.00"),
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(accountService.getAccountById(accountId, userId))
                .thenReturn(response);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("My Checking Account"))
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.initialBalance").value(1500.00))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(accountService).getAccountById(accountId, userId);
    }

    @Test
    void shouldReturn404WhenAccountDoesNotBelongToAuthenticatedUser()
            throws Exception {

        // Arrange
        Long accountId = 10L;
        Long userId = 8L;

        when(accountService.getAccountById(accountId, userId))
                .thenThrow(new AccountNotFoundException());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "8")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/accounts/{id}", accountId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(accountService).getAccountById(accountId, userId);
    }

    @Test
    void shouldReturn200WhenAccountIsUpdated() throws Exception {
        // Arrange
        Long accountId = 10L;
        Long userId = 5L;

        UpdateAccountRequest request = new UpdateAccountRequest(
                "Emergency Savings",
                AccountType.SAVINGS,
                new BigDecimal("2500.00")
        );

        AccountResponse response = new AccountResponse(
                accountId,
                "Emergency Savings",
                AccountType.SAVINGS,
                new BigDecimal("2500.00"),
                new BigDecimal("1500.00"),
                OffsetDateTime.parse("2026-10-13T12:00:00Z")
        );

        when(accountService.updateAccount(
                eq(accountId),
                eq(userId),
                any(UpdateAccountRequest.class)
        )).thenReturn(response);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Emergency Savings"))
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.initialBalance").value(2500.00))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(accountService).updateAccount(
                eq(accountId),
                eq(userId),
                any(UpdateAccountRequest.class)
        );
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherUsersAccount() throws Exception {
        // Arrange
        Long accountId = 10L;
        Long userId = 8L;

        UpdateAccountRequest request = new UpdateAccountRequest(
                "Unauthorized Update",
                AccountType.CHECKING,
                new BigDecimal("9999.00")
        );

        when(accountService.updateAccount(
                eq(accountId),
                eq(userId),
                any(UpdateAccountRequest.class)
        )).thenThrow(new AccountNotFoundException());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "8")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(accountService).updateAccount(
                eq(accountId),
                eq(userId),
                any(UpdateAccountRequest.class)
        );
    }

    @Test
    void shouldReturn400WhenUpdatingAccountWithInvalidBalance()
            throws Exception {

        // Arrange
        Long accountId = 10L;

        UpdateAccountRequest request = new UpdateAccountRequest(
                "Emergency Savings",
                AccountType.SAVINGS,
                new BigDecimal("2500.555")
        );

        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void shouldReturn204WhenAccountIsDeleted() throws Exception {
        // Arrange
        Long accountId = 10L;
        Long userId = 5L;

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(delete("/api/accounts/{id}", accountId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(accountService).deleteAccount(accountId, userId);
    }

    @Test
    void shouldReturn404WhenDeletingAnotherUsersAccount() throws Exception {
        // Arrange
        Long accountId = 10L;
        Long userId = 8L;

        doThrow(new AccountNotFoundException())
                .when(accountService)
                .deleteAccount(accountId, userId);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "8")
        );

        var authentication =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(delete("/api/accounts/{id}", accountId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(accountService).deleteAccount(accountId, userId);
    }


}
