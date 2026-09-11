package planification.inside;

import com.officeapp.model.Floor;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * MainController
 * ──────────────
 * Owns the AnimationTimer game loop, keyboard input, movement physics,
 * elevator zone detection and floor-switching.
 *
 * Root layout is AnchorPane — layoutX/Y work correctly on all children,
 * so the popup centres precisely on the 900×600 scene.
 */
public class MainController implements Initializable {

    private static final double SCENE_W    = 900.0;
    private static final double SCENE_H    = 600.0;
    private static final double MOVE_SPEED = 180.0;
    private static final double CHAR_W     = 60.0;
    private static final double CHAR_H     = 90.0;
    private static final double POPUP_W    = 210.0;
    private static final double POPUP_H    = 240.0;

    @FXML private ImageView mapImageView;
    @FXML private Pane      elevatorHighlight;
    @FXML private ImageView characterView;
    @FXML private Pane      elevatorPopup;

    private final List<Floor>    floors      = new ArrayList<>();
    private final Set<KeyCode>   pressedKeys = new HashSet<>();
    private Floor   currentFloor;
    private double  charX, charY;
    private long    lastFrameTime  = -1;
    private boolean popupVisible   = false;
    private boolean insideElevator = false;

    private final AnimationTimer gameLoop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastFrameTime < 0) { lastFrameTime = now; return; }
            double dt = Math.min((now - lastFrameTime) / 1_000_000_000.0, 0.05);
            lastFrameTime = now;
            if (!popupVisible) {
                processMovement(dt);
                checkElevatorZone();
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildFloors();
        loadCharacterImage();
        buildElevatorPopupContent();
        goToFloor(floors.get(0));
        gameLoop.start();
    }

    // ── Floor definitions ─────────────────────────────────────────────────────

    private void buildFloors() {
        floors.add(new Floor("Floor 1",  "/images/floor1.png",
                new Rectangle2D(130, 440, 95, 85), new Point2D(177, 482)));
        floors.add(new Floor("Floor 2",  "/images/floor2.png",
                new Rectangle2D(365, 475, 95, 85), new Point2D(412, 517)));
        floors.add(new Floor("Rooftop",  "/images/rooftop.png",
                new Rectangle2D(285, 315, 95, 85), new Point2D(332, 357)));
    }

    // ── Character image ───────────────────────────────────────────────────────

    private void loadCharacterImage() {
        URL url = getClass().getResource("/images/employee.png");
        if (url != null) {
            characterView.setImage(new Image(url.toExternalForm()));
        }
        characterView.setFitWidth(CHAR_W);
        characterView.setFitHeight(CHAR_H);
        characterView.setPreserveRatio(true);
    }

    // ── Elevator popup ────────────────────────────────────────────────────────

    private void buildElevatorPopupContent() {
        Label title = new Label("\uD83D\uDED7   Select Floor");
        title.setStyle("-fx-text-fill: #cce0ff; -fx-font-size: 15px; "
                + "-fx-font-weight: bold; -fx-padding: 0 0 10 0;");

        VBox box = new VBox(9);
        box.setStyle("-fx-padding: 20 25 20 25; -fx-alignment: center;");
        box.getChildren().add(title);

        String[] labels  = {"Floor 1", "Floor 2", "Rooftop"};
        int[]    indices = {0, 1, 2};
        for (int i = 0; i < labels.length; i++) {
            final int idx = indices[i];
            Button btn = makeBtn(labels[i]);
            btn.setOnAction(e -> { hideElevatorPopup(); goToFloor(floors.get(idx)); });
            box.getChildren().add(btn);
        }

        Button closeBtn = new Button("\u2715  Stay here");
        closeBtn.setPrefWidth(160); closeBtn.setPrefHeight(30);
        closeBtn.setStyle("-fx-background-color: #2a2a3a; -fx-text-fill: #9aaacc; "
                + "-fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> hideElevatorPopup());
        box.getChildren().add(closeBtn);
        elevatorPopup.getChildren().add(box);
    }

    private Button makeBtn(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(160); btn.setPrefHeight(36);
        String base  = "-fx-background-color: #2a4a80; -fx-text-fill: white; "
                + "-fx-font-size: 13px; -fx-background-radius: 7; -fx-cursor: hand;";
        String hover = "-fx-background-color: #3a6abf; -fx-text-fill: white; "
                + "-fx-font-size: 13px; -fx-background-radius: 7; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    // ── Floor switching ───────────────────────────────────────────────────────

    private void goToFloor(Floor floor) {
        currentFloor = floor;
        URL imgUrl = getClass().getResource(floor.getImagePath());
        if (imgUrl != null) mapImageView.setImage(new Image(imgUrl.toExternalForm()));
        Rectangle2D ez = floor.getElevatorZone();
        elevatorHighlight.setLayoutX(ez.getMinX());
        elevatorHighlight.setLayoutY(ez.getMinY());
        elevatorHighlight.setPrefWidth(ez.getWidth());
        elevatorHighlight.setPrefHeight(ez.getHeight());
        elevatorHighlight.setVisible(true);
        charX = floor.getSpawnPoint().getX();
        charY = floor.getSpawnPoint().getY();
        applyCharacterPosition();
        insideElevator = false;
        lastFrameTime  = -1;
    }

    // ── Key listeners (called from MainApp) ───────────────────────────────────

    public void attachKeyListeners(javafx.scene.Scene scene) {
        scene.setOnKeyPressed(e  -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }

    // ── Movement ──────────────────────────────────────────────────────────────

    private void processMovement(double dt) {
        boolean left  = pressedKeys.contains(KeyCode.LEFT)  || pressedKeys.contains(KeyCode.A);
        boolean right = pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D);
        boolean up    = pressedKeys.contains(KeyCode.UP)    || pressedKeys.contains(KeyCode.W);
        boolean down  = pressedKeys.contains(KeyCode.DOWN)  || pressedKeys.contains(KeyCode.S);

        double dx = 0, dy = 0;
        if (left)  dx -= 1; if (right) dx += 1;
        if (up)    dy -= 1; if (down)  dy += 1;

        if (dx != 0 && dy != 0) { double l = Math.sqrt(2); dx /= l; dy /= l; }

        double hw = CHAR_W / 2.0, hh = CHAR_H / 2.0;
        charX = Math.max(hw, Math.min(SCENE_W - hw, charX + dx * MOVE_SPEED * dt));
        charY = Math.max(hh, Math.min(SCENE_H - hh, charY + dy * MOVE_SPEED * dt));
        applyCharacterPosition();
    }

    private void applyCharacterPosition() {
        characterView.setLayoutX(charX - CHAR_W / 2.0);
        characterView.setLayoutY(charY - CHAR_H / 2.0);
    }

    // ── Elevator detection ────────────────────────────────────────────────────

    private void checkElevatorZone() {
        if (currentFloor == null) return;
        Rectangle2D z = currentFloor.getElevatorZone();
        double cx = charX - CHAR_W / 2.0, cy = charY - CHAR_H / 2.0;
        boolean overlaps = cx < z.getMaxX() && cx + CHAR_W > z.getMinX()
                        && cy < z.getMaxY() && cy + CHAR_H > z.getMinY();
        if (overlaps && !insideElevator)  { insideElevator = true;  showElevatorPopup(); }
        else if (!overlaps)               { insideElevator = false; }
    }

    private void showElevatorPopup() {
        popupVisible = true;
        elevatorPopup.setLayoutX((SCENE_W - POPUP_W) / 2.0);
        elevatorPopup.setLayoutY((SCENE_H - POPUP_H) / 2.0);
        elevatorPopup.setVisible(true);
        elevatorPopup.toFront();
    }

    private void hideElevatorPopup() {
        popupVisible = false;
        elevatorPopup.setVisible(false);
        lastFrameTime = -1;
    }
}
