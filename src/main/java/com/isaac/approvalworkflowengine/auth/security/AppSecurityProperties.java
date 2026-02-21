package com.isaac.approvalworkflowengine.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private SecurityMode mode = SecurityMode.LOCAL_AUTH;
    private Jwt jwt = new Jwt();
    private RateLimit rateLimit = new RateLimit();

    public SecurityMode getMode() {
        return mode;
    }

    public void setMode(SecurityMode mode) {
        this.mode = mode;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class Jwt {

        private int accessTokenTtlMinutes = 30;
        private String hmacSecret = "local-dev-signing-key-change-me-1234567890";

        public int getAccessTokenTtlMinutes() {
            return accessTokenTtlMinutes;
        }

        public void setAccessTokenTtlMinutes(int accessTokenTtlMinutes) {
            this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        }

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }
    }

    public static class RateLimit {

        private boolean enabled = true;
        private int authenticatedLimit = 120;
        private int anonymousLimit = 30;
        private long windowSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getAuthenticatedLimit() {
            return authenticatedLimit;
        }

        public void setAuthenticatedLimit(int authenticatedLimit) {
            this.authenticatedLimit = authenticatedLimit;
        }

        public int getAnonymousLimit() {
            return anonymousLimit;
        }

        public void setAnonymousLimit(int anonymousLimit) {
            this.anonymousLimit = anonymousLimit;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
