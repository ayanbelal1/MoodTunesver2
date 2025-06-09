package com.musicapp.controllers;

import com.musicapp.database.UserDAO;
import com.musicapp.models.User;
import com.musicapp.models.GuestUser;
import com.musicapp.utils.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button guestLoginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Label errorLabel;
    
    private final UserDAO userDAO = new UserDAO();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Clear error message initially
        errorLabel.setText("");
        
        // Add focus listeners to clear error when user starts typing
        usernameField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                errorLabel.setText("");
            }
        });
        
        passwordField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                errorLabel.setText("");
            }
        });
        
        // Add listeners to enable/disable login button
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> updateLoginButton());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> updateLoginButton());
        
        // Initial button state
        updateLoginButton();
    }
    
    private void updateLoginButton() {
        loginButton.setDisable(usernameField.getText().trim().isEmpty() || 
                             passwordField.getText().isEmpty());
    }
    
    @FXML
    private void handleGuestLogin(ActionEvent event) {
        // Set guest user in session
        SessionManager.getInstance().setCurrentUser(GuestUser.getInstance());
        
        try {
            // Load main view
            Parent mainView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/MainView.fxml")));
            Scene scene = new Scene(mainView);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/styles.css")).toExternalForm());
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading application: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required");
            return;
        }
        
        // Show loading state
        loginButton.setText("Logging in...");
        loginButton.setDisable(true);
        
        // Run authentication in background thread
        new Thread(() -> {
            User user = userDAO.authenticateUser(username, password);
            
            Platform.runLater(() -> {
                if (user != null) {
                    // Login successful, save user in session
                    SessionManager.getInstance().setCurrentUser(user);
                    
                    try {
                        // Load main view
                        Parent mainView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/MainView.fxml")));
                        Scene scene = new Scene(mainView);
                        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/styles.css")).toExternalForm());
                        
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.setScene(scene);
                        stage.centerOnScreen();
                        stage.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showError("Error loading application: " + e.getMessage());
                        resetLoginButton();
                    }
                } else {
                    // Login failed
                    showError("Invalid username or password");
                    resetLoginButton();
                    
                    // Clear password field
                    passwordField.clear();
                    passwordField.requestFocus();
                }
            });
        }).start();
    }
    
    private void resetLoginButton() {
        loginButton.setText("Log In");
        updateLoginButton();
    }
    
    @FXML
    private void handleRegisterLink(ActionEvent event) {
        try {
            Parent registerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Register.fxml")));
            Scene scene = new Scene(registerView);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/styles.css")).toExternalForm());
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading registration form: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}