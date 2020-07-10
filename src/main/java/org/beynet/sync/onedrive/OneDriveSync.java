package org.beynet.sync.onedrive;

import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.model.Observable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by beynet on 25/10/14.
 */
public class OneDriveSync extends Observable implements Runnable {

    public static void init(Stage applicationMainStage) {
        synchronized (OneDriveSync.class) {
            if (_instance==null) {
                _instance = new OneDriveSync(applicationMainStage);
            }
        }
    }

    public static OneDriveSync getInstance() {
        return _instance;
    }

    private OneDriveSync(Stage applicationMainStage) {
        this.credentials = new HashMap<>();
        this.credentials.put(OneDriveSyncState.APPLICATION_MAIN_STAGE,applicationMainStage);
        this.syncState = OneDriveSyncState.STOP;
    }

    /**
     * @return the state of the synchronisation with google drive account
     */
    public OneDriveSyncState getOneDriveSyncState() {
        return syncState;
    }
    public void setOneDriveSyncState(OneDriveSyncState state) {
        this.syncState = state;
        setChanged();
        notifyObservers(state);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("OneDriveSync");
        this.syncState = OneDriveSyncState.START;
        logger.info("!!!! start thread");
        try {
            while (!OneDriveSyncState.STOP.equals(syncState)) {
                setOneDriveSyncState(syncState.process(credentials));
                if (Thread.currentThread().isInterrupted()) {
                    syncState = OneDriveSyncState.STOP;
                }
            }
        }
        catch(Exception e){
            logger.error("end of thread with exception",e);
        }
        finally {
            logger.info("!!!!! end of thread");
        }
    }


    private Map<String,Object> credentials;
    private OneDriveSyncState syncState ;


    private final static Logger logger = Logger.getLogger(OneDriveSync.class);
    private static OneDriveSync _instance = null;



}
