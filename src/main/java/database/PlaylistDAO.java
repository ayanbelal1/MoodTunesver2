package com.musicapp.database;

import com.musicapp.models.Playlist;
import com.musicapp.models.Song;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDAO {
    
    public boolean createPlaylist(Playlist playlist) {
        String query = "INSERT INTO playlists (user_id, name, description, cover_art) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, playlist.getUserId());
            pstmt.setString(2, playlist.getName());
            pstmt.setString(3, playlist.getDescription());
            pstmt.setString(4, playlist.getCoverImage());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    playlist.setId(rs.getInt(1));
                }
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Playlist> getUserPlaylists(int userId) {
        List<Playlist> playlists = new ArrayList<>();
        String query = "SELECT * FROM playlists WHERE user_id = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Playlist playlist = new Playlist();
                playlist.setId(rs.getInt("id"));
                playlist.setUserId(rs.getInt("user_id"));
                playlist.setName(rs.getString("name"));
                playlist.setDescription(rs.getString("description"));
                playlist.setCoverImage(rs.getString("cover_art"));
                playlists.add(playlist);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return playlists;
    }
    
    public boolean addSongToPlaylist(int playlistId, int songId) {
        // First get the next position
        String positionQuery = "SELECT COALESCE(MAX(position), 0) + 1 FROM playlist_songs WHERE playlist_id = ?";
        String insertQuery = "INSERT INTO playlist_songs (playlist_id, song_id, position) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD)) {
            
            // Get the next position
            int nextPosition;
            try (PreparedStatement pstmt = conn.prepareStatement(positionQuery)) {
                pstmt.setInt(1, playlistId);
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                nextPosition = rs.getInt(1);
            }
            
            // Insert the song
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setInt(1, playlistId);
                pstmt.setInt(2, songId);
                pstmt.setInt(3, nextPosition);
                
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean removeSongFromPlaylist(int playlistId, int songId) {
        String query = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, playlistId);
            pstmt.setInt(2, songId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Song> getPlaylistSongs(int playlistId) {
        List<Song> songs = new ArrayList<>();
        String query = "SELECT s.* FROM songs s JOIN playlist_songs ps ON s.id = ps.song_id WHERE ps.playlist_id = ? ORDER BY ps.position";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, playlistId);
            
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
    
    public boolean updatePlaylist(Playlist playlist) {
        String query = "UPDATE playlists SET name = ?, description = ?, cover_art = ? WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, playlist.getName());
            pstmt.setString(2, playlist.getDescription());
            pstmt.setString(3, playlist.getCoverImage());
            pstmt.setInt(4, playlist.getId());
            pstmt.setInt(5, playlist.getUserId());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deletePlaylist(int playlistId, int userId) {
        String query = "DELETE FROM playlists WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, playlistId);
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteSpotifyPlaylists(int userId) {
        String query = "DELETE FROM playlists WHERE user_id = ? AND description = 'Imported from Spotify'";
        
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}