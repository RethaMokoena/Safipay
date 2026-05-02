package com.safipay.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min=2, max=50)
    private String firstName;
    @Size(min=2, max=50)
    private String lastName;
    private String phoneNumber;
    private String profilePictureUrl;
}
