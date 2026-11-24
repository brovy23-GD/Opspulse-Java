package com.rovy.opspulse.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** Monitoring is off unless explicitly enabled by the operator. Targets are application configuration, not API inputs. */
@Component
@ConfigurationProperties(prefix = "opspulse.monitor")
public class MonitorSettings {
    private boolean enabled = false;
    private List<Target> targets = new ArrayList<>();
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<Target> getTargets() { return targets; }
    public void setTargets(List<Target> targets) { this.targets = targets; }

    public static class Target {
        private String serviceKey;
        private String name;
        private String url;
        public String getServiceKey() { return serviceKey; }
        public void setServiceKey(String value) { serviceKey = value; }
        public String getName() { return name; }
        public void setName(String value) { name = value; }
        public String getUrl() { return url; }
        public void setUrl(String value) { url = value; }
    }
}
