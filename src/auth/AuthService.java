package auth;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class AuthService {

    private static final String FILE = "data/users.txt";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    // Register a new user
    public static synchronized boolean register(
            String username, String password) {

        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()
                || username.contains(":")) {
            return false;
        }

        try {
            Files.createDirectories(Paths.get("data"));

            if (Files.exists(Paths.get(FILE))) {
                for (String line : Files.readAllLines(Paths.get(FILE))) {
                    String[] parts = line.split(":", 3);

                    if (parts.length == 3 &&
                            parts[0].equals(username)) {
                        return false;
                    }
                }
            }

            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            byte[] hash = hashPassword(password, salt);

            String record = username + ":"
                    + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);

            Files.write(
                    Paths.get(FILE),
                    (record + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            return true;

        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    // Login an existing user
    public static synchronized boolean login(
            String username, String password) {

        try {
            if (!Files.exists(Paths.get(FILE))) {
                return false;
            }

            for (String line : Files.readAllLines(Paths.get(FILE))) {

                String[] parts = line.split(":", 3);

                if (parts.length == 3 &&
                        parts[0].equals(username)) {

                    byte[] salt = Base64.getDecoder()
                            .decode(parts[1]);

                    byte[] storedHash = Base64.getDecoder()
                            .decode(parts[2]);

                    byte[] enteredHash =
                            hashPassword(password, salt);

                    return MessageDigest.isEqual(
                            storedHash, enteredHash);
                }
            }

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
        }

        return false;
    }

    // Secure password hashing
    private static byte[] hashPassword(
            String password, byte[] salt)
            throws Exception {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } finally {
            spec.clearPassword();
        }
    }
}
