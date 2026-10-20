
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionProcessorServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private RecurringTransactionProcessorService processorService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 11, 15)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        ReflectionTestUtils.setField(
                processorService,
                "clock",
                fixedClock
        );
    }

    @Test
    void processRecurringTransaction_createsTransactionAndAdvancesSchedule() {

        Long recurringTransactionId = 1L;
        LocalDate occurrenceDate = LocalDate.of(2026, 11, 1);

        Account account = new Account();

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setAccount(account);
        recurringTransaction.setAmount(new BigDecimal("60.00"));
        recurringTransaction.setDescription("Gym membership");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(occurrenceDate);
        recurringTransaction.setNextOccurrenceDate(occurrenceDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdForUpdate(
                recurringTransactionId
        )).thenReturn(Optional.of(recurringTransaction));

        when(transactionRepository
                .existsByRecurringTransactionIdAndTransactionDate(
                        recurringTransactionId,
                        occurrenceDate
                )).thenReturn(false);

        processorService.processRecurringTransaction(
                recurringTransactionId
        );

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(
                transactionCaptor.capture()
        );

        Transaction savedTransaction =
                transactionCaptor.getValue();

        assertEquals(
                new BigDecimal("60.00"),
                savedTransaction.getAmount()
        );

        assertEquals(
                "Gym membership",
                savedTransaction.getDescription()
        );

        assertEquals(
                TransactionType.EXPENSE,
                savedTransaction.getType()
        );

        assertEquals(
                occurrenceDate,
                savedTransaction.getTransactionDate()
        );

        assertSame(
                recurringTransaction,
                savedTransaction.getRecurringTransaction()
        );

        assertEquals(
                LocalDate.of(2026, 12, 1),
                recurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(recurringTransaction.isActive());

        verify(recurringTransactionRepository).save(
                recurringTransaction
        );
    }

    @Test
    void processRecurringTransaction_skipsDuplicateAndAdvancesSchedule() {

        Long recurringTransactionId = 1L;
        LocalDate occurrenceDate = LocalDate.of(2026, 11, 1);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(occurrenceDate);
        recurringTransaction.setNextOccurrenceDate(occurrenceDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdForUpdate(
                recurringTransactionId
        )).thenReturn(Optional.of(recurringTransaction));

        when(transactionRepository
                .existsByRecurringTransactionIdAndTransactionDate(
                        recurringTransactionId,
                        occurrenceDate
                )).thenReturn(true);

        processorService.processRecurringTransaction(
                recurringTransactionId
        );

        verify(transactionRepository, never()).save(
                any(Transaction.class)
        );

        assertEquals(
                LocalDate.of(2026, 12, 1),
                recurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(recurringTransaction.isActive());

        verify(recurringTransactionRepository).save(
                recurringTransaction
        );
    }

    @Test
    void processRecurringTransaction_doesNotProcessPausedSchedule() {

        Long recurringTransactionId = 1L;
        LocalDate occurrenceDate = LocalDate.of(2026, 11, 1);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(occurrenceDate);
        recurringTransaction.setNextOccurrenceDate(occurrenceDate);
        recurringTransaction.setActive(false);

        when(recurringTransactionRepository.findByIdForUpdate(
                recurringTransactionId
        )).thenReturn(Optional.of(recurringTransaction));

        processorService.processRecurringTransaction(
                recurringTransactionId
        );

        verifyNoInteractions(transactionRepository);

        verify(recurringTransactionRepository, never()).save(
                any(RecurringTransaction.class)
        );

        assertEquals(
                occurrenceDate,
                recurringTransaction.getNextOccurrenceDate()
        );

        assertFalse(recurringTransaction.isActive());
    }

    @Test
    void processRecurringTransaction_doesNotProcessFutureSchedule() {

        Long recurringTransactionId = 1L;
        LocalDate futureDate = LocalDate.of(2026, 12, 1);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(futureDate);
        recurringTransaction.setNextOccurrenceDate(futureDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdForUpdate(
                recurringTransactionId
        )).thenReturn(Optional.of(recurringTransaction));

        processorService.processRecurringTransaction(
                recurringTransactionId
        );

        verifyNoInteractions(transactionRepository);

        verify(recurringTransactionRepository, never()).save(
                any(RecurringTransaction.class)
        );

        assertEquals(
                futureDate,
                recurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(recurringTransaction.isActive());
    }

    @Test
    void processRecurringTransaction_doesNotProcessAfterEndDate() {

        Long recurringTransactionId = 1L;

        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate occurrenceDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setFrequency(RecurringFrequency.MONTHLY);
        recurringTransaction.setStartDate(startDate);
        recurringTransaction.setNextOccurrenceDate(occurrenceDate);
        recurringTransaction.setEndDate(endDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdForUpdate(
                recurringTransactionId
        )).thenReturn(Optional.of(recurringTransaction));

        processorService.processRecurringTransaction(
                recurringTransactionId
        );

        verifyNoInteractions(transactionRepository);

        verify(recurringTransactionRepository).save(
                recurringTransaction
        );

        assertEquals(
                occurrenceDate,
                recurringTransaction.getNextOccurrenceDate()
        );

        assertFalse(recurringTransaction.isActive());
    }

    @Test
    void processRecurringTransaction_catchesUpMissedDailyOccurrences() {

        Long recurringTransactionId = 1L;
        LocalDate startDate = LocalDate.of(2026, 11, 11);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setAccount(new Account());
        recurringTransaction.setAmount(new BigDecimal("10.00"));
        recurringTransaction.setDescription("Daily savings");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.DAILY);
        recurringTransaction.setStartDate(startDate);
        recurringTransaction.setNextOccurrenceDate(startDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdForUpdate(
                recurringTransactionId
        )).thenReturn(Optional.of(recurringTransaction));

        processorService.processRecurringTransaction(
                recurringTransactionId
        );

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(5)).save(
                transactionCaptor.capture()
        );

        assertEquals(
                java.util.List.of(
                        LocalDate.of(2026, 11, 11),
                        LocalDate.of(2026, 11, 12),
                        LocalDate.of(2026, 11, 13),
                        LocalDate.of(2026, 11, 14),
                        LocalDate.of(2026, 11, 15)
                ),
                transactionCaptor.getAllValues()
                        .stream()
                        .map(Transaction::getTransactionDate)
                        .toList()
        );

        assertEquals(
                LocalDate.of(2026, 11, 16),
                recurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(recurringTransaction.isActive());

        verify(recurringTransactionRepository, times(5)).save(
                recurringTransaction
        );
    }

    @Test
    void processRecurringTransaction_stopsAfterOneHundredOccurrences() {

        Long recurringTransactionId = 1L;
        LocalDate startDate = LocalDate.of(2026, 1, 1);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setAccount(new Account());
        recurringTransaction.setAmount(new BigDecimal("10.00"));
        recurringTransaction.setDescription("Daily savings");
        recurringTransaction.setType(TransactionType.EXPENSE);
        recurringTransaction.setFrequency(RecurringFrequency.DAILY);
        recurringTransaction.setStartDate(startDate);
        recurringTransaction.setNextOccurrenceDate(startDate);
        recurringTransaction.setActive(true);

        when(recurringTransactionRepository.findByIdForUpdate(
                recurringTransactionId
        )).thenReturn(Optional.of(recurringTransaction));

        processorService.processRecurringTransaction(
                recurringTransactionId
        );

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(100)).save(
                transactionCaptor.capture()
        );

        assertEquals(
                100,
                transactionCaptor.getAllValues().size()
        );

        assertEquals(
                LocalDate.of(2026, 1, 1),
                transactionCaptor.getAllValues()
                        .get(0)
                        .getTransactionDate()
        );

        assertEquals(
                LocalDate.of(2026, 4, 10),
                transactionCaptor.getAllValues()
                        .get(99)
                        .getTransactionDate()
        );

        assertEquals(
                LocalDate.of(2026, 4, 11),
                recurringTransaction.getNextOccurrenceDate()
        );

        assertTrue(recurringTransaction.isActive());

        verify(recurringTransactionRepository, times(100)).save(
                recurringTransaction
        );
    }





}
