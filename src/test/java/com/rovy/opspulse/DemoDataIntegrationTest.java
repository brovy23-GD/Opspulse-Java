package com.rovy.opspulse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemoDataIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void loadsTenFictionalServicesAndFiveIncidentsWithoutDuplicatesOrOverwritingUserData() throws Exception {
        mvc.perform(post("/api/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"serviceKey":"real-payments","name":"User Payments","environment":"production"}
                  """))
            .andExpect(status().isCreated());

        String first = mvc.perform(post("/api/v1/demo/load"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdServices").value(10))
            .andExpect(jsonPath("$.createdIncidents").value(5))
            .andExpect(jsonPath("$.skippedServices").value(0))
            .andReturn().getResponse().getContentAsString();
        JsonNode firstResult = mapper.readTree(first);
        org.junit.jupiter.api.Assertions.assertEquals(10, firstResult.get("createdServices").asInt());

        mvc.perform(get("/api/v1/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalServices").value(11))
            .andExpect(jsonPath("$.healthy").value(5))
            .andExpect(jsonPath("$.degraded").value(2))
            .andExpect(jsonPath("$.down").value(1))
            .andExpect(jsonPath("$.unknown").value(3))
            .andExpect(jsonPath("$.activeIncidents").value(3));

        mvc.perform(post("/api/v1/demo/load"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdServices").value(0))
            .andExpect(jsonPath("$.createdIncidents").value(0))
            .andExpect(jsonPath("$.skippedServices").value(10));

        String services = mvc.perform(get("/api/v1/services"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode list = mapper.readTree(services);
        org.junit.jupiter.api.Assertions.assertEquals(11, list.size());
        boolean preserved = false;
        int incidents = 0;
        int resolved = 0;
        for (JsonNode svc : list) {
            if ("real-payments".equals(svc.get("serviceKey").asText())) {
                preserved = true;
                org.junit.jupiter.api.Assertions.assertEquals("UNKNOWN", svc.get("health").asText());
            }
            String history = mvc.perform(get("/api/v1/services/" + svc.get("id").asLong() + "/incidents"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            JsonNode rows = mapper.readTree(history);
            incidents += rows.size();
            for (JsonNode incident : rows) if ("RESOLVED".equals(incident.get("status").asText())) resolved++;
        }
        org.junit.jupiter.api.Assertions.assertTrue(preserved);
        org.junit.jupiter.api.Assertions.assertEquals(5, incidents);
        org.junit.jupiter.api.Assertions.assertEquals(2, resolved);
    }
}
