package com.example.syntheticfit.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Google google = new Google();

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Google getGoogle() { return google; }
    public void setGoogle(Google google) { this.google = google; }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Google {
        private String mapsApiKey = "";
        public String getMapsApiKey() { return mapsApiKey; }
        public void setMapsApiKey(String mapsApiKey) { this.mapsApiKey = mapsApiKey; }
    }
}
