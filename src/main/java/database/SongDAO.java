package com.musicapp.database;

import com.musicapp.models.Song;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SongDAO {
    
    public int addSong(Song song) {
        String sql = "INSERT INTO songs (title, artist, album, duration, file_path, cover_art) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.setString(3, song.getAlbum());
            pstmt.setInt(4, song.getDuration());
            pstmt.setString(5, song.getFilePath());
            pstmt.setString(6, song.getCoverArt());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        String query = "SELECT * FROM songs";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Song song = new Song();
                song.setId(rs.getInt("id"));
                song.setTitle(rs.getString("title"));
                song.setArtist(rs.getString("artist"));
                song.setAlbum(rs.getString("album"));
                song.setDuration(rs.getInt("duration"));
                song.setFilePath(rs.getString("file_path"));
                song.setCoverArt(rs.getString("cover_art"));
                songs.add(song);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return songs;
    }
    
    public List<Song> searchSongs(String searchTerm) {
        List<Song> songs = new ArrayList<>();
        String query = "SELECT * FROM songs WHERE title LIKE ? OR artist LIKE ? OR album LIKE ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            String term = "%" + searchTerm + "%";
            pstmt.setString(1, term);
            pstmt.setString(2, term);
            pstmt.setString(3, term);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Song song = new Song();
                song.setId(rs.getInt("id"));
                song.setTitle(rs.getString("title"));
                song.setArtist(rs.getString("artist"));
                song.setAlbum(rs.getString("album"));
                song.setDuration(rs.getInt("duration"));
                song.setFilePath(rs.getString("file_path"));
                song.setCoverArt(rs.getString("cover_art"));
                songs.add(song);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return songs;
    }
    
    public boolean addSongToUserLibrary(int userId, int songId) {
        String query = "INSERT INTO user_library (user_id, song_id) VALUES (?, ?)";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, songId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Song> getUserLibrary(int userId) {
        List<Song> songs = new ArrayList<>();
        String query = "SELECT s.* FROM songs s JOIN user_library ul ON s.id = ul.song_id WHERE ul.user_id = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Song song = new Song();
                song.setId(rs.getInt("id"));
                song.setTitle(rs.getString("title"));
                song.setArtist(rs.getString("artist"));
                song.setAlbum(rs.getString("album"));
                song.setDuration(rs.getInt("duration"));
                song.setFilePath(rs.getString("file_path"));
                song.setCoverArt(rs.getString("cover_art"));
                songs.add(song);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return songs;
    }
}