package lk.sliit.ieee.eventfinance;
import java.net.*;
import java.net.http.*;
import java.util.*;
import lk.sliit.ieee.eventfinance.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@org.springframework.test.context.ContextConfiguration(initializers=JwtTestInitializer.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiTests extends AuthenticatedApiTest {
    private final List<Long> created=new ArrayList<>();
    @AfterEach void cleanupUsers() { for(Long id:created) { refreshTokens.deleteAll(refreshTokens.findAll().stream().filter(t -> t.getUser().getId().equals(id)).toList()); accounts.deleteById(id); } }
    private ChapterUser account(ChapterRole role) {
        var user=accounts.save(new ChapterUser("auth-"+UUID.randomUUID()+"@example.invalid","Test user",passwords.encode(TEST_PASSWORD),roles.findByName(role).orElseThrow())); created.add(user.getId()); return user;
    }
    @Test void authenticatesBothRolesAndRejectsBadLogins() throws Exception {
        var volunteer=account(ChapterRole.VOLUNTEER); var browser=session();
        var response=login(browser,volunteer.getEmail().toUpperCase(Locale.ROOT),TEST_PASSWORD);
        assertEquals(200,response.statusCode(),response.body());
        var json=JSON.readTree(response.body()); assertEquals("VOLUNTEER",json.get("role").asText()); assertEquals("Bearer",json.get("tokenType").asText()); assertEquals(900,json.get("expiresIn").asInt());
        assertFalse(response.body().contains("password")); assertFalse(response.body().contains("tokenHash"));
        assertTrue(response.headers().allValues("Set-Cookie").stream().anyMatch(v -> v.contains("HttpOnly") && v.contains("Path=/api/auth") && v.contains("SameSite=Lax")));
        assertTrue(response.headers().allValues("Set-Cookie").stream().noneMatch(v -> v.contains("JSESSIONID")));
        assertEquals(401,login(session(),volunteer.getEmail(),"incorrect-password").statusCode());
        assertEquals(401,login(session(),"nobody@example.invalid",TEST_PASSWORD).statusCode());
        assertEquals(400,login(session(),"member@example.invalid","a".repeat(73)).statusCode());
        assertEquals(400,login(session(),"bad-email",TEST_PASSWORD).statusCode());
        volunteer.updateAccess(roles.findByName(ChapterRole.VOLUNTEER).orElseThrow(),false,false); accounts.save(volunteer);
        assertEquals(401,login(session(),volunteer.getEmail(),TEST_PASSWORD).statusCode());
        assertEquals(401,send(browser,"/api/auth/me","GET",null,false).statusCode());
    }
    @Test void enforcesEndpointAndAccountAdministrationPermissions() throws Exception {
        var volunteer=account(ChapterRole.VOLUNTEER); var member=session(); assertEquals(200,login(member,volunteer.getEmail(),TEST_PASSWORD).statusCode());
        assertEquals(200,send(member,"/api/events","GET",null,false).statusCode());
        assertEquals(403,send(member,"/api/users","GET",null,false).statusCode());
        assertEquals(403,send(member,"/api/events","POST","{}",false).statusCode());
        for(String endpoint:List.of("budget","expenses","incomes","purchase-groups")) {
            assertEquals(403,send(member,"/api/events/0/"+endpoint,"GET",null,false).statusCode());
            assertEquals(403,send(member,"/api/events/0/"+endpoint,"POST","{}",false).statusCode());
        }
        assertEquals(403,send(member,"/api/events/0/purchase-groups/1/selection/2","PUT",null,false).statusCode());
        var committee=account(ChapterRole.COMMITTEE_MEMBER); var normal=session(); login(normal,committee.getEmail(),TEST_PASSWORD);
        assertEquals(403,send(normal,"/api/users","GET",null,false).statusCode());
        assertEquals(400,send(normal,"/api/events","POST","{}",false).statusCode());
        assertEquals(403,send(normal,"/api/users/"+committee.getId(),"PUT","{\"role\":\"COMMITTEE_MEMBER\",\"enabled\":true,\"accountAdmin\":true}",false).statusCode());
        assertEquals(401,send(session(),"/api/events","GET",null,false).statusCode());
        assertEquals(403,send(client,"/api/unlisted","GET",null,false).statusCode());
    }
    @Test void createsValidatedHashedAccountsAndRevokesOnAccessChanges() throws Exception {
        String email="new-"+UUID.randomUUID()+"@example.invalid";
        String body=JSON.writeValueAsString(Map.of("email",email,"displayName"," New user ","password",TEST_PASSWORD,"role","COMMITTEE_MEMBER"));
        var response=send(client,"/api/users","POST",body,false); assertEquals(201,response.statusCode(),response.body());
        long id=JSON.readTree(response.body()).get("id").asLong(); created.add(id);
        var user=accounts.findById(id).orElseThrow(); assertTrue(passwords.matches(TEST_PASSWORD,user.getPasswordHash())); assertFalse(user.isAccountAdmin());
        assertEquals(409,send(client,"/api/users","POST",body,false).statusCode());
        assertEquals(400,send(client,"/api/users","POST",body.replace(TEST_PASSWORD,"short"),false).statusCode());
        var browser=session(); login(browser,email,TEST_PASSWORD);
        assertEquals(200,send(client,"/api/users/"+id,"PUT","{\"role\":\"VOLUNTEER\",\"enabled\":true,\"accountAdmin\":false}",false).statusCode());
        assertEquals(401,send(browser,"/api/events","GET",null,false).statusCode());
        accessTokens.remove(browser); assertEquals(401,send(browser,"/api/auth/refresh","POST",null,true).statusCode());
        assertEquals(409,send(client,"/api/users/"+chair.getId(),"PUT","{\"role\":\"VOLUNTEER\",\"enabled\":true,\"accountAdmin\":false}",false).statusCode());
    }
    @Test void changesPasswordAndInvalidatesOldTokens() throws Exception {
        var user=account(ChapterRole.VOLUNTEER); var browser=session(); login(browser,user.getEmail(),TEST_PASSWORD);
        assertEquals(400,send(browser,"/api/auth/password","PUT",JSON.writeValueAsString(Map.of("currentPassword","wrong","newPassword","New-Test-Password-44")),false).statusCode());
        assertEquals(204,send(browser,"/api/auth/password","PUT",JSON.writeValueAsString(Map.of("currentPassword",TEST_PASSWORD,"newPassword","New-Test-Password-44")),false).statusCode());
        assertEquals(401,send(browser,"/api/events","GET",null,false).statusCode());
        accessTokens.remove(browser); assertEquals(401,send(browser,"/api/auth/refresh","POST",null,true).statusCode());
        assertEquals(401,login(session(),user.getEmail(),TEST_PASSWORD).statusCode()); assertEquals(200,login(session(),user.getEmail(),"New-Test-Password-44").statusCode());
    }
    @Test void protectsCookieEndpointsAndRejectsForeignOrigins() throws Exception {
        assertEquals(403,send(session(),"/api/auth/login","POST","{}",false).statusCode());
        assertEquals(403,send(session(),"/api/auth/refresh","POST",null,false).statusCode());
        assertEquals(403,send(session(),"/api/auth/logout","POST",null,false).statusCode());
        var preflight=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+authPort+"/api/auth/login")).header("Origin","https://untrusted.invalid").header("Access-Control-Request-Method","POST").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(403,session().send(preflight,HttpResponse.BodyHandlers.ofString()).statusCode());
        var allowed=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+authPort+"/api/auth/login")).header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
        var result=session().send(allowed,HttpResponse.BodyHandlers.ofString());assertEquals(200,result.statusCode());
        assertEquals("http://localhost:5173",result.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals("true",result.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
    }
}
