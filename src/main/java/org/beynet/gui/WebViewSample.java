package org.beynet.gui;

import com.sun.javafx.webkit.WebConsoleListener;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class WebViewSample extends Application {
    private Scene scene;
    @Override public void start(Stage stage) {
        // create the scene
        stage.setTitle("Web View");
        scene = new Scene(new Browser(),750,500, Color.web("#666970"));
        stage.setScene(scene);
        scene.getStylesheets().add("webviewsample/BrowserToolbar.css");
        stage.show();
    }

    public static void main(String[] args){
        launch(args);
    }
}
class Browser extends Region {

    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();

    public Browser() {
        //apply the styles
        getStyleClass().add("browser");
        WebConsoleListener.setDefaultListener(new WebConsoleListener()
        {
            @Override
            public void messageAdded(WebView webView, String message, int lineNumber, String sourceId)
            {
                System.out.println("Console: [" + sourceId + ":" + lineNumber + "] " + message);
            }
        });
        // load the web page
        webEngine.load("https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=92f2b06c-cde4-47e1-87d0-731726fec933&grant_type=authorization_code&code=A19iZqU_iyGtS~jxTQuGX1~J4~41Hw-Dom&redirect_uri=http%3A%2F%2Flocalhost%3A9091%2Fjpasswd&response_type=code&response_mode=query&scope=offline_access user.read mail.read&state=12345");
        //add the web view to the scene
        getChildren().add(browser);

    }
    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    @Override protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(browser,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
    }

    @Override protected double computePrefWidth(double height) {
        return 750;
    }

    @Override protected double computePrefHeight(double width) {
        return 500;
    }
}