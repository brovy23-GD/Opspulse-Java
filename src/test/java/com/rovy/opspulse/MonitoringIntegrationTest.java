package com.rovy.opspulse;

import com.rovy.opspulse.service.*;
import com.rovy.opspulse.domain.*;
import com.rovy.opspulse.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "opspulse.monitor.enabled=true",
    "opspulse.monitor.targets[0].service-key=mon-example",
    "opspulse.monitor.targets[0].name=Example Endpoint",
    "opspulse.monitor.targets[0].url=https://example.com/",
    "opspulse.monitor.interval-ms=3600000"
})
@AutoConfigureMockMvc
@Transactional
class MonitoringIntegrationTest {
    @Autowired MonitoringService monitor;
    @Autowired ManagedServiceRepository services;
    @Autowired MonitorCheckRepository checks;
    @Autowired MockMvc mvc;
    @MockitoBean HttpHealthProbe probe;

    @Test
    void recordsHealthLatencyAndRecoveryWithoutOpeningAnIncident() throws Exception {
        when(probe.check(any(URI.class)))
            .thenReturn(new HttpHealthProbe.Result(true, 200, "HTTP 200"))
            .thenReturn(new HttpHealthProbe.Result(false, 503, "HTTP 503"))
            .thenReturn(new HttpHealthProbe.Result(true, 204, "HTTP 204"));

        monitor.scheduledCheck();
        ManagedService service = services.findByServiceKeyIgnoreCase("mon-example").orElseThrow();
        assertEquals(HealthState.HEALTHY, service.getHealth());
        monitor.scheduledCheck();
        assertEquals(HealthState.DOWN, services.findByServiceKeyIgnoreCase("mon-example").orElseThrow().getHealth());
        monitor.scheduledCheck();
        assertEquals(HealthState.HEALTHY, services.findByServiceKeyIgnoreCase("mon-example").orElseThrow().getHealth());
        assertEquals(3, checks.findTop20ByService_IdOrderByCheckedAtDescIdDesc(service.getId()).size());
        mvc.perform(get("/api/v1/monitor"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.configuredTargets").value(1))
            .andExpect(jsonPath("$.recentChecks.length()").value(3))
            .andExpect(jsonPath("$.recentChecks[0].httpStatus").value(204))
            .andExpect(jsonPath("$.recentChecks[0].latencyMs").isNumber());
        mvc.perform(get("/api/v1/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeIncidents").value(0));
        verify(probe, times(3)).check(URI.create("https://example.com/"));
    }

    @Test
    void refusesUnsafeOrMalformedProbeUrlsBeforeAnyRequest() {
        for (String url : new String[] {
            "http://example.com", "https://localhost/health", "https://127.0.0.1/",
            "https://[::1]/", "https://example.com:8443/", "https://user:pass@example.com/",
            "https://example.com/?token=secret", "https://foo.local/", "ftp://example.com/"
        }) {
            MonitorSettings.Target target = new MonitorSettings.Target();
            target.setServiceKey("mon-unsafe");
            target.setName("Unsafe");
            target.setUrl(url);
            assertThrows(IllegalArgumentException.class, () -> MonitoringService.validate(target), url);
        }
    }
}
