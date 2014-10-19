package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.model.password.Password;
import org.beynet.model.password.PasswordString;
import org.beynet.model.password.WebLoginAndPassword;
import org.beynet.model.store.PasswordModifiedOrCreated;
import org.beynet.model.store.PasswordRemoved;
import org.beynet.model.store.PasswordStoreEvent;
import org.beynet.model.store.PasswordStoreEventVisitor;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by beynet on 12/10/2014.
 */
public class PasswordList extends ListView<Password> implements Observer,PasswordStoreEventVisitor {


    public PasswordList(Stage parent,Consumer<Password> selectedPasswordChange) {
        this.selectedPasswordChange = selectedPasswordChange;
        elements = new HashMap<>();
        setCellFactory(param -> new PasswordCell());
        Controller.suscribeToPassword(this);
        setOnKeyPressed(e->{
            if (KeyCode.DELETE.equals(e.getCode())) {
                int selectedIndex = getSelectionModel().getSelectedIndex();
                if (selectedIndex>=0) {
                    Password p = getItems().get(selectedIndex);
                    Controller.notifyPasswordRemoved(p.getId());
                }
            }
        });
        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedPasswordChange.accept(newValue);
        });
        setOnMouseClicked(mouseEvent->{
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) if (mouseEvent.getClickCount() == 2) {
                    int selectedIndex = getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0) {
                        Password p = getItems().get(selectedIndex);
                        p.accept(new org.beynet.model.password.PasswordVisitor() {
                            @Override
                            public void visit(WebLoginAndPassword t) {
                                new CreateOrModifyWebSitePassword(parent, t).show();
                            }

                            @Override
                            public void visit(PasswordString s) {

                            }
                        });

                    }
                }
            }
        );
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
                if (filter!=null) {
                    final Map<String,Password> matching = Controller.getMatching(filter);
                    if (matching.containsKey(p.getId())) getItems().add(p);
                }
                else {
                    getItems().add(p);
                }
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

    public void updateFilter(String text) {
        this.filter = text;
        Platform.runLater(()->{
            final Map<String,Password> matching ;
            getItems().clear();
            if (text!=null && !"".equals(text)) {
                matching=Controller.getMatching(text);
                for (Password el:elements.values()) {
                    if (matching.containsKey(el.getId())) getItems().add(el);
                }
            }
            else {
                getItems().addAll(elements.values());
            }

        });
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public void update(Observable o, Object arg) {
        ((PasswordStoreEvent)arg).accept(this);
    }

    Map<String,Password> elements;
    private final Consumer<Password> selectedPasswordChange;

    private String filter;
}
