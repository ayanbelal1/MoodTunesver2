package com.musicapp.services;

import com.musicapp.models.Song;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueueService {
    private static QueueService instance;
    
    private final MusicPlayerService playerService;
    private final ObservableList<Song> queue;
    private final ObservableList<Song> history;
    
    private QueueService() {
        playerService = MusicPlayerService.getInstance();
        queue = FXCollections.observableArrayList();
        history = FXCollections.observableArrayList();
        
        // Listen for song changes to update history
        playerService.currentSongIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal.intValue() >= 0 && oldVal.intValue() < playerService.getPlaylist().size()) {
                Song previousSong = playerService.getPlaylist().get(oldVal.intValue());
                if (previousSong != null) {
                    history.add(0, previousSong);
                    if (history.size() > 50) { // Limit history size
                        history.remove(history.size() - 1);
                    }
                }
            }
        });
    }
    
    public static synchronized QueueService getInstance() {
        if (instance == null) {
            instance = new QueueService();
        }
        return instance;
    }
    
    public void addToQueue(Song song) {
        if (song != null) {
            queue.add(song);
            
            // If nothing is playing, start playing this song
            if (!playerService.playingProperty().get() && queue.size() == 1) {
                List<Song> playlist = new ArrayList<>(queue);
                playerService.setPlaylist(playlist);
                playerService.playSong(song);
            }
        }
    }
    
    public void addToQueue(List<Song> songs) {
        if (songs != null && !songs.isEmpty()) {
            queue.addAll(songs);
            
            // If nothing is playing, start playing the first song
            if (!playerService.playingProperty().get() && queue.size() == songs.size()) {
                List<Song> playlist = new ArrayList<>(queue);
                playerService.setPlaylist(playlist);
                playerService.playSong(songs.get(0));
            }
        }
    }
    
    public void removeFromQueue(Song song) {
        queue.remove(song);
        
        // Update player's playlist
        List<Song> playlist = new ArrayList<>(queue);
        playerService.setPlaylist(playlist);
    }
    
    public void clearQueue() {
        queue.clear();
        playerService.stop();
        playerService.setPlaylist(new ArrayList<>());
    }
    
    public void shuffleQueue() {
        if (queue.isEmpty()) return;
        
        // Get current song
        int currentIndex = playerService.currentSongIndexProperty().get();
        Song currentSong = currentIndex >= 0 && currentIndex < playerService.getPlaylist().size() 
            ? playerService.getPlaylist().get(currentIndex) 
            : null;
        
        // Shuffle remaining songs
        List<Song> remainingSongs = new ArrayList<>(queue);
        if (currentSong != null) {
            remainingSongs.remove(currentSong);
        }
        Collections.shuffle(remainingSongs);
        
        // Reconstruct queue
        queue.clear();
        if (currentSong != null) {
            queue.add(currentSong);
        }
        queue.addAll(remainingSongs);
        
        // Update player's playlist
        List<Song> playlist = new ArrayList<>(queue);
        playerService.setPlaylist(playlist);
    }
    
    public void moveInQueue(int oldIndex, int newIndex) {
        if (oldIndex >= 0 && oldIndex < queue.size() && 
            newIndex >= 0 && newIndex < queue.size()) {
            Song song = queue.remove(oldIndex);
            queue.add(newIndex, song);
            
            // Update player's playlist
            List<Song> playlist = new ArrayList<>(queue);
            playerService.setPlaylist(playlist);
        }
    }
    
    public ObservableList<Song> getQueue() {
        return FXCollections.unmodifiableObservableList(queue);
    }
    
    public ObservableList<Song> getHistory() {
        return FXCollections.unmodifiableObservableList(history);
    }
} 