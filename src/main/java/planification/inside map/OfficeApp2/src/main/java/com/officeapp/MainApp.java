package com.officeapp;

import com.officeapp.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Application entry point.
 *
 * Run via:
 *   mvn javafx:run
 * or directly from IntelliJ (add JavaFX 21 as a library / module).
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        URL fxmlUrl = getClass().getResource("/com/officeapp/MainView.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("MainView.fxml not found on classpath.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // Retrieve controller so we can pass the scene once it exists
        MainController controller = loader.getController();

        Scene scene = new Scene(root, 900, 600);

        // Attach keyboard listeners now that scene exists
        controller.attachKeyListeners(scene);

        primaryStage.setTitle("Office Navigator");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
