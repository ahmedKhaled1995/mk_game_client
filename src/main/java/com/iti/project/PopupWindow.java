package com.iti.project;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PopupWindow {

    public static void display (String msg){
        final Stage window = new Stage();
        window.setMinWidth(250);
        window.setMinHeight(100);
        window.initModality(Modality.APPLICATION_MODAL);
        VBox dialogVbox = new VBox(5);
        HBox buttonHBox = new HBox(5);
        Button okButton = new Button("OK");
        okButton.setOnAction((e)->{
            window.close();
        });
        buttonHBox.getChildren().addAll(okButton);
        dialogVbox.getChildren().addAll(new Text(msg), buttonHBox);
        Scene dialogScene = new Scene(dialogVbox, 250, 50);
        window.setScene(dialogScene);
        window.show();
        //window.focusedProperty();
    }

}
