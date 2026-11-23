package com.rovy.opspulse.service;

import com.rovy.opspulse.domain.*;
import com.rovy.opspulse.dto.Requests.*;
import com.rovy.opspulse.dto.Responses.*;
import com.rovy.opspulse.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional
public class OperationsService {
    private final ManagedServiceRepository services;
    private final IncidentRepository incidents;
    private final HealthObservationRepository observations;
    public OperationsService(ManagedServiceRepository services,IncidentRepository incidents,HealthObservationRepository observations){
        this.services=services;this.incidents=incidents;this.observations=observations;
    }
    public ServiceView register(NewService input){
        if(services.existsByServiceKeyIgnoreCase(input.serviceKey()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Service key already exists");
        try {return ServiceView.from(services.saveAndFlush(new ManagedService(input.serviceKey(),input.name().trim(),input.environment().trim())));}
        catch(DataIntegrityViolationException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"Service key already exists");}
    }
    @Transactional(readOnly=true)
    public List<ServiceView> allServices(){return services.findAll().stream().map(ServiceView::from).toList();}
    @Transactional(readOnly=true)
    public ServiceView getService(Long id){return ServiceView.from(requireService(id));}
    public ObservationView recordObservation(Long id,NewObservation input){
        ManagedService service=requireService(id);
        service.setHealth(input.health());
        HealthObservation o=observations.save(new HealthObservation(service,input.health(),input.note()));
        return ObservationView.from(o);
    }
    @Transactional(readOnly=true)
    public List<ObservationView> recentObservations(Long id){requireService(id);return observations.findTop20ByService_IdOrderByObservedAtDesc(id).stream().map(ObservationView::from).toList();}
    public IncidentView openIncident(NewIncident input){return IncidentView.from(incidents.save(new Incident(requireService(input.serviceId()),input.title().trim(),input.severity())));}
    @Transactional(readOnly=true)
    public List<IncidentView> incidentsForService(Long id){requireService(id);return incidents.findByService_IdOrderByOpenedAtDesc(id).stream().map(IncidentView::from).toList();}
    public IncidentView changeIncident(Long id,ChangeIncident input){
        Incident i=incidents.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));
        try {i.transitionTo(input.status());}
        catch(IllegalStateException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());}
        return IncidentView.from(i);
    }
    @Transactional(readOnly=true)
    public Dashboard dashboard(){
        List<ManagedService> list=services.findAll();
        return new Dashboard(list.size(),count(list,HealthState.HEALTHY),count(list,HealthState.DEGRADED),count(list,HealthState.DOWN),count(list,HealthState.UNKNOWN),incidents.countByStatusNot(IncidentStatus.RESOLVED));
    }
    private long count(List<ManagedService> list, HealthState h){return list.stream().filter(s->s.getHealth()==h).count();}
    private ManagedService requireService(Long id){return services.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Service not found"));}
}