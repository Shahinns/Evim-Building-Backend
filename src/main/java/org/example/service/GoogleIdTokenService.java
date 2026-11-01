package org.example.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class GoogleIdTokenService {

    private final String clientId;

    public GoogleIdTokenService(@Value("${app.oauth2.google.client-id:}") String clientId) {
        this.clientId = clientId != null ? clientId.trim() : "";
    }

    public boolean isConfigured() {
        return !clientId.isEmpty();
    }

    /**
     * @return email and display name from a valid Google ID token
     */
    public Optional<GoogleProfile> verifyAndExtract(String idTokenString) {
        if (!isConfigured() || idTokenString == null || idTokenString.isBlank()) {
            return Optional.empty();
        }
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(clientId)).build();

            GoogleIdToken idToken = verifier.verify(idTokenString.trim());
            if (idToken == null) {
                return Optional.empty();
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }
            boolean verified = Boolean.TRUE.equals(payload.getEmailVerified());
            if (!verified) {
                return Optional.empty();
            }
            String name = (String) payload.get("name");
            return Optional.of(new GoogleProfile(email.toLowerCase(), name != null ? name : ""));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static record GoogleProfile(String email, String fullName) {}
}
