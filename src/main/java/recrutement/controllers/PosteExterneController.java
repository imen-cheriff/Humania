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
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import recrutement.models.PosteExterne;
import recrutement.services.ServicePosteExterne;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PosteExterneController implements Initializable {

    @FXML private Button           btnToggleSidebar;
    @FXML private Button           btnAjouter;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbFilter;
    @FXML private VBox             cardsContainer;

    @FXML private Label statTotal;
    @FXML private Label statOuverts;
    @FXML private Label statFermes;
    @FXML private Label statHautePriorite;
    @FXML private Label statCDI;

    private ServicePosteExterne servicePoste;
    private List<PosteExterne> allPostes;
    private List<PosteExterne> displayedList;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    // true when logged-in user is EMPLOYE (read-only / apply mode)
    private boolean isEmploye;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        servicePoste = new ServicePosteExterne();

        // Detect role
        String role = utils.UserSession.getInstance().getRole();
        isEmploye = "EMPLOYE".equalsIgnoreCase(role);

        // Hide "Publier un poste" button for EMPLOYE
        if (isEmploye && btnAjouter != null) {
            btnAjouter.setVisible(false);
            btnAjouter.setManaged(false);
        }

        loadPostes();
        cmbFilter.setItems(javafx.collections.FXCollections.observableArrayList(
                "Tous", "Ouvert", "Fermé", "CDI", "CDD", "Stagiaire", "Haute", "Moyenne", "Faible"));
        cmbFilter.setValue("Tous");
    }

    private void loadPostes() {
        try {
            allPostes = servicePoste.getAll();
            displayedList = allPostes != null ? new ArrayList<>(allPostes) : new ArrayList<>();
            updateStats();
            refreshUI();
        } catch (Exception ex) { showError("Erreur chargement", ex.getMessage()); }
    }

    private void updateStats() {
        if (allPostes == null) return;
        long total   = allPostes.size();
        long ouverts = allPostes.stream().filter(p -> "Ouvert".equals(p.getStatut())).count();
        long fermes  = allPostes.stream().filter(p -> "Fermé".equals(p.getStatut())).count();
        long haute   = allPostes.stream().filter(p -> p.getPriorite() == 1).count();
        long cdi     = allPostes.stream().filter(p -> "CDI".equals(p.getTypeContrat())).count();
        if (statTotal         != null) statTotal.setText(String.valueOf(total));
        if (statOuverts       != null) statOuverts.setText(String.valueOf(ouverts));
        if (statFermes        != null) statFermes.setText(String.valueOf(fermes));
        if (statHautePriorite != null) statHautePriorite.setText(String.valueOf(haute));
        if (statCDI           != null) statCDI.setText(String.valueOf(cdi));
    }

    private void refreshUI() {
        cardsContainer.getChildren().clear();
        if (displayedList == null || displayedList.isEmpty()) {
            Label e = new Label("Aucun poste externe trouvé");
            e.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8; -fx-padding:40;");
            cardsContainer.getChildren().add(e);
            return;
        }
        for (PosteExterne p : displayedList) addPosteCard(cardsContainer, p);
    }

    private void addPosteCard(VBox container, PosteExterne p) {
        String dateStr = p.getDatePublication() != null ? SDF.format(p.getDatePublication()) : "—";

        // Priorité badge config
        String prioriteLabel, prioriteBg, prioriteFg, accentGrad;
        if (p.getPriorite() == 1) {
            prioriteLabel = "🔥 Haute";  prioriteBg = "#FEE2E2"; prioriteFg = "#DC2626";
            accentGrad = "linear-gradient(to bottom,#EF4444,#F87171)";
        } else if (p.getPriorite() == 3) {
            prioriteLabel = "📘 Faible"; prioriteBg = "#DBEAFE"; prioriteFg = "#2563EB";
            accentGrad = "linear-gradient(to bottom,#3B82F6,#60A5FA)";
        } else {
            prioriteLabel = "🟡 Moyenne"; prioriteBg = "#FEF3C7"; prioriteFg = "#D97706";
            accentGrad = "linear-gradient(to bottom,#F59E0B,#FBBF24)";
        }

        // Statut badge
        String statutBg = "Ouvert".equals(p.getStatut()) ? "#D1FAE5" : "#F1F5F9";
        String statutFg = "Ouvert".equals(p.getStatut()) ? "#059669" : "#6B7280";
        String statutIcon = "Ouvert".equals(p.getStatut()) ? "✅" : "🔒";

        // Contrat badge
        String contratBg = "CDI".equals(p.getTypeContrat()) ? "#EDE9FE" : "#F0F9FF";
        String contratFg = "CDI".equals(p.getTypeContrat()) ? "#7C3AED" : "#0EA5E9";

        HBox card = new HBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(140);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        Region accent = new Region();
        accent.setPrefWidth(5); accent.setMinWidth(5); accent.setMaxWidth(5);
        accent.setStyle("-fx-background-color:" + accentGrad + "; -fx-background-radius:14 0 0 14;");

        // Icon block
        StackPane iconPane = new StackPane();
        iconPane.setPrefWidth(72); iconPane.setMinWidth(72); iconPane.setAlignment(Pos.CENTER);
        Rectangle iconBg = new Rectangle(44, 44);
        iconBg.setArcWidth(12); iconBg.setArcHeight(12);
        iconBg.setStyle("-fx-fill: linear-gradient(to bottom right,#0EA5E9,#38BDF8);");
        Label iconLbl = new Label("💼");
        iconLbl.setStyle("-fx-font-size:20px;");
        iconPane.getChildren().addAll(iconBg, iconLbl);
        HBox.setMargin(iconPane, new Insets(0, 0, 0, 12));

        VBox content = new VBox(0);
        content.setPadding(new Insets(14, 18, 12, 4));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: title + badges
        HBox row1 = new HBox(8); row1.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label(p.getTitre() != null ? p.getTitre() : "#" + p.getId());
        lblTitle.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#0f172a;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        row1.getChildren().addAll(lblTitle, spacer,
                mkBadge(statutIcon + " " + (p.getStatut() != null ? p.getStatut() : "—"), statutBg, statutFg),
                mkBadge(prioriteLabel, prioriteBg, prioriteFg));

        Region sep = new Region(); sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#F1F5F9;");
        VBox.setMargin(sep, new Insets(8,0,8,0));

        // Row 2: description + meta
        HBox row2 = new HBox(18); row2.setAlignment(Pos.CENTER_LEFT);
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            String desc = p.getDescription().length() > 70 ? p.getDescription().substring(0,70) + "…" : p.getDescription();
            Label lblDesc = new Label(desc);
            lblDesc.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
            row2.getChildren().add(lblDesc);
        }

        // Row 3: tags + actions
        HBox row3 = new HBox(8); row3.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(row3, new Insets(8,0,0,0));
        row3.getChildren().add(mkBadge(p.getTypeContrat() != null ? p.getTypeContrat() : "—", contratBg, contratFg));
        row3.getChildren().add(mkBadge("📅 " + dateStr, "#F8FAFC", "#64748B"));


        Region btnSpacer = new Region(); HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        row3.getChildren().add(btnSpacer);

        if (isEmploye) {
            // EMPLOYE: single "Postuler" button
            Button btnPostuler = mkBtn("📩  Postuler", "#E0F2FE", "#0369A1");
            btnPostuler.setUserData(p);
            btnPostuler.setOnAction(this::handlePostuler);
            row3.getChildren().add(btnPostuler);
        } else {
            // ADMIN / RH: Modifier + Supprimer
            Button btnEdit = mkBtn("✏  Modifier",  "#FEF3C7", "#D97706");
            Button btnDel  = mkBtn("🗑  Supprimer", "#FEE2E2", "#DC2626");
            btnEdit.setUserData(p); btnEdit.setOnAction(this::handleEdit);
            btnDel.setUserData(p);  btnDel.setOnAction(this::handleDelete);
            row3.getChildren().addAll(btnEdit, btnDel);
        }

        content.getChildren().addAll(row1, sep, row2, row3);
        card.getChildren().addAll(accent, iconPane, content);
        container.getChildren().add(card);
    }

    private Label mkBadge(String t, String bg, String fg) {
        Label l = new Label(t);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:11px; -fx-font-weight:700; -fx-background-radius:20; -fx-padding:4 12 4 12;");
        return l;
    }
    private Button mkBtn(String t, String bg, String fg) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:12px; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:6 16 6 16; -fx-cursor:hand;");
        return b;
    }
    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setContentText(m); a.showAndWait();
    }

    @FXML public void handleAjouter(ActionEvent e) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/recrutement/Formposteexterne.fxml"));
            Parent root = l.load(); FormPosteExterneController fc = l.getController();
            fc.setParentController(this); fc.setModeAjout();
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Ajouter un poste externe"); s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleVoir(ActionEvent e) {}
    @FXML public void handleEdit(ActionEvent event) {
        Object data = event.getSource() instanceof Button ? ((Button)event.getSource()).getUserData() : null;
        if (!(data instanceof PosteExterne)) return;
        PosteExterne p = (PosteExterne)data;
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/recrutement/Formposteexterne.fxml"));
            Parent root = l.load(); FormPosteExterneController fc = l.getController();
            fc.setParentController(this); fc.setModeEdition(p);
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier le poste"); s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleDelete(ActionEvent event) {
        Object data = event.getSource() instanceof Button ? ((Button)event.getSource()).getUserData() : null;
        if (!(data instanceof PosteExterne)) return;
        PosteExterne p = (PosteExterne)data;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer"); confirm.setHeaderText("Supprimer ce poste ?");
        confirm.setContentText(p.getTitre() != null ? p.getTitre() : "ID " + p.getId());
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { try { servicePoste.delete(p); loadPostes(); } catch (Exception ex) { showError("Erreur", ex.getMessage()); } }
        });
    }
    @FXML public void handleSearch() {
        String q = txtSearch.getText().toLowerCase().trim();
        if (allPostes == null) displayedList = new ArrayList<>();
        else if (q.isEmpty()) displayedList = new ArrayList<>(allPostes);
        else displayedList = allPostes.stream()
                    .filter(p -> (p.getTitre() != null && p.getTitre().toLowerCase().contains(q))
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                            || (p.getTypeContrat() != null && p.getTypeContrat().toLowerCase().contains(q))
                            || String.valueOf(p.getId()).contains(q))
                    .collect(Collectors.toList());
        refreshUI();
    }
    @FXML public void handleFilter(ActionEvent event) {
        String s = cmbFilter.getValue();
        if (allPostes == null) { displayedList = new ArrayList<>(); refreshUI(); return; }
        if (s == null || "Tous".equals(s)) { displayedList = new ArrayList<>(allPostes); refreshUI(); return; }
        if ("Ouvert".equals(s) || "Fermé".equals(s)) {
            displayedList = allPostes.stream().filter(p -> s.equals(p.getStatut())).collect(Collectors.toList());
        } else if ("CDI".equals(s) || "CDD".equals(s) || "Stagiaire".equals(s)) {
            displayedList = allPostes.stream().filter(p -> s.equals(p.getTypeContrat())).collect(Collectors.toList());
        } else {
            int cible = "Haute".equals(s) ? 1 : "Faible".equals(s) ? 3 : 2;
            displayedList = allPostes.stream().filter(p -> p.getPriorite() == cible).collect(Collectors.toList());
        }
        refreshUI();
    }
    @FXML public void handlePostuler(ActionEvent event) {
        Object data = event.getSource() instanceof Button ? ((Button) event.getSource()).getUserData() : null;
        PosteExterne p = (data instanceof PosteExterne) ? (PosteExterne) data : null;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/recrutement/FormPostuler.fxml"));
            Parent root = loader.load();
            FormPostulerController fc = loader.getController();
            if (p != null) fc.setPoste(p);
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Postuler — " + (p != null && p.getTitre() != null ? p.getTitre() : "Poste externe"));
            s.setScene(new Scene(root));
            s.setResizable(false);
            s.showAndWait();
        } catch (Exception ex) {
            showError("Erreur", "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    public void hideFormAndRefresh() { loadPostes(); }
}