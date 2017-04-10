package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.server_connection.ServerHandlerReader;
import model.server_connection.ServerHandlerWriter;
import model.server_connection.TelnetWriter;

import java.io.IOException;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ResourceBundle;

public class ConnectedController extends PreGameView implements Initializable {
    TelnetWriter connectionWriter;
    @FXML RadioButton bke;
    @FXML RadioButton reversi;
    @FXML ComboBox opponentSelection;
    @FXML TextArea logArea;
    @FXML ToggleGroup test;
    @FXML Label loggedInAs;
    private String selectedOpponent;
    boolean succesfull = false;
    String command = "no command yet"; //@todo create command class
    String playerName = null;

    public void clickButton(){
        System.out.println("test");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // @todo: EVEN LEZEN!
        // @todo We moeten timelines gebruiken, omdat bij het initialiseren getOpponents() wordt aangeroepen voor de
        // @todo get playerlist commando. Vervolgens moet de thread (ServerHandlerReader.java) deze afhandelen en de
        // @todo players eruit filteren om ze in deze superclass te zetten. Omdat het een thread is, is de thread soms
        // @todo eerder of later. Dit zorgt voor unexpected behaviour (soms null values bij super.getPlayerList() en soms niet).
        // @todo Daarom heb ik een soort Task of Keyframe systeem toegevoegd om te zorgen dat ze altijd in de juiste
        // @todo volgorde worden afgehandeld. (Misschien kan dit ook naar de startGame() method verplaatst worden. Even overleggen).
        //Platform.runLater(()-> {
            //updateOpponentSelection();
            //System.out.println("Playerlist22 " + super.getPlayerList());
        //});
        // Create keyframes and use a timeline to run the tasks after each other
        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), e -> getOpponents());
        final KeyFrame kf2 = new KeyFrame(Duration.millis(500), e ->  updateOpponentSelection());
        final Timeline timeline = new Timeline(kf1, kf2);
        Platform.runLater(timeline::play);
    }

//    @Override
//    public void setSuccesfull(boolean status) {
//        succesfull = status;
//        System.out.println("ConnectedController: Last executed command was succesfull");
//        opponentSelection.getItems().add("test");
//        updateOpponentSelection();
//    }

    public void setPlayerName(String playerName){
        this.playerName = playerName;
        loggedInAs.setText("Logged in as: " + playerName);
    }

    @Override
    public void startGame(String game, String playerToMove) {
//        final Stage stage = this.stage;
//        final String playerName = this.playerName;
        System.out.println("startgame wel aangeroepen?");
        System.out.println(game);
        //change view and start game controller of certain game
        Platform.runLater(() -> {
            if (game.equals("Tictactoe")) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/tictactoeGrid.fxml"));
                Parent root = null;
                try {
                    root = (Parent) fxmlLoader.load();

                    //Set writer in controller
                    TictactoeController tictactoeController = fxmlLoader.getController();
                    tictactoeController.setConnectionWriter(connectionWriter);

//                    //Create a game telnet reader @todo dirty please think of something cleaner
//                    GameReader gameReader = new GameReader(super.getSocket());
//                    Thread t1 = new Thread(gameReader);
//                    t1.start();
//                    tictactoeController.setConnectionReader(gameReader);


                    //Notify if we have to start or opponent is starting with a move, after this the gamereader will handle everything
                    if (playerToMove.equals(this.playerName)) {
                        tictactoeController.ourturn();
                    }
                    // Remove this view from the views that get notified on updates from the reader
                    //connectionReader.removeView(this);
                    ServerHandlerReader.currentController = null;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ConcurrentModificationException cme) {
                    //System.out.println("Test");
                    cme.printStackTrace();
                }
                ServerHandlerReader.stage.setTitle("Our playername = " + playerName);
                ServerHandlerReader.stage.setScene(new Scene(root, 800, 800));

            } else if (game.equals("Reversi")) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/othelloGrid.fxml"));
                Parent root = null;
                try {
                    root = (Parent) fxmlLoader.load();

                    //Set writer in controller
                    OthelloController othelloController = fxmlLoader.getController();
                    othelloController.setConnectionWriter(connectionWriter);

//                    //Create a game telnet reader @todo dirty please think of something cleaner
//                    GameReader gameReader = new GameReader(super.getSocket());
//                    Thread t1 = new Thread(gameReader);
//                    t1.start();
//                    othelloController.setConnectionReader(gameReader);


                    //Notify if we have to start or opponent is starting with a move, after this the gamereader will handle everything
                    if (playerToMove.equals(this.playerName)) {
                        othelloController.ourturn();
                    }
                    // Remove this view from the views that get notified on updates from the reader
                    connectionReader.removeView(this);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ConcurrentModificationException cme) {
//                    System.out.println("Test");
                    cme.printStackTrace();
                }
                ServerHandlerReader.stage.setTitle("Our playername = " + playerName);
                ServerHandlerReader.stage.setScene(new Scene(root, 800, 800));
            }


        });}



    public void logout(){
        System.out.println("Logged out of the server");
        super.updateLog("logout");
        connectionWriter.sendData("logout");

    }

    public void updateLog(String currentLine){
        super.updateLog(currentLine);
        logArea.clear();
        logArea.appendText(super.getLog());
    }

    /**
     * Get all possible commands from server
     */
    public void help(){
        super.updateLog("help");
        connectionWriter.sendData("help");
    }

    /**
     * Subscribe to a game
     * Usage Subscribe <game> @ server
     */
    public void subscribe(){
        String game = "";
        if(reversi.isSelected()){
            game = "Reversi";
        }else{
            game = "Tic-tac-toe";
        }

        //Options server-sided are "Reversi" and "Tic-tac-toe"
        super.updateLog("subscribe " + game);
        connectionWriter.sendData("subscribe " + game);
    }

    public void getOpponents(){
        ServerHandlerWriter.getPlayerList();
    }

    private void updateOpponentSelection(){
        if(super.getPlayerList()!=null){
            opponentSelection.getItems().clear();
            for(String player: super.getPlayerList()){
                opponentSelection.getItems().add(player);
            }
            System.out.println(super.getPlayerList().size());
        }
    }

    public void getOpponentIndividual() {
        selectedOpponent = opponentSelection.getSelectionModel().getSelectedItem().toString();
    }

    public void invPlayer() {
        String selectedGame = "";
        if (bke.isSelected()) {
            selectedGame = "Tic-tac-toe";
        } else if (reversi.isSelected()) {
            selectedGame = "Reversi";
        }
        ServerHandlerWriter.writeSend("challenge " + "\"" + selectedOpponent.replace(" ", "") +"\""  + " " + "\"" + selectedGame + "\"");
    }

}
