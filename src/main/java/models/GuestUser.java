package com.musicapp.models;

public class GuestUser extends User {
    private static final GuestUser INSTANCE = new GuestUser();
    
    private GuestUser() {
        super(-1, "Guest", "guest@local", true);
    }
    
    public static GuestUser getInstance() {
        return INSTANCE;
    }
    
    @Override
    public boolean isGuest() {
        return true;
    }
} 