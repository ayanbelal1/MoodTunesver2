package com.musicapp.services;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MediaConversionService {
    private static final Logger logger = LoggerFactory.getLogger(MediaConversionService.class);
    private static MediaConversionService instance;
    private boolean ffmpegAvailable = false;
    
    private static final List<String> CONVERTIBLE_EXTENSIONS = Arrays.asList(
        ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v",  // video formats
        ".aac", ".wma", ".ogg", ".m4a", ".flac"  // audio formats
    );
    
    private MediaConversionService() {
        // Verify FFmpeg installation on startup
        verifyFfmpegInstallation();
    }
    
    private void verifyFfmpegInstallation() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
            Process process = processBuilder.start();
            
            // Read the output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null && firstLine.startsWith("ffmpeg version")) {
                    logger.info("FFmpeg is properly installed: {}", firstLine);
                    ffmpegAvailable = true;
                } else {
                    logger.error("FFmpeg is not properly installed. Output: {}", firstLine);
                    showFfmpegError("FFmpeg is not properly installed. Please check your installation.");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("FFmpeg verification failed with exit code: {}", exitCode);
                // Read error stream
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.error("FFmpeg error: {}", line);
                    }
                }
                showFfmpegError("FFmpeg verification failed. Please check your installation.");
            }
        } catch (IOException e) {
            logger.error("Failed to execute FFmpeg. Make sure it's installed and added to PATH", e);
            showFfmpegError("Failed to execute FFmpeg. Make sure it's installed and added to PATH.");
        } catch (InterruptedException e) {
            logger.error("FFmpeg verification was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
    
    private void showFfmpegError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("FFmpeg Error");
            alert.setHeaderText("FFmpeg Not Available");
            alert.setContentText(message + "\n\nVideo and non-MP3 audio files will not be supported until FFmpeg is properly installed.");
            alert.showAndWait();
        });
    }
    
    public static synchronized MediaConversionService getInstance() {
        if (instance == null) {
            instance = new MediaConversionService();
        }
        return instance;
    }
    
    public boolean needsConversion(String filePath) {
        String lowercaseName = filePath.toLowerCase();
        return CONVERTIBLE_EXTENSIONS.stream().anyMatch(lowercaseName::endsWith);
    }
    
    public CompletableFuture<File> convertToMp3(File inputFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ffmpegAvailable) {
                throw new RuntimeException("FFmpeg is not available. Please install FFmpeg and add it to your system PATH.");
            }
            
            try {
                logger.info("Starting conversion of file: {}", inputFile.getName());
                
                // Create temp directory if it doesn't exist
                Path tempDir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "musicapp", "conversions"));
                logger.info("Using temp directory: {}", tempDir);
                
                // Create output file path with .mp3 extension
                String outputFileName = inputFile.getName().substring(0, inputFile.getName().lastIndexOf('.')) + ".mp3";
                File outputFile = new File(tempDir.toFile(), outputFileName);
                logger.info("Output file will be: {}", outputFile.getAbsolutePath());
                
                // Build ffmpeg command
                List<String> command = Arrays.asList(
                    "ffmpeg",
                    "-i", inputFile.getAbsolutePath(),
                    "-vn",  // Disable video if present
                    "-acodec", "libmp3lame",
                    "-ab", "320k",  // High quality audio bitrate
                    "-ar", "44100",  // Standard audio sample rate
                    "-y",  // Overwrite output file if exists
                    outputFile.getAbsolutePath()
                );
                
                logger.info("Executing command: {}", String.join(" ", command));
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true); // Merge error stream with input stream
                
                // Start the conversion process
                Process process = processBuilder.start();
                
                // Read and log the output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("FFmpeg: {}", line);
                    }
                }
                
                // Wait for the conversion to complete
                int exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    String errorMessage = "FFmpeg conversion failed with exit code: " + exitCode;
                    logger.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
                
                if (!outputFile.exists()) {
                    String errorMessage = "Output file was not created: " + outputFile.getAbsolutePath();
                    logger.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
                
                logger.info("Successfully converted {} to MP3 format", inputFile.getName());
                return outputFile;
                
            } catch (IOException e) {
                logger.error("Error converting file to MP3: " + inputFile.getName(), e);
                throw new RuntimeException("Failed to convert file to MP3: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.error("Conversion was interrupted: " + inputFile.getName(), e);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Conversion was interrupted", e);
            }
        });
    }
} 