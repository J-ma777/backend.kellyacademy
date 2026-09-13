package com.kellyacademy.security.auth.controller;

import com.kellyacademy.security.auth.dto.AuthRequest;
import com.kellyacademy.security.auth.dto.AuthResponse;
import com.kellyacademy.security.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}
