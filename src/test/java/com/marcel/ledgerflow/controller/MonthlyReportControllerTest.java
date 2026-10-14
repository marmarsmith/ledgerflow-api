package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.MonthlyReportResponse;
import com.marcel.ledgerflow.service.MonthlyReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MonthlyReportControllerTest {

    @Mock
    private MonthlyReportService monthlyReportService;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MonthlyReportController controller =
                new MonthlyReportController(monthlyReportService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter()
                )
                .build();
    }

    @Test
    void getMonthlyReport_ShouldReturn200ForValidDate() throws Exception {

        MonthlyReportResponse response = new MonthlyReportResponse(
                2026,
                10,
                new BigDecimal("5000.00"),
                new BigDecimal("1850.00"),
                new BigDecimal("3150.00"),
                List.of()
        );

        when(authentication.getName()).thenReturn("1");

        when(monthlyReportService.getMonthlyReport(1L, 2026, 10))
                .thenReturn(response);

        mockMvc.perform(get("/api/reports/monthly")
                        .principal(authentication)
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpenses").value(1850.00))
                .andExpect(jsonPath("$.netSavings").value(3150.00))
                .andExpect(jsonPath("$.expensesByCategory").isArray());

        verify(monthlyReportService)
                .getMonthlyReport(1L, 2026, 10);
    }

    @Test
    void getMonthlyReport_ShouldReturn400ForInvalidMonth() throws Exception {

        mockMvc.perform(get("/api/reports/monthly")
                        .principal(authentication)
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(monthlyReportService);
    }

    @Test
    void getMonthlyReport_ShouldReturn400ForInvalidYear() throws Exception {

        mockMvc.perform(get("/api/reports/monthly")
                        .principal(authentication)
                        .param("year", "0")
                        .param("month", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(monthlyReportService);
    }
}