package com.musicgame;

import com.musicgame.core.*;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {
    public static final double GAME_W = 1920;
    public static final double GAME_H = 1080;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Calculate full-screen scale
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();
        double scaleWidth = screenWidth / GAME_W;
        double scaleHeight = screenHeight / GAME_H;
        double scale = Math.min(scaleWidth, scaleHeight);

        // Get pane from ViewManager
        Pane gamePane = ViewManager.getInstance().getView();
        gamePane.setPrefSize(GAME_W, GAME_H);
        gamePane.setMaxSize(GAME_W, GAME_H);
        gamePane.setMinSize(GAME_W, GAME_H);
        
        // Scale settings
        Group scaledContent = new Group(gamePane);
        javafx.scene.transform.Scale scaleTransform = new javafx.scene.transform.Scale(scale, scale);
        scaleTransform.setPivotX(GAME_W / 2.0);
        scaleTransform.setPivotY(GAME_H / 2.0);
        scaledContent.getTransforms().add(scaleTransform);
        StackPane root = new StackPane(scaledContent);
        root.setStyle("-fx-background-color: black;");

        // Set full-screen
        primaryStage.setResizable(false);
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // Final configurations
        Scene scene = new Scene(root, screenWidth, screenHeight, Color.BLACK);
        primaryStage.setTitle("Music Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
