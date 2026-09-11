package planification.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import planification.models.Espace;
import planification.models.ResevEspace;
import planification.services.ServiceEspace;
import planification.services.ServiceReservEsp;
import utils.UserSession;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReservEspaceCont implements Initializable {

    // ── FXML ──────────────────────────────────────────────
    @FXML private TilePane      cardsContainer;
    @FXML private StackPane     centerStack;
    @FXML private TextField     searchField;
    // Filter toggles — injected from FXML
    @FXML private ToggleButton  btnTypeTous;
    @FXML private ToggleButton  btnTypeReunion;
    @FXML private ToggleButton  btnTypeCo;
    @FXML private ToggleButton  btnStatutTous;
    @FXML private ToggleButton  btnStatutDispo;
    @FXML private ToggleButton  btnStatutOccupe;

    // ── Filter state ──────────────────────────────────────
    private String filterType   = "ALL";   // ALL | REUNION | COWORKING
    private String filterStatus = "ALL";   // ALL | DISPONIBLE | OCCUPE

    // ── Services ──────────────────────────────────────────
    private final ServiceEspace    serviceEspace    = new ServiceEspace();
    private final ServiceReservEsp serviceReservEsp = new ServiceReservEsp();
    private List<Espace> allEspaces;

    // ── Card dimensions ───────────────────────────────────
    private static final double CARD_W  = 380;
    private static final double CARD_H  = 460;
    private static final double IMAGE_H = CARD_W * 9.0 / 16.0;

    // ── Init ──────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wireFilterToggles();
        loadEspaces();
    }

    // ── Wire toggle groups so only one button per group stays selected ────
    private void wireFilterToggles() {
        ToggleGroup typeGroup = new ToggleGroup();
        btnTypeTous.setToggleGroup(typeGroup);
        btnTypeReunion.setToggleGroup(typeGroup);
        btnTypeCo.setToggleGroup(typeGroup);
        // Prevent deselecting all
        typeGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == null) typeGroup.selectToggle(old);
        });

        ToggleGroup statutGroup = new ToggleGroup();
        btnStatutTous.setToggleGroup(statutGroup);
        btnStatutDispo.setToggleGroup(statutGroup);
        btnStatutOccupe.setToggleGroup(statutGroup);
        statutGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == null) statutGroup.selectToggle(old);
        });
    }

    // ── FXML handlers for filter toggles ─────────────────
    @FXML
    private void handleTypeFilter(javafx.event.ActionEvent ev) {
        if (ev.getSource() instanceof ToggleButton tb) {
            filterType = (String) tb.getUserData();
            filterAndDisplayEspaces();
        }
    }

    @FXML
    private void handleStatutFilter(javafx.event.ActionEvent ev) {
        if (ev.getSource() instanceof ToggleButton tb) {
            filterStatus = (String) tb.getUserData();
            filterAndDisplayEspaces();
        }
    }

    // ── Data ──────────────────────────────────────────────
    private void loadEspaces() {
        allEspaces = serviceEspace.getAll();
        filterAndDisplayEspaces();
    }

    private void filterAndDisplayEspaces() {
        String q = searchField != null ? searchField.getText().toLowerCase() : "";

        List<Espace> filtered = allEspaces.stream()
                // ── Text search ─────────────────────────────────────────────
                .filter(e -> q.isEmpty()
                        || (e.getNom() != null && e.getNom().toLowerCase().contains(q))
                        || (e.getListeEquipements() != null &&
                        e.getListeEquipements().stream().anyMatch(eq -> eq.toLowerCase().contains(q))))
                // ── Type filter ─────────────────────────────────────────────
                .filter(e -> switch (filterType) {
                    case "COWORKING" ->  e.isCoworking();
                    case "REUNION"   -> !e.isCoworking();
                    default          ->  true;
                })
                // ── Status filter ───────────────────────────────────────────
                .filter(e -> {
                    if ("ALL".equals(filterStatus)) return true;
                    boolean occupe = isOccupeAujourdhui(e);
                    return "OCCUPE".equals(filterStatus) ? occupe : !occupe;
                })
                .collect(Collectors.toList());

        cardsContainer.getChildren().clear();
        filtered.forEach(e -> cardsContainer.getChildren().add(buildCard(e)));
    }

    // ── Dynamic availability check ────────────────────────
    /**
     * Returns true if this espace has at least one active reservation
     * for today (any time slot). Used to show the live badge on cards.
     */
    private boolean isOccupeAujourdhui(Espace e) {
        try {
            LocalDate today = LocalDate.now();
            return serviceReservEsp.getAll().stream()
                    .filter(r -> r.getIdEspace() == e.getId())
                    .anyMatch(r -> {
                        if (r.getDateReservation() == null) return false;
                        LocalDate resDate = r.getDateReservation().toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        return resDate.equals(today);
                    });
        } catch (Exception ex) {
            return false; // if DB error, assume available
        }
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

        // Dynamic availability: check if this espace has any active reservation today
        boolean occupeAujourdhui = isOccupeAujourdhui(e);
        Label statusBadge = new Label(occupeAujourdhui ? "● Occupé" : "● Disponible");
        statusBadge.getStyleClass().add(occupeAujourdhui ? "badge-occupe" : "badge-disponible");
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBadge, new Insets(10, 10, 0, 0));

        // Badge type d'espace (réunion / coworking)
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

        HBox floorRow = infoRow("🏢", e.getEtage() == 1 ? "1er étage" : e.getEtage() + "ème étage");
        HBox capRow   = infoRow("👥", e.getCapacite() + " personnes");

        // Ligne info selon type
        HBox typeInfoRow = e.isCoworking()
                ? infoRow("💺", "Réservation à la chaise")
                : infoRow("🔒", "Réservation de toute la salle");

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

        body.getChildren().addAll(title, floorRow, capRow, typeInfoRow, tagsRow);
        card.getChildren().addAll(imgBox, body);

        // ── Divider ──────────────────────────────────────
        Region div = new Region();
        div.getStyleClass().add("card-divider");
        VBox.setMargin(div, new Insets(12, 0, 0, 0));
        card.getChildren().add(div);

        // ── CTA ──────────────────────────────────────────
        VBox ctaBox = new VBox();
        ctaBox.setPadding(new Insets(12, 16, 16, 16));

        String btnLabel = e.isCoworking()
                ? "💺 Choisir une chaise"
                : "✓ Réserver toute la salle";

        Button reserverBtn = new Button(btnLabel);
        reserverBtn.getStyleClass().add("btn-reserver");
        reserverBtn.setMaxWidth(Double.MAX_VALUE);
        // For meeting rooms: disable if fully booked today; coworking always clickable (seat-level)
        reserverBtn.setDisable(!e.isCoworking() && occupeAujourdhui);

        // ── Routage selon type ────────────────────────────
        if (e.isCoworking()) {
            reserverBtn.setOnAction(ev -> openCoworkingPlan(e));
        } else {
            reserverBtn.setOnAction(ev -> openReservationSalleForm(e));
        }

        ctaBox.getChildren().add(reserverBtn);
        card.getChildren().add(ctaBox);

        return card;
    }

    // ── Helper icon row ───────────────────────────────────
    private HBox infoRow(String icon, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ic  = new Label(icon); ic.getStyleClass().add("info-icon");
        Label val = new Label(text); val.getStyleClass().add("info-value");
        row.getChildren().addAll(ic, val);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //  COWORKING — ouvrir le plan existant (CoworkingController)
    // ═══════════════════════════════════════════════════════
    private void openCoworkingPlan(Espace e) {
        if (centerStack == null) return;
        try {
            URL url = getClass().getResource("/views/planification/coworking_reservation (4).fxml");
            if (url == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Fichier coworking_reservation (4).fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent coworkingRoot = loader.load();
            CoworkingController controller = loader.getController();
            if (controller != null) controller.setEspace(e);

            VBox overlayContent = new VBox(10);
            overlayContent.setAlignment(Pos.TOP_RIGHT);
            overlayContent.setPadding(new Insets(10));
            overlayContent.setMaxWidth(950);
            overlayContent.setMaxHeight(600);

            Button closeBtn = new Button("✕");
            closeBtn.getStyleClass().add("btn-supprimer");
            closeBtn.setOnAction(ev -> closeFormOverlay());
            VBox.setMargin(closeBtn, new Insets(0, 0, 4, 0));
            overlayContent.getChildren().addAll(closeBtn, coworkingRoot);

            pushOverlay(overlayContent);
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger le plan coworking : " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  RÉUNION — formulaire de réservation avec créneaux horaires
    // ═══════════════════════════════════════════════════════
    private void openReservationSalleForm(Espace e) {
        if (centerStack == null) return;

        // ── Resolve current user from session ─────────────
        int    sessionUserId = UserSession.getInstance().getUserId();
        String sessionName   = UserSession.getInstance().getUser();

        VBox form = new VBox(14);
        form.setPadding(new Insets(28));
        form.setMaxWidth(480);
        form.getStyleClass().add("form-container");
        form.setAlignment(Pos.TOP_LEFT);

        // ── Titre ────────────────────────────────────────
        Label formTitle = new Label("🏢  Réserver — " + (e.getNom() != null ? e.getNom() : ""));
        formTitle.getStyleClass().add("form-title");
        formTitle.setWrapText(true);

        Label subLabel = new Label("Réservation de toute la salle");
        subLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280;");

        // ── Utilisateur connecté (lecture seule) ─────────
        Label empLabel = new Label("Réservé par");
        Label empValue = new Label(
                (sessionName != null && !sessionName.isBlank() ? sessionName : "Utilisateur")
                        + "  (ID : " + sessionUserId + ")");
        empValue.setStyle("-fx-font-weight:bold; -fx-text-fill:#1a2540;");

        // ── Date picker ──────────────────────────────────
        Label dateLabel = new Label("Date de réservation *");
        DatePicker datePicker = new DatePicker(LocalDate.now());

        // ── Créneaux horaires (8h–17h, cases 1h) — sélection unique ─────────
        Label slotsLabel = new Label("Choisissez un créneau (1h) *");
        slotsLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        // 3-column grid of toggle buttons, one per hour slot
        GridPane slotsGrid = new GridPane();
        slotsGrid.setHgap(8);
        slotsGrid.setVgap(8);
        slotsGrid.setPadding(new Insets(4, 0, 4, 0));

        // Track selected slot
        final int[] selectedStartHour = {-1};
        final Button[] selectedBtn    = {null};

        // Style constants
        final String STYLE_FREE =
                "-fx-font-size:11px; -fx-background-radius:7; -fx-border-radius:7; -fx-cursor:hand;" +
                        "-fx-background-color:#EFF6FF; -fx-text-fill:#1D4ED8; -fx-border-color:#BFDBFE;" +
                        "-fx-padding:6 8;";
        final String STYLE_SELECTED =
                "-fx-font-size:11px; -fx-background-radius:7; -fx-border-radius:7; -fx-cursor:hand;" +
                        "-fx-background-color:#1D4ED8; -fx-text-fill:white; -fx-border-color:#1D4ED8;" +
                        "-fx-padding:6 8; -fx-font-weight:bold;";
        final String STYLE_TAKEN =
                "-fx-font-size:11px; -fx-background-radius:7; -fx-border-radius:7;" +
                        "-fx-background-color:#F3F4F6; -fx-text-fill:#9CA3AF; -fx-border-color:#E5E7EB;" +
                        "-fx-padding:6 8;";

        // Map hour → button, built once; availability updated by refreshSlots
        java.util.Map<Integer, Button> slotButtons = new java.util.LinkedHashMap<>();
        int col = 0, gridRow = 0;
        for (int h = 8; h < 17; h++) {
            final int sh = h;
            Button slotBtn = new Button(String.format("%02d:00–%02d:00", sh, sh + 1));
            slotBtn.setPrefWidth(130);
            slotBtn.setPrefHeight(34);
            slotBtn.setStyle(STYLE_FREE);
            slotButtons.put(h, slotBtn);
            slotsGrid.add(slotBtn, col, gridRow);
            col++;
            if (col == 3) { col = 0; gridRow++; }
        }

        // Label that shows which slot is currently chosen
        Label selectedSlotLabel = new Label("Aucun créneau sélectionné");
        selectedSlotLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-font-style:italic;");

        // ── Objectif ─────────────────────────────────────
        Label objLabel = new Label("Objectif *");
        TextField objField = new TextField();
        objField.setPromptText("Ex : Réunion équipe marketing");

        // ── Conflict warning label ────────────────────────
        Label conflictLabel = new Label();
        conflictLabel.setStyle("-fx-text-fill:#e53935; -fx-font-size:12px;" +
                "-fx-background-color:#fdecea; -fx-padding:6 10; -fx-background-radius:6;");
        conflictLabel.setWrapText(true);
        conflictLabel.setVisible(false);
        conflictLabel.setManaged(false);

        // ── Boutons ───────────────────────────────────────
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 0, 0, 0));

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn-supprimer");
        cancelBtn.setOnAction(ev -> closeFormOverlay());

        Button saveBtn = new Button("✓ Confirmer la réservation");
        saveBtn.getStyleClass().add("btn-reserver");
        saveBtn.setDisable(true);

        // Style for a slot the CURRENT user already owns
        final String STYLE_MINE =
                "-fx-font-size:11px; -fx-background-radius:7; -fx-border-radius:7;" +
                        "-fx-background-color:#D1FAE5; -fx-text-fill:#065F46; -fx-border-color:#6EE7B7;" +
                        "-fx-padding:6 8; -fx-font-weight:bold;";

        // ── Refresh: mark taken/own/free slots (called on date change) ───────
        Runnable refreshSlots = () -> {
            LocalDate date = datePicker.getValue();
            if (date == null) return;

            // Reset selection
            selectedStartHour[0] = -1;
            selectedBtn[0]       = null;
            selectedSlotLabel.setText("Aucun créneau sélectionné");
            selectedSlotLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-font-style:italic;");
            saveBtn.setDisable(true);
            conflictLabel.setVisible(false);
            conflictLabel.setManaged(false);

            // Fetch ALL reservations for this espace on this date in ONE query
            List<ResevEspace> dayReservations;
            try {
                dayReservations = serviceReservEsp.getByEspaceAndDate(
                        e.getId(), java.sql.Date.valueOf(date));
            } catch (Exception ex) {
                dayReservations = new java.util.ArrayList<>();
            }

            // Build a map: startHour → reservation (for fast lookup)
            java.util.Map<Integer, ResevEspace> takenMap = new java.util.HashMap<>();
            for (ResevEspace r : dayReservations) {
                if (r.getDateHeureDebut() == null) continue;
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(r.getDateHeureDebut());
                int rHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                takenMap.put(rHour, r);
            }

            slotButtons.forEach((h, btn) -> {
                ResevEspace existing = takenMap.get(h);

                if (existing != null) {
                    btn.setDisable(true);
                    btn.setOnAction(null);
                    if (existing.getIdEmploye() == sessionUserId) {
                        // This slot belongs to the CURRENT user
                        btn.setText(String.format("%02d:00–%02d:00 ✓ Moi", h, h + 1));
                        btn.setStyle(STYLE_MINE);
                        Tooltip tp = new Tooltip(
                                "Réservé par vous\nObjectif : " + existing.getObjectif());
                        tp.setStyle("-fx-font-size:11px;");
                        Tooltip.install(btn, tp);
                    } else {
                        // Taken by someone else
                        btn.setText(String.format("%02d:00–%02d:00 🔒", h, h + 1));
                        btn.setStyle(STYLE_TAKEN);
                        Tooltip tp = new Tooltip(
                                "Créneau déjà réservé");
                        tp.setStyle("-fx-font-size:11px;");
                        Tooltip.install(btn, tp);
                    }
                } else {
                    // Free slot
                    btn.setDisable(false);
                    btn.setText(String.format("%02d:00–%02d:00", h, h + 1));
                    btn.setStyle(STYLE_FREE);
                    Tooltip.install(btn, null);
                    btn.setOnAction(ev -> {
                        if (selectedBtn[0] != null)
                            selectedBtn[0].setStyle(STYLE_FREE);
                        btn.setStyle(STYLE_SELECTED);
                        selectedBtn[0]       = btn;
                        selectedStartHour[0] = h;
                        selectedSlotLabel.setText("✓ " + String.format("%02d:00 – %02d:00", h, h + 1));
                        selectedSlotLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#1D4ED8; -fx-font-weight:bold;");
                        conflictLabel.setVisible(false);
                        conflictLabel.setManaged(false);
                        saveBtn.setDisable(false);
                    });
                }
            });
        };

        datePicker.valueProperty().addListener((obs, o, n) -> refreshSlots.run());
        refreshSlots.run(); // initial check

        // ── Save action ───────────────────────────────────
        saveBtn.setOnAction(ev -> {
            try {
                if (sessionUserId <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Session invalide. Veuillez vous reconnecter.");
                    return;
                }
                if (selectedStartHour[0] < 0) {
                    showAlert(Alert.AlertType.WARNING, "Créneau manquant",
                            "Veuillez sélectionner un créneau horaire.");
                    return;
                }
                String objectif = objField.getText().trim();
                if (objectif.isEmpty())
                    throw new IllegalArgumentException("L'objectif ne peut pas être vide.");

                LocalDate date  = datePicker.getValue();
                if (date == null)
                    throw new IllegalArgumentException("Veuillez sélectionner une date.");

                int sh = selectedStartHour[0];
                LocalDateTime debut = LocalDateTime.of(date, LocalTime.of(sh, 0));
                LocalDateTime fin   = LocalDateTime.of(date, LocalTime.of(sh + 1, 0));
                Date dateRes   = java.sql.Date.valueOf(date);
                Date dateDebut = new Date(java.sql.Timestamp.valueOf(debut).getTime());
                Date dateFin   = new Date(java.sql.Timestamp.valueOf(fin).getTime());

                // Final DB-level guard (race condition)
                if (serviceReservEsp.hasConflict(e.getId(), dateDebut, dateFin)) {
                    showAlert(Alert.AlertType.ERROR, "Créneau indisponible",
                            "Un autre utilisateur vient de réserver ce créneau. Choisissez un autre horaire.");
                    refreshSlots.run();
                    return;
                }

                ResevEspace reservation = new ResevEspace(
                        e.getId(), sessionUserId, dateRes, dateDebut, dateFin,
                        objectif, true, new Date());
                serviceReservEsp.add(reservation);
                closeFormOverlay();
                filterAndDisplayEspaces();
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "La salle \"" + e.getNom() + "\" a été réservée avec succès !\n" +
                                "Créneau : " + String.format("%02d:00 – %02d:00", sh, sh + 1));
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(
                formTitle, subLabel,
                empLabel, empValue,
                dateLabel, datePicker,
                slotsLabel, slotsGrid,
                selectedSlotLabel,
                objLabel, objField,
                conflictLabel,
                btnRow
        );

        pushOverlay(form);
    }

    // ── Overlay helpers ───────────────────────────────────
    private void pushOverlay(javafx.scene.Node content) {
        Pane dimmer = new Pane();
        dimmer.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        dimmer.setPickOnBounds(true);
        dimmer.setOnMouseClicked(ev -> closeFormOverlay());

        StackPane layer = new StackPane(dimmer, content);
        StackPane.setAlignment(content, Pos.CENTER);
        layer.setPickOnBounds(false);
        centerStack.getChildren().add(layer);
    }

    private void closeFormOverlay() {
        if (centerStack != null && centerStack.getChildren().size() > 1)
            centerStack.getChildren().remove(centerStack.getChildren().size() - 1);
    }

    @FXML void handleSearch(KeyEvent event) { filterAndDisplayEspaces(); }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}