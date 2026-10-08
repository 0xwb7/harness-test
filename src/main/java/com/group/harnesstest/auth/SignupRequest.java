package com.group.harnesstest.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(@NotBlank @Email @Size(max = 255) String email,
                            @NotBlank @Size(min = 8) String password,
                            @NotBlank @Size(max = 255) String name) {
    public SignupRequest {
        email = EmailNormalizer.normalize(email);
    }
}
