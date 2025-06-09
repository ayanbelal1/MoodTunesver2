package com.musicapp.database;

import com.musicapp.models.User;
import com.musicapp.models.Playlist;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SocialDAO {
    private static final String CREATE_FOLLOWERS_TABLE = """
        CREATE TABLE IF NOT EXISTS followers (
            follower_id INT NOT NULL,
            following_id INT NOT NULL,
            follow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (follower_id, following_id),
            FOREIGN KEY (follower_id) REFERENCES users(id),
            FOREIGN KEY (following_id) REFERENCES users(id)
        )
    """;
    
    private static final String CREATE_SHARED_PLAYLISTS_TABLE = """
        CREATE TABLE IF NOT EXISTS shared_playlists (
            playlist_id INT NOT NULL,
            shared_by_id INT NOT NULL,
            shared_with_id INT NOT NULL,
            share_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (playlist_id, shared_by_id, shared_with_id),
            FOREIGN KEY (playlist_id) REFERENCES playlists(id),
            FOREIGN KEY (shared_by_id) REFERENCES users(id),
            FOREIGN KEY (shared_with_id) REFERENCES users(id)
        )
    """;
    
    public SocialDAO() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_FOLLOWERS_TABLE);
            stmt.execute(CREATE_SHARED_PLAYLISTS_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean followUser(int followerId, int followingId) {
        String sql = "INSERT INTO followers (follower_id, following_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean unfollowUser(int followerId, int followingId) {
        String sql = "DELETE FROM followers WHERE follower_id = ? AND following_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean isFollowing(int followerId, int followingId) {
        String sql = "SELECT 1 FROM followers WHERE follower_id = ? AND following_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<User> getFollowers(int userId) {
        String sql = """
            SELECT u.* FROM users u
            JOIN followers f ON u.id = f.follower_id
            WHERE f.following_id = ?
        """;
        List<User> followers = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                followers.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    null  // Don't include password
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return followers;
    }
    
    public List<User> getFollowing(int userId) {
        String sql = """
            SELECT u.* FROM users u
            JOIN followers f ON u.id = f.following_id
            WHERE f.follower_id = ?
        """;
        List<User> following = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                following.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    null  // Don't include password
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return following;
    }
    
    public boolean sharePlaylist(int playlistId, int sharedById, int sharedWithId) {
        String sql = "INSERT INTO shared_playlists (playlist_id, shared_by_id, shared_with_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            pstmt.setInt(2, sharedById);
            pstmt.setInt(3, sharedWithId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Playlist> getSharedPlaylists(int userId) {
        String sql = """
            SELECT p.* FROM playlists p
            JOIN shared_playlists sp ON p.id = sp.playlist_id
            WHERE sp.shared_with_id = ?
        """;
        List<Playlist> sharedPlaylists = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                sharedPlaylists.add(new Playlist(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("cover_image")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sharedPlaylists;
    }
} 