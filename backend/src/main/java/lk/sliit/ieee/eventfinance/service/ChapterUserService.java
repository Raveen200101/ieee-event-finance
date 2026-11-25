package lk.sliit.ieee.eventfinance.service;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional(readOnly=true)
public class ChapterUserService {
    private final ChapterUserRepository users; private final RoleRepository roles; private final RefreshTokenRepository tokens; private final PasswordEncoder passwords;
    public ChapterUserService(ChapterUserRepository users,RoleRepository roles,RefreshTokenRepository tokens,PasswordEncoder passwords) { this.users=users; this.roles=roles; this.tokens=tokens; this.passwords=passwords; }
    @PreAuthorize("@access.admin(authentication)")
    public List<UserResponse> list() { return users.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList(); }
    @PreAuthorize("@access.self(authentication,#id)")
    public UserResponse current(Long id) { return UserResponse.from(users.findById(id).filter(ChapterUser::isEnabled).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))); }
    public static void validatePassword(String password) {
        if(password==null || password.isBlank() || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must contain at least 12 characters and at most 72 UTF-8 bytes.");
    }
    private Role role(ChapterRole role) { return roles.findByName(role).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown role.")); }
    private ChapterUser persist(CreateUserRequest request) {
        validatePassword(request.password());
        if(users.existsByEmail(ChapterUser.normalizeEmail(request.email()))) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
        return users.saveAndFlush(new ChapterUser(request.email(),request.displayName(),passwords.encode(request.password()),role(request.role())));
    }
    @Transactional @PreAuthorize("@access.admin(authentication)")
    public UserResponse create(CreateUserRequest request) { return UserResponse.from(persist(request)); }
    /** Trusted startup only: no HTTP endpoint, no reset of existing credentials. */
    @Transactional public void bootstrap(CreateUserRequest request) {
        if(users.count()!=0) return;
        var user=persist(request); user.updateAccess(role(ChapterRole.COMMITTEE_MEMBER),true,true);
    }
    @Transactional @PreAuthorize("@access.admin(authentication)")
    public UserResponse update(Long id,UpdateUserAccessRequest request) {
        var accounts=users.lockAllForAccessUpdate();
        var user=accounts.stream().filter(u -> u.getId().equals(id)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found."));
        if(request.accountAdmin() && request.role()!=ChapterRole.COMMITTEE_MEMBER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Account administrators must be committee members.");
        boolean removesAdmin=user.isEnabled() && user.isAccountAdmin() && (!request.enabled() || !request.accountAdmin());
        long admins=accounts.stream().filter(u -> u.isEnabled() && u.isAccountAdmin()).count();
        if(removesAdmin && admins<=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Keep at least one active account administrator.");
        user.updateAccess(role(request.role()),request.enabled(),request.accountAdmin());
        tokens.revokeUser(id,Instant.now());
        return UserResponse.from(user);
    }
    @Transactional @PreAuthorize("@access.self(authentication,#id)")
    public void changePassword(Long id,ChangePasswordRequest request) {
        validatePassword(request.newPassword());
        var user=users.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(request.currentPassword().getBytes(StandardCharsets.UTF_8).length>72 || !passwords.matches(request.currentPassword(),user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect.");
        user.changePassword(passwords.encode(request.newPassword())); tokens.revokeUser(id,Instant.now());
    }
}
