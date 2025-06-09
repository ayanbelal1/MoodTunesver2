package com.musicapp.models;

public class User {
    private int id;
    private String username;
    private String email;
    private String password;
    private String profilePicture;
    private String themePreference;
    private boolean emailNotificationsEnabled;
    private boolean pushNotificationsEnabled;
    private boolean emailVerified;
    
    public User() {
    }
    
    public User(int id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.themePreference = "Dark"; // Default theme
        this.emailNotificationsEnabled = true; // Default notification settings
        this.pushNotificationsEnabled = true;
        this.emailVerified = false;
    }
    
    // Constructor without password for social features and guest users
    public User(int id, String username, String email, boolean emailVerified) {
        this(id, username, email, null);
        this.emailVerified = emailVerified;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getProfilePicture() {
        return profilePicture;
    }
    
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
    
    public String getThemePreference() {
        return themePreference;
    }
    
    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }
    
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }
    
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }
    
    public boolean isPushNotificationsEnabled() {
        return pushNotificationsEnabled;
    }
    
    public void setPushNotificationsEnabled(boolean pushNotificationsEnabled) {
        this.pushNotificationsEnabled = pushNotificationsEnabled;
    }
    
    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public boolean isGuest() {
        return false;
    }
    
    @Override
    public String toString() {
        return username;
    }
}