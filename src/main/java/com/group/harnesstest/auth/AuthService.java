package com.group.harnesstest.auth;

import com.group.harnesstest.user.User;
import com.group.harnesstest.user.UserRepository;
import com.group.harnesstest.user.UserResponse;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder jwtEncoder) {
        this.users = users;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        try {
            User saved = users.saveAndFlush(new User(request.email(), passwords.encode(request.password()), request.name()));
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            // The database uniqueness constraint also protects concurrent signups.
            throw new DuplicateEmailException();
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email()).orElseThrow(InvalidCredentialsException::new);
        if (request.password() == null || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new TokenResponse(jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), "Bearer");
    }
}
