package com.rovy.opspulse.api;

import com.rovy.opspulse.service.DemoDataService;
import com.rovy.opspulse.service.DemoDataService.DemoResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Disabled unless the opspulse.demo.enabled application property is explicitly true. */
@RestController
@RequestMapping("/api/v1/demo")
@ConditionalOnProperty(name = "opspulse.demo.enabled", havingValue = "true")
public class DemoDataController {
    private final DemoDataService demo;
    public DemoDataController(DemoDataService demo) { this.demo = demo; }

    @PostMapping("/load")
    @ResponseStatus(HttpStatus.OK)
    public DemoResult load() { return demo.load(); }
}
