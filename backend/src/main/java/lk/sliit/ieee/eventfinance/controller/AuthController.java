package lk.sliit.ieee.eventfinance.controller;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import lk.sliit.ieee.eventfinance.config.JwtProperties;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; private final RefreshTokenService refresh; private final ChapterUserService users; private final JwtProperties properties;
    public AuthController(AuthService auth, RefreshTokenService refresh, ChapterUserService users, JwtProperties properties) { this.auth=auth; this.refresh=refresh; this.users=users; this.properties=properties; }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("headerName",token.getHeaderName(),"token",token.getToken()); }
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var result=auth.login(request); cookie(response,result.refreshToken(),properties.refreshSeconds()); return result.response();
    }
    @PostMapping("/refresh") public LoginResponse refresh(@CookieValue(name="CHAPTER-REFRESH",required=false) String token, HttpServletResponse response) {
        try { var result=auth.refresh(token); cookie(response,result.refreshToken(),properties.refreshSeconds()); return result.response(); }
        catch (org.springframework.security.core.AuthenticationException | lk.sliit.ieee.eventfinance.exception.RefreshTokenRejectedException exception) { cookie(response,"",0); throw exception; }
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name="CHAPTER-REFRESH",required=false) String token, HttpServletResponse response) { refresh.logout(token); cookie(response,"",0); }
    @GetMapping("/me") public UserResponse me(@AuthenticationPrincipal Jwt jwt) { return users.current(Long.valueOf(jwt.getSubject())); }
    @PutMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ChangePasswordRequest request,HttpServletResponse response) { users.changePassword(Long.valueOf(jwt.getSubject()),request); cookie(response,"",0); }
    private void cookie(HttpServletResponse response,String token,long seconds) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("CHAPTER-REFRESH",token).httpOnly(true).secure(properties.cookieSecure()).sameSite(properties.cookieSameSite()).path("/api/auth").maxAge(seconds).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");
    }
}
