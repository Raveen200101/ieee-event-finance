package lk.sliit.ieee.eventfinance.service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import lk.sliit.ieee.eventfinance.config.JwtProperties;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.exception.RefreshTokenRejectedException;
import lk.sliit.ieee.eventfinance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RefreshTokenService {
    public record Issued(ChapterUser user, String rawToken) {}
    private final ChapterUserRepository users;
    private final RefreshTokenRepository tokens;
    private final JwtProperties properties;
    private final SecureRandom random = new SecureRandom();
    public RefreshTokenService(ChapterUserRepository users, RefreshTokenRepository tokens, JwtProperties properties) { this.users = users; this.tokens = tokens; this.properties = properties; }
    @Transactional public Issued issue(Long userId, long expectedVersion) {
        var user = users.lockById(userId).filter(ChapterUser::isEnabled)
                .filter(u -> u.getCredentialVersion() == expectedVersion).orElseThrow(RefreshTokenRejectedException::new);
        return create(user, UUID.randomUUID().toString(), Instant.now().plusSeconds(properties.refreshSeconds()));
    }
    private Issued create(ChapterUser user, String family, Instant expires) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.saveAndFlush(new RefreshToken(user, hash(raw), family, Instant.now(), expires));
        return new Issued(user, raw);
    }
    public static String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private RefreshToken locked(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw new RefreshTokenRejectedException();
        String hash = hash(raw);
        Long owner = tokens.ownerOf(hash).orElseThrow(RefreshTokenRejectedException::new);
        users.lockById(owner).orElseThrow(RefreshTokenRejectedException::new);
        return tokens.findByTokenHash(hash).orElseThrow(RefreshTokenRejectedException::new);
    }
    @Transactional(noRollbackFor = RefreshTokenRejectedException.class)
    public Issued rotate(String raw) {
        var token = locked(raw); Instant now = Instant.now();
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(now) || !token.getUser().isEnabled()) {
            tokens.revokeFamily(token.getFamilyId(), now);
            throw new RefreshTokenRejectedException();
        }
        token.revoke(now); tokens.flush();
        // Absolute seven-day family lifetime; rotation does not extend it indefinitely.
        return create(token.getUser(), token.getFamilyId(), token.getExpiresAt());
    }
    @Transactional public void logout(String raw) {
        if (raw == null) return;
        try { var token = locked(raw); tokens.revokeFamily(token.getFamilyId(), Instant.now()); }
        catch (RefreshTokenRejectedException ignored) { /* Idempotent logout for unknown cookies. */ }
    }
}
