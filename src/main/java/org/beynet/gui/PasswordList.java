package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.controller.Controller;
import org.beynet.model.Observable;
import org.beynet.model.Observer;
import org.beynet.model.password.*;
import org.beynet.model.store.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by beynet on 12/10/2014.
 */
public class PasswordList extends ListView<Password> implements Observer,PasswordStoreEventVisitor {


    public PasswordList(Stage parent,Consumer<Password> selectedPasswordChange) {
        this.selectedPasswordChange = selectedPasswordChange;
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
    public void visit(PasswordDefinitivelyRemoved r) {

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

                //final Password previousVersion = elements.get(p.getId());
                Platform.runLater(() -> {

                    Optional<Password> found = getItems().stream().filter(i -> p.getId().equals(i.getId())).findFirst();
                    found.ifPresent(previousVersion->getItems().remove(previousVersion));

                    if (thisIsADeletedPassword) return;

                    if (filter!=null) {
                        final Map<String,Password> matching = Controller.getMatching(filter);
                        if (matching.containsKey(p.getId())) getItems().add(p);
                    }
                    else {
                        getItems().add(p);
                    }
                });

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
                    getItems().stream().filter(found->id.equals(found.getId())).findFirst().ifPresent(found->getItems().remove(found));
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
                matching.values().stream().forEach(el->getItems().add(el));
            }
            else {
                matching=Controller.getMatching(null);
                getItems().addAll(matching.values());
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

    private final Consumer<Password> selectedPasswordChange;

    private String filter;

    private final static Logger logger = Logger.getLogger(PasswordList.class);

}
