package com.musicapp.controllers;

import com.musicapp.database.SocialDAO;
import com.musicapp.database.UserDAO;
import com.musicapp.models.Playlist;
import com.musicapp.models.User;
import com.musicapp.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class SocialViewController implements Initializable {
    
    @FXML private TextField searchUserField;
    @FXML private ListView<User> searchResultsListView;
    @FXML private ListView<User> followingListView;
    @FXML private ListView<User> followersListView;
    @FXML private ListView<Playlist> sharedPlaylistsListView;
    
    private final SocialDAO socialDAO = new SocialDAO();
    private final UserDAO userDAO = new UserDAO();
    private final User currentUser = SessionManager.getInstance().getCurrentUser();
    
    private final ObservableList<User> searchResults = FXCollections.observableArrayList();
    private final ObservableList<User> following = FXCollections.observableArrayList();
    private final ObservableList<User> followers = FXCollections.observableArrayList();
    private final ObservableList<Playlist> sharedPlaylists = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListViews();
        loadSocialData();
    }
    
    private void setupListViews() {
        // Set up search results list
        searchResultsListView.setItems(searchResults);
        searchResultsListView.setCellFactory(lv -> new UserListCell(true));
        
        // Set up following list
        followingListView.setItems(following);
        followingListView.setCellFactory(lv -> new UserListCell(false));
        
        // Set up followers list
        followersListView.setItems(followers);
        followersListView.setCellFactory(lv -> new UserListCell(false));
        
        // Set up shared playlists list
        sharedPlaylistsListView.setItems(sharedPlaylists);
        sharedPlaylistsListView.setCellFactory(lv -> new ListCell<>() {
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
        });
        
        // Handle playlist selection
        sharedPlaylistsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Playlist selectedPlaylist = sharedPlaylistsListView.getSelectionModel().getSelectedItem();
                if (selectedPlaylist != null) {
                    openPlaylist(selectedPlaylist);
                }
            }
        });
    }
    
    private void loadSocialData() {
        if (currentUser == null) return;
        
        // Load following
        new Thread(() -> {
            List<User> followingUsers = socialDAO.getFollowing(currentUser.getId());
            Platform.runLater(() -> following.setAll(followingUsers));
        }).start();
        
        // Load followers
        new Thread(() -> {
            List<User> followerUsers = socialDAO.getFollowers(currentUser.getId());
            Platform.runLater(() -> followers.setAll(followerUsers));
        }).start();
        
        // Load shared playlists
        new Thread(() -> {
            List<Playlist> shared = socialDAO.getSharedPlaylists(currentUser.getId());
            Platform.runLater(() -> sharedPlaylists.setAll(shared));
        }).start();
    }
    
    @FXML
    private void handleSearchUsers() {
        String searchTerm = searchUserField.getText().trim();
        if (searchTerm.isEmpty()) return;
        
        new Thread(() -> {
            List<User> users = userDAO.searchUsers(searchTerm);
            Platform.runLater(() -> searchResults.setAll(users));
        }).start();
    }
    
    private void handleFollowUser(User user) {
        if (currentUser == null || user == null) return;
        
        new Thread(() -> {
            boolean success = socialDAO.followUser(currentUser.getId(), user.getId());
            if (success) {
                Platform.runLater(() -> {
                    following.add(user);
                    showAlert("Success", "You are now following " + user.getUsername());
                });
            }
        }).start();
    }
    
    private void handleUnfollowUser(User user) {
        if (currentUser == null || user == null) return;
        
        new Thread(() -> {
            boolean success = socialDAO.unfollowUser(currentUser.getId(), user.getId());
            if (success) {
                Platform.runLater(() -> {
                    following.remove(user);
                    showAlert("Success", "You have unfollowed " + user.getUsername());
                });
            }
        }).start();
    }
    
    private void openPlaylist(Playlist playlist) {
        try {
            MainViewController mainController = (MainViewController) searchResultsListView.getScene()
                .getUserData();
            if (mainController != null) {
                mainController.openPlaylist(playlist);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error opening playlist: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private class UserListCell extends ListCell<User> {
        private final boolean showFollowButton;
        
        public UserListCell(boolean showFollowButton) {
            this.showFollowButton = showFollowButton;
        }
        
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            
            HBox container = new HBox(10);
            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label nameLabel = new Label(user.getUsername());
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            
            container.getChildren().add(nameLabel);
            
            if (showFollowButton && currentUser != null && user.getId() != currentUser.getId()) {
                Button followButton = new Button();
                followButton.getStyleClass().add("icon-button");
                
                boolean isFollowing = socialDAO.isFollowing(currentUser.getId(), user.getId());
                FontIcon icon = new FontIcon(isFollowing ? "fas-user-minus" : "fas-user-plus");
                icon.setIconColor(javafx.scene.paint.Color.WHITE);
                followButton.setGraphic(icon);
                
                followButton.setOnAction(e -> {
                    if (isFollowing) {
                        handleUnfollowUser(user);
                    } else {
                        handleFollowUser(user);
                    }
                });
                
                container.getChildren().add(followButton);
            }
            
            setGraphic(container);
        }
    }
} 