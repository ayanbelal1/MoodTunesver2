package com.musicapp.services;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalMusicService {
    private static LocalMusicService instance;
    private final ObservableList<File> songFiles = FXCollections.observableArrayList();
    private final ObjectProperty<MediaPlayer> currentPlayer = new SimpleObjectProperty<>();
    private int currentSongIndex = -1;
    private File currentSong;
    private File selectedDirectory;
    private static final Logger logger = LoggerFactory.getLogger(LocalMusicService.class);
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
        ".mp3", ".wav", ".m4a",  // Direct playback formats
        ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v",  // Video formats
        ".aac", ".wma", ".ogg", ".flac"  // Other audio formats
    );
    
    private final MediaConversionService conversionService = MediaConversionService.getInstance();
    
    private LocalMusicService() {
    }
    
    public static synchronized LocalMusicService getInstance() {
        if (instance == null) {
            instance = new LocalMusicService();
        }
        return instance;
    }
    
    public void loadSongsFromDirectory(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        selectedDirectory = directory;
        songFiles.clear();
        
        File[] files = directory.listFiles((dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return SUPPORTED_EXTENSIONS.stream().anyMatch(lowercaseName::endsWith);
        });
        
        if (files != null) {
            songFiles.addAll(Arrays.asList(files));
            logger.info("Loaded {} songs from directory: {}", files.length, directory.getPath());
        }
    }
    
    public void addFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        
        // Filter files with supported extensions
        List<File> supportedFiles = files.stream()
            .filter(file -> SUPPORTED_EXTENSIONS.stream()
                .anyMatch(ext -> file.getName().toLowerCase().endsWith(ext)))
            .collect(Collectors.toList());
        
        if (!supportedFiles.isEmpty()) {
            songFiles.addAll(supportedFiles);
            logger.info("Added {} files to the library", supportedFiles.size());
        }
    }
    
    public void reloadCurrentDirectory() {
        if (selectedDirectory != null && selectedDirectory.exists()) {
            loadSongsFromDirectory(selectedDirectory);
        }
    }
    
    public File getSelectedDirectory() {
        return selectedDirectory;
    }
    
    public ObservableList<File> getSongFiles() {
        return songFiles;
    }
    
    public void playSong(File file) {
        try {
            stopCurrentSong();
            
            if (conversionService.needsConversion(file.getAbsolutePath())) {
                logger.info("Converting file before playback: {}", file.getName());
                conversionService.convertToMp3(file)
                    .thenAccept(convertedFile -> {
                        playConvertedFile(convertedFile);
                        currentSongIndex = songFiles.indexOf(file);
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to convert file: " + file.getName(), throwable);
                        return null;
                    });
            } else {
                Media media = new Media(file.toURI().toString());
                MediaPlayer player = new MediaPlayer(media);
                
                player.setOnEndOfMedia(() -> {
                    playNextSong();
                });
                
                currentSongIndex = songFiles.indexOf(file);
                currentPlayer.set(player);
                player.play();
            }
            
        } catch (Exception e) {
            logger.error("Error playing song: " + file.getName(), e);
        }
    }
    
    private void playConvertedFile(File convertedFile) {
        try {
            Media media = new Media(convertedFile.toURI().toString());
            MediaPlayer player = new MediaPlayer(media);
            
            player.setOnEndOfMedia(() -> {
                playNextSong();
                // Delete the temporary converted file
                convertedFile.deleteOnExit();
            });
            
            currentPlayer.set(player);
            player.play();
            
        } catch (Exception e) {
            logger.error("Error playing converted file: " + convertedFile.getName(), e);
        }
    }
    
    public void playNextSong() {
        if (!songFiles.isEmpty() && currentSongIndex < songFiles.size() - 1) {
            playSong(songFiles.get(currentSongIndex + 1));
        }
    }
    
    public void playPreviousSong() {
        if (!songFiles.isEmpty() && currentSongIndex > 0) {
            playSong(songFiles.get(currentSongIndex - 1));
        }
    }
    
    public void pauseCurrentSong() {
        MediaPlayer player = currentPlayer.get();
        if (player != null) {
            player.pause();
        }
    }
    
    public void resumeCurrentSong() {
        MediaPlayer player = currentPlayer.get();
        if (player != null) {
            player.play();
        }
    }
    
    public void stopCurrentSong() {
        MediaPlayer player = currentPlayer.get();
        if (player != null) {
            player.stop();
            player.dispose();
            currentPlayer.set(null);
        }
    }
    
    public boolean isPlaying() {
        MediaPlayer player = currentPlayer.get();
        return player != null && player.getStatus() == MediaPlayer.Status.PLAYING;
    }
    
    public MediaPlayer getCurrentPlayer() {
        return currentPlayer.get();
    }
    
    public File getCurrentSong() {
        return currentSong;
    }
    
    public ObjectProperty<MediaPlayer> currentPlayerProperty() {
        return currentPlayer;
    }
} 