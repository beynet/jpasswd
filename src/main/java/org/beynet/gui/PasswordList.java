package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import org.beynet.model.Config;
import org.beynet.model.password.Password;
import org.beynet.model.store.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

/**
 * Created by beynet on 12/10/2014.
 */
public class PasswordList extends ListView<Password> implements Observer,PasswordStoreEventVisitor {


    public PasswordList(Consumer<Password> selectedPasswordChange) {
        this.selectedPasswordChange = selectedPasswordChange;
        elements = Config.getInstance().getPasswordStore().getCopie();
        getItems().addAll(elements.values());
        setCellFactory(param -> new PasswordCell());
        Config.getInstance().getPasswordStore().addObserver(this);
        setOnKeyPressed(e->{
            if (KeyCode.DELETE.equals(e.getCode())) {
                int selectedIndex = getSelectionModel().getSelectedIndex();
                if (selectedIndex>=0) {
                    Password p = getItems().get(selectedIndex);
                    Config.getInstance().getPasswordStore().removePassword(p.getId());
                }
            }
        });
        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedPasswordChange.accept(newValue);
        });
    }

    @Override
    public void visit(PasswordModifiedOrCreated modif) {
        Password p = modif.getPassword();
        synchronized (elements) {
            final Password previousVersion = elements.get(p.getId());

            Platform.runLater(() -> {
                if (previousVersion !=null) {
                    elements.remove(previousVersion);
                    getItems().remove(previousVersion);
                }
                elements.put(p.getId(),p);
                getItems().add(p);
            });
        }
    }

    @Override
    public void visit(PasswordRemoved removed) {
        Password p = removed.getPassword();
        String id = p.getId();
        Platform.runLater(() -> {
            Password toRemove=elements.remove(id);
            getItems().remove(toRemove);
        });
    }

    @Override
    public void update(Observable o, Object arg) {
        ((PasswordStoreEvent)arg).accept(this);
    }

    Map<String,Password> elements;
    private final Consumer<Password> selectedPasswordChange;
}
