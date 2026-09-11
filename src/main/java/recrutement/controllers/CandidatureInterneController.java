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
import recrutement.models.CandidatureInterne;
import recrutement.services.ServiceCandidatureInterne;
import utils.EventBus;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CandidatureInterneController implements Initializable {

    @FXML private Button      btnToggleSidebar;
    @FXML private Button      btnAjouter;
    @FXML private TextField   txtSearch;
    @FXML private ComboBox<String> cmbFilter;
    @FXML private VBox        cardsContainer;

    @FXML private Label statTotal;
    @FXML private Label statAttente;
    @FXML private Label statAcceptees;
    @FXML private Label statRefusees;

    private ServiceCandidatureInterne serviceCand;
    private List<CandidatureInterne>  allCandidatures;
    private List<CandidatureInterne>  displayedList;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceCand = new ServiceCandidatureInterne();
        loadCandidatures();
        if (cmbFilter.getItems().isEmpty()) {
            cmbFilter.getItems().addAll("Tous", "En attente", "Acceptée", "Refusée");
            cmbFilter.setValue("Tous");
        }
    }

    private void loadCandidatures() {
        try {
            allCandidatures = serviceCand.getAll();
            displayedList = allCandidatures != null ? new ArrayList<>(allCandidatures) : new ArrayList<>();
            updateStats();
            refreshUI();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }

    private void updateStats() {
        if (allCandidatures == null) return;
        long total = allCandidatures.size();
        if (statTotal     != null) statTotal.setText(String.valueOf(total));
        // statAttente, statAcceptees, statRefusees require a statut field not present in CandidatureInterne
        if (statAttente   != null) statAttente.setText("—");
        if (statAcceptees != null) statAcceptees.setText("—");
        if (statRefusees  != null) statRefusees.setText("—");
    }

    private void refreshUI() {
        cardsContainer.getChildren().clear();
        if (displayedList == null || displayedList.isEmpty()) {
            Label empty = new Label("Aucune candidature interne trouvée");
            empty.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8; -fx-padding:40;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (CandidatureInterne c : displayedList) addCandidatureRow(cardsContainer, c);
    }

    private String accentGradient() {
        return "linear-gradient(to bottom,#7C3AED,#A78BFA)";
    }

    private void addCandidatureRow(VBox container, CandidatureInterne c) {
        String dateStr    = c.getDateDemande() != null ? SDF.format(c.getDateDemande()) : "—";
        String posteActuel = c.getPosteActuel()  != null ? c.getPosteActuel()  : "?";
        String nouveauPoste= c.getNouveauPoste() != null ? c.getNouveauPoste() : "?";
        String initiales = posteActuel.length() >= 2 ? posteActuel.substring(0,2).toUpperCase() : "PI";

        HBox card = new HBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(130);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        Region accent = new Region();
        accent.setPrefWidth(5); accent.setMinWidth(5); accent.setMaxWidth(5);
        accent.setStyle("-fx-background-color:" + accentGradient() + "; -fx-background-radius:14 0 0 14;");

        StackPane avatarPane = new StackPane();
        avatarPane.setPrefWidth(72); avatarPane.setMinWidth(72); avatarPane.setAlignment(Pos.CENTER);
        Circle circle = new Circle(22);
        circle.setStyle("-fx-fill: linear-gradient(to bottom right,#7C3AED,#A78BFA);");
        Label initLbl = new Label(initiales);
        initLbl.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:white;");
        avatarPane.getChildren().addAll(circle, initLbl);
        HBox.setMargin(avatarPane, new Insets(0, 0, 0, 12));

        VBox content = new VBox(0);
        content.setPadding(new Insets(14, 18, 12, 4));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: title + status badge
        HBox row1 = new HBox(10); row1.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label(posteActuel + "  →  " + nouveauPoste);
        lblTitle.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#0f172a;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label idBadge = new Label("#" + c.getId());
        idBadge.setStyle("-fx-background-color:#F1F5F9; -fx-text-fill:#64748b; -fx-font-size:11px;" +
                " -fx-background-radius:12; -fx-padding:3 10 3 10;");
        row1.getChildren().addAll(lblTitle, spacer, idBadge);

        Region sep = new Region(); sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#F1F5F9;");
        VBox.setMargin(sep, new Insets(8,0,8,0));

        // Row 2: meta
        HBox row2 = new HBox(18); row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().add(buildMeta("📅", dateStr));
        if (c.getMotif() != null && !c.getMotif().isBlank()) {
            String motif = c.getMotif().length() > 60 ? c.getMotif().substring(0,60) + "…" : c.getMotif();
            row2.getChildren().add(buildMeta("💬", motif));
        }

        // Row 3: actions
        HBox row3 = new HBox(8); row3.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(row3, new Insets(8,0,0,0));
        Button btnEdit = buildBtn("✏  Modifier", "#FEF3C7", "#D97706");
        Button btnDel  = buildBtn("🗑  Supprimer", "#FEE2E2", "#DC2626");
        btnEdit.setUserData(c); btnEdit.setOnAction(this::handleEdit);
        btnDel.setUserData(c);  btnDel.setOnAction(this::handleDelete);
        row3.getChildren().addAll(btnEdit, btnDel);

        content.getChildren().addAll(row1, sep, row2, row3);
        card.getChildren().addAll(accent, avatarPane, content);
        container.getChildren().add(card);
    }

    private Label buildBadge(String statut) {
        String bg, fg;
        switch (statut) {
            case "Acceptée":   bg="#D1FAE5"; fg="#059669"; break;
            case "Refusée":    bg="#FEE2E2"; fg="#DC2626"; break;
            case "En attente": bg="#FEF3C7"; fg="#D97706"; break;
            default:           bg="#F1F5F9"; fg="#64748B";
        }
        Label l = new Label(statut);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:11px; -fx-font-weight:700; -fx-background-radius:20; -fx-padding:4 12 4 12;");
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
                "; -fx-font-size:12px; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:6 16 6 16; -fx-cursor:hand;");
        return b;
    }
    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setContentText(message); a.showAndWait();
    }

    @FXML public void handleAjouter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/recrutement/Formcandidatureinterne.fxml"));
            Parent root = loader.load();
            FormCandidatureInterneController fc = loader.getController();
            fc.setParentController(this); fc.setModeAjout();
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Ajouter une candidature interne"); s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleEdit(ActionEvent event) {
        Object data = event.getSource() instanceof Button ? ((Button)event.getSource()).getUserData() : null;
        if (!(data instanceof CandidatureInterne)) return;
        CandidatureInterne c = (CandidatureInterne)data;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/recrutement/Formcandidatureinterne.fxml"));
            Parent root = loader.load();
            FormCandidatureInterneController fc = loader.getController();
            fc.setParentController(this); fc.setModeEdition(c);
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier la candidature"); s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleDelete(ActionEvent event) {
        Object data = event.getSource() instanceof Button ? ((Button)event.getSource()).getUserData() : null;
        if (!(data instanceof CandidatureInterne)) return;
        CandidatureInterne c = (CandidatureInterne)data;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer"); confirm.setHeaderText("Supprimer cette candidature interne ?");
        confirm.setContentText(c.getPosteActuel() + " → " + c.getNouveauPoste());
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
                    .filter(c -> (c.getPosteActuel()  != null && c.getPosteActuel().toLowerCase().contains(q))
                            || (c.getNouveauPoste() != null && c.getNouveauPoste().toLowerCase().contains(q))
                            || (c.getMotif()        != null && c.getMotif().toLowerCase().contains(q))
                            || String.valueOf(c.getId()).contains(q))
                    .collect(Collectors.toList());
        refreshUI();
    }
    @FXML public void handleFilter(ActionEvent event) {
        String s = cmbFilter.getValue();
        if (allCandidatures == null) { displayedList = new ArrayList<>(); refreshUI(); return; }
        // CandidatureInterne n'a pas de champ statut — filtre non applicable
        displayedList = new ArrayList<>(allCandidatures);
        refreshUI();
    }
    public void hideFormAndRefresh() { loadCandidatures(); EventBus.fireCandidatureChanged(); }
}