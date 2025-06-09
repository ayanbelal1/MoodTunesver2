package com.musicapp.database;

public class DatabaseConfig {
    // Database connection details
    public static final String DB_URL = "jdbc:mysql://localhost:3306/musicapp?createDatabaseIfNotExist=true";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "Putin@777";
    
    // SQL queries for user authentication
    public static final String CREATE_USER_TABLE = 
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "username VARCHAR(50) UNIQUE NOT NULL, " +
            "email VARCHAR(100) UNIQUE NOT NULL, " +
            "password VARCHAR(255) NOT NULL, " +
            "profile_picture VARCHAR(255), " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
    
    public static final String CREATE_PLAYLISTS_TABLE = 
            "CREATE TABLE IF NOT EXISTS playlists (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "user_id INT, " +
            "name VARCHAR(100) NOT NULL, " +
            "description TEXT, " +
            "cover_image VARCHAR(255), " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
    
    public static final String CREATE_SONGS_TABLE = 
            "CREATE TABLE IF NOT EXISTS songs (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "title VARCHAR(255) NOT NULL, " +
            "artist VARCHAR(255) NOT NULL, " +
            "album VARCHAR(255), " +
            "duration INT, " +
            "file_path VARCHAR(255), " +
            "cover_art VARCHAR(255), " +
            "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
    
    public static final String CREATE_PLAYLIST_SONGS_TABLE = 
            "CREATE TABLE IF NOT EXISTS playlist_songs (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "playlist_id INT, " +
            "song_id INT, " +
            "position INT, " +
            "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE, " +
            "FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE)";
            
    public static final String CREATE_USER_LIBRARY_TABLE =
            "CREATE TABLE IF NOT EXISTS user_library (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "user_id INT, " +
            "song_id INT, " +
            "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
            "FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE)";
}