
package com.marcel.ledgerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marcel.ledgerflow.dto.RecurringTransactionRequest;
import com.marcel.ledgerflow.dto.RecurringTransactionResponse;
import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.exception.RecurringTransactionNotFoundException;
import com.marcel.ledgerflow.service.RecurringTransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionControllerTest {

    @Mock
    private RecurringTransactionService recurringTransactionService;

    @InjectMocks
    private RecurringTransactionController recurringTransactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(recurringTransactionController)
                .setCustomArgumentResolvers(
                        new org.springframework.web.method.support.HandlerMethodArgumentResolver() {

                            @Override
                            public boolean supportsParameter(
                                    org.springframework.core.MethodParameter parameter
                            ) {
                                return parameter.hasParameterAnnotation(
                                        org.springframework.security.core.annotation.AuthenticationPrincipal.class
                                );
                            }

                            @Override
                            public Object resolveArgument(
                                    org.springframework.core.MethodParameter parameter,
                                    org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                    org.springframework.web.context.request.NativeWebRequest webRequest,
                                    org.springframework.web.bind.support.WebDataBinderFactory binderFactory
                            ) {
                                return Jwt.withTokenValue("test-token")
                                        .header("alg", "none")
                                        .subject("1")
                                        .build();
                            }
                        }
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void createRecurringTransaction_shouldReturnCreated() throws Exception {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        1L,
                        null,
                        new BigDecimal("50.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        RecurringTransactionResponse response =
                new RecurringTransactionResponse(
                        1L,
                        1L,
                        null,
                        new BigDecimal("50.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 1),
                        null,
                        true
                );

        when(recurringTransactionService.createRecurringTransaction(
                eq(1L),
                eq(request)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/recurring-transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.description").value("Gym membership"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.active").value(true));

        verify(recurringTransactionService)
                .createRecurringTransaction(1L, request);
    }


    @Test
    void createRecurringTransaction_shouldReturnBadRequestForNegativeAmount()
            throws Exception {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        1L,
                        null,
                        new BigDecimal("-50.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        mockMvc.perform(
                        post("/api/recurring-transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

        verify(recurringTransactionService, never())
                .createRecurringTransaction(
                        any(),
                        any(RecurringTransactionRequest.class)
                );
    }



    @Test
    void getRecurringTransactions_shouldReturnAllRecurringTransactions()
            throws Exception {

        RecurringTransactionResponse gymMembership =
                new RecurringTransactionResponse(
                        1L,
                        1L,
                        null,
                        new BigDecimal("50.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 12, 1),
                        null,
                        true
                );

        RecurringTransactionResponse streamingSubscription =
                new RecurringTransactionResponse(
                        2L,
                        1L,
                        null,
                        new BigDecimal("15.99"),
                        "Streaming subscription",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 5),
                        LocalDate.of(2026, 12, 5),
                        null,
                        false
                );

        when(recurringTransactionService.getRecurringTransactions(1L))
                .thenReturn(List.of(
                        gymMembership,
                        streamingSubscription
                ));

        mockMvc.perform(
                        get("/api/recurring-transactions")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Gym membership"))
                .andExpect(jsonPath("$[0].amount").value(50.00))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].description").value("Streaming subscription"))
                .andExpect(jsonPath("$[1].amount").value(15.99))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(recurringTransactionService)
                .getRecurringTransactions(1L);
    }


    @Test
    void getRecurringTransactions_shouldReturnEmptyList()
            throws Exception {

        when(recurringTransactionService.getRecurringTransactions(1L))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/recurring-transactions")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(recurringTransactionService)
                .getRecurringTransactions(1L);
    }


    @Test
    void getRecurringTransactionById_shouldReturnRecurringTransaction()
            throws Exception {

        Long recurringTransactionId = 5L;

        RecurringTransactionResponse response =
                new RecurringTransactionResponse(
                        recurringTransactionId,
                        1L,
                        null,
                        new BigDecimal("75.00"),
                        "Internet bill",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 12, 10),
                        null,
                        true
                );

        when(recurringTransactionService.getRecurringTransactionById(
                1L,
                recurringTransactionId
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/recurring-transactions/{recurringTransactionId}",
                                recurringTransactionId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.amount").value(75.00))
                .andExpect(jsonPath("$.description").value("Internet bill"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.active").value(true));

        verify(recurringTransactionService)
                .getRecurringTransactionById(
                        1L,
                        recurringTransactionId
                );
    }


    @Test
    void getRecurringTransactionById_shouldReturnNotFound()
            throws Exception {

        Long recurringTransactionId = 999L;

        when(recurringTransactionService.getRecurringTransactionById(
                1L,
                recurringTransactionId
        )).thenThrow(new RecurringTransactionNotFoundException());

        mockMvc.perform(
                        get("/api/recurring-transactions/{recurringTransactionId}",
                                recurringTransactionId)
                )
                .andExpect(status().isNotFound());

        verify(recurringTransactionService)
                .getRecurringTransactionById(
                        1L,
                        recurringTransactionId
                );
    }


    @Test
    void updateRecurringTransaction_shouldReturnUpdatedRecurringTransaction()
            throws Exception {

        Long recurringTransactionId = 5L;

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        1L,
                        null,
                        new BigDecimal("60.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        RecurringTransactionResponse response =
                new RecurringTransactionResponse(
                        recurringTransactionId,
                        1L,
                        null,
                        new BigDecimal("60.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 12, 1),
                        null,
                        true
                );

        when(recurringTransactionService.updateRecurringTransaction(
                eq(1L),
                eq(recurringTransactionId),
                eq(request)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/recurring-transactions/{recurringTransactionId}",
                                recurringTransactionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.amount").value(60.00))
                .andExpect(jsonPath("$.description").value("Gym membership"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.active").value(true));

        verify(recurringTransactionService)
                .updateRecurringTransaction(
                        1L,
                        recurringTransactionId,
                        request
                );
    }


    @Test
    void updateRecurringTransaction_shouldReturnBadRequestForNegativeAmount()
            throws Exception {

        Long recurringTransactionId = 5L;

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        1L,
                        null,
                        new BigDecimal("-60.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        null
                );

        mockMvc.perform(
                        put("/api/recurring-transactions/{recurringTransactionId}",
                                recurringTransactionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

        verify(recurringTransactionService, never())
                .updateRecurringTransaction(
                        any(),
                        any(),
                        any(RecurringTransactionRequest.class)
                );
    }


    @Test
    void pauseRecurringTransaction_shouldReturnPausedRecurringTransaction()
            throws Exception {

        Long recurringTransactionId = 5L;

        RecurringTransactionResponse response =
                new RecurringTransactionResponse(
                        recurringTransactionId,
                        1L,
                        null,
                        new BigDecimal("60.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 12, 1),
                        null,
                        false
                );

        when(recurringTransactionService.pauseRecurringTransaction(
                1L,
                recurringTransactionId
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/recurring-transactions/{recurringTransactionId}/pause",
                                recurringTransactionId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.amount").value(60.00))
                .andExpect(jsonPath("$.description").value("Gym membership"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.nextOccurrenceDate").value("2026-12-01"))
                .andExpect(jsonPath("$.active").value(false));

        verify(recurringTransactionService)
                .pauseRecurringTransaction(
                        1L,
                        recurringTransactionId
                );
    }



    @Test
    void resumeRecurringTransaction_shouldReturnActiveRecurringTransaction()
            throws Exception {

        Long recurringTransactionId = 5L;

        RecurringTransactionResponse response =
                new RecurringTransactionResponse(
                        recurringTransactionId,
                        1L,
                        null,
                        new BigDecimal("60.00"),
                        "Gym membership",
                        TransactionType.EXPENSE,
                        RecurringFrequency.MONTHLY,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2027, 2, 1),
                        null,
                        true
                );

        when(recurringTransactionService.resumeRecurringTransaction(
                1L,
                recurringTransactionId
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/recurring-transactions/{recurringTransactionId}/resume",
                                recurringTransactionId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.amount").value(60.00))
                .andExpect(jsonPath("$.description").value("Gym membership"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.nextOccurrenceDate").value("2027-02-01"))
                .andExpect(jsonPath("$.active").value(true));

        verify(recurringTransactionService)
                .resumeRecurringTransaction(
                        1L,
                        recurringTransactionId
                );
    }



    @Test
    void deleteRecurringTransaction_shouldReturnNoContent()
            throws Exception {

        Long recurringTransactionId = 5L;

        mockMvc.perform(
                        delete("/api/recurring-transactions/{recurringTransactionId}",
                                recurringTransactionId)
                )
                .andExpect(status().isNoContent())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .content().string("")
                );

        verify(recurringTransactionService)
                .deleteRecurringTransaction(
                        1L,
                        recurringTransactionId
                );
    }




}
