package com.rovy.opspulse.api;

import com.rovy.opspulse.dto.Requests.*;
import com.rovy.opspulse.dto.Responses.*;
import com.rovy.opspulse.service.OperationsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OperationsController {
    private final OperationsService service;
    public OperationsController(OperationsService service){this.service=service;}
    @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED)
    public ServiceView register(@Valid @RequestBody NewService request){return service.register(request);}
    @GetMapping("/services") public List<ServiceView> allServices(){return service.allServices();}
    @GetMapping("/services/{id}") public ServiceView service(@PathVariable Long id){return service.getService(id);}
    @PostMapping("/services/{id}/observations") @ResponseStatus(HttpStatus.CREATED)
    public ObservationView observe(@PathVariable Long id,@Valid @RequestBody NewObservation request){return service.recordObservation(id,request);}
    @GetMapping("/services/{id}/observations") public List<ObservationView> history(@PathVariable Long id){return service.recentObservations(id);}
    @PostMapping("/incidents") @ResponseStatus(HttpStatus.CREATED)
    public IncidentView openIncident(@Valid @RequestBody NewIncident request){return service.openIncident(request);}
    @GetMapping("/services/{id}/incidents") public List<IncidentView> incidents(@PathVariable Long id){return service.incidentsForService(id);}
    @PatchMapping("/incidents/{id}") public IncidentView updateIncident(@PathVariable Long id,@Valid @RequestBody ChangeIncident request){return service.changeIncident(id,request);}
    @GetMapping("/dashboard") public Dashboard dashboard(){return service.dashboard();}
}