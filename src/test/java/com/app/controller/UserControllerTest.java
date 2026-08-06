package com.app.controller;

import com.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check of the users endpoint against the in-memory test database,
 * covering the snake_case contract defined by the API Standard.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.app.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.app.repository.LoginHistoryRepository loginHistoryRepository;

    @BeforeEach
    void resetDatabase() {
        loginHistoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser
    void createUserReturnsCreatedWithSnakeCasePayload() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"full_name": "Jane Doe", "email": "jane.doe@example.com"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.public_id").exists())
            .andExpect(jsonPath("$.full_name").value("Jane Doe"))
            .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    @WithMockUser
    void createUserReturnsFieldErrorsForInvalidPayload() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"full_name": "", "email": "not-an-email"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.field_errors[*].field",
                org.hamcrest.Matchers.hasItem("full_name")));
    }

    @Test
    @WithMockUser
    void createUserReturnsConflictForDuplicateEmail() throws Exception {
        String payload = """
            {"full_name": "Jane Doe", "email": "duplicate@example.com"}
            """;

        mockMvc.perform(post("/v1/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @WithMockUser
    void getUsersReturnsCreatedUsers() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"full_name": "Jane Doe", "email": "list@example.com"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("list@example.com"));
    }

    @Test
    @WithMockUser
    void getUnknownUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/users/{public_id}", "11111111-1111-1111-1111-111111111111"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("RESOURCE_NOT_FOUND"));
    }
}
