package com.musicapp.controllers;

import com.musicapp.models.Playlist;
import com.musicapp.services.SpotifyService;
import com.musicapp.database.PlaylistDAO;
import com.musicapp.utils.SessionManager;
import com.musicapp.utils.SpotifyCallbackServer;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SpotifyImportController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyImportController.class);
    
    @FXML private VBox authBox;
    @FXML private VBox playlistBox;
    @FXML private TableView<PlaylistTableItem> playlistTable;
    @FXML private TableColumn<PlaylistTableItem, Boolean> selectColumn;
    @FXML private TableColumn<PlaylistTableItem, String> nameColumn;
    @FXML private TableColumn<PlaylistTableItem, String> tracksColumn;
    @FXML private TableColumn<PlaylistTableItem, String> statusColumn;
    @FXML private CheckBox selectAllCheckbox;
    @FXML private ProgressBar progressBar;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    
    private final SpotifyService spotifyService;
    private final PlaylistDAO playlistDAO = new PlaylistDAO();
    private final ObservableList<PlaylistTableItem> playlists = FXCollections.observableArrayList();
    private final SpotifyCallbackServer callbackServer;
    
    public SpotifyImportController() {
        spotifyService = SpotifyService.getInstance();
        callbackServer = new SpotifyCallbackServer();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        updateViewState();
        
        // Handle select all checkbox
        selectAllCheckbox.setOnAction(event -> {
            boolean selected = selectAllCheckbox.isSelected();
            playlists.forEach(item -> item.setSelected(selected));
        });
        
        updateUI();
    }
    
    private void setupTable() {
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            
            {
                checkBox.setOnAction(event -> {
                    PlaylistTableItem item = getTableRow().getItem();
                    if (item != null) {
                        item.setSelected(checkBox.isSelected());
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item != null && item);
                    setGraphic(checkBox);
                }
            }
        });
        
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        tracksColumn.setCellValueFactory(cellData -> cellData.getValue().tracksProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        
        playlistTable.setItems(playlists);
    }
    
    @FXML
    private void handleLoginClick() {
        if (!spotifyService.isAuthenticated()) {
            startAuthentication();
        } else {
            // Handle logout or re-authentication
            statusLabel.setText("Already authenticated with Spotify");
        }
    }
    
    private void startAuthentication() {
        try {
            // Get the authorization URL and open it in the default browser
            String authUrl = spotifyService.getAuthorizationURL();
            Desktop.getDesktop().browse(new URI(authUrl));
            
            statusLabel.setText("Please authorize in your browser and paste the code here...");
            progressIndicator.setVisible(true);
            
            // Wait for manual code input
            callbackServer.getAuthorizationCode(code -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(true);
                    statusLabel.setText("Processing authentication...");
                });
                
                try {
                    // Handle the authorization code
                    spotifyService.handleAuthorizationCode(code);
                    
                    Platform.runLater(() -> {
                        updateUI();
                        if (spotifyService.isAuthenticated()) {
                            statusLabel.setText("Successfully authenticated! Loading playlists...");
                            refreshPlaylists();
                        }
                    });
                } catch (Exception e) {
                    logger.error("Authentication failed", e);
                    Platform.runLater(() -> {
                        statusLabel.setText("Authentication failed: " + e.getMessage());
                        progressIndicator.setVisible(false);
                        showError("Authentication failed: " + e.getMessage());
                    });
                }
            });
            
        } catch (Exception e) {
            logger.error("Error during Spotify authentication", e);
            statusLabel.setText("Error: " + e.getMessage());
            progressIndicator.setVisible(false);
            showError("Failed to start authentication: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefreshPlaylists() {
        refreshPlaylists();
    }
    
    @FXML
    private void handleImportSelected() {
        List<PlaylistTableItem> selectedItems = playlists.stream()
            .filter(PlaylistTableItem::isSelected)
            .toList();
            
        if (selectedItems.isEmpty()) {
            showAlert("Please select at least one playlist to import.");
            return;
        }
        
        progressBar.setVisible(true);
        int totalPlaylists = selectedItems.size();
        int[] importedCount = {0};
        
        for (PlaylistTableItem item : selectedItems) {
            item.setStatus("Importing...");
            
            Playlist playlist = item.getPlaylist();
            playlist.setUserId(SessionManager.getInstance().getCurrentUser().getId());
            
            new Thread(() -> {
                boolean success = playlistDAO.createPlaylist(playlist);
                Platform.runLater(() -> {
                    item.setStatus(success ? "Imported" : "Failed");
                    importedCount[0]++;
                    progressBar.setProgress((double) importedCount[0] / totalPlaylists);
                    
                    if (importedCount[0] == totalPlaylists) {
                        progressBar.setVisible(false);
                        showAlert("Import completed!");
                    }
                });
            }).start();
        }
    }
    
    @FXML
    private void handleRemoveAllPlaylists() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Remove All Spotify Playlists");
        confirmDialog.setHeaderText("Are you sure you want to remove all imported Spotify playlists?");
        confirmDialog.setContentText("This action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                progressBar.setVisible(true);
                statusLabel.setText("Removing Spotify playlists...");
                
                new Thread(() -> {
                    boolean success = spotifyService.removeAllSpotifyPlaylists();
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        if (success) {
                            playlists.clear();
                            statusLabel.setText("All Spotify playlists have been removed");
                            showAlert("All Spotify playlists have been removed successfully");
                        } else {
                            statusLabel.setText("Failed to remove Spotify playlists");
                            showError("Failed to remove Spotify playlists");
                        }
                    });
                }).start();
            }
        });
    }
    
    private void refreshPlaylists() {
        progressBar.setVisible(true);
        playlists.clear();
        
        spotifyService.importUserPlaylists()
            .thenAccept(importedPlaylists -> {
                Platform.runLater(() -> {
                    importedPlaylists.forEach(playlist -> 
                        playlists.add(new PlaylistTableItem(playlist))
                    );
                    progressBar.setVisible(false);
                    statusLabel.setText("Successfully loaded " + importedPlaylists.size() + " playlists");
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    statusLabel.setText("Failed to load playlists");
                    showError("Failed to load playlists: " + throwable.getMessage());
                });
                return null;
            });
    }
    
    private void updateViewState() {
        boolean isAuthenticated = spotifyService.isAuthenticated();
        authBox.setVisible(!isAuthenticated);
        authBox.setManaged(!isAuthenticated);
        playlistBox.setVisible(isAuthenticated);
        playlistBox.setManaged(isAuthenticated);
    }
    
    private void updateUI() {
        if (spotifyService.isAuthenticated()) {
            loginButton.setText("Connected to Spotify");
            loginButton.setDisable(true);
            statusLabel.setText("Successfully connected to Spotify");
        } else {
            loginButton.setText("Connect to Spotify");
            loginButton.setDisable(false);
            statusLabel.setText("Not connected to Spotify");
        }
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
    
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // Helper class for table items
    private static class PlaylistTableItem {
        private final SimpleBooleanProperty selected;
        private final SimpleStringProperty name;
        private final SimpleStringProperty tracks;
        private final SimpleStringProperty status;
        private final Playlist playlist;
        
        public PlaylistTableItem(Playlist playlist) {
            this.playlist = playlist;
            this.selected = new SimpleBooleanProperty(false);
            this.name = new SimpleStringProperty(playlist.getName());
            this.tracks = new SimpleStringProperty(playlist.getSongs().size() + " tracks");
            this.status = new SimpleStringProperty("Not imported");
        }
        
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean value) { selected.set(value); }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        
        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        
        public String getTracks() { return tracks.get(); }
        public SimpleStringProperty tracksProperty() { return tracks; }
        
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        public SimpleStringProperty statusProperty() { return status; }
        
        public Playlist getPlaylist() { return playlist; }
    }
} 