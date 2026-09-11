package planification.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import planification.models.Evenement;
import planification.services.ServiceEvenement;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GestionEventCont implements Initializable {

    // ── FXML ──────────────────────────────────────────────
    @FXML private GridPane  cardsContainer;
    @FXML private TextField searchField;
    @FXML private StackPane centerStack;

    // ── Services & state ──────────────────────────────────
    private final ServiceEvenement serviceEvenement = new ServiceEvenement();
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");
    private List<Evenement> allEvents;

    private static final int COLS = 3;

    // ── Init ──────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEvents();
    }

    // ── Data ──────────────────────────────────────────────
    private void loadEvents() {
        allEvents = serviceEvenement.getAll();
        filterAndDisplayEvents();
    }

    private void filterAndDisplayEvents() {
        String q = searchField != null ? searchField.getText().toLowerCase() : "";

        List<Evenement> filtered = allEvents.stream()
                .filter(ev -> q.isEmpty()
                        || (ev.getTitre()       != null && ev.getTitre().toLowerCase().contains(q))
                        || (ev.getLieu()        != null && ev.getLieu().toLowerCase().contains(q))
                        || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        cardsContainer.getChildren().clear();

        for (int i = 0; i < filtered.size(); i++) {
            VBox card = new EventCard(filtered.get(i)).build();
            GridPane.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            cardsContainer.add(card, i % COLS, i / COLS);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  EventCard — New flat white design (ref: Advanced React Workshop)
    // ═══════════════════════════════════════════════════════
    private class EventCard {

        private final Evenement ev;

        EventCard(Evenement ev) {
            this.ev = ev;
        }

        VBox build() {
            VBox card = new VBox();
            card.setSpacing(0);
            card.setMinHeight(420);
            card.setPrefHeight(420);
            card.setMaxHeight(420);
            card.getStyleClass().add("event-card");

            // ── Status ───────────────────────────────────
            Date now = new Date();
            String badgeClass, badgeText;

            if (ev.getDateHeureFin() != null && ev.getDateHeureFin().before(now)) {
                badgeClass = "badge-termine"; badgeText = "Terminé";
            } else if (ev.getDateHeureDebut() != null && ev.getDateHeureDebut().before(now)
                    && ev.getDateHeureFin() != null && ev.getDateHeureFin().after(now)) {
                badgeClass = "badge-encours"; badgeText = "En cours";
            } else {
                badgeClass = "badge-avenir"; badgeText = "À venir";
            }

            // ── Header: title + badge ─────────────────────
            StackPane header = new StackPane();
            header.getStyleClass().add("card-header");
            header.setPadding(new Insets(18, 16, 6, 16));

            VBox titleBox = new VBox(6);
            titleBox.setAlignment(Pos.TOP_LEFT);

            Label titleLabel = new Label(ev.getTitre() != null ? ev.getTitre() : "");
            titleLabel.getStyleClass().add("card-title");
            titleLabel.setWrapText(true);
            titleLabel.setMaxWidth(280);

            String desc = ev.getDescription();
            if (desc != null && desc.length() > 70) desc = desc.substring(0, 67) + "…";
            Label descLabel = new Label(desc != null ? desc : "");
            descLabel.getStyleClass().add("card-description");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(280);

            // Category tag (type/lieu as category placeholder)
            Label catTag = new Label("📌 Événement");
            catTag.getStyleClass().add("tag-category");

            titleBox.getChildren().addAll(titleLabel, descLabel, catTag);

            Label badgeLabel = new Label(badgeText);
            badgeLabel.getStyleClass().addAll("badge", badgeClass);
            StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
            StackPane.setMargin(badgeLabel, new Insets(0, 0, 0, 0));
            StackPane.setAlignment(titleBox, Pos.TOP_LEFT);

            header.getChildren().addAll(titleBox, badgeLabel);
            card.getChildren().add(header);

            // ── Body ─────────────────────────────────────
            VBox body = new VBox(8);
            body.getStyleClass().add("card-body");
            body.setPadding(new Insets(10, 16, 0, 16));
            VBox.setVgrow(body, Priority.ALWAYS);

            String dateStr  = fmt(ev.getDateEvenement(),  DATE_FMT);
            String debutStr = fmt(ev.getDateHeureDebut(), TIME_FMT);
            String finStr   = fmt(ev.getDateHeureFin(),   TIME_FMT);
            int    maxP     = ev.getNbParticipantsMax();

            // 2-column info grid
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(8);
            infoGrid.setVgap(8);
            ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
            ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
            infoGrid.getColumnConstraints().addAll(col1, col2);

            infoGrid.add(infoCell("📅", dateStr),                    0, 0);
            infoGrid.add(infoCell("📍", ev.getLieu() != null ? ev.getLieu() : "—"), 1, 0);
            infoGrid.add(infoCell("👥", maxP + " max"),               0, 1);
            infoGrid.add(infoCell("🕐", debutStr + " – " + finStr),  1, 1);

            // Capacity bar
            // Simulate some capacity (0..max); use 0 as placeholder if no real data
            int currentP = 0; // replace with real participant count if available
            double pct = maxP > 0 ? Math.min(1.0, (double) currentP / maxP) : 0.0;
            int pctInt = (int) (pct * 100);

            HBox capHeader = new HBox();
            capHeader.setAlignment(Pos.CENTER_LEFT);
            Label capLabel = new Label("Capacité");
            capLabel.getStyleClass().add("capacity-label");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Label pctLabel = new Label(pctInt + "%");
            pctLabel.getStyleClass().add("capacity-pct");
            capHeader.getChildren().addAll(capLabel, spacer, pctLabel);

            ProgressBar bar = new ProgressBar(pct);
            bar.getStyleClass().add("capacity-bar");
            bar.setMaxWidth(Double.MAX_VALUE);

            // Tags row (lieu as tag, placeholder tags)
            HBox tagsRow = new HBox(6);
            tagsRow.setAlignment(Pos.CENTER_LEFT);
            for (String t : new String[]{"Entreprise", "Interne"}) {
                Label tl = new Label(t);
                tl.getStyleClass().add("tag");
                tagsRow.getChildren().add(tl);
            }

            body.getChildren().addAll(infoGrid, capHeader, bar, tagsRow);
            card.getChildren().add(body);

            // ── Divider ──────────────────────────────────
            Region div = new Region();
            div.getStyleClass().add("card-divider");
            VBox.setMargin(div, new Insets(10, 0, 0, 0));
            card.getChildren().add(div);

            // ── Actions: "Modifier" (outline) + "Supprimer" (red outline) ──
            HBox actionsBox = new HBox(10);
            actionsBox.getStyleClass().add("card-actions");
            actionsBox.setPadding(new Insets(12, 16, 16, 16));
            actionsBox.setAlignment(Pos.CENTER);

            Button editBtn = new Button("✏  Modifier");
            editBtn.getStyleClass().add("btn-modifier");
            editBtn.setOnAction(e -> openForm(ev));
            HBox.setHgrow(editBtn, Priority.ALWAYS);
            editBtn.setMaxWidth(Double.MAX_VALUE);

            Button delBtn = new Button("🗑  Supprimer");
            delBtn.getStyleClass().add("btn-supprimer");
            delBtn.setOnAction(e -> handleDeleteEvent(ev));
            HBox.setHgrow(delBtn, Priority.ALWAYS);
            delBtn.setMaxWidth(Double.MAX_VALUE);

            actionsBox.getChildren().addAll(editBtn, delBtn);
            card.getChildren().add(actionsBox);

            return card;
        }

        // ── Helpers ──────────────────────────────────────
        private HBox infoCell(String icon, String text) {
            HBox cell = new HBox(6);
            cell.setAlignment(Pos.CENTER_LEFT);
            Label ic  = new Label(icon); ic.getStyleClass().add("info-icon");
            Label val = new Label(text); val.getStyleClass().add("info-text");
            cell.getChildren().addAll(ic, val);
            return cell;
        }

        private String fmt(Date d, SimpleDateFormat fmt) {
            if (d == null) return "—";
            try { return fmt.format(d); } catch (Exception e) { return "—"; }
        }
    }

    // ── FXML handlers ────────────────────────────────────
    @FXML void handleAjouterEvenement(ActionEvent event) { openForm(null); }
    @FXML void handleModifier(ActionEvent event) {}
    @FXML void handleSupprimer(ActionEvent event) {}
    @FXML void handleSearch(KeyEvent event) { filterAndDisplayEvents(); }

    private void openForm(Evenement existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/planification/AjouterEvent.fxml"));
            VBox root = loader.load();
            AjouterEventCont controller = loader.getController();
            if (existing != null) controller.setEvenementToEdit(existing);

            Stage stage = new Stage();
            stage.initOwner(cardsContainer.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(existing == null ? "Nouvel événement" : "Modifier l'événement");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadEvents();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    private void handleDeleteEvent(Evenement ev) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'événement ?");
        confirm.setContentText("Voulez-vous vraiment supprimer \""
                + (ev.getTitre() != null ? ev.getTitre() : "") + "\" ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                serviceEvenement.delete(ev);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement supprimé.");
                loadEvents();
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