package lk.sliit.ieee.eventfinance.config;
import lk.sliit.ieee.eventfinance.entity.ChapterUser;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
public class ChapterPrincipal extends User {
    private final long credentialVersion;
    public ChapterPrincipal(ChapterUser user) {
        super(user.getEmail(), user.getPasswordHash(), user.isEnabled(), true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name()));
        this.credentialVersion = user.getCredentialVersion();
    }
    public long credentialVersion() { return credentialVersion; }
}
