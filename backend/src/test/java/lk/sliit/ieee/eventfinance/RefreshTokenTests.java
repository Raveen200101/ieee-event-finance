package lk.sliit.ieee.eventfinance;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lk.sliit.ieee.eventfinance.entity.RefreshToken;
import lk.sliit.ieee.eventfinance.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@org.springframework.test.context.ContextConfiguration(initializers=JwtTestInitializer.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class RefreshTokenTests extends AuthenticatedApiTest {
    private int rawRefresh(String raw) throws Exception {
        var browser=session();
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+authPort+"/api/auth/refresh"));
        token(browser,builder);
        builder.header("Cookie","CHAPTER-REFRESH="+raw+"; CHAPTER-CSRF="+cookie(browser,"CHAPTER-CSRF"));
        return browser.send(builder.POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode();
    }
    @Test void rotatesHashesAndDetectsReuseWithFamilyRevocation() throws Exception {
        String original=cookie(client,"CHAPTER-REFRESH"); accessTokens.remove(client);
        var response=send(client,"/api/auth/refresh","POST",null,true); assertEquals(200,response.statusCode(),response.body());
        String replacement=cookie(client,"CHAPTER-REFRESH"); assertNotEquals(original,replacement);
        assertTrue(refreshTokens.findAll().stream().anyMatch(t -> t.getTokenHash().equals(RefreshTokenService.hash(original))));
        assertTrue(refreshTokens.findAll().stream().noneMatch(t -> t.getTokenHash().equals(original)));
        assertEquals(401,rawRefresh(original));
        assertEquals(401,rawRefresh(replacement));
    }
    @Test void expiresRevokesAndLogsOutWithoutPretendingAccessJwtIsRevoked() throws Exception {
        String oldAccess=accessTokens.get(client); String raw=cookie(client,"CHAPTER-REFRESH");
        assertEquals(204,send(client,"/api/auth/logout","POST",null,true).statusCode());
        assertEquals(401,rawRefresh(raw));
        assertEquals(200,send(client,"/api/auth/me","GET",null,false).statusCode());
        assertEquals(oldAccess,accessTokens.get(client));
        String expired=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        refreshTokens.save(new RefreshToken(chair,RefreshTokenService.hash(expired),UUID.randomUUID().toString(),Instant.now().minusSeconds(120),Instant.now().minusSeconds(60)));
        assertEquals(401,rawRefresh(expired));
        assertEquals(401,rawRefresh("invalid"));
    }
    @Test void concurrentReplayDoesNotIssueTwoUsableRefreshTokens() throws Exception {
        String raw=cookie(client,"CHAPTER-REFRESH");
        var first=CompletableFuture.supplyAsync(() -> attempt(raw)); var second=CompletableFuture.supplyAsync(() -> attempt(raw));
        var statuses=new ArrayList<>(List.of(first.get(),second.get())); Collections.sort(statuses);
        assertEquals(List.of(200,401),statuses);
        assertTrue(refreshTokens.findAll().stream().filter(t -> t.getUser().getId().equals(chair.getId())).allMatch(t -> t.getRevokedAt()!=null));
    }
    private int attempt(String raw) { try { return rawRefresh(raw); } catch(Exception e) { throw new RuntimeException(e); } }
    @Test void rejectsRefreshForDisabledAccounts() throws Exception {
        String raw=cookie(client,"CHAPTER-REFRESH");
        chair.updateAccess(roles.findByName(lk.sliit.ieee.eventfinance.entity.ChapterRole.COMMITTEE_MEMBER).orElseThrow(),false,true);
        accounts.save(chair);
        assertEquals(401,rawRefresh(raw));
    }
}
