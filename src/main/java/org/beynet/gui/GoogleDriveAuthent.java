package org.beynet.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by beynet on 22/10/14.
 */
public class GoogleDriveAuthent extends Dialog {
    public GoogleDriveAuthent(Stage father,String authentURI,Object mutex,Consumer<String> setCode) {
        super(father,null,null);
        this.setCode = setCode;
        this.mutex = mutex;
        VBox root = new VBox();
        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();
        Button launch = new Button("launch");
        launch.setOnAction(p->{
            webEngine.load(authentURI);
        });
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue!=null && newValue.startsWith("http://localhost")) {
                setCode.accept(getCodeFromURL(newValue));
                synchronized (mutex) {
                    mutex.notify();
                }
                close();
            }
        });
        root.getChildren().addAll(launch,browser);
        getRootGroup().getChildren().add(root);
    }


    /**
     * parse http://localhost URI to retrieve query param code value
     * @param uri
     * @return
     */
    static String getCodeFromURL(String uri) {
        final String code ;
        final String afterCode = uri.substring(uri.toString().indexOf("code=") + "code=".length());
        int offset = afterCode.indexOf("&");
        if (offset==-1) {
            code = afterCode;
        }
        else {
            code = afterCode.substring(0,offset);
        }
        return code;
    }

    private Object mutex;
    private Consumer<String> setCode;
}
