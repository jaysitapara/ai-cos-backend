package com.app.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("V1 Session Controller Integration Tests")
class V1SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String testEmail = "session_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String registerPayload = """
            {
                "full_name": "Session User",
                "email": "%s",
                "password": "Password123!"
            }
            """.formatted(testEmail);

        String response = mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        this.token = com.jayway.jsonpath.JsonPath.read(response, "$.access_token");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url);
    }

    @Test
    @DisplayName("GET /v1/sessions returns HTTP 200 and list of active sessions")
    void testGetActiveSessions() throws Exception {
        mockMvc.perform(get("/v1/sessions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /v1/sessions/login-history returns HTTP 200 and login history")
    void testGetLoginHistory() throws Exception {
        mockMvc.perform(get("/v1/sessions/login-history")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
