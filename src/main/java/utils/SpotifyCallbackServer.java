package com.musicapp.utils;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import com.musicapp.services.MusicPlayerService;
import com.musicapp.models.Song;

public class SpotifyCallbackServer {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyCallbackServer.class);

    public CompletableFuture<String> getAuthorizationCode(Consumer<String> codeHandler) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Spotify Authorization");
            dialog.setHeaderText("Enter the authorization code");
            dialog.setContentText("After authorizing in the browser, copy the code from the URL (after 'code=') and paste it here:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().isEmpty()) {
                String code = result.get().trim();
                logger.info("Received authorization code from user input");
                future.complete(code);
                codeHandler.accept(code);
            } else {
                logger.error("No authorization code provided");
                future.completeExceptionally(new RuntimeException("No authorization code provided"));
            }
        });
        
        return future;
    }

    private void updateSongInfo(Song song) {
        System.out.println("MainViewController.updateSongInfo called with: " + (song != null ? song.getTitle() : "null"));
        // ... rest of your code
    }

    public void refreshUI() {
        Platform.runLater(() -> updateSongInfo(MusicPlayerService.getInstance().getCurrentSong()));
    }
} 