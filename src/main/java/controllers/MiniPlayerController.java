package com.musicapp.controllers;

import com.musicapp.models.Song;
import com.musicapp.services.MusicPlayerService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.input.TransferMode;
import java.io.File;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class MiniPlayerController implements Initializable {
    @FXML private HBox miniPlayerRoot;
    @FXML private Button miniPlayPauseButton;
    @FXML private Button miniPrevButton;
    @FXML private Button miniNextButton;
    @FXML private Label songTitleLabel;
    @FXML private Label artistLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Slider progressSlider;
    @FXML private ImageView albumArtView;
    @FXML private Button expandButton;
    @FXML private FontIcon miniPlayPauseIcon;
    
    private final MusicPlayerService playerService = MusicPlayerService.getInstance();
    private Stage mainStage;
    private Stage miniStage;
    private final Image defaultAlbumImage;
    private double xOffset, yOffset;
    private MainViewController mainViewController;
    
    public MiniPlayerController() {
        System.out.println("MiniPlayerController: " + this + " MusicPlayerService: " + MusicPlayerService.getInstance());
        URL imageUrl = getClass().getResource("/images/app_icon.png");
        if (imageUrl != null) {
            defaultAlbumImage = new Image(imageUrl.toString());
        } else {
            // Create a 1x1 transparent image as fallback
            defaultAlbumImage = new Image(new ByteArrayInputStream(new byte[]{0}));
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up progress slider
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                playerService.seek(newVal.doubleValue() / 100.0);
            }
        });
        
        // Bind time labels
        currentTimeLabel.textProperty().bind(playerService.currentTimeProperty());
        totalTimeLabel.textProperty().bind(playerService.totalTimeProperty());
        
        // Update progress slider
        playerService.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (!progressSlider.isValueChanging()) {
                Platform.runLater(() -> progressSlider.setValue(newVal.doubleValue() * 100));
            }
        });
        
        // Update play/pause button based on player state
        playerService.playingProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> miniPlayPauseIcon.setIconLiteral(newVal ? "fas-pause" : "fas-play"));
        });
        
        // Set initial play/pause icon state
        miniPlayPauseIcon.setIconLiteral(playerService.playingProperty().get() ? "fas-pause" : "fas-play");
        
        // Update song info when current song changes
        playerService.currentSongProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateSongInfo(newVal));
        });
        
        // Set initial song info
        updateSongInfo(playerService.getCurrentSong());
        
        // Add window drag support
        setupWindowDrag();
        
        // Add drag and drop support
        setupDragAndDrop();
    }

    private void updateSongInfo(Song song) {
        if (song != null) {
            songTitleLabel.setText(song.getTitle());
            artistLabel.setText(song.getArtist());
            if (song.getCoverArt() != null && !song.getCoverArt().isEmpty()) {
                albumArtView.setImage(new Image(song.getCoverArt()));
            } else {
                albumArtView.setImage(defaultAlbumImage);
            }
        } else {
            songTitleLabel.setText("No song playing");
            artistLabel.setText("");
            albumArtView.setImage(defaultAlbumImage);
        }
    }
    
    @FXML
    private void handlePlayPauseButton() {
        if (playerService.getCurrentSong() == null) {
            if (!playerService.getPlaylist().isEmpty()) {
                playerService.playSong(playerService.getPlaylist().get(0));
            }
            return;
        }
        
        if (playerService.playingProperty().get()) {
            playerService.pause();
        } else {
            playerService.play();
        }
    }
    
    @FXML
    private void handlePreviousButton() {
        playerService.playPrevious();
    }
    
    @FXML
    private void handleNextButton() {
        playerService.playNext();
    }
    
    @FXML
    private void handleMaximizeButton() {
        if (miniStage != null && mainStage != null) {
            // Create fade transitions
            FadeTransition fadeOutMini = new FadeTransition(Duration.millis(300), miniPlayerRoot);
            fadeOutMini.setFromValue(1.0);
            fadeOutMini.setToValue(0.0);
            
            // Show main stage first but with opacity 0
            mainStage.show();
            mainStage.getScene().getRoot().setOpacity(0);
            
            // Create fade in for main stage
            FadeTransition fadeInMain = new FadeTransition(Duration.millis(300), mainStage.getScene().getRoot());
            fadeInMain.setFromValue(0.0);
            fadeInMain.setToValue(1.0);
            
            // When mini player fade out is done, close it and fade in main stage
            fadeOutMini.setOnFinished(e -> {
                miniStage.close();
                fadeInMain.play();
                if (mainViewController != null) {
                    mainViewController.maximizeFromMiniPlayer();
                }
            });
            
            // Start the transition
            fadeOutMini.play();
            mainStage.toFront();
        }
    }
    
    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
        
        // Get the current stage
        miniStage = (Stage) miniPlayerRoot.getScene().getWindow();
        miniStage.setAlwaysOnTop(true);
        
        // Position mini player
        miniStage.setX(mainStage.getX() + mainStage.getWidth() - miniPlayerRoot.getPrefWidth());
        miniStage.setY(mainStage.getY() + mainStage.getHeight() - miniPlayerRoot.getPrefHeight());
        
        // Show mini player and hide main stage
        miniStage.show();
        mainStage.hide();
    }

    private boolean isSupportedAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") || 
               name.endsWith(".m4a") || name.endsWith(".aac") ||
               name.endsWith(".ogg");
    }

    public void refreshUI() {
        Platform.runLater(() -> updateSongInfo(MusicPlayerService.getInstance().getCurrentSong()));
    }

    public void setMainViewController(MainViewController controller) {
        this.mainViewController = controller;
    }

    public void updateVolume(double value) {
        if (playerService != null) {
            playerService.volumeProperty().set(value);
        }
    }

    public void updateProgress(double value) {
        if (!progressSlider.isValueChanging()) {
            Platform.runLater(() -> progressSlider.setValue(value));
        }
    }

    private void setupWindowDrag() {
        miniPlayerRoot.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        miniPlayerRoot.setOnMouseDragged(event -> {
            if (miniStage != null) {
                miniStage.setX(event.getScreenX() - xOffset);
                miniStage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    private void setupDragAndDrop() {
        miniPlayerRoot.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        miniPlayerRoot.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                for (File file : files) {
                    if (isSupportedAudioFile(file)) {
                        Song song = new Song();
                        song.setTitle(file.getName().replaceFirst("[.][^.]+$", ""));
                        song.setFilePath(file.getAbsolutePath());
                        song.setArtist("Unknown Artist");
                        
                        playerService.getPlaylist().add(song);
                        playerService.playSong(song);
                        success = true;
                        break;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
} 