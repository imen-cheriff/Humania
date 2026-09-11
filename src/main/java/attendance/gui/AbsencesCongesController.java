package attendance.gui;

import attendance.interfaces.service;
import attendance.models.*;
import attendance.services.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class AbsencesCongesController {


    @FXML private VBox viewMesAbsences;
    @FXML private VBox viewMesConges;

    @FXML private ComboBox<String> comboHeureDebut;
    @FXML private ComboBox<String> comboMinuteDebut;
    @FXML private ComboBox<String> comboHeureFin;
    @FXML private ComboBox<String> comboMinuteFin;
    @FXML private TextField txtDureeHeures;

    @FXML private TextField    txtRechercheAbsence;
    @FXML private ComboBox<String> comboStatutAbsence;
    @FXML private ComboBox<String> comboTypeAbsence;
    @FXML private VBox         containerAbsences;
    @FXML private VBox         emptyStateAbsences;
    @FXML private VBox         formAbsence;
    @FXML private DatePicker   dpDebutAbsence;
    @FXML private DatePicker   dpFinAbsence;
    @FXML private TextField    txtNbrJoursAbsence;
    @FXML private ComboBox<String> comboStatutFormAbsence;
    @FXML private ComboBox<String> comboTypeFormAbsence;
    @FXML private TextField    txtMotifAbsence;
    private Absence absenceEnEdition = null;


    @FXML private TextField    txtRechercheConge;
    @FXML private ComboBox<String> comboStatutConge;
    @FXML private ComboBox<String> comboTypeCongeFiltre;
    @FXML private VBox         containerConges;
    @FXML private VBox         emptyStateConges;
    @FXML private VBox         formConge;
    @FXML private DatePicker   dpDebutConge;
    @FXML private DatePicker   dpFinConge;
    @FXML private TextField    txtNbrJoursConge;
    @FXML private ComboBox<String> comboStatutFormConge;
    @FXML private ComboBox<String> comboTypeFormConge;
    @FXML private TextField    txtMotifConge;
    private Conge congeEnEdition = null;


    private final service<Absence>     serviceAbsence = new ServiceAbsence();
    private final service<Conge>       serviceConge   = new ServiceConge();
    private final service<TypeAbsence> serviceTA      = new ServiceTypeAbsence();
    private final service<TypeConge>   serviceTC      = new ServiceTypeConge();
    private final service<Utilisateur> serviceUtilisateur = new ServiceUtilisateur();


    @FXML
    public void initialize() {
        initFiltresAbsences();
        initFiltresConges();
        try {
            java.sql.DatabaseMetaData meta = utils.MyDataBase.getInstance().getCnx().getMetaData();
            java.sql.ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            System.out.println("📋 TABLES :");
            while (tables.next()) System.out.println("  → " + tables.getString("TABLE_NAME"));
        } catch (Exception ex) { ex.printStackTrace(); }
        afficherAbsences();
    }

    private void initFiltresAbsences() {
        if (comboStatutAbsence != null) {
            comboStatutAbsence.getItems().addAll("Tous les statuts", "En attente", "Approuvé", "Refusé");
            comboStatutAbsence.setValue("Tous les statuts");
            comboStatutAbsence.setOnAction(e -> filtrerAbsences());
        }
        if (comboTypeAbsence != null) {
            comboTypeAbsence.getItems().add("Tous les types");
            try { serviceTA.getAll().forEach(ta -> comboTypeAbsence.getItems().add(ta.getLibelle())); }
            catch (Exception ignored) { comboTypeAbsence.getItems().addAll("Médicale", "Administrative", "Personnelle"); }
            comboTypeAbsence.setValue("Tous les types");
            comboTypeAbsence.setOnAction(e -> filtrerAbsences());
        }
        if (txtRechercheAbsence != null)
            txtRechercheAbsence.textProperty().addListener((o, v, n) -> filtrerAbsences());

        if (comboStatutFormAbsence != null) {
            comboStatutFormAbsence.getItems().addAll("En attente", "Approuvé", "Refusé");
            comboStatutFormAbsence.setValue("En attente");
        }
        if (comboTypeFormAbsence != null) {
            comboTypeFormAbsence.getItems().add("-- Sélectionner --");
            try { serviceTA.getAll().forEach(ta -> comboTypeFormAbsence.getItems().add(ta.getLibelle())); }
            catch (Exception ignored) { comboTypeFormAbsence.getItems().addAll("Médicale", "Administrative", "Personnelle"); }
            comboTypeFormAbsence.setValue("-- Sélectionner --");
        }

        initComboHeures();
        bloquerDatesPasteesAbsence();
    }

    private void initFiltresConges() {
        if (comboStatutConge != null) {
            comboStatutConge.getItems().addAll("Tous les statuts", "En attente", "Approuvé", "Refusé");
            comboStatutConge.setValue("Tous les statuts");
            comboStatutConge.setOnAction(e -> filtrerConges());
        }
        if (comboTypeCongeFiltre != null) {
            comboTypeCongeFiltre.getItems().add("Tous les types");
            try { serviceTC.getAll().forEach(tc -> comboTypeCongeFiltre.getItems().add(tc.getLibelle())); }
            catch (Exception ignored) { comboTypeCongeFiltre.getItems().addAll("Congés Payés", "RTT", "Sans Solde"); }
            comboTypeCongeFiltre.setValue("Tous les types");
            comboTypeCongeFiltre.setOnAction(e -> filtrerConges());
        }
        if (txtRechercheConge != null)
            txtRechercheConge.textProperty().addListener((o, v, n) -> filtrerConges());

        if (comboStatutFormConge != null) {
            comboStatutFormConge.getItems().addAll("En attente", "Approuvé", "Refusé");
            comboStatutFormConge.setValue("En attente");
        }
        if (comboTypeFormConge != null) {
            comboTypeFormConge.getItems().add("-- Sélectionner --");
            try { serviceTC.getAll().forEach(tc -> comboTypeFormConge.getItems().add(tc.getLibelle())); }
            catch (Exception ignored) { comboTypeFormConge.getItems().addAll("Congés Payés", "RTT", "Sans Solde"); }
            comboTypeFormConge.setValue("-- Sélectionner --");
        }

        bloquerDatesPasteesConge();
    }

    private void initComboHeures() {
        if (comboHeureDebut != null) {
            comboHeureDebut.getItems().clear();
            for (int h = 8; h <= 18; h++) comboHeureDebut.getItems().add(String.format("%02d", h));
            comboHeureDebut.setMouseTransparent(false);
            comboHeureDebut.setDisable(false);
        }
        if (comboHeureFin != null) {
            comboHeureFin.getItems().clear();
            for (int h = 8; h <= 18; h++) comboHeureFin.getItems().add(String.format("%02d", h));
            comboHeureFin.setMouseTransparent(false);
            comboHeureFin.setDisable(false);
        }
        if (comboMinuteDebut != null) {
            comboMinuteDebut.getItems().clear();
            comboMinuteDebut.getItems().addAll("00", "15", "30", "45");
            comboMinuteDebut.setMouseTransparent(false);
            comboMinuteDebut.setDisable(false);
        }
        if (comboMinuteFin != null) {
            comboMinuteFin.getItems().clear();
            comboMinuteFin.getItems().addAll("00", "15", "30", "45");
            comboMinuteFin.setMouseTransparent(false);
            comboMinuteFin.setDisable(false);
        }

        if (comboHeureDebut  != null) comboHeureDebut.setOnAction(e -> calculerDuree());
        if (comboMinuteDebut != null) comboMinuteDebut.setOnAction(e -> calculerDuree());
        if (comboHeureFin    != null) comboHeureFin.setOnAction(e -> calculerDuree());
        if (comboMinuteFin   != null) comboMinuteFin.setOnAction(e -> calculerDuree());
    }

    private void calculerDuree() {
        try {
            if (comboHeureDebut == null || comboMinuteDebut == null ||
                    comboHeureFin == null || comboMinuteFin == null || txtDureeHeures == null) return;

            String hd = comboHeureDebut.getValue();
            String md = comboMinuteDebut.getValue();
            String hf = comboHeureFin.getValue();
            String mf = comboMinuteFin.getValue();

            if (hd == null || md == null || hf == null || mf == null) return;

            int minutesDebut = Integer.parseInt(hd) * 60 + Integer.parseInt(md);
            int minutesFin   = Integer.parseInt(hf) * 60 + Integer.parseInt(mf);
            int dureeMinutes = minutesFin - minutesDebut;

            if (dureeMinutes <= 0) {
                txtDureeHeures.setText("❌ Invalide");
                txtDureeHeures.setStyle("-fx-padding: 8px; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 2; -fx-background-radius: 6; -fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                return;
            }

            int heures  = dureeMinutes / 60;
            int minutes = dureeMinutes % 60;
            String dureeStr = heures > 0 ? heures + "h" + (minutes > 0 ? String.format("%02d", minutes) : "") : minutes + "min";

            if (dureeMinutes < 30) {
                txtDureeHeures.setText("⚠️ " + dureeStr + " (min 30min)");
                txtDureeHeures.setStyle("-fx-padding: 8px; -fx-border-color: #f59e0b; -fx-border-radius: 6; -fx-border-width: 2; -fx-background-radius: 6; -fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold;");
            } else if (dureeMinutes > 240) {
                txtDureeHeures.setText("⚠️ " + dureeStr + " (max 4h)");
                txtDureeHeures.setStyle("-fx-padding: 8px; -fx-border-color: #f59e0b; -fx-border-radius: 6; -fx-border-width: 2; -fx-background-radius: 6; -fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold;");
            } else {
                txtDureeHeures.setText("✓ " + dureeStr);
                txtDureeHeures.setStyle("-fx-padding: 8px; -fx-border-color: #10b981; -fx-border-radius: 6; -fx-border-width: 2; -fx-background-radius: 6; -fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold;");
            }
        } catch (Exception ignored) {}
    }

    private void bloquerDatesPasteesAbsence() {
        if (dpDebutAbsence != null) {
            dpDebutAbsence.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date.isBefore(LocalDate.now())) { setDisable(true); setStyle("-fx-background-color: #fecaca;"); }
                }
            });
        }
        if (dpFinAbsence != null) {
            dpFinAbsence.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date.isBefore(LocalDate.now())) { setDisable(true); setStyle("-fx-background-color: #fecaca;"); }
                    LocalDate dateDebut = dpDebutAbsence != null ? dpDebutAbsence.getValue() : null;
                    if (dateDebut != null && date.isBefore(dateDebut)) { setDisable(true); setStyle("-fx-background-color: #fecaca;"); }
                }
            });
        }
    }

    private void bloquerDatesPasteesConge() {
        if (dpDebutConge != null) {
            dpDebutConge.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date.isBefore(LocalDate.now())) { setDisable(true); setStyle("-fx-background-color: #fecaca;"); }
                }
            });
            dpDebutConge.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && dpFinConge != null && dpFinConge.getValue() != null
                        && dpFinConge.getValue().isBefore(newVal)) {
                    dpFinConge.setValue(null);
                }
                recalculerJoursConge();
            });
        }

        if (dpFinConge != null) {
            dpFinConge.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date.isBefore(LocalDate.now())) { setDisable(true); setStyle("-fx-background-color: #fecaca;"); }
                    LocalDate dateDebut = dpDebutConge != null ? dpDebutConge.getValue() : null;
                    if (dateDebut != null && date.isBefore(dateDebut)) { setDisable(true); setStyle("-fx-background-color: #fecaca;"); }
                }
            });
            dpFinConge.valueProperty().addListener((obs, oldVal, newVal) -> recalculerJoursConge());
        }

        if (txtNbrJoursConge != null) {
            txtNbrJoursConge.setEditable(false);
            txtNbrJoursConge.setStyle(
                    "-fx-padding: 9px 12px; -fx-border-color: #d1fae5; -fx-border-radius: 8;" +
                            "-fx-border-width: 1; -fx-background-radius: 8; -fx-font-size: 13px;" +
                            "-fx-font-weight: bold; -fx-background-color: #f0fdf4; -fx-text-fill: #059669;"
            );
        }
    }

    private void recalculerJoursConge() {
        if (txtNbrJoursConge == null || dpDebutConge == null || dpFinConge == null) return;

        LocalDate debut = dpDebutConge.getValue();
        LocalDate fin   = dpFinConge.getValue();

        if (debut == null || fin == null) {
            txtNbrJoursConge.setText("");
            txtNbrJoursConge.setPromptText("Sera calcule automatiquement");
            return;
        }

        if (fin.isBefore(debut)) {
            txtNbrJoursConge.setText("Invalide");
            txtNbrJoursConge.setStyle(
                    "-fx-padding: 9px 12px; -fx-border-color: #ef4444; -fx-border-radius: 8;" +
                            "-fx-border-width: 2; -fx-background-radius: 8; -fx-font-size: 13px;" +
                            "-fx-font-weight: bold; -fx-background-color: #fee2e2; -fx-text-fill: #ef4444;"
            );
            return;
        }

        long jours = debut.until(fin, java.time.temporal.ChronoUnit.DAYS) + 1;
        txtNbrJoursConge.setText(jours + " jour" + (jours > 1 ? "s" : ""));
        txtNbrJoursConge.setStyle(
                "-fx-padding: 9px 12px; -fx-border-color: #d1fae5; -fx-border-radius: 8;" +
                        "-fx-border-width: 1; -fx-background-radius: 8; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-background-color: #f0fdf4; -fx-text-fill: #059669;"
        );
    }


    public void afficherAbsences() {
        if (viewMesAbsences != null) { viewMesAbsences.setVisible(true); viewMesAbsences.setManaged(true); }
        if (viewMesConges   != null) { viewMesConges.setVisible(false);  viewMesConges.setManaged(false); }
        chargerAbsences();
    }

    public void afficherConges() {
        if (viewMesConges   != null) { viewMesConges.setVisible(true);   viewMesConges.setManaged(true); }
        if (viewMesAbsences != null) { viewMesAbsences.setVisible(false); viewMesAbsences.setManaged(false); }
        chargerConges();
    }


    // ──────────────────────────────────────────────────────────
    //  ABSENCES – CRUD
    // ──────────────────────────────────────────────────────────

    private void chargerAbsences() { filtrerAbsences(); }

    private void filtrerAbsences() {
        if (containerAbsences == null) return;
        containerAbsences.getChildren().clear();

        List<Absence> liste = getAbsencesFiltrees();

        if (liste.isEmpty()) {
            if (emptyStateAbsences != null) containerAbsences.getChildren().add(emptyStateAbsences);
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            liste.forEach(a -> containerAbsences.getChildren().add(creerCarteAbsence(a, fmt)));
        }
    }

    private List<Absence> getAbsencesFiltrees() {
        List<Absence> liste;
        try { liste = serviceAbsence.getAll(); } catch (Exception e) { return Collections.emptyList(); }

        // ── Filtre par utilisateur connecté (miroir de AbsenceController.php : WHERE a.utilisateur_id = :uid)
        int currentUserId = getUserIdConnecte();
        if (currentUserId > 0) {
            final int uid = currentUserId;
            liste = liste.stream()
                    .filter(a -> a.getUtilisateurId() == uid)
                    .collect(Collectors.toList());
        }

        liste = liste.stream().filter(a -> !"Archivé".equalsIgnoreCase(a.getStatut())).collect(Collectors.toList());

        String statut = comboStatutAbsence != null ? comboStatutAbsence.getValue() : null;
        if (statut != null && !statut.equals("Tous les statuts"))
            liste = liste.stream().filter(a -> statut.equalsIgnoreCase(a.getStatut())).collect(Collectors.toList());

        String type = comboTypeAbsence != null ? comboTypeAbsence.getValue() : null;
        if (type != null && !type.equals("Tous les types")) {
            liste = liste.stream().filter(a -> {
                try { return serviceTA.getAll().stream().filter(x -> x.getId() == a.getTypeAbsenceId()).anyMatch(x -> x.getLibelle().equalsIgnoreCase(type)); }
                catch (Exception ex) { return true; }
            }).collect(Collectors.toList());
        }
        String q = txtRechercheAbsence != null ? txtRechercheAbsence.getText().trim().toLowerCase() : "";
        if (!q.isEmpty())
            liste = liste.stream().filter(a -> a.getStatut() != null && a.getStatut().toLowerCase().contains(q)).collect(Collectors.toList());

        return liste;
    }

    private HBox creerCarteAbsence(Absence a, DateTimeFormatter fmt) {

        String accentColor = getAccentColor(a.getStatut());

        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #eef2ff;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(100,116,234,0.08), 12, 0, 0, 3);"
        );

        Region accentBar = new Region();
        accentBar.setPrefWidth(4);
        accentBar.setMinWidth(4);
        accentBar.setMaxHeight(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 12 0 0 12;");

        String iconeBg = getIconeBg(a.getStatut());
        Label ico = new Label(getIconeStatut(a.getStatut()));
        ico.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-padding: 10px;" +
                        "-fx-background-color: " + iconeBg + ";" +
                        "-fx-background-radius: 50;"
        );
        HBox icoWrapper = new HBox(ico);
        icoWrapper.setAlignment(Pos.CENTER);
        icoWrapper.setPadding(new Insets(14, 10, 14, 16));

        String typeNom = "Absence";
        try {
            typeNom = serviceTA.getAll().stream()
                    .filter(x -> x.getId() == a.getTypeAbsenceId())
                    .map(TypeAbsence::getLibelle).findFirst().orElse("Absence");
        } catch (Exception ignored) {}

        int dureeMinutes = a.getNbrJours();
        String dureeTexte;
        if (dureeMinutes < 60) {
            dureeTexte = dureeMinutes + " min";
        } else {
            int h = dureeMinutes / 60, m = dureeMinutes % 60;
            dureeTexte = m == 0 ? h + "h" : h + "h" + String.format("%02d", m);
        }

        String motif = a.getMotif() != null && !a.getMotif().isBlank() ? a.getMotif() : "";
        String heureDebut = a.getHeureDebut() != null ? a.getHeureDebut() : "";
        String heureFin   = a.getHeureFin()   != null ? a.getHeureFin()   : "";
        String horaire    = (!heureDebut.isEmpty() && !heureFin.isEmpty()) ? heureDebut + " – " + heureFin : "";

        VBox infos = new VBox(5);
        HBox.setHgrow(infos, Priority.ALWAYS);
        infos.setPadding(new Insets(14, 14, 14, 6));

        Label nom = new Label(typeNom);
        nom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        String d1 = a.getDateDebut() != null ? a.getDateDebut().format(fmt) : "?";

        HBox metaRow = new HBox(14);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label lblDate = creerMeta("📅", d1);
        Label lblHoraire = horaire.isEmpty() ? null : creerMeta("⏰", horaire);
        Label lblDuree  = creerMeta("⌛", dureeTexte);

        metaRow.getChildren().addAll(lblDate, lblDuree);
        if (lblHoraire != null) metaRow.getChildren().add(1, lblHoraire);

        infos.getChildren().addAll(nom, metaRow);

        if (!motif.isEmpty()) {
            Label lblMotif = new Label("💬 " + motif);
            lblMotif.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");
            infos.getChildren().add(lblMotif);
        }

        Label badge = new Label(a.getStatut() != null ? a.getStatut().toUpperCase() : "?");
        badge.setStyle(getBadgeStyleV2(a.getStatut()));
        VBox badgeWrapper = new VBox(badge);
        badgeWrapper.setAlignment(Pos.CENTER);
        badgeWrapper.setPadding(new Insets(0, 16, 0, 0));

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setPadding(new Insets(12, 8, 12, 8));
        sep.setStyle("-fx-opacity: 0.3;");

        Button btnEdit = new Button("✏️  Modifier");
        btnEdit.setStyle(
                "-fx-background-color: #eef2ff;" +
                        "-fx-text-fill: #667eea;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 16px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: #c7d2fe;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;"
        );
        btnEdit.setOnAction(e -> modifierAbsence(a));

        Button btnDel = new Button("📦  Archiver");
        btnDel.setStyle(
                "-fx-background-color: #fff1f2;" +
                        "-fx-text-fill: #ef4444;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 8px 12px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: #fecaca;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;"
        );
        btnDel.setOnAction(e -> supprimerAbsence(a));

        VBox actionsWrapper = new VBox(8, btnEdit, btnDel);
        actionsWrapper.setAlignment(Pos.CENTER);
        actionsWrapper.setPadding(new Insets(14, 16, 14, 8));

        card.getChildren().addAll(accentBar, icoWrapper, infos, badgeWrapper, sep, actionsWrapper);
        return card;
    }

    private Label creerMeta(String icone, String texte) {
        Label l = new Label(icone + "  " + texte);
        l.setStyle(
                "-fx-text-fill: #64748b;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-color: #f8fafc;" +
                        "-fx-padding: 3px 10px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 20;"
        );
        return l;
    }

    @FXML public void ajouterAbsence() {
        absenceEnEdition = null;
        viderFormAbsence();
        afficherFormAbsence(true);
    }

    private void modifierAbsence(Absence a) {
        absenceEnEdition = a;
        if (dpDebutAbsence != null) dpDebutAbsence.setValue(a.getDateDebut());
        if (dpFinAbsence   != null) dpFinAbsence.setValue(a.getDateFin());
        if (txtNbrJoursAbsence != null) txtNbrJoursAbsence.setText(String.valueOf(a.getNbrJours()));
        if (comboStatutFormAbsence != null && a.getStatut() != null) comboStatutFormAbsence.setValue(a.getStatut());
        afficherFormAbsence(true);
    }

    private void supprimerAbsence(Absence a) {
        if (confirmer("Archiver cette autorisation ?\n\nElle sera masquée de la liste mais conservée dans la base pour la traçabilité.")) {
            try {
                String sql = "UPDATE absence SET statut = 'Archivé' WHERE id = ?";
                try (java.sql.PreparedStatement ps = utils.MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
                    ps.setInt(1, a.getId());
                    ps.executeUpdate();
                }
                chargerAbsences();
                alert("Archivé", "✅ Autorisation archivée.\nConservée en base pour la traçabilité.", Alert.AlertType.INFORMATION);
            } catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
        }
    }

    @FXML
    public void enregistrerAbsence() throws SQLException {
        try {
            System.out.println("▶️ enregistrerAbsence() démarré");
            LocalDate date = dpDebutAbsence != null ? dpDebutAbsence.getValue() : null;
            System.out.println("📅 date=" + date);
            if (date == null) { alert("Erreur", "La date est obligatoire !", Alert.AlertType.ERROR); return; }
            if (date.isBefore(LocalDate.now())) { alert("Erreur", "La date ne peut pas être dans le passé !", Alert.AlertType.ERROR); return; }

            String hd = comboHeureDebut != null ? comboHeureDebut.getValue() : null;
            String md = comboMinuteDebut != null ? comboMinuteDebut.getValue() : null;
            String hf = comboHeureFin   != null ? comboHeureFin.getValue()   : null;
            String mf = comboMinuteFin  != null ? comboMinuteFin.getValue()  : null;
            System.out.println("⏰ heures: " + hd + ":" + md + " → " + hf + ":" + mf);

            if (hd == null || md == null || hf == null || mf == null) {
                alert("Erreur", "Veuillez sélectionner les heures de début et de fin !", Alert.AlertType.ERROR); return;
            }

            int minutesDebut = Integer.parseInt(hd) * 60 + Integer.parseInt(md);
            int minutesFin   = Integer.parseInt(hf) * 60 + Integer.parseInt(mf);
            int dureeMinutes = minutesFin - minutesDebut;

            if (dureeMinutes <= 0)  { alert("Erreur", "L'heure de fin doit être après l'heure de début !", Alert.AlertType.ERROR); return; }
            if (dureeMinutes < 30)  { alert("Erreur", "⏱️ Durée minimale : 30 minutes\n\nDurée actuelle : " + dureeMinutes + " minutes", Alert.AlertType.ERROR); return; }
            if (dureeMinutes > 240) { alert("Erreur", "⏱️ Durée maximale : 4 heures\n\nDurée actuelle : " + (dureeMinutes / 60.0) + " heures", Alert.AlertType.ERROR); return; }

            String motif = txtMotifAbsence != null ? txtMotifAbsence.getText().trim() : "";
            if (motif.isEmpty()) { alert("Erreur", "Le motif est obligatoire !", Alert.AlertType.ERROR); return; }

            String statut = comboStatutFormAbsence != null ? comboStatutFormAbsence.getValue() : "En attente";
            if (statut == null || statut.isEmpty()) statut = "En attente";

            int typeId = getTypeIdAbsence();
            System.out.println("🏷️ typeId=" + typeId);
            if (typeId == 0) { alert("Erreur", "Veuillez sélectionner un type d'autorisation !", Alert.AlertType.ERROR); return; }

            String heureDebut = hd + ":" + md;
            String heureFin   = hf + ":" + mf;

            if (absenceEnEdition == null) {
                int userId = getUserIdConnecte();
                if (userId == 0) { alert("Erreur", "Impossible de déterminer l'utilisateur connecté !", Alert.AlertType.ERROR); return; }

                Absence a = new Absence();
                a.setDateDebut(date);
                a.setDateFin(date);
                a.setNbrJours(1);
                a.setStatut(statut);
                a.setTypeAbsenceId(typeId);
                a.setUtilisateurId(userId);
                a.setHeureDebut(heureDebut);
                a.setHeureFin(heureFin);
                a.setDureeMinutes(dureeMinutes);
                a.setMotif(motif);

                serviceAbsence.add(a);
                new Thread(() -> new EmailService().envoyerNotificationAbsence(a, utils.UserSession.getInstance().getUser())).start();

                alert("Succès",
                        "✅ Demande d'autorisation créée !\n\n" +
                                "📅 Date : " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                                "⏱️ Horaire : " + heureDebut + " - " + heureFin + "\n" +
                                "⌛ Durée : " + formatDuree(dureeMinutes) + "\n" +
                                "📝 Motif : " + motif + "\n" +
                                "📊 Statut : " + statut,
                        Alert.AlertType.INFORMATION);
            } else {
                absenceEnEdition.setDateDebut(date);
                absenceEnEdition.setDateFin(date);
                absenceEnEdition.setNbrJours(1);
                absenceEnEdition.setStatut(statut);
                absenceEnEdition.setTypeAbsenceId(typeId);
                absenceEnEdition.setHeureDebut(heureDebut);
                absenceEnEdition.setHeureFin(heureFin);
                absenceEnEdition.setDureeMinutes(dureeMinutes);
                absenceEnEdition.setMotif(motif);
                serviceAbsence.update(absenceEnEdition);
                alert("Succès", "✅ Demande d'autorisation modifiée !", Alert.AlertType.INFORMATION);
            }

            annulerFormAbsence();
            chargerAbsences();

        } catch (NumberFormatException e) {
            alert("Erreur", "Format d'heure invalide !", Alert.AlertType.ERROR);
            e.printStackTrace();
        } catch (Exception e) {
            alert("Erreur", "❌ Erreur lors de l'enregistrement:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  getUserIdConnecte — miroir exact de la logique Symfony :
    //  1) UserSession.getUserId() si syncFromUtilisateur() a été appelé (= cas normal)
    //  2) Résolution DB par username / email / nom (fallback robuste)
    //  3) Pas de fallback sur id=1 codé en dur : retourne 0 si non trouvé
    //     → l'appelant affiche un message d'erreur explicite
    // ══════════════════════════════════════════════════════════
    private int getUserIdConnecte() {
        try {
            // ── 1. Chemin rapide : UserSession.getUserId() (rempli par syncFromUtilisateur)
            int sessionId = utils.UserSession.getInstance().getUserId();
            if (sessionId > 0) {
                System.out.println("✅ userId depuis UserSession : " + sessionId);
                return sessionId;
            }

            // ── 2. Fallback : résolution DB par username / email / nom
            String username = utils.UserSession.getInstance().getUser();
            System.out.println("🔍 Résolution DB pour : " + username);

            if (username != null && !username.isEmpty()) {
                for (String col : new String[]{"username", "email", "nom", "login"}) {
                    try (java.sql.PreparedStatement ps = utils.MyDataBase.getInstance().getCnx()
                            .prepareStatement("SELECT id FROM utilisateur WHERE " + col + " = ?")) {
                        ps.setString(1, username);
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            int id = rs.getInt("id");
                            System.out.println("✅ userId trouvé via colonne '" + col + "' : " + id);
                            // Mémorise pour les prochains appels
                            utils.UserSession.getInstance().setUserId(id);
                            return id;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // ── 3. Aucun utilisateur trouvé → retourner 0 (pas de hardcode id=1)
            System.out.println("⚠️ Impossible de déterminer l'utilisateur connecté (getUserId=" + sessionId + ", user=" + username + ")");
            return 0;

        } catch (Exception e) { e.printStackTrace(); return 0; }
    }

    private String formatDuree(int minutes) {
        if (minutes < 60) return minutes + " minutes";
        int h = minutes / 60, m = minutes % 60;
        return m == 0 ? h + " heure" + (h > 1 ? "s" : "") : h + "h" + String.format("%02d", m);
    }

    @FXML public void annulerFormAbsence() {
        absenceEnEdition = null;
        viderFormAbsence();
        afficherFormAbsence(false);
    }

    @FXML public void actualiserAbsences() { chargerAbsences(); }

    private void viderFormAbsence() {
        if (dpDebutAbsence    != null) dpDebutAbsence.setValue(null);
        if (comboHeureDebut   != null) { comboHeureDebut.getSelectionModel().clearSelection(); comboHeureDebut.setPromptText("HH"); }
        if (comboMinuteDebut  != null) { comboMinuteDebut.getSelectionModel().clearSelection(); comboMinuteDebut.setPromptText("MM"); }
        if (comboHeureFin     != null) { comboHeureFin.getSelectionModel().clearSelection(); comboHeureFin.setPromptText("HH"); }
        if (comboMinuteFin    != null) { comboMinuteFin.getSelectionModel().clearSelection(); comboMinuteFin.setPromptText("MM"); }
        if (txtDureeHeures    != null) { txtDureeHeures.clear(); txtDureeHeures.setPromptText("—"); txtDureeHeures.setStyle("-fx-padding: 9px 12px; -fx-border-color: #e8eeff; -fx-border-radius: 8; -fx-border-width: 1; -fx-background-radius: 8; -fx-background-color: #f8faff; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;"); }
        if (comboStatutFormAbsence != null) comboStatutFormAbsence.setValue("En attente");
        if (comboTypeFormAbsence   != null) comboTypeFormAbsence.setValue("-- Sélectionner --");
        if (txtMotifAbsence   != null) txtMotifAbsence.clear();
    }

    private void afficherFormAbsence(boolean visible) {
        if (formAbsence != null) { formAbsence.setVisible(visible); formAbsence.setManaged(visible); }
    }

    private int getTypeIdAbsence() {
        if (comboTypeFormAbsence == null) { System.out.println("⚠️ comboTypeFormAbsence est null"); return 0; }
        String nom = comboTypeFormAbsence.getValue();
        System.out.println("🔍 getTypeIdAbsence() → valeur combo : " + nom);
        if (nom == null || nom.startsWith("--")) { System.out.println("⚠️ Aucun type sélectionné"); return 0; }
        // Requête directe avec connexion fraîche — évite les connexions expirées du service
        try (java.sql.PreparedStatement ps = utils.MyDataBase.getInstance().getCnx()
                .prepareStatement("SELECT id FROM type_absence WHERE libelle = ?")) {
            ps.setString(1, nom);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("✅ typeAbsenceId trouvé : " + id + " pour '" + nom + "'");
                return id;
            }
            System.out.println("⚠️ Aucun type_absence trouvé pour libelle='" + nom + "'");
            return 0;
        } catch (Exception e) {
            System.out.println("❌ getTypeIdAbsence erreur : " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }


    // ──────────────────────────────────────────────────────────
    //  CONGÉS – CRUD
    // ──────────────────────────────────────────────────────────

    private void chargerConges() { filtrerConges(); }

    private void filtrerConges() {
        if (containerConges == null) return;
        containerConges.getChildren().clear();

        List<Conge> liste = getCongesFiltres();

        if (liste.isEmpty()) {
            if (emptyStateConges != null) containerConges.getChildren().add(emptyStateConges);
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            liste.forEach(c -> containerConges.getChildren().add(creerCarteConge(c, fmt)));
        }
    }

    private List<Conge> getCongesFiltres() {
        List<Conge> liste;
        try { liste = serviceConge.getAll(); } catch (Exception e) { return Collections.emptyList(); }

        // ── Filtre par utilisateur connecté (miroir de CongeController.php : WHERE c.utilisateurId = :uid)
        int currentUserId = getUserIdConnecte();
        if (currentUserId > 0) {
            final int uid = currentUserId;
            liste = liste.stream()
                    .filter(c -> c.getUtilisateurId() == uid)
                    .collect(Collectors.toList());
        }

        liste = liste.stream().filter(c -> !"Archivé".equalsIgnoreCase(c.getStatut())).collect(Collectors.toList());

        String statut = comboStatutConge != null ? comboStatutConge.getValue() : null;
        if (statut != null && !statut.equals("Tous les statuts"))
            liste = liste.stream().filter(c -> statut.equalsIgnoreCase(c.getStatut())).collect(Collectors.toList());

        String q = txtRechercheConge != null ? txtRechercheConge.getText().trim().toLowerCase() : "";
        if (!q.isEmpty())
            liste = liste.stream().filter(c -> c.getStatut() != null && c.getStatut().toLowerCase().contains(q)).collect(Collectors.toList());

        return liste;
    }

    private HBox creerCarteConge(Conge c, DateTimeFormatter fmt) {

        String accentColor = getAccentColorConge(c.getStatut());

        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e6faf3;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.08), 12, 0, 0, 3);"
        );

        Region accentBar = new Region();
        accentBar.setPrefWidth(4);
        accentBar.setMinWidth(4);
        accentBar.setMaxHeight(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 12 0 0 12;");

        String iconeBg = getIconeBgConge(c.getStatut());
        Label ico = new Label(getIconeStatut(c.getStatut()));
        ico.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-padding: 10px;" +
                        "-fx-background-color: " + iconeBg + ";" +
                        "-fx-background-radius: 50;"
        );
        HBox icoWrapper = new HBox(ico);
        icoWrapper.setAlignment(Pos.CENTER);
        icoWrapper.setPadding(new Insets(14, 10, 14, 16));

        String typeNom = "Congé";
        try {
            typeNom = serviceTC.getAll().stream()
                    .filter(x -> x.getId() == c.getTypeCongeId())
                    .map(TypeConge::getLibelle).findFirst().orElse("Congé");
        } catch (Exception ignored) {}

        String d1 = c.getDateDebut() != null ? c.getDateDebut().format(fmt) : "?";
        String d2 = c.getDateFin()   != null ? c.getDateFin().format(fmt)   : "?";

        VBox infos = new VBox(5);
        HBox.setHgrow(infos, Priority.ALWAYS);
        infos.setPadding(new Insets(14, 14, 14, 6));

        Label nom = new Label(typeNom);
        nom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label lblPeriode = creerMetaVert("📅", d1 + " → " + d2);
        Label lblJours   = creerMetaVert("📆", c.getNbrJours() + " jour" + (c.getNbrJours() > 1 ? "s" : ""));

        metaRow.getChildren().addAll(lblPeriode, lblJours);
        infos.getChildren().addAll(nom, metaRow);

        Label badge = new Label(c.getStatut() != null ? c.getStatut().toUpperCase() : "?");
        badge.setStyle(getBadgeStyleConge(c.getStatut()));
        VBox badgeWrapper = new VBox(badge);
        badgeWrapper.setAlignment(Pos.CENTER);
        badgeWrapper.setPadding(new Insets(0, 16, 0, 0));

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setPadding(new Insets(12, 8, 12, 8));
        sep.setStyle("-fx-opacity: 0.3;");

        Button btnEdit = new Button("✏️  Modifier");
        btnEdit.setStyle(
                "-fx-background-color: #ecfdf5;" +
                        "-fx-text-fill: #059669;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 16px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: #a7f3d0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;"
        );
        btnEdit.setOnAction(e -> modifierConge(c));

        Button btnDel = new Button("📦  Archiver");
        btnDel.setStyle(
                "-fx-background-color: #fff1f2;" +
                        "-fx-text-fill: #ef4444;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 8px 12px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: #fecaca;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;"
        );
        btnDel.setOnAction(e -> supprimerConge(c));

        VBox actionsWrapper = new VBox(8, btnEdit, btnDel);
        actionsWrapper.setAlignment(Pos.CENTER);
        actionsWrapper.setPadding(new Insets(14, 16, 14, 8));

        card.getChildren().addAll(accentBar, icoWrapper, infos, badgeWrapper, sep, actionsWrapper);
        return card;
    }

    private Label creerMetaVert(String icone, String texte) {
        Label l = new Label(icone + "  " + texte);
        l.setStyle(
                "-fx-text-fill: #059669;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-color: #f0fdf4;" +
                        "-fx-padding: 3px 10px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #a7f3d0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 20;"
        );
        return l;
    }

    @FXML public void ajouterConge() {
        congeEnEdition = null;
        viderFormConge();
        afficherFormConge(true);
    }

    private void modifierConge(Conge c) {
        congeEnEdition = c;
        if (dpDebutConge != null) dpDebutConge.setValue(c.getDateDebut());
        if (dpFinConge   != null) dpFinConge.setValue(c.getDateFin());
        if (txtNbrJoursConge != null) txtNbrJoursConge.setText(c.getNbrJours() + " jour" + (c.getNbrJours() > 1 ? "s" : ""));
        if (comboStatutFormConge != null && c.getStatut() != null) comboStatutFormConge.setValue(c.getStatut());
        afficherFormConge(true);
    }

    private void supprimerConge(Conge c) {
        if (confirmer("Archiver ce congé ?\n\nIl sera masqué de la liste mais conservé dans la base pour la traçabilité.")) {
            try {
                String sql = "UPDATE conge SET statut = 'Archivé' WHERE id = ?";
                try (java.sql.PreparedStatement ps = utils.MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
                    ps.setInt(1, c.getId());
                    ps.executeUpdate();
                }
                chargerConges();
                alert("Archivé", "✅ Congé archivé.\nConservé en base pour la traçabilité.", Alert.AlertType.INFORMATION);
            } catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
        }
    }

    @FXML public void enregistrerConge() {
        try {
            LocalDate debut = dpDebutConge != null ? dpDebutConge.getValue() : null;
            LocalDate fin   = dpFinConge   != null ? dpFinConge.getValue()   : null;

            if (debut == null || fin == null) { alert("Erreur", "Les dates sont obligatoires !", Alert.AlertType.ERROR); return; }
            if (debut.isBefore(LocalDate.now())) { alert("Erreur", "La date de début ne peut pas être dans le passé !", Alert.AlertType.ERROR); return; }
            if (fin.isBefore(debut)) { alert("Erreur", "La date de fin doit être après la date de début !", Alert.AlertType.ERROR); return; }

            int nbrJours = (int) debut.until(fin, java.time.temporal.ChronoUnit.DAYS) + 1;
            String statut = comboStatutFormConge != null ? comboStatutFormConge.getValue() : "En attente";
            if (statut == null || statut.isEmpty()) statut = "En attente";
            int typeId = getTypeIdConge();

            if (congeEnEdition == null) {
                // ✅ FIX : utilise getUserIdConnecte() comme Symfony utilise $user->getId()
                //          avec fallback sur 1 si non trouvé (même comportement que le else { setUtilisateurId(1) })
                int userId = getUserIdConnecte();

                Conge c = new Conge(0, debut, fin, nbrJours, statut, typeId, userId);
                serviceConge.add(c);
                new Thread(() -> new EmailService().envoyerNotificationConge(c, utils.UserSession.getInstance().getUser())).start();
                alert("Succès", "✅ Congé ajouté !", Alert.AlertType.INFORMATION);
            } else {
                congeEnEdition.setDateDebut(debut);
                congeEnEdition.setDateFin(fin);
                congeEnEdition.setNbrJours(nbrJours);
                congeEnEdition.setStatut(statut);
                congeEnEdition.setTypeCongeId(typeId);
                serviceConge.update(congeEnEdition);
                alert("Succès", "✅ Congé modifié !", Alert.AlertType.INFORMATION);
            }
            annulerFormConge();
            chargerConges();
        } catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
    }

    @FXML public void annulerFormConge() {
        congeEnEdition = null;
        viderFormConge();
        afficherFormConge(false);
    }

    @FXML public void actualiserConges() { chargerConges(); }

    private void viderFormConge() {
        if (dpDebutConge != null)    dpDebutConge.setValue(null);
        if (dpFinConge != null)      dpFinConge.setValue(null);
        if (txtNbrJoursConge != null) txtNbrJoursConge.clear();
        if (comboStatutFormConge != null) comboStatutFormConge.setValue("En attente");
        if (comboTypeFormConge != null)   comboTypeFormConge.setValue("-- Sélectionner --");
        if (txtMotifConge != null)   txtMotifConge.clear();
    }

    private void afficherFormConge(boolean visible) {
        if (formConge != null) { formConge.setVisible(visible); formConge.setManaged(visible); }
    }

    private int getTypeIdConge() {
        if (comboTypeFormConge == null) return 1;
        String nom = comboTypeFormConge.getValue();
        if (nom == null || nom.startsWith("--")) return 1;
        try { return serviceTC.getAll().stream().filter(x -> x.getLibelle().equals(nom)).map(TypeConge::getId).findFirst().orElse(1); }
        catch (Exception e) { return 1; }
    }


    // ──────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ──────────────────────────────────────────────────────────

    private int calculerJours(TextField field, LocalDate debut, LocalDate fin) {
        if (field != null && !field.getText().trim().isEmpty()) {
            try { return Integer.parseInt(field.getText().trim()); } catch (NumberFormatException ignored) {}
        }
        int j = (int) debut.until(fin, java.time.temporal.ChronoUnit.DAYS) + 1;
        if (field != null) field.setText(String.valueOf(j));
        return j;
    }

    private String getIconeStatut(String statut) {
        if (statut == null) return "❓";
        return switch (statut.toLowerCase()) {
            case "approuvé", "validé" -> "✅";
            case "refusé", "rejeté"  -> "❌";
            case "en attente"        -> "⏳";
            default -> "📋";
        };
    }

    private String getAccentColor(String statut) {
        if (statut == null) return "#94a3b8";
        return switch (statut.toLowerCase()) {
            case "approuvé", "validé" -> "#10b981";
            case "refusé", "rejeté"  -> "#ef4444";
            case "en attente"        -> "#f59e0b";
            default -> "#667eea";
        };
    }

    private String getAccentColorConge(String statut) {
        if (statut == null) return "#94a3b8";
        return switch (statut.toLowerCase()) {
            case "approuvé", "validé" -> "#10b981";
            case "refusé", "rejeté"  -> "#ef4444";
            case "en attente"        -> "#f59e0b";
            default -> "#059669";
        };
    }

    private String getIconeBg(String statut) {
        if (statut == null) return "#f1f5f9";
        return switch (statut.toLowerCase()) {
            case "approuvé", "validé" -> "#d1fae5";
            case "refusé", "rejeté"  -> "#fee2e2";
            case "en attente"        -> "#fef3c7";
            default -> "#eef2ff";
        };
    }

    private String getIconeBgConge(String statut) {
        if (statut == null) return "#f1f5f9";
        return switch (statut.toLowerCase()) {
            case "approuvé", "validé" -> "#d1fae5";
            case "refusé", "rejeté"  -> "#fee2e2";
            case "en attente"        -> "#fef3c7";
            default -> "#ecfdf5";
        };
    }

    private String getBadgeStyleV2(String statut) {
        String base = "-fx-padding: 4px 14px; -fx-background-radius: 20; -fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;";
        if (statut == null) return base + " -fx-background-color: #94a3b8;";
        return switch (statut.toLowerCase()) {
            case "approuvé", "validé" -> base + " -fx-background-color: #10b981;";
            case "refusé", "rejeté"  -> base + " -fx-background-color: #ef4444;";
            case "en attente"        -> base + " -fx-background-color: #f59e0b;";
            default                  -> base + " -fx-background-color: #667eea;";
        };
    }

    private String getBadgeStyleConge(String statut) {
        String base = "-fx-padding: 4px 14px; -fx-background-radius: 20; -fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;";
        if (statut == null) return base + " -fx-background-color: #94a3b8;";
        return switch (statut.toLowerCase()) {
            case "approuvé", "validé" -> base + " -fx-background-color: #10b981;";
            case "refusé", "rejeté"  -> base + " -fx-background-color: #ef4444;";
            case "en attente"        -> base + " -fx-background-color: #f59e0b;";
            default                  -> base + " -fx-background-color: #059669;";
        };
    }

    private String getBadgeStyle(String statut) { return getBadgeStyleV2(statut); }

    private void alert(String titre, String msg, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private boolean confirmer(String msg) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        c.setHeaderText(null);
        return c.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}