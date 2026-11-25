package lk.sliit.ieee.eventfinance;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import lk.sliit.ieee.eventfinance.config.*;
import lk.sliit.ieee.eventfinance.service.ChapterUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;
@org.springframework.test.context.ContextConfiguration(initializers=JwtTestInitializer.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtValidationTests extends AuthenticatedApiTest {
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired JwtProperties settings;
    @Autowired JwtAccountConverter converter;
    @Autowired ChapterUserService service;
    private String signed(Consumer<JwtClaimsSet.Builder> change) {
        var builder=JwtClaimsSet.builder().subject(chair.getId().toString()).issuer(settings.issuer()).audience(List.of(settings.audience()))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).claim("roles",List.of("COMMITTEE_MEMBER")).claim("ver",chair.getCredentialVersion());
        change.accept(builder);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),builder.build())).getTokenValue();
    }
    private int status(String token) throws Exception {
        return session().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+authPort+"/api/auth/me")).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode();
    }
    @Test void validatesRealSignaturesRequiredClaimsAndAuthorities() throws Exception {
        assertEquals(200,status(accessTokens.get(client)));
        for(String token:List.of("invalid",signed(b -> b.issuer("wrong")),signed(b -> b.audience(List.of("wrong"))),
                signed(b -> b.issuedAt(Instant.now().minusSeconds(120)).expiresAt(Instant.now().minusSeconds(60))),signed(b -> b.subject("not-an-id")),
                signed(b -> b.claim("roles",List.of("ADMIN"))),signed(b -> b.claims(c -> c.remove("roles"))),
                signed(b -> b.claims(c -> c.remove("exp"))),signed(b -> b.claims(c -> c.remove("ver"))),
                signed(b -> b.claim("ver",999999)))) assertEquals(401,status(token),token.substring(0,Math.min(20,token.length())));
        String valid=accessTokens.get(client); var parts=valid.split("\\.");
        String tampered=parts[0]+"."+Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"999\"}".getBytes())+"."+parts[2];
        assertEquals(401,status(tampered));
        String badSignature=parts[0]+"."+parts[1]+"."+(parts[2].startsWith("A")?"B":"A")+parts[2].substring(1);
        assertEquals(401,status(badSignature));
        String unsupportedHeader=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        assertEquals(401,status(unsupportedHeader+"."+parts[1]+"."+parts[2]));
        var authentication=converter.convert(decoder.decode(valid));
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMMITTEE_MEMBER")));
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ACCOUNT_ADMIN")));
    }
    @Test void methodSecurityPreventsOtherUsersProfileAccess() {
        var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(converter.convert(decoder.decode(accessTokens.get(client))));
        SecurityContextHolder.setContext(context);
        try { assertThrows(org.springframework.security.access.AccessDeniedException.class,() -> service.current(chair.getId()+9999)); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
