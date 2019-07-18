package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.model.Observable;
import org.beynet.model.Observer;
import org.beynet.model.password.GoogleDrive;
import org.beynet.model.store.PasswordModifiedOrCreated;
import org.beynet.model.store.PasswordRemoved;


/**
 * Created by beynet on 07/11/14.
 */
public class GDriveSyncCheckMenu extends CheckMenuItem implements Observer {

    private Stage applicationMainStage;

    public GDriveSyncCheckMenu(Stage applicationMainStage,String label) {
        super(label);
        this.applicationMainStage=applicationMainStage;
        setSelected(false);
        Controller.suscribeToPassword(this);
        setOnAction(evt->{
            if (this.isSelected()) {
                Controller.enableGoogleDriveSync(this.applicationMainStage);
            }
            else {
                Controller.disableGoogleDriveSync();
            }
        });
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg!=null && arg instanceof PasswordModifiedOrCreated) {
            if (((PasswordModifiedOrCreated) arg).getPassword() instanceof GoogleDrive) {
                if (isSelected() == false) {
                    Platform.runLater(() -> setSelected(true));
                    Controller.enableGoogleDriveSync(this.applicationMainStage);
                }
                gdriveId = ((PasswordModifiedOrCreated) arg).getPassword().getId();
            }
        }
        else if (arg!=null && arg instanceof PasswordRemoved) {
            if (gdriveId!=null && ((PasswordRemoved) arg).getPassword().getId().equals(gdriveId)) {
                if (isSelected()==true) {
                    Platform.runLater(() -> setSelected(false));
                }
                Controller.disableGoogleDriveSync();
            }
            //Controller.enableGoogleDriveSync();
        }
    }

    String gdriveId = null;
}
