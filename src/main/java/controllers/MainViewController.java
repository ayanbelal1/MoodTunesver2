package com.musicapp.controllers;

import com.musicapp.database.PlaylistDAO;
import com.musicapp.database.SongDAO;
import com.musicapp.models.Playlist;
import com.musicapp.models.Song;
import com.musicapp.models.User;
import com.musicapp.services.MusicPlayerService;
import com.musicapp.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MainViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    
    @FXML private BorderPane mainPane;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebarVBox;
    @FXML private Label userNameLabel;
    @FXML private TextField searchField;
    @FXML private ListView<Playlist> playlistListView;
    
    @FXML private Button playPauseButton;
    @FXML private FontIcon playPauseIcon;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Button shuffleButton;
    @FXML private FontIcon shuffleIcon;
    @FXML private Button repeatButton;
    @FXML private FontIcon repeatIcon;
    @FXML private FontIcon volumeIcon;
    @FXML private Slider progressSlider;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label songTitleLabel;
    @FXML private Label artistLabel;
    @FXML private ImageView albumArtView;
    @FXML private Slider volumeSlider;
    
    // Sidebar buttons
    @FXML private Button homeButton;
    @FXML private Button searchButton;
    @FXML private Button libraryButton;
    @FXML private Button createPlaylistButton;
    @FXML private Button youtubeButton;
    @FXML private Button spotifyButton;
    @FXML private Button cloudStorageButton;
    @FXML private Button socialButton;
    @FXML private Button userProfileButton;
    @FXML private HBox guestActionsBox;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    
    private final MusicPlayerService playerService = MusicPlayerService.getInstance();
    private final SongDAO songDAO = new SongDAO();
    private final PlaylistDAO playlistDAO = new PlaylistDAO();
    private final ObservableList<Playlist> userPlaylists = FXCollections.observableArrayList();
    
    private final Image defaultAlbumImage;
    
    private Stage mainStage;
    private MiniPlayerController miniPlayerController = null;
    private Stage miniPlayerStage = null;
    private double xOffset, yOffset;
    private boolean isMiniPlayerActive = false;
    
    public MainViewController() {
        System.out.println("MainViewController: " + this + " MusicPlayerService: " + MusicPlayerService.getInstance());
        defaultAlbumImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/app_icon.png")));
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPlayerControls();
        loadUserPlaylists();
        setupUserInfo();
        setupGuestRestrictions();
        
        mainPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                mainStage = (Stage) newScene.getWindow();
            }
        });
        
        playlistListView.setItems(userPlaylists);
        playlistListView.setCellFactory(param -> new PlaylistListCell());
        playlistListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
                if (selectedPlaylist != null) {
                    openPlaylistView(selectedPlaylist);
                }
            }
        });
        
        loadView("/fxml/HomeView.fxml");
    }
    
    private void loadUserPlaylists() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        new Thread(() -> {
            List<Playlist> playlists = playlistDAO.getUserPlaylists(currentUser.getId());
            Platform.runLater(() -> userPlaylists.setAll(playlists));
        }).start();
    }
    
    private void setupPlayerControls() {
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                playerService.seek(newVal.doubleValue() / 100.0);
            }
        });
        
        volumeSlider.valueProperty().bindBidirectional(playerService.volumeProperty());
        currentTimeLabel.textProperty().bind(playerService.currentTimeProperty());
        totalTimeLabel.textProperty().bind(playerService.totalTimeProperty());
        
        // Update play/pause button based on player state
        playerService.playingProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                playPauseIcon.setIconLiteral(newVal ? "fas-pause" : "fas-play");
                if (miniPlayerController != null) {
                    miniPlayerController.refreshUI();
                }
            });
        });
        
        // Set initial play/pause icon state
        playPauseIcon.setIconLiteral(playerService.playingProperty().get() ? "fas-pause" : "fas-play");
        
        playerService.shuffleProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> shuffleIcon.setIconColor(newVal ? Color.GREEN : Color.WHITE));
        });
        
        playerService.repeatProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> repeatIcon.setIconColor(newVal ? Color.GREEN : Color.WHITE));
        });
        
        playerService.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (!progressSlider.isValueChanging()) {
                Platform.runLater(() -> {
                    progressSlider.setValue(newVal.doubleValue() * 100);
                    if (miniPlayerController != null) {
                        miniPlayerController.updateProgress(newVal.doubleValue() * 100);
                    }
                });
            }
        });
        
        playerService.volumeProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateVolumeIcon(newVal.doubleValue()));
        });
        
        // Update current song info and sync with mini player
        playerService.currentSongProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                updateSongInfo(newVal);
                if (miniPlayerController != null) {
                    miniPlayerController.refreshUI();
                }
            });
        });
        
        // Sync volume changes with mini player
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (miniPlayerController != null) {
                miniPlayerController.updateVolume(newVal.doubleValue());
            }
        });
        
        // Set initial states
        updateSongInfo(playerService.getCurrentSong());
        updateVolumeIcon(playerService.volumeProperty().get());
        shuffleIcon.setIconColor(playerService.shuffleProperty().get() ? Color.GREEN : Color.WHITE);
        repeatIcon.setIconColor(playerService.repeatProperty().get() ? Color.GREEN : Color.WHITE);
    }
    
    private void updateSongInfo(Song song) {
        if (song != null) {
            songTitleLabel.setText(song.getTitle());
            artistLabel.setText(song.getArtist());
            if (song.getCoverArt() != null && !song.getCoverArt().isEmpty()) {
                albumArtView.setImage(new Image(song.getCoverArt()));
            } else {
                albumArtView.setImage(defaultAlbumImage);
            }
        } else {
            songTitleLabel.setText("No song playing");
            artistLabel.setText("");
            albumArtView.setImage(defaultAlbumImage);
        }
    }
    
    private void updateVolumeIcon(double volume) {
        String iconLiteral;
        if (volume == 0) {
            iconLiteral = "fas-volume-mute";
        } else if (volume < 0.5) {
            iconLiteral = "fas-volume-down";
        } else {
            iconLiteral = "fas-volume-up";
        }
        volumeIcon.setIconLiteral(iconLiteral);
        volumeIcon.setIconColor(Color.WHITE);
    }
    
    @FXML
    private void handlePlayPauseButton() {
        if (playerService.getCurrentSong() == null) {
            if (!playerService.getPlaylist().isEmpty()) {
                playerService.playSong(playerService.getPlaylist().get(0));
            }
            return;
        }
        
        if (playerService.playingProperty().get()) {
            playerService.pause();
        } else {
            playerService.play();
        }
    }
    
    @FXML
    private void handlePreviousButton() {
        playerService.playPrevious();
    }
    
    @FXML
    private void handleNextButton() {
        playerService.playNext();
    }
    
    @FXML
    private void handleShuffleButton() {
        playerService.shuffleProperty().set(!playerService.shuffleProperty().get());
    }
    
    @FXML
    private void handleRepeatButton() {
        playerService.repeatProperty().set(!playerService.repeatProperty().get());
    }
    
    @FXML
    private void handleHomeButton() {
        loadView("/fxml/HomeView.fxml");
    }
    
    @FXML
    private void handleSearchButton() {
        if (searchField.getText().trim().isEmpty()) {
            return;
        }
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SearchResultsView.fxml"));
        try {
            Parent view = loader.load();
            SearchResultsController controller = loader.getController();
            controller.performSearch(searchField.getText().trim());
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            logger.error("Error loading search results", e);
            showError("Error loading search results: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLibraryButton() {
        loadView("/fxml/LibraryView.fxml");
    }
    
    @FXML
    private void handleCreatePlaylistButton() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Enter a name for your new playlist");
        dialog.setContentText("Playlist name:");
        
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser == null) return;
                
                Playlist newPlaylist = new Playlist(currentUser.getId(), name.trim(), "", null);
                
                new Thread(() -> {
                    boolean success = playlistDAO.createPlaylist(newPlaylist);
                    Platform.runLater(() -> {
                        if (success) {
                            userPlaylists.add(newPlaylist);
                            showAlert("Playlist created successfully!");
                        } else {
                            showError("Failed to create playlist");
                        }
                    });
                }).start();
            }
        });
    }
    
    @FXML
    private void handleYouTubeButton() {
        loadView("/fxml/YouTubeView.fxml");
    }
    
    @FXML
    private void handleLogoutButton() {
        playerService.stop();
        SessionManager.getInstance().clearSession();
        redirectToLogin();
    }
    
    @FXML
    private void handleMinimizeToMiniPlayer() {
        if (mainStage == null) {
            mainStage = (Stage) mainPane.getScene().getWindow();
        }
        try {
            if (miniPlayerController == null) {
                miniPlayerController = new MiniPlayerController();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MiniPlayerView.fxml"));
                loader.setController(miniPlayerController);
                Parent miniPlayerRoot = loader.load();

                miniPlayerStage = new Stage(StageStyle.TRANSPARENT);
                Scene miniScene = new Scene(miniPlayerRoot);
                miniScene.setFill(null);
                miniPlayerStage.setScene(miniScene);

                // Position the mini player
                Screen screen = Screen.getPrimary();
                miniPlayerStage.setX(screen.getVisualBounds().getMaxX() - miniScene.getWidth() - 20);
                miniPlayerStage.setY(screen.getVisualBounds().getMaxY() - miniScene.getHeight() - 20);

                miniPlayerController.setMainStage(mainStage);
                miniPlayerController.setMainViewController(this);
            }

            // Always refresh the mini player UI with the current song
            miniPlayerController.refreshUI();
            miniPlayerController.updateVolume(volumeSlider.getValue());
            miniPlayerController.updateProgress(progressSlider.getValue());

            // Set up fade transitions
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), miniPlayerStage.getScene().getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            fadeOut.setOnFinished(e -> {
                double currentOpacity = mainPane.getOpacity();
                mainStage.hide();
                mainPane.setOpacity(currentOpacity);
                miniPlayerStage.show();
                fadeIn.play();
                isMiniPlayerActive = true;
            });

            fadeOut.play();
        } catch (IOException e) {
            logger.error("Error loading mini player", e);
            showError("Failed to load mini player: " + e.getMessage());
        }
    }
    
    public void maximizeFromMiniPlayer() {
        isMiniPlayerActive = false;
        if (miniPlayerController != null) {
            miniPlayerController.refreshUI();
        }
    }
    
    public boolean isMiniPlayerActive() {
        return isMiniPlayerActive;
    }
    
    @FXML
    private void handleQueueButton() {
        loadView("/fxml/QueueView.fxml");
    }
    
    @FXML
    private void handleSpotifyButton() {
        loadView("/fxml/SpotifyImportView.fxml");
    }
    
    @FXML
    private void handleCloudStorageButton() {
        loadView("/fxml/CloudStorageView.fxml");
    }
    
    @FXML
    private void handleSocialButton() {
        loadView("/fxml/SocialView.fxml");
    }
    
    @FXML
    private void handleUserProfileButton() {
        loadView("/fxml/UserProfileView.fxml");
    }
    
    private void openPlaylistView(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlaylistView.fxml"));
            Parent view = loader.load();
            PlaylistViewController controller = loader.getController();
            controller.setPlaylist(playlist);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            logger.error("Error loading playlist view", e);
            showError("Error loading playlist view: " + e.getMessage());
        }
    }
    
    public void openPlaylist(Playlist playlist) {
        openPlaylistView(playlist);
    }
    
    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            logger.error("Error loading view: " + fxmlPath, e);
            showError("Error loading view: " + e.getMessage());
        }
    }
    
    private void redirectToLogin() {
        try {
            Parent loginView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            Scene scene = new Scene(loginView);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/styles.css")).toExternalForm());
            
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            logger.error("Error redirecting to login", e);
            System.exit(1);
        }
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
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void setupUserInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getUsername());
            guestActionsBox.setVisible(currentUser.isGuest());
            guestActionsBox.setManaged(currentUser.isGuest());
        }
    }
    
    private void setupGuestRestrictions() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isGuest()) {
            // Add lock icons to restricted features
            addLockIcon(youtubeButton, "YouTube integration requires login");
            addLockIcon(spotifyButton, "Spotify integration requires login");
            addLockIcon(cloudStorageButton, "Cloud storage requires login");
            addLockIcon(socialButton, "Social features require login");
            addLockIcon(userProfileButton, "User profile requires login");
            
            // Disable restricted features
            youtubeButton.setDisable(true);
            spotifyButton.setDisable(true);
            cloudStorageButton.setDisable(true);
            socialButton.setDisable(true);
            userProfileButton.setDisable(true);
            
            // Disable playlist creation
            createPlaylistButton.setDisable(true);
            addLockIcon(createPlaylistButton, "Creating playlists requires login");
            
            // Update user name label
            userNameLabel.setText("Guest User");
        }
    }
    
    private void addLockIcon(Button button, String tooltipText) {
        HBox container = new HBox(5);
        container.setAlignment(Pos.CENTER_LEFT);
        
        // Get existing content
        Node existingGraphic = button.getGraphic();
        if (existingGraphic != null) {
            container.getChildren().add(existingGraphic);
        }
        
        // Add lock icon
        FontIcon lockIcon = new FontIcon("fas-lock");
        lockIcon.setIconColor(Color.GRAY);
        container.getChildren().add(lockIcon);
        
        button.setGraphic(container);
        button.setTooltip(new Tooltip(tooltipText));
    }
    
    @FXML
    private void handleLoginButton() {
        redirectToLogin();
    }
    
    @FXML
    private void handleRegisterButton() {
        try {
            Parent registerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Register.fxml")));
            Scene scene = new Scene(registerView);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/styles.css")).toExternalForm());
            
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            logger.error("Error redirecting to register", e);
            showError("Error loading register view: " + e.getMessage());
        }
    }
    
    private static class PlaylistListCell extends ListCell<Playlist> {
        @Override
        protected void updateItem(Playlist playlist, boolean empty) {
            super.updateItem(playlist, empty);
            
            if (empty || playlist == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(playlist.getName());
            }
        }
    }
} 