package com.musicapp.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class YouTubeService {
    private static final String APPLICATION_NAME = "MusicBuddy";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final long MAX_RESULTS = 10;
    
    private final YouTube youtube;
    private final String apiKey;
    private List<String> currentPlaylist;
    private int currentIndex = -1;
    private final Map<String, String> moodSearchQueries;
    
    public YouTubeService() {
        this.currentPlaylist = new ArrayList<>();
        this.moodSearchQueries = new HashMap<>();
        initializeMoodQueries();
        
        try {
            // Load API key
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    throw new RuntimeException("config.properties not found");
                }
                props.load(input);
            }
            
            this.apiKey = props.getProperty("youtube.api.key");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new RuntimeException("YouTube API key not found in config.properties");
            }
            
            // Initialize YouTube client
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            youtube = new YouTube.Builder(httpTransport, JSON_FACTORY, null)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize YouTube service", e);
        }
    }
    
    private void initializeMoodQueries() {
        moodSearchQueries.put("happy", "happy upbeat music playlist");
        moodSearchQueries.put("sad", "sad emotional music playlist");
        moodSearchQueries.put("energetic", "energetic workout music playlist");
        moodSearchQueries.put("relaxed", "relaxing chill music playlist");
        moodSearchQueries.put("focused", "focus study music playlist");
    }
    
    public CompletableFuture<String> loadPlaylistForMood(String mood) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String searchQuery = moodSearchQueries.getOrDefault(mood.toLowerCase(), "music playlist");
                currentPlaylist = searchYouTube(searchQuery);
                currentIndex = 0;
                return currentPlaylist.isEmpty() ? null : currentPlaylist.get(currentIndex);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load playlist: " + e.getMessage(), e);
            }
        });
    }
    
    private List<String> searchYouTube(String query) throws IOException {
        List<String> videoIds = new ArrayList<>();
        
        YouTube.Search.List request = youtube.search()
                .list(List.of("id", "snippet"))
                .setKey(apiKey)
                .setQ(query)
                .setType(List.of("video"))
                .setVideoCategoryId("10") // Music category
                .setMaxResults(MAX_RESULTS);
        
        SearchListResponse response = request.execute();
        List<SearchResult> items = response.getItems();
        
        if (items != null) {
            for (SearchResult result : items) {
                if (result.getId() != null && result.getId().getVideoId() != null) {
                    videoIds.add(result.getId().getVideoId());
                }
            }
        }
        
        return videoIds;
    }
    
    public CompletableFuture<String> getNextVideo() {
        return CompletableFuture.supplyAsync(() -> {
            if (currentPlaylist.isEmpty() || currentIndex >= currentPlaylist.size() - 1) {
                return null;
            }
            currentIndex++;
            return currentPlaylist.get(currentIndex);
        });
    }
    
    public CompletableFuture<String> getPreviousVideo() {
        return CompletableFuture.supplyAsync(() -> {
            if (currentPlaylist.isEmpty() || currentIndex <= 0) {
                return null;
            }
            currentIndex--;
            return currentPlaylist.get(currentIndex);
        });
    }
}