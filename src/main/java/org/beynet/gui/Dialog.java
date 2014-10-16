package org.beynet.gui;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created with IntelliJ IDEA.
 * User: beynet
 * Date: 12/10/13
 * Time: 15:49
 * To change this template use File | Settings | File Templates.
 */
public abstract class Dialog  extends Stage {
    public Dialog(Stage parent,Double with,Double height) {
        this.parent = parent;
        root = new Group();
        root.getStyleClass().add(Styles.CHILD_WINDOW);

        if (with!=null && height!=null) {
            scene = new Scene(root, with, height);
        }else {
            scene = new Scene(root);
        }
        scene.getStylesheets().add(getClass().getResource("/default.css").toExternalForm());

        setScene(scene);

        setX(parent.getX() + parent.getWidth() /2);
        setY(parent.getY() + parent.getHeight()/2);
        initOwner(parent);

    }



    protected final Scene getCurrentScene() {
        return scene;
    }

    protected final Group getRootGroup() {
        return root;
    }

    protected final Stage getParentStage() {
        return parent;
    }
    private Group root;
    private Stage parent ;
    private Scene scene ;
}
