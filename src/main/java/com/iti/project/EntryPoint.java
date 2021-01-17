package com.iti.project;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class EntryPoint extends Application {

    private static Stage window;

    private ListView<String> usersList;

    private BorderPane sceneTwoPane;
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5;
    private Button button6;
    private Button button7;
    private Button button8;
    private Button button9;
    private Button playButton;
    private Button[] buttonsArray;
    private TextField textField;

    private BorderPane sceneOnePane;
    private TextField logInField;
    private Button loginButton;

    private Scene sceneOne;
    private Scene sceneTwo;

    private GameClient gameClient;

    @Override
    public void init(){

        this.usersList = new ListView<>();

        this.sceneOnePane = new BorderPane();
        this.logInField = new TextField();
        this.loginButton = new Button();
        this.loginButton.setText("Login");
        this.sceneOnePane.setTop(logInField);
        this.sceneOnePane.setBottom(loginButton);

        this.button1 = new Button("");
        this.button1.setId("0");
        this.button2 = new Button("");
        this.button2.setId("1");
        this.button3 = new Button("");
        this.button3.setId("2");
        this.button4 = new Button("");
        this.button4.setId("3");
        this.button5 = new Button("");
        this.button5.setId("4");
        this.button6 = new Button("");
        this.button6.setId("5");
        this.button7 = new Button("");
        this.button7.setId("6");
        this.button8 = new Button("");
        this.button8.setId("7");
        this.button9 = new Button("");
        this.button9.setId("8");
        this.playButton = new Button("Play Opponent");
        this.buttonsArray = new Button[]{button1, button2, button3, button4, button5, button6, button7, button8, button9};

        this.textField = new TextField();

        this.sceneTwoPane = new BorderPane();

        VBox vBox = new VBox();
        vBox.setSpacing(10);

        FlowPane flowPane1 = new FlowPane();
        flowPane1.setHgap(5);
        FlowPane flowPane2 = new FlowPane();
        flowPane2.setHgap(5);
        FlowPane flowPane3 = new FlowPane();
        flowPane3.setHgap(5);

        flowPane1.getChildren().addAll(button1, button2, button3);
        flowPane2.getChildren().addAll(button4, button5, button6);
        flowPane3.getChildren().addAll(button7, button8, button9);

        vBox.getChildren().addAll(flowPane1, flowPane2, flowPane3, playButton);

        sceneTwoPane.setCenter(vBox);
        sceneTwoPane.setBottom(textField);
        sceneTwoPane.setRight(usersList);


        sceneOne = new Scene(this.sceneOnePane, 400, 300);
        sceneTwo = new Scene(this.sceneTwoPane, 400, 300);

        //this.gameClient = new GameClient(this.textField, this.buttonsArray, sceneTwo, usersList);

    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        window = primaryStage;
        this.gameClient = new GameClient(this.textField, this.buttonsArray, sceneTwo, usersList);

        // To ensure program terminates when exit icon (x) is pressed
        window.setOnCloseRequest((e)-> {
            System.exit(0);
        });

        this.loginButton.setOnAction(e->{
            gameClient.login(logInField.getText());
        });

        for(Button button : this.buttonsArray){
            button.setOnAction((e)->{
                if(gameClient.getMyTurn() && button.getText().equals("")){
                    gameClient.sendMoveToServer(button.getId(), gameClient.getSymbol());
                }
            });
        }

        this.playButton.setOnAction(e->{
            ObservableList<String> selectedUser = this.usersList.getSelectionModel().getSelectedItems();
            if(selectedUser.size() == 1){
                String opponent = selectedUser.get(0);
                // Remove this code if you remove the astrix at the end of logged in users
                if(opponent.endsWith("*")){
                    opponent = opponent.substring(0, opponent.length()-1);
                }

                gameClient.startGameWithOpponent(opponent);
            }
        });

        primaryStage.setTitle("Hello World");
        primaryStage.setScene(sceneOne);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getWindow(){
        return window;
    }

    public Stage createPopUpWindow(String msg, Stage parentStage){
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        VBox dialogVbox = new VBox(45);
        dialogVbox.getChildren().add(new Text(msg));
        Scene dialogScene = new Scene(dialogVbox, 250, 50);
        dialog.setScene(dialogScene);
        return dialog;
    }


}
