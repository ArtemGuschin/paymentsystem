module com.artem.project {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.artem.project to javafx.fxml;
    exports com.artem.project;
}