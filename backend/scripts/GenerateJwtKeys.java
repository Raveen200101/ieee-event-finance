import java.nio.file.*;
import java.security.*;
import java.util.Base64;

/** Run with Java 21: java scripts/GenerateJwtKeys.java .local/jwt-keys */
class GenerateJwtKeys {
    public static void main(String[] args) throws Exception {
        Path directory=Path.of(args.length==0?".local/jwt-keys":args[0]).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path privateKey=directory.resolve("jwt.private.pem"),publicKey=directory.resolve("jwt.public.pem");
        if(Files.exists(privateKey)||Files.exists(publicKey)) throw new IllegalStateException("Keys already exist; refusing to overwrite them.");
        var generator=KeyPairGenerator.getInstance("RSA"); generator.initialize(3072); var pair=generator.generateKeyPair();
        Files.writeString(privateKey,pem("PRIVATE KEY",pair.getPrivate().getEncoded()),StandardOpenOption.CREATE_NEW);
        Files.writeString(publicKey,pem("PUBLIC KEY",pair.getPublic().getEncoded()),StandardOpenOption.CREATE_NEW);
        System.out.println("Private key: "+privateKey+"\nPublic key: "+publicKey);
    }
    private static String pem(String label,byte[] bytes) { return "-----BEGIN "+label+"-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(bytes)+"\n-----END "+label+"-----\n"; }
}
