package com.app.security;

import com.app.entity.BrandEntity;
import com.app.entity.ProjectEntity;
import com.app.entity.UserEntity;
import com.app.repository.BrandRepository;
import com.app.repository.ProjectRepository;
import com.app.repository.UserRepository;
import com.app.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private com.app.repository.ProjectCreationSessionRepository projectCreationSessionRepository;

    @Autowired
    private com.app.repository.LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private com.app.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.app.repository.OAuthAccountRepository oAuthAccountRepository;

    @Autowired
    private com.app.repository.AiUsageLogRepository aiUsageLogRepository;

    @Autowired
    private com.app.repository.ContentThreadRepository contentThreadRepository;

    @Autowired
    private com.app.repository.ContentAnalyticsRepository contentAnalyticsRepository;

    @Autowired
    private com.app.repository.BrandMemoryRepository brandMemoryRepository;

    @Autowired
    private JwtService jwtService;

    private UserEntity userA;
    private UserEntity userB;

    private String tokenA;
    private String tokenB;

    private BrandEntity brandB;
    private ProjectEntity projectB;

    @BeforeEach
    void setUp() {
        aiUsageLogRepository.deleteAll();
        contentThreadRepository.deleteAll();
        contentAnalyticsRepository.deleteAll();
        brandMemoryRepository.deleteAll();
        loginHistoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        oAuthAccountRepository.deleteAll();
        projectCreationSessionRepository.deleteAll();
        projectRepository.deleteAll();
        brandRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.saveAndFlush(UserEntity.builder()
                .publicId(UUID.randomUUID())
                .email("usera@example.com")
                .passwordHash("$argon2id$v=19$m=65536,t=3,p=1$placeholder$placeholder")
                .fullName("User A")
                .role(com.app.enums.UserRole.ROLE_USER)
                .status(com.app.enums.UserStatus.ACTIVE)
                .passwordLoginEnabled(true)
                .build());

        userB = userRepository.saveAndFlush(UserEntity.builder()
                .publicId(UUID.randomUUID())
                .email("userb@example.com")
                .passwordHash("$argon2id$v=19$m=65536,t=3,p=1$placeholder$placeholder")
                .fullName("User B")
                .role(com.app.enums.UserRole.ROLE_USER)
                .status(com.app.enums.UserStatus.ACTIVE)
                .passwordLoginEnabled(true)
                .build());

        tokenA = jwtService.generateAccessToken(userA, UUID.randomUUID());
        tokenB = jwtService.generateAccessToken(userB, UUID.randomUUID());

        brandB = brandRepository.saveAndFlush(BrandEntity.builder()
                .publicId(UUID.randomUUID())
                .user(userB)
                .name("User B Brand")
                .industry("Technology")
                .build());

        projectB = projectRepository.saveAndFlush(ProjectEntity.builder()
                .publicId(UUID.randomUUID())
                .user(userB)
                .name("User B Project")
                .status("ACTIVE")
                .progress(0)
                .build());
    }

    @Test
    @DisplayName("User A cannot access User B's brand details")
    void testUserCannotAccessOtherUserBrand() throws Exception {
        mockMvc.perform(get("/v1/brands/" + brandB.getPublicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User A cannot update User B's brand")
    void testUserCannotUpdateOtherUserBrand() throws Exception {
        mockMvc.perform(patch("/v1/brands/" + brandB.getPublicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .contentType("application/json")
                .content("{\"name\":\"Hacked Brand\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User A cannot delete User B's brand")
    void testUserCannotDeleteOtherUserBrand() throws Exception {
        mockMvc.perform(delete("/v1/brands/" + brandB.getPublicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User A cannot view User B's project details")
    void testUserCannotAccessOtherUserProject() throws Exception {
        mockMvc.perform(get("/v1/projects/" + projectB.getPublicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User B can access own brand details cleanly")
    void testUserCanAccessOwnBrand() throws Exception {
        mockMvc.perform(get("/v1/brands/" + brandB.getPublicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }
}
