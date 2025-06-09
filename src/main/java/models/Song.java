package com.musicapp.models;

import java.util.Objects;

public class Song {
    private int id;
    private String title;
    private String artist;
    private String album;
    private int duration; // in seconds
    private String filePath;
    private String coverArt;
    
    public Song() {
    }
    
    public Song(String title, String artist, String album, int duration, String filePath, String coverArt) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.filePath = filePath;
        this.coverArt = coverArt;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getAlbum() {
        return album;
    }
    
    public void setAlbum(String album) {
        this.album = album;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getCoverArt() {
        return coverArt;
    }
    
    public void setCoverArt(String coverArt) {
        this.coverArt = coverArt;
    }
    
    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return id == song.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}