package com.account_service.be.utils;

import com.account_service.be.exception.BadRequestException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;

@Slf4j
public class GoogleTokenUtils {

    @Value("${spring.security.oauth2.authorizationserver.client.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.authorizationserver.client.google.client-secret}")
    private String clientSecret;


    private static final String BASE_URL = "${app.base-url}";

    public static GoogleIdToken.Payload verifyGoogleToken(String idTokenString, String CLIENT_ID) throws Exception {
        try {

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance() // ✅ ganti JacksonFactory
            )
                    .setAudience(Collections.singletonList(CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload(); // email, name, dsb
            } else {
                throw new BadRequestException("Invalid ID token.");
            }
        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            throw new Exception("Google login failed. Please try again.");
        }
    }

    public static String exchangeCodeForTokens(
            String code,
            String clientId,
            String clientSecret,
            String redirectUri
    ) throws Exception {
        try {
            HttpTransport transport = new NetHttpTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            GoogleTokenResponse tokenResponse =
                    new GoogleAuthorizationCodeTokenRequest(
                            transport,
                            jsonFactory,
                            "https://oauth2.googleapis.com/token",
                            clientId,
                            clientSecret,
                            code,
                            redirectUri
                    ).execute();

            return tokenResponse.getIdToken();
        } catch (Exception e) {
            throw new Exception("Google login failed. Please try again.");
        }
    }

}
