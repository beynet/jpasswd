package org.beynet.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.model.password.WebLoginAndPassword;
import org.beynet.utils.I18NHelper;

import java.net.URI;
import java.util.ResourceBundle;

/**
 * Created by beynet on 16/10/2014.
 */
public class CreateOrModifyWebSitePassword extends Dialog {
    public CreateOrModifyWebSitePassword(Stage parent) {
        this(parent,null);
    }
    public CreateOrModifyWebSitePassword(Stage parent,WebLoginAndPassword password) {
        super(parent, null, null);
        final ResourceBundle labelResourceBundle = I18NHelper.getLabelResourceBundle();

        this.password = password;

        Text loginL    = new Text(labelResourceBundle.getString("login"));
        Text passwordL = new Text(labelResourceBundle.getString("password"));
        Text uriL      = new Text(labelResourceBundle.getString("uri"));
        Text confirmL   = new Text(labelResourceBundle.getString("save"));


        final String loginMessage =labelResourceBundle.getString("login");
        final String passwordMessage =labelResourceBundle.getString("password");
        final String uriMessage=labelResourceBundle.getString("uri");
        TextField login=new TextFieldWithShortcuts();

        login.setMinWidth(loginL.getLayoutBounds().getWidth()*5);
        login.setPromptText(loginMessage);


        TextField passwordT = new TextFieldWithShortcuts(hiddenPassword);
        passwordT.setMinWidth(loginL.getLayoutBounds().getWidth()*5);
        passwordT.setPromptText(passwordMessage);



        TextField uri = new TextFieldWithShortcuts();
        uri.setMinWidth(loginL.getLayoutBounds().getWidth()*5);
        uri.setPromptText(uriMessage);

        if (this.password!=null) {
            uri.setText(this.password.getUri().toString());
            currentPasswordValue=this.password.getPassword().getPassword();
            login.setText(this.password.getLogin());
        }

        passwordT.focusedProperty().addListener((obs,prev,curr)->{
            if (Boolean.TRUE.equals(prev)) {
                currentPasswordValue=passwordT.getText();
            }
            if (Boolean.TRUE.equals(curr)) {
                passwordT.setText(currentPasswordValue);
            }
            else {
                passwordT.setText(hiddenPassword);
            }
        });

        Button confirm = new Button(labelResourceBundle.getString("save"));
        confirm.setMinWidth(confirmL.getLayoutBounds().getWidth()+20);
        confirm.setOnAction(p->{
            URI uriCreated ;
            try {
                uriCreated = new URI(uri.getText());
            }catch(Exception e) {
                String message = labelResourceBundle.getString("invaliduri");
                new Alert(this,message.replaceAll("VAL",(uri.getText()!=null?uri.getText():""))).show();
                return;
            }

            if (currentPasswordValue==null||currentPasswordValue.isEmpty()) {
                new Alert(this,labelResourceBundle.getString("emptypassword")).show();
                return;
            }
            if (login.getText()==null||login.getText().isEmpty()) {
                new Alert(this,labelResourceBundle.getString("emptyuri")).show();
                return;
            }

            WebLoginAndPassword newP = new WebLoginAndPassword(uriCreated,login.getText(),currentPasswordValue);
            Controller.notifyPasswordModified(this.password,newP);
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



    private WebLoginAndPassword password;
    private String hiddenPassword = "***";
    private String currentPasswordValue = null;
}
