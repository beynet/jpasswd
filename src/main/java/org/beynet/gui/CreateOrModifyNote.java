package org.beynet.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.beynet.controller.Controller;
import org.beynet.model.password.Note;
import org.beynet.model.password.WebLoginAndPassword;

import java.net.URI;

/**
 * Created by beynet on 16/10/2014.
 */
public class CreateOrModifyNote extends Dialog {
    public CreateOrModifyNote(Stage parent) {
        this(parent,null);
    }
    public CreateOrModifyNote(Stage parent, Note note) {
        super(parent, null, null);
        this.note = note;

        Text titleL    = new Text("note title");
        Text contentL = new Text("note contente");


        final String titlePrompt ="note title";
        final String contentPrompt ="note contente";

        TextField title=new TextField();
        title.setMinWidth(titleL.getLayoutBounds().getWidth()*5);
        title.setPromptText(titlePrompt);

        TextArea contente = new TextArea();




        if (this.note!=null) {
            title.setText(this.note.getTitle());
            contente.setText(this.note.getContent());
        }

        Button confirm = new Button("save");
        confirm.setOnAction(p->{

            if (title.getText()==null||title.getText().isEmpty()) {
                new Alert(this,"title MUST not be empty").show();
                return;
            }

            if (contente.getText()==null||contente.getText().isEmpty()) {
                new Alert(this,"contente MUST not be empty").show();
                return;
            }

            Note newNote = new Note(title.getText(),contente.getText());
            Controller.notifyPasswordModified(this.note,newNote);
            close();
        });


        GridPane grid = new GridPane();
        grid.prefWidthProperty().bind(getCurrentScene().widthProperty());
        grid.setPadding(new Insets(5));
        grid.setHgap(5);
        grid.setVgap(5);

        grid.add(titleL,0,0);
        grid.add(title,1,0);
        grid.add(contentL,0,1);
        grid.add(contente,0,2,4,10);

        grid.add(confirm,0,12);

        getRootGroup().getChildren().add(grid);
    }



    Note note;
}
