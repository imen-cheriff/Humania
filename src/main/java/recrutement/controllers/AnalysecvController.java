package recrutement.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import recrutement.services.Cvanalysisservice;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Controller pour la page Analyse RH (PAGE_ANALYSE_RH).
 * Intègre l'analyse CV n8n directement dans le layout Humania principal.
 *
 * FXML : /views/recrutement/AnalyseRH.fxml
 */
public class AnalysecvController implements Initializable {

    // ── FXML bindings ──────────────────────────────────────────────────────
    @FXML private Label         lblFileName;
    @FXML private Button        btnChoisirCV;
    @FXML private Button        btnAnalyser;
    @FXML private ProgressIndicator spinner;
    @FXML private Label         lblStatus;

    // Résultats
    @FXML private VBox          resultPane;
    @FXML private Label         lblScoreValue;
    @FXML private Label         lblScoreDesc;
    @FXML private Canvas        radarCanvas;
    @FXML private TextArea      txtAssessment;

    // ── State ──────────────────────────────────────────────────────────────
    private File selectedFile;
    private final Cvanalysisservice analysisService = new Cvanalysisservice();

    private static final String[] AXES = {
            "Compétences\nTechniques",
            "Expérience",
            "Adéquation\nPoste",
            "Localisation",
            "Potentiel\nÉvolution"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Résultats cachés jusqu'à une analyse
        if (resultPane != null) {
            resultPane.setVisible(false);
            resultPane.setManaged(false);
        }
        if (spinner != null) spinner.setVisible(false);
        if (btnAnalyser != null) btnAnalyser.setDisable(true);
        if (lblStatus != null) lblStatus.setText("");
    }

    // ── Choisir CV ─────────────────────────────────────────────────────────
    @FXML
    public void handleChoisirCV(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un CV (PDF)");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File f = fc.showOpenDialog(btnChoisirCV.getScene().getWindow());
        if (f != null) {
            selectedFile = f;
            lblFileName.setText(f.getName());
            lblFileName.setStyle("-fx-text-fill: #1a2540; -fx-font-weight: bold;");
            btnAnalyser.setDisable(false);
            resultPane.setVisible(false);
            resultPane.setManaged(false);
            lblStatus.setText("");
        }
    }

    // ── Lancer l'analyse ───────────────────────────────────────────────────
    @FXML
    public void handleAnalyser(ActionEvent event) {
        if (selectedFile == null) return;
        setLoading(true);
        resultPane.setVisible(false);
        resultPane.setManaged(false);

        CompletableFuture.supplyAsync(() -> {
            try {
                return analysisService.analyze(selectedFile);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }).whenComplete((result, err) -> Platform.runLater(() -> {
            setLoading(false);
            if (err != null) {
                lblStatus.setText("❌ Erreur : " + err.getCause().getMessage());
                lblStatus.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                afficherResultat(result);
            }
        }));
    }

    // ── Afficher les résultats ─────────────────────────────────────────────
    private void afficherResultat(Cvanalysisservice.AnalysisResult result) {
        // Score badge
        String couleur = result.vote >= 7 ? "#27ae60" : result.vote >= 4 ? "#f39c12" : "#e74c3c";
        lblScoreValue.setText(String.format("%.0f / 10", result.vote));
        lblScoreValue.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");

        String mention = result.vote >= 8 ? "Excellent profil ✅"
                : result.vote >= 6 ? "Bon profil 👍"
                : result.vote >= 4 ? "Profil moyen ⚠️"
                :                    "Profil insuffisant ❌";
        lblScoreDesc.setText(mention);

        // Assessment text
        txtAssessment.setText(result.consideration);

        // Spider chart
        drawRadarChart(radarCanvas, result.axisScores);

        // Afficher le panneau résultats
        resultPane.setVisible(true);
        resultPane.setManaged(true);

        lblStatus.setText("✅ Analyse terminée avec succès");
        lblStatus.setStyle("-fx-text-fill: #27ae60;");
    }

    // ── Spider / Radar chart ───────────────────────────────────────────────
    private void drawRadarChart(Canvas canvas, double[] scores) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        int n = AXES.length;
        double cx = w / 2;
        double cy = h / 2 + 10;
        double maxR = Math.min(w, h) * 0.34;
        int gridLevels = 5;

        // Grille
        for (int lvl = 1; lvl <= gridLevels; lvl++) {
            double r = maxR * lvl / gridLevels;
            gc.setStroke(Color.web("#dce1e7")); gc.setLineWidth(1);
            double[] px = new double[n], py = new double[n];
            for (int i = 0; i < n; i++) {
                double angle = Math.PI * 2 * i / n - Math.PI / 2;
                px[i] = cx + r * Math.cos(angle);
                py[i] = cy + r * Math.sin(angle);
            }
            gc.strokePolygon(px, py, n);
            // Label valeur grille
            gc.setFill(Color.web("#94a3b8"));
            gc.setFont(Font.font("System", 9));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.valueOf(lvl * 2), cx + 4, cy - r + 4);
        }

        // Axes
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n - Math.PI / 2;
            gc.setStroke(Color.web("#bdc3c7")); gc.setLineWidth(1);
            gc.strokeLine(cx, cy, cx + maxR * Math.cos(angle), cy + maxR * Math.sin(angle));
        }

        // Polygone données
        double[] dpx = new double[n], dpy = new double[n];
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n - Math.PI / 2;
            double r = maxR * (scores[i] / 10.0);
            dpx[i] = cx + r * Math.cos(angle);
            dpy[i] = cy + r * Math.sin(angle);
        }
        gc.setFill(Color.web("#5b8dee", 0.25));
        gc.fillPolygon(dpx, dpy, n);
        gc.setStroke(Color.web("#5b8dee")); gc.setLineWidth(2.5);
        gc.strokePolygon(dpx, dpy, n);

        // Points
        for (int i = 0; i < n; i++) {
            gc.setFill(Color.web("#5b8dee")); gc.fillOval(dpx[i]-5, dpy[i]-5, 10, 10);
            gc.setFill(Color.WHITE);           gc.fillOval(dpx[i]-2, dpy[i]-2, 4, 4);
        }

        // Labels axes
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n - Math.PI / 2;
            double labelR = maxR + 30;
            double lx = cx + labelR * Math.cos(angle);
            double ly = cy + labelR * Math.sin(angle);
            gc.setFill(Color.web("#1a2540"));
            // Multi-line label
            String[] lines = AXES[i].split("\n");
            for (int l = 0; l < lines.length; l++)
                gc.fillText(lines[l], lx, ly + l * 13);

            // Score value
            gc.setFill(Color.web("#e57373"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            double vr = maxR * (scores[i] / 10.0) + 14;
            gc.fillText(String.format("%.1f", scores[i]),
                    cx + vr * Math.cos(angle), cy + vr * Math.sin(angle) + 4);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        }

        // Centre
        gc.setFill(Color.web("#5b8dee", 0.5));
        gc.fillOval(cx-3, cy-3, 6, 6);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void setLoading(boolean loading) {
        spinner.setVisible(loading);
        btnAnalyser.setDisable(loading);
        btnChoisirCV.setDisable(loading);
        lblStatus.setText(loading ? "⏳ Analyse en cours… veuillez patienter." : "");
        lblStatus.setStyle("-fx-text-fill: #7f8c8d;");
    }
}