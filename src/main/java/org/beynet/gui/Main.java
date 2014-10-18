package org.beynet.gui;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.beynet.controller.Controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main extends Application {

    private static Path savePath;
    public static void main(String...args) {
        if (args.length==0) {
            Path userHome = Paths.get((String) System.getProperty("user.home"));
            savePath = userHome.resolve(".jpasswd");
        }
        else {
            Path userHome = Paths.get(args[0]);
            if (!Files.exists(userHome)) throw new RuntimeException("path "+userHome+" does not exist");
            savePath = userHome.resolve(".jpasswd");
        }
        launch(args);
    }


    private void createAskPassword(String ...args) {
        Group group  = new Group();

        HBox pane = new HBox();
        group.getChildren().add(pane);

        final TextField password = new TextField();
        final Label passwordLabel = new Label("password :");
        final Button ok = new Button("ok");

        ok.setOnAction(event -> {
            if (password.getText()!=null && !password.getText().isEmpty()) {
                Controller.initConfig(password.getText(), savePath);
                createMainScene();
            }
        });
        pane.getChildren().addAll(passwordLabel,password,ok);

        currentScene = new Scene(group);
        currentScene.getStylesheets().add(getClass().getResource("/default.css").toExternalForm());
        currentStage.setScene(currentScene);
    }

    private void createMainScene() {
        Group group  = new Group();

        BorderPane pane = new BorderPane();


        group.getChildren().add(pane);

        addMenuBar(pane);
        addPasswordList(pane);
        addPasswordContent(pane);



        currentScene = new Scene(group, 640, 480);
        currentScene.getStylesheets().add(getClass().getResource("/default.css").toExternalForm());
        currentStage.setScene(currentScene);
        passwordList.setPrefWidth(currentStage.getWidth()*0.33);
        currentStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            passwordList.setPrefWidth(newValue.doubleValue() * 0.33);
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        currentStage = primaryStage;
        currentStage.setOnCloseRequest((t)->quitApp());
        setTitle();

        createAskPassword();
        currentStage.show();
    }



    private void addPasswordList(BorderPane pane) {

        // create list with a call back to be used
        // to refresh passwordContentPane when selected passw has changed
        // --------------------------------------------------------------

        passwordList = new PasswordList(this.currentStage,newPasswd->{
            passwordContentPane.getChildren().clear();
            if (newPasswd!=null) {
                newPasswd.accept(new PasswordDisplayer(passwordContentPane));
            }
        });
        passwordList.getStyleClass().add(Styles.PASSWD_LIST);
        passwordList.setPrefWidth(currentStage.getWidth() * 0.33);
        pane.setLeft(passwordList);
    }

    private void addPasswordContent(BorderPane pane) {
        passwordContentPane = new GridPane();
        passwordContentPane.getStyleClass().add(Styles.PASSWORD_CONTENT);
        pane.setCenter(passwordContentPane);
    }

    private void addMenuBar(BorderPane pane) {
        final MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(currentStage.widthProperty());
        pane.setTop(menuBar);

        // Main menu
        // **********
        {
            Menu mainMenu = new Menu("JPasswd");
            menuBar.getMenus().add(mainMenu);

            // files menu
            MenuItem exit = new MenuItem("Exit");
            exit.setOnAction(t -> quitAppFromMenu());

            MenuItem newWebSite = new MenuItem("Add Web Site");
            newWebSite.setOnAction(t -> new CreateOrModifyWebSitePassword(currentStage).show());

            MenuItem test = new MenuItem("test");
            test.setOnAction(t -> new Alert(currentStage, "attention").show());
            mainMenu.getItems().addAll(test, newWebSite, exit);
        }

        // Password Menu
        // **************
        {
            Menu passwords = new Menu("Tools");
            menuBar.getMenus().add(passwords);
            MenuItem generatePassword = new MenuItem("Generate Password");
            generatePassword.setOnAction(t -> {
                new GeneratePassword(currentStage).show();
            });
            passwords.getItems().add(generatePassword);
        }


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

    public static void quitApplication() {
        System.exit(0);
    }

    private Stage currentStage;
    private Scene currentScene;
    private PasswordList passwordList;
    private GridPane passwordContentPane;

}