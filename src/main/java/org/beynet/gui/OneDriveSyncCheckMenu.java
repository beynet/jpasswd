package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.model.Observable;
import org.beynet.model.Observer;
import org.beynet.model.password.OneDrive;
import org.beynet.model.store.PasswordModifiedOrCreated;
import org.beynet.model.store.PasswordRemoved;


/**
 * Created by beynet on 07/11/14.
 */
public class OneDriveSyncCheckMenu extends CheckMenuItem implements Observer {

    private Stage applicationMainStage;

    public OneDriveSyncCheckMenu(Stage applicationMainStage, String label) {
        super(label);
        this.applicationMainStage=applicationMainStage;
        setSelected(false);
        Controller.suscribeToPassword(this);
        setOnAction(evt->{
            if (this.isSelected()) {
                Controller.enableOneDriveSync(this.applicationMainStage);
            }
            else {
                Controller.disableOneDriveSync();
            }
        });
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg!=null && arg instanceof PasswordModifiedOrCreated) {
            if (((PasswordModifiedOrCreated) arg).getPassword() instanceof OneDrive) {
                if (isSelected() == false) {
                    Platform.runLater(() -> setSelected(true));
                    Controller.enableOneDriveSync(this.applicationMainStage);
                }
                onedriveid = ((PasswordModifiedOrCreated) arg).getPassword().getId();
            }
        }
        else if (arg!=null && arg instanceof PasswordRemoved) {
            if (onedriveid !=null && ((PasswordRemoved) arg).getPassword().getId().equals(onedriveid)) {
                if (isSelected()==true) {
                    Platform.runLater(() -> setSelected(false));
                }
                Controller.disableOneDriveSync();
            }
            //Controller.enableGoogleDriveSync();
        }
    }

    String onedriveid = null;
}
