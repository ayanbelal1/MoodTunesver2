package com.musicapp.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class CloudStorageService {
    private static final Logger logger = LoggerFactory.getLogger(CloudStorageService.class);
    private static CloudStorageService instance;
    
    private static final String APPLICATION_NAME = "MusicBuddy";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CONFIG_FILE = "config.properties";
    
    private String googleDriveCredentialsPath;
    private boolean isGoogleDriveInitialized = false;
    
    private Drive driveService;
    
    private CloudStorageService() {
        loadConfiguration();
        initializeGoogleDrive();
    }
    
    public static synchronized CloudStorageService getInstance() {
        if (instance == null) {
            instance = new CloudStorageService();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        try {
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (input == null) {
                    logger.error("config.properties not found");
                    return;
                }
                props.load(input);
            }
            
            googleDriveCredentialsPath = props.getProperty("google.drive.credentials.path");
            if (googleDriveCredentialsPath == null || googleDriveCredentialsPath.trim().isEmpty()) {
                logger.error("Google Drive credentials path not configured");
                return;
            }
            
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            showError("Failed to load cloud storage configuration");
        }
    }
    
    private void initializeGoogleDrive() {
        try {
            if (googleDriveCredentialsPath == null) {
                logger.error("Google Drive credentials path not set");
                return;
            }
            
            String credentialsPath = System.getProperty("user.dir") + "/src/main/resources/" + googleDriveCredentialsPath;
            java.io.File credentialsFile = new java.io.File(credentialsPath);
            
            if (!credentialsFile.exists()) {
                logger.error("Google Drive credentials file not found at: {}", credentialsPath);
                showError("Google Drive credentials file not found. Please check the configuration.");
                return;
            }
            
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(new FileInputStream(credentialsFile)));
                
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
                
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            
            driveService = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
                
            isGoogleDriveInitialized = true;
            logger.info("Google Drive service initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing Google Drive service", e);
            showError("Failed to initialize Google Drive: " + e.getMessage());
        }
    }
    
    public boolean isCloudStorageAvailable() {
        return isGoogleDriveInitialized && driveService != null;
    }
    
    public CompletableFuture<Void> uploadFile(String filePath, String mimeType) {
        return CompletableFuture.runAsync(() -> {
            if (!isCloudStorageAvailable()) {
                throw new IllegalStateException("Cloud storage service is not initialized");
            }
            
            try {
                java.io.File fileContent = new java.io.File(filePath);
                if (!fileContent.exists()) {
                    throw new IOException("File not found: " + filePath);
                }
                
                File fileMetadata = new File();
                fileMetadata.setName(fileContent.getName());
                
                FileContent mediaContent = new FileContent(mimeType, fileContent);
                File file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name")
                    .execute();
                    
                logger.info("File '{}' uploaded to Google Drive with ID: {}", file.getName(), file.getId());
                Platform.runLater(() -> showSuccess("File uploaded successfully to cloud storage"));
            } catch (IOException e) {
                logger.error("Error uploading to cloud storage", e);
                showError("Failed to upload to cloud storage: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<List<String>> listFiles() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> fileList = new ArrayList<>();
                String pageToken = null;
                do {
                    var result = driveService.files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                    result.getFiles().forEach(file -> fileList.add(file.getName()));
                    pageToken = result.getNextPageToken();
                } while (pageToken != null);
                
                return fileList;
            } catch (IOException e) {
                logger.error("Error listing cloud storage files", e);
                showError("Failed to list cloud storage files: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }
    
    public CompletableFuture<Void> downloadFile(String fileId, String destinationPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path destination = Path.of(destinationPath);
                if (!Files.exists(destination.getParent())) {
                    Files.createDirectories(destination.getParent());
                }
                
                try (FileOutputStream outputStream = new FileOutputStream(destinationPath)) {
                    driveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream);
                }
                
                logger.info("File downloaded from cloud storage to: {}", destinationPath);
                Platform.runLater(() -> showSuccess("File downloaded successfully"));
            } catch (IOException e) {
                logger.error("Error downloading from cloud storage", e);
                showError("Failed to download file: " + e.getMessage());
            }
        });
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
} 