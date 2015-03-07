package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.beynet.controller.Controller;
import org.beynet.sync.googledrive.GoogleDriveSyncState;
import org.beynet.utils.I18NHelper;

import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;


/**
 * This class will display a visual state of google drive sync status
 */
public class GoogleDriveVisualState extends HBox implements Observer{
    public GoogleDriveVisualState() {
        super(8);
        final ResourceBundle labelResourceBundle = I18NHelper.getLabelResourceBundle();
        Label gdrive = new Label(labelResourceBundle.getString("gdrivesyncstatus"));
        visualState= new ImageView(OFF);
        visualState.setFitWidth(24);
        visualState.setFitHeight(24);

        getChildren().addAll(gdrive,visualState);
        Controller.subscribeToGDriveSyncStatusChange(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (GoogleDriveSyncState.SEND_FILE.equals(arg)) {
            Platform.runLater(() -> visualState.setImage(UP));
        }else if (GoogleDriveSyncState.WAIT_FOR_CHANGE.equals(arg)) {
            Platform.runLater(() -> visualState.setImage(OK));
        }
        else {
            Platform.runLater(() -> visualState.setImage(OFF));
        }
    }

    private ImageView visualState ;

    private static final Image OK = new Image(PasswordOrFolderTreeNode.class.getResourceAsStream("/ok.png"));
    private static final Image OFF = new Image(PasswordOrFolderTreeNode.class.getResourceAsStream("/off.png"));
    private static final Image UP = new Image(PasswordOrFolderTreeNode.class.getResourceAsStream("/upload.png"));
}
