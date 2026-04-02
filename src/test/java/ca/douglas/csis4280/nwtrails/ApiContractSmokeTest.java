package ca.douglas.csis4280.nwtrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Test
    void checkinPhotoUploadAndHistoryIncludePhotoUrls() throws Exception {
        String token = loginAndGetAccessToken("admin01", "AdminPass!");

        MockMultipartFile file1 = new MockMultipartFile(
            "file",
            "checkin-1.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
            "file",
            "checkin-2.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-2".getBytes()
        );

        MvcResult uploadResult1 = mockMvc
            .perform(
                multipart("/api/v1/checkins/photos")
                    .file(file1)
                    .header("Authorization", "Bearer " + token)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.photoUrl").exists())
            .andReturn();

        MvcResult uploadResult2 = mockMvc
            .perform(
                multipart("/api/v1/checkins/photos")
                    .file(file2)
                    .header("Authorization", "Bearer " + token)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.photoUrl").exists())
            .andReturn();

        String photoUrl1 = objectMapper
            .readTree(uploadResult1.getResponse().getContentAsString())
            .get("photoUrl")
            .asText();

        String photoUrl2 = objectMapper
            .readTree(uploadResult2.getResponse().getContentAsString())
            .get("photoUrl")
            .asText();

        mockMvc
            .perform(
                post("/api/v1/checkins")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "landmarkId": "l2",
                          "latitude": 49.2060,
                          "longitude": -122.9079,
                          "photoUrls": ["%s", "%s"]
                        }
                        """
                            .formatted(photoUrl1, photoUrl2)
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.checkIn.photoUrls[0]").value(photoUrl1))
            .andExpect(jsonPath("$.checkIn.photoUrls[1]").value(photoUrl2));

        mockMvc
            .perform(get("/api/v1/checkins").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].photoUrls[0]").value(photoUrl1))
            .andExpect(jsonPath("$.items[0].photoUrls[1]").value(photoUrl2));
    }

    @Test
    void checkinPhotoCannotBeAccessedByAnotherUser() throws Exception {
        String ownerToken = loginAndGetAccessToken("admin01", "AdminPass!");
        String otherUserToken = loginAndGetAccessToken("student01", "Passw0rd!");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "private-checkin.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "private-image".getBytes()
        );

        MvcResult uploadResult = mockMvc
            .perform(
                multipart("/api/v1/checkins/photos")
                    .file(file)
                    .header("Authorization", "Bearer " + ownerToken)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.photoUrl").exists())
            .andReturn();

        String photoUrl = objectMapper
            .readTree(uploadResult.getResponse().getContentAsString())
            .get("photoUrl")
            .asText();

        mockMvc
            .perform(get("/api/v1" + photoUrl).header("Authorization", "Bearer " + otherUserToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
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
