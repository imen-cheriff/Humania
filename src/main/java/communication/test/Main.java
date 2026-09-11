package communication.test;

import communication.controllers.FeedController;
import communication.services.UserService;
import utils.UserSession;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/feed.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1400, 800);

        primaryStage.setTitle("HUMANIA - Feed");
        primaryStage.setScene(scene);

        UserService userService = new UserService();

        // ✅ Marque online au démarrage
        userService.setOnline(UserSession.getInstance().getUserId(), true);

        // ✅ Marque offline à la fermeture
        primaryStage.setOnCloseRequest(e -> {
            userService.setOnline(UserSession.getInstance().getUserId(), false);

            FeedController feedController = loader.getController();
            if (feedController != null && feedController.getNotificationPoller() != null) {
                feedController.getNotificationPoller().stop();
            }
            feedController.cleanupChat();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}