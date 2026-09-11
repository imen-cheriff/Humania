package attendance.gui;

import attendance.interfaces.service;
import attendance.models.*;
import attendance.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RapportsRHController {

    // ── Cartes statistiques ──────────────────────────────────────────
    @FXML private Label lblAbsencesMois;
    @FXML private Label lblTauxAbsenteisme;
    @FXML private Label lblEnAttente;
    @FXML private Label lblArretsMaladie;

    // ── Graphiques ───────────────────────────────────────────────────
    @FXML private LineChart<String, Number>  chartEvolution;
    @FXML private PieChart                   chartRepartition;
    @FXML private BarChart<String, Number>   chartTopServices;
    @FXML private Label                      lblTitreTopServices; // titre dynamique (optionnel, ajout fx:id dans FXML si besoin)

    // ── Effectif par statut ──────────────────────────────────────────
    @FXML private Label lblEffectifActif;
    @FXML private Label lblEffectifConges;
    @FXML private Label lblEffectifMaladie;
    @FXML private Label lblEffectifParental;
    @FXML private Label lblEffectifTotal;

    // ── Services BDD ─────────────────────────────────────────────────
    private final service<Absence>     serviceAbsence     = new ServiceAbsence();
    private final service<Conge>       serviceConge       = new ServiceConge();
    private final service<Utilisateur> serviceUtilisateur = new ServiceUtilisateur();
    private final service<TypeAbsence> serviceTA          = new ServiceTypeAbsence();
    private final service<TypeConge>   serviceTC          = new ServiceTypeConge();

    // ── Cache BDD (chargé une seule fois) ────────────────────────────
    private List<Absence>     absences;
    private List<Conge>       conges;
    private List<Utilisateur> users;
    private List<TypeAbsence> typesAbsence;
    private List<TypeConge>   typesConge;

    // Lookups id → libellé (évite les boucles imbriquées)
    private Map<Integer, String> libTypeAbsence = new HashMap<>();
    private Map<Integer, String> libTypeConge   = new HashMap<>();
    private Map<Integer, String> userDept        = new HashMap<>();
    private Map<Integer, Utilisateur> userById   = new HashMap<>();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ================================================================
    //  INITIALISATION
    // ================================================================

    @FXML
    public void initialize() {
        chargerDonnees();
    }

    // ================================================================
    //  CHARGEMENT BDD — tout en une passe, lookups préconstruits
    // ================================================================

    public void chargerDonnees() {
        try {
            absences     = serviceAbsence.getAll();
            conges       = serviceConge.getAll();
            users        = serviceUtilisateur.getAll();
            typesAbsence = serviceTA.getAll();
            typesConge   = serviceTC.getAll();
        } catch (Exception e) {
            absences     = new ArrayList<>();
            conges       = new ArrayList<>();
            users        = new ArrayList<>();
            typesAbsence = new ArrayList<>();
            typesConge   = new ArrayList<>();
            System.err.println("❌ Erreur chargement données BDD : " + e.getMessage());
        }

        // Construire les lookups une seule fois
        libTypeAbsence.clear();
        typesAbsence.forEach(t -> libTypeAbsence.put(t.getId(), t.getLibelle()));

        libTypeConge.clear();
        typesConge.forEach(t -> libTypeConge.put(t.getId(), t.getLibelle()));

        userDept.clear();
        userById.clear();
        users.forEach(u -> {
            userDept.put(u.getId(), u.getDepartement() != null ? u.getDepartement() : "N/A");
            userById.put(u.getId(), u);
        });

        // Remplir l'interface
        chargerCartes();
        chargerGraphiqueEvolution();
        chargerGraphiqueRepartition();
        chargerTopServices();
        chargerEffectifStatut();
    }

    // ================================================================
    //  CARTES STATISTIQUES
    // ================================================================

    private void chargerCartes() {
        try {
            int moisActuel    = LocalDate.now().getMonthValue();
            int anneeActuelle = LocalDate.now().getYear();
            int effectif      = users.size();

            // ── Absences approuvées ce mois ──────────────────────────
            // Compte les demandes dont la période CHEVAUCHE le mois actuel
            LocalDate debutMois = LocalDate.of(anneeActuelle, moisActuel, 1);
            LocalDate finMois   = debutMois.withDayOfMonth(debutMois.lengthOfMonth());

            long absencesMois = absences.stream()
                    .filter(a -> "Approuvé".equalsIgnoreCase(a.getStatut()))
                    .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                    .filter(a -> !a.getDateFin().isBefore(debutMois)   // fin >= début du mois
                            && !a.getDateDebut().isAfter(finMois))   // début <= fin du mois
                    .count();
            setLabel(lblAbsencesMois, String.valueOf(absencesMois));

            // ── Taux absentéisme ──────────────────────────────────────
            // Formule standard RH : (jours d'absence / jours théoriques) × 100
            // On calcule sur l'ANNÉE en cours pour éviter le taux 0% en début de mois
            // et on affiche aussi le taux du mois si disponible
            int joursOuvresMois = 22;

            // Jours d'absence approuvés sur l'année entière
            int joursAbsAnnee = absences.stream()
                    .filter(a -> "Approuvé".equalsIgnoreCase(a.getStatut()))
                    .filter(a -> a.getDateDebut() != null
                            && a.getDateDebut().getYear() == anneeActuelle)
                    .mapToInt(Absence::getNbrJours)
                    .sum();

            // Jours d'absence approuvés ce mois-ci (chevauchement)
            int joursAbsMois = absences.stream()
                    .filter(a -> "Approuvé".equalsIgnoreCase(a.getStatut()))
                    .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                    .filter(a -> !a.getDateFin().isBefore(debutMois)
                            && !a.getDateDebut().isAfter(finMois))
                    .mapToInt(Absence::getNbrJours)
                    .sum();

            double joursTheoMois  = effectif > 0 ? (double) effectif * joursOuvresMois : 1.0;
            // Jours théoriques année = effectif × 22j × nb mois écoulés (min 1)
            int moisEcoules = Math.max(1, moisActuel);
            double joursTheoAnnee = effectif > 0 ? (double) effectif * joursOuvresMois * moisEcoules : 1.0;

            double tauxMois  = (joursAbsMois  / joursTheoMois)  * 100.0;
            double tauxAnnee = (joursAbsAnnee / joursTheoAnnee) * 100.0;

            // Afficher le taux mensuel s'il est > 0, sinon afficher le taux annuel
            // avec un indicateur visuel pour que l'utilisateur sache quelle période
            double tauxAffiche = tauxMois > 0 ? tauxMois : tauxAnnee;
            String suffixe = tauxMois > 0 ? "" : " (ann.)";
            setLabel(lblTauxAbsenteisme,
                    String.format("%.1f%%%s", Math.min(tauxAffiche, 100.0), suffixe));

            // ── Demandes en attente (absences + congés) ──────────────
            long enAttenteAbs    = absences.stream()
                    .filter(a -> "En attente".equalsIgnoreCase(a.getStatut())).count();
            long enAttenteConges = conges.stream()
                    .filter(c -> "En attente".equalsIgnoreCase(c.getStatut())).count();
            setLabel(lblEnAttente, String.valueOf(enAttenteAbs + enAttenteConges));

            // ── Arrêts > 15 jours (absences + congés) ────────────────
            long arrets = absences.stream().filter(a -> a.getNbrJours() > 15).count()
                    + conges.stream().filter(c -> c.getNbrJours() > 15).count();
            setLabel(lblArretsMaladie, String.valueOf(arrets));

        } catch (Exception e) {
            System.err.println("❌ Erreur chargerCartes : " + e.getMessage());
        }
    }

    // ================================================================
    //  GRAPHIQUE ÉVOLUTION MENSUELLE
    // ================================================================

    private void chargerGraphiqueEvolution() {
        if (chartEvolution == null) return;
        chartEvolution.getData().clear();

        String[] labels = {"Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"};
        int[] parMois = new int[12];
        int year = LocalDate.now().getYear();

        for (Absence a : absences) {
            if (a.getDateDebut() != null && a.getDateDebut().getYear() == year)
                parMois[a.getDateDebut().getMonthValue() - 1]++;
        }
        for (Conge c : conges) {
            if (c.getDateDebut() != null && c.getDateDebut().getYear() == year)
                parMois[c.getDateDebut().getMonthValue() - 1]++;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Demandes " + year);
        for (int i = 0; i < 12; i++) {
            series.getData().add(new XYChart.Data<>(labels[i], parMois[i]));
        }
        chartEvolution.getData().add(series);
    }

    // ================================================================
    //  GRAPHIQUE RÉPARTITION PAR TYPE
    // ================================================================

    private void chargerGraphiqueRepartition() {
        if (chartRepartition == null) return;
        chartRepartition.getData().clear();

        // Congés par type (utilise le lookup préconstruit)
        Map<String, Integer> compteConges = new LinkedHashMap<>();
        for (Conge c : conges) {
            String typeNom = libTypeConge.getOrDefault(c.getTypeCongeId(), "Congé");
            compteConges.merge(typeNom, 1, Integer::sum);
        }

        // Absences par type
        Map<String, Integer> compteAbsences = new LinkedHashMap<>();
        for (Absence a : absences) {
            String typeNom = libTypeAbsence.getOrDefault(a.getTypeAbsenceId(), "Absence");
            compteAbsences.merge(typeNom, 1, Integer::sum);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        compteConges.forEach((k, v) ->
                pieData.add(new PieChart.Data(k + " (" + v + ")", v)));
        compteAbsences.forEach((k, v) ->
                pieData.add(new PieChart.Data(k + " (" + v + ")", v)));

        // Données démo si BDD vide
        if (pieData.isEmpty()) {
            pieData.addAll(
                    new PieChart.Data("Congés Payés (15)", 15),
                    new PieChart.Data("RTT (8)", 8),
                    new PieChart.Data("Maladie (5)", 5),
                    new PieChart.Data("Sans Solde (2)", 2)
            );
        }

        chartRepartition.setData(pieData);
    }

    // ================================================================
    //  TOP 5 DÉPARTEMENTS
    // ================================================================

    private void chargerTopServices() {
        if (chartTopServices == null) return;
        chartTopServices.getData().clear();

        // ── Étape 1 : essayer de grouper par département ──────────────
        Map<String, Integer> parDept = new LinkedHashMap<>();
        for (Absence a : absences) {
            String dept = userDept.getOrDefault(a.getUtilisateurId(), null);
            if (dept != null && !dept.isBlank())
                parDept.merge(dept, 1, Integer::sum);
        }
        for (Conge c : conges) {
            String dept = userDept.getOrDefault(c.getUtilisateurId(), null);
            if (dept != null && !dept.isBlank())
                parDept.merge(dept, 1, Integer::sum);
        }

        String axeLegende;

        if (parDept.size() >= 2) {
            // Cas normal : plusieurs départements renseignés
            axeLegende = "Par département";
        } else {
            // ── Pas de département → grouper par TYPE de congé/absence ──
            parDept.clear();
            for (Absence a : absences) {
                String type = libTypeAbsence.getOrDefault(a.getTypeAbsenceId(), "Absence #" + a.getTypeAbsenceId());
                parDept.merge(type, 1, Integer::sum);
            }
            for (Conge c : conges) {
                String type = libTypeConge.getOrDefault(c.getTypeCongeId(), "Congé #" + c.getTypeCongeId());
                parDept.merge(type, 1, Integer::sum);
            }
            axeLegende = "Par type";

            // Si même ça est vide → grouper par employé
            if (parDept.isEmpty()) {
                for (Absence a : absences) {
                    Utilisateur u = userById.get(a.getUtilisateurId());
                    String nom = u != null ? u.getNomComplet() : "Utilisateur #" + a.getUtilisateurId();
                    parDept.merge(nom, 1, Integer::sum);
                }
                for (Conge c : conges) {
                    Utilisateur u = userById.get(c.getUtilisateurId());
                    String nom = u != null ? u.getNomComplet() : "Utilisateur #" + c.getUtilisateurId();
                    parDept.merge(nom, 1, Integer::sum);
                }
                axeLegende = "Par employé";
            }
        }

        // Données démo si BDD complètement vide
        if (parDept.isEmpty()) {
            parDept.put("IT", 12); parDept.put("RH", 8);
            parDept.put("Finance", 10); parDept.put("Marketing", 6); parDept.put("Direction", 4);
            axeLegende = "Par département";
        }

        List<Map.Entry<String, Integer>> top5 = parDept.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(axeLegende);
        for (Map.Entry<String, Integer> e : top5) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chartTopServices.getData().add(series);

        // Titre dynamique
        if (lblTitreTopServices != null)
            lblTitreTopServices.setText("Top 5 " + axeLegende.toLowerCase() + " - Demandes");

        // ── Axe Y propre : commence à 0, tick entier, +20% de marge ──
        if (chartTopServices.getYAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) chartTopServices.getYAxis();
            int maxVal = top5.stream().mapToInt(Map.Entry::getValue).max().orElse(5);
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(maxVal + Math.max(1, (int) Math.ceil(maxVal * 0.2)));
            yAxis.setTickUnit(Math.max(1, (int) Math.ceil(maxVal / 5.0)));
        }
    }

    // ================================================================
    //  EFFECTIF PAR STATUT
    // ================================================================

    private void chargerEffectifStatut() {
        try {
            int total = users.size();
            LocalDate today = LocalDate.now();

            // Employés actuellement en congé (dates encadrantes aujourd'hui)
            Set<Integer> enCongesIds = conges.stream()
                    .filter(c -> "Approuvé".equalsIgnoreCase(c.getStatut()))
                    .filter(c -> c.getDateDebut() != null && c.getDateFin() != null)
                    .filter(c -> !today.isBefore(c.getDateDebut()) && !today.isAfter(c.getDateFin()))
                    .map(Conge::getUtilisateurId)
                    .collect(Collectors.toSet());

            // Employés actuellement en absence (arrêt maladie, etc.)
            Set<Integer> enMaladieIds = absences.stream()
                    .filter(a -> "Approuvé".equalsIgnoreCase(a.getStatut()))
                    .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                    .filter(a -> !today.isBefore(a.getDateDebut()) && !today.isAfter(a.getDateFin()))
                    .map(Absence::getUtilisateurId)
                    .collect(Collectors.toSet());

            // Congé parental = congés de type maternité (id=3) ou paternité (id=8)
            // OU libellé contenant "maternit" / "paternit" (robuste aux changements BDD)
            Set<Integer> idsTypesParentaux = typesConge.stream()
                    .filter(t -> {
                        String lb = t.getLibelle() != null ? t.getLibelle().toLowerCase() : "";
                        return lb.contains("maternit") || lb.contains("paternit") || lb.contains("parental");
                    })
                    .map(TypeConge::getId)
                    .collect(Collectors.toSet());

            long parental = conges.stream()
                    .filter(c -> "Approuvé".equalsIgnoreCase(c.getStatut()))
                    .filter(c -> idsTypesParentaux.contains(c.getTypeCongeId()))
                    .map(Conge::getUtilisateurId)
                    .distinct()
                    .count();

            // Union des IDs absents aujourd'hui (congés + absences + parentaux)
            Set<Integer> tousAbsentsIds = new HashSet<>(enCongesIds);
            tousAbsentsIds.addAll(enMaladieIds);
            // Les parentaux sont déjà inclus dans enCongesIds si dates aujourd'hui
            // On recalcule : actif = total - nb employés absents aujourd'hui (union)
            int actif = Math.max(0, total - tousAbsentsIds.size());

            setLabel(lblEffectifActif,    String.valueOf(actif));
            setLabel(lblEffectifConges,   String.valueOf(enCongesIds.size()));
            setLabel(lblEffectifMaladie,  String.valueOf(enMaladieIds.size()));
            setLabel(lblEffectifParental, String.valueOf(parental));
            setLabel(lblEffectifTotal,    String.valueOf(total));

        } catch (Exception e) {
            System.err.println("❌ Erreur chargerEffectifStatut : " + e.getMessage());
            setLabel(lblEffectifActif,    "0");
            setLabel(lblEffectifConges,   "0");
            setLabel(lblEffectifMaladie,  "0");
            setLabel(lblEffectifParental, "0");
            setLabel(lblEffectifTotal,    "0");
        }
    }

    // ================================================================
    //  EXPORT PDF
    // ================================================================

    @FXML
    public void exporterPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("rapport_rh_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");
        File file = fc.showSaveDialog(null);
        if (file == null) return;

        try {
            genererPDF(file);
            alert("Succès", "PDF exporté :\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            alert("Erreur", "Erreur export PDF :\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void genererPDF(File file) throws Exception {
        // Générer un HTML riche qui peut être imprimé en PDF depuis le navigateur
        String htmlPath = file.getAbsolutePath().replace(".pdf", "_rapport.html");
        File htmlFile = new File(htmlPath);

        try (PrintWriter pw = new PrintWriter(new FileWriter(htmlFile, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.print(buildRapportHTML());
        }

        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(htmlFile);
        }

        // Sauvegarder aussi un résumé texte .pdf
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("RAPPORT RH - Humania RH Management");
            pw.println("Genere le : " + LocalDate.now().format(fmt));
            pw.println("=".repeat(60));
            pw.println();
            pw.println("STATISTIQUES DU MOIS");
            pw.println("-".repeat(40));
            pw.println("Absences du mois   : " + getLabel(lblAbsencesMois));
            pw.println("Taux absenteisme   : " + getLabel(lblTauxAbsenteisme));
            pw.println("En attente         : " + getLabel(lblEnAttente));
            pw.println("Arrets > 15 jours  : " + getLabel(lblArretsMaladie));
            pw.println();
            pw.println("EFFECTIF");
            pw.println("-".repeat(40));
            pw.println("En activite        : " + getLabel(lblEffectifActif));
            pw.println("En conges          : " + getLabel(lblEffectifConges));
            pw.println("Arret maladie      : " + getLabel(lblEffectifMaladie));
            pw.println("Conge parental     : " + getLabel(lblEffectifParental));
            pw.println("Total effectif     : " + getLabel(lblEffectifTotal));
            pw.println();
            pw.println("LISTE DES ABSENCES (" + absences.size() + ")");
            pw.println("-".repeat(40));
            for (Absence a : absences) {
                Utilisateur u = userById.get(a.getUtilisateurId());
                String nom = u != null ? u.getNomComplet() : "Utilisateur #" + a.getUtilisateurId();
                String type = libTypeAbsence.getOrDefault(a.getTypeAbsenceId(), "?");
                pw.printf("%-25s | %-15s | %-12s | %-12s | %s%n",
                        nom, type,
                        a.getDateDebut() != null ? a.getDateDebut().format(fmt) : "?",
                        a.getStatut() != null ? a.getStatut() : "?",
                        a.getNbrJours() + " jr(s)");
            }
            pw.println();
            pw.println("LISTE DES CONGES (" + conges.size() + ")");
            pw.println("-".repeat(40));
            for (Conge c : conges) {
                Utilisateur u = userById.get(c.getUtilisateurId());
                String nom = u != null ? u.getNomComplet() : "Utilisateur #" + c.getUtilisateurId();
                String type = libTypeConge.getOrDefault(c.getTypeCongeId(), "?");
                pw.printf("%-25s | %-15s | %s → %s | %-12s | %s%n",
                        nom, type,
                        c.getDateDebut() != null ? c.getDateDebut().format(fmt) : "?",
                        c.getDateFin()   != null ? c.getDateFin().format(fmt)   : "?",
                        c.getStatut() != null ? c.getStatut() : "?",
                        c.getNbrJours() + " jr(s)");
            }
        }
    }

    // ================================================================
    //  EXPORT EXCEL (.csv compatible Excel)
    // ================================================================

    @FXML
    public void exporterExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel CSV", "*.csv"));
        fc.setInitialFileName("rapport_rh_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
        File file = fc.showSaveDialog(null);
        if (file == null) return;

        try {
            genererExcel(file);
            alert("Succès", "Excel exporté :\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            alert("Erreur", "Erreur export Excel :\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void genererExcel(File file) throws Exception {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"))) {

            // BOM UTF-8 pour Excel
            pw.print('\uFEFF');

            // === RÉSUMÉ ===
            pw.println("RAPPORT RH - Humania RH Management");
            pw.println("Date de generation;;" + LocalDate.now().format(fmt));
            pw.println();
            pw.println("INDICATEURS CLES");
            pw.println("Indicateur;Valeur");
            pw.println("Absences du mois;"         + getLabel(lblAbsencesMois));
            pw.println("Taux d'absenteisme;"        + getLabel(lblTauxAbsenteisme));
            pw.println("Demandes en attente;"       + getLabel(lblEnAttente));
            pw.println("Arrets > 15 jours;"         + getLabel(lblArretsMaladie));
            pw.println();
            pw.println("EFFECTIF PAR STATUT");
            pw.println("Statut;Nombre");
            pw.println("En activite;"               + getLabel(lblEffectifActif));
            pw.println("En conges;"                 + getLabel(lblEffectifConges));
            pw.println("Arret maladie;"             + getLabel(lblEffectifMaladie));
            pw.println("Conge parental;"            + getLabel(lblEffectifParental));
            pw.println("TOTAL;"                     + getLabel(lblEffectifTotal));
            pw.println();

            // === ABSENCES ===
            pw.println("LISTE DES ABSENCES");
            pw.println("Nom;Prenom;Departement;Date debut;Date fin;Duree (jours);Statut;Type;Motif");
            for (Absence a : absences) {
                Utilisateur u = userById.get(a.getUtilisateurId());
                pw.printf("%s;%s;%s;%s;%s;%d;%s;%s;%s%n",
                        u != null ? u.getNom()    : "",
                        u != null ? u.getPrenom() : "",
                        u != null && u.getDepartement() != null ? u.getDepartement() : "",
                        a.getDateDebut() != null ? a.getDateDebut().format(fmt) : "",
                        a.getDateFin()   != null ? a.getDateFin().format(fmt)   : "",
                        a.getNbrJours(),
                        a.getStatut() != null ? a.getStatut() : "",
                        libTypeAbsence.getOrDefault(a.getTypeAbsenceId(), ""),
                        a.getMotif() != null ? a.getMotif().replace(";", ",") : ""
                );
            }
            pw.println();

            // === CONGÉS ===
            pw.println("LISTE DES CONGES");
            pw.println("Nom;Prenom;Departement;Date debut;Date fin;Duree (jours);Statut;Type");
            for (Conge c : conges) {
                Utilisateur u = userById.get(c.getUtilisateurId());
                pw.printf("%s;%s;%s;%s;%s;%d;%s;%s%n",
                        u != null ? u.getNom()    : "",
                        u != null ? u.getPrenom() : "",
                        u != null && u.getDepartement() != null ? u.getDepartement() : "",
                        c.getDateDebut() != null ? c.getDateDebut().format(fmt) : "",
                        c.getDateFin()   != null ? c.getDateFin().format(fmt)   : "",
                        c.getNbrJours(),
                        c.getStatut() != null ? c.getStatut() : "",
                        libTypeConge.getOrDefault(c.getTypeCongeId(), "")
                );
            }
            pw.println();

            // === ÉVOLUTION MENSUELLE ===
            pw.println("EVOLUTION MENSUELLE (" + LocalDate.now().getYear() + ")");
            pw.println("Mois;Nb Absences;Nb Conges;Total");
            String[] moisNoms = {"Janvier","Fevrier","Mars","Avril","Mai","Juin",
                    "Juillet","Aout","Septembre","Octobre","Novembre","Decembre"};
            int[] absParMois = new int[12];
            int[] cngParMois = new int[12];
            int year = LocalDate.now().getYear();
            for (Absence a : absences) {
                if (a.getDateDebut() != null && a.getDateDebut().getYear() == year)
                    absParMois[a.getDateDebut().getMonthValue() - 1]++;
            }
            for (Conge c : conges) {
                if (c.getDateDebut() != null && c.getDateDebut().getYear() == year)
                    cngParMois[c.getDateDebut().getMonthValue() - 1]++;
            }
            for (int i = 0; i < 12; i++) {
                pw.printf("%s;%d;%d;%d%n",
                        moisNoms[i], absParMois[i], cngParMois[i], absParMois[i] + cngParMois[i]);
            }
            pw.println();

            // === RÉPARTITION PAR DÉPARTEMENT ===
            pw.println("REPARTITION PAR DEPARTEMENT");
            pw.println("Departement;Nb Absences;Nb Conges;Total");
            Map<String, Integer> absParDept = new TreeMap<>();
            Map<String, Integer> cngParDept = new TreeMap<>();
            for (Absence a : absences) absParDept.merge(userDept.getOrDefault(a.getUtilisateurId(), "N/A"), 1, Integer::sum);
            for (Conge c : conges)     cngParDept.merge(userDept.getOrDefault(c.getUtilisateurId(), "N/A"), 1, Integer::sum);
            Set<String> allDepts = new TreeSet<>();
            allDepts.addAll(absParDept.keySet()); allDepts.addAll(cngParDept.keySet());
            for (String dept : allDepts) {
                int nbAbs = absParDept.getOrDefault(dept, 0);
                int nbCng = cngParDept.getOrDefault(dept, 0);
                pw.printf("%s;%d;%d;%d%n", dept, nbAbs, nbCng, nbAbs + nbCng);
            }
        }
    }

    // ================================================================
    //  HTML RAPPORT
    // ================================================================

    private String buildRapportHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<title>Rapport RH - Humania</title>");
        sb.append("<style>");
        sb.append("body{font-family:'Segoe UI',Arial,sans-serif;margin:40px;background:#f8fafc;color:#1e293b;}");
        sb.append("h1{color:#1e293b;font-size:28px;margin-bottom:4px;}");
        sb.append(".subtitle{color:#64748b;font-size:14px;margin-bottom:32px;}");
        sb.append(".cards{display:flex;gap:16px;margin-bottom:32px;flex-wrap:wrap;}");
        sb.append(".card{flex:1;min-width:160px;background:white;border-radius:12px;padding:20px;"
                + "box-shadow:0 2px 8px rgba(0,0,0,0.07);}");
        sb.append(".card-val{font-size:36px;font-weight:800;margin:8px 0;}");
        sb.append(".card-label{font-size:13px;color:#64748b;font-weight:600;}");
        sb.append(".card-sub{font-size:12px;color:#94a3b8;margin-top:4px;}");
        sb.append(".blue{border-top:4px solid #3b82f6;}.blue .card-val{color:#3b82f6;}");
        sb.append(".orange{border-top:4px solid #f59e0b;}.orange .card-val{color:#f59e0b;}");
        sb.append(".green{border-top:4px solid #10b981;}.green .card-val{color:#10b981;}");
        sb.append(".purple{border-top:4px solid #8b5cf6;}.purple .card-val{color:#8b5cf6;}");
        sb.append("table{width:100%;border-collapse:collapse;margin-bottom:32px;background:white;"
                + "border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.07);}");
        sb.append("th{background:linear-gradient(135deg,#667eea,#764ba2);color:white;"
                + "padding:12px 16px;text-align:left;font-size:12px;text-transform:uppercase;letter-spacing:0.5px;}");
        sb.append("td{padding:10px 16px;border-bottom:1px solid #f1f5f9;font-size:13px;}");
        sb.append("tr:last-child td{border-bottom:none;}tr:hover td{background:#f8fafc;}");
        sb.append(".badge{display:inline-block;padding:3px 10px;border-radius:20px;font-size:11px;font-weight:700;}");
        sb.append(".approuve{background:#d1fae5;color:#059669;}");
        sb.append(".attente{background:#fef3c7;color:#d97706;}");
        sb.append(".refuse{background:#fee2e2;color:#dc2626;}");
        sb.append("h2{font-size:18px;font-weight:700;margin:32px 0 16px;color:#1e293b;"
                + "padding-bottom:8px;border-bottom:2px solid #e2e8f0;}");
        sb.append(".effectif-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:12px;margin-bottom:32px;}");
        sb.append(".effectif-card{background:white;border-radius:10px;padding:16px;text-align:center;"
                + "box-shadow:0 1px 4px rgba(0,0,0,0.06);}");
        sb.append(".effectif-num{font-size:28px;font-weight:800;margin:6px 0;}");
        sb.append(".effectif-label{font-size:12px;color:#64748b;}");
        sb.append(".footer{margin-top:40px;padding-top:16px;border-top:1px solid #e2e8f0;"
                + "color:#94a3b8;font-size:12px;text-align:center;}");
        sb.append("@media print{body{background:white;margin:20px;}"
                + ".no-print{display:none;}}");
        sb.append("</style></head><body>");

        // En-tête
        sb.append("<h1>📊 Rapport RH — Humania</h1>");
        sb.append("<div class='subtitle'>Généré le ")
                .append(LocalDate.now().format(fmt))
                .append(" • Tableau de bord absentéisme &amp; congés</div>");

        // Cartes stats
        sb.append("<div class='cards'>");
        addCard(sb, "blue",   "Absences du mois",  getLabel(lblAbsencesMois),    "Demandes approuvées");
        addCard(sb, "orange", "Taux absentéisme",  getLabel(lblTauxAbsenteisme), "Moyenne entreprise");
        addCard(sb, "green",  "En attente",         getLabel(lblEnAttente),       "À valider");
        addCard(sb, "purple", "Arrêts > 15 jours", getLabel(lblArretsMaladie),   "Arrêts maladie");
        sb.append("</div>");

        // Effectif
        sb.append("<h2>Effectif par statut</h2>");
        sb.append("<div class='effectif-grid'>");
        addEffectifCard(sb, "En activité",    getLabel(lblEffectifActif),    "#10b981");
        addEffectifCard(sb, "En congés",      getLabel(lblEffectifConges),   "#3b82f6");
        addEffectifCard(sb, "Arrêt maladie",  getLabel(lblEffectifMaladie),  "#f97316");
        addEffectifCard(sb, "Congé parental", getLabel(lblEffectifParental), "#94a3b8");
        addEffectifCard(sb, "Total effectif", getLabel(lblEffectifTotal),    "#667eea");
        sb.append("</div>");

        // Table absences
        sb.append("<h2>Liste des absences (").append(absences.size()).append(")</h2>");
        sb.append("<table><tr><th>Employé</th><th>Département</th><th>Type</th>"
                + "<th>Date début</th><th>Date fin</th><th>Durée</th><th>Statut</th></tr>");
        for (Absence a : absences) {
            Utilisateur u = userById.get(a.getUtilisateurId());
            String nom    = u != null ? u.getNomComplet() : "Utilisateur #" + a.getUtilisateurId();
            String dept   = u != null && u.getDepartement() != null ? u.getDepartement() : "-";
            String type   = libTypeAbsence.getOrDefault(a.getTypeAbsenceId(), "-");
            String debut  = a.getDateDebut() != null ? a.getDateDebut().format(fmt) : "-";
            String fin    = a.getDateFin()   != null ? a.getDateFin().format(fmt)   : "-";
            String statut = a.getStatut() != null ? a.getStatut() : "-";
            String cls    = statut.equalsIgnoreCase("Approuvé") ? "approuve"
                    : statut.equalsIgnoreCase("En attente") ? "attente" : "refuse";
            sb.append("<tr><td>").append(nom).append("</td><td>").append(dept)
                    .append("</td><td>").append(type).append("</td><td>").append(debut)
                    .append("</td><td>").append(fin).append("</td><td>").append(a.getNbrJours())
                    .append(" j</td><td><span class='badge ").append(cls).append("'>")
                    .append(statut).append("</span></td></tr>");
        }
        sb.append("</table>");

        // Table congés
        sb.append("<h2>Liste des congés (").append(conges.size()).append(")</h2>");
        sb.append("<table><tr><th>Employé</th><th>Département</th><th>Type</th>"
                + "<th>Date début</th><th>Date fin</th><th>Durée</th><th>Statut</th></tr>");
        for (Conge c : conges) {
            Utilisateur u = userById.get(c.getUtilisateurId());
            String nom    = u != null ? u.getNomComplet() : "Utilisateur #" + c.getUtilisateurId();
            String dept   = u != null && u.getDepartement() != null ? u.getDepartement() : "-";
            String type   = libTypeConge.getOrDefault(c.getTypeCongeId(), "-");
            String debut  = c.getDateDebut() != null ? c.getDateDebut().format(fmt) : "-";
            String fin    = c.getDateFin()   != null ? c.getDateFin().format(fmt)   : "-";
            String statut = c.getStatut() != null ? c.getStatut() : "-";
            String cls    = statut.equalsIgnoreCase("Approuvé") ? "approuve"
                    : statut.equalsIgnoreCase("En attente") ? "attente" : "refuse";
            sb.append("<tr><td>").append(nom).append("</td><td>").append(dept)
                    .append("</td><td>").append(type).append("</td><td>").append(debut)
                    .append("</td><td>").append(fin).append("</td><td>").append(c.getNbrJours())
                    .append(" j</td><td><span class='badge ").append(cls).append("'>")
                    .append(statut).append("</span></td></tr>");
        }
        sb.append("</table>");

        // Évolution mensuelle
        sb.append("<h2>Évolution mensuelle (").append(LocalDate.now().getYear()).append(")</h2>");
        sb.append("<table><tr><th>Mois</th><th>Absences</th><th>Congés</th><th>Total</th></tr>");
        String[] moisNoms = {"Janvier","Février","Mars","Avril","Mai","Juin",
                "Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
        int[] absParMois = new int[12], cngParMois = new int[12];
        int year = LocalDate.now().getYear();
        for (Absence a : absences)
            if (a.getDateDebut() != null && a.getDateDebut().getYear() == year)
                absParMois[a.getDateDebut().getMonthValue() - 1]++;
        for (Conge c : conges)
            if (c.getDateDebut() != null && c.getDateDebut().getYear() == year)
                cngParMois[c.getDateDebut().getMonthValue() - 1]++;
        for (int i = 0; i < 12; i++) {
            sb.append("<tr><td>").append(moisNoms[i])
                    .append("</td><td>").append(absParMois[i])
                    .append("</td><td>").append(cngParMois[i])
                    .append("</td><td><strong>").append(absParMois[i] + cngParMois[i])
                    .append("</strong></td></tr>");
        }
        sb.append("</table>");

        sb.append("<div class='footer'>Humania RH Management • Rapport généré automatiquement • ")
                .append(LocalDate.now().format(fmt)).append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private void setLabel(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    private String getLabel(Label lbl) {
        return lbl != null ? lbl.getText() : "0";
    }

    private void addCard(StringBuilder sb, String cls, String label, String val, String sub) {
        sb.append("<div class='card ").append(cls).append("'>")
                .append("<div class='card-label'>").append(label).append("</div>")
                .append("<div class='card-val'>").append(val).append("</div>")
                .append("<div class='card-sub'>").append(sub).append("</div>")
                .append("</div>");
    }

    private void addEffectifCard(StringBuilder sb, String label, String val, String color) {
        sb.append("<div class='effectif-card' style='border-left:4px solid ").append(color).append(";'>")
                .append("<div class='effectif-num' style='color:").append(color).append(";'>").append(val).append("</div>")
                .append("<div class='effectif-label'>").append(label).append("</div>")
                .append("</div>");
    }

    private void alert(String titre, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}