package com.app.controller;

import com.app.repository.RefreshTokenRepository;
import com.app.repository.UserRepository;
import com.app.util.SecureTokenUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
    void unauthenticatedAccessToProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));
    }

    @Test
    void agentWorkspaceIsNoLongerReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/v1/agent-workspace/sessions"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenIsRotatedAndTheOldOneStopsWorking() throws Exception {
        String registerPayload = """
            {
                "full_name": "Rotate User",
                "email": "rotate@example.com",
                "password": "Password123!"
            }
            """;

        String registerBody = mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String firstRefreshToken = JsonPath.read(registerBody, "$.refresh_token");

        String refreshBody = mockMvc.perform(post("/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\": \"%s\"}".formatted(firstRefreshToken)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String secondRefreshToken = JsonPath.read(refreshBody, "$.refresh_token");
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        // Replaying the rotated token is treated as a compromise and rejected.
        mockMvc.perform(post("/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\": \"%s\"}".formatted(firstRefreshToken)))
            .andExpect(status().isUnauthorized());

        // ...and it takes every other session for that account down with it.
        mockMvc.perform(post("/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\": \"%s\"}".formatted(secondRefreshToken)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void storedRefreshTokenIsNeverThePlaintextHandedToTheClient() throws Exception {
        String registerPayload = """
            {
                "full_name": "Digest User",
                "email": "digest@example.com",
                "password": "Password123!"
            }
            """;

        String body = mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String issuedRefreshToken = JsonPath.read(body, "$.refresh_token");

        assertThat(refreshTokenRepository.findAll())
            .singleElement()
            .satisfies(stored -> {
                assertThat(stored.getTokenHash()).isNotEqualTo(issuedRefreshToken);
                assertThat(stored.getTokenHash()).isEqualTo(SecureTokenUtil.hash(issuedRefreshToken));
            });
    }
}
