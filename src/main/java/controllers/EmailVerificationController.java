package com.musicapp.controllers;

import com.musicapp.models.User;
import com.musicapp.services.EmailService;
import com.musicapp.services.UserService;
import com.musicapp.utils.NavigationManager;
import com.musicapp.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class EmailVerificationController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);
    
    @FXML private Label emailLabel;
    @FXML private TextField otpField;
    @FXML private Label timerLabel;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Label statusLabel;
    
    private final EmailService emailService = EmailService.getInstance();
    private final UserService userService = new UserService();
    private User pendingUser;
    private Timer countdownTimer;
    private int timeRemaining;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resendButton.setDisable(true);
        startCountdownTimer();
    }
    
    public void setPendingUser(User user) {
        this.pendingUser = user;
        emailLabel.setText("Verification code sent to: " + user.getEmail());
        sendVerificationCode();
    }
    
    private void sendVerificationCode() {
        String otp = emailService.generateOTP();
        if (emailService.sendOTP(pendingUser.getEmail(), otp) &&
            userService.saveOTP(pendingUser.getId(), pendingUser.getEmail(), otp)) {
            showStatus("Verification code sent!", false);
            startCountdownTimer();
        } else {
            showStatus("Failed to send verification code. Please try again.", true);
            resendButton.setDisable(false);
        }
    }
    
    @FXML
    private void handleVerify() {
        String otp = otpField.getText().trim();
        if (otp.isEmpty()) {
            showStatus("Please enter the verification code.", true);
            return;
        }
        
        if (userService.verifyOTP(pendingUser.getId(), otp)) {
            // Mark email as verified
            userService.markEmailAsVerified(pendingUser.getId());
            
            // Update session user
            pendingUser.setEmailVerified(true);
            SessionManager.getInstance().updateCurrentUser(pendingUser);
            
            showStatus("Email verified successfully!", false);
            
            // Disable verify button to prevent multiple clicks
            verifyButton.setDisable(true);
            
            // Navigate to home view after short delay using JavaFX Timeline
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            delay.setOnFinished(event -> {
                try {
                    NavigationManager.getInstance().navigateToHome();
                } catch (Exception e) {
                    logger.error("Error navigating to home", e);
                    showStatus("Error navigating to home page. Please try again.", true);
                    verifyButton.setDisable(false);
                }
            });
            delay.play();
        } else {
            showStatus("Invalid or expired verification code.", true);
        }
    }
    
    @FXML
    private void handleResend() {
        sendVerificationCode();
        resendButton.setDisable(true);
    }
    
    private void startCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        
        timeRemaining = 300; // 5 minutes
        countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (timeRemaining > 0) {
                    timeRemaining--;
                    Platform.runLater(() -> updateTimerLabel());
                } else {
                    Platform.runLater(() -> {
                        resendButton.setDisable(false);
                        timerLabel.setText("Code expired");
                    });
                    cancel();
                }
            }
        }, 0, 1000);
    }
    
    private void updateTimerLabel() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("Code expires in: %d:%02d", minutes, seconds));
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #E61E32;" : "-fx-text-fill: #1DB954;");
    }
    
    public void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
    }
} 