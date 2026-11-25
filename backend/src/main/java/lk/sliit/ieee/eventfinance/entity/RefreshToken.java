package lk.sliit.ieee.eventfinance.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private ChapterUser user;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false, length = 36) private String familyId;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant expiresAt;
    private Instant revokedAt;
    protected RefreshToken() {}
    public RefreshToken(ChapterUser user, String hash, String family, Instant now, Instant expires) {
        this.user = user; tokenHash = hash; familyId = family; createdAt = now; expiresAt = expires;
    }
    public ChapterUser getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public String getFamilyId() { return familyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
}
