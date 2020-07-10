package org.beynet.controller;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.gui.Alert;
import org.beynet.gui.GoogleDriveVisualState;
import org.beynet.gui.OneDriveVisualState;
import org.beynet.model.Config;
import org.beynet.model.MainPasswordError;
import org.beynet.model.password.Password;
import org.beynet.model.Observer;
import org.beynet.sync.googledrive.GoogleDriveSync;
import org.beynet.sync.onedrive.OneDriveSync;
import org.beynet.utils.I18NHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * Created by beynet on 16/10/2014.
 */
public class Controller {

    private static Thread gdriveSyncThread = null;
    private static Thread oneDriveSyncThread = null;

    public static void enableGoogleDriveSync(Stage mainStage) {
        Platform.runLater(() -> {
            synchronized (Controller.class) {
                //verify that previous thread is dead
                if (gdriveSyncThread!=null && gdriveSyncThread.isAlive()) {
                    new Alert(mainStage,"previous gdrive thread is not dead").show();
                    return;
                }
                else {
                    gdriveSyncThread = new Thread(GoogleDriveSync.getInstance());
                    gdriveSyncThread.start();
                }
            }
        });
    }

    public static void enableOneDriveSync(Stage mainStage) {
        Platform.runLater(() -> {
            synchronized (Controller.class) {
                //verify that previous thread is dead
                if (oneDriveSyncThread!=null && oneDriveSyncThread.isAlive()) {
                    new Alert(mainStage,"previous onedrive thread is not dead").show();
                    return;
                }
                else {
                    oneDriveSyncThread = new Thread(OneDriveSync.getInstance());
                    oneDriveSyncThread.start();
                }
            }
        });
    }


    public static void subscribeToGDriveSyncStatusChange(GoogleDriveVisualState googleDriveVisualState) {
        GoogleDriveSync.getInstance().addObserver(googleDriveVisualState);
        googleDriveVisualState.update(GoogleDriveSync.getInstance(),GoogleDriveSync.getInstance().getGoogleDriveSyncState());
    }

    public static void subscribeToOneDriveSyncStatusChange(OneDriveVisualState oneDriveVisualState) {
        OneDriveSync.getInstance().addObserver(oneDriveVisualState);
        oneDriveVisualState.update(OneDriveSync.getInstance(),OneDriveSync.getInstance().getOneDriveSyncState());
    }

    public static void rebuildIndexes() {
        Platform.runLater(() -> {
            Config.getInstance().getPasswordStore().reIndexeLuceneDataBase();
        });
    }

    /**
     * remove all deleted passwords from database
     */
    public static void compressDatabase(Stage mainStage) {
        final ResourceBundle labelResourceBundle = I18NHelper.getLabelResourceBundle();
        Platform.runLater(()->{
            int total = Config.getInstance().getPasswordStore().removeDeletedPasswords();
            String message = labelResourceBundle.getString("deletedpasswordremoved");
            new Alert(mainStage,message.replaceAll("VAL",""+total)).show();
        });
    }

    public static void disableGoogleDriveSync() {
        Platform.runLater(() -> {
            synchronized (Controller.class) {
                if (gdriveSyncThread != null && gdriveSyncThread.isAlive()) {
                    gdriveSyncThread.interrupt();
                    Config.getInstance().removeGoogleDrivePassword();
                }
            }
        });
    }

    public static void disableOneDriveSync() {
        Platform.runLater(() -> {
            synchronized (Controller.class) {
                if (oneDriveSyncThread != null && oneDriveSyncThread.isAlive()) {
                    oneDriveSyncThread.interrupt();
                    Config.getInstance().removeOneDrivePassword();
                }
            }
        });
    }

    /**
     * subsribe to changes from password database
     * @param suscriber
     */
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

    public static void notifyPasswordModified(Password previousValue,Password newValue) {
        Platform.runLater(() -> {
            if (previousValue==null) {
                Config.getInstance().getPasswordStore().savePassword(newValue);
            }
            else {
                Config.getInstance().getPasswordStore().savePassword(previousValue.refresh(newValue));
            }
        });
    }

    public static Map<String, Password> getMatching(String filter) {
        try {
            return Config.getInstance().getPasswordStore().search(filter);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public static void initConfig(String text, Path savePath, String fileName) throws MainPasswordError {
        Config.initConfig(text, savePath, fileName);
    }

    public final static Logger logger = Logger.getLogger(Controller.class);
}


