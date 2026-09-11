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
import recrutement.models.PosteInterne;
import recrutement.services.ServicePosteInterne;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PostInterneController implements Initializable {

    @FXML private Button           btnToggleSidebar;
    @FXML private Button           btnAjouter;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbFilter;
    @FXML private VBox             cardsContainer;

    @FXML private Label statTotal;
    @FXML private Label statInterne;
    @FXML private Label statExterne;
    @FXML private Label statRenfort;

    private ServicePosteInterne servicePoste;
    private List<PosteInterne> allPostes;
    private List<PosteInterne> displayedList;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        servicePoste = new ServicePosteInterne();
        loadPostes();
        cmbFilter.setItems(javafx.collections.FXCollections.observableArrayList(
                "Tous", "Mission interne", "Mission externe", "Renfort"));
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
        long total    = allPostes.size();
        long interne  = allPostes.stream().filter(p -> "Mission interne".equals(p.getTypePoste())).count();
        long externe  = allPostes.stream().filter(p -> "Mission externe".equals(p.getTypePoste())).count();
        long renfort  = allPostes.stream().filter(p -> "Renfort".equals(p.getTypePoste())).count();
        if (statTotal   != null) statTotal.setText(String.valueOf(total));
        if (statInterne != null) statInterne.setText(String.valueOf(interne));
        if (statExterne != null) statExterne.setText(String.valueOf(externe));
        if (statRenfort != null) statRenfort.setText(String.valueOf(renfort));
    }

    private void refreshUI() {
        cardsContainer.getChildren().clear();
        if (displayedList == null || displayedList.isEmpty()) {
            Label e = new Label("Aucun poste interne trouvé");
            e.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8; -fx-padding:40;");
            cardsContainer.getChildren().add(e);
            return;
        }
        for (PosteInterne p : displayedList) addPosteCard(cardsContainer, p);
    }

    private void addPosteCard(VBox container, PosteInterne p) {
        String d1 = p.getDateDebut() != null ? SDF.format(p.getDateDebut()) : "—";
        String d2 = p.getDateFin()   != null ? SDF.format(p.getDateFin())   : "—";

        String accentGrad, typeBg, typeFg, typeIcon;
        String type = p.getTypePoste() != null ? p.getTypePoste() : "—";
        switch (type) {
            case "Mission interne":
                accentGrad="linear-gradient(to bottom,#3B82F6,#60A5FA)"; typeBg="#EFF6FF"; typeFg="#2563EB"; typeIcon="🏠"; break;
            case "Mission externe":
                accentGrad="linear-gradient(to bottom,#8B5CF6,#A78BFA)"; typeBg="#F5F3FF"; typeFg="#7C3AED"; typeIcon="🌐"; break;
            case "Renfort":
                accentGrad="linear-gradient(to bottom,#F59E0B,#FBBF24)"; typeBg="#FFFBEB"; typeFg="#D97706"; typeIcon="💪"; break;
            default:
                accentGrad="linear-gradient(to bottom,#10B981,#34D399)"; typeBg="#ECFDF5"; typeFg="#059669"; typeIcon="📋";
        }

        HBox card = new HBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(130);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        Region accent = new Region();
        accent.setPrefWidth(5); accent.setMinWidth(5); accent.setMaxWidth(5);
        accent.setStyle("-fx-background-color:" + accentGrad + "; -fx-background-radius:14 0 0 14;");

        StackPane iconPane = new StackPane();
        iconPane.setPrefWidth(72); iconPane.setMinWidth(72); iconPane.setAlignment(Pos.CENTER);
        Rectangle iconBg = new Rectangle(44, 44);
        iconBg.setArcWidth(12); iconBg.setArcHeight(12);
        iconBg.setStyle("-fx-fill:" + typeBg + ";");
        Label iconLbl = new Label(typeIcon);
        iconLbl.setStyle("-fx-font-size:22px;");
        iconPane.getChildren().addAll(iconBg, iconLbl);
        HBox.setMargin(iconPane, new Insets(0, 0, 0, 12));

        VBox content = new VBox(0);
        content.setPadding(new Insets(14, 18, 12, 4));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1
        HBox row1 = new HBox(8); row1.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label(type);
        lblTitle.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#0f172a;");
        Label idBadge = new Label("#" + p.getId());
        idBadge.setStyle("-fx-background-color:#F1F5F9; -fx-text-fill:#64748b; -fx-font-size:11px; -fx-background-radius:12; -fx-padding:3 10 3 10;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label typeBadge = new Label(typeIcon + " " + type);
        typeBadge.setStyle("-fx-background-color:" + typeBg + "; -fx-text-fill:" + typeFg +
                "; -fx-font-size:11px; -fx-font-weight:700; -fx-background-radius:20; -fx-padding:4 12 4 12;");
        row1.getChildren().addAll(lblTitle, idBadge, spacer, typeBadge);

        Region sep = new Region(); sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#F1F5F9;");
        VBox.setMargin(sep, new Insets(8,0,8,0));

        // Row 2: meta
        HBox row2 = new HBox(18); row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().add(mkMeta("💰", "Rémun. " + p.getRemuneration()));
        row2.getChildren().add(mkMeta("📅", "Début : " + d1));
        if (!"—".equals(d2)) row2.getChildren().add(mkMeta("🏁", "Fin : " + d2));


        // Row 3: actions
        HBox row3 = new HBox(8); row3.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(row3, new Insets(8,0,0,0));
        Button btnEdit = mkBtn("✏  Modifier",  "#FEF3C7", "#D97706");
        Button btnDel  = mkBtn("🗑  Supprimer", "#FEE2E2", "#DC2626");
        btnEdit.setUserData(p); btnEdit.setOnAction(this::handleEdit);
        btnDel.setUserData(p);  btnDel.setOnAction(this::handleDelete);
        row3.getChildren().addAll(btnEdit, btnDel);

        content.getChildren().addAll(row1, sep, row2, row3);
        card.getChildren().addAll(accent, iconPane, content);
        container.getChildren().add(card);
    }

    private Label mkMeta(String icon, String text) {
        Label l = new Label(icon + "  " + text);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
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
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/recrutement/Formpostinterne.fxml"));
            Parent root = l.load(); FormPosteInterneController fc = l.getController();
            fc.setParentController(this); fc.setModeAjout();
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Ajouter un poste interne"); s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleVoir(ActionEvent e) {}
    @FXML public void handleEdit(ActionEvent event) {
        Object data = event.getSource() instanceof Button ? ((Button)event.getSource()).getUserData() : null;
        if (!(data instanceof PosteInterne)) return;
        PosteInterne p = (PosteInterne)data;
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/recrutement/Formpostinterne.fxml"));
            Parent root = l.load(); FormPosteInterneController fc = l.getController();
            fc.setParentController(this); fc.setModeEdition(p);
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier le poste"); s.setScene(new Scene(root)); s.showAndWait();
        } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
    }
    @FXML public void handleDelete(ActionEvent event) {
        Object data = event.getSource() instanceof Button ? ((Button)event.getSource()).getUserData() : null;
        if (!(data instanceof PosteInterne)) return;
        PosteInterne p = (PosteInterne)data;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer"); confirm.setHeaderText("Supprimer ce poste interne ?");
        confirm.setContentText(p.getTypePoste() != null ? p.getTypePoste() : "ID " + p.getId());
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
                    .filter(p -> (p.getTypePoste() != null && p.getTypePoste().toLowerCase().contains(q))
                            || String.valueOf(p.getId()).contains(q)
                            || String.valueOf(p.getRemuneration()).contains(q))
                    .collect(Collectors.toList());
        refreshUI();
    }
    @FXML public void handleFilter(ActionEvent event) {
        String s = cmbFilter.getValue();
        if (allPostes == null) { displayedList = new ArrayList<>(); refreshUI(); return; }
        if (s == null || "Tous".equals(s)) { displayedList = new ArrayList<>(allPostes); refreshUI(); return; }
        displayedList = allPostes.stream().filter(p -> s.equals(p.getTypePoste())).collect(Collectors.toList());
        refreshUI();
    }
    public void hideFormAndRefresh() { loadPostes(); }
}