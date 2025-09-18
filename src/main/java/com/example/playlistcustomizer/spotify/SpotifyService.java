package com.example.playlistcustomizer.spotify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyService {

    private final RestTemplate rest = new RestTemplate();

    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public SpotifyService(
            @Value("${spotify.client-id}") String clientId,
            @Value("${spotify.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private synchronized void ensureToken() {
        if (accessToken == null || Instant.now().isAfter(tokenExpiry.minusSeconds(30))) {
            // request a client_credentials token
            String url = "https://accounts.spotify.com/api/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String creds = clientId + ":" + clientSecret;
            String b64 = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + b64);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = rest.postForEntity(url, req, Map.class);
            Map<String, Object> map = resp.getBody();
            if (map != null) {
                this.accessToken = (String) map.get("access_token");
                Integer exp = (Integer) map.get("expires_in");
                tokenExpiry = Instant.now().plusSeconds(exp != null ? exp : 3600);
            } else {
                throw new RuntimeException("Failed to obtain Spotify token");
            }
        }
    }

    /** Search tracks by a query string (simple wrapper). */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchTracks(String q, int limit) {
        ensureToken();
        String url = UriComponentsBuilder.fromHttpUrl("https://api.spotify.com/v1/search")
                .queryParam("q", q)
                .queryParam("type", "track")
                .queryParam("limit", limit)
                .build().toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = rest.exchange(url, HttpMethod.GET, req, Map.class);
        Map<String, Object> body = resp.getBody();
        if (body == null) return List.of();
        Map<String, Object> tracks = (Map<String, Object>) body.get("tracks");
        if (tracks == null) return List.of();
        return (List<Map<String, Object>>) tracks.get("items");
    }

    // later: add audio-features, recommendations, playlist creation methods
}
