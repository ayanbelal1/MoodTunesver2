package com.musicapp.models;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private int id;
    private int userId;
    private String name;
    private String description;
    private String coverImage;
    private List<Song> songs;
    
    public Playlist() {
        this.songs = new ArrayList<>();
    }
    
    public Playlist(int userId, String name, String description, String coverImage) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.coverImage = coverImage;
        this.songs = new ArrayList<>();
    }
    
    public Playlist(int id, int userId, String name, String description, String coverImage) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.coverImage = coverImage;
        this.songs = new ArrayList<>();
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCoverImage() {
        return coverImage;
    }
    
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
    
    public List<Song> getSongs() {
        return songs;
    }
    
    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }
    
    public void addSong(Song song) {
        if (!songs.contains(song)) {
            songs.add(song);
        }
    }
    
    public void removeSong(Song song) {
        songs.remove(song);
    }
    
    public int getTotalDuration() {
        return songs.stream().mapToInt(Song::getDuration).sum();
    }
    
    public String getFormattedTotalDuration() {
        int totalSeconds = getTotalDuration();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d hr %d min", hours, minutes);
        } else {
            return String.format("%d min %d sec", minutes, seconds);
        }
    }
}