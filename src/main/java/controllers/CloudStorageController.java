package com.musicapp.controllers;

import com.musicapp.services.CloudStorageService;
import com.musicapp.services.MediaConversionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CloudStorageController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(CloudStorageController.class);
    
    @FXML private ListView<String> filesList;
    @FXML private ProgressBar progressBar;
    
    private final CloudStorageService cloudService = CloudStorageService.getInstance();
    private final MediaConversionService conversionService = MediaConversionService.getInstance();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshFiles();
    }
    
    @FXML
    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        
        // Set up file filters
        FileChooser.ExtensionFilter musicFilter = new FileChooser.ExtensionFilter(
            "Music Files", "*.mp3", "*.wav", "*.m4a", "*.aac", "*.wma", "*.ogg", "*.flac");
        FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter(
            "Video Files", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.flv", "*.webm", "*.m4v");
        fileChooser.getExtensionFilters().addAll(musicFilter, videoFilter);
        
        File file = fileChooser.showOpenDialog(filesList.getScene().getWindow());
        if (file != null) {
            progressBar.setVisible(true);
            progressBar.setProgress(-1); // Indeterminate progress
            
            if (conversionService.needsConversion(file.getAbsolutePath())) {
                // Show conversion alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("File Conversion");
                alert.setHeaderText("Converting file to MP3");
                alert.setContentText("The selected file will be converted to MP3 format before upload. This may take a moment.");
                alert.show();
                
                // Convert file to MP3
                conversionService.convertToMp3(file)
                    .thenAccept(convertedFile -> {
                        // Upload the converted file
                        cloudService.uploadFile(convertedFile.getAbsolutePath(), "audio/mpeg")
                            .thenRun(() -> {
                                Platform.runLater(() -> {
                                    progressBar.setVisible(false);
                                    refreshFiles();
                                    alert.close();
                                });
                            });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            progressBar.setVisible(false);
                            showError("Failed to convert file: " + throwable.getMessage());
                        });
                        return null;
                    });
            } else {
                // Upload original file if it's already in a supported audio format
                cloudService.uploadFile(file.getAbsolutePath(), "audio/mpeg")
                    .thenRun(() -> {
                        Platform.runLater(() -> {
                            progressBar.setVisible(false);
                            refreshFiles();
                        });
                    });
            }
        }
    }
    
    @FXML
    private void handleDownload() {
        String selectedFile = filesList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showError("Please select a file to download");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(selectedFile);
        
        File destination = fileChooser.showSaveDialog(filesList.getScene().getWindow());
        if (destination != null) {
            progressBar.setVisible(true);
            progressBar.setProgress(-1);
            
            cloudService.downloadFile(selectedFile, destination.getAbsolutePath())
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                    });
                });
        }
    }
    
    @FXML
    private void handleRefresh() {
        refreshFiles();
    }
    
    private void refreshFiles() {
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        cloudService.listFiles()
            .thenAccept(files -> {
                Platform.runLater(() -> {
                    filesList.setItems(FXCollections.observableArrayList(files));
                    progressBar.setVisible(false);
                });
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
} 