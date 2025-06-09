package com.musicapp.controllers;

import com.musicapp.services.MusicPlayerService;
import com.musicapp.models.Song;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;

import java.io.File;
import java.util.List;

public class HomeViewController {
    @FXML private ListView<File> songListView;
    @FXML private Button playPauseButton;
    @FXML private FontIcon playPauseIcon;
    @FXML private Slider progressSlider;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Slider volumeSlider;
    
    private final MusicPlayerService playerService = MusicPlayerService.getInstance();
    private boolean isSliderDragging = false;
    private static final Logger logger = LoggerFactory.getLogger(HomeViewController.class);
    
    @FXML
    public void initialize() {
        logger.info("Initializing HomeViewController");
        setupSongListView();
        setupPlayerControls();
        
        // Set initial states
        updatePlayPauseButton(playerService.playingProperty().get());
    }
    
    private void setupSongListView() {
        songListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                }
            }
        });
        
        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                File selectedFile = songListView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    Song song = new Song();
                    song.setTitle(selectedFile.getName().replaceFirst("[.][^.]+$", ""));
                    song.setFilePath(selectedFile.getAbsolutePath());
                    song.setArtist("Unknown Artist");
                    playerService.playSong(song);
                }
            }
        });
    }
    
    private void setupPlayerControls() {
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
            Platform.runLater(() -> updatePlayPauseButton(newVal));
        });
        
        // Set up volume slider
        volumeSlider.valueProperty().bindBidirectional(playerService.volumeProperty());
    }
    
    @FXML
    public void selectMusicFolder() {
        logger.info("Opening folder selection dialog");
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Music Folder");
        
        File selectedDirectory = directoryChooser.showDialog(songListView.getScene().getWindow());
        if (selectedDirectory != null) {
            logger.info("Selected directory: {}", selectedDirectory.getPath());
            loadSongsFromDirectory(selectedDirectory);
        }
    }
    
    private void loadSongsFromDirectory(File directory) {
        songListView.getItems().clear();
        try {
            File[] files = directory.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".mp3") || 
                name.toLowerCase().endsWith(".wav") || 
                name.toLowerCase().endsWith(".m4a"));
            
            if (files != null) {
                songListView.getItems().addAll(files);
            }
        } catch (Exception e) {
            logger.error("Error loading songs from directory", e);
        }
    }
    
    @FXML
    public void selectMusicFiles() {
        logger.info("Opening file selection dialog");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Music Files");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(songListView.getScene().getWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            logger.info("Selected {} files", selectedFiles.size());
            songListView.getItems().addAll(selectedFiles);
        }
    }
    
    @FXML
    public void togglePlayPause() {
        if (playerService.getCurrentSong() == null) {
            File selectedFile = songListView.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                Song song = new Song();
                song.setTitle(selectedFile.getName().replaceFirst("[.][^.]+$", ""));
                song.setFilePath(selectedFile.getAbsolutePath());
                song.setArtist("Unknown Artist");
                playerService.playSong(song);
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
    public void playNext() {
        playerService.playNext();
    }
    
    @FXML
    public void playPrevious() {
        playerService.playPrevious();
    }
    
    private void updatePlayPauseButton(boolean isPlaying) {
        playPauseIcon.setIconLiteral(isPlaying ? "fas-pause" : "fas-play");
    }
}