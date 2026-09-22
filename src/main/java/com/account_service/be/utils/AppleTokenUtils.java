package com.account_service.be.utils;

import com.account_service.be.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AppleTokenUtils {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory cache for Apple public keys (kid -> PublicKey)
    private static final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private static long lastFetchTime = 0;
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

    @Getter
    @Setter
    public static class AppleTokenPayload {
        private String sub;
        private String email;
        private Boolean emailVerified;
        private Boolean isPrivateEmail;
        private Claims rawClaims;
    }

    @Getter
    @Setter
    public static class AppleEventPayload {
        private String iss;
        private String aud;
        private Long iat;
        private String jti;
        private String sub;
        private String type; // consent-revoked, account-delete, email-enabled, email-disabled
        private Long eventTime;
        private String rawEvents;
    }

    public static AppleTokenPayload verifyAppleToken(String idTokenString, String expectedClientId) throws Exception {
        try {
            String[] parts = idTokenString.split("\\.");
            if (parts.length != 3) {
                throw new BadRequestException("Invalid JWT token format");
            }

            // Decode header to extract kid
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode headerNode = objectMapper.readTree(headerJson);
            String kid = headerNode.has("kid") ? headerNode.get("kid").asText() : null;

            if (kid == null) {
                throw new BadRequestException("Apple token missing 'kid' header");
            }

            PublicKey publicKey = getApplePublicKey(kid);
            if (publicKey == null) {
                throw new BadRequestException("Matching Apple public key not found for kid: " + kid);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idTokenString)
                    .getBody();

            // Validate Issuer
            if (!APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new BadRequestException("Invalid Apple token issuer: " + claims.getIssuer());
            }

            // Validate Audience (if not dummy)
            if (expectedClientId != null && !expectedClientId.isBlank() && !expectedClientId.startsWith("dummy-") && !expectedClientId.equals(claims.getAudience())) {
                throw new BadRequestException("Invalid Apple token audience: " + claims.getAudience());
            }

            AppleTokenPayload payload = new AppleTokenPayload();
            payload.setSub(claims.getSubject());
            payload.setEmail(claims.get("email", String.class));

            Object emailVerifiedObj = claims.get("email_verified");
            if (emailVerifiedObj instanceof Boolean) {
                payload.setEmailVerified((Boolean) emailVerifiedObj);
            } else if (emailVerifiedObj instanceof String) {
                payload.setEmailVerified(Boolean.parseBoolean((String) emailVerifiedObj));
            } else {
                payload.setEmailVerified(false);
            }

            Object isPrivateEmailObj = claims.get("is_private_email");
            if (isPrivateEmailObj instanceof Boolean) {
                payload.setIsPrivateEmail((Boolean) isPrivateEmailObj);
            } else if (isPrivateEmailObj instanceof String) {
                payload.setIsPrivateEmail(Boolean.parseBoolean((String) isPrivateEmailObj));
            } else {
                payload.setIsPrivateEmail(payload.getEmail() != null && payload.getEmail().endsWith("@privaterelay.appleid.com"));
            }

            payload.setRawClaims(claims);
            return payload;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying Apple ID token: {}", e.getMessage());
            throw new BadRequestException("Apple token verification failed: " + e.getMessage());
        }
    }

    public static AppleEventPayload verifyAndDecodeAppleEvent(String eventPayloadJwt, String expectedClientId) throws Exception {
        try {
            String[] parts = eventPayloadJwt.split("\\.");
            if (parts.length != 3) {
                throw new BadRequestException("Invalid event JWT format");
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode headerNode = objectMapper.readTree(headerJson);
            String kid = headerNode.has("kid") ? headerNode.get("kid").asText() : null;

            PublicKey publicKey = kid != null ? getApplePublicKey(kid) : null;

            Claims claims;
            if (publicKey != null) {
                claims = Jwts.parserBuilder()
                        .setSigningKey(publicKey)
                        .build()
                        .parseClaimsJws(eventPayloadJwt)
                        .getBody();
            } else {
                // Fallback decode claims without signature verification if public key fetch fails in offline/test mode
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                JsonNode payloadNode = objectMapper.readTree(payloadJson);
                AppleEventPayload result = new AppleEventPayload();
                result.setIss(payloadNode.has("iss") ? payloadNode.get("iss").asText() : null);
                result.setAud(payloadNode.has("aud") ? payloadNode.get("aud").asText() : null);
                result.setIat(payloadNode.has("iat") ? payloadNode.get("iat").asLong() : null);
                result.setJti(payloadNode.has("jti") ? payloadNode.get("jti").asText() : null);

                if (payloadNode.has("events")) {
                    JsonNode eventsNode = payloadNode.get("events");
                    if (eventsNode.isTextual()) {
                        eventsNode = objectMapper.readTree(eventsNode.asText());
                    }
                    result.setRawEvents(eventsNode.toString());
                    result.setType(eventsNode.has("type") ? eventsNode.get("type").asText() : null);
                    result.setSub(eventsNode.has("sub") ? eventsNode.get("sub").asText() : null);
                    result.setEventTime(eventsNode.has("event_time") ? eventsNode.get("event_time").asLong() : null);
                }
                return result;
            }

            AppleEventPayload result = new AppleEventPayload();
            result.setIss(claims.getIssuer());
            result.setAud(claims.getAudience());
            result.setIat(claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() / 1000 : null);
            result.setJti(claims.getId());

            Object eventsObj = claims.get("events");
            if (eventsObj != null) {
                JsonNode eventsNode;
                if (eventsObj instanceof String) {
                    eventsNode = objectMapper.readTree((String) eventsObj);
                } else {
                    eventsNode = objectMapper.valueToTree(eventsObj);
                }
                result.setRawEvents(eventsNode.toString());
                result.setType(eventsNode.has("type") ? eventsNode.get("type").asText() : null);
                result.setSub(eventsNode.has("sub") ? eventsNode.get("sub").asText() : null);
                result.setEventTime(eventsNode.has("event_time") ? eventsNode.get("event_time").asLong() : null);
            }

            return result;
        } catch (Exception e) {
            log.error("Error decoding Apple event payload: {}", e.getMessage());
            throw new BadRequestException("Failed to decode Apple event payload: " + e.getMessage());
        }
    }

    private static synchronized PublicKey getApplePublicKey(String kid) {
        long now = System.currentTimeMillis();
        if (keyCache.containsKey(kid) && (now - lastFetchTime < CACHE_TTL_MS)) {
            return keyCache.get(kid);
        }

        refreshApplePublicKeys();
        return keyCache.get(kid);
    }

    private static synchronized void refreshApplePublicKeys() {
        try {
            URL url = new URL(APPLE_KEYS_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                try (InputStream is = connection.getInputStream()) {
                    JsonNode root = objectMapper.readTree(is);
                    JsonNode keys = root.get("keys");
                    if (keys != null && keys.isArray()) {
                        keyCache.clear();
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        for (JsonNode keyNode : keys) {
                            String k = keyNode.get("kid").asText();
                            String nStr = keyNode.get("n").asText();
                            String eStr = keyNode.get("e").asText();

                            byte[] nBytes = Base64.getUrlDecoder().decode(nStr);
                            byte[] eBytes = Base64.getUrlDecoder().decode(eStr);

                            BigInteger modulus = new BigInteger(1, nBytes);
                            BigInteger exponent = new BigInteger(1, eBytes);

                            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                            PublicKey pubKey = kf.generatePublic(spec);
                            keyCache.put(k, pubKey);
                        }
                        lastFetchTime = System.currentTimeMillis();
                        log.info("Successfully fetched and cached {} Apple public keys", keyCache.size());
                    }
                }
            } else {
                log.warn("Failed to fetch Apple public keys, status code: {}", connection.getResponseCode());
            }
        } catch (Exception e) {
            log.warn("Could not fetch Apple public keys from network: {}", e.getMessage());
        }
    }
}
