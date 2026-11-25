package lk.sliit.ieee.eventfinance.entity;
import jakarta.persistence.*;
import java.util.Locale;
import java.time.Instant;

@Entity
@Table(name = "chapter_users")
public class ChapterUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 150) private String displayName;
    @Column(nullable = false, length = 100) private String passwordHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "role_id", nullable = false) private Role role;
    @Column(nullable = false) private boolean accountAdmin;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Column(nullable = false) private boolean enabled = true;
    @Column(nullable = false) private long credentialVersion;
    protected ChapterUser() {}
    public ChapterUser(String email, String displayName, String passwordHash, Role role) {
        this.email = normalizeEmail(email); this.displayName = displayName.strip();
        this.passwordHash = passwordHash; this.role = role;
    }
    public static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    public void updateAccess(Role role, boolean enabled, boolean accountAdmin) {
        this.role = role; this.enabled = enabled; this.accountAdmin = accountAdmin; this.credentialVersion++;
    }
    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public ChapterRole getRole() { return role.getName(); }
    public boolean isAccountAdmin() { return accountAdmin; }
    public boolean isEnabled() { return enabled; }
    public long getCredentialVersion() { return credentialVersion; }
    public void changePassword(String hash) { this.passwordHash = hash; this.credentialVersion++; }
}
