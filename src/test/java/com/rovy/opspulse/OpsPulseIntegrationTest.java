package com.rovy.opspulse;

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
class OpsPulseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void incidentLifecycleAndDashboard() throws Exception {
        String created = mvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON)
                .content(""" 
                    {"serviceKey":"billing-api","name":"Billing API","environment":"dev"}
                    """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.health").value("UNKNOWN"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();
        mvc.perform(post("/api/v1/services/" + id + "/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"health":"DOWN","note":"Timeouts from synthetic monitor"}
                    """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.health").value("DOWN"));
        String incident = mvc.perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"serviceId":%d,"title":"API unreachable","severity":"HIGH"}
                    """.formatted(id)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long incidentId = mapper.readTree(incident).get("id").asLong();
        mvc.perform(get("/api/v1/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.down").value(1))
                .andExpect(jsonPath("$.activeIncidents").value(1));
        mvc.perform(patch("/api/v1/incidents/" + incidentId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"RESOLVED"}
                    """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
        mvc.perform(get("/api/v1/dashboard")).andExpect(jsonPath("$.activeIncidents").value(0));
    }

    @Test void validatesInputsAndMissingIds() throws Exception {
        mvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"serviceKey":"!","name":"","environment":""}
                    """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/services/9999999")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"serviceId":9999999,"title":"Missing","severity":"LOW"}
                    """))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/services/9999999/observations").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"health":"DOWN"}
                    """))
                .andExpect(status().isNotFound());
    }

    @Test void duplicateKeysReturnConflict() throws Exception {
        String payload = """
            {"serviceKey":"inventory-1","name":"Inventory","environment":"test"}
            """;
        mvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/services").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }
}