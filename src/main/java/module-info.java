module com.musicgame {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;

    opens com.musicgame to javafx.fxml;
    exports com.musicgame;
}
