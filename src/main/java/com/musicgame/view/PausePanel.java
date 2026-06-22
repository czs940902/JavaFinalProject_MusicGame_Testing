package com.musicgame.view;

import com.musicgame.Main;
import com.musicgame.interfaces.IPanel;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PausePanel implements IPanel {
    private final StackPane root;
    
    public PausePanel() {
        root = new StackPane();
        root.setPrefSize(Main.GAME_W, Main.GAME_H);

        // Background
        Rectangle background = new Rectangle(Main.GAME_W, Main.GAME_H);
        background.setFill(Color.BLACK);
        background.setOpacity(0.5);

        // Dialog box
        Rectangle dialogBackground = new Rectangle(600, 600);
        dialogBackground.setFill(Color.web("#525252"));

        // Dialog text
        Label dialogText = new Label("遊戲暫停");
        dialogText.setStyle("""
            -fx-font-size: 60px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
            """);

        // Cancel button
        Button cancelButton = new Button("繼續");
        cancelButton.setPrefSize(580, 100);
        cancelButton.setStyle("""
            -fx-background-color: #619AE3;
            -fx-font-size: 40px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
            """);
        cancelButton.setOnAction(null);

        // Exit button
        Button exitButton = new Button("退出");
        exitButton.setPrefSize(580, 100);
        exitButton.setStyle("""
            -fx-background-color: #D41D04;
            -fx-font-size: 40px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
            """);
        exitButton.setOnAction(null);

        // Restart button
        Button restartButton = new Button("重新開始");
        restartButton.setPrefSize(580, 100);
        restartButton.setStyle("""
            -fx-background-color: #949494;
            -fx-font-size: 40px;
            -fx-text-fill: black;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
            """);
        exitButton.setOnAction(null);
        
        // Groupping
        VBox dialogButtons = new VBox(10, cancelButton, exitButton, restartButton);
        dialogButtons.setPrefSize(600, 340);
        dialogButtons.setAlignment(Pos.CENTER);
        VBox dialogContent = new VBox();
        dialogContent.setPrefSize(600, 600);
        dialogContent.setAlignment(Pos.CENTER);
        StackPane textArea = new StackPane(dialogText);
        textArea.setPrefSize(600, 260);
        StackPane buttonArea = new StackPane(dialogButtons);
        buttonArea.setPrefSize(600, 340);
        dialogContent.getChildren().addAll(textArea, buttonArea);
        StackPane dialogPane = new StackPane(dialogBackground, dialogContent);
        dialogPane.setPrefSize(600, 600);
        root.getChildren().addAll(background, dialogPane);
    }
    
    @Override
    public Pane getRoot() {
        return root;
    }
    
    @Override
    public void onShow() {

    }
    
    @Override
    public void onHide() {

    }
    
    @Override
    public void onClose() {

    }
}
