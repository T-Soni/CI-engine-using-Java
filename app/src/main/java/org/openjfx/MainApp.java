// package org.openjfx;

// import javafx.application.Application;
// import javafx.fxml.FXMLLoader;
// import javafx.scene.Parent;
// import javafx.scene.Scene;
// import javafx.stage.Stage;


// public class MainApp extends Application {

//     @Override
//     public void start(Stage stage) throws Exception {
//         Parent root = FXMLLoader.load(getClass().getResource("scene.fxml"));
        
//         Scene scene = new Scene(root);
//         scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        
//         stage.setTitle("JavaFX and Gradle");
//         stage.setScene(scene);
//         stage.show();
//     }

//     public static void main(String[] args) {
//         launch(args);
//     }

// }

package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/openjfx/scene.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.setTitle("JavaFX Application");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
