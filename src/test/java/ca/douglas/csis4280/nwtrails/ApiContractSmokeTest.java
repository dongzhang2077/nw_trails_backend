package ca.douglas.csis4280.nwtrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectsLandmarksEndpointWhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/landmarks")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsTokenAndAllowsLandmarkQuery() throws Exception {
        String token = loginAndGetAccessToken("student01", "Passw0rd!");

        mockMvc
            .perform(get("/api/v1/landmarks").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void checkinReturnsOutOfRangeWhenCoordinatesAreFarAway() throws Exception {
        String token = loginAndGetAccessToken("admin01", "AdminPass!");

        mockMvc
            .perform(
                post("/api/v1/checkins")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "landmarkId": "l1",
                          "latitude": 0.0,
                          "longitude": 0.0
                        }
                        """
                    )
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("OUT_OF_RANGE"));
    }

    @Test
    void checkinReturnsConflictWhenDuplicateOnSameDay() throws Exception {
        String token = loginAndGetAccessToken("student01", "Passw0rd!");

        String validCheckInBody =
            """
            {
              "landmarkId": "l1",
              "latitude": 49.2064,
              "longitude": -122.9094
            }
            """;

        mockMvc
            .perform(
                post("/api/v1/checkins")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validCheckInBody)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc
            .perform(
                post("/api/v1/checkins")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validCheckInBody)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_CHECKIN"));
    }

    @Test
    void startRouteAndProgressMeExposeActiveRouteProgress() throws Exception {
        String token = loginAndGetAccessToken("student01", "Passw0rd!");

        mockMvc
            .perform(post("/api/v1/routes/r1/start").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.routeId").value("r1"))
            .andExpect(jsonPath("$.completed").value(false));

        mockMvc
            .perform(get("/api/v1/progress/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.badgeProgress").exists())
            .andExpect(jsonPath("$.categoryProgress").isArray())
            .andExpect(jsonPath("$.activeRoute.routeId").value("r1"));
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
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

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
