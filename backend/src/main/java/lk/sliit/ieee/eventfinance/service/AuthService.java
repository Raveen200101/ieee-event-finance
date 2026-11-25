package lk.sliit.ieee.eventfinance.service;
import java.nio.charset.StandardCharsets;
import lk.sliit.ieee.eventfinance.config.JwtProperties;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.repository.ChapterUserRepository;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
    public record Result(LoginResponse response, String refreshToken) {}
    private final AuthenticationManager manager;
    private final ChapterUserRepository users;
    private final RefreshTokenService refresh;
    private final JwtService jwt;
    private final JwtProperties properties;
    public AuthService(AuthenticationManager manager, ChapterUserRepository users, RefreshTokenService refresh, JwtService jwt, JwtProperties properties) {
        this.manager = manager; this.users = users; this.refresh = refresh; this.jwt = jwt; this.properties = properties;
    }
    public Result login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new BadCredentialsException("Invalid credentials");
        var auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        var principal = (lk.sliit.ieee.eventfinance.config.ChapterPrincipal) auth.getPrincipal();
        return result(refresh.issue(users.findByEmail(auth.getName()).orElseThrow(() -> new BadCredentialsException("Invalid credentials")).getId(), principal.credentialVersion()));
    }
    public Result refresh(String token) { return result(refresh.rotate(token)); }
    private Result result(RefreshTokenService.Issued issued) {
        return new Result(new LoginResponse(jwt.issue(issued.user()), "Bearer", properties.accessSeconds(), issued.user().getRole(), UserResponse.from(issued.user())), issued.rawToken());
    }
}
