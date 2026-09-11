package utilisateur.controllers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.opencv.core.Mat;
import utilisateur.services.FaceRecognitionService;

/**
 * Reusable live-camera dialog used by:
 *   - ProfilController  → enroll face
 *   - LoginController   → recognize face
 *
 * Runs the camera preview on a daemon thread.
 * The caller receives either onSuccess(faceData) or onCancelled().
 */
public class FaceCaptureDialog {

    public interface CaptureCallback {
        /** Called on the JavaFX thread with the serialized histogram bytes. */
        void onSuccess(byte[] faceData, FaceRecognitionService faceService);
        /** Called when cancelled or if no face detected. msg may be null. */
        void onCancelled(String msg);
    }

    public enum Mode { ENROLL, LOGIN }

    private final Window          owner;
    private final Mode            mode;
    private final CaptureCallback callback;

    private final FaceRecognitionService faceService = new FaceRecognitionService();
    private volatile boolean cameraRunning = false;
    private Thread           previewThread;
    private Stage            dialog;

    // UI refs needed across methods
    private Label       statusLabel;
    private Button      captureBtn;
    private ProgressBar progressBar;

    public FaceCaptureDialog(Window owner, Mode mode, CaptureCallback callback) {
        this.owner    = owner;
        this.mode     = mode;
        this.callback = callback;
    }

    // ── Public entry point ────────────────────────────────────────────────────

    public void show() {
        // Init OpenCV off the UI thread
        new Thread(() -> {
            try {
                faceService.init();
                Platform.runLater(this::buildDialog);
            } catch (Exception e) {
                Platform.runLater(() ->
                        callback.onCancelled("Erreur initialisation OpenCV: " + e.getMessage()));
            }
        }).start();
    }

    // ── Build dialog ──────────────────────────────────────────────────────────

    private void buildDialog() {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(mode == Mode.ENROLL
                ? "Enregistrer mon visage"
                : "Connexion par reconnaissance faciale");
        dialog.setResizable(false);

        // Camera preview
        ImageView preview = new ImageView();
        preview.setFitWidth(440);
        preview.setFitHeight(330);
        preview.setPreserveRatio(true);
        preview.setStyle(
                "-fx-background-color: #0f172a;" +
                        "-fx-border-color: #334155; -fx-border-radius: 10;");

        // Instruction label
        String instruction = mode == Mode.ENROLL
                ? "Regardez la camera puis cliquez Capturer"
                : "Regardez la camera puis cliquez Se connecter";
        Label instrLabel = new Label("👁  " + instruction);
        instrLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Status label
        statusLabel = new Label("Demarrage de la camera...");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        // Progress bar (shown during multi-sample capture)
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(440);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: #7c3aed;");

        // Capture button
        String captureText = mode == Mode.ENROLL ? "📸  Capturer mon visage" : "🔍  Se connecter";
        captureBtn = new Button(captureText);
        captureBtn.setDisable(true);
        captureBtn.setPrefWidth(220);
        captureBtn.setPrefHeight(44);
        captureBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea, #764ba2);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-font-size: 14px; -fx-cursor: hand;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setPrefHeight(44);
        cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;" +
                        "-fx-text-fill: #718096; -fx-font-size: 13px; -fx-cursor: hand;");

        HBox btnRow = new HBox(12, captureBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER);

        VBox root = new VBox(12,
                preview, instrLabel, progressBar, statusLabel, btnRow);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: white;");

        dialog.setScene(new Scene(root, 500, 500));

        // ── Start camera ──────────────────────────────────────────────────────
        if (!faceService.startCamera()) {
            setStatus("❌ Camera introuvable. Verifiez votre webcam.", "#e53e3e");
        } else {
            cameraRunning = true;
            captureBtn.setDisable(false);
            setStatus("✅ Camera active — cadrez votre visage dans le rectangle vert.", "#38a169");
            startPreviewThread(preview);
        }

        captureBtn.setOnAction(e -> onCapture());
        cancelBtn.setOnAction(e -> closeAndCancel(null));
        dialog.setOnCloseRequest(e -> shutdown());

        dialog.showAndWait();
    }

    // ── Preview thread ────────────────────────────────────────────────────────

    private void startPreviewThread(ImageView preview) {
        previewThread = new Thread(() -> {
            while (cameraRunning) {
                Mat frame = faceService.captureRawFrame();
                if (frame != null && !frame.empty()) {
                    Mat annotated = faceService.drawFaceRectangles(frame);
                    javafx.scene.image.Image img = faceService.matToImage(annotated);
                    if (img != null) Platform.runLater(() -> preview.setImage(img));
                }
                try { Thread.sleep(33); } catch (InterruptedException e) { break; }
            }
        });
        previewThread.setDaemon(true);
        previewThread.start();
    }

    // ── Capture ───────────────────────────────────────────────────────────────

    private void onCapture() {
        captureBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        if (mode == Mode.ENROLL) {
            setStatus("⏳ Capture de plusieurs echantillons pour plus de precision...", "#f6ad55");
        } else {
            setStatus("⏳ Analyse du visage en cours...", "#f6ad55");
        }

        new Thread(() -> {
            try {
                byte[] faceData;

                if (mode == Mode.ENROLL) {
                    // Average over 5 samples — much more robust enrollment
                    faceData = faceService.captureAverageHistogram(5);
                } else {
                    // Login: single best capture (retrying up to 5 frames)
                    faceData = captureSingleBest();
                }

                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    shutdown();
                    dialog.close();

                    if (faceData != null) {
                        callback.onSuccess(faceData, faceService);
                    } else {
                        callback.onCancelled(
                                "Aucun visage detecte.\n\n" +
                                        "Conseils:\n" +
                                        "  • Assurez-vous d'etre bien eclaire\n" +
                                        "  • Regardez directement la camera\n" +
                                        "  • Rapprochez-vous si necessaire");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    shutdown();
                    dialog.close();
                    callback.onCancelled("Erreur: " + ex.getMessage());
                });
            }
        }).start();
    }

    private byte[] captureSingleBest() throws InterruptedException {
        for (int attempt = 0; attempt < 8; attempt++) {
            Thread.sleep(100);
            Mat frame = faceService.captureRawFrame();
            if (frame == null) continue;
            Mat face = faceService.detectAndExtractFace(frame);
            if (face != null) {
                return faceService.serializeHistogram(faceService.computeHistogram(face));
            }
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void shutdown() {
        cameraRunning = false;
        if (previewThread != null) previewThread.interrupt();
        faceService.stopCamera();
    }

    private void closeAndCancel(String msg) {
        shutdown();
        dialog.close();
        callback.onCancelled(msg);
    }

    private void setStatus(String text, String colorHex) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + colorHex + ";");
    }
}