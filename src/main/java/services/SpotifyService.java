package com.musicapp.services;

import com.musicapp.models.Playlist;
import com.musicapp.models.Song;
import com.musicapp.database.PlaylistDAO;
import com.musicapp.database.SongDAO;
import com.musicapp.utils.SessionManager;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpotifyService {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);
    private static SpotifyService instance;
    
    private final String clientId = "be223b36dbe44fba96929ccc7ddf36f1";
    private final String clientSecret = "cadbeaf5452148028d7e083d4cb1535a";
    private final String redirectUriStr = "http://127.0.0.1:8080/callback";
    private final URI redirectUri = URI.create(redirectUriStr);
    
    private final SpotifyApi spotifyApi;
    private final PlaylistDAO playlistDAO;
    private final SongDAO songDAO;
    private boolean isAuthenticated = false;
    
    private SpotifyService() {
        logger.info("Initializing SpotifyService with clientId: {}, redirectUri: {}", clientId, redirectUriStr);
        spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(redirectUri)
            .build();
        playlistDAO = new PlaylistDAO();
        songDAO = new SongDAO();
    }
    
    public static synchronized SpotifyService getInstance() {
        if (instance == null) {
            instance = new SpotifyService();
        }
        return instance;
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    public String getAuthorizationURL() {
        AuthorizationCodeUriRequest authorizationRequest = spotifyApi.authorizationCodeUri()
            .scope("playlist-read-private playlist-read-collaborative user-library-read")
            .show_dialog(true)
            .build();
        
        String url = authorizationRequest.execute().toString();
        logger.info("Generated Spotify authorization URL: {}", url);
        return url;
    }
    
    public void handleAuthorizationCode(String code) {
        try {
            logger.info("Processing authorization code...");
            AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
            
            try {
                AuthorizationCodeCredentials credentials = authorizationCodeRequest.execute();
                
                // Set access and refresh tokens
                spotifyApi.setAccessToken(credentials.getAccessToken());
                spotifyApi.setRefreshToken(credentials.getRefreshToken());
                
                isAuthenticated = true;
                logger.info("Successfully authenticated with Spotify");
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error during Spotify token exchange", e);
                throw new RuntimeException("Failed to exchange authorization code for tokens: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error during Spotify authentication", e);
            isAuthenticated = false;
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
    
    public CompletableFuture<List<Playlist>> importUserPlaylists() {
        if (!isAuthenticated) {
            CompletableFuture<List<Playlist>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated with Spotify"));
            return future;
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Fetching user playlists from Spotify...");
                var playlistsRequest = spotifyApi.getListOfCurrentUsersPlaylists().build();
                var playlistsPaging = playlistsRequest.execute();
                List<Playlist> importedPlaylists = new ArrayList<>();
                
                int currentUserId = SessionManager.getInstance().getCurrentUser().getId();
                
                for (PlaylistSimplified spotifyPlaylist : playlistsPaging.getItems()) {
                    try {
                        logger.info("Processing playlist: {}", spotifyPlaylist.getName());
                        // Create playlist in database first
                        Playlist playlist = new Playlist();
                        playlist.setName(spotifyPlaylist.getName());
                        playlist.setDescription("Imported from Spotify");
                        playlist.setUserId(currentUserId);
                        playlist.setCoverImage(spotifyPlaylist.getImages().length > 0 ? 
                            spotifyPlaylist.getImages()[0].getUrl() : "");
                        
                        // Save playlist to database
                        boolean playlistCreated = playlistDAO.createPlaylist(playlist);
                        if (!playlistCreated) {
                            logger.error("Failed to create playlist in database: {}", spotifyPlaylist.getName());
                            continue;
                        }
                        
                        // Get playlist tracks
                        var tracksRequest = spotifyApi.getPlaylistsItems(spotifyPlaylist.getId()).build();
                        var tracks = tracksRequest.execute();
                        List<Song> songs = new ArrayList<>();
                        
                        for (PlaylistTrack playlistTrack : tracks.getItems()) {
                            Track track = (Track) playlistTrack.getTrack();
                            if (track != null) {
                                Song song = new Song();
                                song.setTitle(track.getName());
                                song.setArtist(track.getArtists()[0].getName());
                                song.setAlbum(track.getAlbum().getName());
                                song.setDuration((int) track.getDurationMs() / 1000);
                                song.setFilePath("spotify:" + track.getId()); // Store Spotify URI as file path
                                song.setCoverArt(track.getAlbum().getImages().length > 0 ? 
                                    track.getAlbum().getImages()[0].getUrl() : "");
                                
                                // Save song to database
                                int songId = songDAO.addSong(song);
                                if (songId > 0) {
                                    // Add song to playlist in database
                                    playlistDAO.addSongToPlaylist(playlist.getId(), songId);
                                    songs.add(song);
                                }
                            }
                        }
                        
                        playlist.setSongs(songs);
                        importedPlaylists.add(playlist);
                        logger.info("Successfully processed playlist: {} with {} songs", 
                            spotifyPlaylist.getName(), songs.size());
                    } catch (Exception e) {
                        logger.error("Error processing playlist: " + spotifyPlaylist.getName(), e);
                        // Continue with next playlist instead of failing completely
                    }
                }
                
                logger.info("Successfully imported {} playlists", importedPlaylists.size());
                return importedPlaylists;
            } catch (Exception e) {
                logger.error("Error importing Spotify playlists", e);
                throw new RuntimeException("Failed to import playlists: " + e.getMessage());
            }
        });
    }
    
    public boolean removeAllSpotifyPlaylists() {
        int currentUserId = SessionManager.getInstance().getCurrentUser().getId();
        return playlistDAO.deleteSpotifyPlaylists(currentUserId);
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
} 