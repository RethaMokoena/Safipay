package com.safipay.user.controller;

import com.safipay.user.dto.response.*;
import com.safipay.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.validateAndGetUser(userId)));
    }
}
