package planification.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import planification.models.Espace;
import planification.services.ServiceEspace;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GestionEspaCont implements Initializable {

    // ── FXML ──────────────────────────────────────────────
    @FXML private TilePane  cardsContainer;
    @FXML private TextField searchField;
    @FXML private StackPane centerStack;

    // ── Services & state ──────────────────────────────────
    private final ServiceEspace serviceEspace = new ServiceEspace();
    private List<Espace> allEspaces;

    // ── Card dimensions ───────────────────────────────────
    private static final double CARD_W  = 380;
    private static final double CARD_H  = 460;
    private static final double IMAGE_H = CARD_W * 9.0 / 16.0;   // ≈ 213.75

    // ── Init ──────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEspaces();
    }

    // ── Data ──────────────────────────────────────────────
    private void loadEspaces() {
        allEspaces = serviceEspace.getAll();
        filterAndDisplayEspaces();
    }

    private void filterAndDisplayEspaces() {
        String q = searchField != null ? searchField.getText().toLowerCase() : "";
        List<Espace> filtered = allEspaces.stream()
                .filter(e -> q.isEmpty()
                        || (e.getNom() != null && e.getNom().toLowerCase().contains(q))
                        || (e.getListeEquipements() != null &&
                        e.getListeEquipements().stream().anyMatch(eq -> eq.toLowerCase().contains(q)))
                        || (e.getTypeEspace() != null && e.getTypeEspace().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        cardsContainer.getChildren().clear();
        filtered.forEach(e -> cardsContainer.getChildren().add(buildCard(e)));
    }

    // ── Card builder ──────────────────────────────────────
    private VBox buildCard(Espace e) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.setPrefSize(CARD_W, CARD_H);
        card.setMinSize(CARD_W, CARD_H);
        card.setMaxSize(CARD_W, CARD_H);
        card.getStyleClass().add("space-card");

        // ── Image 16:9 ───────────────────────────────────
        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(IMAGE_H);
        imgBox.setMinHeight(IMAGE_H);
        imgBox.setMaxHeight(IMAGE_H);
        imgBox.getStyleClass().add("card-image-container");

        ImageView iv = new ImageView();
        iv.setFitWidth(CARD_W);
        iv.setFitHeight(IMAGE_H);
        iv.setPreserveRatio(false);
        String url = e.getUrlImage();
        if (url != null && !url.isBlank()) {
            try { iv.setImage(new Image(url, true)); } catch (Exception ignored) {}
        }

        // Badge disponibilité (haut droite)
        Label statusBadge = new Label(e.isDisponible() ? "● Disponible" : "● Occupé");
        statusBadge.getStyleClass().add(e.isDisponible() ? "badge-disponible" : "badge-occupe");
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBadge, new Insets(10, 10, 0, 0));

        // Badge type (haut gauche)
        Label typeBadge = new Label(e.isCoworking() ? "💺 Coworking" : "🏢 Réunion");
        typeBadge.getStyleClass().add(e.isCoworking() ? "badge-coworking" : "badge-reunion");
        StackPane.setAlignment(typeBadge, Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(10, 0, 0, 10));

        imgBox.getChildren().addAll(iv, typeBadge, statusBadge);

        // ── Body ─────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 16, 0, 16));
        body.getStyleClass().add("card-body");
        VBox.setVgrow(body, Priority.ALWAYS);

        Label title = new Label(e.getNom() != null ? e.getNom() : "");
        title.getStyleClass().add("card-title");
        title.setMaxWidth(CARD_W - 32);

        HBox floorRow = infoRow("🏢",
                e.getEtage() == 1 ? "1er étage" : e.getEtage() + "ème étage");
        HBox capRow = infoRow("👥", e.getCapacite() + " personnes");
        HBox typeRow = e.isCoworking()
                ? infoRow("💺", "Réservation à la chaise")
                : infoRow("🔒", "Réservation salle entière");

        // Equipment tags
        HBox tagsRow = new HBox(6);
        tagsRow.setAlignment(Pos.CENTER_LEFT);
        tagsRow.getStyleClass().add("equipments-row");
        tagsRow.getChildren().add(new Label("🔧"));
        if (e.getListeEquipements() != null) {
            String[] tagCls = {"tag-blue", "tag-purple", "tag-cyan", "tag-orange", "tag-green"};
            int i = 0;
            for (String eq : e.getListeEquipements()) {
                Label tag = new Label(eq);
                tag.getStyleClass().addAll("tag", tagCls[i % tagCls.length]);
                tagsRow.getChildren().add(tag);
                i++;
            }
        }

        body.getChildren().addAll(title, floorRow, capRow, typeRow, tagsRow);
        card.getChildren().addAll(imgBox, body);

        // ── Divider ──────────────────────────────────────
        Region div = new Region();
        div.getStyleClass().add("card-divider");
        VBox.setMargin(div, new Insets(12, 0, 0, 0));
        card.getChildren().add(div);

        // ── Actions ──────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(12, 16, 16, 16));

        Button editBtn = new Button("✏ Modifier");
        editBtn.getStyleClass().add("btn-modifier");
        editBtn.setOnAction(ev -> openForm(e));
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setMaxWidth(Double.MAX_VALUE);

        Button delBtn = new Button("🗑 Supprimer");
        delBtn.getStyleClass().add("btn-supprimer");
        delBtn.setOnAction(ev -> handleDeleteEspace(e));
        HBox.setHgrow(delBtn, Priority.ALWAYS);
        delBtn.setMaxWidth(Double.MAX_VALUE);

        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().add(actions);

        return card;
    }

    // ── Helper ───────────────────────────────────────────
    private HBox infoRow(String icon, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ic  = new Label(icon); ic.getStyleClass().add("info-icon");
        Label val = new Label(text); val.getStyleClass().add("info-value");
        row.getChildren().addAll(ic, val);
        return row;
    }

    // ── Handlers ─────────────────────────────────────────
    @FXML void handleAjouterEspace(ActionEvent event) { openForm(null); }
    @FXML void handleModifier(ActionEvent event) {}
    @FXML void handleSupprimer(ActionEvent event) {}
    @FXML void handleSearch(KeyEvent event) { filterAndDisplayEspaces(); }

    private void openForm(Espace existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/planification/AjouterEspace.fxml"));
            VBox root = loader.load();
            AjouterEspaceContr controller = loader.getController();
            if (existing != null) controller.setEspaceToEdit(existing);

            Stage stage = new Stage();
            stage.initOwner(cardsContainer.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(existing == null ? "Nouvelle salle" : "Modifier la salle");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadEspaces();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    private void handleDeleteEspace(Espace e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'espace ?");
        confirm.setContentText("Voulez-vous vraiment supprimer \""
                + (e.getNom() != null ? e.getNom() : "") + "\" ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                serviceEspace.delete(e);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Espace supprimé.");
                loadEspaces();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}