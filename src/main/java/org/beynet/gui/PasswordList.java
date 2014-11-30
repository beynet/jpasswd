package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.controller.Controller;
import org.beynet.model.password.*;
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
                            public void visit(Note note) {
                                new CreateOrModifyNote(parent,note).show();
                            }

                            @Override
                            public void visit(GoogleDrive t) {

                            }

                            @Override
                            public void visit(DeletedPassword t) {

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
        p.accept(new PasswordVisitor() {
            @Override
            public void visit(Note note) {

            }
            @Override
            public void visit(WebLoginAndPassword t) {
                updateList(false);
            }

            @Override
            public void visit(GoogleDrive t) {
                return;
            }

            @Override
            public void visit(PasswordString s) {
                return;
            }

            @Override
            public void visit(DeletedPassword s) {
                updateList(true);
            }

            private void updateList(boolean thisIsADeletedPassword) {
                synchronized (elements) {
                    final Password previousVersion = elements.get(p.getId());
                    Platform.runLater(() -> {
                        if (previousVersion !=null) {
                            elements.remove(previousVersion);
                            getItems().remove(previousVersion);
                        }
                        if (thisIsADeletedPassword) return;
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
        });
    }

    @Override
    public void visit(PasswordRemoved removed) {
        Password p = removed.getPassword();
        p.accept(new PasswordVisitor() {
            @Override
            public void visit(Note note) {

            }
            @Override
            public void visit(WebLoginAndPassword t) {
                removePassword(t);
            }

            @Override
            public void visit(GoogleDrive t) {
                return;
            }

            @Override
            public void visit(PasswordString s) {
                return;
            }

            @Override
            public void visit(DeletedPassword s) {
                removePassword(s);
            }
            private void removePassword(Password p) {
                String id = p.getId();
                Platform.runLater(() -> {
                    Password toRemove=elements.remove(id);
                    if (toRemove!=null) getItems().remove(toRemove);
                });
            }
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

    private final static Logger logger = Logger.getLogger(PasswordList.class);
}
