package attendance.gui;

import attendance.interfaces.service;
import attendance.models.*;
import attendance.services.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ValidationsSoldesController {

    @FXML private VBox viewValidations;
    @FXML private VBox viewMesSoldes;

    @FXML private Label lblDemandesAttente;
    @FXML private Label lblTauxAbsence;
    @FXML private Label lblApprouvees;
    @FXML private VBox  containerValidations;
    @FXML private VBox  emptyStateValidations;
    @FXML private VBox  containerPlanning;
    @FXML private VBox  calendarContainer;

    @FXML private PieChart chartSoldes;

    // ── Labels section "Mes Soldes" ──────────────────────────────────────────
    // 4 cartes du haut
    @FXML private Label lblCPRestants;
    @FXML private Label lblCPRestantsDetail;
    @FXML private Label lblCPConsommes;
    @FXML private Label lblCPEnAttente;
    @FXML private Label lblCPMaladie;

    // Nom utilisateur connecté
    @FXML private Label lblNomUtilisateur;

    // Détail congés payés (barre de progression)
    @FXML private Label lblSoldeCP;          // ex : "18 / 30 jours"
    @FXML private Label lblCPProgLabel;      // ex : "12 consommés sur 30 acquis"
    @FXML private Label lblCPProgPct;        // ex : "40%"
    @FXML private Label lblCPAcquis;         // ex : "Acquis : 30 j"
    @FXML private Label lblCPPris;           // ex : "Pris : 12 j"
    @FXML private Label lblCPRestantBas;     // ex : "Restant : 18 j"

    // Barre de progression (StackPane + Region fill)
    @FXML private Region progCPTrack;
    @FXML private Region progCPFill;

    // Congé maladie / sans solde
    @FXML private Label lblCongesMaladie;
    @FXML private Label lblCongesSansSolde;

    // Total consommé
    @FXML private Label lblCongesConsommes;           // total en gros
    @FXML private Label lblCongesConsommesDetail;     // "X congés · Y absences"

    // Labels optionnels
    @FXML private Label lblCongesExceptionnels;
    @FXML private Label lblCongesPaternite;
    @FXML private Label lblAbsencesJustifiees;
    @FXML private Label lblAbsencesInjustifiees;
    @FXML private Label lblAbsencesMedicales;
    @FXML private Label lblGraphSubtitle;

    // Conteneur des cartes de soldes par type (généré dynamiquement)
    @FXML private VBox containerSoldesTypes;

    // Jours fériés
    @FXML private VBox containerFeriesTunisie;

    private TeamCalendarWebView teamCalendar;

    private final service<Absence>     serviceAbsence     = new ServiceAbsence();
    private final service<Conge>       serviceConge       = new ServiceConge();
    private final service<TypeAbsence> serviceTA          = new ServiceTypeAbsence();
    private final service<TypeConge>   serviceTC          = new ServiceTypeConge();
    private final service<Utilisateur> serviceUtilisateur = new ServiceUtilisateur();

    // Quota annuel CP (modifiable selon politique RH)
    private static final int QUOTA_ANNUEL_CP = 30;

    @FXML
    public void initialize() {
        afficherValidations();
        initCalendar();
    }

    private void initCalendar() {
        if (calendarContainer == null) return;
        try {
            teamCalendar = new TeamCalendarWebView();
            calendarContainer.getChildren().add(teamCalendar);
        } catch (Exception e) {
            System.err.println("❌ Erreur init calendrier : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  NAVIGATION ENTRE VUES
    // ══════════════════════════════════════════════════════════════════

    public void afficherValidations() {
        if (viewValidations != null) { viewValidations.setVisible(true);  viewValidations.setManaged(true); }
        if (viewMesSoldes   != null) { viewMesSoldes.setVisible(false);   viewMesSoldes.setManaged(false); }
        chargerValidations();
    }

    public void afficherSoldes() {
        if (viewMesSoldes   != null) { viewMesSoldes.setVisible(true);    viewMesSoldes.setManaged(true); }
        if (viewValidations != null) { viewValidations.setVisible(false); viewValidations.setManaged(false); }
        chargerGraphiqueSoldes();
        chargerJoursFeriesTunisie();
    }

    // ══════════════════════════════════════════════════════════════════
    //  VUE VALIDATIONS
    // ══════════════════════════════════════════════════════════════════

    private void chargerValidations() {
        if (containerValidations == null) return;
        containerValidations.getChildren().clear();

        try {
            List<Absence> toutes     = serviceAbsence.getAll();
            List<Absence> enAttente  = toutes.stream().filter(a -> "En attente".equalsIgnoreCase(a.getStatut())).collect(Collectors.toList());
            List<Absence> approuvees = toutes.stream().filter(a -> "Approuvé".equalsIgnoreCase(a.getStatut())).collect(Collectors.toList());

            if (lblDemandesAttente != null) lblDemandesAttente.setText(String.valueOf(enAttente.size()));
            if (lblApprouvees      != null) lblApprouvees.setText(String.valueOf(approuvees.size()));
            if (lblTauxAbsence != null) {
                int taux = toutes.isEmpty() ? 0 : (int)((double) enAttente.size() / toutes.size() * 100);
                lblTauxAbsence.setText(taux + "%");
            }

            if (enAttente.isEmpty()) {
                if (emptyStateValidations != null) containerValidations.getChildren().add(emptyStateValidations);
            } else {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
                enAttente.forEach(a -> containerValidations.getChildren().add(creerCarteValidation(a, fmt)));
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargerValidations : " + e.getMessage());
            if (lblDemandesAttente != null) lblDemandesAttente.setText("1");
            if (lblApprouvees      != null) lblApprouvees.setText("2");
            containerValidations.getChildren().add(creerCarteValidationDemo());
        }

        chargerPlanning();
    }

    // ══════════════════════════════════════════════════════════════════
    //  GRAPHIQUE SOLDES — MIS À JOUR COMPLET DEPUIS LA BDD
    // ══════════════════════════════════════════════════════════════════

    private void chargerGraphiqueSoldes() {
        if (chartSoldes == null) return;
        chartSoldes.getData().clear();

        try {
            // ── 1. Chargement BDD ───────────────────────────────────
            List<Conge>       tousConges     = serviceConge.getAll();
            List<Absence>     toutesAbsences = serviceAbsence.getAll();
            List<TypeConge>   typesConge     = serviceTC.getAll();
            List<TypeAbsence> typesAbsence   = serviceTA.getAll();

            // ── 2. Nom utilisateur connecté (premier utilisateur si disponible) ──
            try {
                List<Utilisateur> users = serviceUtilisateur.getAll();
                if (!users.isEmpty() && lblNomUtilisateur != null) {
                    lblNomUtilisateur.setText(users.get(0).getNomComplet());
                }
            } catch (Exception ignored) {}

            // ── 3. Calculs Congés ────────────────────────────────────

            // Jours approuvés par type de congé
            Map<Integer, Integer> joursParTypeConge = new HashMap<>();
            tousConges.stream()
                    .filter(c -> "Approuvé".equalsIgnoreCase(c.getStatut()))
                    .forEach(c -> joursParTypeConge.merge(c.getTypeCongeId(), c.getNbrJours(), Integer::sum));

            // Jours en attente par type de congé
            Map<Integer, Integer> congesEnAttenteParType = new HashMap<>();
            tousConges.stream()
                    .filter(c -> "En attente".equalsIgnoreCase(c.getStatut()))
                    .forEach(c -> congesEnAttenteParType.merge(c.getTypeCongeId(), c.getNbrJours(), Integer::sum));

            int totalCongesApprouves  = joursParTypeConge.values().stream().mapToInt(Integer::intValue).sum();
            int totalCongesEnAttente  = congesEnAttenteParType.values().stream().mapToInt(Integer::intValue).sum();
            int joursCongesMaladieVal = joursParTypeConge.getOrDefault(2, 0);
            int joursCongesExcep      = joursParTypeConge.getOrDefault(5, 0);
            int joursCongesPat        = joursParTypeConge.getOrDefault(8, 0);

            int restantCP = Math.max(0, QUOTA_ANNUEL_CP - totalCongesApprouves);
            int pctConsomme = QUOTA_ANNUEL_CP > 0
                    ? (int) Math.round((double) totalCongesApprouves / QUOTA_ANNUEL_CP * 100)
                    : 0;

            // ── 4. Calculs Absences ──────────────────────────────────

            Map<Integer, Integer> joursParTypeAbsence = new HashMap<>();
            toutesAbsences.stream()
                    .filter(a -> "Approuvé".equalsIgnoreCase(a.getStatut())
                            || "Justifiée".equalsIgnoreCase(a.getStatut()))
                    .forEach(a -> joursParTypeAbsence.merge(a.getTypeAbsenceId(), a.getNbrJours(), Integer::sum));

            int joursAbsJust   = joursParTypeAbsence.getOrDefault(1, 0);
            int joursAbsInjust = joursParTypeAbsence.getOrDefault(2, 0);
            int joursAbsMed    = joursParTypeAbsence.getOrDefault(3, 0);
            int totalAbsences  = joursAbsJust + joursAbsInjust + joursAbsMed;

            int totalGlobal = totalCongesApprouves + totalAbsences;

            // ── 5. Mise à jour des 4 cartes du haut ─────────────────

            setLabel(lblCPRestants,      restantCP + " j");
            setLabel(lblCPRestantsDetail, "sur " + QUOTA_ANNUEL_CP + " acquis");
            setLabel(lblCPConsommes,      totalCongesApprouves + " j");
            setLabel(lblCPEnAttente,      totalCongesEnAttente + " j");
            setLabel(lblCPMaladie,        joursCongesMaladieVal + " j");

            // ── 6. Section Congés Payés (barre de progression) ───────

            setLabel(lblSoldeCP,       restantCP + " / " + QUOTA_ANNUEL_CP + " jours");
            setLabel(lblCPProgLabel,   totalCongesApprouves + " consommés sur " + QUOTA_ANNUEL_CP + " acquis");
            setLabel(lblCPProgPct,     pctConsomme + "%");
            setLabel(lblCPAcquis,      "Acquis : " + QUOTA_ANNUEL_CP + " j");
            setLabel(lblCPPris,        "Pris : " + totalCongesApprouves + " j");
            setLabel(lblCPRestantBas,  "Restant : " + restantCP + " j");

            // Animer la barre de progression via binding après rendu
            if (progCPFill != null && progCPTrack != null) {
                double pct = Math.max(0.0, Math.min(1.0, (double) totalCongesApprouves / QUOTA_ANNUEL_CP));
                Platform.runLater(() ->
                        progCPFill.prefWidthProperty().bind(
                                progCPTrack.widthProperty().multiply(pct)
                        )
                );
            }

            // ── 7. Congé maladie / sans solde ───────────────────────

            setLabel(lblCongesMaladie,    joursCongesMaladieVal + " jours");
            setLabel(lblCongesSansSolde,  "0 jours");

            // ── 8. Total consommé ────────────────────────────────────

            setLabel(lblCongesConsommes,       totalGlobal + " jours");
            setLabel(lblCongesConsommesDetail,
                    totalCongesApprouves + " congé" + (totalCongesApprouves > 1 ? "s" : "")
                            + " · " + totalAbsences + " absence" + (totalAbsences > 1 ? "s" : ""));

            // ── 9. Labels optionnels ─────────────────────────────────

            setLabel(lblCongesExceptionnels,  joursCongesExcep + " jours");
            setLabel(lblCongesPaternite,      joursCongesPat + " jours");
            setLabel(lblAbsencesJustifiees,   joursAbsJust + " jours");
            setLabel(lblAbsencesInjustifiees, joursAbsInjust + " jours");
            setLabel(lblAbsencesMedicales,    joursAbsMed + " jours");
            setLabel(lblGraphSubtitle,        totalGlobal + " jours au total cette année");

            // ── 10. PieChart ─────────────────────────────────────────

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            if (restantCP > 0)
                pieData.add(new PieChart.Data("Solde restant : " + restantCP + "j", restantCP));
            if (totalCongesApprouves > 0)
                pieData.add(new PieChart.Data("Congés pris : " + totalCongesApprouves + "j", totalCongesApprouves));
            if (joursAbsJust > 0)
                pieData.add(new PieChart.Data("Abs. justifiées : " + joursAbsJust + "j", joursAbsJust));
            if (joursAbsInjust > 0)
                pieData.add(new PieChart.Data("Abs. injustifiées : " + joursAbsInjust + "j", joursAbsInjust));
            if (joursAbsMed > 0)
                pieData.add(new PieChart.Data("Abs. médicales : " + joursAbsMed + "j", joursAbsMed));
            if (pieData.isEmpty())
                pieData.add(new PieChart.Data("Solde restant : " + QUOTA_ANNUEL_CP + "j", QUOTA_ANNUEL_CP));

            chartSoldes.setData(pieData);
            chartSoldes.setLabelsVisible(true);
            chartSoldes.setLegendVisible(true);

            // ── 11. Cartes détaillées par type ───────────────────────

            if (containerSoldesTypes != null) {
                containerSoldesTypes.getChildren().clear();

                Label titreSoldes = new Label("Détail par type de congé / absence");
                titreSoldes.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 0 0 8 0;");
                containerSoldesTypes.getChildren().add(titreSoldes);

                List<SoldeItem> items = new ArrayList<>();

                for (TypeConge tc : typesConge) {
                    int jours = joursParTypeConge.getOrDefault(tc.getId(), 0);
                    int att   = congesEnAttenteParType.getOrDefault(tc.getId(), 0);
                    items.add(new SoldeItem(
                            iconePourTypeConge(tc.getLibelle()),
                            tc.getLibelle(),
                            jours + " jours pris",
                            att > 0 ? att + "j en attente" : null,
                            couleurPourTypeConge(tc.getLibelle()),
                            "CONGÉ"
                    ));
                }

                Map<Integer, Integer> absEnAttenteParType = new HashMap<>();
                toutesAbsences.stream()
                        .filter(a -> "En attente".equalsIgnoreCase(a.getStatut()))
                        .forEach(a -> absEnAttenteParType.merge(a.getTypeAbsenceId(), a.getNbrJours(), Integer::sum));

                for (TypeAbsence ta : typesAbsence) {
                    int jours = joursParTypeAbsence.getOrDefault(ta.getId(), 0);
                    int att   = absEnAttenteParType.getOrDefault(ta.getId(), 0);
                    items.add(new SoldeItem(
                            iconePourTypeAbsence(ta.getLibelle()),
                            ta.getLibelle(),
                            jours + " jours",
                            att > 0 ? att + "j en attente" : null,
                            couleurPourTypeAbsence(ta.getLibelle()),
                            "ABSENCE"
                    ));
                }

                HBox currentRow = null;
                for (int i = 0; i < items.size(); i++) {
                    if (i % 2 == 0) {
                        currentRow = new HBox(12);
                        currentRow.setPadding(new Insets(0, 0, 12, 0));
                        containerSoldesTypes.getChildren().add(currentRow);
                    }
                    VBox carte = creerCarteSolde(items.get(i));
                    HBox.setHgrow(carte, Priority.ALWAYS);
                    currentRow.getChildren().add(carte);
                }
                if (items.size() % 2 == 1 && currentRow != null) {
                    Region ghost = new Region();
                    HBox.setHgrow(ghost, Priority.ALWAYS);
                    currentRow.getChildren().add(ghost);
                }

                containerSoldesTypes.getChildren().add(
                        creerResumeGlobal(restantCP, QUOTA_ANNUEL_CP, totalCongesApprouves, totalAbsences)
                );
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur chargerGraphiqueSoldes: " + e.getMessage());
            e.printStackTrace();
            // Fallback
            chartSoldes.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Solde restant : " + QUOTA_ANNUEL_CP + "j", QUOTA_ANNUEL_CP)
            ));
            setLabel(lblCPRestants,           QUOTA_ANNUEL_CP + " j");
            setLabel(lblCPRestantsDetail,     "sur " + QUOTA_ANNUEL_CP + " acquis");
            setLabel(lblCPConsommes,          "0 j");
            setLabel(lblCPEnAttente,          "0 j");
            setLabel(lblCPMaladie,            "0 j");
            setLabel(lblSoldeCP,              QUOTA_ANNUEL_CP + " / " + QUOTA_ANNUEL_CP + " jours");
            setLabel(lblCongesConsommes,      "0 jours");
            setLabel(lblCongesConsommesDetail,"0 congés · 0 absences");
            setLabel(lblCongesMaladie,        "0 jours");
            setLabel(lblCongesSansSolde,      "0 jours");
        }
    }

    // ── Utilitaire : set label seulement s'il est non null ──────────────────
    private void setLabel(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    // ══════════════════════════════════════════════════════════════════
    //  CARTE VALIDATION
    // ══════════════════════════════════════════════════════════════════

    private VBox creerCarteValidation(Absence abs, DateTimeFormatter fmt) {

        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);

        Region accentBar = new Region();
        accentBar.setPrefWidth(4);
        accentBar.setMinWidth(4);
        accentBar.setMaxHeight(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: #f59e0b; -fx-background-radius: 14 0 0 14;");

        Utilisateur user = null;
        String nomComplet   = "Utilisateur #" + abs.getUtilisateurId();
        String posteComplet = "";
        try {
            user = serviceUtilisateur.getById(abs.getUtilisateurId());
            if (user != null) {
                nomComplet = user.getNomComplet();
                if (user.getPoste() != null && user.getDepartement() != null)
                    posteComplet = user.getPoste() + " · " + user.getDepartement();
                else if (user.getPoste() != null)
                    posteComplet = user.getPoste();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur récupération utilisateur : " + e.getMessage());
        }

        String initiales = "U" + abs.getUtilisateurId();
        if (user != null && user.getPrenom() != null && user.getNom() != null)
            initiales = user.getPrenom().substring(0, 1).toUpperCase()
                    + user.getNom().substring(0, 1).toUpperCase();

        String[] avatarColors = {"#667eea", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"};
        String avatarColor = avatarColors[Math.abs(abs.getUtilisateurId()) % avatarColors.length];

        StackPane avatar = new StackPane();
        Region cercle = new Region();
        cercle.setStyle("-fx-background-color: " + avatarColor + "; -fx-background-radius: 50;"
                + " -fx-min-width: 46; -fx-min-height: 46; -fx-max-width: 46; -fx-max-height: 46;");
        Label lblIni = new Label(initiales);
        lblIni.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatar.getChildren().addAll(cercle, lblIni);
        HBox avatarWrapper = new HBox(avatar);
        avatarWrapper.setAlignment(Pos.CENTER);
        avatarWrapper.setPadding(new Insets(16, 12, 16, 18));

        VBox infosUser = new VBox(3);
        infosUser.setPadding(new Insets(16, 0, 16, 0));
        infosUser.setMinWidth(160);
        Label lblNom2 = new Label(nomComplet);
        lblNom2.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
        infosUser.getChildren().add(lblNom2);
        if (!posteComplet.isEmpty()) {
            Label lblPoste2 = new Label(posteComplet);
            lblPoste2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            infosUser.getChildren().add(lblPoste2);
        }

        Separator sep1 = makeSep(); Separator sep2 = makeSep(); Separator sep3 = makeSep();

        String typeNom = "Absence";
        try {
            typeNom = serviceTA.getAll().stream()
                    .filter(x -> x.getId() == abs.getTypeAbsenceId())
                    .map(TypeAbsence::getLibelle).findFirst().orElse("Absence");
        } catch (Exception ignored) {}

        VBox infoType = new VBox(4);
        infoType.setPadding(new Insets(16, 0, 16, 0));
        infoType.setMinWidth(130);
        Label t1 = new Label("TYPE");
        t1.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        Label t2 = new Label(typeNom);
        t2.setStyle("-fx-text-fill: #667eea; -fx-font-size: 12px; -fx-font-weight: bold;"
                + " -fx-background-color: #eef2ff; -fx-padding: 3px 10px; -fx-background-radius: 20;");
        infoType.getChildren().addAll(t1, t2);

        VBox infoPer = new VBox(4);
        infoPer.setPadding(new Insets(16, 0, 16, 0));
        infoPer.setMinWidth(120);
        Label p1 = new Label("PÉRIODE");
        p1.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        String d1s = abs.getDateDebut() != null ? abs.getDateDebut().format(fmt) : "?";
        String d2s = abs.getDateFin()   != null ? abs.getDateFin().format(fmt)   : "?";
        Label p2 = new Label("📅 " + d1s + " → " + d2s);
        p2.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        infoPer.getChildren().addAll(p1, p2);

        VBox infoDur = new VBox(4);
        infoDur.setPadding(new Insets(16, 0, 16, 0));
        infoDur.setMinWidth(90);
        Label d1l = new Label("DURÉE");
        d1l.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        int duree = abs.getNbrJours();
        String dureeStr;
        if (duree < 60) dureeStr = "⌛ " + duree + " min";
        else { int h = duree / 60, m = duree % 60; dureeStr = "⌛ " + (m == 0 ? h + "h" : h + "h" + String.format("%02d", m)); }
        Label d2l = new Label(dureeStr);
        d2l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        infoDur.getChildren().addAll(d1l, d2l);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefuser = new Button("✕  Refuser");
        btnRefuser.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; -fx-font-size: 12px;"
                + " -fx-font-weight: bold; -fx-padding: 9px 18px; -fx-background-radius: 9; -fx-cursor: hand;"
                + " -fx-border-color: #fecaca; -fx-border-width: 1; -fx-border-radius: 9;");
        btnRefuser.setOnAction(e -> { validerDemande(abs, "Refusé"); chargerValidations(); });

        Button btnApprouver = new Button("✓  Approuver");
        btnApprouver.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #059669);"
                + " -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 9px 18px;"
                + " -fx-background-radius: 9; -fx-cursor: hand;"
                + " -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.35), 8, 0, 0, 2);");
        btnApprouver.setOnAction(e -> { validerDemande(abs, "Approuvé"); chargerValidations(); });

        HBox actions = new HBox(10, btnRefuser, btnApprouver);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 18, 0, 0));

        card.getChildren().addAll(accentBar, avatarWrapper, infosUser, sep1, infoType, sep2, infoPer, sep3, infoDur, spacer, actions);

        VBox wrapper = new VBox(0);
        wrapper.getChildren().add(card);

        if (abs.getMotif() != null && !abs.getMotif().isBlank()) {
            HBox motifRow = new HBox(8);
            motifRow.setAlignment(Pos.CENTER_LEFT);
            motifRow.setPadding(new Insets(8, 18, 10, 26));
            motifRow.setStyle("-fx-background-color: #fffbeb; -fx-background-radius: 0 0 14 14;");
            Label motifIco = new Label("💬");
            motifIco.setStyle("-fx-font-size: 12px;");
            Label motifTxt = new Label(abs.getMotif());
            motifTxt.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400e; -fx-font-style: italic;");
            motifRow.getChildren().addAll(motifIco, motifTxt);
            wrapper.getChildren().add(motifRow);
        }

        String wrapperStyle = "-fx-background-color: white; -fx-background-radius: 14;"
                + " -fx-border-color: #eef2ff; -fx-border-width: 1; -fx-border-radius: 14;"
                + " -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.10), 14, 0, 0, 4);";
        wrapper.setStyle(wrapperStyle);
        card.setStyle("-fx-background-color: transparent;");

        return wrapper;
    }

    private Separator makeSep() {
        Separator s = new Separator(javafx.geometry.Orientation.VERTICAL);
        s.setPadding(new Insets(12, 14, 12, 14));
        s.setStyle("-fx-opacity: 0.3;");
        return s;
    }

    private VBox creerCarteValidationDemo() {
        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);

        Region accentBar = new Region();
        accentBar.setPrefWidth(4);
        accentBar.setMinWidth(4);
        accentBar.setMaxHeight(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: #f59e0b; -fx-background-radius: 14 0 0 14;");

        StackPane avatar = new StackPane();
        Region cercle = new Region();
        cercle.setStyle("-fx-background-color: #10b981; -fx-background-radius: 50; -fx-min-width: 46; -fx-min-height: 46; -fx-max-width: 46; -fx-max-height: 46;");
        Label ini = new Label("LM");
        ini.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatar.getChildren().addAll(cercle, ini);
        HBox avatarWrapper = new HBox(avatar);
        avatarWrapper.setAlignment(Pos.CENTER);
        avatarWrapper.setPadding(new Insets(16, 12, 16, 18));

        VBox infosUser = new VBox(3);
        infosUser.setPadding(new Insets(16, 0, 16, 0));
        infosUser.setMinWidth(160);
        Label n1 = new Label("Lucas Martin");
        n1.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
        Label n2 = new Label("Chef de Projet · IT");
        n2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        infosUser.getChildren().addAll(n1, n2);

        Separator s1 = makeSep(); Separator s2 = makeSep(); Separator s3 = makeSep();

        VBox infoType = new VBox(4);
        infoType.setPadding(new Insets(16, 0, 16, 0));
        infoType.setMinWidth(130);
        Label t1 = new Label("TYPE");
        t1.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        Label t2 = new Label("Congés Payés");
        t2.setStyle("-fx-text-fill: #667eea; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #eef2ff; -fx-padding: 3px 10px; -fx-background-radius: 20;");
        infoType.getChildren().addAll(t1, t2);

        VBox infoPer = new VBox(4);
        infoPer.setPadding(new Insets(16, 0, 16, 0));
        infoPer.setMinWidth(120);
        Label p1 = new Label("PÉRIODE");
        p1.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        Label p2 = new Label("📅 01/08 → 14/08");
        p2.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        infoPer.getChildren().addAll(p1, p2);

        VBox infoDur = new VBox(4);
        infoDur.setPadding(new Insets(16, 0, 16, 0));
        infoDur.setMinWidth(90);
        Label d1 = new Label("DURÉE");
        d1.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        Label d2 = new Label("⌛ 10 jours");
        d2.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        infoDur.getChildren().addAll(d1, d2);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRef = new Button("✕  Refuser");
        btnRef.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 9px 18px; -fx-background-radius: 9; -fx-cursor: hand; -fx-border-color: #fecaca; -fx-border-width: 1; -fx-border-radius: 9;");
        Button btnApp = new Button("✓  Approuver");
        btnApp.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #059669); -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 9px 18px; -fx-background-radius: 9; -fx-cursor: hand;");

        HBox actions = new HBox(10, btnRef, btnApp);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 18, 0, 0));

        card.getChildren().addAll(accentBar, avatarWrapper, infosUser, s1, infoType, s2, infoPer, s3, infoDur, spacer, actions);

        VBox wrapper = new VBox(card);
        wrapper.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                + " -fx-border-color: #eef2ff; -fx-border-width: 1; -fx-border-radius: 14;"
                + " -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.10), 14, 0, 0, 4);");
        return wrapper;
    }

    private void validerDemande(Absence abs, String statut) {
        try {
            abs.setStatut(statut);
            serviceAbsence.update(abs);
            if (teamCalendar != null) teamCalendar.refresh();
            alert("Succès", "Demande " + statut.toLowerCase() + "e !", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            alert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PLANNING
    // ══════════════════════════════════════════════════════════════════

    private void chargerPlanning() {
        if (containerPlanning == null) return;
        containerPlanning.getChildren().clear();
        try {
            List<Conge> conges = serviceConge.getAll()
                    .stream()
                    .filter(c -> !"Archivé".equalsIgnoreCase(c.getStatut()))
                    .sorted(Comparator.comparing(c -> c.getDateDebut() != null ? c.getDateDebut() : LocalDate.MIN))
                    .collect(Collectors.toList());

            if (conges.isEmpty()) {
                VBox empty = new VBox(12);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(40));
                Label ico = new Label("🌴");
                ico.setStyle("-fx-font-size: 40px; -fx-opacity: 0.20;");
                Label msg = new Label("Aucun congé planifié");
                msg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
                Label sub = new Label("Les congés approuvés apparaîtront ici");
                sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #e2e8f0;");
                empty.getChildren().addAll(ico, msg, sub);
                containerPlanning.getChildren().add(empty);
                return;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM", new Locale("fr"));
            LocalDate today = LocalDate.now();
            int col = 0;
            HBox currentRow = null;

            for (Conge c : conges) {
                if (col % 2 == 0) {
                    currentRow = new HBox(12);
                    containerPlanning.getChildren().add(currentRow);
                }

                String nomUser   = "Utilisateur #" + c.getUtilisateurId();
                String posteUser = "";
                try {
                    Utilisateur user = serviceUtilisateur.getById(c.getUtilisateurId());
                    if (user != null) {
                        nomUser = user.getNomComplet();
                        if (user.getPoste() != null)      posteUser = user.getPoste();
                        if (user.getDepartement() != null) posteUser += (posteUser.isEmpty() ? "" : " · ") + user.getDepartement();
                    }
                } catch (Exception e) { System.err.println("⚠️ Erreur user planning : " + e.getMessage()); }

                String typeNomP = "Congé";
                try {
                    typeNomP = serviceTC.getAll().stream()
                            .filter(x -> x.getId() == c.getTypeCongeId())
                            .map(TypeConge::getLibelle).findFirst().orElse("Congé");
                } catch (Exception ignored) {}

                String accentColor  = getPlanningAccentColor(c.getStatut());
                String[] palette    = {"#667eea","#10b981","#f59e0b","#ef4444","#8b5cf6","#06b6d4","#ec4899"};
                String avatarColor  = palette[Math.abs(c.getUtilisateurId()) % palette.length];

                VBox card = new VBox(0);
                HBox.setHgrow(card, Priority.ALWAYS);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                        + " -fx-border-color: #eef2ff; -fx-border-width: 1; -fx-border-radius: 14;"
                        + " -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.09), 14, 0, 0, 4);");

                HBox header = new HBox(0);
                header.setAlignment(Pos.CENTER_LEFT);
                header.setStyle("-fx-background-color: " + accentColor + "18; -fx-padding: 12px 16px;"
                        + " -fx-background-radius: 14 14 0 0; -fx-border-color: " + accentColor + "33;"
                        + " -fx-border-width: 0 0 1 0;");

                Region headerBar = new Region();
                headerBar.setPrefWidth(3); headerBar.setMinWidth(3); headerBar.setMaxHeight(Double.MAX_VALUE);
                headerBar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 3;");
                header.getChildren().add(headerBar);

                StackPane avatar = new StackPane();
                Region avBg = new Region();
                avBg.setStyle("-fx-background-color: " + avatarColor + "; -fx-background-radius: 50;"
                        + " -fx-min-width: 38; -fx-min-height: 38; -fx-max-width: 38; -fx-max-height: 38;");
                String ini = nomUser.length() >= 2 ? nomUser.substring(0, 2).toUpperCase() : "?";
                Label avLbl = new Label(ini);
                avLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
                avatar.getChildren().addAll(avBg, avLbl);
                HBox avWrap = new HBox(avatar);
                avWrap.setAlignment(Pos.CENTER);
                avWrap.setPadding(new Insets(0, 12, 0, 10));

                VBox userInfo = new VBox(2);
                HBox.setHgrow(userInfo, Priority.ALWAYS);
                Label lblNomP = new Label(nomUser);
                lblNomP.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
                userInfo.getChildren().add(lblNomP);
                if (!posteUser.isEmpty()) {
                    Label lblPosteP = new Label(posteUser);
                    lblPosteP.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
                    userInfo.getChildren().add(lblPosteP);
                }

                Label statBadge = new Label(c.getStatut() != null ? c.getStatut() : "?");
                statBadge.setStyle(getBadgeStyle(c.getStatut()));
                header.getChildren().addAll(avWrap, userInfo, statBadge);

                VBox body = new VBox(10);
                body.setPadding(new Insets(14, 16, 14, 16));

                HBox typeRow = new HBox(8);
                typeRow.setAlignment(Pos.CENTER_LEFT);
                Label typeIco = new Label("🏷️");
                typeIco.setStyle("-fx-font-size: 12px;");
                Label typeLbl = new Label(typeNomP);
                typeLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #667eea;"
                        + " -fx-background-color: #eef2ff; -fx-padding: 3px 10px; -fx-background-radius: 20;"
                        + " -fx-border-color: #c7d2fe; -fx-border-width: 1; -fx-border-radius: 20;");
                typeRow.getChildren().addAll(typeIco, typeLbl);

                String ds = c.getDateDebut() != null ? c.getDateDebut().format(fmt) : "?";
                String de = c.getDateFin()   != null ? c.getDateFin().format(fmt)   : "?";
                HBox datesRow = new HBox(10);
                datesRow.setAlignment(Pos.CENTER_LEFT);
                datesRow.setStyle("-fx-background-color: #f8faff; -fx-background-radius: 8; -fx-padding: 8px 12px;"
                        + " -fx-border-color: #e8eeff; -fx-border-width: 1; -fx-border-radius: 8;");
                Label dateDebLabel = new Label("📅  " + ds);
                dateDebLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                Label arrow = new Label("→");
                arrow.setStyle("-fx-font-size: 14px; -fx-text-fill: #c7d2fe; -fx-font-weight: bold;");
                Label dateFinLabel = new Label(de);
                dateFinLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                Region dateSpacer = new Region();
                HBox.setHgrow(dateSpacer, Priority.ALWAYS);
                Label joursLbl = new Label(c.getNbrJours() + " jour" + (c.getNbrJours() > 1 ? "s" : ""));
                joursLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;"
                        + " -fx-background-color: " + accentColor + "; -fx-padding: 3px 12px; -fx-background-radius: 20;");
                datesRow.getChildren().addAll(dateDebLabel, arrow, dateFinLabel, dateSpacer, joursLbl);

                if (c.getDateDebut() != null && c.getDateFin() != null) {
                    long totalDays = ChronoUnit.DAYS.between(c.getDateDebut(), c.getDateFin()) + 1;
                    long elapsed   = Math.max(0, Math.min(totalDays, ChronoUnit.DAYS.between(c.getDateDebut(), today)));
                    double pct     = totalDays > 0 ? (double) elapsed / totalDays : 0;

                    String progressLabel, progressColor;
                    if (today.isBefore(c.getDateDebut()))       { progressLabel = "À venir";               progressColor = "#94a3b8"; pct = 0; }
                    else if (today.isAfter(c.getDateFin()))     { progressLabel = "Terminé";               progressColor = "#10b981"; pct = 1.0; }
                    else                                        { progressLabel = "En cours · J+" + elapsed; progressColor = accentColor; }

                    VBox progressBox = new VBox(5);
                    HBox progHeader  = new HBox();
                    progHeader.setAlignment(Pos.CENTER_LEFT);
                    Label progLbl = new Label(progressLabel);
                    progLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + progressColor + ";");
                    Region progSpacer = new Region();
                    HBox.setHgrow(progSpacer, Priority.ALWAYS);
                    Label pctLbl = new Label(Math.round(pct * 100) + "%");
                    pctLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
                    progHeader.getChildren().addAll(progLbl, progSpacer, pctLbl);

                    StackPane progressTrack = new StackPane();
                    Region track = new Region();
                    track.setPrefHeight(5);
                    track.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 5;");
                    HBox fillWrapper = new HBox();
                    fillWrapper.setAlignment(Pos.CENTER_LEFT);
                    Region fill = new Region();
                    fill.setPrefHeight(5);
                    double clampedPct = Math.max(0.02, Math.min(1.0, pct));
                    fill.prefWidthProperty().bind(progressTrack.widthProperty().multiply(clampedPct));
                    fill.setStyle("-fx-background-color: " + progressColor + "; -fx-background-radius: 5;");
                    fillWrapper.getChildren().add(fill);
                    progressTrack.getChildren().addAll(track, fillWrapper);
                    progressBox.getChildren().addAll(progHeader, progressTrack);
                    body.getChildren().addAll(typeRow, datesRow, progressBox);
                } else {
                    body.getChildren().addAll(typeRow, datesRow);
                }

                card.getChildren().addAll(header, body);
                currentRow.getChildren().add(card);
                col++;
            }

            if (col % 2 == 1 && currentRow != null) {
                Region ghost = new Region();
                HBox.setHgrow(ghost, Priority.ALWAYS);
                currentRow.getChildren().add(ghost);
            }

        } catch (Exception e) {
            Label lbl = new Label("Erreur chargement planning : " + e.getMessage());
            lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            containerPlanning.getChildren().add(lbl);
        }
    }

    private String getPlanningAccentColor(String statut) {
        if (statut == null) return "#94a3b8";
        return switch (statut.toLowerCase()) {
            case "approuvé"   -> "#10b981";
            case "refusé"     -> "#ef4444";
            case "en attente" -> "#f59e0b";
            default -> "#667eea";
        };
    }

    // ══════════════════════════════════════════════════════════════════
    //  CARTES SOLDES DÉTAILLÉES
    // ══════════════════════════════════════════════════════════════════

    private VBox creerCarteSolde(SoldeItem item) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                + " -fx-border-color: " + item.couleur + "33; -fx-border-width: 1; -fx-border-radius: 14;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane icoBox = new StackPane();
        Region icoBg = new Region();
        icoBg.setStyle("-fx-background-color: " + item.couleur + "22; -fx-background-radius: 10;"
                + " -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
        Label icoLbl = new Label(item.icone);
        icoLbl.setStyle("-fx-font-size: 18px;");
        icoBox.getChildren().addAll(icoBg, icoLbl);

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label nomLbl = new Label(item.nom);
        nomLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        nomLbl.setWrapText(true);
        Label catLbl = new Label(item.categorie);
        catLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + item.couleur + ";"
                + " -fx-background-color: " + item.couleur + "18; -fx-padding: 2px 8px; -fx-background-radius: 10;");
        titleBox.getChildren().addAll(nomLbl, catLbl);
        header.getChildren().addAll(icoBox, titleBox);

        Label valeurLbl = new Label(item.valeur);
        valeurLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + item.couleur + ";");

        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);
        if (item.enAttente != null) {
            Label attLbl = new Label("⏳ " + item.enAttente);
            attLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;"
                    + " -fx-background-color: #fffbeb; -fx-padding: 3px 10px; -fx-background-radius: 20;"
                    + " -fx-border-color: #fde68a; -fx-border-width: 1; -fx-border-radius: 20;");
            footer.getChildren().add(attLbl);
        }

        Region bottomBar = new Region();
        bottomBar.setPrefHeight(3);
        bottomBar.setStyle("-fx-background-color: linear-gradient(to right, " + item.couleur + ", " + item.couleur + "44);"
                + " -fx-background-radius: 3;");

        card.getChildren().addAll(header, valeurLbl);
        if (!footer.getChildren().isEmpty()) card.getChildren().add(footer);
        card.getChildren().add(bottomBar);
        return card;
    }

    private HBox creerResumeGlobal(int restantCP, int quotaTotal, int totalConges, int totalAbsences) {
        HBox resume = new HBox(0);
        resume.setAlignment(Pos.CENTER);
        resume.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-background-radius: 14;"
                + " -fx-padding: 20px; -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.30), 16, 0, 0, 6);");
        resume.setPadding(new Insets(20, 0, 0, 0));
        resume.getChildren().addAll(
                creerStatResume("🏖️", "Solde restant",   restantCP + " j",    "#ffffff"),
                creerSepResume(),
                creerStatResume("✅", "Congés pris",      totalConges + " j",  "#a7f3d0"),
                creerSepResume(),
                creerStatResume("📋", "Absences",         totalAbsences + " j","#fde68a"),
                creerSepResume(),
                creerStatResume("📊", "Quota annuel",     quotaTotal + " j",   "#c4b5fd")
        );
        return resume;
    }

    private VBox creerStatResume(String icone, String label, String valeur, String couleur) {
        VBox v = new VBox(4);
        v.setAlignment(Pos.CENTER);
        HBox.setHgrow(v, Priority.ALWAYS);
        Label ico = new Label(icone);
        ico.setStyle("-fx-font-size: 22px;");
        Label val = new Label(valeur);
        val.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: bold;");
        v.getChildren().addAll(ico, val, lbl);
        return v;
    }

    private Region creerSepResume() {
        Region sep = new Region();
        sep.setPrefWidth(1); sep.setMinWidth(1);
        sep.setMaxHeight(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.20);");
        sep.setPrefHeight(60);
        return sep;
    }

    // ══════════════════════════════════════════════════════════════════
    //  JOURS FÉRIÉS TUNISIE 2026
    // ══════════════════════════════════════════════════════════════════

    private void chargerJoursFeriesTunisie() {
        if (containerFeriesTunisie == null) return;
        containerFeriesTunisie.getChildren().clear();
        try {
            List<JourFerie> feries = getJoursFeriesTunisie2026();
            DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", new Locale("fr", "TN"));

            for (JourFerie ferie : feries) {
                HBox card = new HBox(0);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                        + " -fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-border-radius: 12;"
                        + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

                Region bar = new Region();
                bar.setPrefWidth(4); bar.setMinWidth(4); bar.setMaxHeight(Double.MAX_VALUE);
                bar.setStyle("-fx-background-color: " + ferie.couleur + "; -fx-background-radius: 12 0 0 12;");

                StackPane icoCircle = new StackPane();
                Region icoBg = new Region();
                icoBg.setStyle("-fx-background-color: " + ferie.couleur + "22; -fx-background-radius: 50;"
                        + " -fx-min-width: 44; -fx-min-height: 44; -fx-max-width: 44; -fx-max-height: 44;");
                Label icoLabel = new Label(ferie.icone);
                icoLabel.setStyle("-fx-font-size: 20px;");
                icoCircle.getChildren().addAll(icoBg, icoLabel);
                HBox icoWrap = new HBox(icoCircle);
                icoWrap.setAlignment(Pos.CENTER);
                icoWrap.setPadding(new Insets(12, 12, 12, 16));

                VBox infos = new VBox(3);
                infos.setPadding(new Insets(12, 0, 12, 0));
                HBox.setHgrow(infos, Priority.ALWAYS);
                Label nom  = new Label(ferie.nom);
                nom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                Label date = new Label(ferie.date.format(fmt));
                date.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                infos.getChildren().addAll(nom, date);

                Label badge = new Label(ferie.type);
                badge.setStyle("-fx-background-color: " + ferie.couleurBadge + "; -fx-text-fill: white;"
                        + " -fx-padding: 4px 12px; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: bold;");
                VBox badgeWrap = new VBox(badge);
                badgeWrap.setAlignment(Pos.CENTER);
                badgeWrap.setPadding(new Insets(0, 16, 0, 0));

                card.getChildren().addAll(bar, icoWrap, infos, badgeWrap);
                containerFeriesTunisie.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargerJoursFeriesTunisie: " + e.getMessage());
        }
    }

    private List<JourFerie> getJoursFeriesTunisie2026() {
        List<JourFerie> feries = new ArrayList<>();
        feries.add(new JourFerie("🎉","Jour de l'An",                  LocalDate.of(2026,1,1),  "Officiel",  "#3b82f6","#2563eb"));
        feries.add(new JourFerie("✊","Fête de la Révolution",          LocalDate.of(2026,1,14), "National",  "#ef4444","#dc2626"));
        feries.add(new JourFerie("🌸","Fête de l'Indépendance",         LocalDate.of(2026,3,20), "National",  "#10b981","#059669"));
        feries.add(new JourFerie("👨","Fête de la Jeunesse",            LocalDate.of(2026,3,21), "Officiel",  "#f59e0b","#d97706"));
        feries.add(new JourFerie("🕌","Aïd el-Fitr",                    LocalDate.of(2026,3,30), "Religieux", "#10b981","#059669"));
        feries.add(new JourFerie("🕌","Aïd el-Fitr (2ème jour)",        LocalDate.of(2026,3,31), "Religieux", "#10b981","#059669"));
        feries.add(new JourFerie("🎖️","Fête des Martyrs",               LocalDate.of(2026,4,9),  "National",  "#7c3aed","#6d28d9"));
        feries.add(new JourFerie("🛠️","Fête du Travail",                LocalDate.of(2026,5,1),  "Officiel",  "#ef4444","#dc2626"));
        feries.add(new JourFerie("🐑","Aïd el-Adha",                    LocalDate.of(2026,6,6),  "Religieux", "#f59e0b","#d97706"));
        feries.add(new JourFerie("🐑","Aïd el-Adha (2ème jour)",        LocalDate.of(2026,6,7),  "Religieux", "#f59e0b","#d97706"));
        feries.add(new JourFerie("📅","Nouvel An Hégire",               LocalDate.of(2026,7,18), "Religieux", "#8b5cf6","#7c3aed"));
        feries.add(new JourFerie("🇹🇳","Fête de la République",         LocalDate.of(2026,7,25), "National",  "#10b981","#059669"));
        feries.add(new JourFerie("👩","Fête de la Femme",               LocalDate.of(2026,8,13), "National",  "#ec4899","#db2777"));
        feries.add(new JourFerie("🌙","Mawlid (Naissance du Prophète)", LocalDate.of(2026,9,27), "Religieux", "#06b6d4","#0891b2"));
        feries.add(new JourFerie("🌊","Fête de l'Évacuation",           LocalDate.of(2026,10,15),"National",  "#06b6d4","#0891b2"));
        feries.sort(Comparator.comparing(f -> f.date));
        return feries;
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════

    private String iconePourTypeConge(String l) {
        if (l == null) return "📅";
        String lb = l.toLowerCase();
        if (lb.contains("maladie"))               return "🏥";
        if (lb.contains("maternit"))              return "🤱";
        if (lb.contains("paternit"))              return "👨‍👧";
        if (lb.contains("exceptionnel"))          return "⭐";
        if (lb.contains("sans solde"))            return "💸";
        return "🏖️";
    }

    private String couleurPourTypeConge(String l) {
        if (l == null) return "#667eea";
        String lb = l.toLowerCase();
        if (lb.contains("maladie"))               return "#ef4444";
        if (lb.contains("maternit"))              return "#ec4899";
        if (lb.contains("paternit"))              return "#06b6d4";
        if (lb.contains("exceptionnel"))          return "#f59e0b";
        if (lb.contains("sans solde"))            return "#94a3b8";
        return "#10b981";
    }

    private String iconePourTypeAbsence(String l) {
        if (l == null) return "📋";
        String lb = l.toLowerCase();
        if (lb.contains("justifi") && !lb.contains("in"))  return "✅";
        if (lb.contains("injustifi"))                      return "❌";
        if (lb.contains("médical") || lb.contains("medical") || lb.contains("médicale")) return "💊";
        return "📋";
    }

    private String couleurPourTypeAbsence(String l) {
        if (l == null) return "#64748b";
        String lb = l.toLowerCase();
        if (lb.contains("justifi") && !lb.contains("in"))  return "#10b981";
        if (lb.contains("injustifi"))                      return "#ef4444";
        if (lb.contains("médical") || lb.contains("medical") || lb.contains("médicale")) return "#8b5cf6";
        return "#64748b";
    }

    private String getBadgeStyle(String statut) {
        String base = "-fx-padding: 4px 12px; -fx-background-radius: 20; -fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;";
        if (statut == null) return base + " -fx-background-color: #94a3b8;";
        return switch (statut.toLowerCase()) {
            case "approuvé"   -> base + " -fx-background-color: #10b981;";
            case "refusé"     -> base + " -fx-background-color: #ef4444;";
            case "en attente" -> base + " -fx-background-color: #f59e0b;";
            default           -> base + " -fx-background-color: #64748b;";
        };
    }

    private void alert(String titre, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    // ── Classes internes ────────────────────────────────────────────────────

    private static class SoldeItem {
        String icone, nom, valeur, enAttente, couleur, categorie;
        SoldeItem(String icone, String nom, String valeur, String enAttente, String couleur, String categorie) {
            this.icone = icone; this.nom = nom; this.valeur = valeur;
            this.enAttente = enAttente; this.couleur = couleur; this.categorie = categorie;
        }
    }

    private static class JourFerie {
        String icone, nom, type, couleur, couleurBadge;
        LocalDate date;
        JourFerie(String icone, String nom, LocalDate date, String type, String couleur, String couleurBadge) {
            this.icone = icone; this.nom = nom; this.date = date;
            this.type = type; this.couleur = couleur; this.couleurBadge = couleurBadge;
        }
    }
}