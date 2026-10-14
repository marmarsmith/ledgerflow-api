package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.MonthlyReportResponse;
import com.marcel.ledgerflow.service.MonthlyReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    public MonthlyReportController(
            MonthlyReportService monthlyReportService
    ) {
        this.monthlyReportService = monthlyReportService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month
    ) {
        if (year < 1 || year > 9999) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Year must be between 1 and 9999"
            );
        }

        if (month < 1 || month > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Month must be between 1 and 12"
            );
        }

        Long userId = Long.valueOf(authentication.getName());

        MonthlyReportResponse report =
                monthlyReportService.getMonthlyReport(
                        userId,
                        year,
                        month
                );

        return ResponseEntity.ok(report);
    }
}