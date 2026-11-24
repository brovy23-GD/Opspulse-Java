package com.rovy.opspulse.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "monitor_checks", indexes = @Index(name = "idx_monitor_service_time", columnList = "service_id,checkedAt"))
public class MonitorCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = false) private ManagedService service;
    @Column(nullable = false) private Instant checkedAt = Instant.now();
    @Column(nullable = false) private boolean successful;
    @Column(nullable = false) private long latencyMs;
    private Integer httpStatus;
    @Column(length = 160) private String detail;
    protected MonitorCheck() {}
    public MonitorCheck(ManagedService service, boolean successful, long latencyMs, Integer httpStatus, String detail) {
        this.service = service;
        this.successful = successful;
        this.latencyMs = latencyMs;
        this.httpStatus = httpStatus;
        this.detail = detail;
    }
    public Long getId() { return id; }
    public ManagedService getService() { return service; }
    public Instant getCheckedAt() { return checkedAt; }
    public boolean isSuccessful() { return successful; }
    public long getLatencyMs() { return latencyMs; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getDetail() { return detail; }
}
