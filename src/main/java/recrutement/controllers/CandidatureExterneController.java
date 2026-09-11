package recrutement.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import recrutement.models.CandidatureExterne;
import recrutement.services.ServiceCandidatureExterne;
import utils.EventBus;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CandidatureExterneController implements Initializable {

    @FXML private Button           btnToggleSidebar;
    @FXML private Button           btnAjouter;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbFilterStatut;
    @FXML private ComboBox<String> cmbFilterPipeline;
    @FXML private VBox             cardsContainer;

    // Stats labels
    @FXML private Label statTotal;
    @FXML private Label statAttente;
    @FXML private Label statAcceptees;
    @FXML private Label statRefusees;
    @FXML private Label statEntretien;

    private ServiceCandidatureExterne serviceCand;
    private List<CandidatureExterne> allCandidatures;
    private List<CandidatureExterne> displayedList;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceCand = new ServiceCandidatureExterne();
        loadCandidatures();
        cmbFilterStatut.setItems(javafx.collections.FXCollections.observableArrayList(
                "Tous", "En attente", "Acceptée", "Refusée"));
        cmbFilterStatut.setValue("Tous");
        cmbFilterPipeline.setItems(javafx.collections.FXCollections.observableArrayList(
                "Tous", "Réception", "Entretien", "Test Technique", "Offre", "Présélection", "Finalisé"));
        cmbFilterPipeline.setValue("Tous");
    }

    private void loadCandidatures() {
        try {
            allCandidatures = serviceCand.getAll();
            displayedList = allCandidatures != null ? new ArrayList<>(allCandidatures) : new ArrayList<>();
            updateStats();
            refreshUI();
        } catch (Exception ex) { showError("Erreur chargement", ex.getMessage()); }
    }

    private void updateStats() {
        if (allCandidatures == null) return;
        long total     = allCandidatures.size();
        long attente   = allCandidatures.stream().filter(c -> "En attente".equals(c.getStatut())).count();
        long acceptees = allCandidatures.stream().filter(c -> "Acceptée".equals(c.getStatut())).count();
        long refusees  = allCandidatures.stream().filter(c -> "Refusée".equals(c.getStatut())).count();
        long entretien = allCandidatures.stream().filter(c -> "Entretien".equals(c.getEtapePipeline())).count();
        if (statTotal     != null) statTotal.setText(String.valueOf(total));
        if (statAttente   != null) statAttente.setText(String.valueOf(attente));
        if (statAcceptees != null) statAcceptees.setText(String.valueOf(acceptees));
        if (statRefusees  != null) statRefusees.setText(String.valueOf(refusees));
        if (statEntretien != null) statEntretien.setText(String.valueOf(entretien));
    }

    private void refreshUI() {
        cardsContainer.getChildren().clear();
        if (displayedList == null || displayedList.isEmpty()) {
            Label empty = new Label("Aucune candidature trouvée");
            empty.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8; -fx-padding:40;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (CandidatureExterne c : displayedList) addCandidatureCard(cardsContainer, c);
    }

    // ── Color maps ────────────────────────────────────────────────────────────
    private String statutBg(String s) {
        if (s == null) return "#F1F5F9";
        return switch (s) {
            case "Acceptée"  -> "#D1FAE5";
            case "Refusée"   -> "#FEE2E2";
            case "En attente"-> "#FEF3C7";
            default          -> "#F1F5F9";
        };
    }
    private String statutFg(String s) {
        if (s == null) return "#64748B";
        return switch (s) {
            case "Acceptée"  -> "#059669";
            case "Refusée"   -> "#DC2626";
            case "En attente"-> "#D97706";
            default          -> "#64748B";
        };
    }
    private String pipelineBg(String p) {
        if (p == null) return "#F1F5F9";
        return switch (p) {
            case "Entretien"     -> "#EFF6FF";
            case "Test Technique"-> "#F5F3FF";
            case "Offre"         -> "#ECFDF5";
            case "Finalisé"      -> "#D1FAE5";
            default              -> "#F1F5F9";
        };
    }
    private String pipelineFg(String p) {
        if (p == null) return "#64748B";
        return switch (p) {
            case "Entretien"     -> "#2563EB";
            case "Test Technique"-> "#7C3AED";
            case "Offre"         -> "#059669";
            case "Finalisé"      -> "#065F46";
            default              -> "#64748B";
        };
    }
    private String accentColor(String statut) {
        if (statut == null) return "#E2E8F0";
        return switch (statut) {
            case "Acceptée"  -> "linear-gradient(to bottom,#10B981,#34D399)";
            case "Refusée"   -> "linear-gradient(to bottom,#EF4444,#F87171)";
            case "En attente"-> "linear-gradient(to bottom,#F59E0B,#FBBF24)";
            default          -> "linear-gradient(to bottom,#4F46E5,#818CF8)";
        };
    }

    private void addCandidatureCard(VBox container, CandidatureExterne c) {
        String dateStr    = c.getDateDepot() != null ? SDF.format(c.getDateDepot()) : "—";
        String nomComplet = ((c.getPrenom() != null ? c.getPrenom() : "") + " " +
                (c.getNom()    != null ? c.getNom()    : "")).trim();
        String initiales  = nomComplet.isEmpty() ? "?" :
                Arrays.stream(nomComplet.split(" ")).limit(2)
                        .map(w -> String.valueOf(w.charAt(0)).toUpperCase())
                        .collect(Collectors.joining());

        // ── Outer card ───────────────────────────────────────────────────────
        HBox card = new HBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(130);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        // Accent strip left
        Region accent = new Region();
        accent.setPrefWidth(5); accent.setMinWidth(5); accent.setMaxWidth(5);
        accent.setStyle("-fx-background-color:" + accentColor(c.getStatut()) +
                "; -fx-background-radius:14 0 0 14;");

        // Avatar circle
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefWidth(72); avatarPane.setMinWidth(72);
        avatarPane.setAlignment(Pos.CENTER);
        Circle circle = new Circle(22);
        circle.setStyle("-fx-fill: linear-gradient(to bottom right,#4F46E5,#7C3AED);");
        Label initLbl = new Label(initiales);
        initLbl.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:white;");
        avatarPane.getChildren().addAll(circle, initLbl);
        HBox.setMargin(avatarPane, new Insets(0, 0, 0, 12));

        // Content
        VBox content = new VBox(0);
        content.setPadding(new Insets(14, 18, 12, 4));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: name + badges
        HBox row1 = new HBox(10); row1.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(nomComplet.isEmpty() ? "#" + c.getId() : nomComplet);
        lblName.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#0f172a;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statutBadge = buildBadge(c.getStatut() != null ? c.getStatut() : "—", statutBg(c.getStatut()), statutFg(c.getStatut()));
        Label pipeBadge   = buildBadge(c.getEtapePipeline() != null ? c.getEtapePipeline() : "—", pipelineBg(c.getEtapePipeline()), pipelineFg(c.getEtapePipeline()));
        row1.getChildren().addAll(lblName, spacer, pipeBadge, statutBadge);

        // Separator
        Region sep = new Region(); sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#F1F5F9;");
        VBox.setMargin(sep, new Insets(8, 0, 8, 0));

        // Row 2: meta info
        HBox row2 = new HBox(18); row2.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = buildMeta("📅", dateStr);
        Label pipeIcon = buildMeta("🎯", c.getEtapePipeline() != null ? c.getEtapePipeline() : "—");
        row2.getChildren().addAll(dateIcon, pipeIcon);

        // Row 3: actions
        HBox row3 = new HBox(8); row3.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(row3, new Insets(8, 0, 0, 0));
        Button btnEdit = buildBtn("✏  Modifier",  "#FEF3C7", "#D97706");
        Button btnDel  = buildBtn("🗑  Supprimer", "#FEE2E2", "#DC2626");
        btnEdit.setUserData(c); btnEdit.setOnAction(this::handleEdit);
        btnDel.setUserData(c);  btnDel.setOnAction(this::handleDelete);
        row3.getChildren().addAll(btnEdit, btnDel);

        content.getChildren().addAll(row1, sep, row2, row3);
        card.getChildren().addAll(accent, avatarPane, content);
        container.getChildren().add(card);
    }

    private Label buildBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:11px; -fx-font-weight:700;" +
                " -fx-background-radius:20; -fx-padding:4 12 4 12;");
        return l;
    }
    private Label buildMeta(String icon, String text) {
        Label l = new Label(icon + "  " + text);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        return l;
    }
    private Button buildBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:12px; -fx-font-weight:700;" +
                " -fx-background-radius:8; -fx-padding:6 16 6 16; -fx-cursor:hand;");
        return b;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setContentText(message); a.showAndWait();
    }

    @FXML public void handleAjouter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/recrutement/Formcandidatureexterne.fxml"));
            Parent root = loader.load();
            FormCandidatureExterneController fc = loader.getController();
            fc.setParentController(this); fc.setModeAjout();
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Ajouter une candidature externe");
            s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleEdit(ActionEvent event) {
        if (!(event.getSource() instanceof Button)) return;
        Object data = ((Button)event.getSource()).getUserData();
        if (!(data instanceof CandidatureExterne)) return;
        CandidatureExterne c = (CandidatureExterne)data;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/recrutement/Formcandidatureexterne.fxml"));
            Parent root = loader.load();
            FormCandidatureExterneController fc = loader.getController();
            fc.setParentController(this); fc.setModeEdition(c);
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier la candidature");
            s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleDelete(ActionEvent event) {
        if (!(event.getSource() instanceof Button)) return;
        Object data = ((Button)event.getSource()).getUserData();
        if (!(data instanceof CandidatureExterne)) return;
        CandidatureExterne c = (CandidatureExterne)data;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer"); confirm.setHeaderText("Supprimer cette candidature ?");
        confirm.setContentText((c.getPrenom() != null ? c.getPrenom() : "") + " " + (c.getNom() != null ? c.getNom() : ""));
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { try { serviceCand.delete(c); loadCandidatures(); } catch (Exception ex) { showError("Erreur", ex.getMessage()); } }
        });
    }
    @FXML public void handleSearch() {
        String q = txtSearch.getText().toLowerCase().trim();
        if (allCandidatures == null) displayedList = new ArrayList<>();
        else if (q.isEmpty()) displayedList = new ArrayList<>(allCandidatures);
        else displayedList = allCandidatures.stream()
                    .filter(c -> (c.getNom()    != null && c.getNom().toLowerCase().contains(q))
                            || (c.getPrenom() != null && c.getPrenom().toLowerCase().contains(q))
                            || String.valueOf(c.getId()).contains(q))
                    .collect(Collectors.toList());
        refreshUI();
    }
    @FXML public void handleFilterStatut(ActionEvent event) {
        String s = cmbFilterStatut.getValue();
        if (allCandidatures == null) { displayedList = new ArrayList<>(); refreshUI(); return; }
        displayedList = (s == null || "Tous".equals(s)) ? new ArrayList<>(allCandidatures)
                : allCandidatures.stream().filter(c -> s.equals(c.getStatut())).collect(Collectors.toList());
        refreshUI();
    }
    @FXML public void handleFilterPipeline(ActionEvent event) {
        String s = cmbFilterPipeline.getValue();
        if (allCandidatures == null) { displayedList = new ArrayList<>(); refreshUI(); return; }
        displayedList = (s == null || "Tous".equals(s)) ? new ArrayList<>(allCandidatures)
                : allCandidatures.stream().filter(c -> s.equals(c.getEtapePipeline())).collect(Collectors.toList());
        refreshUI();
    }
    public void hideFormAndRefresh() { loadCandidatures(); EventBus.fireCandidatureChanged(); }
}