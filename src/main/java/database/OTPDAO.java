package com.musicapp.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

public class OTPDAO {
    private static final Logger logger = LoggerFactory.getLogger(OTPDAO.class);
    
    public boolean saveOTP(int userId, String email, String otp) {
        String query = "INSERT INTO user_otps (user_id, email, otp, expires_at) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            // Set OTP to expire in 5 minutes
            Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, email);
            pstmt.setString(3, otp);
            pstmt.setTimestamp(4, expiresAt);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error saving OTP", e);
            return false;
        }
    }
    
    public boolean verifyOTP(int userId, String otp) {
        String query = """
            SELECT * FROM user_otps 
            WHERE user_id = ? AND otp = ? AND expires_at > ? AND is_verified = FALSE 
            ORDER BY created_at DESC LIMIT 1
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, otp);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // Mark OTP as verified
                String updateQuery = "UPDATE user_otps SET is_verified = TRUE WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, rs.getInt("id"));
                    updateStmt.executeUpdate();
                }
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("Error verifying OTP", e);
            return false;
        }
    }
    
    public void cleanupExpiredOTPs() {
        String query = "DELETE FROM user_otps WHERE expires_at < ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error cleaning up expired OTPs", e);
        }
    }
} 