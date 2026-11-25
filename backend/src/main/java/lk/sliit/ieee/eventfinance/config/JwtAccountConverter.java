package lk.sliit.ieee.eventfinance.config;
import java.util.*;
import lk.sliit.ieee.eventfinance.repository.ChapterUserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
@Component
public class JwtAccountConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final ChapterUserRepository users;
    public JwtAccountConverter(ChapterUserRepository users) { this.users = users; }
    @Override public AbstractAuthenticationToken convert(Jwt jwt) {
        var user = users.findById(Long.valueOf(jwt.getSubject())).filter(u -> u.isEnabled()
                && u.getCredentialVersion() == ((Number) jwt.getClaim("ver")).longValue()
                && jwt.getClaimAsStringList("roles").equals(List.of(u.getRole().name())))
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        if (user.isAccountAdmin()) authorities.add(new SimpleGrantedAuthority("ACCOUNT_ADMIN"));
        return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
    }
}
