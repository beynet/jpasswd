package org.beynet.gui;

import javafx.scene.control.ListCell;
import org.beynet.model.password.Password;

/**
 * Created by beynet on 12/10/2014.
 */
public class PasswordCell extends ListCell<Password> {
    @Override
    protected void updateItem(Password item, boolean empty) {
        super.updateItem(item, empty);
        if (true==empty) {
            setText(null);
        }
        else {
            setText(item.getSummary());
        }
    }
}
