package com.safipay.user.dto.response;

import com.safipay.user.model.User;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class UserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private User.Role role;
    private User.UserStatus status;
    private Boolean emailVerified;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
}
