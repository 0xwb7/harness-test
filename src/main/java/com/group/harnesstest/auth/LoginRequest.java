package com.group.harnesstest.auth;

public record LoginRequest(String email, String password) {
    public LoginRequest {
        email = EmailNormalizer.normalize(email);
    }
}
