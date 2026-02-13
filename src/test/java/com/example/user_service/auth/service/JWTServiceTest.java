package com.example.user_service.auth.service;

import com.example.user_service.user.models.User;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JWTServiceTest {

    private static final String SECRET = "unit-test-secret";
    private static final String ISSUER = "unit-test-issuer";
    private static final long TTL_SECONDS = 900L;

    @Test
    void generateAccessTokenIncludesExpectedClaimsAndExpiration() throws Exception {
        JWTService jwtService = new JWTService(SECRET, ISSUER, TTL_SECONDS);
        User user = buildUser();
        long before = Instant.now().getEpochSecond();

        String token = jwtService.generateAccessToken(user);

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String header = decodePart(parts[0]);
        String payload = decodePart(parts[1]);

        assertTrue(header.contains("\"alg\":\"HS256\""));
        assertTrue(header.contains("\"typ\":\"JWT\""));
        assertTrue(payload.contains("\"iss\":\"" + ISSUER + "\""));
        assertTrue(payload.contains("\"sub\":\"" + user.getId() + "\""));
        assertTrue(payload.contains("\"email\":\"" + user.getEmail() + "\""));
        assertTrue(payload.contains("\"role\":\"" + user.getRole() + "\""));

        long iat = extractLong(payload, "iat");
        long exp = extractLong(payload, "exp");
        long after = Instant.now().getEpochSecond();

        assertTrue(iat >= before && iat <= after);
        assertEquals(iat + TTL_SECONDS, exp);
    }

    @Test
    void generateAccessTokenCreatesValidHmacSignature() throws Exception {
        JWTService jwtService = new JWTService(SECRET, ISSUER, TTL_SECONDS);
        User user = buildUser();

        String token = jwtService.generateAccessToken(user);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken, SECRET);

        assertEquals(expectedSignature, parts[2]);
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.fromString("0d4e8b2f-5b65-4f4d-b9ef-a4b2dbe4c1de"));
        user.setEmail("user@example.com");
        user.setRole("USER");
        return user;
    }

    private String decodePart(String base64Url) {
        byte[] decoded = Base64.getUrlDecoder().decode(base64Url);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String sign(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private long extractLong(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":(\\d+)");
        Matcher matcher = pattern.matcher(json);
        assertTrue(matcher.find());
        return Long.parseLong(matcher.group(1));
    }
}
