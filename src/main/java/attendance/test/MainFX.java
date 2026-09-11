
package attendance.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🚀 Lancement de Humania...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardView.fxml"));
            Scene scene = new Scene(loader.load());

            primaryStage.setTitle("Humania - Gestion RH");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            System.out.println("✅ Application lancée !");

        } catch (Exception e) {
            System.err.println("❌ Erreur :");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}