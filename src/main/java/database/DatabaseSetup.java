package com.musicapp.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.sql.ResultSet;

public class DatabaseSetup {
    
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/?allowPublicKeyRetrieval=true&useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "Putin@777";  // Make sure to update this to match your new device's MySQL password
    
    public static void initializeDatabase() {
        try {
            // Connect to MySQL server
            Connection conn = DriverManager.getConnection(MYSQL_URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();
            
            // Check if database exists
            ResultSet resultSet = stmt.executeQuery("SHOW DATABASES LIKE 'musicapp'");
            if (!resultSet.next()) {
                // Create database only if it doesn't exist
                stmt.executeUpdate("CREATE DATABASE musicapp");
                System.out.println("Database created successfully");
            }
            
            // Use musicapp database
            stmt.executeUpdate("USE musicapp");
            
            // Read and execute schema.sql
            String schemaPath = "src/main/resources/sql/schema.sql";
            try {
                String schemaContent = new String(Files.readAllBytes(Paths.get(schemaPath)));
                
                // Split the schema into individual statements
                String[] statements = schemaContent.split(";");
                
                for (String sql : statements) {
                    sql = sql.trim();
                    // Skip empty statements and comments
                    if (!sql.isEmpty() && !sql.startsWith("--")) {
                        try {
                            stmt.executeUpdate(sql);
                        } catch (SQLException e) {
                            System.err.println("Error executing SQL statement: " + sql);
                            System.err.println("Error message: " + e.getMessage());
                            throw e;
                        }
                    }
                }
                
                System.out.println("Database tables initialized successfully");
            } catch (IOException e) {
                System.err.println("Error reading schema file: " + schemaPath);
                System.err.println("Current working directory: " + System.getProperty("user.dir"));
                System.err.println("Error message: " + e.getMessage());
                throw new RuntimeException("Failed to read schema file", e);
            } finally {
                try {
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing database connections: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}