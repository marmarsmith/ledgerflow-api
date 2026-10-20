
package com.marcel.ledgerflow.util;

import com.marcel.ledgerflow.enums.RecurringFrequency;

import java.time.LocalDate;
import java.time.YearMonth;

public final class RecurringTransactionDateCalculator {

    private RecurringTransactionDateCalculator() {
        // Utility class - prevent instantiation.
    }

    public static LocalDate calculateNextOccurrence(
            LocalDate currentOccurrence,
            RecurringFrequency frequency
    ) {
        if (currentOccurrence == null) {
            throw new IllegalArgumentException(
                    "Current occurrence date cannot be null"
            );
        }

        if (frequency == null) {
            throw new IllegalArgumentException(
                    "Recurring frequency cannot be null"
            );
        }

        return switch (frequency) {
            case DAILY -> currentOccurrence.plusDays(1);
            case WEEKLY -> currentOccurrence.plusWeeks(1);
            case MONTHLY -> currentOccurrence.plusMonths(1);
        };
    }

    public static LocalDate calculateNextMonthlyOccurrence(
            LocalDate currentOccurrence,
            LocalDate originalStartDate
    ) {
        if (currentOccurrence == null || originalStartDate == null) {
            throw new IllegalArgumentException(
                    "Occurrence date and original start date cannot be null"
            );
        }

        YearMonth nextMonth = YearMonth.from(currentOccurrence).plusMonths(1);

        int originalDay = originalStartDate.getDayOfMonth();

        int adjustedDay = Math.min(
                originalDay,
                nextMonth.lengthOfMonth()
        );

        return nextMonth.atDay(adjustedDay);
    }


    public static LocalDate calculateNextOccurrenceOnOrAfter(
            LocalDate originalStartDate,
            RecurringFrequency frequency,
            LocalDate referenceDate
    ) {
        if (originalStartDate == null
                || frequency == null
                || referenceDate == null) {
            throw new IllegalArgumentException(
                    "Start date, frequency, and reference date cannot be null"
            );
        }

        if (!originalStartDate.isBefore(referenceDate)) {
            return originalStartDate;
        }

        return switch (frequency) {
            case DAILY -> {
                long daysElapsed = java.time.temporal.ChronoUnit.DAYS
                        .between(originalStartDate, referenceDate);

                yield originalStartDate.plusDays(daysElapsed);
            }

            case WEEKLY -> {
                long daysElapsed = java.time.temporal.ChronoUnit.DAYS
                        .between(originalStartDate, referenceDate);

                long weeksElapsed = (daysElapsed + 6) / 7;

                yield originalStartDate.plusWeeks(weeksElapsed);
            }

            case MONTHLY -> {
                java.time.YearMonth startMonth =
                        java.time.YearMonth.from(originalStartDate);

                java.time.YearMonth referenceMonth =
                        java.time.YearMonth.from(referenceDate);

                long monthsElapsed = java.time.temporal.ChronoUnit.MONTHS
                        .between(startMonth, referenceMonth);

                java.time.YearMonth targetMonth =
                        startMonth.plusMonths(monthsElapsed);

                int billingDay = Math.min(
                        originalStartDate.getDayOfMonth(),
                        targetMonth.lengthOfMonth()
                );

                LocalDate candidate = targetMonth.atDay(billingDay);

                if (candidate.isBefore(referenceDate)) {
                    java.time.YearMonth nextMonth =
                            targetMonth.plusMonths(1);

                    candidate = nextMonth.atDay(
                            Math.min(
                                    originalStartDate.getDayOfMonth(),
                                    nextMonth.lengthOfMonth()
                            )
                    );
                }

                yield candidate;
            }
        };
    }

}
