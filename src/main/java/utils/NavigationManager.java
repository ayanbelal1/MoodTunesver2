package com.musicapp.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationManager {
    private static final Logger logger = LoggerFactory.getLogger(NavigationManager.class);
    private static NavigationManager instance;
    private Stage mainStage;
    private FXMLLoader currentLoader;
    
    private NavigationManager() {}
    
    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }
    
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }
    
    public void navigateToHome() {
        loadView("/fxml/MainView.fxml", "MusicApp");
    }
    
    public void navigateToLogin() {
        loadView("/fxml/Login.fxml", "MusicApp - Login");
    }
    
    public void navigateToRegister() {
        loadView("/fxml/Register.fxml", "MusicApp - Register");
    }
    
    public FXMLLoader navigateToEmailVerification() {
        return loadView("/fxml/EmailVerificationView.fxml", "MusicApp - Email Verification");
    }
    
    private FXMLLoader loadView(String fxmlPath, String title) {
        try {
            currentLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = currentLoader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            
            // Set minimum window size for main view
            if (fxmlPath.contains("MainView")) {
                mainStage.setMinWidth(1000);
                mainStage.setMinHeight(700);
            } else {
                mainStage.setMinWidth(400);
                mainStage.setMinHeight(600);
            }
            
            mainStage.setTitle(title);
            mainStage.setScene(scene);
            mainStage.show();
            return currentLoader;
        } catch (Exception e) {
            logger.error("Error loading view: " + fxmlPath, e);
            return null;
        }
    }
    
    public <T> T getCurrentController() {
        return currentLoader != null ? currentLoader.getController() : null;
    }
} 