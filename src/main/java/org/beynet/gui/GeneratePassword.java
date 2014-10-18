package org.beynet.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.beynet.controller.PasswordGenerator;


/**
 * Created by beynet on 18/10/2014.
 */
public class GeneratePassword extends DialogNotModal {
    public GeneratePassword(Stage parent) {
        super(parent,null,null);

        Label sizeLabel = new Label("Password Length");
        TextField size = new TextField("10");

        Label digitsLabel = new Label("Total digits");
        TextField digits = new TextField("0");

        Label symbolsLabel = new Label("Total symbols");
        TextField symbols = new TextField("0");

        Label passwordLabel = new Label("Password :");
        TextField password = new TextField(PasswordGenerator.generateNewPassword(10,0,0));
        password.setMinWidth(new Text(password.getText()).getLayoutBounds().getWidth()+20);

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        getRootGroup().getChildren().add(grid);

        Button generate = new Button("generate");
        generate.setOnAction(evt->{
                    Integer val;
                    Integer totaldigits;
                    Integer totalsymbols;
                    try {
                        val = Integer.valueOf(size.getText());
                    }catch(NumberFormatException e) {
                        new Alert(this,"Size must be an integer");
                        return;
                    }
                    try {
                        totaldigits = Integer.valueOf(digits.getText());
                    }catch(NumberFormatException e) {
                        new Alert(this,"numbers digits must be an integer");
                        return;
                    }
                    try {
                        totalsymbols = Integer.valueOf(symbols.getText());
                    }catch(NumberFormatException e) {
                        new Alert(this,"numbers symbols must be an integer");
                        return;
                    }
                    double prevMinWidth = password.getMinWidth();
                    password.setText(PasswordGenerator.generateNewPassword(val, totaldigits, totalsymbols));
                    password.setMinWidth(new Text(password.getText()).getLayoutBounds().getWidth()+20);
                    double inc = password.getMinWidth()-prevMinWidth;
                    if (inc>0) {
                        setWidth(getWidth() + inc);
                    }

                }
        );

        grid.add(sizeLabel,0,0);
        grid.add(size,1,0);

        grid.add(digitsLabel,2,0);
        grid.add(digits,3,0);

        grid.add(symbolsLabel,4,0);
        grid.add(symbols,5,0);

        grid.add(passwordLabel,0,1);
        grid.add(password,1,1,5,1);

        grid.add(generate,1,2);


    }
}
