package com.musicapp.controllers;

import com.musicapp.database.SongDAO;
import com.musicapp.models.Song;
import com.musicapp.services.MusicPlayerService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SearchResultsController implements Initializable {

    @FXML private Label searchQueryLabel;
    @FXML private TableView<Song> resultsTableView;
    @FXML private TableColumn<Song, String> titleColumn;
    @FXML private TableColumn<Song, String> artistColumn;
    @FXML private TableColumn<Song, String> albumColumn;
    @FXML private TableColumn<Song, String> durationColumn;
    
    private final SongDAO songDAO = new SongDAO();
    private final MusicPlayerService playerService = MusicPlayerService.getInstance();
    
    private final ObservableList<Song> searchResults = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up table columns
        titleColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTitle()));
        
        artistColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getArtist()));
        
        albumColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getAlbum()));
        
        durationColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getFormattedDuration()));
        
        // Set table items
        resultsTableView.setItems(searchResults);
        
        // Add double-click handler
        resultsTableView.setOnMouseClicked(this::handleTableClick);
    }
    
    public void performSearch(String searchQuery) {
        searchQueryLabel.setText("Search results for: \"" + searchQuery + "\"");
        
        new Thread(() -> {
            List<Song> results = songDAO.searchSongs(searchQuery);
            Platform.runLater(() -> {
                searchResults.setAll(results);
                if (results.isEmpty()) {
                    searchQueryLabel.setText("No results found for: \"" + searchQuery + "\"");
                } else {
                    searchQueryLabel.setText("Found " + results.size() + " results for: \"" + searchQuery + "\"");
                }
            });
        }).start();
    }
    
    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Song selectedSong = resultsTableView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                playerService.setPlaylist(searchResults);
                playerService.playSong(selectedSong);
            }
        }
    }
}