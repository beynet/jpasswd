package org.beynet.gui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.beynet.model.password.*;
import org.beynet.utils.I18NHelper;

import java.util.ResourceBundle;

/**
 * Created by beynet on 13/10/2014.
 */
public class PasswordDisplayer implements PasswordVisitor {
    PasswordDisplayer(Stage stage,GridPane p) {
        this.stage = stage;
        pane = p;
    }

    @Override
    public void visit(GoogleDrive t) {

    }

    @Override
    public void visit(DeletedPassword t) {
        
    }

    @Override
    public void visit(Note t) {
        if (pane.getChildren()!=null) pane.getChildren().clear();
        Label labelTitle = new Label("title");
        TextField title = new TextField(t.getTitle());
        title.setEditable(false);

        Label labelContent = new Label("content");
        TextArea content = new TextArea(t.getContent());
        content.getStyleClass().addAll("content");
        content.prefWidthProperty().bind(pane.widthProperty());
        content.setEditable(false);


        pane.add(labelTitle,0,0);
        pane.add(title,1,0);
        pane.add(labelContent,0,1);
        pane.add(content,0,2,4,10);
    }

    @Override
    public void visit(WebLoginAndPassword t) {
        final ResourceBundle labelResourceBundle = I18NHelper.getLabelResourceBundle();
        if (pane.getChildren()!=null) pane.getChildren().clear();
        Label label = new Label(labelResourceBundle.getString("uri")+" = ");
        TextFieldWithShortcuts uri = new TextFieldWithShortcuts();
        uri.setText(t.getUri()!=null?t.getUri().toString():" ");
        uri.setEditable(false);

        pane.add(label,0,0,1,1);
        pane.add(uri,1,0,1,1);

        Label loginLabel = new Label(labelResourceBundle.getString("login"));
        TextFieldWithShortcuts login = new TextFieldWithShortcuts(t.getLogin());
        login.setEditable(false);
        pane.add(loginLabel,0,1);
        pane.add(login,1,1);

        Label passwordLabel = new Label(labelResourceBundle.getString("password"));
        TextFieldWithShortcuts password = new TextFieldWithShortcuts(t.getPassword().getHiddenPassword());
        ImageView imageView = new ImageView(eye);
        imageView.setFitWidth(23);
        imageView.setFitHeight(23);
        Button toggleDisplay = new Button();
        toggleDisplay.setGraphic(imageView);
        toggleDisplay.setOnAction(event -> {
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
        pane.add(toggleDisplay,2,2);

    }

    @Override
    public void visit(PasswordString s) {

    }

    GridPane pane;
    Stage stage;

    private static final Image eye = new Image(PasswordDisplayer.class.getResourceAsStream("/eye.png"));
    private static final Image eyeHidden = new Image(PasswordDisplayer.class.getResourceAsStream("/eye-hidden.png"));
}
