package org.beynet.gui;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import org.beynet.model.password.PasswordString;
import org.beynet.model.password.PasswordVisitor;
import org.beynet.model.password.WebLoginAndPassword;

/**
 * Created by beynet on 13/10/2014.
 */
public class PasswordDisplayer implements PasswordVisitor {
    PasswordDisplayer(GridPane p) {
        pane = p;
    }

    @Override
    public void visit(WebLoginAndPassword t) {
        Label label = new Label("Web site="+t.getUri().getHost()!=null?t.getUri().getHost():t.getUri().toString());
        pane.add(label,0,0);

        Label loginLabel = new Label("Login");
        TextField login = new TextField(t.getLogin());
        login.setEditable(false);
        pane.add(loginLabel,0,1);
        pane.add(login,1,1);

        Label passwordLabel = new Label("Password");
        TextField password = new TextField(t.getPassword().getHiddenPassword());
        ImageView imageView = new ImageView(eye);
        imageView.setFitWidth(24);
        imageView.setFitHeight(24);
        imageView.setOnMouseClicked(event -> {
            if (t.getPassword().getHiddenPassword().equals(password.getText())) {
                password.setText(t.getPassword().getPassword());
                imageView.setImage(eyeHidden);
            }
            else {
                password.setText(t.getPassword().getHiddenPassword());
                imageView.setImage(eye);
            }
        });


        password.setEditable(false);
        pane.add(passwordLabel,0,2);
        pane.add(password,1,2);
        pane.add(imageView,2,2);

    }

    @Override
    public void visit(PasswordString s) {

    }

    GridPane pane;

    private static final Image eye = new Image(PasswordDisplayer.class.getResourceAsStream("/eye.png"));
    private static final Image eyeHidden = new Image(PasswordDisplayer.class.getResourceAsStream("/eye-hidden.png"));
}
