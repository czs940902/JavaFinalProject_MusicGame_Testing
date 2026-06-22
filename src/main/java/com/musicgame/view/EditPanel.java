package com.musicgame.view;

import com.musicgame.Main;
import com.musicgame.interfaces.IPanel;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class EditPanel implements IPanel {
    private final StackPane root;

    public EditPanel() {
        root = new StackPane();
        root.setPrefSize(Main.GAME_W, Main.GAME_H);

        // Background
        Rectangle background = new Rectangle(Main.GAME_W, Main.GAME_H);
        background.setFill(Color.BLACK);
        background.setOpacity(0.5);

        // Dialog box
        Rectangle dialogBackground = new Rectangle(1600, 900);
        dialogBackground.setFill(Color.web("#525252"));

        // Dialog title
        Label dialogTitle = new Label("編輯譜面");
        dialogTitle.setStyle("""
                -fx-font-size: 80px;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
                """);

        // Textbox labels
        Label textMusicTitle = new Label("歌曲名稱");
        Label textMusicSinger = new Label("歌手");
        Label textMapAuthor = new Label("譜面作者");
        String textStyle = """
                -fx-font-size: 30px;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
                """;
        textMusicTitle.setStyle(textStyle);
        textMusicSinger.setStyle(textStyle);
        textMapAuthor.setStyle(textStyle);

        // Textboxes
        String textfieldDefaultStyle = """
                -fx-background-color: transparent;
                -fx-background-radius: 0;
                -fx-border-color: transparent transparent white transparent;
                -fx-border-width: 0 0 2px 0;
                -fx-border-radius: 0;
                -fx-font-size: 30px;
                -fx-text-fill: white;
                -fx-padding: 5px 4px;
                """;
        String textfieldFocusedStyle = """
                -fx-background-color: transparent;
                -fx-background-radius: 0;
                -fx-border-color: transparent transparent #3b82f6 transparent;
                -fx-border-width: 0 0 3px 0;
                -fx-border-radius: 0;
                -fx-font-size: 30px;
                -fx-text-fill: white;
                -fx-padding: 5px 4px;
                """;
        TextField textFieldMusicTitle = new TextField();
        TextField textFieldMusicSinger = new TextField();
        TextField textFieldMapAuthor = new TextField();
        textFieldMusicTitle.setPrefSize(600, 60);
        textFieldMusicSinger.setPrefSize(600, 60);
        textFieldMapAuthor.setPrefSize(600, 60);
        textFieldMusicTitle.setStyle(textfieldDefaultStyle);
        textFieldMusicSinger.setStyle(textfieldDefaultStyle);
        textFieldMapAuthor.setStyle(textfieldDefaultStyle);
        textFieldMusicTitle.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                textFieldMusicTitle.setStyle(textfieldFocusedStyle);
            } else {
                textFieldMusicTitle.setStyle(textfieldDefaultStyle);
            }
        });
        textFieldMusicSinger.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                textFieldMusicSinger.setStyle(textfieldFocusedStyle);
            } else {
                textFieldMusicSinger.setStyle(textfieldDefaultStyle);
            }
        });
        textFieldMapAuthor.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                textFieldMapAuthor.setStyle(textfieldFocusedStyle);
            } else {
                textFieldMapAuthor.setStyle(textfieldDefaultStyle);
            }
        });

        // Right side buttons
        Button soundSourceButton = new Button("請選擇音源...");
        Button thumbnailSourceButton = new Button("請選擇封面圖片...");
        soundSourceButton.setPrefSize(640, 100);
        thumbnailSourceButton.setPrefSize(640, 360);
        soundSourceButton.setOnAction(null);
        thumbnailSourceButton.setOnAction(null);
        String rightButtonStyle = """
                -fx-font-size: 30px;
                -fx-text-fill: black;
                -fx-font-weight: bold;
                -fx-background-color: #C9C9C9;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
                """;
        soundSourceButton.setStyle(rightButtonStyle);
        thumbnailSourceButton.setStyle(rightButtonStyle);

        // Buttom side buttons
        Button cancelButton = new Button("取消");
        Button nextButton = new Button("下一步");
        cancelButton.setPrefSize(200, 80);
        nextButton.setPrefSize(200, 80);
        cancelButton.setOnAction(null);
        nextButton.setOnAction(null);
        String buttomButtonStyle = """
                -fx-font-size: 42px;
                -fx-font-weight: bold;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
                """;
        cancelButton.setStyle("-fx-background-color: #D41D04; -fx-text-fill: white;" + buttomButtonStyle);
        nextButton.setStyle("-fx-background-color: #619AE3; -fx-text-fill: white;" + buttomButtonStyle);

        // Groupping
        AnchorPane group_dialog = new AnchorPane();
        group_dialog.setMaxSize(1600, 900);
        group_dialog.getChildren().addAll(
            dialogBackground,
            dialogTitle, 
            textMusicTitle,
            textFieldMusicTitle,
            textMusicSinger,
            textFieldMusicSinger,
            textMapAuthor,
            textFieldMapAuthor,
            soundSourceButton,
            thumbnailSourceButton,
            cancelButton,
            nextButton
        );
        AnchorPane.setLeftAnchor(dialogBackground, 0.0);
        AnchorPane.setTopAnchor(dialogBackground, 0.0);
        AnchorPane.setLeftAnchor(dialogTitle, 40.0);
        AnchorPane.setTopAnchor(dialogTitle, 30.0);
        AnchorPane.setLeftAnchor(textMusicTitle, 65.0);
        AnchorPane.setTopAnchor(textMusicTitle, 160.0);
        AnchorPane.setLeftAnchor(textFieldMusicTitle, 65.0);
        AnchorPane.setTopAnchor(textFieldMusicTitle, 220.0);
        AnchorPane.setLeftAnchor(textMusicSinger, 65.0);
        AnchorPane.setTopAnchor(textMusicSinger, 310.0);
        AnchorPane.setLeftAnchor(textFieldMusicSinger, 65.0);
        AnchorPane.setTopAnchor(textFieldMusicSinger, 370.0);
        AnchorPane.setLeftAnchor(textMapAuthor, 65.0);
        AnchorPane.setTopAnchor(textMapAuthor, 460.0);
        AnchorPane.setLeftAnchor(textFieldMapAuthor, 65.0);
        AnchorPane.setTopAnchor(textFieldMapAuthor, 520.0);
        AnchorPane.setLeftAnchor(soundSourceButton, 890.0);
        AnchorPane.setTopAnchor(soundSourceButton, 160.0);
        AnchorPane.setLeftAnchor(thumbnailSourceButton, 890.0);
        AnchorPane.setTopAnchor(thumbnailSourceButton, 310.0);
        AnchorPane.setLeftAnchor(cancelButton, 1140.0);
        AnchorPane.setTopAnchor(cancelButton, 780.0);
        AnchorPane.setLeftAnchor(nextButton, 1360.0);
        AnchorPane.setTopAnchor(nextButton, 780.0);
        root.getChildren().addAll(background, group_dialog);
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
