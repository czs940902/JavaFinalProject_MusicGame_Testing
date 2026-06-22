package com.musicgame.view;

import com.musicgame.Main;
import com.musicgame.core.AssetManager;
import com.musicgame.interfaces.IView;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class LibraryView implements IView {
    private final StackPane root;

    public LibraryView() {
        root = new StackPane();
        root.setPrefSize(Main.GAME_W, Main.GAME_H);

        // Get background image from AssetManager
        Image backgroundImage = AssetManager.getInstance().getBackgroundImage();
        ImageView backgroundView = new ImageView(backgroundImage);

        // Title
        Label libraryTitle = new Label("Library");
        libraryTitle.setStyle("""
                -fx-font-size: 100px;
                -fx-text-fill: white;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);
                """);

        // Search box
        TextField searchBox = new TextField();
        searchBox.setPrefSize(600, 80);
        searchBox.setPromptText("搜尋譜面...");
        searchBox.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 0;
                -fx-border-width: 0;
                -fx-border-radius: 0;
                -fx-font-size: 30px;
                -fx-text-fill: black;
                -fx-prompt-text-fill: #9ca3af;
                -fx-padding: 5px 10px;
                """);

        // Groupping
        AnchorPane libraryContent = new AnchorPane();
        libraryContent.setMaxSize(1920, 1080);
        libraryContent.getChildren().addAll(
            backgroundView,
            libraryTitle,
            searchBox
        );
        AnchorPane.setLeftAnchor(backgroundView, 0.0);
        AnchorPane.setTopAnchor(backgroundView, 0.0);
        AnchorPane.setLeftAnchor(libraryTitle, 100.0);
        AnchorPane.setTopAnchor(libraryTitle, 80.0);
        AnchorPane.setLeftAnchor(searchBox, 1220.0);
        AnchorPane.setTopAnchor(searchBox, 100.0);

        root.getChildren().add(libraryContent);
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
}
