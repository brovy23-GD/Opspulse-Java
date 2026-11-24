package com.rovy.opspulse.service;

import com.rovy.opspulse.domain.*;
import com.rovy.opspulse.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional, explicitly invoked sample data for a local portfolio demo.
 * Uses reserved demo- service keys and never changes an existing service.
 */
@Service
public class DemoDataService {
    private final ManagedServiceRepository services;
    private final HealthObservationRepository observations;
    private final IncidentRepository incidents;

    public record DemoResult(int createdServices, int createdIncidents, int skippedServices) {}
    private record Sample(String key, String name, String environment, HealthState health, String note) {}
    private record Scenario(String serviceKey, String title, Severity severity, IncidentStatus status) {}

    private static final Sample[] SAMPLES = {
        new Sample("demo-api-gateway", "API Gateway", "production", HealthState.HEALTHY, "Synthetic demo: gateway requests normal"),
        new Sample("demo-auth-service", "Identity & Access", "production", HealthState.HEALTHY, "Synthetic demo: login checks passing"),
        new Sample("demo-orders-api", "Orders API", "production", HealthState.DEGRADED, "Synthetic demo: elevated checkout latency"),
        new Sample("demo-primary-db", "Primary Database", "production", HealthState.HEALTHY, "Synthetic demo: replica lag within threshold"),
        new Sample("demo-redis-cache", "Redis Cache", "production", HealthState.DOWN, "Synthetic demo: connection failures"),
        new Sample("demo-message-queue", "Message Queue", "production", HealthState.DEGRADED, "Synthetic demo: growing consumer backlog"),
        new Sample("demo-cdn-edge", "CDN Edge", "production", HealthState.HEALTHY, "Synthetic demo: successful edge responses"),
        new Sample("demo-worker-pool", "Background Workers", "staging", HealthState.HEALTHY, "Synthetic demo: jobs processing"),
        new Sample("demo-notifications", "Notification Service", "production", HealthState.HEALTHY, "Synthetic demo: deliveries normal"),
        new Sample("demo-analytics", "Analytics Pipeline", "staging", HealthState.UNKNOWN, "Synthetic demo: no recent observation")
    };
    private static final Scenario[] INCIDENTS = {
        new Scenario("demo-redis-cache", "Redis Cache connections unavailable", Severity.CRITICAL, IncidentStatus.INVESTIGATING),
        new Scenario("demo-orders-api", "Orders API checkout latency elevated", Severity.HIGH, IncidentStatus.OPEN),
        new Scenario("demo-message-queue", "Message queue processing delayed", Severity.MEDIUM, IncidentStatus.INVESTIGATING),
        new Scenario("demo-api-gateway", "Intermittent gateway 502 responses", Severity.HIGH, IncidentStatus.RESOLVED),
        new Scenario("demo-notifications", "Notification delivery delay", Severity.LOW, IncidentStatus.RESOLVED)
    };

    public DemoDataService(ManagedServiceRepository services, HealthObservationRepository observations, IncidentRepository incidents) {
        this.services = services;
        this.observations = observations;
        this.incidents = incidents;
    }

    @Transactional
    public DemoResult load() {
        int createdServices = 0;
        int createdIncidents = 0;
        int skippedServices = 0;
        for (Sample sample : SAMPLES) {
            if (services.existsByServiceKeyIgnoreCase(sample.key())) {
                skippedServices++;
                continue;
            }
            ManagedService service = services.saveAndFlush(new ManagedService(sample.key(), sample.name(), sample.environment()));
            service.setHealth(sample.health());
            observations.save(new HealthObservation(service, sample.health(), sample.note()));
            createdServices++;
            for (Scenario scenario : INCIDENTS) {
                if (!scenario.serviceKey().equals(sample.key())) continue;
                Incident incident = new Incident(service, scenario.title(), scenario.severity());
                if (scenario.status() != IncidentStatus.OPEN) incident.transitionTo(scenario.status());
                incidents.save(incident);
                createdIncidents++;
            }
        }
        return new DemoResult(createdServices, createdIncidents, skippedServices);
    }
}
