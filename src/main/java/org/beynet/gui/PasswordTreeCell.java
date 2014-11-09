package org.beynet.gui;

import javafx.scene.control.TreeCell;

/**
 * Created by beynet on 09/11/14.
 */
public class PasswordTreeCell extends TreeCell<PasswordOrFolderTreeNode> {
    @Override
    protected void updateItem(PasswordOrFolderTreeNode item, boolean empty) {
        super.updateItem(item, empty);
        if (empty==true||item==null) {
            setGraphic(null);
            setText(null);
        }
        else {
            if (item!=null) {
                setGraphic(item.getImageView());
                setText(item.getText());
            }
        }
    }
}
