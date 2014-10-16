package org.beynet.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.model.password.WebLoginAndPassword;

import java.net.URI;

/**
 * Created by beynet on 16/10/2014.
 */
public class CreateOrModifyWebSitePassword extends Dialog {
    public CreateOrModifyWebSitePassword(Stage parent) {
        this(parent,null);
    }
    public CreateOrModifyWebSitePassword(Stage parent,WebLoginAndPassword password) {
        super(parent, null, null);
        this.password = password;

        Text loginL    = new Text("login");
        Text passwordL = new Text("password");
        Text uriL      = new Text("uri");


        final String loginMessage ="web site login";
        final String passwordMessage ="web site password";
        final String uriMessage="web site URI";
        TextField login=new TextField(loginMessage);

        login.setMinWidth(loginL.getLayoutBounds().getWidth()*5);
        login.setPromptText(loginMessage);

        TextField passwordT = new TextField();
        passwordT.setMinWidth(loginL.getLayoutBounds().getWidth()*5);
        passwordT.setPromptText(passwordMessage);


        TextField uri = new TextField();
        uri.setMinWidth(loginL.getLayoutBounds().getWidth()*5);
        uri.setPromptText(uriMessage);

        if (this.password!=null) {
            uri.setText(this.password.getUri().toString());
            passwordT.setText(this.password.getPassword().getPassword());
            login.setText(this.password.getLogin());
        }

        Button confirm = new Button("save");
        confirm.setOnAction(p->{
            URI uriCreated ;
            try {
                uriCreated = new URI(uri.getText());
            }catch(Exception e) {
                new Alert(this,"uri <"+(uri.getText()!=null?uri.getText():"")+"> invalid").show();
                return;
            }

            if (passwordT.getText()==null||passwordT.getText().isEmpty()) {
                new Alert(this,"passsword MUST not be empty").show();
                return;
            }
            if (login.getText()==null||login.getText().isEmpty()) {
                new Alert(this,"login MUST not be empty").show();
                return;
            }

            WebLoginAndPassword newP = new WebLoginAndPassword(uriCreated,login.getText(),passwordT.getText());
            if (this.password==null) {
                Controller.notifyPasswordModified(newP);
            }
            else {
                Controller.notifyPasswordModified(this.password.refresh(newP));
            }
            close();
        });


        GridPane grid = new GridPane();
        grid.prefWidthProperty().bind(getCurrentScene().widthProperty());
        grid.setPadding(new Insets(5));
        grid.setHgap(5);
        grid.setVgap(5);

        grid.add(loginL,0,0);
        grid.add(login,1,0);
        grid.add(passwordL,2,0);
        grid.add(passwordT,3,0);
        grid.add(uriL,0,1);
        grid.add(uri,1,1,3,1);

        grid.add(confirm,2,2);

        getRootGroup().getChildren().add(grid);
    }



    WebLoginAndPassword password;
}
