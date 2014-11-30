package org.beynet.gui;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.beynet.controller.Controller;
import org.beynet.model.password.*;
import org.beynet.model.store.PasswordModifiedOrCreated;
import org.beynet.model.store.PasswordRemoved;
import org.beynet.model.store.PasswordStoreEvent;
import org.beynet.model.store.PasswordStoreEventVisitor;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

/**
 * Created by beynet on 09/11/14.
 */
public class PasswordTree extends TreeView<PasswordOrFolderTreeNode> implements Observer,PasswordStoreEventVisitor {
    private final Consumer<Password> selectedPasswordChange;

    public PasswordTree(Stage parent,Consumer<Password> selectedPasswordChange,Pane passwordContentPane) {
        this.selectedPasswordChange = selectedPasswordChange;
        setCellFactory(fileCopiedTreeView -> new PasswordTreeCell());

        TreeItem<PasswordOrFolderTreeNode> rootTreeItem = new TreeItem<>(new FolderTreeNode("/"));
        rootTreeItem.setExpanded(true);
        webPasswords=new TreeItem<>(new FolderTreeNode("Web passwords"));
        notes=new TreeItem<>(new FolderTreeNode("notes"));
        notes.setExpanded(true);
        webPasswords.setExpanded(true);
        rootTreeItem.getChildren().add(webPasswords);
        rootTreeItem.getChildren().add(notes);

        setRoot(rootTreeItem);
        setShowRoot(true);

        Controller.suscribeToPassword(this);

        setOnKeyPressed(e->{
            if (KeyCode.DELETE.equals(e.getCode())) {
                int selectedIndex = getSelectionModel().getSelectedIndex();
                if (selectedIndex>=0) {
                    TreeItem<PasswordOrFolderTreeNode> itemSelected = getSelectionModel().getSelectedItem();
                    itemSelected.getValue().remove(itemSelected.getParent(),itemSelected);
                }
            }
        });

        setOnMouseClicked(mouseEvent->{
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) if (mouseEvent.getClickCount() == 2) {
                    int selectedIndex = getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0) {
                        TreeItem<PasswordOrFolderTreeNode> itemSelected = getSelectionModel().getSelectedItem();
                        itemSelected.getValue().onDoubleClick(parent);

                    }
                }
            }
        );

        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            passwordContentPane.getChildren().clear();
            if (newValue!=null && newValue.getValue()!=null) newValue.getValue().display(this.selectedPasswordChange);
        });

        displayer = new PasswordVisitor() {
            @Override
            public void visit(WebLoginAndPassword t) {
                TreeItem<PasswordOrFolderTreeNode> toRemove = null;
                for (TreeItem<PasswordOrFolderTreeNode> child : webPasswords.getChildren()) {
                    if (child.getValue().match(t)) {
                        toRemove = child;
                        break;
                    }
                }
                if (toRemove != null) webPasswords.getChildren().remove(toRemove);
                if (filter!=null) {
                    final Map<String,Password> matching = Controller.getMatching(filter);
                    if (matching.containsKey(t.getId())) webPasswords.getChildren().add(new TreeItem<>(new PasswordTreeNode(t)));
                }
                else {
                    webPasswords.getChildren().add(new TreeItem<>(new PasswordTreeNode(t)));
                }

            }

            @Override
            public void visit(Note note) {
                TreeItem<PasswordOrFolderTreeNode> toRemove = null;
                for (TreeItem<PasswordOrFolderTreeNode> child : notes.getChildren()) {
                    if (child.getValue().match(note)) {
                        toRemove = child;
                        break;
                    }
                }
                if (toRemove != null) notes.getChildren().remove(toRemove);
                if (filter!=null) {
                    final Map<String,Password> matching = Controller.getMatching(filter);
                    if (matching.containsKey(note.getId())) notes.getChildren().add(new TreeItem<>(new PasswordTreeNode(note)));
                }
                else {
                    notes.getChildren().add(new TreeItem<>(new PasswordTreeNode(note)));
                }
            }

            @Override
            public void visit(GoogleDrive t) {

            }

            @Override
            public void visit(PasswordString s) {

            }

            @Override
            public void visit(DeletedPassword s) {

            }
        };

    }

    @Override
    public void update(Observable o, Object arg) {
        ((PasswordStoreEvent)arg).accept(this);
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
            public void visit(DeletedPassword t) {
                {
                    ObservableList<TreeItem<PasswordOrFolderTreeNode>> children = webPasswords.getChildren();
                    for (TreeItem<PasswordOrFolderTreeNode> child : children) {
                        if (child.getValue().match(t)) {
                            Platform.runLater(() -> {
                                children.remove(child);
                            });
                            break;
                        }
                    }
                }

                {
                    ObservableList<TreeItem<PasswordOrFolderTreeNode>> children = notes.getChildren();
                    for (TreeItem<PasswordOrFolderTreeNode> child : children) {
                        if (child.getValue().match(t)) {
                            Platform.runLater(() -> {
                                children.remove(child);
                            });
                            break;
                        }
                    }
                }
            }

        });
    }

    @Override
    public void visit(PasswordModifiedOrCreated p) {
        Password password = p.getPassword();
        Platform.runLater(() -> password.accept(displayer));
    }

    public void updateFilter(String text) {
        this.filter = text;
        Platform.runLater(()->{
            final Map<String,Password> matching ;
            webPasswords.getChildren().clear();
            notes.getChildren().clear();
            if (text!=null && !"".equals(text)) {
                matching=Controller.getMatching(text);
            }
            else {
                matching=Controller.getMatching(null);
            }
            for (Password el:matching.values()) {
                el.accept(displayer);
            }
        });
    }

    private TreeItem<PasswordOrFolderTreeNode> webPasswords;
    private TreeItem<PasswordOrFolderTreeNode> notes;
    private String filter;
    private PasswordVisitor displayer;
}
