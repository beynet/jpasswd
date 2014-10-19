package org.beynet.gui;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.beynet.controller.Controller;

/**
 * Created by beynet on 19/10/2014.
 */
public class ChangeMainPassword extends DialogModal {
    public ChangeMainPassword(Stage parent) {
        super(parent,null,null);

        HBox pane = new HBox();

        final TextField password = new TextField();
        final Label passwordLabel = new Label("password :");
        final Button ok = new Button("ok");

        ok.setOnAction(event -> {
            if (password.getText()!=null && !password.getText().isEmpty()) {
                Controller.changeMainPassword(password.getText());
                close();
            }
        });
        pane.getChildren().addAll(passwordLabel,password,ok);
        getRootGroup().getChildren().add(pane);
    }
}
