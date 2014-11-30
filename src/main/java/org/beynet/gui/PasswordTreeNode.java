package org.beynet.gui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.model.password.*;

import java.util.function.Consumer;

/**
 * Created by beynet on 09/11/14.
 */
public class PasswordTreeNode implements PasswordOrFolderTreeNode {

    public PasswordTreeNode(Password password) {
        if (password==null) throw new IllegalArgumentException("password must not be null");
        this.password = password;
    }

    @Override
    public Node getImageView() {
        return null;
    }

    public String getText() {
        return password.getSummary();
    }

    @Override
    public boolean match(Object o) {
        if (o==null || !(o instanceof Password)) return false;
        return password.getId().equals(((Password) o).getId());
    }

    @Override
    public void remove(TreeItem<PasswordOrFolderTreeNode> parent, TreeItem<PasswordOrFolderTreeNode> itemSelected) {
        Controller.notifyPasswordRemoved(password.getId());
    }

    @Override
    public void onDoubleClick(Stage parent) {
        password.accept(new org.beynet.model.password.PasswordVisitor() {
            @Override
            public void visit(WebLoginAndPassword t) {
                new CreateOrModifyWebSitePassword(parent, t).show();
            }

            @Override
            public void visit(Note note) {

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

    @Override
    public void display(Consumer<Password> selectedPasswordChange) {
        selectedPasswordChange.accept(password);
    }

    private Password password;
}
