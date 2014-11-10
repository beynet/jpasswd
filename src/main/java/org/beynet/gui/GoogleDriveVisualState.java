package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.beynet.controller.Controller;
import org.beynet.sync.googledrive.GoogleDriveSync;
import org.beynet.sync.googledrive.GoogleDriveSyncState;

import java.util.Observable;
import java.util.Observer;


/**
 * This class will display a visual state of google drive sync status
 */
public class GoogleDriveVisualState extends HBox implements Observer{
    public GoogleDriveVisualState() {
        Label gdrive = new Label("Google drive sync status");
        visualState= new ImageView(red);
        visualState.setFitWidth(24);
        visualState.setFitHeight(24);
        getChildren().addAll(gdrive,visualState);
        Controller.subscribeToGDriveSyncStatusChange(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (GoogleDriveSyncState.SEND_FILE.equals(arg)||GoogleDriveSyncState.WAIT_FOR_CHANGE.equals(arg)) {
            Platform.runLater(() -> visualState.setImage(green));
        }
        else {
            Platform.runLater(() -> visualState.setImage(red));
        }
    }

    private ImageView visualState ;

    private static final Image green = new Image(PasswordOrFolderTreeNode.class.getResourceAsStream("/Fix_G.gif"));
    private static final Image red = new Image(PasswordOrFolderTreeNode.class.getResourceAsStream("/Fix_R.gif"));
}
