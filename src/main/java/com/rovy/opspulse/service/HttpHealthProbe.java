package com.rovy.opspulse.service;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** HTTP GET with a bounded duration, no redirects, and no response body storage. */
@Component
public class HttpHealthProbe {
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    public record Result(boolean healthy, Integer status, String detail) {}
    public Result check(URI endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(5))
            .header("User-Agent", "OpsPulse-HealthMonitor/1.0")
            .GET().build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        int code = response.statusCode();
        return new Result(code >= 200 && code < 300, code, "HTTP " + code);
    }
}
