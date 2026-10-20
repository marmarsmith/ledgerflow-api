
package com.marcel.ledgerflow.util;

import com.marcel.ledgerflow.enums.RecurringFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecurringTransactionDateCalculatorTest {

    @Test
    void calculateNextOccurrence_shouldAdvanceOneDayForDailyFrequency() {

        LocalDate currentOccurrence = LocalDate.of(2026, 11, 1);

        LocalDate nextOccurrence =
                RecurringTransactionDateCalculator.calculateNextOccurrence(
                        currentOccurrence,
                        RecurringFrequency.DAILY
                );

        assertEquals(
                LocalDate.of(2026, 11, 2),
                nextOccurrence
        );
    }


    @Test
    void calculateNextOccurrence_shouldAdvanceOneWeekForWeeklyFrequency() {

        LocalDate currentOccurrence = LocalDate.of(2026, 11, 1);

        LocalDate nextOccurrence =
                RecurringTransactionDateCalculator.calculateNextOccurrence(
                        currentOccurrence,
                        RecurringFrequency.WEEKLY
                );

        assertEquals(
                LocalDate.of(2026, 11, 8),
                nextOccurrence
        );
    }


    @Test
    void calculateNextOccurrence_shouldAdvanceOneMonthForMonthlyFrequency() {

        LocalDate currentOccurrence = LocalDate.of(2026, 11, 15);

        LocalDate nextOccurrence =
                RecurringTransactionDateCalculator.calculateNextOccurrence(
                        currentOccurrence,
                        RecurringFrequency.MONTHLY
                );

        assertEquals(
                LocalDate.of(2026, 12, 15),
                nextOccurrence
        );
    }


    @Test
    void calculateNextMonthlyOccurrence_shouldPreserveOriginalBillingDay() {

        LocalDate originalStartDate = LocalDate.of(2027, 1, 31);

        LocalDate februaryOccurrence =
                RecurringTransactionDateCalculator.calculateNextMonthlyOccurrence(
                        originalStartDate,
                        originalStartDate
                );

        assertEquals(
                LocalDate.of(2027, 2, 28),
                februaryOccurrence
        );

        LocalDate marchOccurrence =
                RecurringTransactionDateCalculator.calculateNextMonthlyOccurrence(
                        februaryOccurrence,
                        originalStartDate
                );

        assertEquals(
                LocalDate.of(2027, 3, 31),
                marchOccurrence
        );

        LocalDate aprilOccurrence =
                RecurringTransactionDateCalculator.calculateNextMonthlyOccurrence(
                        marchOccurrence,
                        originalStartDate
                );

        assertEquals(
                LocalDate.of(2027, 4, 30),
                aprilOccurrence
        );

        LocalDate mayOccurrence =
                RecurringTransactionDateCalculator.calculateNextMonthlyOccurrence(
                        aprilOccurrence,
                        originalStartDate
                );

        assertEquals(
                LocalDate.of(2027, 5, 31),
                mayOccurrence
        );
    }


    @Test
    void calculateNextMonthlyOccurrence_shouldHandleLeapYearCorrectly() {

        LocalDate originalStartDate = LocalDate.of(2028, 1, 31);

        LocalDate februaryOccurrence =
                RecurringTransactionDateCalculator.calculateNextMonthlyOccurrence(
                        originalStartDate,
                        originalStartDate
                );

        assertEquals(
                LocalDate.of(2028, 2, 29),
                februaryOccurrence
        );

        LocalDate marchOccurrence =
                RecurringTransactionDateCalculator.calculateNextMonthlyOccurrence(
                        februaryOccurrence,
                        originalStartDate
                );

        assertEquals(
                LocalDate.of(2028, 3, 31),
                marchOccurrence
        );
    }



    @Test
    void calculateNextOccurrence_shouldRejectNullCurrentOccurrenceDate() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> RecurringTransactionDateCalculator
                                .calculateNextOccurrence(
                                        null,
                                        RecurringFrequency.DAILY
                                )
                );

        assertEquals(
                "Current occurrence date cannot be null",
                exception.getMessage()
        );
    }



    @Test
    void calculateNextOccurrence_shouldRejectNullFrequency() {

        LocalDate currentOccurrence = LocalDate.of(2026, 11, 1);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> RecurringTransactionDateCalculator
                                .calculateNextOccurrence(
                                        currentOccurrence,
                                        null
                                )
                );

        assertEquals(
                "Recurring frequency cannot be null",
                exception.getMessage()
        );
    }



    @Test
    void calculateNextMonthlyOccurrence_shouldRejectNullCurrentOccurrenceDate() {

        LocalDate originalStartDate = LocalDate.of(2027, 1, 31);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> RecurringTransactionDateCalculator
                                .calculateNextMonthlyOccurrence(
                                        null,
                                        originalStartDate
                                )
                );

        assertEquals(
                "Occurrence date and original start date cannot be null",
                exception.getMessage()
        );
    }



    @Test
    void calculateNextMonthlyOccurrence_shouldRejectNullOriginalStartDate() {

        LocalDate currentOccurrence = LocalDate.of(2027, 2, 28);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> RecurringTransactionDateCalculator
                                .calculateNextMonthlyOccurrence(
                                        currentOccurrence,
                                        null
                                )
                );

        assertEquals(
                "Occurrence date and original start date cannot be null",
                exception.getMessage()
        );
    }



    @Test
    void calculateNextOccurrenceOnOrAfter_shouldReturnReferenceDateForDailyFrequency() {

        LocalDate originalStartDate = LocalDate.of(2026, 11, 1);
        LocalDate referenceDate = LocalDate.of(2026, 11, 10);

        LocalDate nextOccurrence =
                RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                RecurringFrequency.DAILY,
                                referenceDate
                        );

        assertEquals(
                LocalDate.of(2026, 11, 10),
                nextOccurrence
        );
    }



    @Test
    void calculateNextOccurrenceOnOrAfter_shouldReturnNextWeeklyOccurrence() {

        LocalDate originalStartDate = LocalDate.of(2026, 11, 1);
        LocalDate referenceDate = LocalDate.of(2026, 11, 10);

        LocalDate nextOccurrence =
                RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                RecurringFrequency.WEEKLY,
                                referenceDate
                        );

        assertEquals(
                LocalDate.of(2026, 11, 15),
                nextOccurrence
        );
    }



    @Test
    void calculateNextOccurrenceOnOrAfter_shouldReturnNextMonthlyOccurrence() {

        LocalDate originalStartDate = LocalDate.of(2026, 11, 1);
        LocalDate referenceDate = LocalDate.of(2026, 11, 10);

        LocalDate nextOccurrence =
                RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                RecurringFrequency.MONTHLY,
                                referenceDate
                        );

        assertEquals(
                LocalDate.of(2026, 12, 1),
                nextOccurrence
        );
    }



    @Test
    void calculateNextOccurrenceOnOrAfter_shouldPreserveMonthEndBillingDay() {

        LocalDate originalStartDate = LocalDate.of(2027, 1, 31);

        // February 15: the February occurrence has not happened yet.
        LocalDate februaryOccurrence =
                RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                RecurringFrequency.MONTHLY,
                                LocalDate.of(2027, 2, 15)
                        );

        assertEquals(
                LocalDate.of(2027, 2, 28),
                februaryOccurrence
        );

        // March 1: the February occurrence has passed.
        LocalDate marchOccurrence =
                RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                RecurringFrequency.MONTHLY,
                                LocalDate.of(2027, 3, 1)
                        );

        assertEquals(
                LocalDate.of(2027, 3, 31),
                marchOccurrence
        );

        // April 15: April has only 30 days.
        LocalDate aprilOccurrence =
                RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                RecurringFrequency.MONTHLY,
                                LocalDate.of(2027, 4, 15)
                        );

        assertEquals(
                LocalDate.of(2027, 4, 30),
                aprilOccurrence
        );
    }



    @Test
    void calculateNextOccurrenceOnOrAfter_shouldReturnOriginalStartDateWhenReferenceIsEarlier() {

        LocalDate originalStartDate = LocalDate.of(2026, 12, 1);
        LocalDate referenceDate = LocalDate.of(2026, 11, 15);

        for (RecurringFrequency frequency : RecurringFrequency.values()) {

            LocalDate nextOccurrence =
                    RecurringTransactionDateCalculator
                            .calculateNextOccurrenceOnOrAfter(
                                    originalStartDate,
                                    frequency,
                                    referenceDate
                            );

            assertEquals(
                    originalStartDate,
                    nextOccurrence,
                    "Incorrect next occurrence for frequency: " + frequency
            );
        }
    }



    @Test
    void calculateNextOccurrenceOnOrAfter_shouldRejectNullArguments() {

        LocalDate originalStartDate = LocalDate.of(2026, 11, 1);
        LocalDate referenceDate = LocalDate.of(2026, 11, 10);

        // Null original start date
        assertThrows(
                IllegalArgumentException.class,
                () -> RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                null,
                                RecurringFrequency.MONTHLY,
                                referenceDate
                        )
        );

        // Null frequency
        assertThrows(
                IllegalArgumentException.class,
                () -> RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                null,
                                referenceDate
                        )
        );

        // Null reference date
        assertThrows(
                IllegalArgumentException.class,
                () -> RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                originalStartDate,
                                RecurringFrequency.MONTHLY,
                                null
                        )
        );
    }


}
