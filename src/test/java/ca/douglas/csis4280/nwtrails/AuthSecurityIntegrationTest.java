package ca.douglas.csis4280.nwtrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ca.douglas.csis4280.nwtrails.repository.LandmarkRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LandmarkRepository landmarkRepository;

    @Test
    void refreshIssuesNewTokenPairWhenRefreshTokenIsValid() throws Exception {
        JsonNode loginBody = login("student01", "Passw0rd!");
        String refreshToken = loginBody.get("refreshToken").asText();

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "refreshToken": "%s"
                        }
                        """
                            .formatted(refreshToken)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void meReturnsCurrentUserForValidToken() throws Exception {
        String accessToken = login("student01", "Passw0rd!").get("accessToken").asText();

        mockMvc
            .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("student01"))
            .andExpect(jsonPath("$.displayName").isNotEmpty())
            .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void logoutRevokesRefreshTokenForCurrentUser() throws Exception {
        JsonNode loginBody = login("student01", "Passw0rd!");
        String accessToken = loginBody.get("accessToken").asText();
        String refreshToken = loginBody.get("refreshToken").asText();

        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "refreshToken": "%s"
                        }
                        """
                            .formatted(refreshToken)
                    )
            )
            .andExpect(status().isNoContent());

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "refreshToken": "%s"
                        }
                        """
                            .formatted(refreshToken)
                    )
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void refreshRejectsPreviouslyUsedRefreshToken() throws Exception {
        JsonNode loginBody = login("student01", "Passw0rd!");
        String refreshToken = loginBody.get("refreshToken").asText();

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "refreshToken": "%s"
                        }
                        """
                            .formatted(refreshToken)
                    )
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "refreshToken": "%s"
                        }
                        """
                            .formatted(refreshToken)
                    )
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminEndpointReturnsUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc
            .perform(
                post("/api/v1/admin/landmarks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validLandmarkPayload())
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.details.path").value("/api/v1/admin/landmarks"));
    }

    @Test
    void adminEndpointReturnsForbiddenForNonAdminUser() throws Exception {
        String accessToken = login("student01", "Passw0rd!").get("accessToken").asText();

        mockMvc
            .perform(
                post("/api/v1/admin/landmarks")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validLandmarkPayload())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.details.path").value("/api/v1/admin/landmarks"));
    }

    @Test
    void loginReturnsBadRequestForMalformedJsonPayload() throws Exception {
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\\\"username\\\":\\\"student01\\\",\\\"password\\\":\\\"Passw0rd!\\\"}"
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """
                            .formatted(username, password)
                    )
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString());
    }

    private String validLandmarkPayload() {
        return
            """
            {
              "name": "Auth Test Landmark",
              "category": "historic",
              "address": "302 Royal Ave",
              "description": "Security integration test landmark.",
              "latitude": 49.2064,
              "longitude": -122.9094,
              "imageUrl": "https://picsum.photos/seed/auth-test/1200/700",
              "rating": 4.5
            }
            """;
    }
}
