package lk.sliit.ieee.eventfinance.config;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app.security")
public record JwtProperties(String issuer, String audience, String privateKeyPath, String publicKeyPath,
        long accessSeconds, long refreshSeconds, List<String> allowedOrigins, boolean cookieSecure, String cookieSameSite) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) throw new IllegalArgumentException("JWT issuer and audience are required");
        if (accessSeconds < 1 || accessSeconds > 3600 || refreshSeconds < accessSeconds || refreshSeconds > 2592000) throw new IllegalArgumentException("Invalid token lifetimes");
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        if (allowedOrigins.stream().anyMatch(o -> o.contains("*"))) throw new IllegalArgumentException("Use explicit frontend origins");
        if (!List.of("Strict", "Lax", "None").contains(cookieSameSite) || (cookieSameSite.equals("None") && !cookieSecure)) throw new IllegalArgumentException("Invalid refresh cookie settings");
    }
}
