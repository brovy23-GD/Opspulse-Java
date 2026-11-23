package com.rovy.opspulse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardPageTest {
    @Autowired MockMvc mvc;

    @Test
    void dashboardIsServedAtRoot() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("Infrastructure overview")))
            .andExpect(content().string(containsString("Record observation")))
            .andExpect(content().string(containsString("Open incident")))
            .andExpect(content().string(containsString("Service status is manually reported")));
    }

    @Test
    void javascriptAndStylesheetsAreServed() throws Exception {
        mvc.perform(get("/dashboard/app.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("api('/dashboard')")))
            .andExpect(content().string(containsString("createElement")));
        mvc.perform(get("/dashboard/styles.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(".summary-grid")))
            .andExpect(content().string(containsString("@media")));
    }

    @Test
    void apiAndDashboardCoexistOnSameOrigin() throws Exception {
        mvc.perform(get("/api/v1/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalServices").isNumber())
            .andExpect(jsonPath("$.activeIncidents").isNumber());
    }
}
