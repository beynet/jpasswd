package org.beynet.controller;

import javafx.application.Platform;
import org.beynet.model.Config;
import org.beynet.model.password.Password;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

/**
 * Created by beynet on 16/10/2014.
 */
public class Controller {
    public static void suscribeToPassword(Observer suscriber) {
        Platform.runLater(()->{
            Config.getInstance().getPasswordStore().addObserver(suscriber);
            Config.getInstance().getPasswordStore().sendAllPasswords(suscriber);
        });
    }

    public static void notifyPasswordRemoved(String id) {
        Platform.runLater(()->{
            Config.getInstance().getPasswordStore().removePassword(id);
        });
    }

    public static void notifyPasswordModified(Password p) {
        Platform.runLater(()->{
            Config.getInstance().getPasswordStore().savePassword(p);
        });
    }

    public static List<Password> getMatching(String filter) {
        try {
            return Config.getInstance().getPasswordStore().search(filter);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void initConfig(String text, Path savePath) {
        Config.initConfig(text, savePath);
    }
}
