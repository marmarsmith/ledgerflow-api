package com.marcel.ledgerflow.controller;


import com.marcel.ledgerflow.dto.TransactionResponse;
import com.marcel.ledgerflow.entity.TransactionType;
import com.marcel.ledgerflow.exception.GlobalExceptionHandler;
import com.marcel.ledgerflow.service.TransactionService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.marcel.ledgerflow.exception.TransactionNotFoundException;
import com.marcel.ledgerflow.exception.AccountNotFoundException;

import com.marcel.ledgerflow.dto.UpdateTransactionRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

class TransactionControllerTest {

    private MockMvc mockMvc;
    private TransactionService transactionService;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {

        transactionService = mock(TransactionService.class);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .setValidator(validator)
                .build();

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void shouldCreateTransactionSuccessfully() throws Exception {

        TransactionResponse response = new TransactionResponse(
                101L,
                10L,
                TransactionType.EXPENSE,
                new BigDecimal("85.50"),
                "Weekly groceries",
                LocalDate.of(2026, 10, 14),
                OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
        );

        when(transactionService.createTransaction(eq(5L), eq(10L), any()))
                .thenReturn(response);

        String requestBody = """
                {
                    "type": "EXPENSE",
                    "amount": 85.50,
                    "description": "Weekly groceries",
                    "transactionDate": "2026-10-14"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts/10/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(85.50))
                .andExpect(jsonPath("$.description").value("Weekly groceries"))
                .andExpect(jsonPath("$.transactionDate").value("2026-10-14"));

        verify(transactionService)
                .createTransaction(eq(5L), eq(10L), any());
    }

    @Test
    void shouldRejectTransactionWithNegativeAmount() throws Exception {

        String requestBody = """
                {
                    "type": "EXPENSE",
                    "amount": -50.00,
                    "description": "Invalid transaction",
                    "transactionDate": "2026-10-14"
                }
                """;

        mockMvc.perform(
                        post("/api/accounts/10/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldRejectTransactionWithBlankDescription() throws Exception {

        String requestBody = """
            {
                "type": "EXPENSE",
                "amount": 50.00,
                "description": "   ",
                "transactionDate": "2026-10-14"
            }
            """;

        mockMvc.perform(
                        post("/api/accounts/10/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldRejectTransactionWithTooManyDecimalPlaces() throws Exception {

        String requestBody = """
            {
                "type": "EXPENSE",
                "amount": 85.555,
                "description": "Groceries",
                "transactionDate": "2026-10-14"
            }
            """;

        mockMvc.perform(
                        post("/api/accounts/10/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldRejectTransactionWithMissingDate() throws Exception {

        String requestBody = """
            {
                "type": "INCOME",
                "amount": 2500.00,
                "description": "Paycheck"
            }
            """;

        mockMvc.perform(
                        post("/api/accounts/10/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldReturnTransactionsForAccount() throws Exception {

        TransactionResponse paycheck = new TransactionResponse(
                101L,
                10L,
                TransactionType.INCOME,
                new BigDecimal("2500.00"),
                "Paycheck",
                LocalDate.of(2026, 10, 14),
                OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
        );

        TransactionResponse groceries = new TransactionResponse(
                102L,
                10L,
                TransactionType.EXPENSE,
                new BigDecimal("85.50"),
                "Weekly groceries",
                LocalDate.of(2026, 10, 12),
                OffsetDateTime.parse("2026-10-12T15:00:00-05:00")
        );

        when(transactionService.getTransactionsByAccountId(5L, 10L))
                .thenReturn(List.of(paycheck, groceries));

        mockMvc.perform(
                        get("/api/accounts/10/transactions")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[0].description").value("Paycheck"))
                .andExpect(jsonPath("$[1].id").value(102))
                .andExpect(jsonPath("$[1].type").value("EXPENSE"))
                .andExpect(jsonPath("$[1].description").value("Weekly groceries"));

        verify(transactionService)
                .getTransactionsByAccountId(5L, 10L);
    }

    @Test
    void shouldReturnEmptyListWhenAccountHasNoTransactions() throws Exception {

        when(transactionService.getTransactionsByAccountId(5L, 10L))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/accounts/10/transactions")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService)
                .getTransactionsByAccountId(5L, 10L);
    }

    @Test
    void shouldReturnTransactionById() throws Exception {

        TransactionResponse response = new TransactionResponse(
                101L,
                10L,
                TransactionType.EXPENSE,
                new BigDecimal("85.50"),
                "Weekly groceries",
                LocalDate.of(2026, 10, 14),
                OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
        );

        when(transactionService.getTransactionById(5L, 10L, 101L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/accounts/10/transactions/101")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(85.50))
                .andExpect(jsonPath("$.description").value("Weekly groceries"))
                .andExpect(jsonPath("$.transactionDate").value("2026-10-14"));

        verify(transactionService)
                .getTransactionById(5L, 10L, 101L);
    }

    @Test
    void shouldReturnNotFoundForTransactionInDifferentAccount() throws Exception {

        when(transactionService.getTransactionById(5L, 10L, 999L))
                .thenThrow(new TransactionNotFoundException());

        mockMvc.perform(
                        get("/api/accounts/10/transactions/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Transaction not found"));

        verify(transactionService)
                .getTransactionById(5L, 10L, 999L);
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotOwnAccount() throws Exception {

        Long userId = 5L;
        Long accountId = 99L;
        Long transactionId = 101L;

        when(transactionService.getTransactionById(
                userId,
                accountId,
                transactionId
        )).thenThrow(new AccountNotFoundException());

        mockMvc.perform(
                        get("/api/accounts/99/transactions/101")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Account not found"));

        verify(transactionService)
                .getTransactionById(userId, accountId, transactionId);
    }

    @Test
    void shouldUpdateTransactionSuccessfully() throws Exception {

        TransactionResponse updatedResponse = new TransactionResponse(
                101L,
                10L,
                TransactionType.EXPENSE,
                new BigDecimal("120.75"),
                "Updated grocery shopping",
                LocalDate.of(2026, 10, 15),
                OffsetDateTime.parse("2026-10-14T09:30:00-05:00")
        );

        when(transactionService.updateTransaction(
                eq(5L),
                eq(10L),
                eq(101L),
                any(UpdateTransactionRequest.class)
        )).thenReturn(updatedResponse);

        String requestBody = """
            {
                "type": "EXPENSE",
                "amount": 120.75,
                "description": "Updated grocery shopping",
                "transactionDate": "2026-10-15"
            }
            """;

        mockMvc.perform(
                        put("/api/accounts/10/transactions/101")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(120.75))
                .andExpect(jsonPath("$.description").value("Updated grocery shopping"))
                .andExpect(jsonPath("$.transactionDate").value("2026-10-15"));

        verify(transactionService).updateTransaction(
                eq(5L),
                eq(10L),
                eq(101L),
                any(UpdateTransactionRequest.class)
        );
    }

    @Test
    void shouldRejectUpdateWithZeroAmount() throws Exception {

        String requestBody = """
            {
                "type": "EXPENSE",
                "amount": 0.00,
                "description": "Groceries",
                "transactionDate": "2026-10-15"
            }
            """;

        mockMvc.perform(
                        put("/api/accounts/10/transactions/101")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldRejectUpdateWithMissingType() throws Exception {

        String requestBody = """
            {
                "amount": 120.75,
                "description": "Groceries",
                "transactionDate": "2026-10-15"
            }
            """;

        mockMvc.perform(
                        put("/api/accounts/10/transactions/101")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonexistentTransaction() throws Exception {

        String requestBody = """
            {
                "type": "EXPENSE",
                "amount": 120.75,
                "description": "Updated groceries",
                "transactionDate": "2026-10-15"
            }
            """;

        when(transactionService.updateTransaction(
                eq(5L),
                eq(10L),
                eq(999L),
                any(UpdateTransactionRequest.class)
        )).thenThrow(new TransactionNotFoundException());

        mockMvc.perform(
                        put("/api/accounts/10/transactions/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Transaction not found"));

        verify(transactionService).updateTransaction(
                eq(5L),
                eq(10L),
                eq(999L),
                any(UpdateTransactionRequest.class)
        );
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingAnotherUsersAccount() throws Exception {

        String requestBody = """
            {
                "type": "EXPENSE",
                "amount": 120.75,
                "description": "Updated groceries",
                "transactionDate": "2026-10-15"
            }
            """;

        when(transactionService.updateTransaction(
                eq(5L),
                eq(99L),
                eq(101L),
                any(UpdateTransactionRequest.class)
        )).thenThrow(new AccountNotFoundException());

        mockMvc.perform(
                        put("/api/accounts/99/transactions/101")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Account not found"));

        verify(transactionService).updateTransaction(
                eq(5L),
                eq(99L),
                eq(101L),
                any(UpdateTransactionRequest.class)
        );
    }

    @Test
    void shouldDeleteTransactionSuccessfully() throws Exception {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 101L;

        mockMvc.perform(
                        delete("/api/accounts/10/transactions/101")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(transactionService).deleteTransaction(
                userId,
                accountId,
                transactionId
        );
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonexistentTransaction() throws Exception {

        Long userId = 5L;
        Long accountId = 10L;
        Long transactionId = 999L;

        doThrow(new TransactionNotFoundException())
                .when(transactionService)
                .deleteTransaction(userId, accountId, transactionId);

        mockMvc.perform(
                        delete("/api/accounts/10/transactions/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Transaction not found"));

        verify(transactionService)
                .deleteTransaction(userId, accountId, transactionId);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingAnotherUsersTransaction() throws Exception {

        Long userId = 5L;
        Long accountId = 99L;
        Long transactionId = 101L;

        doThrow(new AccountNotFoundException())
                .when(transactionService)
                .deleteTransaction(userId, accountId, transactionId);

        mockMvc.perform(
                        delete("/api/accounts/99/transactions/101")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Account not found"));

        verify(transactionService)
                .deleteTransaction(userId, accountId, transactionId);
    }


}