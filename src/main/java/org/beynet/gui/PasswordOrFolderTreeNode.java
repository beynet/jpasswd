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
    public Node getImageView() ;

    public String getText();

    public boolean match(Object o);

    public void remove(TreeItem<PasswordOrFolderTreeNode> parent, TreeItem<PasswordOrFolderTreeNode> itemSelected);

    public void onDoubleClick(Stage parent);

    void display(Consumer<Password> selectedPasswordChange);
}
