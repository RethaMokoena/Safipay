package com.safipay.user.controller;

import com.safipay.user.dto.RegisterRequest;
import com.safipay.user.dto.LoginRequest;
import com.safipay.user.model.User;
import com.safipay.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
