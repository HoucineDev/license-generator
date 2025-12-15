module com.app.licence.licensegen {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.app.licence.licensegen to javafx.fxml;
    exports com.app.licence.licensegen;
}