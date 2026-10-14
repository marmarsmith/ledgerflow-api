package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.DashboardResponse;
import com.marcel.ledgerflow.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(
            @RequestParam int year,
            Authentication authentication
    ) {

        if (year < 1 || year > 9999) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Year must be between 1 and 9999"
            );
        }

        Long userId = Long.valueOf(authentication.getName());

        return dashboardService.getDashboard(userId, year);
    }
}