
package com.marcel.ledgerflow.controller;

import com.marcel.ledgerflow.dto.RegisterRequest;
import com.marcel.ledgerflow.dto.UserResponse;
import com.marcel.ledgerflow.service.UserService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.marcel.ledgerflow.dto.LoginRequest;
import com.marcel.ledgerflow.dto.LoginResponse;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.service.AuthService;
import com.marcel.ledgerflow.service.JwtService;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;

    public UserController(
            UserService userService,
            AuthService authService,
            JwtService jwtService
    ) {
        this.userService = userService;
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = authService.authenticate(request);

        LoginResponse response = jwtService.generateToken(user);

        return ResponseEntity.ok(response);
    }

}
