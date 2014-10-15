package org.beynet.gui;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.beynet.model.Config;
import org.beynet.model.password.WebLoginAndPassword;
import org.beynet.model.store.PasswordStore;

import java.net.URI;

public class Main extends Application{
    public static void main(String...args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        currentStage = primaryStage;
        currentStage.setOnCloseRequest((t)->quitApp());
        setTitle();

        Group group  = new Group();

        BorderPane pane = new BorderPane();


        group.getChildren().add(pane);

        addMenuBar(pane);
        addPasswordList(pane);
        addPasswordContent(pane);



        currentScene = new Scene(group, 640, 480);
        currentScene.getStylesheets().add(getClass().getResource("/default.css").toExternalForm());
        currentStage.setScene(currentScene);
        currentStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            passwordList.setPrefWidth(newValue.doubleValue() * 0.33);
        });
        currentStage.show();
    }



    private void addPasswordList(BorderPane pane) {

        // create list with a call back to be used
        // to refresh passwordContentPane when selected passw has changed
        // --------------------------------------------------------------

        passwordList = new PasswordList(newPasswd->{
            passwordContentPane.getChildren().clear();
            if (newPasswd!=null) {
                newPasswd.accept(new PasswordVisitor(passwordContentPane));
            }
        });
        passwordList.getStyleClass().add("passwdlist");
        passwordList.setPrefWidth(currentStage.getWidth() * 0.33);
        pane.setLeft(passwordList);
    }

    private void addPasswordContent(BorderPane pane) {
        passwordContentPane = new GridPane();
        passwordContentPane.getStyleClass().add("content");
        pane.setCenter(passwordContentPane);
    }

    private void addMenuBar(BorderPane pane) {
        final MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(currentStage.widthProperty());
        pane.setTop(menuBar);

        Menu files = new Menu("files");
        menuBar.getMenus().add(files);

        // files menu
        MenuItem exit = new MenuItem("exit");
        exit.setOnAction(t -> quitAppFromMenu());

        MenuItem test = new MenuItem("test");
        test.setOnAction(t -> createTest());

        files.getItems().addAll(test,exit);
    }

    private void setTitle() {
        currentStage.setTitle("jpasswd");
    }

    private void quitApp() {
        System.out.println("exit with button");
        quitApplication();
    }
    private void quitAppFromMenu() {
        System.out.println("exit with menu");
        quitApplication();
    }

    private void createTest() {
        WebLoginAndPassword web = new WebLoginAndPassword(URI.create("http://fake-uri.fake"),"login","password");
        Config.getInstance().getPasswordStore().savePassword(web);
    }

    public static void quitApplication() {
        System.exit(0);
    }

    private Stage currentStage;
    private Scene currentScene;
    private PasswordList passwordList;
    private GridPane passwordContentPane;

}