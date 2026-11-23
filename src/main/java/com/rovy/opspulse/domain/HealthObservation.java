package com.rovy.opspulse.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="health_observations")
public class HealthObservation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.EAGER) @JoinColumn(name="service_id",nullable=false) private ManagedService service;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private HealthState health;
    @Column(nullable=false) private Instant observedAt=Instant.now();
    @Column(length=250) private String note;
    protected HealthObservation() {}
    public HealthObservation(ManagedService service, HealthState health,String note){this.service=service;this.health=health;this.note=note;}
    public Long getId(){return id;}
    public ManagedService getService(){return service;}
    public HealthState getHealth(){return health;}
    public Instant getObservedAt(){return observedAt;}
    public String getNote(){return note;}
}