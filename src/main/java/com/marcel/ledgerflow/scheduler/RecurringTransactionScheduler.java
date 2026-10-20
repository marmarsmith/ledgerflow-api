
package com.marcel.ledgerflow.scheduler;

import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.service.RecurringTransactionProcessorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionProcessorService processorService;
    private final Clock clock;
    private static final Logger log =
            LoggerFactory.getLogger(RecurringTransactionScheduler.class);


    public RecurringTransactionScheduler(
            RecurringTransactionRepository recurringTransactionRepository,
            RecurringTransactionProcessorService processorService,
            Clock clock
    ) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.processorService = processorService;
        this.clock = clock;
    }





    @Scheduled(
            cron = "${ledgerflow.recurring.cron:-}",
            zone = "${ledgerflow.recurring.zone:UTC}"
    )

    public void processDueRecurringTransactions() {


        LocalDate today = LocalDate.now(clock);

        List<RecurringTransaction> dueTransactions =
                recurringTransactionRepository
                        .findByActiveTrueAndNextOccurrenceDateLessThanEqual(today);

        log.info(
                "Found {} recurring transaction schedules due for processing",
                dueTransactions.size()
        );

        for (RecurringTransaction recurringTransaction : dueTransactions) {

            Long recurringTransactionId = recurringTransaction.getId();

            try {

                processorService.processRecurringTransaction(
                        recurringTransactionId
                );

                log.info(
                        "Successfully processed recurring transaction schedule {}",
                        recurringTransactionId
                );

            } catch (Exception exception) {

                log.error(
                        "Failed to process recurring transaction schedule {}",
                        recurringTransactionId,
                        exception
                );
            }
        }
    }

}
