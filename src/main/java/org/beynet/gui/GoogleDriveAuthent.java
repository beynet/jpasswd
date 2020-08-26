package org.beynet.gui;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Document;

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

        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("old="+oldValue+" new="+newValue);
            if (newValue!=null && newValue.startsWith("http://localhost")) {
                setCode.accept(getCodeFromURL(newValue));
                synchronized (mutex) {
                    mutex.notify();
                }
                close();
            }
        });

        /*webEngine.getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if ("SUCCEEDED".equals(newState.name())) {
                Document document = webEngine.getDocument();
                System.out.println(newState);
            }
                });*/


        root.getChildren().addAll(browser);
        getRootGroup().getChildren().add(root);
        Platform.runLater(()->webEngine.load(authentURI));
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
        return code.replaceAll("%2F","/");
    }

    private Object mutex;
    private Consumer<String> setCode;
}
