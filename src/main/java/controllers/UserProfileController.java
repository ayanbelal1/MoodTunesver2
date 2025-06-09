package com.musicapp.controllers;

import com.musicapp.models.User;
import com.musicapp.services.UserService;
import com.musicapp.services.EmailService;
import com.musicapp.database.OTPDAO;
import com.musicapp.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ImageView profileImageView;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox emailNotificationsCheckbox;
    @FXML private CheckBox pushNotificationsCheckbox;
    @FXML private Label statusLabel;
    @FXML private TextField otpField;
    @FXML private Button verifyOTPButton;
    @FXML private Button sendOTPButton;
    
    private final UserService userService = new UserService();
    private final EmailService emailService = EmailService.getInstance();
    private final OTPDAO otpDAO = new OTPDAO();
    private User currentUser;
    private Image defaultProfileImage;
    private String pendingEmail;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load default profile image
        defaultProfileImage = new Image(getClass().getResourceAsStream("/images/app_icon.png"));
        profileImageView.setImage(defaultProfileImage);
        
        // Initialize theme options
        themeComboBox.getItems().addAll("Dark", "Light");
        
        // Load current user data
        loadUserData();
        
        // Initially hide OTP fields
        otpField.setVisible(false);
        verifyOTPButton.setVisible(false);
        sendOTPButton.setVisible(false);
        
        // Add listener to email field
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(currentUser.getEmail())) {
                sendOTPButton.setVisible(true);
            } else {
                sendOTPButton.setVisible(false);
                otpField.setVisible(false);
                verifyOTPButton.setVisible(false);
            }
        });
    }
    
    private void loadUserData() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            usernameField.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail());
            
            // Load profile picture if exists
            if (currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
                try {
                    profileImageView.setImage(new Image(currentUser.getProfilePicture()));
                } catch (Exception e) {
                    logger.error("Error loading profile picture", e);
                    profileImageView.setImage(defaultProfileImage);
                }
            }
            
            // Load preferences
            themeComboBox.setValue(currentUser.getThemePreference());
            emailNotificationsCheckbox.setSelected(currentUser.isEmailNotificationsEnabled());
            pushNotificationsCheckbox.setSelected(currentUser.isPushNotificationsEnabled());
        }
    }
    
    @FXML
    private void handleSendOTP() {
        if (!validateEmail(emailField.getText().trim())) {
            return;
        }
        
        pendingEmail = emailField.getText().trim();
        String otp = emailService.generateOTP();
        
        if (emailService.sendOTP(pendingEmail, otp) && 
            otpDAO.saveOTP(currentUser.getId(), pendingEmail, otp)) {
            
            showSuccess("OTP sent to " + pendingEmail);
            otpField.setVisible(true);
            verifyOTPButton.setVisible(true);
            sendOTPButton.setText("Resend OTP");
        } else {
            showError("Failed to send OTP");
        }
    }
    
    @FXML
    private void handleVerifyOTP() {
        String otp = otpField.getText().trim();
        if (otp.isEmpty()) {
            showError("Please enter OTP");
            return;
        }
        
        if (otpDAO.verifyOTP(currentUser.getId(), otp)) {
            currentUser.setEmail(pendingEmail);
            if (userService.updateUser(currentUser)) {
                showSuccess("Email updated successfully");
                otpField.setVisible(false);
                verifyOTPButton.setVisible(false);
                sendOTPButton.setVisible(false);
                pendingEmail = null;
            } else {
                showError("Failed to update email");
            }
        } else {
            showError("Invalid or expired OTP");
        }
    }
    
    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            showError("Email cannot be empty");
            return false;
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Invalid email format");
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void handleSave() {
        if (!validateInputs()) {
            return;
        }
        
        try {
            // Update user data except email (which requires OTP)
            currentUser.setUsername(usernameField.getText().trim());
            
            // Update password if new password is provided
            if (!newPasswordField.getText().isEmpty()) {
                if (!userService.verifyPassword(currentUser.getId(), currentPasswordField.getText())) {
                    showError("Current password is incorrect");
                    return;
                }
                userService.updatePassword(currentUser.getId(), newPasswordField.getText());
            }
            
            // Update preferences
            currentUser.setThemePreference(themeComboBox.getValue());
            currentUser.setEmailNotificationsEnabled(emailNotificationsCheckbox.isSelected());
            currentUser.setPushNotificationsEnabled(pushNotificationsCheckbox.isSelected());
            
            // Save changes to database
            if (userService.updateUser(currentUser)) {
                showSuccess("Profile updated successfully");
                // Update session
                SessionManager.getInstance().updateCurrentUser(currentUser);
                clearPasswordFields();
            } else {
                showError("Failed to update profile");
            }
            
        } catch (Exception e) {
            logger.error("Error updating profile", e);
            showError("An error occurred while updating profile");
        }
    }
    
    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(image);
                currentUser.setProfilePicture(selectedFile.toURI().toString());
            } catch (Exception e) {
                logger.error("Error loading profile picture", e);
                showError("Failed to load selected image");
            }
        }
    }
    
    @FXML
    private void handleCancel() {
        // Reset fields to current values
        loadUserData();
        clearPasswordFields();
        showStatus("");
    }
    
    private boolean validateInputs() {
        if (usernameField.getText().trim().isEmpty()) {
            showError("Username cannot be empty");
            return false;
        }
        
        if (emailField.getText().trim().isEmpty()) {
            showError("Email cannot be empty");
            return false;
        }
        
        if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Invalid email format");
            return false;
        }
        
        // Validate password change if attempted
        if (!newPasswordField.getText().isEmpty()) {
            if (currentPasswordField.getText().isEmpty()) {
                showError("Current password is required to set new password");
                return false;
            }
            
            if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
                showError("New passwords do not match");
                return false;
            }
            
            if (newPasswordField.getText().length() < 6) {
                showError("New password must be at least 6 characters long");
                return false;
            }
        }
        
        return true;
    }
    
    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #E61E32;");
    }
    
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #1DB954;");
    }
    
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: -fx-text-secondary;");
    }
} 