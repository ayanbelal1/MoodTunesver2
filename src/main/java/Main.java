package com.musicapp;

import com.musicapp.database.DatabaseSetup;
import com.musicapp.utils.NavigationManager;
import com.musicapp.services.MusicPlayerService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            DatabaseSetup.initializeDatabase();
            
            // Set up the navigation manager
            NavigationManager navigationManager = NavigationManager.getInstance();
            navigationManager.setMainStage(primaryStage);
            
            // Navigate to login view
            navigationManager.navigateToLogin();
            
            primaryStage.setTitle("Mood Tunes");
            primaryStage.show();
            
        } catch (Exception e) {
            logger.error("Error starting application", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}