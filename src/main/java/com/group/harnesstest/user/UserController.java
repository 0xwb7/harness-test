package com.group.harnesstest.user;

import com.group.harnesstest.auth.InvalidCredentialsException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(JwtAuthenticationToken authentication) {
        try {
            return users.me(Long.valueOf(authentication.getToken().getSubject()));
        } catch (NumberFormatException exception) {
            throw new InvalidCredentialsException();
        }
    }
}
