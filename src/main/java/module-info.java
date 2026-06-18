module com.czs940902 {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;

    opens com.czs940902 to javafx.fxml;
    exports com.czs940902;
}
