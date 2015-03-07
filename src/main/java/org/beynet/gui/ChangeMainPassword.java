package org.beynet.gui;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.utils.I18NHelper;

import java.util.ResourceBundle;

/**
 * Created by beynet on 19/10/2014.
 */
public class ChangeMainPassword extends DialogModal {
    public ChangeMainPassword(Stage parent) {
        super(parent,null,null);

        final ResourceBundle labelResourceBundle = I18NHelper.getLabelResourceBundle();

        GridPane pane = new GridPane();

        final PasswordField password = new PasswordField();
        password.setPromptText(labelResourceBundle.getString("fillnewpassword"));
        final PasswordField passwordConfirm = new PasswordField();
        passwordConfirm.setPromptText(labelResourceBundle.getString("confirmnewpassword"));
        final Button ok = new Button(labelResourceBundle.getString("changepassword"));

        ok.setOnAction(event -> {
            if (password.getText()!=null && !password.getText().isEmpty()) {
                if (password.getText().equals(passwordConfirm.getText())) {
                    Controller.changeMainPassword(password.getText());
                    close();
                }
                else {
                    new Alert(this,labelResourceBundle.getString("passwordmismatch")).show();
                }
            }
        });
        pane.add(password,0,0,2,1);
        pane.add(passwordConfirm,0,1,2,1);
        pane.add(ok,1,2);
        getRootGroup().getChildren().add(pane);
    }
}
