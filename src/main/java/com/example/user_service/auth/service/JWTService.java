package com.example.user_service.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.user_service.user.models.User;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.JsonNodeException;

@Service
public class JWTService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretKey;
    private final String issuer;
    private final long accessTtlSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public JWTService(
        @Value("${security.jwt.secret:dev-secret-change-me}") String secret,
        @Value("${security.jwt.issuer:user-service}") String issuer,
        @Value("${security.jwt.access-ttl-seconds:900}") long accessTtlSeconds
    ) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
        this.accessTtlSeconds = accessTtlSeconds;
    }

    public String generateAccessToken(User user) {
        long now = Instant.now().getEpochSecond();
        long exp = now + accessTtlSeconds;

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", issuer);
        payload.put("sub", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole());
        payload.put("iat", now);
        payload.put("exp", exp);

        String headerJson = writeJson(header);
        String payloadJson = writeJson(payload);
        String headerEncoded = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadEncoded = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = headerEncoded + "." + payloadEncoded;
        String signature = base64UrlEncode(hmacSha256(unsignedToken));
        return unsignedToken + "." + signature;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonNodeException e) {
            throw new IllegalStateException("Failed to serialize JWT payload", e);
        }
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
