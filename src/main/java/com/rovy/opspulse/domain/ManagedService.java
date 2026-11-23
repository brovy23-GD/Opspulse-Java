package com.rovy.opspulse.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="managed_services", uniqueConstraints = @UniqueConstraint(columnNames="service_key"))
public class ManagedService {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="service_key",nullable=false,length=60) private String serviceKey;
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false,length=60) private String environment;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private HealthState health = HealthState.UNKNOWN;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    protected ManagedService() {}
    public ManagedService(String serviceKey, String name, String environment) {
        this.serviceKey=serviceKey; this.name=name; this.environment=environment;
    }
    public Long getId(){return id;}
    public String getServiceKey(){return serviceKey;}
    public String getName(){return name;}
    public String getEnvironment(){return environment;}
    public HealthState getHealth(){return health;}
    public Instant getCreatedAt(){return createdAt;}
    public void setHealth(HealthState health){this.health=health;}
}