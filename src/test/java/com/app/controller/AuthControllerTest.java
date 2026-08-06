package com.app.controller;

import com.app.repository.RefreshTokenRepository;
import com.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.app.repository.LoginHistoryRepository loginHistoryRepository;

    @BeforeEach
    void resetDatabase() {
        loginHistoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerAndLoginFlow() throws Exception {
        // Register user
        String registerPayload = """
            {
                "full_name": "Auth User",
                "email": "authuser@example.com",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("authuser@example.com"))
            .andExpect(jsonPath("$.user.role").value("ROLE_USER"));

        // Login with credentials
        String loginPayload = """
            {
                "email": "authuser@example.com",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists());
    }

    @Test
    void loginFailsWithInvalidCredentials() throws Exception {
        String loginPayload = """
            {
                "email": "nonexistent@example.com",
                "password": "WrongPassword123!"
            }
            """;

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));
    }

    @Test
    void unauthenticatedAccessToUsersReturns401() throws Exception {
        mockMvc.perform(get("/v1/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));
    }
}
