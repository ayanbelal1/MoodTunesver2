package com.musicapp.controllers;

import com.musicapp.database.UserDAO;
import com.musicapp.models.User;
import com.musicapp.services.UserService;
import com.musicapp.utils.NavigationManager;
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label errorLabel;
    
    private final UserDAO userDAO = new UserDAO();
    private final BooleanProperty registering = new SimpleBooleanProperty(false);
    private final UserService userService = new UserService();
    
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
        
        // Enable register button only when all fields have values and not registering
        BooleanProperty fieldsEmpty = new SimpleBooleanProperty();
        fieldsEmpty.bind(
            usernameField.textProperty().isEmpty()
            .or(emailField.textProperty().isEmpty())
            .or(passwordField.textProperty().isEmpty())
            .or(confirmPasswordField.textProperty().isEmpty())
        );
        
        registerButton.disableProperty().bind(fieldsEmpty.or(registering));
    }
    
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate input
        if (!validateInput(username, email, password, confirmPassword)) {
            return;
        }
        
        try {
            registering.set(true);
            
            // Check if email exists first
            if (userService.isEmailRegistered(email)) {
                showError("This email address is already registered. Please use a different email or try logging in.");
                emailField.requestFocus();
                return;
            }
            
            // Create user
            User newUser = userService.registerUser(username, email, password);
            if (newUser != null) {
                // Set the user in session
                SessionManager.getInstance().setCurrentUser(newUser);
                
                // Navigate to email verification and get the controller
                NavigationManager navigationManager = NavigationManager.getInstance();
                FXMLLoader loader = navigationManager.navigateToEmailVerification();
                
                if (loader != null) {
                    EmailVerificationController verificationController = loader.getController();
                    if (verificationController != null) {
                        verificationController.setPendingUser(newUser);
                    } else {
                        logger.error("Failed to get email verification controller");
                        showError("An error occurred during registration.");
                    }
                } else {
                    logger.error("Failed to load email verification view");
                    showError("An error occurred during registration.");
                }
            } else {
                showError("Registration failed. Please try again.");
            }
        } catch (Exception e) {
            logger.error("Error during registration", e);
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                showError("This email address is already registered. Please use a different email or try logging in.");
                emailField.requestFocus();
            } else {
                showError("An error occurred during registration. Please try again.");
            }
        } finally {
            registering.set(false);
        }
    }
    
    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        if (username.isEmpty()) {
            showError("Username is required");
            return false;
        }
        
        if (email.isEmpty()) {
            showError("Email is required");
            return false;
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Invalid email format");
            return false;
        }
        
        if (password.isEmpty()) {
            showError("Password is required");
            return false;
        }
        
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return false;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void handleBackToLogin() {
        redirectToLogin();
    }
    
    @FXML
    private void handleLoginLink(ActionEvent event) {
        redirectToLogin();
    }
    
    private void redirectToLogin() {
        try {
            Parent loginView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            Scene scene = new Scene(loginView);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/styles.css")).toExternalForm());
            
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading login view: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}