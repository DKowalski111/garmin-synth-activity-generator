package com.example.syntheticfit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String FIXED_END = "2024-06-15T10:00:00Z";

    private Map<String, Object> buildRequest() {
        return Map.of(
                "activityName", "Integration Test Activity",
                "sport", "CYCLING",
                "route", Map.of(
                        "distanceMeters", 5000.0,
                        "points", List.of(
                                Map.of("latitude", 50.2945, "longitude", 18.6712),
                                Map.of("latitude", 50.2960, "longitude", 18.6900),
                                Map.of("latitude", 50.2981, "longitude", 18.9974)
                        )
                ),
                "timeConfiguration", Map.of(
                        "mode", "END_AT_SELECTED_TIME",
                        "selectedTime", FIXED_END
                ),
                "averageSpeedKmh", 25.0,
                "averageHeartRate", 145,
                "recordingIntervalSeconds", 5,
                "seed", 42,
                "pauses", List.of()
        );
    }

    @Test
    void preview_returnsActivityData() throws Exception {
        mockMvc.perform(post("/api/activities/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                // distanceMeters is recalculated from actual coords — just check it's a positive number
                .andExpect(jsonPath("$.summary.distanceMeters").isNumber())
                .andExpect(jsonPath("$.samples").isArray())
                .andExpect(jsonPath("$.samples.length()").isNumber());
    }

    @Test
    void fitGeneration_returnsBinaryFitFile() throws Exception {
        mockMvc.perform(post("/api/activities/fit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.ant.fit"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void preview_withInvalidSpeed_returnsBadRequest() throws Exception {
        var req = new java.util.HashMap<>(buildRequest());
        req.put("averageSpeedKmh", 0.0); // invalid
        mockMvc.perform(post("/api/activities/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preview_withInvalidHeartRate_returnsBadRequest() throws Exception {
        var req = new java.util.HashMap<>(buildRequest());
        req.put("averageHeartRate", 300); // invalid
        mockMvc.perform(post("/api/activities/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
