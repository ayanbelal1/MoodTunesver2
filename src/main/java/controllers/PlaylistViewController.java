package com.musicapp.controllers;

import com.musicapp.database.PlaylistDAO;
import com.musicapp.database.SongDAO;
import com.musicapp.database.UserDAO;
import com.musicapp.database.SocialDAO;
import com.musicapp.models.Playlist;
import com.musicapp.models.Song;
import com.musicapp.models.User;
import com.musicapp.services.MusicPlayerService;
import com.musicapp.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.io.File;

public class PlaylistViewController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistViewController.class);

    @FXML private Label playlistNameLabel;
    @FXML private Label playlistDescriptionLabel;
    @FXML private Label playlistInfoLabel;
    @FXML private ImageView playlistCoverImageView;
    @FXML private Button playButton;
    @FXML private Button shuffleButton;
    @FXML private Button shareButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private TableView<Song> songsTableView;
    @FXML private TableColumn<Song, Integer> indexColumn;
    @FXML private TableColumn<Song, String> titleColumn;
    @FXML private TableColumn<Song, String> artistColumn;
    @FXML private TableColumn<Song, String> albumColumn;
    @FXML private TableColumn<Song, String> durationColumn;
    @FXML private Button addSongButton;
    
    private final PlaylistDAO playlistDAO = new PlaylistDAO();
    private final SongDAO songDAO = new SongDAO();
    private final MusicPlayerService playerService = MusicPlayerService.getInstance();
    
    private Playlist currentPlaylist;
    private final ObservableList<Song> playlistSongs = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up songs table view
        songsTableView.setItems(playlistSongs);
        
        // Set up table columns
        indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));
        albumColumn.setCellValueFactory(new PropertyValueFactory<>("album"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        
        // Add row click handler
        songsTableView.setOnMouseClicked(this::handleSongSelection);
        
        // Set default playlist cover
        try {
            URL imageUrl = getClass().getResource("/images/default_playlist.png");
            if (imageUrl != null) {
                playlistCoverImageView.setImage(new Image(imageUrl.toString()));
            }
        } catch (Exception e) {
            logger.error("Failed to load default playlist image", e);
        }
    }
    
    public void setPlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;
        
        // Update UI with playlist info
        playlistNameLabel.setText(playlist.getName());
        playlistDescriptionLabel.setText(playlist.getDescription());
        
        // Load playlist songs
        loadPlaylistSongs();
        
        // Set playlist cover if available
        if (playlist.getCoverImage() != null && !playlist.getCoverImage().isEmpty()) {
            playlistCoverImageView.setImage(new Image("file:" + playlist.getCoverImage()));
        }
        
        // Check if user owns this playlist
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getId() == playlist.getUserId();
        
        // Show/hide edit controls based on ownership
        editButton.setVisible(isOwner);
        deleteButton.setVisible(isOwner);
    }
    
    private void loadPlaylistSongs() {
        if (currentPlaylist == null) return;
        
        new Thread(() -> {
            List<Song> songs = playlistDAO.getPlaylistSongs(currentPlaylist.getId());
            currentPlaylist.setSongs(songs);
            
            Platform.runLater(() -> {
                playlistSongs.setAll(songs);
                updatePlaylistInfo();
            });
        }).start();
    }
    
    private void updatePlaylistInfo() {
        int songCount = playlistSongs.size();
        String songText = songCount == 1 ? "song" : "songs";
        String duration = currentPlaylist.getFormattedTotalDuration();
        
        playlistInfoLabel.setText(songCount + " " + songText + " • " + duration);
    }
    
    @FXML
    private void handlePlayButton() {
        if (playlistSongs.isEmpty()) {
            showAlert("Information", "This playlist is empty");
            return;
        }
        
        playerService.setPlaylist(playlistSongs);
        playerService.playSong(playlistSongs.get(0));
    }
    
    @FXML
    private void handleShuffleButton() {
        if (playlistSongs.isEmpty()) {
            showAlert("Information", "This playlist is empty");
            return;
        }
        
        playerService.shuffleProperty().set(true);
        playerService.setPlaylist(playlistSongs);
        playerService.playSong(playlistSongs.get(0));
    }
    
    @FXML
    private void handleEditButton() {
        if (currentPlaylist == null) return;
        
        Dialog<Playlist> dialog = new Dialog<>();
        dialog.setTitle("Edit Playlist");
        dialog.setHeaderText("Edit playlist details");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the form fields
        TextField nameField = new TextField(currentPlaylist.getName());
        TextArea descriptionArea = new TextArea(currentPlaylist.getDescription());
        descriptionArea.setPrefRowCount(3);
        
        // Create and populate the grid
        dialog.getDialogPane().setContent(createEditForm(nameField, descriptionArea));
        
        // Convert the result to a playlist when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                currentPlaylist.setName(nameField.getText());
                currentPlaylist.setDescription(descriptionArea.getText());
                return currentPlaylist;
            }
            return null;
        });
        
        Optional<Playlist> result = dialog.showAndWait();
        result.ifPresent(playlist -> {
            new Thread(() -> {
                boolean success = playlistDAO.updatePlaylist(playlist);
                Platform.runLater(() -> {
                    if (success) {
                        playlistNameLabel.setText(playlist.getName());
                        playlistDescriptionLabel.setText(playlist.getDescription());
                        showAlert("Success", "Playlist updated successfully");
                    } else {
                        showError("Failed to update playlist");
                    }
                });
            }).start();
        });
    }
    
    @FXML
    private void handleDeleteButton() {
        if (currentPlaylist == null) return;
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Playlist");
        confirmDialog.setHeaderText("Are you sure you want to delete this playlist?");
        confirmDialog.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) return;
            
            new Thread(() -> {
                boolean success = playlistDAO.deletePlaylist(currentPlaylist.getId(), currentUser.getId());
                Platform.runLater(() -> {
                    if (success) {
                        // Navigate back to home
                        Button homeButton = new Button();
                        homeButton.fire();
                        showAlert("Success", "Playlist deleted successfully");
                    } else {
                        showError("Failed to delete playlist");
                    }
                });
            }).start();
        }
    }
    
    @FXML
    private void handleAddSongButton() {
        if (currentPlaylist == null) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Music Files");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(songsTableView.getScene().getWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            new Thread(() -> {
                for (File file : selectedFiles) {
                    Song song = new Song();
                    song.setTitle(file.getName().replaceFirst("[.][^.]+$", ""));
                    song.setFilePath(file.getAbsolutePath());
                    song.setArtist("Unknown Artist");
                    
                    int songId = songDAO.addSong(song);
                    if (songId > 0) {
                        song.setId(songId);
                        boolean success = playlistDAO.addSongToPlaylist(currentPlaylist.getId(), songId);
                        if (success) {
                            Platform.runLater(() -> {
                                playlistSongs.add(song);
                                updatePlaylistInfo();
                                showAlert("Success", "Song added to playlist");
                            });
                        }
                    }
                }
            }).start();
        }
    }
    
    @FXML
    private void handleRemoveSongButton() {
        if (currentPlaylist == null) return;
        
        Song selectedSong = songsTableView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) {
            showAlert("Information", "Please select a song to remove");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Remove Song");
        confirmDialog.setHeaderText("Remove song from playlist");
        confirmDialog.setContentText("Are you sure you want to remove '" + selectedSong.getTitle() + "' from this playlist?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                boolean success = playlistDAO.removeSongFromPlaylist(currentPlaylist.getId(), selectedSong.getId());
                Platform.runLater(() -> {
                    if (success) {
                        playlistSongs.remove(selectedSong);
                        currentPlaylist.removeSong(selectedSong);
                        updatePlaylistInfo();
                        showAlert("Success", "Song removed from playlist");
                    } else {
                        showError("Failed to remove song from playlist");
                    }
                });
            }).start();
        }
    }
    
    @FXML
    private void handleSongSelection(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Song selectedSong = songsTableView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                playerService.setPlaylist(playlistSongs);
                playerService.playSong(selectedSong);
            }
        }
    }
    
    @FXML
    private void handleShareButton() {
        if (currentPlaylist == null) return;
        
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Share Playlist");
        dialog.setHeaderText("Share \"" + currentPlaylist.getName() + "\" with another user");
        
        // Set the button types
        ButtonType shareButtonType = new ButtonType("Share", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(shareButtonType, ButtonType.CANCEL);
        
        // Create the username field
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username or email");
        
        // Create and populate the grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Share with:"), 0, 0);
        grid.add(usernameField, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        // Enable/Disable share button depending on whether a username was entered
        Node shareButton = dialog.getDialogPane().lookupButton(shareButtonType);
        shareButton.setDisable(true);
        
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            shareButton.setDisable(newValue.trim().isEmpty());
        });
        
        // Convert the result to a user when the share button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == shareButtonType) {
                String username = usernameField.getText().trim();
                UserDAO userDAO = new UserDAO();
                return userDAO.findByUsername(username);
            }
            return null;
        });
        
        Optional<User> result = dialog.showAndWait();
        result.ifPresent(user -> {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) return;
            
            SocialDAO socialDAO = new SocialDAO();
            new Thread(() -> {
                boolean success = socialDAO.sharePlaylist(currentPlaylist.getId(), currentUser.getId(), user.getId());
                Platform.runLater(() -> {
                    if (success) {
                        showAlert("Success", "Playlist shared with " + user.getUsername());
                    } else {
                        showError("Failed to share playlist");
                    }
                });
            }).start();
        });
    }
    
    private javafx.scene.Node createEditForm(TextField nameField, TextArea descriptionArea) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        return grid;
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        showAlert("Error", message);
    }
    
    private static class SongListCell extends ListCell<Song> {
        @Override
        protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);
            
            if (empty || song == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(song.getTitle() + " - " + song.getArtist());
            }
        }
    }
}