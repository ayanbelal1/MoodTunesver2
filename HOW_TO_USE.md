# Mood Tunes - Setup and Usage Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Email Configuration](#email-configuration)
4. [Application Setup](#application-setup)
5. [Running the Application](#running-the-application)
6. [Troubleshooting](#troubleshooting)

## Prerequisites

Before running Mood Tunes, ensure you have the following installed:
- Java Development Kit (JDK) 17 or later
- MySQL Server 8.0 or later
- Maven 3.6 or later
- Git (optional, for cloning the repository)

## Database Setup

1. **Install MySQL Server**
   - Download and install MySQL Server from [MySQL Official Website](https://dev.mysql.com/downloads/mysql/)
   - During installation, note down your root password

2. **Create Database and User**
   - Open MySQL Command Line Client or MySQL Workbench
   - Run the following commands:

   ```sql
   -- Create the database
   CREATE DATABASE musicbuddy;

   -- Create a user for the application
   CREATE USER 'musicapp'@'localhost' IDENTIFIED BY 'your_password_here';

   -- Grant privileges to the user
   GRANT ALL PRIVILEGES ON musicbuddy.* TO 'musicapp'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Create Required Tables**
   - Run the following SQL commands:

   ```sql
   USE musicbuddy;

   -- Users table
   CREATE TABLE users (
       id INT PRIMARY KEY AUTO_INCREMENT,
       username VARCHAR(50) NOT NULL,
       email VARCHAR(100) NOT NULL UNIQUE,
       password_hash VARCHAR(255) NOT NULL,
       profile_picture VARCHAR(255),
       theme_preference VARCHAR(20) DEFAULT 'Dark',
       email_notifications BOOLEAN DEFAULT TRUE,
       push_notifications BOOLEAN DEFAULT TRUE,
       email_verified BOOLEAN DEFAULT FALSE,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );

   -- OTP table for email verification
   CREATE TABLE otp_tokens (
       id INT PRIMARY KEY AUTO_INCREMENT,
       user_id INT NOT NULL,
       email VARCHAR(100) NOT NULL,
       otp VARCHAR(6) NOT NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       expires_at TIMESTAMP,
       FOREIGN KEY (user_id) REFERENCES users(id)
   );

   -- Playlists table
   CREATE TABLE playlists (
       id INT PRIMARY KEY AUTO_INCREMENT,
       user_id INT NOT NULL,
       name VARCHAR(100) NOT NULL,
       description TEXT,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (user_id) REFERENCES users(id)
   );

   -- Songs table
   CREATE TABLE songs (
       id INT PRIMARY KEY AUTO_INCREMENT,
       playlist_id INT NOT NULL,
       title VARCHAR(255) NOT NULL,
       artist VARCHAR(255),
       album VARCHAR(255),
       duration INT,
       url VARCHAR(255),
       added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (playlist_id) REFERENCES playlists(id)
   );
   ```

## Email Configuration

1. **Create Gmail Account**
   - Create a new Gmail account or use an existing one
   - Current email: moodtunes2@gmail.com (change this to your email)

2. **Enable 2-Step Verification**
   - Go to Google Account settings
   - Navigate to Security
   - Enable "2-Step Verification"

3. **Generate App Password**
   - Go to Google Account settings → Security → 2-Step Verification
   - Scroll down to "App passwords"
   - Select "Other" from the dropdown
   - Enter "MusicBuddy" as the app name
   - Click "Generate"
   - Copy the 16-character password
   - Current app password: ujgl pzsq rvko jylf (change this to your generated password)

4. **Update Email Configuration**
   - Open `src/main/java/com/musicapp/services/EmailService.java`
   - Update the following constants:
     ```java
     private static final String USERNAME = "your_email@gmail.com";
     private static final String PASSWORD = "your_app_password";
     ```

## Application Setup

1. **Clone or Download the Project**
   ```bash
   git clone <repository-url>
   cd moodtunes
   ```

2. **Update Database Configuration**
   - Open `src/main/java/com/musicapp/database/DatabaseConnection.java`
   - Update the following constants with your MySQL credentials:
     ```java
     private static final String URL = "jdbc:mysql://localhost:3306/moodtunes";
     private static final String USER = "musicapp";
     private static final String PASSWORD = "your_password_here";
     ```

3. **Install Dependencies**
   ```bash
   mvn clean install
   ```

## Running the Application

1. **Start MySQL Server**
   - Ensure MySQL service is running
   - On Windows: Open Services and start MySQL
   - On Linux: `sudo systemctl start mysql`
   - On macOS: `brew services start mysql`

2. **Run the Application**
   ```bash
   mvn javafx:run
   ```

## Troubleshooting

### Common Issues and Solutions

1. **Database Connection Failed**
   - Verify MySQL is running
   - Check database credentials in DatabaseConnection.java
   - Ensure the musicbuddy database exists
   - Try connecting using MySQL client to verify credentials

2. **Email Verification Not Working**
   - Check if Gmail account has 2-Step Verification enabled
   - Verify app password is correct
   - Check spam folder for verification emails
   - Ensure internet connection is stable

3. **Application Won't Start**
   - Verify Java version (must be JDK 17 or later)
   - Check Maven installation
   - Clear Maven cache: `mvn clean`
   - Check system logs for errors

4. **UI Issues**
   - Update JavaFX if needed: `mvn javafx:run`
   - Clear target folder: `mvn clean`
   - Check display resolution settings

### Need Help?

If you encounter any issues not covered here:
1. Check the application logs
2. Verify all configurations
3. Contact support at moodtunes2@gmail.com

---

**Note**: Keep your database credentials and email app password secure and never share them publicly. 
