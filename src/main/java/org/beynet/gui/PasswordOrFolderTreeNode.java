package org.beynet.gui;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.beynet.model.password.Password;

import java.util.function.Consumer;

/**
 * Created by beynet on 09/11/14.
 */
public interface PasswordOrFolderTreeNode {
    Node getImageView() ;

    String getText();

    boolean match(Object o);

    void remove(TreeItem<PasswordOrFolderTreeNode> parent, TreeItem<PasswordOrFolderTreeNode> itemSelected);

    void onDoubleClick(Stage parent);

    void display(Consumer<Password> selectedPasswordChange);

    void setExpanded(Boolean newValue);
}
