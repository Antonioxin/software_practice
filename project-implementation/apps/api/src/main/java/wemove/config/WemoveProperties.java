package wemove.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wemove")
public record WemoveProperties(Security security, Registration registration, Bootstrap bootstrap) {
    public record Security(String allowedOrigins) {}
    public record Registration(String termsVersion, String privacyVersion) {}
    public record Bootstrap(String adminEmail, String adminPassword, String adminNickname) {}
}
