package com.group.harnesstest;

import com.group.harnesstest.user.User;
import com.group.harnesstest.user.UserRepository;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {
    private static final String TEST_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.secret", () -> TEST_SECRET);
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtDecoder jwtDecoder;

    @BeforeEach
    void clearUsers() {
        users.deleteAll();
    }

    @Test
    void signupNormalizesEmailAndStoresBcryptHash() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  Test@Example.COM  \",\"password\":\"password123\",\"name\":\"홍길동\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))));
        User user = users.findByEmail("test@example.com").orElseThrow();
        assertFalse(user.getPasswordHash().equals("password123"));
        assertTrue(passwords.matches("password123", user.getPasswordHash()));
    }

    @Test
    void invalidSignupAndDuplicateEmail() throws Exception {
        for (String body : new String[] {
                "{\"email\":\"bad\",\"password\":\"password123\",\"name\":\"홍길동\"}",
                "{\"email\":\"a@example.com\",\"password\":\"short\",\"name\":\"홍길동\"}",
                "{\"email\":\"a@example.com\",\"password\":\"password123\",\"name\":\"   \"}"
        }) {
            mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        signup();
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" TEST@EXAMPLE.COM \",\"password\":\"password123\",\"name\":\"다른 이름\"}"))
                .andExpect(status().isConflict());
        assertEquals(1, users.count());
    }

    @Test
    void loginAndMe() throws Exception {
        signup();
        String token = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" TEST@EXAMPLE.COM \",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(token, "$.accessToken");
        var jwt = jwtDecoder.decode(accessToken);
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals(3600, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))));
    }

    @Test
    void invalidLoginsHaveIdenticalResponses() throws Exception {
        signup();
        String unknown = loginError("missing@example.com", "password123");
        String wrongPassword = loginError("test@example.com", "wrongpass");
        assertEquals(unknown, wrongPassword);
    }

    @Test
    void meRejectsMissingForgedAndExpiredTokens() throws Exception {
        signup();
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer x.y.z"))
                .andExpect(status().isUnauthorized());
        String subject = users.findByEmail("test@example.com").orElseThrow().getId().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(subject)
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600)).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    private void signup() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\",\"name\":\"홍길동\"}"))
                .andExpect(status().isCreated());
    }

    private String loginError(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
    }
}
