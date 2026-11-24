package com.rovy.opspulse.api;

import com.rovy.opspulse.service.MonitoringService;
import com.rovy.opspulse.service.MonitoringService.MonitorInfo;
import org.springframework.web.bind.annotation.*;

/** Read-only operator view of scheduled monitoring; no arbitrary URLs can be submitted through the API. */
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitoringController {
    private final MonitoringService monitoring;
    public MonitoringController(MonitoringService monitoring) { this.monitoring = monitoring; }
    @GetMapping public MonitorInfo info() { return monitoring.info(); }
}
