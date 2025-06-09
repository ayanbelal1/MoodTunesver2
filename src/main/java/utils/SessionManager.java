package com.musicapp.utils;

import com.musicapp.models.User;
import java.io.*;
import java.util.Properties;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private static final String SESSION_FILE = System.getProperty("user.home") + File.separator + ".musicapp_session";
    
    private SessionManager() {
        loadSession();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        saveSession();
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    private void saveSession() {
        Properties props = new Properties();
        if (currentUser != null) {
            props.setProperty("userId", String.valueOf(currentUser.getId()));
            props.setProperty("username", currentUser.getUsername());
            props.setProperty("email", currentUser.getEmail());
        }
        
        try (FileOutputStream out = new FileOutputStream(SESSION_FILE)) {
            props.store(out, "MusicApp Session");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadSession() {
        File sessionFile = new File(SESSION_FILE);
        if (!sessionFile.exists()) {
            return;
        }
        
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(sessionFile)) {
            props.load(in);
            
            if (props.containsKey("userId") && props.containsKey("username") && props.containsKey("email")) {
                User user = new User();
                user.setId(Integer.parseInt(props.getProperty("userId")));
                user.setUsername(props.getProperty("username"));
                user.setEmail(props.getProperty("email"));
                this.currentUser = user;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void clearSession() {
        this.currentUser = null;
        File sessionFile = new File(SESSION_FILE);
        if (sessionFile.exists()) {
            sessionFile.delete();
        }
    }
    
    public void updateCurrentUser(User user) {
        this.currentUser = user;
        saveSession();
    }
}