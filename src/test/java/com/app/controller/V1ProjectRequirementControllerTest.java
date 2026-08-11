package com.app.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("V1 Requirement Controller Integration Tests")
class V1ProjectRequirementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String testEmail = "req_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String registerPayload = """
            {
                "full_name": "Req User",
                "email": "%s",
                "password": "Password123!"
            }
            """.formatted(testEmail);

        String registerBody = mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        token = JsonPath.read(registerBody, "$.access_token");
    }

    @Test
    @DisplayName("E2E Requirement Analysis Flow: Start -> Save Answers (POST 2xx) -> Next Step -> Complete")
    void fullRequirementAnalysisFlow() throws Exception {
        // 1. Start requirement analysis session
        String startPayload = """
            {
                "initialPrompt": "Build a real-time analytics dashboard"
            }
            """;

        String startResponseBody = mockMvc.perform(post("/v1/projects/requirements/start")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(startPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").exists())
            .andExpect(jsonPath("$.currentStep").value(1))
            .andExpect(jsonPath("$.questions").isArray())
            .andReturn().getResponse().getContentAsString();

        String sessionId = JsonPath.read(startResponseBody, "$.sessionId");
        assertThat(sessionId).isNotNull();

        // 2. GET requirement session by ID
        mockMvc.perform(get("/v1/projects/requirements/" + sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.initialPrompt").value("Build a real-time analytics dashboard"));

        // 3. Save requirement question answers — MUST return 200 OK (no 500 error!)
        String answersPayload = """
            {
                "answers": {
                    "frontend": "react",
                    "backend": "springboot"
                },
                "customValues": {}
            }
            """;

        mockMvc.perform(post("/v1/projects/requirements/" + sessionId + "/answers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(answersPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.questions").isArray())
            .andExpect(jsonPath("$.currentStep").value(3));

        // 4. Save remaining answers to reach REVIEW_READY
        String completeAnswersPayload = """
            {
                "answers": {
                    "frontend": "react",
                    "backend": "springboot",
                    "database": "postgres",
                    "authentication": "jwt",
                    "styling": "tailwind"
                },
                "customValues": {}
            }
            """;

        mockMvc.perform(post("/v1/projects/requirements/" + sessionId + "/answers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeAnswersPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVIEW_READY"));

        // 5. Finalize configuration & create project
        mockMvc.perform(post("/v1/projects/requirements/" + sessionId + "/complete")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").exists())
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @DisplayName("Unauthenticated request to POST /v1/projects/requirements/{sessionId}/answers fails or is rejected")
    void unauthenticatedAccessFails() throws Exception {
        mockMvc.perform(post("/v1/projects/requirements/00000000-0000-0000-0000-000000000000/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
