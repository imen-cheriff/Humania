package planification.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import planification.models.Evenement;
import planification.models.ParticipEven;
import planification.services.ServiceEvenement;
import planification.services.ServicePartEve;
import utils.UserSession;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class PartEventCont implements Initializable {

    // ── FXML ──────────────────────────────────────────────
    @FXML private GridPane    cardsContainer;
    @FXML private StackPane   centerStack;
    @FXML private TextField   searchField;
    @FXML private ToggleButton btnStatutTous;
    @FXML private ToggleButton btnStatutAvenir;
    @FXML private ToggleButton btnStatutEncours;
    @FXML private ToggleButton btnStatutTermine;

    // ── Filter state ──────────────────────────────────────
    // ALL | AVENIR | ENCOURS | TERMINE
    private String filterStatut = "ALL";

    // ── Services & state ──────────────────────────────────
    private final ServiceEvenement serviceEvenement = new ServiceEvenement();
    private final ServicePartEve   servicePartEve   = new ServicePartEve();
    private static final SimpleDateFormat DATE_FMT  = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
    private static final SimpleDateFormat DATE_SHORT = new SimpleDateFormat("dd/MM/yyyy",  Locale.FRENCH);
    private static final SimpleDateFormat TIME_FMT  = new SimpleDateFormat("HH:mm");
    private List<Evenement> allEvents;

    private static final int COLS    = 3;
    private static final int QR_MOD  = 6; // pixels per QR module — crisp rendering

    // ── Logged-in user ────────────────────────────────────
    private int    currentUserId;
    private String currentUserName;

    // ── Company logo (loaded once at init) ───────────────
    private BufferedImage logoAWT;

    // ── Init ──────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        UserSession session = UserSession.getInstance();
        currentUserId   = session.getUserId();
        currentUserName = session.getUser() != null ? session.getUser() : "Utilisateur";
        loadLogo();
        wireFilterToggles();
        loadEvents();
    }

    // ── Wire ToggleGroup so only one button stays selected ────────────────
    private void wireFilterToggles() {
        ToggleGroup g = new ToggleGroup();
        btnStatutTous.setToggleGroup(g);
        btnStatutAvenir.setToggleGroup(g);
        btnStatutEncours.setToggleGroup(g);
        btnStatutTermine.setToggleGroup(g);
        // Prevent deselecting all
        g.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == null) g.selectToggle(old);
        });
    }

    // ── FXML handler for statut filter ────────────────────
    @FXML
    private void handleStatutFilter(javafx.event.ActionEvent ev) {
        if (ev.getSource() instanceof ToggleButton tb) {
            filterStatut = (String) tb.getUserData();
            filterAndDisplayEvents();
        }
    }

    /**
     * Tries to load logo from classpath (/images/logo.png).
     * Place the logo PNG at src/main/resources/images/logo.png in your project.
     */
    private void loadLogo() {
        try {
            InputStream is = getClass().getResourceAsStream("/images/logo.png");
            if (is != null) {
                logoAWT = ImageIO.read(is);
                return;
            }
            // Fallback: project-relative path
            File f = new File("src/main/resources/images/logo.png");
            if (f.exists()) logoAWT = ImageIO.read(f);
        } catch (Exception e) {
            logoAWT = null; // will use text "H" fallback on badge
        }
    }

    // ── Data ──────────────────────────────────────────────
    private void loadEvents() {
        allEvents = serviceEvenement.getAll();
        filterAndDisplayEvents();
    }

    private void filterAndDisplayEvents() {
        String q   = searchField != null ? searchField.getText().toLowerCase() : "";
        Date   now = new Date();

        List<Evenement> filtered = allEvents.stream()
                // ── Text search ──────────────────────────────────────────
                .filter(ev -> q.isEmpty()
                        || (ev.getTitre()       != null && ev.getTitre().toLowerCase().contains(q))
                        || (ev.getLieu()        != null && ev.getLieu().toLowerCase().contains(q))
                        || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(q)))
                // ── Statut filter ────────────────────────────────────────
                .filter(ev -> {
                    if ("ALL".equals(filterStatut)) return true;
                    boolean termine = ev.getDateHeureFin() != null && ev.getDateHeureFin().before(now);
                    boolean encours = ev.getDateHeureDebut() != null && ev.getDateHeureDebut().before(now)
                            && ev.getDateHeureFin() != null && ev.getDateHeureFin().after(now);
                    boolean avenir  = !termine && !encours;
                    return switch (filterStatut) {
                        case "TERMINE" -> termine;
                        case "ENCOURS" -> encours;
                        case "AVENIR"  -> avenir;
                        default        -> true;
                    };
                })
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
    //  EventCard inner class  (unchanged from original)
    // ═══════════════════════════════════════════════════════
    private class EventCard {

        private final Evenement ev;
        EventCard(Evenement ev) { this.ev = ev; }

        VBox build() {
            VBox card = new VBox();
            card.setSpacing(0);
            card.setMinHeight(420); card.setPrefHeight(420); card.setMaxHeight(420);
            card.getStyleClass().add("event-card");

            Date now = new Date();
            String badgeClass, badgeText;
            boolean isTermine;

            if (ev.getDateHeureFin() != null && ev.getDateHeureFin().before(now)) {
                badgeClass = "badge-termine"; badgeText = "Terminé"; isTermine = true;
            } else if (ev.getDateHeureDebut() != null && ev.getDateHeureDebut().before(now)
                    && ev.getDateHeureFin() != null && ev.getDateHeureFin().after(now)) {
                badgeClass = "badge-encours"; badgeText = "En cours"; isTermine = false;
            } else {
                badgeClass = "badge-avenir"; badgeText = "À venir"; isTermine = false;
            }

            boolean alreadyRegistered = currentUserId > 0
                    && servicePartEve.isAlreadyRegistered(ev.getId(), currentUserId);

            StackPane header = new StackPane();
            header.setPadding(new Insets(18, 16, 6, 16));
            VBox titleBox = new VBox(6);
            titleBox.setAlignment(Pos.TOP_LEFT);
            Label titleLabel = new Label(ev.getTitre() != null ? ev.getTitre() : "");
            titleLabel.getStyleClass().add("card-title");
            titleLabel.setWrapText(true); titleLabel.setMaxWidth(280);
            String desc = ev.getDescription();
            if (desc != null && desc.length() > 70) desc = desc.substring(0, 67) + "…";
            Label descLabel = new Label(desc != null ? desc : "");
            descLabel.getStyleClass().add("card-description");
            descLabel.setWrapText(true); descLabel.setMaxWidth(280);
            Label catTag = new Label("📌 Événement");
            catTag.getStyleClass().add("tag-category");
            titleBox.getChildren().addAll(titleLabel, descLabel, catTag);
            Label badgeLabel = new Label(badgeText);
            badgeLabel.getStyleClass().addAll("badge", badgeClass);
            StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
            StackPane.setAlignment(titleBox,   Pos.TOP_LEFT);
            header.getChildren().addAll(titleBox, badgeLabel);
            card.getChildren().add(header);

            VBox body = new VBox(8);
            body.getStyleClass().add("card-body");
            body.setPadding(new Insets(10, 16, 0, 16));
            VBox.setVgrow(body, Priority.ALWAYS);
            String dateStr  = fmt(ev.getDateEvenement(),  DATE_FMT);
            String debutStr = fmt(ev.getDateHeureDebut(), TIME_FMT);
            String finStr   = fmt(ev.getDateHeureFin(),   TIME_FMT);
            int maxP = ev.getNbParticipantsMax();
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(8); infoGrid.setVgap(8);
            ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
            ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
            infoGrid.getColumnConstraints().addAll(col1, col2);
            infoGrid.add(infoCell("📅", dateStr),                                    0, 0);
            infoGrid.add(infoCell("📍", ev.getLieu() != null ? ev.getLieu() : "—"), 1, 0);
            infoGrid.add(infoCell("👥", maxP + " max"),                              0, 1);
            infoGrid.add(infoCell("🕐", debutStr + " – " + finStr),                 1, 1);
            HBox capHeader = new HBox();
            capHeader.setAlignment(Pos.CENTER_LEFT);
            Label capLabel = new Label("Capacité"); capLabel.getStyleClass().add("capacity-label");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Label pctLabel = new Label("0%"); pctLabel.getStyleClass().add("capacity-pct");
            capHeader.getChildren().addAll(capLabel, spacer, pctLabel);
            ProgressBar progBar = new ProgressBar(0);
            progBar.getStyleClass().add("capacity-bar"); progBar.setMaxWidth(Double.MAX_VALUE);
            HBox tagsRow = new HBox(6); tagsRow.setAlignment(Pos.CENTER_LEFT);
            for (String t : new String[]{"Entreprise", "Interne"}) {
                Label tl = new Label(t); tl.getStyleClass().add("tag"); tagsRow.getChildren().add(tl);
            }
            body.getChildren().addAll(infoGrid, capHeader, progBar, tagsRow);
            card.getChildren().add(body);

            Region div = new Region();
            div.getStyleClass().add("card-divider");
            VBox.setMargin(div, new Insets(10, 0, 0, 0));
            card.getChildren().add(div);

            HBox actionsBox = new HBox(10);
            actionsBox.setPadding(new Insets(12, 16, 16, 16));
            actionsBox.setAlignment(Pos.CENTER);
            Button detailBtn = new Button("👁  Voir les détails");
            detailBtn.getStyleClass().add("btn-modifier");
            HBox.setHgrow(detailBtn, Priority.ALWAYS); detailBtn.setMaxWidth(Double.MAX_VALUE);
            detailBtn.setOnAction(e -> showDetailsOverlay(ev));

            if (isTermine) {
                Button btn = new Button("✗  Terminé");
                btn.getStyleClass().add("btn-participer"); btn.setDisable(true);
                HBox.setHgrow(btn, Priority.ALWAYS); btn.setMaxWidth(Double.MAX_VALUE);
                actionsBox.getChildren().addAll(detailBtn, btn);
            } else {
                Button inscBtn = new Button(alreadyRegistered ? "✓  Inscrit" : "✓  S'inscrire");
                inscBtn.getStyleClass().add("btn-participer");
                inscBtn.setDisable(alreadyRegistered);
                if (alreadyRegistered) inscBtn.setStyle("-fx-opacity: 0.6;");
                HBox.setHgrow(inscBtn, Priority.ALWAYS); inscBtn.setMaxWidth(Double.MAX_VALUE);

                Button annulBtn = new Button("✗  Annuler");
                annulBtn.getStyleClass().add("btn-supprimer");
                HBox.setHgrow(annulBtn, Priority.ALWAYS); annulBtn.setMaxWidth(Double.MAX_VALUE);
                annulBtn.setVisible(alreadyRegistered); annulBtn.setManaged(alreadyRegistered);

                inscBtn.setOnAction(e -> handleInscription(ev, inscBtn, annulBtn));
                annulBtn.setOnAction(e -> {
                    boolean ok = servicePartEve.cancelParticipation(ev.getId(), currentUserId);
                    if (ok) {
                        inscBtn.setText("✓  S'inscrire"); inscBtn.setDisable(false); inscBtn.setStyle("");
                        annulBtn.setVisible(false); annulBtn.setManaged(false);
                        showAlert(Alert.AlertType.INFORMATION, "Annulé", "Votre participation a été annulée.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'annuler la participation.");
                    }
                });
                actionsBox.getChildren().addAll(detailBtn, inscBtn, annulBtn);
            }
            card.getChildren().add(actionsBox);
            return card;
        }

        private HBox infoCell(String icon, String text) {
            HBox cell = new HBox(6); cell.setAlignment(Pos.CENTER_LEFT);
            Label ic  = new Label(icon); ic.getStyleClass().add("info-icon");
            Label val = new Label(text); val.getStyleClass().add("info-text");
            cell.getChildren().addAll(ic, val);
            return cell;
        }
        private String fmt(Date d, SimpleDateFormat f) {
            if (d == null) return "—";
            try { return f.format(d); } catch (Exception e) { return "—"; }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Inscription handler
    // ═══════════════════════════════════════════════════════
    private void handleInscription(Evenement ev, Button inscBtn, Button annulBtn) {
        if (currentUserId <= 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de récupérer votre identifiant. Veuillez vous reconnecter.");
            return;
        }
        try {
            ParticipEven p = new ParticipEven();
            p.setIdEvenement(ev.getId());
            p.setIdEmploye(currentUserId);
            p.setDateParticipation(ev.getDateEvenement() != null ? ev.getDateEvenement() : new Date());
            p.setStatut(true);
            p.setCreeLe(new Date());
            servicePartEve.add(p);

            inscBtn.setText("✓  Inscrit");
            inscBtn.setDisable(true);
            inscBtn.setStyle("-fx-opacity: 0.6;");
            annulBtn.setVisible(true);
            annulBtn.setManaged(true);

            showBadgeOverlay(ev);   // ← replaces old showQRCode()

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur d'inscription", ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Badge overlay — shown in-app after inscription
    // ═══════════════════════════════════════════════════════
    private void showBadgeOverlay(Evenement ev) {
        if (centerStack == null) return;

        String titre    = ev.getTitre()          != null ? ev.getTitre()  : "Événement";
        String lieu     = ev.getLieu()            != null ? ev.getLieu()   : "";
        String dateStr  = ev.getDateEvenement()   != null ? DATE_FMT.format(ev.getDateEvenement())   : "";
        String debutStr = ev.getDateHeureDebut()  != null ? TIME_FMT.format(ev.getDateHeureDebut())  : "";
        String finStr   = ev.getDateHeureFin()    != null ? TIME_FMT.format(ev.getDateHeureFin())    : "";

        // Plain multi-line QR content — iPhones read this perfectly
        String qrContent = "Participant: " + stripAccents(currentUserName) + "\n"
                + "Evenement: "   + stripAccents(titre)           + "\n"
                + "Date: "        + (ev.getDateEvenement() != null
                ? DATE_SHORT.format(ev.getDateEvenement()) : "") + "\n"
                + "Heure: "       + debutStr + " - " + finStr     + "\n"
                + "Lieu: "        + stripAccents(lieu)             + "\n"
                + "ID: "          + currentUserId;

        // Render badge as AWT then convert to JavaFX image
        WritableImage badgeFX;
        final BufferedImage badgeAWT;
        try {
            badgeAWT = renderBadge(titre, lieu, dateStr, debutStr, finStr, qrContent);
            badgeFX  = awtToFx(badgeAWT);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur badge", ex.getMessage());
            return;
        }

        // ── Overlay popup ──────────────────────────────────
        VBox popup = new VBox(12);
        popup.setMaxWidth(380); popup.setPrefWidth(380);
        popup.setAlignment(Pos.TOP_CENTER);
        popup.setStyle(
                "-fx-background-color: #f4f6fb;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 22 22 18 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,20,80,0.28), 36, 0, 0, 10);");

        Label headLbl = new Label("🎉  Inscription confirmée — voici votre badge");
        headLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1a2540;");
        headLbl.setWrapText(true);

        // Badge image preview
        ImageView badgeView = new ImageView(badgeFX);
        badgeView.setFitWidth(320);
        badgeView.setPreserveRatio(true);
        badgeView.setSmooth(true);
        VBox previewBox = new VBox(badgeView);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 8;" +
                        "-fx-border-color: #dde4f0; -fx-border-width: 1; -fx-border-radius: 14;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,20,80,0.10), 12, 0, 0, 4);");

        Label hintLbl = new Label("Scannez le QR code à l'entrée · Téléchargez pour l'imprimer");
        hintLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;-fx-font-style:italic;");
        hintLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        HBox btnRow = new HBox(10); btnRow.setAlignment(Pos.CENTER);

        Button dlBtn = new Button("⬇  Télécharger le badge");
        HBox.setHgrow(dlBtn, Priority.ALWAYS); dlBtn.setMaxWidth(Double.MAX_VALUE);
        String dlSty = "-fx-background-color:#5b8dee;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:10;-fx-padding:10 0;-fx-cursor:hand;-fx-font-size:13px;";
        dlBtn.setStyle(dlSty);
        dlBtn.setOnMouseEntered(e -> dlBtn.setStyle(dlSty.replace("#5b8dee","#3a6fd8")));
        dlBtn.setOnMouseExited( e -> dlBtn.setStyle(dlSty));
        dlBtn.setOnAction(e -> saveBadgePNG(ev, qrContent));

        Button closeBtn = new Button("Fermer");
        HBox.setHgrow(closeBtn, Priority.ALWAYS); closeBtn.setMaxWidth(Double.MAX_VALUE);
        String clSty = "-fx-background-color:#1a2540;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:10;-fx-padding:10 0;-fx-cursor:hand;-fx-font-size:13px;";
        closeBtn.setStyle(clSty);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(clSty.replace("#1a2540","#e57373")));
        closeBtn.setOnMouseExited( e -> closeBtn.setStyle(clSty));
        closeBtn.setOnAction(e -> closeBadgeOverlay());
        btnRow.getChildren().addAll(dlBtn, closeBtn);

        popup.getChildren().addAll(headLbl, previewBox, hintLbl, btnRow);

        ScrollPane scrollPane = new ScrollPane(popup);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxWidth(380);
        scrollPane.setMaxHeight(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.85);
        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;" +
                        "-fx-border-color: transparent;");

        Pane dimmer = new Pane();
        dimmer.setStyle("-fx-background-color: rgba(0,0,0,0.52);");
        dimmer.setPickOnBounds(true);
        dimmer.setOnMouseClicked(e -> closeBadgeOverlay());

        StackPane layer = new StackPane(dimmer, scrollPane);
        StackPane.setAlignment(scrollPane, Pos.CENTER);
        layer.setPickOnBounds(false);
        centerStack.getChildren().add(layer);
    }

    // ── Save badge PNG via FileChooser ─────────────────────
    private void saveBadgePNG(Evenement ev, String qrContent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le badge");
        String safe = stripAccents(ev.getTitre() != null ? ev.getTitre() : "event")
                .replaceAll("[\\s/\\\\:*?\"<>|]", "_");
        fc.setInitialFileName("badge_" + safe + ".png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File file = fc.showSaveDialog(centerStack.getScene().getWindow());
        if (file == null) return;
        try {
            String titre    = ev.getTitre()          != null ? ev.getTitre()  : "Événement";
            String lieu     = ev.getLieu()            != null ? ev.getLieu()   : "";
            String dateStr  = ev.getDateEvenement()   != null ? DATE_FMT.format(ev.getDateEvenement())   : "";
            String debutStr = ev.getDateHeureDebut()  != null ? TIME_FMT.format(ev.getDateHeureDebut())  : "";
            String finStr   = ev.getDateHeureFin()    != null ? TIME_FMT.format(ev.getDateHeureFin())    : "";
            BufferedImage badge = renderBadge(titre, lieu, dateStr, debutStr, finStr, qrContent);
            ImageIO.write(badge, "PNG", file);
            showAlert(Alert.AlertType.INFORMATION, "Badge sauvegardé",
                    "Enregistré : " + file.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de sauvegarder : " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  renderBadge  —  500 × 640 professional event badge
    //
    //  ┌──────────────────────────────────┐
    //  │ ████ 4-color stripe (8px) ██████ │
    //  │  [LOGO]  HUMANIA                 │  dark header
    //  │          RH Management           │
    //  │                  BADGE PARTICIP. │
    //  ├──────────────────────────────────┤
    //  │         NOM  PRÉNOM              │  big bold name
    //  │      ───── accent line ─────     │  blue underline
    //  │       Titre de l'événement       │  event title
    //  │  DATE  15 mars 2026  |  10h–12h  │  info pills
    //  │  LIEU  Salle de direction        │
    //  ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  ┤  dashed divider
    //  │            [ QR CODE ]           │
    //  │   Scannez à l'entrée …           │
    //  ├──────────────────────────────────┤
    //  │  footer · ID                     │  light strip
    //  └──────────────────────────────────┘
    // ═══════════════════════════════════════════════════════
    private BufferedImage renderBadge(
            String titre, String lieu,
            String dateStr, String debutStr, String finStr,
            String qrContent) throws Exception {

        final int W = 500, H = 640;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        // ── White rounded card ────────────────────────────
        g.setColor(java.awt.Color.WHITE);
        g.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, W, H, 24, 24));

        // ── 4-color stripe (top 8px) ──────────────────────
        int[] stripeColors = { 0xe57373, 0x5b8dee, 0xf5c842, 0x5db87a };
        int sw = W / 4;
        for (int i = 0; i < 4; i++) {
            g.setColor(new java.awt.Color(stripeColors[i]));
            if      (i == 0) g.fill(new java.awt.geom.RoundRectangle2D.Float(0,       0, sw, 8, 8, 8));
            else if (i == 3) g.fill(new java.awt.geom.RoundRectangle2D.Float(3 * sw,  0, sw, 8, 8, 8));
            else             g.fillRect(i * sw, 0, sw, 8);
        }

        // ── Dark header band ──────────────────────────────
        g.setColor(new java.awt.Color(0x1a2540));
        g.fillRect(0, 8, W, 98);

        // ── Logo ──────────────────────────────────────────
        int logoSz = 56, lx = 18, ly = 15;
        if (logoAWT != null) {
            // Scale logo keeping aspect ratio, fit inside logoSz × logoSz
            BufferedImage scaled = new BufferedImage(logoSz, logoSz, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gl = scaled.createGraphics();
            gl.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gl.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
            double asp = (double) logoAWT.getWidth() / logoAWT.getHeight();
            int dw = asp >= 1 ? logoSz : (int)(logoSz * asp);
            int dh = asp <  1 ? logoSz : (int)(logoSz / asp);
            gl.drawImage(logoAWT, (logoSz - dw) / 2, (logoSz - dh) / 2, dw, dh, null);
            gl.dispose();
            g.drawImage(scaled, lx, ly, null);
        } else {
            // Text fallback: blue circle with "H"
            g.setColor(new java.awt.Color(0x5b8dee));
            g.fillOval(lx, ly, logoSz, logoSz);
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new Font("Georgia", Font.BOLD, 26));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("H",
                    lx + (logoSz - fm.stringWidth("H")) / 2,
                    ly + (logoSz + fm.getAscent()) / 2 - fm.getDescent());
        }

        // App name beside logo
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("HUMANIA", lx + logoSz + 12, ly + 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new java.awt.Color(0x94a3b8));
        g.drawString("RH Management", lx + logoSz + 12, ly + 40);

        // "BADGE PARTICIPANT" pill (top-right of header)
        String badgeTag = "BADGE PARTICIPANT";
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fmPill = g.getFontMetrics();
        int pillW = fmPill.stringWidth(badgeTag) + 16;
        int pillX = W - pillW - 16, pillY = ly + 8;
        g.setColor(new java.awt.Color(0x2a3a5c));
        g.fill(new java.awt.geom.RoundRectangle2D.Float(pillX, pillY, pillW, 18, 9, 9));
        g.setColor(new java.awt.Color(0x8ab4f8));
        g.drawString(badgeTag, pillX + 8, pillY + 13);

        // Header bottom border
        g.setColor(new java.awt.Color(0xe8edf5));
        g.fillRect(0, 106, W, 1);

        // ── NAME (large, centred) ─────────────────────────
        int nameY = 106 + 46;
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics fmName = g.getFontMetrics();
        if (fmName.stringWidth(currentUserName) > W - 40) {
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            fmName = g.getFontMetrics();
        }
        g.setColor(new java.awt.Color(0x1a2540));
        int nameX = (W - fmName.stringWidth(currentUserName)) / 2;
        g.drawString(currentUserName, nameX, nameY);

        // Blue underline accent
        int ulW = Math.min(fmName.stringWidth(currentUserName) + 24, 200);
        g.setColor(new java.awt.Color(0x5b8dee));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((W - ulW) / 2, nameY + 6, (W + ulW) / 2, nameY + 6);
        g.setStroke(new BasicStroke(1f));

        // ── Event title ───────────────────────────────────
        int evY = nameY + 36;
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new java.awt.Color(0x5b8dee));
        FontMetrics fmEv = g.getFontMetrics();
        evY = drawCenteredWrapped(g, titre, evY, W - 60, 30, fmEv);

        // ── Info pills ────────────────────────────────────
        int pilY = evY + 24;
        String dateLine = dateStr + (debutStr.isEmpty() ? "" : "  |  " + debutStr + " - " + finStr);
        if (!dateLine.trim().isEmpty()) { drawInfoPill(g, "DATE", dateLine, W / 2, pilY); pilY += 36; }
        if (!lieu.isEmpty())            { drawInfoPill(g, "LIEU", lieu,     W / 2, pilY); pilY += 36; }

        // ── Dashed divider ────────────────────────────────
        int dashY = pilY + 4;
        g.setColor(new java.awt.Color(0xdde4f0));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[]{7f, 5f}, 0f));
        g.drawLine(30, dashY, W - 30, dashY);
        g.setStroke(new BasicStroke(1f));

        // ── QR code ──────────────────────────────────────
        BufferedImage qrBuf = buildAWTQR(qrContent);
        int qrX = (W - qrBuf.getWidth()) / 2;
        int qrY = dashY + 14;
        int pad = 10;
        g.setColor(java.awt.Color.WHITE);
        g.fill(new java.awt.geom.RoundRectangle2D.Float(
                qrX - pad, qrY - pad,
                qrBuf.getWidth() + pad * 2, qrBuf.getHeight() + pad * 2, 12, 12));
        g.setColor(new java.awt.Color(0xdde4f0));
        g.draw(new java.awt.geom.RoundRectangle2D.Float(
                qrX - pad, qrY - pad,
                qrBuf.getWidth() + pad * 2, qrBuf.getHeight() + pad * 2, 12, 12));
        g.drawImage(qrBuf, qrX, qrY, null);

        // ── Scan hint ─────────────────────────────────────
        int scanY = qrY + qrBuf.getHeight() + pad + 20;
        g.setFont(new Font("SansSerif", Font.ITALIC, 11));
        g.setColor(new java.awt.Color(0x94a3b8));
        String hint = "Scannez ce QR code a l'entree de l'evenement";
        FontMetrics fmH = g.getFontMetrics();
        g.drawString(hint, (W - fmH.stringWidth(hint)) / 2, scanY);

        // ── Footer ───────────────────────────────────────
  /*      g.setColor(new java.awt.Color(0xf4f6fb));
        g.fill(new java.awt.geom.RoundRectangle2D.Float(0, H - 28, W, 28, 24, 24));
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g.setColor(new java.awt.Color(0xaab4c8));
        String footer = "Genere par Humania RH Management  •  ID: " + currentUserId;
        FontMetrics fmF = g.getFontMetrics();
        g.drawString("", (W - fmF.stringWidth(footer)) / 2, H - 10);
*/
        g.dispose();
        return img;
    }

    // ── Centered text, with word-wrap, returns new Y ──────
    private int drawCenteredWrapped(Graphics2D g, String text, int y, int maxW, int lineH, FontMetrics fm) {
        if (fm.stringWidth(text) <= maxW) {
            g.drawString(text, (500 - fm.stringWidth(text)) / 2, y);
            return y;
        }
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int curY = y;
        for (String w : words) {
            String test = line.length() == 0 ? w : line + " " + w;
            if (fm.stringWidth(test) > maxW) {
                g.drawString(line.toString(), (500 - fm.stringWidth(line.toString())) / 2, curY);
                curY += lineH;
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0)
            g.drawString(line.toString(), (500 - fm.stringWidth(line.toString())) / 2, curY);
        return curY;
    }

    // ── Centred info pill: [LABEL] value ─────────────────
    private void drawInfoPill(Graphics2D g, String label, String value, int centerX, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fmL = g.getFontMetrics();
        int lw = fmL.stringWidth(label) + 14;

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fmV = g.getFontMetrics();
        int vw = fmV.stringWidth(value);

        int totalW = lw + 8 + vw;
        int x = centerX - totalW / 2;

        // Label background pill
        g.setColor(new java.awt.Color(0x5b8dee));
        g.fill(new java.awt.geom.RoundRectangle2D.Float(x, y - 14, lw, 17, 8, 8));

        // Label text
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.setColor(java.awt.Color.WHITE);
        FontMetrics fmLd = g.getFontMetrics();
        g.drawString(label, x + (lw - fmLd.stringWidth(label)) / 2, y);

        // Value text
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new java.awt.Color(0x1a2540));
        g.drawString(value, x + lw + 8, y);
    }

    // ═══════════════════════════════════════════════════════
    //  QR helpers
    // ═══════════════════════════════════════════════════════
    private BitMatrix encodeQR(String content) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET,    "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN,           2);
        return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 1, 1, hints);
    }

    /** Block-pixel AWT QR — crisp, no interpolation blur */
    private BufferedImage buildAWTQR(String content) throws Exception {
        BitMatrix m = encodeQR(content);
        int mods = m.getWidth(), size = mods * QR_MOD;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        for (int r = 0; r < mods; r++)
            for (int c = 0; c < mods; c++) {
                g.setColor(m.get(c, r) ? java.awt.Color.BLACK : java.awt.Color.WHITE);
                g.fillRect(c * QR_MOD, r * QR_MOD, QR_MOD, QR_MOD);
            }
        g.dispose();
        return img;
    }

    // ── AWT BufferedImage → JavaFX WritableImage ──────────
    private WritableImage awtToFx(BufferedImage awt) {
        int w = awt.getWidth(), h = awt.getHeight();
        WritableImage fx = new WritableImage(w, h);
        PixelWriter pw = fx.getPixelWriter();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int argb = awt.getRGB(x, y);
                int a  = (argb >> 24) & 0xFF;
                int r  = (argb >> 16) & 0xFF;
                int gr = (argb >>  8) & 0xFF;
                int b  =  argb        & 0xFF;
                pw.setColor(x, y, Color.rgb(r, gr, b, a / 255.0));
            }
        return fx;
    }

    // ── Accent / util ────────────────────────────────────
    private String stripAccents(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^\\x00-\\x7F]", "");
    }

    // ═══════════════════════════════════════════════════════
    //  Details overlay — shown when user clicks "Voir les détails"
    // ═══════════════════════════════════════════════════════
    private void showDetailsOverlay(Evenement ev) {
        if (centerStack == null) return;

        Date now      = new Date();
        boolean isTermine = ev.getDateHeureFin() != null && ev.getDateHeureFin().before(now);
        boolean isEncours = ev.getDateHeureDebut() != null && ev.getDateHeureDebut().before(now)
                && ev.getDateHeureFin() != null && ev.getDateHeureFin().after(now);
        String badgeText  = isTermine ? "Terminé" : isEncours ? "En cours" : "À venir";
        String badgeColor = isTermine ? "#6B7280" : isEncours ? "#0284C7"  : "#111827";

        // ── Popup container ───────────────────────────────
        VBox popup = new VBox(0);
        popup.setMaxWidth(500);
        popup.setPrefWidth(500);
        popup.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 40, 0, 0, 12);");

        // ── Coloured top band ─────────────────────────────
        javafx.scene.shape.Rectangle topBand = new javafx.scene.shape.Rectangle(500, 6);
        topBand.setFill(Color.web(badgeColor));
        topBand.setArcWidth(18); topBand.setArcHeight(18);
        popup.getChildren().add(topBand);

        // ── Header: title + close btn ─────────────────────
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(18, 18, 10, 22));

        Label titleLbl = new Label(ev.getTitre() != null ? ev.getTitre() : "Événement");
        titleLbl.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#111827; -fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(360);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label statusPill = new Label(badgeText);
        statusPill.setStyle(
                "-fx-background-color:" + badgeColor + ";" +
                        "-fx-text-fill:white; -fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-background-radius:20; -fx-padding:4 12;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#6B7280;" +
                        "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:0 4;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#111827;" +
                        "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:0 4;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#6B7280;" +
                        "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:0 4;"));
        closeBtn.setOnAction(e -> closeBadgeOverlay());

        headerRow.getChildren().addAll(titleLbl, statusPill, closeBtn);
        popup.getChildren().add(headerRow);

        // ── Thin divider ──────────────────────────────────
        javafx.scene.shape.Rectangle divider = new javafx.scene.shape.Rectangle(500, 1);
        divider.setFill(Color.web("#E5E7EB"));
        popup.getChildren().add(divider);

        // ── Body ─────────────────────────────────────────
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 22, 24, 22));

        // Description block
        String fullDesc = ev.getDescription() != null && !ev.getDescription().isBlank()
                ? ev.getDescription() : "Aucune description disponible.";
        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#6B7280; -fx-padding:0 0 2 0;");
        Label descLbl = new Label(fullDesc);
        descLbl.setStyle("-fx-font-size:14px; -fx-text-fill:#374151; -fx-wrap-text:true; -fx-line-spacing:3;");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(456);

        VBox descBox = new VBox(4, descTitle, descLbl);
        descBox.setStyle(
                "-fx-background-color:#F9FAFB; -fx-background-radius:10;" +
                        "-fx-border-color:#E5E7EB; -fx-border-radius:10; -fx-padding:14 16;");
        body.getChildren().add(descBox);

        // Info grid: date, lieu, horaire, capacité
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(12); infoGrid.setVgap(10);
        infoGrid.setPadding(new Insets(4, 0, 0, 0));

        String dateStr  = ev.getDateEvenement()  != null ? DATE_FMT.format(ev.getDateEvenement())  : "—";
        String debutStr = ev.getDateHeureDebut()  != null ? TIME_FMT.format(ev.getDateHeureDebut()) : "—";
        String finStr   = ev.getDateHeureFin()    != null ? TIME_FMT.format(ev.getDateHeureFin())   : "—";
        String lieu     = ev.getLieu()            != null ? ev.getLieu()                             : "—";
        String maxPart  = String.valueOf(ev.getNbParticipantsMax());

        infoGrid.add(detailInfoBox("📅", "Date",      dateStr),            0, 0);
        infoGrid.add(detailInfoBox("📍", "Lieu",      lieu),               1, 0);
        infoGrid.add(detailInfoBox("🕐", "Horaire",   debutStr + " – " + finStr), 0, 1);
        infoGrid.add(detailInfoBox("👥", "Capacité",  maxPart + " participants max"), 1, 1);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        infoGrid.getColumnConstraints().addAll(cc, new ColumnConstraints() {{ setPercentWidth(50); }});

        body.getChildren().add(infoGrid);

        // Tags row
        HBox tagsRow = new HBox(8);
        tagsRow.setAlignment(Pos.CENTER_LEFT);
        for (String t : new String[]{"📌 Événement", "Entreprise", "Interne"}) {
            Label tag = new Label(t);
            tag.setStyle(
                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:4 10;" +
                            "-fx-background-radius:20; -fx-border-radius:20; -fx-border-width:1.5;" +
                            "-fx-text-fill:#374151; -fx-background-color:#F3F4F6; -fx-border-color:#E5E7EB;");
            tagsRow.getChildren().add(tag);
        }
        body.getChildren().add(tagsRow);

        // Close button
        Button closeBtnBottom = new Button("Fermer");
        closeBtnBottom.setMaxWidth(Double.MAX_VALUE);
        closeBtnBottom.setStyle(
                "-fx-background-color:#111827; -fx-text-fill:white;" +
                        "-fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-padding:11 0; -fx-cursor:hand;");
        closeBtnBottom.setOnMouseEntered(e -> closeBtnBottom.setStyle(
                "-fx-background-color:#1F2937; -fx-text-fill:white;" +
                        "-fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-padding:11 0; -fx-cursor:hand;"));
        closeBtnBottom.setOnMouseExited(e -> closeBtnBottom.setStyle(
                "-fx-background-color:#111827; -fx-text-fill:white;" +
                        "-fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-padding:11 0; -fx-cursor:hand;"));
        closeBtnBottom.setOnAction(e -> closeBadgeOverlay());
        body.getChildren().add(closeBtnBottom);

        popup.getChildren().add(body);

        // ── Overlay dimmer ────────────────────────────────
        Pane dimmer = new Pane();
        dimmer.setStyle("-fx-background-color: rgba(0,0,0,0.50);");
        dimmer.setPickOnBounds(true);
        dimmer.setOnMouseClicked(e -> closeBadgeOverlay());

        StackPane layer = new StackPane(dimmer, popup);
        StackPane.setAlignment(popup, Pos.CENTER);
        layer.setPickOnBounds(false);
        centerStack.getChildren().add(layer);
    }

    /** Small info box used inside the details popup grid */
    private VBox detailInfoBox(String icon, String label, String value) {
        Label lbl = new Label(icon + "  " + label);
        lbl.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#9CA3AF;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:13px; -fx-text-fill:#111827; -fx-wrap-text:true;");
        val.setWrapText(true);
        VBox box = new VBox(3, lbl, val);
        box.setStyle(
                "-fx-background-color:#F9FAFB; -fx-background-radius:8;" +
                        "-fx-border-color:#E5E7EB; -fx-border-radius:8; -fx-padding:10 12;");
        return box;
    }

    private void closeBadgeOverlay() {
        if (centerStack != null && centerStack.getChildren().size() > 1)
            centerStack.getChildren().remove(centerStack.getChildren().size() - 1);
    }

    @FXML void handleSearch(KeyEvent event) { filterAndDisplayEvents(); }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }
}