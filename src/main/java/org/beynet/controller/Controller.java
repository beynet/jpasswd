package org.beynet.controller;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.gui.Alert;
import org.beynet.model.Config;
import org.beynet.model.password.Password;
import org.beynet.sync.googledrive.GoogleDriveSync;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by beynet on 16/10/2014.
 */
public class Controller {

    private static GoogleDriveSync gdriveSync = null;
    private static Thread gdriveSyncThread = null;

    public static void enableGoogleDriveSync(Stage mainStage) {
        Platform.runLater(() -> {
            synchronized (Controller.class) {
                if (gdriveSync == null) {
                    //verify that previous thread is dead
                    if (gdriveSyncThread!=null && gdriveSyncThread.isAlive()) {
                        new Alert(mainStage,"previous gdrive thread is not dead").show();
                        return;
                    }
                    else {
                        gdriveSync = new GoogleDriveSync(mainStage);
                        gdriveSyncThread = new Thread(gdriveSync);
                        gdriveSyncThread.start();
                    }
                }
            }
        });
    }


    public static void rebuildIndexes() {
        Platform.runLater(() -> {
            Config.getInstance().getPasswordStore().reIndexeLuceneDataBase();
        });
    }

    public static void disableGoogleDriveSync() {
        Platform.runLater(() -> {
            synchronized (Controller.class) {
                if (gdriveSync != null) {
                    gdriveSyncThread.interrupt();
                    Config.getInstance().removeGoogleDrivePassword();
                    gdriveSync=null;
                }
            }
        });
    }

    public static void suscribeToPassword(Observer suscriber) {
        Platform.runLater(() -> {
            Config.getInstance().getPasswordStore().addObserver(suscriber);
            Config.getInstance().getPasswordStore().sendAllPasswords(suscriber);
        });
    }

    public static void notifyPasswordRemoved(String id) {
        Platform.runLater(() -> {
            Config.getInstance().getPasswordStore().removePassword(id);
        });
    }

    public static void changeMainPassword(String password) {
        Platform.runLater(() -> {
            Config.getInstance().changeMainPassword(password);
            try {
                Config.getInstance().getPasswordStore().save();
            } catch (IOException e) {
                logger.error("error saving password file", e);
            }
        });
    }

    public static void notifyPasswordModified(Password p) {
        Platform.runLater(() -> {
            Config.getInstance().getPasswordStore().savePassword(p);
        });
    }

    public static Map<String, Password> getMatching(String filter) {
        try {
            return Config.getInstance().getPasswordStore().search(filter);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public static void initConfig(String text, Path savePath, String fileName) {
        Config.initConfig(text, savePath, fileName);
    }

    public final static Logger logger = Logger.getLogger(Controller.class);

}


