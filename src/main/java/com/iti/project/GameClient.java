package com.iti.project;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameClient {

    public static final String ONLINE_SYMBOL = "*";

    private final AtomicBoolean running = new AtomicBoolean(false); // Ignore it for now
    private Thread listenToServerThread;
    private Socket clientSocket;
    private DataInputStream dis;
    private PrintStream ps;

    private TextField textField;
    private Button[] buttonsArray;
    private Scene gameScene;
    private ListView<String> usersList;

    private int gameId;   // -1 is the default, means this client is not in a game
    private String userName;   // null is the default, means user hasn't logged in (or signed up) yet
    private String opponentName;   // null is the default, gets a value when game is accepted by opponent
    private int symbol;
    private boolean myTurn;

    public GameClient(TextField textField, Button[] buttonsArray, Scene gameScene, ListView<String> usersList){
        try {
            this.usersList = usersList;
            this.textField = textField;
            this.buttonsArray = buttonsArray;
            this.gameScene = gameScene;

            this.gameId = -1;
            this.userName = null;
            this.opponentName = null;
            this.symbol = 0;
            this.myTurn = false;

            this.clientSocket = new Socket("localhost", 5000);
            this.dis = new DataInputStream(this.clientSocket.getInputStream());
            this.ps= new PrintStream(this.clientSocket.getOutputStream());

            this.listenToServerThread = new Thread(()->{
                this.listenToServer();
            });
            this.listenToServerThread.start();

        } catch (IOException e) {
            // Handle exception here if user tries to connect and server is down
            Platform.runLater(()->PopupWindow.display("Can't connect to server! Please try again later."));
            e.printStackTrace();
        }
    }

    private void listenToServer(){
        this.running.set(true);
        while (this.running.get()){
            String str= null;
            try {
                str = dis.readLine();
                handleServerResponse(str);
            } catch (IOException e) {
                // Handle here server sudden shut down while client is running
                e.printStackTrace();
               closeConnection();
                Platform.runLater(()->PopupWindow.display("Server connection lost! Please try again later."));
            }
        }
    }

    private void closeConnection(){
        this.running.set(false);
        this.ps.close();
        try {
            this.clientSocket.close();
            this.dis.close();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        System.out.println("Socket Closed");
    }

    private void handleServerResponse(String reply){
        JSONObject replyJson = parseStringToJsonObject(reply);
        String type = replyJson.get("type").toString();
        if(type.equals("startGame")){
            int id = Integer.parseInt(replyJson.get("gameId").toString());
            String opponent = replyJson.get("opponent").toString();
            String myTurn = replyJson.get("myTurn").toString();
            this.gameId = id;
            this.opponentName = opponent;
            if(myTurn.equals("true")){
                this.symbol = 1;  // 'X'
                this.myTurn = true;
            }else{
                this.symbol = -1; // 'O'
                this.myTurn = false;
            }
            // Updating the UI
            Platform.runLater(()->{
                this.textField.setText("Game id = " + id);
                for (Button button : this.buttonsArray){
                    button.setText("");
                }
            });
        }else if(type.equals("gameTurnResult")) {
            handleGameTurnResult(replyJson);
        }else if(type.equals("loginResult")){
            String success = replyJson.get("success").toString();
            if(success.equals("true")){
                this.userName = replyJson.get("userName").toString();
                Platform.runLater(() -> EntryPoint.getWindow().setScene(gameScene));
                requestUsers();
            }else{
                Platform.runLater(() -> PopupWindow.display("Login fail"));
            }
        }else if(type.equals("usersList")){
            this.handleUpdatingUserList(replyJson);
        }else if(type.equals("newLoggedInUser")){
            String newLoggedInUser = replyJson.get("loggedInUser").toString();
            if( this.userName != null && !this.userName.equals(newLoggedInUser)){
                int userListViewIndex = getListItemIndex(newLoggedInUser);
                if(userListViewIndex != -1){   // In case user was already in the list view but offline
                    Platform.runLater(()->this.usersList.getItems().set(userListViewIndex, newLoggedInUser+ONLINE_SYMBOL));
                }else{   // In case new user signed up (not logged in, so he wasn't visible on the list from the start)

                }
            }
        }else if(type.equals("loggedOutUser")){
            // Remove online Symbol in final version
            String loggedOutUser = replyJson.get("loggedOutUser").toString()+ONLINE_SYMBOL;
            //System.out.println(loggedOutUser);
            int userListViewIndex = getListItemIndex(loggedOutUser);
            //System.out.println(userListViewIndex);
            Platform.runLater(()->this.usersList.getItems().set(userListViewIndex,
                    loggedOutUser.substring(0, loggedOutUser.length()-1)));
        }else if(type.equals("gameTerminated")){
            System.out.println("Game terminated");
            this.cleanAfterGameIsOver();
            Platform.runLater(()->{
                this.textField.setText("Game Terminated!");
                for (Button button : this.buttonsArray){
                    button.setText("");
                }
                PopupWindow.display("Disconnected from other player!");
            });
        }else if(type.equals("startGameRequest")){
            String opponentName = replyJson.get("opponentName").toString();
            Platform.runLater(()->{
                boolean isGameAccepted = ConfirmBox.display("Accept game from " + opponentName + "?" );
                JSONObject sendToServer = new JSONObject();
                sendToServer.put("type", "startGameResponse");
                sendToServer.put("result", isGameAccepted);
                sendToServer.put("opponent", opponentName);
                this.ps.println(sendToServer.toJSONString());
            });

        }else if(type.equals("gameRejected")){
            Platform.runLater(()->{
                String error = replyJson.get("error").toString();
                PopupWindow.display(error);
            });
        }
    }

    private int getListItemIndex(String user){
        //user = user + ONLINE_SYMBOL;
        int index = -1;
        ObservableList<String> users = this.usersList.getItems();
        for(int i = 0; i < users.size(); i++){
            System.out.println(users.get(i));
            if(users.get(i).equals(user)){
                index = i;
                break;
            }
        }
        return index;
    }

    public void login(String name){
        JSONObject object = createJsonObject();
        object.put("type", "login");
        object.put("userName", name);
        this.ps.println(object.toJSONString());
    }

    public void requestUsers(){
        JSONObject object = createJsonObject();
        object.put("type", "getUsers");
        this.ps.println(object.toJSONString());
    }

    // Gets called from Main class (Fx UI) when user clicks on a button
    public void sendMoveToServer(String index, int symbol){
        JSONObject jsonObject = createJsonObject();
        jsonObject.put("type", "gameTurn");
        jsonObject.put("gameId", this.gameId);
        jsonObject.put("index", index);
        jsonObject.put("symbol", symbol);
        this.ps.println(jsonObject.toJSONString());
    }

    public void startGameWithOpponent(String opponentName){
        JSONObject jsonObject = createJsonObject();
        jsonObject.put("type", "tryGameWithOpponent");
        jsonObject.put("opponent", opponentName);
        this.ps.println(jsonObject.toJSONString());
    }

    private void handleGameTurnResult(JSONObject replyJson){
        String won = replyJson.get("won").toString();
        String lost = replyJson.get("lost").toString();
        String tie = replyJson.get("tie").toString();

        // Displaying game turn result
        Platform.runLater(()->{
            int index = Integer.parseInt(replyJson.get("index").toString());
            String myTurn = replyJson.get("myTurn").toString();
            if(this.symbol == 1){
                if(myTurn.equals("true")){  // So the previous turn was the opponent
                    this.buttonsArray[index].setText("O");
                }else{
                    this.buttonsArray[index].setText("X");
                }
            }else{
                if(myTurn.equals("true")){  // So the previous turn was the opponent
                    this.buttonsArray[index].setText("X");
                }else{
                    this.buttonsArray[index].setText("O");
                }
            }
        });

        // Check win, lose or tie conditions
        if(won.equals("false") && lost.equals("false") && tie.equals("false")){ // Game is still running
            if(replyJson.get("myTurn").toString().equals("true")){
                this.myTurn = true;
            }else{
                this.myTurn = false;
            }
        }else if(won.equals("true")){  // This user has won
            Platform.runLater(()->PopupWindow.display("Congrats! You Won."));
            this.cleanAfterGameIsOver();
        }else if(lost.equals("true")){  // This user has lost
            Platform.runLater(()->PopupWindow.display("Sorry! better luck next time."));
            this.cleanAfterGameIsOver();
        }else if(tie.equals("true")){  // No one has won, it's a tie (it's a trap :D)
            Platform.runLater(()->PopupWindow.display("Tie!!!"));
            this.cleanAfterGameIsOver();
        }
    }

    private void cleanAfterGameIsOver(){
        this.gameId = -1;
        this.opponentName = null;
        this.symbol = 0;
        this.myTurn = false;
    }

    private void handleUpdatingUserList(JSONObject replyJson){
        JSONArray users = (JSONArray) replyJson.get("users");
        JSONArray availableUsers = (JSONArray) replyJson.get("availableUsers");
        Platform.runLater(()->{
            for(Object user : users){
                // Check if user is online
                boolean isUserOnline = false;
                for(Object onlineUser : availableUsers){
                    if(onlineUser.toString().equals(user.toString())){
                        usersList.getItems().add(user.toString() + ONLINE_SYMBOL);
                        isUserOnline = true;
                        break;
                    }
                }
                if(!isUserOnline){
                    usersList.getItems().add(user.toString());
                }
            }
        });
    }

    public boolean getMyTurn(){
        return this.myTurn;
    }

    public int getSymbol(){
        return this.symbol;
    }

    private JSONObject createJsonObject(){
        return new JSONObject();
    }

    private JSONObject parseStringToJsonObject(String jsonString){
        //System.out.println(jsonString);
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(jsonString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
