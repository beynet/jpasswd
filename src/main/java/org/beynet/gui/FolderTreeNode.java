package org.beynet.gui;

import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.beynet.model.password.Password;

import java.util.function.Consumer;

/**
 * Created by beynet on 09/11/14.
 */
public class FolderTreeNode implements PasswordOrFolderTreeNode {
    public FolderTreeNode(String folderName) {
        this.folderName = folderName;
        imageView = new ImageView(folderOpen);
        imageView.setFitWidth(24);
        imageView.setFitHeight(24);
    }

    public ImageView getImageView() {
        return imageView;
    }

    @Override
    public void setExpanded(Boolean newValue) {
        if (newValue==true) {
            imageView.setImage(folderOpen);
        }
        else {
            imageView.setImage(folder);
        }
    }

    @Override
    public String getText() {
        return folderName;
    }

    @Override
    public boolean match(Object o) {
        return folderName.equals(o);
    }

    @Override
    public void remove(TreeItem<PasswordOrFolderTreeNode> parent, TreeItem<PasswordOrFolderTreeNode> itemSelected) {

    }

    @Override
    public void onDoubleClick(Stage parent) {

    }

    @Override
    public void display(Consumer<Password> selectedPasswordChange) {

    }

    private String folderName;
    private ImageView imageView;
    private static final Image folder = new Image(PasswordOrFolderTreeNode.class.getResourceAsStream("/Folder.png"));
    private static final Image folderOpen = new Image(PasswordOrFolderTreeNode.class.getResourceAsStream("/Folder_Open.png"));
}
