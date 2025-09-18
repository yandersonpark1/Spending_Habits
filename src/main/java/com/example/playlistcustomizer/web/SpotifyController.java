package com.example.playlistcustomizer.web;

import com.example.playlistcustomizer.spotify.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam String q,
                                                            @RequestParam(defaultValue = "10") int limit) {
        var results = spotifyService.searchTracks(q, limit);
        return ResponseEntity.ok(results);
    }
}
