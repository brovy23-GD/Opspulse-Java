package com.rovy.opspulse.service;

import com.rovy.opspulse.domain.*;
import com.rovy.opspulse.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Opt-in scheduled checks of explicitly configured HTTPS endpoints.
 * Never accepts a URL from an HTTP request. Does not create or resolve incidents.
 */
@Service
public class MonitoringService {
    private static final Pattern KEY = Pattern.compile("[a-zA-Z0-9_-]{2,60}");
    private final MonitorSettings settings;
    private final HttpHealthProbe probe;
    private final ManagedServiceRepository services;
    private final HealthObservationRepository observations;
    private final MonitorCheckRepository checks;
    private final TransactionTemplate transactions;

    public record CheckView(Long id, Long serviceId, String serviceName, boolean successful,
                            long latencyMs, Integer httpStatus, String detail, java.time.Instant checkedAt) {}
    public record MonitorInfo(boolean enabled, int configuredTargets, List<CheckView> recentChecks) {}
    private record Target(URI uri, String serviceKey, String name) {}

    public MonitoringService(MonitorSettings settings, HttpHealthProbe probe,
        ManagedServiceRepository services, HealthObservationRepository observations,
        MonitorCheckRepository checks, org.springframework.transaction.PlatformTransactionManager txManager) {
        this.settings = settings;
        this.probe = probe;
        this.services = services;
        this.observations = observations;
        this.checks = checks;
        this.transactions = new TransactionTemplate(txManager);
    }

    public MonitorInfo info() {
        List<CheckView> result = checks.findTop30ByOrderByCheckedAtDescIdDesc().stream().map(this::view).toList();
        return new MonitorInfo(settings.isEnabled(), settings.getTargets().size(), result);
    }

    @Scheduled(fixedDelayString = "${opspulse.monitor.interval-ms:60000}",
               initialDelayString = "${opspulse.monitor.interval-ms:60000}")
    public void scheduledCheck() {
        if (!settings.isEnabled()) return;
        for (MonitorSettings.Target raw : settings.getTargets()) {
            try {
                // Validation is done even for trusted configuration, limiting the monitor to standard HTTPS endpoints.
                Target target = validate(raw);
                long started = System.nanoTime();
                boolean healthy;
                Integer httpStatus = null;
                String detail;
                try {
                    HttpHealthProbe.Result response = probe.check(target.uri());
                    healthy = response.healthy();
                    httpStatus = response.status();
                    detail = response.detail();
                } catch (Exception ex) {
                    healthy = false;
                    detail = "Connection failed (" + ex.getClass().getSimpleName() + ")";
                }
                long elapsedMs = Math.max(0, (System.nanoTime() - started) / 1_000_000L);
                final boolean ok = healthy;
                final Integer status = httpStatus;
                final String reason = detail;
                transactions.executeWithoutResult(tx -> saveCheck(target, ok, elapsedMs, status, reason));
            } catch (IllegalArgumentException ex) {
                // An invalid target is ignored, not fetched; further targets continue to be checked.
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Skipping invalid monitoring configuration: {}", ex.getMessage());
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Unable to record monitoring check", ex);
            }
        }
    }

    public static Target validate(MonitorSettings.Target raw) {
        if (raw == null || raw.getServiceKey() == null || !KEY.matcher(raw.getServiceKey()).matches()
            || raw.getName() == null || raw.getName().isBlank() || raw.getName().length() > 120
            || raw.getUrl() == null) throw new IllegalArgumentException("Missing or invalid monitor target properties");
        final URI uri;
        try { uri = URI.create(raw.getUrl()); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid monitor URL"); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
            || uri.getHost().isBlank() || uri.getPort() != -1 || uri.getUserInfo() != null
            || uri.getFragment() != null || uri.getRawQuery() != null
            || uri.getHost().equalsIgnoreCase("localhost")
            || uri.getHost().endsWith(".local")
            || uri.getHost().matches("(?i)^(\\d{1,3}\\.){3}\\d{1,3}$")
            || uri.getHost().contains(":"))
            throw new IllegalArgumentException("Only standard HTTPS DNS hostnames without query, credentials or custom ports are supported");
        return new Target(uri, raw.getServiceKey(), raw.getName().trim());
    }

    private void saveCheck(Target target, boolean healthy, long latencyMs, Integer status, String detail) {
        ManagedService service = services.findByServiceKeyIgnoreCase(target.serviceKey())
            .orElseGet(() -> services.saveAndFlush(new ManagedService(target.serviceKey(), target.name(), "monitored")));
        HealthState newHealth = healthy ? HealthState.HEALTHY : HealthState.DOWN;
        service.setHealth(newHealth);
        String note = "HTTP probe: " + detail + " (" + latencyMs + " ms)";
        observations.save(new HealthObservation(service, newHealth, note));
        checks.save(new MonitorCheck(service, healthy, latencyMs, status, detail));
    }

    private CheckView view(MonitorCheck entry) {
        return new CheckView(entry.getId(), entry.getService().getId(), entry.getService().getName(),
            entry.isSuccessful(), entry.getLatencyMs(), entry.getHttpStatus(), entry.getDetail(), entry.getCheckedAt());
    }
}
