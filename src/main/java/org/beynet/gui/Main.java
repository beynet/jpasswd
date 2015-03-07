package org.beynet.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beynet.controller.Controller;
import org.beynet.model.MainPasswordError;
import org.beynet.model.password.*;
import org.beynet.model.store.*;
import org.beynet.sync.googledrive.GoogleDriveSync;
import org.beynet.utils.I18NHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class Main extends Application {

    private static Path savePath;
    private static String fileName;

    public static void main(String... args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
        if (args.length == 0) {
            Path userHome = Paths.get((String) System.getProperty("user.home"));
            savePath = userHome.resolve(".jpasswd");
            fileName = null;
        } else {
            Path userHome = Paths.get(args[0]);
            if (!Files.exists(userHome)) throw new RuntimeException("path " + userHome + " does not exist");
            savePath = userHome.resolve(".jpasswd");
            if (args.length == 2) {
                fileName = args[1];
            } else {
                fileName = null;
            }
        }
        launch(args);
    }


    private void createAskPassword(String... args) {
        final ResourceBundle labelResourceBundle = I18NHelper.getLabelResourceBundle();
        Group group = new Group();

        HBox pane = new HBox();
        group.getChildren().add(pane);

        final PasswordField password = new PasswordField();
        final Label passwordLabel = new Label(labelResourceBundle.getString("password"));
        final Button ok = new Button(labelResourceBundle.getString("ok"));


        // if password is OK print main scene
        // else display an error
        // ----------------------------------
        Runnable checkIt =  ()-> {
            try {
                Controller.initConfig(password.getText(), savePath, fileName);
                createMainScene();
            } catch (MainPasswordError mainPasswordError) {
                new Alert(currentStage, labelResourceBundle.getString("perror")).show();
            }
        };

        password.setOnKeyReleased(keyEvent -> {
            if (KeyCode.ENTER.equals(keyEvent.getCode())) {
                checkIt.run();
            }
        });

        ok.setOnAction((event) -> {
            if (password.getText() != null && !password.getText().isEmpty()) {
                checkIt.run();
            }
        });
        pane.getChildren().addAll(passwordLabel, password, ok);

        currentScene = new Scene(group);
        currentScene.getStylesheets().add(getClass().getResource("/default.css").toExternalForm());
        currentStage.setScene(currentScene);

    }

    private void createMainScene() {
        Group group = new Group();

        BorderPane pane = new BorderPane();


        group.getChildren().add(pane);

        currentScene = new Scene(group, 640, 480);
        currentScene.getStylesheets().add(getClass().getResource("/default.css").toExternalForm());

        addMenuBar(pane);
//        addPasswordList(pane);
        addPasswordContent(pane);
        addSyncStatus(pane);
        addPasswordTree(pane);

        currentStage.setScene(currentScene);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        currentStage = primaryStage;
        GoogleDriveSync.init(primaryStage);
        currentStage.setOnCloseRequest((t) -> quitApp());
        setTitle();

        createAskPassword();
        currentStage.show();
    }


    private void addPasswordTree(BorderPane pane) {
        VBox box = new VBox();
        TextField filter = new TextField();
        filter.setPromptText("filter list");

        // create list with a call back to be used
        // to refresh passwordContentPane when selected passw has changed
        // --------------------------------------------------------------

        passwordTree = new PasswordTree(currentStage, newPasswd -> {
            passwordContentPane.getChildren().clear();
            if (newPasswd != null) {
                newPasswd.accept(new PasswordDisplayer(passwordContentPane));
            }
        }, passwordContentPane);
        passwordTree.getStyleClass().add(Styles.PASSWD_TREE);

        filter.setOnKeyReleased((evt) -> {
            final String text = filter.getText();
            passwordTree.updateFilter(text);
        });


        passwordTree.setPrefWidth(currentScene.getWidth() * 0.33);
        currentStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            passwordTree.setPrefWidth(newValue.doubleValue() * 0.33);
        });


        passwordTree.setPrefHeight(currentScene.getHeight() - ((MenuBar) pane.getTop()).getHeight() - state.getHeight() - filter.getHeight());
        currentScene.heightProperty().addListener((observable, oldValue, newValue) -> {
            double size = ((MenuBar) pane.getTop()).getHeight();
            passwordTree.setPrefHeight(newValue.doubleValue() -size-state.getPrefHeight()-filter.getHeight());
        });


        box.getChildren().addAll(filter, passwordTree);
        pane.setLeft(box);
    }

    private void addPasswordList(BorderPane pane) {

        VBox box = new VBox();
        TextField filter = new TextField();
        filter.setPromptText("filter list");

        // create list with a call back to be used
        // to refresh passwordContentPane when selected passw has changed
        // --------------------------------------------------------------

        passwordList = new PasswordList(this.currentStage, newPasswd -> {
            passwordContentPane.getChildren().clear();
            if (newPasswd != null) {
                newPasswd.accept(new PasswordDisplayer(passwordContentPane));
            }
        });
        passwordList.getStyleClass().add(Styles.PASSWD_LIST);
        passwordList.setPrefWidth(currentStage.getWidth() * 0.33);


        filter.setOnKeyReleased((evt) -> {
            final String text = filter.getText();
            passwordList.updateFilter(text);
        });

        passwordList.setPrefWidth(currentStage.getWidth() * 0.33);
        currentStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            passwordList.setPrefWidth(newValue.doubleValue() * 0.33);
        });

        box.getChildren().addAll(filter, passwordList);
        pane.setLeft(box);
    }

    private void addPasswordContent(BorderPane pane) {
        passwordContentPane = new GridPane();
        passwordContentPane.getStyleClass().add(Styles.PASSWORD_CONTENT);
        pane.setCenter(passwordContentPane);
        passwordContentPane.setPadding(new Insets(5));
        currentStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            passwordContentPane.setPrefWidth(newValue.doubleValue() * 0.65);
        });

    }


    private void addSyncStatus(BorderPane pane) {
        state = new GoogleDriveVisualState();
        pane.setBottom(state);
        state.setMaxHeight(40);
        state.setPrefHeight(40);
        BorderPane.setAlignment(state, Pos.BOTTOM_LEFT);

    }

    private void addMenuBar(BorderPane pane) {
        final ResourceBundle labelResourceBundle = I18NHelper.getLabelResourceBundle();
        final MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(currentStage.widthProperty());
        menuBar.setUseSystemMenuBar(true);
        pane.setTop(menuBar);

        // Main menu
        // **********
        {
            Menu mainMenu = new Menu("JPasswd");
            menuBar.getMenus().add(mainMenu);

            // files menu
            MenuItem exit = new MenuItem(labelResourceBundle.getString("exit"));
            exit.setOnAction(t -> quitAppFromMenu());

            MenuItem newWebSite = new MenuItem(labelResourceBundle.getString("addwebsite"));
            newWebSite.setOnAction(t -> new CreateOrModifyWebSitePassword(currentStage).show());

            MenuItem newNote = new MenuItem(labelResourceBundle.getString("addnote"));
            newNote.setOnAction(t -> new CreateOrModifyNote(currentStage).show());

            mainMenu.getItems().addAll(newWebSite, newNote, exit);
        }

        // Tools Menu
        // **************
        {
            Menu tools = new Menu(labelResourceBundle.getString("tools"));
            menuBar.getMenus().add(tools);
            MenuItem generatePassword = new MenuItem(labelResourceBundle.getString("generatepassword"));
            generatePassword.setOnAction(t -> {
                new GeneratePassword(currentStage).show();
            });

            MenuItem changeMainPassword = new MenuItem(labelResourceBundle.getString("changeapppassword"));
            changeMainPassword.setOnAction(t -> {
                new ChangeMainPassword(currentStage).show();
            });

            // add menu to clear deleted passwords
            final String label = labelResourceBundle.getString("compdatabase");
            MenuItem compress = new MenuItem(label.replaceFirst("VAL","0"));
            compress.setOnAction(t->{
                Controller.compressDatabase(currentStage);
            });
            final int[] deleted = {0};

            Consumer<Integer> changeLabel = (i)->{
                Platform.runLater(() -> compress.setText(label.replaceFirst("VAL",""+i)));
            };

            Observer countDeletedPasswords= (o, passwordStoreEvent) -> {
                PasswordStoreEventVisitor storeEvent = new PasswordStoreEventVisitor() {
                    @Override
                    public void visit(PasswordRemoved r) {
                        deleted[0]++;
                        changeLabel.accept(deleted[0]);
                    }

                    @Override
                    public void visit(PasswordDefinitivelyRemoved r) {
                        deleted[0]--;
                        changeLabel.accept(deleted[0]);
                    }
                    @Override
                    public void visit(PasswordModifiedOrCreated p) {
                        PasswordVisitor countDeleted = new PasswordVisitor() {
                            @Override
                            public void visit(WebLoginAndPassword t) {

                            }

                            @Override
                            public void visit(GoogleDrive t) {

                            }

                            @Override
                            public void visit(PasswordString s) {

                            }

                            @Override
                            public void visit(DeletedPassword s) {
                                deleted[0]++;
                                changeLabel.accept(deleted[0]);
                            }

                            @Override
                            public void visit(Note note) {

                            }
                        };
                        p.getPassword().accept(countDeleted);
                    }
                };
                ((PasswordStoreEvent)passwordStoreEvent).accept(storeEvent);

            };
            Controller.suscribeToPassword(countDeletedPasswords);


            MenuItem reIndexeLucene = new MenuItem(labelResourceBundle.getString("rebuildindexes"));
            reIndexeLucene.setOnAction(t -> {
                Controller.rebuildIndexes();
            });

            CheckMenuItem enableSyncToGoogleDrive = new GDriveSyncCheckMenu(currentStage, labelResourceBundle.getString("enablesynctogdrive"));
            enableSyncToGoogleDrive.setSelected(false);


            tools.getItems().addAll(generatePassword, changeMainPassword, enableSyncToGoogleDrive, reIndexeLucene,compress);
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
    private PasswordTree passwordTree;
    private GridPane passwordContentPane;
    private GoogleDriveVisualState state;

}