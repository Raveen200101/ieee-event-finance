package lk.sliit.ieee.eventfinance.service;
import java.time.Instant;
import java.util.*;
import lk.sliit.ieee.eventfinance.config.JwtProperties;
import lk.sliit.ieee.eventfinance.entity.ChapterUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    public JwtService(JwtEncoder encoder, JwtProperties properties) { this.encoder = encoder; this.properties = properties; }
    public String issue(ChapterUser user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(user.getId().toString()).issuer(properties.issuer()).audience(List.of(properties.audience()))
                .issuedAt(now).expiresAt(now.plusSeconds(properties.accessSeconds())).id(UUID.randomUUID().toString())
                .claim("roles", List.of(user.getRole().name())).claim("ver", user.getCredentialVersion()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId("chapter-rsa").build(), claims)).getTokenValue();
    }
}
