package com.musicapp.services;

import com.musicapp.models.Song;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MusicPlayerService {
    private static final Logger logger = LoggerFactory.getLogger(MusicPlayerService.class);
    private static MusicPlayerService instance;
    
    private MediaPlayer mediaPlayer;
    private final ObservableList<Song> playlist = FXCollections.observableArrayList();
    private final IntegerProperty currentSongIndex = new SimpleIntegerProperty(-1);
    private final ObjectProperty<Song> currentSong = new SimpleObjectProperty<>();
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final BooleanProperty repeat = new SimpleBooleanProperty(false);
    private final BooleanProperty shuffle = new SimpleBooleanProperty(false);
    private final DoubleProperty volume = new SimpleDoubleProperty(1.0);
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final StringProperty currentTime = new SimpleStringProperty("0:00");
    private final StringProperty totalTime = new SimpleStringProperty("0:00");
    private final SpotifyService spotifyService;
    
    private MusicPlayerService() {
        spotifyService = SpotifyService.getInstance();
        
        // Update currentSong when currentSongIndex changes
        currentSongIndex.addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() >= 0 && newVal.intValue() < playlist.size()) {
                currentSong.set(playlist.get(newVal.intValue()));
            } else {
                currentSong.set(null);
            }
        });
    }
    
    public static synchronized MusicPlayerService getInstance() {
        if (instance == null) {
            instance = new MusicPlayerService();
        }
        return instance;
    }
    
    public void playSong(Song song) {
        if (song == null) return;
        
        try {
            // Stop current playback if exists
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            
            // Check if it's a Spotify track
            if (song.getFilePath().startsWith("spotify:")) {
                logger.info("Playing Spotify track: {}", song.getTitle());
                // For now, we'll just show an alert that Spotify playback is not supported
                showAlert("Spotify playback is not supported in this version. The track information is imported for reference only.");
                return;
            }
            
            File file = new File(song.getFilePath());
            if (!file.exists()) {
                logger.error("File not found: {}", song.getFilePath());
                showError("File not found: " + song.getFilePath());
                return;
            }
            
            // Create new media player
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            
            // Set up media player properties
            mediaPlayer.setVolume(volume.get());
            
            // Set up time change listener
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (mediaPlayer.getTotalDuration() != null && !progress.isBound()) {
                    double currentSeconds = newTime.toSeconds();
                    double totalSeconds = mediaPlayer.getTotalDuration().toSeconds();
                    progress.set(currentSeconds / totalSeconds);
                    currentTime.set(formatDuration(newTime));
                    totalTime.set(formatDuration(mediaPlayer.getTotalDuration()));
                }
            });
            
            // Set up status change listener
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == MediaPlayer.Status.READY) {
                    totalTime.set(formatDuration(mediaPlayer.getTotalDuration()));
                } else if (newStatus == MediaPlayer.Status.STOPPED || newStatus == MediaPlayer.Status.DISPOSED) {
                    playing.set(false);
                } else if (newStatus == MediaPlayer.Status.PLAYING) {
                    playing.set(true);
                } else if (newStatus == MediaPlayer.Status.PAUSED) {
                    playing.set(false);
                }
            });
            
            // Set up end of media handler
            mediaPlayer.setOnEndOfMedia(() -> {
                if (repeat.get()) {
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.play();
                } else {
                    playNext();
                }
            });
            
            // Update current song index and current song
            int index = playlist.indexOf(song);
            if (index != -1) {
                currentSongIndex.set(index);
            } else {
                // If song is not in playlist, add it
                playlist.add(song);
                currentSongIndex.set(playlist.size() - 1);
            }
            
            // Set current song and start playback
            currentSong.set(song);
            mediaPlayer.play();
            playing.set(true);
            
        } catch (Exception e) {
            logger.error("Error playing song", e);
            showError("Error playing song: " + e.getMessage());
        }
    }
    
    public void setPlaylist(List<Song> songs) {
        playlist.setAll(songs);
    }
    
    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            playing.set(true);
        }
    }
    
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            playing.set(false);
        }
    }
    
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playing.set(false);
        }
    }
    
    public void playNext() {
        if (playlist.isEmpty()) return;
        
        int nextIndex;
        if (shuffle.get()) {
            nextIndex = (int) (Math.random() * playlist.size());
        } else {
            nextIndex = currentSongIndex.get() + 1;
            if (nextIndex >= playlist.size()) {
                if (repeat.get()) {
                    nextIndex = 0;
                } else {
                    stop();
                    return;
                }
            }
        }
        
        playSong(playlist.get(nextIndex));
    }
    
    public void playPrevious() {
        if (playlist.isEmpty()) return;
        
        int prevIndex = currentSongIndex.get() - 1;
        if (prevIndex < 0) {
            if (repeat.get()) {
                prevIndex = playlist.size() - 1;
            } else {
                return;
            }
        }
        
        playSong(playlist.get(prevIndex));
    }
    
    public void seek(double position) {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            Duration seekTime = mediaPlayer.getTotalDuration().multiply(position);
            mediaPlayer.seek(seekTime);
        }
    }
    
    private String formatDuration(Duration duration) {
        int seconds = (int) duration.toSeconds();
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private void showError(String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // Getters for properties
    public BooleanProperty playingProperty() { return playing; }
    public BooleanProperty repeatProperty() { return repeat; }
    public BooleanProperty shuffleProperty() { return shuffle; }
    public DoubleProperty volumeProperty() { return volume; }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty currentTimeProperty() { return currentTime; }
    public StringProperty totalTimeProperty() { return totalTime; }
    public ObservableList<Song> getPlaylist() { return playlist; }
    public IntegerProperty currentSongIndexProperty() { return currentSongIndex; }
    public ReadOnlyObjectProperty<Song> currentSongProperty() {
        return currentSong;
    }
    public Song getCurrentSong() {
        return currentSong.get();
    }
}