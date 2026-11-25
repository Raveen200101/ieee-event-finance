package lk.sliit.ieee.eventfinance;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.util.TestPropertyValues;
public class JwtTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Path KEYS;
    static {
        try {
            KEYS=Files.createTempDirectory("chapter-jwt-tests-");
            var generator=KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); var pair=generator.generateKeyPair();
            Files.writeString(KEYS.resolve("private.pem"),"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----");
            Files.writeString(KEYS.resolve("public.pem"),"-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----");
            KEYS.resolve("private.pem").toFile().deleteOnExit(); KEYS.resolve("public.pem").toFile().deleteOnExit(); KEYS.toFile().deleteOnExit();
        } catch(Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @Override public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of("spring.datasource.url=jdbc:h2:mem:chapterjwt;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
                "spring.flyway.locations=classpath:db/testmigration","app.security.private-key-path="+KEYS.resolve("private.pem"),
                "app.security.public-key-path="+KEYS.resolve("public.pem"),"app.security.cookie-secure=false",
                "APP_BOOTSTRAP_CHAIR_EMAIL=","APP_BOOTSTRAP_CHAIR_PASSWORD=").applyTo(context);
    }
}
