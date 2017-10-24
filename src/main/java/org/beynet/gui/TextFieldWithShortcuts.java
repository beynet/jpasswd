package org.beynet.gui;

import javafx.scene.control.TextField;


public class TextFieldWithShortcuts extends TextField {

    TextFieldWithShortcuts(String text) {
        super(text);
        init();
    }

    public TextFieldWithShortcuts() {
        super();
        init();
    }

    void init() {
        setOnKeyReleased(event->{
                if ("a".equalsIgnoreCase(event.getText()) && event.isShortcutDown()) {
                    selectAll();
                }
            });
    }

}
