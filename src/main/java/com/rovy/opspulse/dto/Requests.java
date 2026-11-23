package com.rovy.opspulse.dto;
import com.rovy.opspulse.domain.*;
import jakarta.validation.constraints.*;
public final class Requests {
    private Requests() {}
    public record NewService(@NotBlank @Pattern(regexp="[a-zA-Z0-9_-]{2,60}") String serviceKey,
                             @NotBlank @Size(max=120) String name,
                             @NotBlank @Size(max=60) String environment) {}
    public record NewObservation(@NotNull HealthState health, @Size(max=250) String note) {}
    public record NewIncident(@NotNull Long serviceId,@NotBlank @Size(max=180) String title,
                              @NotNull Severity severity) {}
    public record ChangeIncident(@NotNull IncidentStatus status) {}
}