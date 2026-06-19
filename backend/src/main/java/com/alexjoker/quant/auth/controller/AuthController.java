package com.alexjoker.quant.auth.controller;

import com.alexjoker.quant.auth.dto.AuthSessionDTO;
import com.alexjoker.quant.auth.dto.LoginRequest;
import com.alexjoker.quant.auth.service.AuthService;
import com.alexjoker.quant.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthSessionDTO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthSessionDTO> me() {
        return ApiResponse.ok(authService.currentSession());
    }
}
