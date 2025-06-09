package com.musicapp.controllers;

import com.musicapp.models.Song;
import com.musicapp.services.QueueService;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.control.Button;

public class QueueViewController {
    
    @FXML
    private ListView<Song> queueListView;
    
    private final QueueService queueService = QueueService.getInstance();
    
    @FXML
    public void initialize() {
        // Bind the ListView to the queue
        queueListView.setItems(queueService.getQueue());
        
        // Set up custom cell factory for queue items
        queueListView.setCellFactory(param -> new QueueListCell());
        
        // Set up drag and drop for reordering
        setupDragAndDrop();
    }
    
    @FXML
    private void clearQueue() {
        queueService.clearQueue();
    }
    
    private void setupDragAndDrop() {
        queueListView.setOnDragDetected(event -> {
            if (queueListView.getSelectionModel().getSelectedItem() != null) {
                Dragboard db = queueListView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(queueListView.getSelectionModel().getSelectedIndex()));
                db.setContent(content);
                event.consume();
            }
        });
        
        queueListView.setOnDragOver(event -> {
            if (event.getGestureSource() == queueListView && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        
        queueListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString()) {
                int draggedIdx = Integer.parseInt(db.getString());
                int dropIdx = Math.min(queueListView.getItems().size(),
                    Math.max(0, (int)((event.getY() - queueListView.getBoundsInLocal().getMinY()) / 
                        queueListView.getFixedCellSize())));
                
                queueService.moveInQueue(draggedIdx, dropIdx);
                success = true;
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    private static class QueueListCell extends ListCell<Song> {
        private final HBox content;
        private final Label title;
        private final Label artist;
        private final Button removeButton;
        
        public QueueListCell() {
            content = new HBox();
            content.setSpacing(10);
            content.getStyleClass().add("queue-list-cell");
            
            title = new Label();
            title.getStyleClass().add("song-title");
            
            artist = new Label();
            artist.getStyleClass().add("song-artist");
            
            VBox textBox = new VBox(title, artist);
            textBox.setSpacing(5);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            removeButton = new Button();
            removeButton.setGraphic(new FontIcon("fas-times"));
            removeButton.getStyleClass().add("icon-button");
            removeButton.setOnAction(e -> {
                Song song = getItem();
                if (song != null) {
                    QueueService.getInstance().removeFromQueue(song);
                }
            });
            
            content.getChildren().addAll(textBox, spacer, removeButton);
        }
        
        @Override
        protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);
            
            if (empty || song == null) {
                setGraphic(null);
            } else {
                title.setText(song.getTitle());
                artist.setText(song.getArtist());
                setGraphic(content);
            }
        }
    }
} 