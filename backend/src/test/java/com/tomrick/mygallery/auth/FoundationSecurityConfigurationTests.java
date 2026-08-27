package com.tomrick.mygallery.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FoundationSecurityConfigurationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void publicPhotoGetEndpointsAreAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/photos/featured"))
                .andExpect(status().isOk());
        mockMvc.perform(get(
                        "/api/v1/photos/{id}",
                        "10000000-0000-0000-0000-000000000101"
                ))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/archive"))
                .andExpect(status().isOk());
    }

    @Test
    void publicPhotoMutationRemainsDenied() throws Exception {
        mockMvc.perform(post("/api/v1/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownFutureEndpointsAreDenied() throws Exception {
        mockMvc.perform(get("/api/v1/foundation-probe"))
                .andExpect(status().isForbidden());
    }
}
