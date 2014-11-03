package org.beynet.sync.googledrive;

import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by beynet on 25/10/14.
 */
public class GoogleDriveSync implements Runnable {

    public GoogleDriveSync(Stage applicationMainStage) {
        this.credentials = new HashMap<>();
        this.credentials.put(GoogleDriveSyncState.APPLICATION_MAIN_STAGE,applicationMainStage);
    }

    @Override
    public void run() {
        GoogleDriveSyncState syncState = GoogleDriveSyncState.START;
        while (!GoogleDriveSyncState.STOP.equals(syncState)) {
            syncState=syncState.process(credentials);
            if (Thread.currentThread().isInterrupted()) break;
        }
    }


    private Map<String,Object> credentials;



    private final static Logger logger = Logger.getLogger(GoogleDriveSync.class);



}
