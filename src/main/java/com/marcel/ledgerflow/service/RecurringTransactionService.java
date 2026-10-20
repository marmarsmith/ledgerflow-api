
package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.RecurringTransactionRequest;
import com.marcel.ledgerflow.dto.RecurringTransactionResponse;
import com.marcel.ledgerflow.entity.Account;
import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.entity.RecurringTransaction;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.enums.RecurringFrequency;
import com.marcel.ledgerflow.exception.AccountNotFoundException;
import com.marcel.ledgerflow.exception.CategoryNotFoundException;
import com.marcel.ledgerflow.exception.CategoryTypeMismatchException;
import com.marcel.ledgerflow.exception.RecurringTransactionNotFoundException;
import com.marcel.ledgerflow.repository.AccountRepository;
import com.marcel.ledgerflow.repository.CategoryRepository;
import com.marcel.ledgerflow.repository.RecurringTransactionRepository;
import com.marcel.ledgerflow.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.marcel.ledgerflow.util.RecurringTransactionDateCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringTransactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }


    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(
            Long userId,
            RecurringTransactionRequest request
    ) {

        // Find the authenticated user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found"
                        )
                );

        // Verify that the account belongs to the user
        Account account = accountRepository
                .findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(AccountNotFoundException::new);

        // Validate the recurrence dates
        if (request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {

            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        // Validate the optional category
        Category category = null;

        if (request.categoryId() != null) {

            category = categoryRepository
                    .findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(CategoryNotFoundException::new);

            if (category.getType() != request.type()) {
                throw new CategoryTypeMismatchException();
            }
        }

        // Create the recurring transaction rule
        RecurringTransaction recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setCategory(category);
        recurringTransaction.setAmount(request.amount());
        recurringTransaction.setDescription(
                request.description().trim()
        );
        recurringTransaction.setType(request.type());
        recurringTransaction.setFrequency(request.frequency());
        recurringTransaction.setStartDate(request.startDate());
        recurringTransaction.setNextOccurrenceDate(request.startDate());
        recurringTransaction.setEndDate(request.endDate());
        recurringTransaction.setActive(true);

        // Persist the rule
        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.saveAndFlush(
                        recurringTransaction
                );

        return buildRecurringTransactionResponse(savedRecurringTransaction);
    }



    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getRecurringTransactions(
            Long userId
    ) {
        return recurringTransactionRepository.findByUserId(userId)
                .stream()
                .map(this::buildRecurringTransactionResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransactionById(
            Long userId,
            Long recurringTransactionId
    ) {
        RecurringTransaction recurringTransaction =
                recurringTransactionRepository
                        .findByIdAndUserId(recurringTransactionId, userId)
                        .orElseThrow(RecurringTransactionNotFoundException::new);

        return buildRecurringTransactionResponse(recurringTransaction);
    }


    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(
            Long userId,
            Long recurringTransactionId,
            RecurringTransactionRequest request
    ) {

        RecurringTransaction recurringTransaction =
                recurringTransactionRepository
                        .findByIdAndUserId(recurringTransactionId, userId)
                        .orElseThrow(RecurringTransactionNotFoundException::new);

        Account account = accountRepository
                .findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(AccountNotFoundException::new);


        if (request.endDate() != null
                && request.endDate().isBefore(
                recurringTransaction.getStartDate()
        )) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        Category category = null;

        if (request.categoryId() != null) {
            category = categoryRepository
                    .findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(CategoryNotFoundException::new);

            if (category.getType() != request.type()) {
                throw new CategoryTypeMismatchException();
            }
        }

        boolean frequencyChanged =
                recurringTransaction.getFrequency() != request.frequency();

        recurringTransaction.setAccount(account);
        recurringTransaction.setCategory(category);
        recurringTransaction.setAmount(request.amount());
        recurringTransaction.setDescription(request.description().trim());
        recurringTransaction.setType(request.type());
        recurringTransaction.setFrequency(request.frequency());
        recurringTransaction.setEndDate(request.endDate());


        if (frequencyChanged) {

            LocalDate referenceDate = LocalDate.now(clock);

            LocalDate nextOccurrenceDate =
                    RecurringTransactionDateCalculator
                            .calculateNextOccurrenceOnOrAfter(
                                    recurringTransaction.getStartDate(),
                                    request.frequency(),
                                    referenceDate
                            );

            recurringTransaction.setNextOccurrenceDate(nextOccurrenceDate);
        }


        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.saveAndFlush(
                        recurringTransaction
                );

        return buildRecurringTransactionResponse(
                savedRecurringTransaction
        );
    }


    @Transactional
    public RecurringTransactionResponse pauseRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {

        RecurringTransaction recurringTransaction =
                recurringTransactionRepository
                        .findByIdAndUserId(
                                recurringTransactionId,
                                userId
                        )
                        .orElseThrow(
                                RecurringTransactionNotFoundException::new
                        );

        recurringTransaction.setActive(false);

        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.save(recurringTransaction);

        return buildRecurringTransactionResponse(savedRecurringTransaction);
    }


    @Transactional
    public RecurringTransactionResponse resumeRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {

        RecurringTransaction recurringTransaction =
                recurringTransactionRepository
                        .findByIdAndUserId(
                                recurringTransactionId,
                                userId
                        )
                        .orElseThrow(
                                RecurringTransactionNotFoundException::new
                        );

        LocalDate referenceDate = LocalDate.now(clock);


        LocalDate nextOccurrenceDate =
                RecurringTransactionDateCalculator
                        .calculateNextOccurrenceOnOrAfter(
                                recurringTransaction.getStartDate(),
                                recurringTransaction.getFrequency(),
                                referenceDate
                        );

// Prevent resuming a schedule with no valid future occurrences.
        if (recurringTransaction.getEndDate() != null
                && nextOccurrenceDate.isAfter(recurringTransaction.getEndDate())) {

            throw new IllegalArgumentException(
                    "Cannot resume recurring transaction because its end date has passed"
            );
        }

        recurringTransaction.setNextOccurrenceDate(nextOccurrenceDate);
        recurringTransaction.setActive(true);


        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.save(recurringTransaction);

        return buildRecurringTransactionResponse(savedRecurringTransaction);
    }


    @Transactional
    public void deleteRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {

        RecurringTransaction recurringTransaction =
                recurringTransactionRepository
                        .findByIdAndUserId(
                                recurringTransactionId,
                                userId
                        )
                        .orElseThrow(
                                RecurringTransactionNotFoundException::new
                        );

        recurringTransactionRepository.delete(recurringTransaction);
    }


    private RecurringTransactionResponse buildRecurringTransactionResponse(
            RecurringTransaction recurringTransaction
    ) {
        return new RecurringTransactionResponse(
                recurringTransaction.getId(),
                recurringTransaction.getAccount().getId(),
                recurringTransaction.getCategory() != null
                        ? recurringTransaction.getCategory().getId()
                        : null,
                recurringTransaction.getAmount(),
                recurringTransaction.getDescription(),
                recurringTransaction.getType(),
                recurringTransaction.getFrequency(),
                recurringTransaction.getStartDate(),
                recurringTransaction.getNextOccurrenceDate(),
                recurringTransaction.getEndDate(),
                recurringTransaction.isActive()
        );
    }


    private LocalDate calculateNextRecurringDate(
            RecurringTransaction recurringTransaction
    ) {

        LocalDate currentOccurrence =
                recurringTransaction.getNextOccurrenceDate();

        if (recurringTransaction.getFrequency()
                == RecurringFrequency.MONTHLY) {

            return RecurringTransactionDateCalculator
                    .calculateNextMonthlyOccurrence(
                            currentOccurrence,
                            recurringTransaction.getStartDate()
                    );
        }

        return RecurringTransactionDateCalculator
                .calculateNextOccurrence(
                        currentOccurrence,
                        recurringTransaction.getFrequency()
                );
    }



}
