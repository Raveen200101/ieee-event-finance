package lk.sliit.ieee.eventfinance.config;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.time.Duration;
import java.util.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
    @Bean KeyPair jwtKeyPair(JwtProperties properties) throws Exception {
        if (properties.privateKeyPath() == null || properties.privateKeyPath().isBlank() || properties.publicKeyPath() == null || properties.publicKeyPath().isBlank()) throw new IllegalStateException("Set JWT_PRIVATE_KEY_PATH and JWT_PUBLIC_KEY_PATH to external RSA PEM files");
        var factory = KeyFactory.getInstance("RSA");
        var privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(pem(properties.privateKeyPath(), "PRIVATE KEY")));
        var publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(pem(properties.publicKeyPath(), "PUBLIC KEY")));
        if (publicKey.getModulus().bitLength() < 2048 || !publicKey.getModulus().equals(privateKey.getModulus())) throw new IllegalStateException("RSA keys must match and be at least 2048 bits");
        return new KeyPair(publicKey, privateKey);
    }
    private byte[] pem(String path, String label) throws Exception {
        String content = Files.readString(Path.of(path));
        return Base64.getMimeDecoder().decode(content.replace("-----BEGIN " + label + "-----", "").replace("-----END " + label + "-----", ""));
    }
    @Bean JwtEncoder jwtEncoder(KeyPair pair) {
        var key = new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID("chapter-rsa").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
    @Bean JwtDecoder jwtDecoder(KeyPair pair, JwtProperties properties) {
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).signatureAlgorithm(SignatureAlgorithm.RS256).build();
        var time = new JwtTimestampValidator(Duration.ZERO);
        OAuth2TokenValidator<Jwt> claims = token -> {
            boolean valid;
            try {
                var roles = token.getClaimAsStringList("roles");
                valid = token.getSubject() != null && Long.parseLong(token.getSubject()) > 0 && token.getExpiresAt() != null
                        && token.getIssuedAt() != null && !token.getIssuedAt().isAfter(java.time.Instant.now().plusSeconds(30))
                        && token.getAudience().contains(properties.audience()) && roles != null && roles.size() == 1
                        && List.of("COMMITTEE_MEMBER", "VOLUNTEER", "FINANCE_TEAM_LEAD", "LOGISTICS_TEAM_LEAD").contains(roles.get(0)) && token.getClaim("ver") instanceof Number;
            } catch (RuntimeException exception) { valid = false; }
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token claims", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(time, new JwtIssuerValidator(properties.issuer()), claims));
        return decoder;
    }
}
