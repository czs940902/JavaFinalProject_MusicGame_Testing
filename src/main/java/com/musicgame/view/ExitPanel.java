package com.musicgame.view;

import com.musicgame.Main;
import com.musicgame.interfaces.IPanel;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ExitPanel implements IPanel {
    private final StackPane root;
    
    public ExitPanel() {
        root = new StackPane();
        root.setPrefSize(Main.GAME_W, Main.GAME_H);

        // Background
        Rectangle background = new Rectangle(Main.GAME_W, Main.GAME_H);
        background.setFill(Color.BLACK);
        background.setOpacity(0.5);

        // Dialog box
        Rectangle dialogBackground = new Rectangle(800, 400);
        dialogBackground.setFill(Color.web("#525252"));

        // Dialog text
        Label dialogText = new Label("是否要離開遊戲？");
        dialogText.setStyle("""
            -fx-font-size: 60px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
            """);

        // Cancel button
        Button cancelButton = new Button("取消");
        cancelButton.setPrefSize(385, 100);
        cancelButton.setStyle("""
            -fx-background-color: #619AE3;
            -fx-font-size: 40px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
            """);
        cancelButton.setOnAction(null);

        // Exit button
        Button exitButton = new Button("離開");
        exitButton.setPrefSize(385, 100);
        exitButton.setStyle("""
            -fx-background-color: #D41D04;
            -fx-font-size: 40px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
            """);
        exitButton.setOnAction(null);
        
        // Groupping
        HBox dialogButtons = new HBox(10, cancelButton, exitButton);
        dialogButtons.setPrefSize(800, 120);
        dialogButtons.setAlignment(Pos.CENTER);
        VBox dialogContent = new VBox();
        dialogContent.setPrefSize(800, 400);
        dialogContent.setAlignment(Pos.CENTER);
        StackPane textArea = new StackPane(dialogText);
        textArea.setPrefSize(800, 280);
        StackPane buttonArea = new StackPane(dialogButtons);
        buttonArea.setPrefSize(800, 120);
        dialogContent.getChildren().addAll(textArea, buttonArea);
        StackPane dialogPane = new StackPane(dialogBackground, dialogContent);
        dialogPane.setPrefSize(800, 400);
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
