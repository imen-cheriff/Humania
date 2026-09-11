package planification.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import planification.models.Espace;
import planification.models.Reunion;
import planification.services.ServiceEspace;
import planification.services.ServiceReunion;
import planification.services.ZoomService;
import planification.services.ResendService;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

public class AjouterReuCont implements Initializable {

    @FXML private Button   btnAnnuler;
    @FXML private Button   btnEnregistrer;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextArea  descriptionArea;
    @FXML private CheckBox  enLigneCheckBox;
    @FXML private TextField heureDebutField;
    @FXML private TextField heureFinField;
    @FXML private TextField participantsField;
    @FXML private ComboBox<String> salleComboBox;
    @FXML private TextField titreField;

    private final ServiceReunion serviceReunion = new ServiceReunion();
    private final ServiceEspace  serviceEspace  = new ServiceEspace();
    private final ZoomService    zoomService    = new ZoomService();
    private final ResendService  resendService  = new ResendService();

    private final Map<String, Integer> salleMap = new LinkedHashMap<>();

    /** Réunion en cours d'édition (null = création) */
    private Reunion currentReunion;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSalles();
        enLigneCheckBox.selectedProperty().addListener((obs, oldV, newV) -> updateSalleEnabledState());
        updateSalleEnabledState();
        btnAnnuler.setOnAction(e -> closeWindow());
    }

    // ── Public setters (called from calendar) ────────────────────────────

    public void prefillDateTime(LocalDateTime dt) {
        if (dt == null) return;
        LocalDate d = dt.toLocalDate();
        dateDebutPicker.setValue(d);
        dateFinPicker.setValue(d);
        heureDebutField.setText(String.format("%02d:%02d", dt.getHour(), dt.getMinute()));
        heureFinField.setText(String.format("%02d:%02d", Math.min(dt.getHour() + 1, 23), dt.getMinute()));
    }

    public void setReunion(Reunion reunion) {
        this.currentReunion = reunion;
        if (reunion == null) return;

        titreField.setText(reunion.getTitre());
        descriptionArea.setText(reunion.getDescription());

        if (reunion.getDateHeureDebut() != null) {
            LocalDateTime deb = LocalDateTime.ofInstant(
                    reunion.getDateHeureDebut().toInstant(), ZoneId.systemDefault());
            dateDebutPicker.setValue(deb.toLocalDate());
            heureDebutField.setText(deb.toLocalTime().format(TIME_FMT));
        }
        if (reunion.getDateHeureFin() != null) {
            LocalDateTime fin = LocalDateTime.ofInstant(
                    reunion.getDateHeureFin().toInstant(), ZoneId.systemDefault());
            dateFinPicker.setValue(fin.toLocalDate());
            heureFinField.setText(fin.toLocalTime().format(TIME_FMT));
        }

        enLigneCheckBox.setSelected(reunion.isEnLigne());

        if (!reunion.isEnLigne() && reunion.getIdSalle() > 0) {
            salleMap.entrySet().stream()
                    .filter(e -> e.getValue() == reunion.getIdSalle())
                    .findFirst()
                    .ifPresent(e -> salleComboBox.getSelectionModel().select(e.getKey()));
        }

        participantsField.setText(reunion.getParticipants());
        updateSalleEnabledState();
    }

    // ── FXML handlers ────────────────────────────────────────────────────

    @FXML
    void handleEnLigneToggle(ActionEvent event) {
        updateSalleEnabledState();
    }

    @FXML
    void handleSave(ActionEvent event) {
        // Basic validation
        try {
            validateForm();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", ex.getMessage());
            return;
        }

        // Build the Reunion object from the form
        Reunion r = buildReunionFromForm();

        boolean isNew     = (currentReunion == null);
        boolean enLigne   = enLigneCheckBox.isSelected();
        boolean hadZoom   = !isNew && r.getZoomMeetingId() != 0;
        boolean needsZoom = enLigne;

        // ── Case 1: online meeting → call Zoom API then save ─────────────
        if (needsZoom) {
            btnEnregistrer.setDisable(true);
            btnEnregistrer.setText("Création Zoom…");

            Task<ZoomService.ZoomMeetingResult> zoomTask = new Task<>() {
                @Override
                protected ZoomService.ZoomMeetingResult call() throws Exception {
                    // If editing and already had a Zoom meeting, delete the old one first
                    if (hadZoom) {
                        try { zoomService.deleteMeeting(r.getZoomMeetingId()); }
                        catch (Exception ignored) { /* old meeting gone, not critical */ }
                    }
                    return zoomService.createMeeting(
                            r.getTitre(),
                            r.getDateHeureDebut(),
                            r.getDateHeureFin(),
                            r.getDescription());
                }
            };

            zoomTask.setOnSucceeded(ev -> {
                ZoomService.ZoomMeetingResult zoom = zoomTask.getValue();
                r.setZoomMeetingId(zoom.meetingId);
                r.setZoomJoinUrl(zoom.joinUrl);
                r.setZoomStartUrl(zoom.startUrl);
                r.setZoomPassword(zoom.password);

                saveReunion(r, isNew);

                btnEnregistrer.setDisable(false);
                btnEnregistrer.setText("Enregistrer");

                // Send invitation emails in background (non-blocking)
                List<String> emails = parseEmails(r.getParticipants());
                if (!emails.isEmpty()) {
                    sendInvitationEmails(emails, r, zoom);
                }

                // Show the links dialog BEFORE closing
                showZoomLinksDialog(zoom, r.getTitre());
            });

            zoomTask.setOnFailed(ev -> {
                btnEnregistrer.setDisable(false);
                btnEnregistrer.setText("Enregistrer");

                Throwable ex = zoomTask.getException();
                boolean saveAnyway = confirmDialog(
                        "Zoom indisponible",
                        "Impossible de créer la réunion Zoom :\n" + ex.getMessage()
                                + "\n\nVoulez-vous enregistrer la réunion sans lien Zoom ?");
                if (saveAnyway) {
                    saveReunion(r, isNew);
                    closeWindow();
                }
            });

            new Thread(zoomTask).start();

        } else {
            // ── Case 2: in-person meeting, just save ─────────────────────
            // If switching from online to in-person, clean up old Zoom meeting
            if (hadZoom) {
                new Thread(() -> {
                    try { zoomService.deleteMeeting(r.getZoomMeetingId()); }
                    catch (Exception ignored) {}
                }).start();
                r.setZoomMeetingId(0);
                r.setZoomJoinUrl(null);
                r.setZoomStartUrl(null);
                r.setZoomPassword(null);
            }
            saveReunion(r, isNew);
            closeWindow();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private void loadSalles() {
        salleComboBox.getItems().clear();
        salleMap.clear();
        try {
            for (Espace e : serviceEspace.getAll()) {
                String nom = e.getNom() != null ? e.getNom() : "Salle " + e.getId();
                salleMap.put(nom, e.getId());
                salleComboBox.getItems().add(nom);
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les salles : " + ex.getMessage());
        }
    }

    private void updateSalleEnabledState() {
        boolean enLigne = enLigneCheckBox.isSelected();
        salleComboBox.setDisable(enLigne);
        if (enLigne) salleComboBox.getSelectionModel().clearSelection();
    }

    /** Validates all form fields. Throws IllegalArgumentException with a user-facing message. */
    private void validateForm() {
        if (titreField.getText() == null || titreField.getText().trim().isEmpty())
            throw new IllegalArgumentException("Le titre est obligatoire.");

        if (dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null)
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");

        if (heureDebutField.getText() == null || heureDebutField.getText().isBlank()
                || heureFinField.getText() == null || heureFinField.getText().isBlank())
            throw new IllegalArgumentException("Les heures de début et de fin sont obligatoires (HH:mm).");

        try {
            LocalTime.parse(heureDebutField.getText().trim(), TIME_FMT);
            LocalTime.parse(heureFinField.getText().trim(), TIME_FMT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Le format des heures doit être HH:mm (ex: 09:30).");
        }

        LocalDate  dDeb  = dateDebutPicker.getValue();
        LocalDate  dFin  = dateFinPicker.getValue();
        LocalTime  tDeb  = LocalTime.parse(heureDebutField.getText().trim(), TIME_FMT);
        LocalTime  tFin  = LocalTime.parse(heureFinField.getText().trim(), TIME_FMT);
        LocalDateTime ldtFin = LocalDateTime.of(dFin, tFin);
        LocalDateTime ldtDeb = LocalDateTime.of(dDeb, tDeb);
        if (ldtFin.isBefore(ldtDeb))
            throw new IllegalArgumentException("La date/heure de fin doit être postérieure à celle de début.");

        boolean enLigne = enLigneCheckBox.isSelected();
        if (!enLigne && salleComboBox.getValue() == null)
            throw new IllegalArgumentException("Sélectionnez une salle ou cochez 'Réunion en ligne'.");
    }

    /** Reads all form fields and returns a populated Reunion (not yet persisted). */
    private Reunion buildReunionFromForm() {
        LocalDate dDeb = dateDebutPicker.getValue();
        LocalDate dFin = dateFinPicker.getValue();
        LocalTime tDeb = LocalTime.parse(heureDebutField.getText().trim(), TIME_FMT);
        LocalTime tFin = LocalTime.parse(heureFinField.getText().trim(), TIME_FMT);

        Date dateDebut = Date.from(LocalDateTime.of(dDeb, tDeb).atZone(ZoneId.systemDefault()).toInstant());
        Date dateFin   = Date.from(LocalDateTime.of(dFin, tFin).atZone(ZoneId.systemDefault()).toInstant());

        boolean enLigne = enLigneCheckBox.isSelected();
        int     idSalle = 0;
        if (!enLigne) idSalle = salleMap.getOrDefault(salleComboBox.getValue(), 0);

        Reunion r = (currentReunion != null) ? currentReunion : new Reunion();
        r.setTitre(titreField.getText().trim());
        r.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");
        r.setDateHeureDebut(dateDebut);
        r.setDateHeureFin(dateFin);
        r.setIdSalle(idSalle);
        r.setParticipants(participantsField.getText() != null ? participantsField.getText().trim() : "");
        r.setEnLigne(enLigne);

        if (r.getNomOrganisateur() == null || r.getNomOrganisateur().isBlank())
            r.setNomOrganisateur("Organisateur inconnu");
        if (r.getEmailOrganisateur() == null || r.getEmailOrganisateur().isBlank())
            r.setEmailOrganisateur("unknown@example.com");
        if (r.getCreeLe() == null)
            r.setCreeLe(new Date());

        return r;
    }

    /** Persists the reunion (add or update) and shows a success/error alert. */
    private void saveReunion(Reunion r, boolean isNew) {
        try {
            if (isNew) {
                serviceReunion.add(r);
            } else {
                serviceReunion.update(r);
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue lors de l'enregistrement : " + ex.getMessage());
        }
    }

    /**
     * Shows a styled dialog with the Zoom join link, start (host) link, and password.
     * Each link has a copy button.  The window is closed after the user dismisses this dialog.
     */
    private void showZoomLinksDialog(ZoomService.ZoomMeetingResult zoom, String title) {
        Stage dialog = new Stage();
        dialog.setTitle("Réunion Zoom créée ✓");
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: white;");
        root.setPrefWidth(480);

        // Header
        Label header = new Label("🎉  Réunion Zoom créée avec succès !");
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        header.setWrapText(true);

        Label subheader = new Label("Partagez ces liens avec les participants de « " + title + " »");
        subheader.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        subheader.setWrapText(true);

        root.getChildren().addAll(header, subheader);

        // Divider
        Region div = new Region();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: #e2e8f0;");
        root.getChildren().add(div);

        // Join link row
        root.getChildren().add(linkRow(
                "🔗  Lien participant (Join URL)",
                zoom.joinUrl,
                "#22c55e"));

        // Start link row
        root.getChildren().add(linkRow(
                "🎙  Lien hôte (Start URL)",
                zoom.startUrl,
                "#3b82f6"));

        // Password row
        if (zoom.password != null && !zoom.password.isEmpty()) {
            root.getChildren().add(linkRow(
                    "🔒  Mot de passe",
                    zoom.password,
                    "#f59e0b"));
        }

        // Meeting ID row
        root.getChildren().add(linkRow(
                "🆔  ID de réunion",
                String.valueOf(zoom.meetingId),
                "#8b5cf6"));

        // Close button
        Region spacer = new Region();
        spacer.setPrefHeight(4);
        root.getChildren().add(spacer);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 8 24; -fx-cursor: hand;");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> {
            dialog.close();
            closeWindow();   // also close the form
        });
        root.getChildren().add(closeBtn);

        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }

    /** Builds one labelled row with a read-only text field and a copy button. */
    private VBox linkRow(String labelText, String value, String accentColor) {
        VBox row = new VBox(4);

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        TextField tf = new TextField(value);
        tf.setEditable(false);
        tf.setStyle("-fx-background-color: #f8fafc; -fx-border-color: " + accentColor + "55; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-font-size: 12px; -fx-padding: 6 10; -fx-text-fill: #1e293b;");

        Button copyBtn = new Button("Copier");
        copyBtn.setStyle("-fx-background-color: " + accentColor + "22; " +
                "-fx-text-fill: " + accentColor + "; -fx-font-size: 11px; " +
                "-fx-background-radius: 6; -fx-border-color: " + accentColor + "55; " +
                "-fx-border-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
        copyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(value);
            Clipboard.getSystemClipboard().setContent(content);
            copyBtn.setText("✓ Copié");
            copyBtn.setDisable(true);
        });

        HBox fieldRow = new HBox(8, tf, copyBtn);
        HBox.setHgrow(tf, Priority.ALWAYS);
        fieldRow.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(lbl, fieldRow);
        return row;
    }

    /** Shows a yes/no confirmation dialog, returns true if the user clicked OK. */
    private boolean confirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType yes = new ButtonType("Oui, enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Annuler",          ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    /**
     * Parses a comma/semicolon-separated string of emails into a clean list.
     * e.g. "alice@example.com, bob@example.com" -> ["alice@example.com", "bob@example.com"]
     */
    private List<String> parseEmails(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split("[,;\s]+")) {
            String email = part.trim();
            if (!email.isEmpty() && email.contains("@")) {
                result.add(email);
            }
        }
        return result;
    }

    /**
     * Sends Zoom invitation emails to all participants on a background thread.
     * Shows a small status notification when done (success or failure).
     */
    private void sendInvitationEmails(List<String> emails, Reunion r,
                                      ZoomService.ZoomMeetingResult zoom) {
        Task<Void> emailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                resendService.sendMeetingInvitation(
                        emails,
                        r.getTitre(),
                        r.getDateHeureDebut(),
                        r.getDateHeureFin(),
                        zoom,
                        r.getNomOrganisateur());
                return null;
            }
        };

        emailTask.setOnSucceeded(ev -> {
            int count = emails.size();
            showToast("\u2709\uFE0F  " + count + " invitation" + (count > 1 ? "s" : "")
                    + " envoy\u00e9e" + (count > 1 ? "s" : "") + " avec succ\u00e8s !");
        });

        emailTask.setOnFailed(ev -> {
            Throwable ex = emailTask.getException();
            showAlert(Alert.AlertType.WARNING, "Emails non envoy\u00e9s",
                    "La r\u00e9union a \u00e9t\u00e9 cr\u00e9\u00e9e mais les emails n'ont pas pu \u00eatre envoy\u00e9s :\n"
                            + ex.getMessage());
        });

        new Thread(emailTask).start();
    }

    /**
     * Shows a brief non-blocking toast notification in the bottom-left of the form.
     */
    private void showToast(String message) {
        Platform.runLater(() -> {
            Tooltip toast = new Tooltip(message);
            toast.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; "
                    + "-fx-font-size: 12px; -fx-background-radius: 6; -fx-padding: 8 14;");
            // Show near the save button for 3 seconds
            if (btnEnregistrer.getScene() != null) {
                javafx.geometry.Bounds b = btnEnregistrer.localToScreen(btnEnregistrer.getBoundsInLocal());
                if (b != null) {
                    toast.show(btnEnregistrer.getScene().getWindow(), b.getMinX(), b.getMaxY() + 8);
                    new Thread(() -> {
                        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                        Platform.runLater(toast::hide);
                    }).start();
                }
            }
        });
    }

    private void closeWindow() {
        Platform.runLater(() -> {
            if (btnAnnuler == null) return;
            Scene scene = btnAnnuler.getScene();
            if (scene == null) return;
            Stage stage = (Stage) scene.getWindow();
            if (stage != null) stage.close();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}