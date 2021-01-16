package com.iti.project;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfirmBox {

    private static boolean answer;

    public static boolean display (String msg){
        final Stage window = new Stage();
        window.setMinWidth(250);
        window.setMinHeight(100);
        window.initModality(Modality.APPLICATION_MODAL);
        //dialog.initOwner(parentStage);
        VBox dialogVbox = new VBox(5);
        HBox yesAndNoButtonsBox = new HBox(5);
        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");
        yesButton.setOnAction((e)->{
            answer = true;
            window.close();
        });
        noButton.setOnAction((e)->{
            answer = false;
            window.close();
        });
        yesAndNoButtonsBox.getChildren().addAll(yesButton, noButton);
        dialogVbox.getChildren().addAll(new Text(msg), yesAndNoButtonsBox);
        Scene dialogScene = new Scene(dialogVbox, 250, 50);
        window.setScene(dialogScene);
        window.showAndWait();
        return answer;
    }
}
