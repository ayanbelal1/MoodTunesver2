package com.musicapp.services;

import com.musicapp.database.DatabaseConnection;
import com.musicapp.database.OTPDAO;
import com.musicapp.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final OTPDAO otpDAO = new OTPDAO();

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, profile_picture = ?, " +
                    "theme_preference = ?, email_notifications = ?, push_notifications = ? " +
                    "WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getProfilePicture());
            stmt.setString(4, user.getThemePreference());
            stmt.setBoolean(5, user.isEmailNotificationsEnabled());
            stmt.setBoolean(6, user.isPushNotificationsEnabled());
            stmt.setInt(7, user.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating user", e);
            return false;
        }
    }
    
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating password", e);
            return false;
        }
    }
    
    public boolean verifyPassword(int userId, String password) {
        String sql = "SELECT password_hash FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                return BCrypt.checkpw(password, storedHash);
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("Error verifying password", e);
            return false;
        }
    }

    public boolean saveOTP(int userId, String email, String otp) {
        return otpDAO.saveOTP(userId, email, otp);
    }

    public boolean verifyOTP(int userId, String otp) {
        return otpDAO.verifyOTP(userId, otp);
    }

    public boolean markEmailAsVerified(int userId) {
        String query = "UPDATE users SET email_verified = TRUE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error marking email as verified", e);
            return false;
        }
    }

    public boolean isEmailVerified(int userId) {
        String query = "SELECT email_verified FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next() && rs.getBoolean("email_verified");
            
        } catch (SQLException e) {
            logger.error("Error checking email verification status", e);
            return false;
        }
    }

    public User registerUser(String username, String email, String password) {
        String sql = """
            INSERT INTO users (username, email, password_hash, theme_preference, 
                             email_notifications, push_notifications, email_verified)
            VALUES (?, ?, ?, 'Dark', TRUE, TRUE, FALSE)
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, BCrypt.hashpw(password, BCrypt.gensalt()));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    User newUser = new User(rs.getInt(1), username, email, false);
                    return newUser;
                }
            }
            return null;
            
        } catch (SQLException e) {
            logger.error("Error registering user", e);
            return null;
        }
    }

    public boolean isEmailRegistered(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("Error checking if email is registered", e);
            return false;
        }
    }
} 