package lk.sliit.ieee.eventfinance;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
abstract class AuthenticatedApiTest {
    @LocalServerPort protected int authPort;
    @Autowired protected ChapterUserRepository accounts;
    @Autowired protected RoleRepository roles;
    @Autowired protected RefreshTokenRepository refreshTokens;
    @Autowired protected PasswordEncoder passwords;
    protected HttpClient client;
    protected ChapterUser chair;
    protected final Map<HttpClient,String> accessTokens=new ConcurrentHashMap<>();
    protected static final String TEST_PASSWORD="Only-for-tests-Password-42";
    protected static final JsonMapper JSON=JsonMapper.builder().build();
    @BeforeEach void signInFixture() throws Exception {
        chair=accounts.save(new ChapterUser("test-"+UUID.randomUUID()+"@example.invalid","Test admin",passwords.encode(TEST_PASSWORD),roles.findByName(ChapterRole.COMMITTEE_MEMBER).orElseThrow()));
        chair.updateAccess(roles.findByName(ChapterRole.COMMITTEE_MEMBER).orElseThrow(),true,true); chair=accounts.save(chair);
        client=session(); assertEquals(200,login(client,chair.getEmail(),TEST_PASSWORD).statusCode());
    }
    @AfterEach void cleanupFixture() { if(chair!=null) { refreshTokens.deleteAll(refreshTokens.findAll().stream().filter(t -> t.getUser().getId().equals(chair.getId())).toList()); accounts.deleteById(chair.getId()); } }
    protected HttpClient session() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build(); }
    protected HttpResponse<String> login(HttpClient session,String email,String password) throws Exception {
        var response=send(session,"/api/auth/login","POST",JSON.writeValueAsString(Map.of("email",email,"password",password)),true);
        if(response.statusCode()==200) accessTokens.put(session,JSON.readTree(response.body()).get("accessToken").asText());
        return response;
    }
    protected HttpResponse<String> send(HttpClient session,String path,String method,String body,boolean csrf) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+authPort+path)).header("Content-Type","application/json");
        if(accessTokens.containsKey(session)) builder.header("Authorization","Bearer "+accessTokens.get(session));
        if(csrf) token(session,builder);
        return session.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    protected HttpRequest.Builder authenticatedRequest(URI uri) { return HttpRequest.newBuilder(uri).header("Authorization","Bearer "+accessTokens.get(client)); }
    protected void token(HttpClient session,HttpRequest.Builder builder) throws Exception {
        var response=session.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+authPort+"/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
        var token=JSON.readTree(response.body()); builder.header(token.get("headerName").asText(),token.get("token").asText());
    }
    protected String cookie(HttpClient session,String name) {
        return ((CookieManager)session.cookieHandler().orElseThrow()).getCookieStore().getCookies().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow().getValue();
    }
}
