
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.entity.Transaction;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.repository.TransactionRepository;
import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.util.RecurringTransactionDateCalculator;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class RecurringTransactionProcessorService {


    private static final int MAX_OCCURRENCES_PER_RUN = 100;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;


    public RecurringTransactionProcessorService(
            RecurringTransactionRepository recurringTransactionRepository,
            TransactionRepository transactionRepository,
            Clock clock
    ) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }


    @Transactional
    public void processRecurringTransaction(Long recurringTransactionId) {

        RecurringTransaction recurringTransaction =
                recurringTransactionRepository.findByIdForUpdate(
                        recurringTransactionId
                ).orElse(null);

        if (recurringTransaction == null) {
            return;
        }

        if (!recurringTransaction.isActive()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);

        if (recurringTransaction.getNextOccurrenceDate().isAfter(today)) {
            return;
        }


        if (recurringTransaction.getEndDate() != null
                && recurringTransaction.getNextOccurrenceDate()
                .isAfter(recurringTransaction.getEndDate())) {

            recurringTransaction.setActive(false);

            recurringTransactionRepository.save(recurringTransaction);

            return;
        }

        int processedOccurrences = 0;

        while (processedOccurrences < MAX_OCCURRENCES_PER_RUN
                && recurringTransaction.isActive()
                && !recurringTransaction.getNextOccurrenceDate().isAfter(today)
                && (recurringTransaction.getEndDate() == null
                || !recurringTransaction.getNextOccurrenceDate()
                .isAfter(recurringTransaction.getEndDate()))) {



            LocalDate occurrenceDate =
                    recurringTransaction.getNextOccurrenceDate();

            boolean alreadyProcessed =
                    transactionRepository.existsByRecurringTransactionIdAndTransactionDate(
                            recurringTransactionId,
                            occurrenceDate
                    );

            if (!alreadyProcessed) {

                Transaction transaction = new Transaction();

                transaction.setAccount(recurringTransaction.getAccount());
                transaction.setCategory(recurringTransaction.getCategory());
                transaction.setAmount(recurringTransaction.getAmount());
                transaction.setDescription(recurringTransaction.getDescription());
                transaction.setType(recurringTransaction.getType());
                transaction.setTransactionDate(occurrenceDate);
                transaction.setRecurringTransaction(recurringTransaction);

                transactionRepository.save(transaction);
            }


            LocalDate nextOccurrenceDate;

            if (recurringTransaction.getFrequency()
                    == com.marcel.ledgerflow.enums.RecurringFrequency.MONTHLY) {

                nextOccurrenceDate =
                        RecurringTransactionDateCalculator.calculateNextMonthlyOccurrence(
                                occurrenceDate,
                                recurringTransaction.getStartDate()
                        );

            } else {

                nextOccurrenceDate =
                        RecurringTransactionDateCalculator.calculateNextOccurrence(
                                occurrenceDate,
                                recurringTransaction.getFrequency()
                        );
            }

            recurringTransaction.setNextOccurrenceDate(nextOccurrenceDate);

            if (recurringTransaction.getEndDate() != null
                    && nextOccurrenceDate.isAfter(recurringTransaction.getEndDate())) {

                recurringTransaction.setActive(false);
            }

            recurringTransactionRepository.save(recurringTransaction);
            processedOccurrences++;
        }


    }

}
