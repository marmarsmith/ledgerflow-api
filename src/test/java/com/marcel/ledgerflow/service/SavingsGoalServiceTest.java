package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.SavingsGoalRequest;
import com.marcel.ledgerflow.dto.SavingsGoalResponse;
import com.marcel.ledgerflow.entity.SavingsContribution;
import com.marcel.ledgerflow.entity.SavingsGoal;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.SavingsContributionRepository;
import com.marcel.ledgerflow.repository.SavingsGoalRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.marcel.ledgerflow.dto.SavingsContributionRequest;
import com.marcel.ledgerflow.dto.SavingsContributionResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SavingsContributionRepository savingsContributionRepository;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
    }

    @Test
    void createGoal_ShouldCreateGoalWithZeroSavings() {

        Long userId = 1L;

        SavingsGoalRequest request = new SavingsGoalRequest(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                LocalDate.of(2027, 12, 31)
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(savingsGoalRepository.save(any(SavingsGoal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavingsGoalResponse response =
                savingsGoalService.createGoal(userId, request);

        assertEquals("Emergency Fund", response.name());

        assertEquals(
                0,
                new BigDecimal("5000.00").compareTo(response.targetAmount())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.savedAmount())
        );

        assertEquals(
                0,
                new BigDecimal("5000.00").compareTo(response.remainingAmount())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.progressPercentage())
        );

        verify(savingsGoalRepository).save(any(SavingsGoal.class));
    }

    @Test
    void getGoal_ShouldReturnGoalWithCorrectProgress() {

        Long userId = 1L;
        Long goalId = 10L;

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("New Car Fund");
        goal.setTargetAmount(new BigDecimal("10000.00"));
        goal.setSavedAmount(new BigDecimal("2500.00"));
        goal.setTargetDate(LocalDate.of(2028, 6, 30));

        when(savingsGoalRepository.findByIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(goal));

        SavingsGoalResponse response =
                savingsGoalService.getGoal(userId, goalId);

        assertEquals("New Car Fund", response.name());

        assertEquals(
                0,
                new BigDecimal("10000.00").compareTo(response.targetAmount())
        );

        assertEquals(
                0,
                new BigDecimal("2500.00").compareTo(response.savedAmount())
        );

        assertEquals(
                0,
                new BigDecimal("7500.00").compareTo(response.remainingAmount())
        );

        assertEquals(
                0,
                new BigDecimal("25.00").compareTo(response.progressPercentage())
        );

        verify(savingsGoalRepository)
                .findByIdAndUserId(goalId, userId);
    }

    @Test
    void getGoal_ShouldReturn404_WhenGoalDoesNotBelongToUser() {

        Long userId = 1L;
        Long goalId = 10L;

        when(savingsGoalRepository.findByIdAndUserId(goalId, userId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> savingsGoalService.getGoal(userId, goalId)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(savingsGoalRepository)
                .findByIdAndUserId(goalId, userId);

        verify(savingsGoalRepository, never())
                .findById(any());
    }

    @Test
    void updateGoal_ShouldPreserveSavedAmount() {

        Long userId = 1L;
        Long goalId = 10L;

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(new BigDecimal("2000.00"));

        SavingsGoalRequest request = new SavingsGoalRequest(
                "New Car Fund",
                new BigDecimal("10000.00"),
                LocalDate.of(2028, 6, 30)
        );

        when(savingsGoalRepository.findByIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(goal));

        when(savingsGoalRepository.save(goal))
                .thenReturn(goal);

        SavingsGoalResponse response =
                savingsGoalService.updateGoal(userId, goalId, request);

        assertEquals("New Car Fund", response.name());

        assertEquals(
                0,
                new BigDecimal("10000.00").compareTo(response.targetAmount())
        );

        assertEquals(
                0,
                new BigDecimal("2000.00").compareTo(response.savedAmount())
        );

        assertEquals(
                0,
                new BigDecimal("8000.00").compareTo(response.remainingAmount())
        );

        assertEquals(
                0,
                new BigDecimal("20.00").compareTo(response.progressPercentage())
        );

        assertEquals(
                LocalDate.of(2028, 6, 30),
                response.targetDate()
        );

        verify(savingsGoalRepository).save(goal);
    }

    @Test
    void deleteGoal_ShouldDeleteGoalBelongingToUser() {

        Long userId = 1L;
        Long goalId = 10L;

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(new BigDecimal("1000.00"));

        when(savingsGoalRepository.findByIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(goal));

        savingsGoalService.deleteGoal(userId, goalId);

        verify(savingsGoalRepository)
                .findByIdAndUserId(goalId, userId);

        verify(savingsGoalRepository)
                .delete(goal);
    }

    @Test
    void addContribution_ShouldIncreaseSavedAmountAndRecordContribution() {

        Long userId = 1L;
        Long goalId = 10L;

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(goal, "id", goalId);

        SavingsContributionRequest request =
                new SavingsContributionRequest(
                        new BigDecimal("250.00")
                );

        when(savingsGoalRepository.findByIdAndUserIdForUpdate(goalId, userId))
                .thenReturn(Optional.of(goal));

        when(savingsContributionRepository.saveAndFlush(
                any(SavingsContribution.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        SavingsContributionResponse response =
                savingsGoalService.addContribution(userId, goalId, request);

        assertEquals(
                0,
                new BigDecimal("1250.00").compareTo(goal.getSavedAmount())
        );

        assertEquals(
                0,
                new BigDecimal("250.00").compareTo(response.amount())
        );

        assertEquals(goalId, response.savingsGoalId());

        verify(savingsGoalRepository)
                .findByIdAndUserIdForUpdate(goalId, userId);

        verify(savingsGoalRepository).save(goal);

        verify(savingsContributionRepository)
                .saveAndFlush(any(SavingsContribution.class));
    }

    @Test
    void addContribution_ShouldReturn404_WhenGoalDoesNotBelongToUser() {

        Long userId = 1L;
        Long goalId = 10L;

        SavingsContributionRequest request =
                new SavingsContributionRequest(
                        new BigDecimal("250.00")
                );

        when(savingsGoalRepository.findByIdAndUserIdForUpdate(goalId, userId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> savingsGoalService.addContribution(userId, goalId, request)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(savingsGoalRepository)
                .findByIdAndUserIdForUpdate(goalId, userId);

        verify(savingsGoalRepository, never())
                .save(any(SavingsGoal.class));

        verifyNoInteractions(savingsContributionRepository);
    }

    @Test
    void getGoal_ShouldHandleSavingsExceedingTarget() {

        Long userId = 1L;
        Long goalId = 10L;

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(new BigDecimal("6000.00"));

        when(savingsGoalRepository.findByIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(goal));

        SavingsGoalResponse response =
                savingsGoalService.getGoal(userId, goalId);

        assertEquals(
                0,
                new BigDecimal("6000.00").compareTo(response.savedAmount())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.remainingAmount())
        );

        assertEquals(
                0,
                new BigDecimal("120.00").compareTo(response.progressPercentage())
        );

        verify(savingsGoalRepository)
                .findByIdAndUserId(goalId, userId);
    }

    @Test
    void getContributions_ShouldReturnContributionHistory() {

        Long userId = 1L;
        Long goalId = 10L;

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setSavedAmount(new BigDecimal("750.00"));

        ReflectionTestUtils.setField(goal, "id", goalId);

        SavingsContribution firstContribution = new SavingsContribution();
        firstContribution.setSavingsGoal(goal);
        firstContribution.setAmount(new BigDecimal("500.00"));

        ReflectionTestUtils.setField(firstContribution, "id", 100L);
        ReflectionTestUtils.setField(
                firstContribution,
                "contributedAt",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        SavingsContribution secondContribution = new SavingsContribution();
        secondContribution.setSavingsGoal(goal);
        secondContribution.setAmount(new BigDecimal("250.00"));

        ReflectionTestUtils.setField(secondContribution, "id", 101L);
        ReflectionTestUtils.setField(
                secondContribution,
                "contributedAt",
                LocalDateTime.of(2026, 10, 15, 9, 30)
        );

        when(savingsGoalRepository.findByIdAndUserId(goalId, userId))
                .thenReturn(Optional.of(goal));

        when(savingsContributionRepository
                .findBySavingsGoalIdOrderByContributedAtDesc(goalId))
                .thenReturn(List.of(secondContribution, firstContribution));

        List<SavingsContributionResponse> responses =
                savingsGoalService.getContributions(userId, goalId);

        assertEquals(2, responses.size());

        assertEquals(101L, responses.get(0).id());
        assertEquals(goalId, responses.get(0).savingsGoalId());

        assertEquals(
                0,
                new BigDecimal("250.00").compareTo(responses.get(0).amount())
        );

        assertEquals(
                LocalDateTime.of(2026, 10, 15, 9, 30),
                responses.get(0).contributedAt()
        );

        assertEquals(100L, responses.get(1).id());

        assertEquals(
                0,
                new BigDecimal("500.00").compareTo(responses.get(1).amount())
        );

        assertEquals(
                LocalDateTime.of(2026, 10, 10, 10, 0),
                responses.get(1).contributedAt()
        );

        verify(savingsGoalRepository)
                .findByIdAndUserId(goalId, userId);

        verify(savingsContributionRepository)
                .findBySavingsGoalIdOrderByContributedAtDesc(goalId);
    }

    @Test
    void getContributions_ShouldReturn404_WhenGoalDoesNotBelongToUser() {

        Long userId = 1L;
        Long goalId = 10L;

        when(savingsGoalRepository.findByIdAndUserId(goalId, userId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> savingsGoalService.getContributions(userId, goalId)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(savingsGoalRepository)
                .findByIdAndUserId(goalId, userId);

        verifyNoInteractions(savingsContributionRepository);
    }
}