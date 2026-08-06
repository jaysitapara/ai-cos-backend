package com.app.controller;

import com.app.entity.UserEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import com.app.repository.UserRepository;
import com.app.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.app.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.app.repository.LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private JwtService jwtService;

    private UserEntity testUser;
    private String bearerToken;

    @BeforeEach
    void setUp() {
        loginHistoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = UserEntity.builder()
            .publicId(UUID.randomUUID())
            .email("dashuser@example.com")
            .fullName("Dashboard User")
            .passwordHash("secret_hash")
            .role(UserRole.ROLE_USER)
            .status(UserStatus.ACTIVE)
            .build();

        testUser = userRepository.save(testUser);
        bearerToken = "Bearer " + jwtService.generateAccessToken(testUser, UUID.randomUUID());
    }

    @Test
    void getSummaryReturnsDashboardMetrics() throws Exception {
        mockMvc.perform(get("/v1/dashboard/summary")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.system_status").value("OPERATIONAL"))
            .andExpect(jsonPath("$.productivity_score").exists());
    }

    @Test
    void getProjectsReturnsProjectsList() throws Exception {
        mockMvc.perform(get("/v1/dashboard/projects")
                .header("Authorization", bearerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchReturnsMatchingResults() throws Exception {
        mockMvc.perform(get("/v1/dashboard/search?q=test")
                .header("Authorization", bearerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void unauthenticatedAccessToDashboardReturns401() throws Exception {
        mockMvc.perform(get("/v1/dashboard/summary"))
            .andExpect(status().isUnauthorized());
    }
}
