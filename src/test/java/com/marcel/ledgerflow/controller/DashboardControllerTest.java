package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.DashboardResponse;
import com.marcel.ledgerflow.dto.MonthlyTrendResponse;
import com.marcel.ledgerflow.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken(
                "1",
                null,
                List.of()
        );
    }

    @Test
    void shouldReturnDashboardForAuthenticatedUser() {

        DashboardResponse expectedResponse = new DashboardResponse(
                2026,
                new BigDecimal("60000.00"),
                new BigDecimal("38500.00"),
                new BigDecimal("21500.00"),
                List.of(
                        new MonthlyTrendResponse(
                                1,
                                new BigDecimal("5000.00"),
                                new BigDecimal("3200.00"),
                                new BigDecimal("1800.00")
                        )
                )
        );

        when(dashboardService.getDashboard(1L, 2026))
                .thenReturn(expectedResponse);

        DashboardResponse response =
                dashboardController.getDashboard(2026, authentication);

        assertEquals(2026, response.year());

        assertEquals(
                0,
                new BigDecimal("60000.00")
                        .compareTo(response.totalIncome())
        );

        assertEquals(
                0,
                new BigDecimal("38500.00")
                        .compareTo(response.totalExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("21500.00")
                        .compareTo(response.netSavings())
        );

        assertEquals(1, response.monthlyTrends().size());

        verify(dashboardService).getDashboard(1L, 2026);
    }

    @Test
    void shouldRejectYearBelowMinimum() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> dashboardController.getDashboard(0, authentication)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(dashboardService);
    }

    @Test
    void shouldRejectYearAboveMaximum() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> dashboardController.getDashboard(10000, authentication)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(dashboardService);
    }
}