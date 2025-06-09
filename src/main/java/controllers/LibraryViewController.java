package com.musicapp.controllers;

import com.musicapp.services.MusicPlayerService;
import com.musicapp.models.Song;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class LibraryViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(LibraryViewController.class);

    @FXML private TableView<File> songsTableView;
    @FXML private TableColumn<File, String> fileNameColumn;
    @FXML private TableColumn<File, String> durationColumn;
    @FXML private Button playPauseButton;
    @FXML private FontIcon playPauseIcon;
    @FXML private Slider progressSlider;
    @FXML private Slider volumeSlider;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalTimeLabel;

    private final MusicPlayerService playerService = MusicPlayerService.getInstance();
    private boolean isSliderDragging = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableView();
        setupPlayerControls();
        
        // Set initial states
        updatePlayPauseButton(playerService.playingProperty().get());
    }

    private void setupTableView() {
        fileNameColumn.setCellValueFactory(cellData -> {
            File file = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                () -> file != null ? file.getName() : ""
            );
        });
        
        songsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                File selectedFile = songsTableView.getSelectionModel().getSelectedItem();
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
    public void handleSelectFolder() {
        logger.info("Opening folder selection dialog");
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Music Folder");
        
        File selectedDirectory = directoryChooser.showDialog(songsTableView.getScene().getWindow());
        if (selectedDirectory != null) {
            logger.info("Selected directory: {}", selectedDirectory.getPath());
            loadSongsFromDirectory(selectedDirectory);
        }
    }

    private void loadSongsFromDirectory(File directory) {
        songsTableView.getItems().clear();
        try {
            File[] files = directory.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".mp3") || 
                name.toLowerCase().endsWith(".wav") || 
                name.toLowerCase().endsWith(".m4a"));
            
            if (files != null) {
                songsTableView.getItems().addAll(files);
            }
        } catch (Exception e) {
            logger.error("Error loading songs from directory", e);
        }
    }

    @FXML
    public void handlePlayPauseButton() {
        if (playerService.getCurrentSong() == null) {
            File selectedFile = songsTableView.getSelectionModel().getSelectedItem();
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
    public void handlePreviousButton() {
        playerService.playPrevious();
    }

    @FXML
    public void handleNextButton() {
        playerService.playNext();
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        playPauseIcon.setIconLiteral(isPlaying ? "fas-pause" : "fas-play");
    }
}