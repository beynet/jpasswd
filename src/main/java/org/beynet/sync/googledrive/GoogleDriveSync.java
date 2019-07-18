package org.beynet.sync.googledrive;

import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.model.Observable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by beynet on 25/10/14.
 */
public class GoogleDriveSync extends Observable implements Runnable {

    public static void init(Stage applicationMainStage) {
        synchronized (GoogleDriveSync.class) {
            if (_instance==null) {
                _instance = new GoogleDriveSync(applicationMainStage);
            }
        }
    }

    public static GoogleDriveSync getInstance() {
        return _instance;
    }

    private GoogleDriveSync(Stage applicationMainStage) {
        this.credentials = new HashMap<>();
        this.credentials.put(GoogleDriveSyncState.APPLICATION_MAIN_STAGE,applicationMainStage);
        this.syncState = GoogleDriveSyncState.STOP;
    }

    /**
     * @return the state of the synchronisation with google drive account
     */
    public GoogleDriveSyncState getGoogleDriveSyncState() {
        return syncState;
    }
    public void setGoogleDriveSyncState(GoogleDriveSyncState state) {
        this.syncState = state;
        setChanged();
        notifyObservers(state);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("GoogleDriveSync");
        this.syncState = GoogleDriveSyncState.START;
        logger.info("!!!! start thread");
        try {
            while (!GoogleDriveSyncState.STOP.equals(syncState)) {
                setGoogleDriveSyncState(syncState.process(credentials));
                if (Thread.currentThread().isInterrupted()) {
                    syncState = GoogleDriveSyncState.STOP;
                }
            }
        } finally {
            logger.info("!!!!! end of thread");
        }
    }


    private Map<String,Object> credentials;
    private GoogleDriveSyncState syncState ;


    private final static Logger logger = Logger.getLogger(GoogleDriveSync.class);
    private static GoogleDriveSync _instance = null;



}
