
package com.marcel.ledgerflow.scheduler;

import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.service.RecurringTransactionProcessorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionSchedulerTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private RecurringTransactionProcessorService processorService;

    @Mock
    private Clock clock;

    @InjectMocks
    private RecurringTransactionScheduler scheduler;

    @BeforeEach
    void setUp() {

        Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 11, 15)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        ReflectionTestUtils.setField(
                scheduler,
                "clock",
                fixedClock
        );
    }

    @Test
    void processDueRecurringTransactions_processesAllDueSchedules() {

        LocalDate today = LocalDate.of(2026, 11, 15);

        RecurringTransaction firstRecurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                firstRecurringTransaction,
                "id",
                1L
        );

        RecurringTransaction secondRecurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                secondRecurringTransaction,
                "id",
                2L
        );

        when(recurringTransactionRepository
                .findByActiveTrueAndNextOccurrenceDateLessThanEqual(today))
                .thenReturn(List.of(
                        firstRecurringTransaction,
                        secondRecurringTransaction
                ));

        scheduler.processDueRecurringTransactions();

        verify(recurringTransactionRepository)
                .findByActiveTrueAndNextOccurrenceDateLessThanEqual(today);

        verify(processorService).processRecurringTransaction(1L);

        verify(processorService).processRecurringTransaction(2L);

        verifyNoMoreInteractions(processorService);
    }

    @Test
    void processDueRecurringTransactions_continuesWhenOneScheduleFails() {

        LocalDate today = LocalDate.of(2026, 11, 15);

        RecurringTransaction firstRecurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                firstRecurringTransaction,
                "id",
                1L
        );

        RecurringTransaction secondRecurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                secondRecurringTransaction,
                "id",
                2L
        );

        RecurringTransaction thirdRecurringTransaction =
                new RecurringTransaction();

        ReflectionTestUtils.setField(
                thirdRecurringTransaction,
                "id",
                3L
        );

        when(recurringTransactionRepository
                .findByActiveTrueAndNextOccurrenceDateLessThanEqual(today))
                .thenReturn(List.of(
                        firstRecurringTransaction,
                        secondRecurringTransaction,
                        thirdRecurringTransaction
                ));

        doThrow(new RuntimeException("Database processing failed"))
                .when(processorService)
                .processRecurringTransaction(2L);

        scheduler.processDueRecurringTransactions();

        verify(processorService).processRecurringTransaction(1L);

        verify(processorService).processRecurringTransaction(2L);

        verify(processorService).processRecurringTransaction(3L);

        verifyNoMoreInteractions(processorService);
    }

}
