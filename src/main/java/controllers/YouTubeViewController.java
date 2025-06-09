package com.musicapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import com.musicapp.services.YouTubeService;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import java.util.Optional;

public class YouTubeViewController {
    @FXML private WebView youtubeWebView;
    @FXML private ComboBox<String> moodComboBox;
    @FXML private Button playButton;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private VBox loadingBox;
    @FXML private Label loadingLabel;
    @FXML private VBox errorBox;
    @FXML private Label errorLabel;
    @FXML private FontIcon playButtonIcon;
    
    private YouTubeService youtubeService;
    private boolean isPlaying = false;
    
    @FXML
    public void initialize() {
        youtubeService = new YouTubeService();
        
        // Initialize mood options
        moodComboBox.getItems().addAll(
            "Happy", "Sad", "Energetic", "Relaxed", "Focused"
        );
        
        // Set default mood
        moodComboBox.setValue("Happy");
        
        // Hide error and loading states initially
        loadingBox.setVisible(false);
        errorBox.setVisible(false);
        
        // Setup button handlers
        playButton.setOnAction(e -> handlePlayButton());
        previousButton.setOnAction(e -> handlePreviousButton());
        nextButton.setOnAction(e -> handleNextButton());
        
        // Disable navigation buttons initially
        previousButton.setDisable(true);
        nextButton.setDisable(true);
        
        // Configure WebView
        youtubeWebView.setContextMenuEnabled(false);
        youtubeWebView.getEngine().setJavaScriptEnabled(true);
        
        // Set initial background
        loadEmptyPlayer();
    }
    
    private void handlePlayButton() {
        if (moodComboBox.getValue() == null) {
            showError("Please select a mood first");
            return;
        }
        
        if (!isPlaying) {
            startPlaying();
        } else {
            pausePlaying();
        }
    }
    
    private void startPlaying() {
        showLoading("Loading your " + moodComboBox.getValue() + " playlist...");
        
        try {
            youtubeService.loadPlaylistForMood(moodComboBox.getValue())
                .thenAccept(videoId -> {
                    Platform.runLater(() -> {
                        hideLoading();
                        if (videoId != null) {
                            loadVideo(videoId);
                            updatePlayState(true);
                            previousButton.setDisable(false);
                            nextButton.setDisable(false);
                        } else {
                            showError("No videos found for this mood");
                        }
                    });
                })
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        showError(getErrorMessage(error));
                    });
                    return null;
                });
        } catch (Exception e) {
            showError(getErrorMessage(e));
        }
    }
    
    private void pausePlaying() {
        // Execute JavaScript to pause the video
        youtubeWebView.getEngine().executeScript("document.querySelector('iframe').contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":\"\"}', '*');");
        updatePlayState(false);
    }
    
    private void updatePlayState(boolean playing) {
        isPlaying = playing;
        playButtonIcon.setIconLiteral(playing ? "fas-pause" : "fas-play");
    }
    
    private void handlePreviousButton() {
        showLoading("Loading previous song...");
        youtubeService.getPreviousVideo()
            .thenAccept(videoId -> {
                Platform.runLater(() -> {
                    hideLoading();
                    if (videoId != null) {
                        loadVideo(videoId);
                        updatePlayState(true);
                    } else {
                        showError("No previous song available");
                    }
                });
            })
            .exceptionally(error -> {
                Platform.runLater(() -> showError(getErrorMessage(error)));
                return null;
            });
    }
    
    private void handleNextButton() {
        showLoading("Loading next song...");
        youtubeService.getNextVideo()
            .thenAccept(videoId -> {
                Platform.runLater(() -> {
                    hideLoading();
                    if (videoId != null) {
                        loadVideo(videoId);
                        updatePlayState(true);
                    } else {
                        showError("No more songs in the playlist");
                    }
                });
            })
            .exceptionally(error -> {
                Platform.runLater(() -> showError(getErrorMessage(error)));
                return null;
            });
    }
    
    @FXML
    private void handleRetryButton() {
        hideError();
        if (isPlaying) {
            startPlaying();
        }
    }
    
    @FXML
    private void handleHelpButton() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("YouTube Player Help");
        alert.setContentText("""
            If you're seeing an error, here are some common solutions:
            
            1. Check your internet connection
            2. Make sure the YouTube API key is properly configured
            3. Try selecting a different mood
            4. The video might have been removed or made private
            
            If the problem persists, try restarting the application.
            """);
        
        // Style the alert dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/youtube.css").toExternalForm());
        dialogPane.getStyleClass().add("alert-dialog");
        
        alert.showAndWait();
    }
    
    private void loadEmptyPlayer() {
        String emptyHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        margin: 0;
                        background-color: #181818;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        height: 100vh;
                        font-family: 'Segoe UI', Arial, sans-serif;
                    }
                    .placeholder {
                        color: #B3B3B3;
                        font-size: 16px;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="placeholder">
                    Select a mood and click Play to start listening
                </div>
            </body>
            </html>
            """;
        youtubeWebView.getEngine().loadContent(emptyHtml);
    }
    
    private void loadVideo(String videoId) {
        hideError();
        String embedHtml = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        margin: 0;
                        background-color: #181818;
                        overflow: hidden;
                    }
                    .video-container {
                        position: relative;
                        padding-bottom: 56.25%%;
                        height: 0;
                        overflow: hidden;
                        background: #000;
                    }
                    .video-container iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%%;
                        height: 100%%;
                        border: none;
                    }
                </style>
            </head>
            <body>
                <div class="video-container">
                    <iframe
                        src="https://www.youtube.com/embed/%s?enablejsapi=1&autoplay=1&rel=0&modestbranding=1&controls=1"
                        frameborder="0"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowfullscreen>
                    </iframe>
                </div>
                <script>
                    // Handle player errors
                    window.addEventListener('error', function(e) {
                        parent.postMessage('error', '*');
                    });
                    
                    // Handle messages from the iframe
                    window.addEventListener('message', function(event) {
                        if (event.data === 'error') {
                            parent.postMessage('error', '*');
                        }
                    });
                </script>
            </body>
            </html>
            """, videoId);
        
        // Set up WebView error handling
        youtubeWebView.getEngine().setOnError(event -> {
            showError("Failed to load video. Please check your internet connection.");
        });
        
        // Handle JavaScript errors and messages
        youtubeWebView.getEngine().setOnAlert(event -> {
            if (event.getData().equals("error")) {
                showError("Failed to load video. The video might be unavailable.");
            }
        });
        
        youtubeWebView.getEngine().loadContent(embedHtml);
    }
    
    private void showLoading(String message) {
        youtubeWebView.setVisible(false);
        errorBox.setVisible(false);
        loadingLabel.setText(message);
        loadingBox.setVisible(true);
    }
    
    private void hideLoading() {
        loadingBox.setVisible(false);
        youtubeWebView.setVisible(true);
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            youtubeWebView.setVisible(false);
            loadingBox.setVisible(false);
            errorLabel.setText(message);
            errorBox.setVisible(true);
            
            // Reset play button state
            updatePlayState(false);
        });
    }
    
    private void hideError() {
        errorBox.setVisible(false);
        youtubeWebView.setVisible(true);
    }
    
    private String getErrorMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return "An unknown error occurred";
        }
        
        if (message.contains("403")) {
            return "YouTube API key is invalid or quota exceeded.\nPlease check your API key configuration.";
        } else if (message.contains("404")) {
            return "Video not found or has been removed from YouTube.";
        } else if (message.contains("network")) {
            return "Network error. Please check your internet connection and try again.";
        } else if (message.contains("quota")) {
            return "YouTube API quota exceeded.\nPlease try again later.";
        } else if (message.contains("restricted")) {
            return "This video is not available in your region or requires age verification.";
        }
        
        return "An error occurred: " + message;
    }
}