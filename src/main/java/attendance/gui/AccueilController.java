package attendance.gui;

import attendance.interfaces.service;
import attendance.models.*;
import attendance.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.List;

public class AccueilController {

    @FXML private Label lblTotalConges;
    @FXML private Label lblTotalAbsences;
    @FXML private Label lblTotalTypes;
    @FXML private LineChart<String, Number> chartEvolution;
    @FXML private PieChart chartRepartition;
    @FXML private VBox containerLeaveBalances;

    private final service<Conge>       serviceConge       = new ServiceConge();
    private final service<Absence>     serviceAbsence     = new ServiceAbsence();
    private final service<TypeConge>   serviceTC          = new ServiceTypeConge();
    private final service<TypeAbsence> serviceTA          = new ServiceTypeAbsence();
    private final service<Utilisateur> serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        chargerDonnees();
    }

    public void chargerDonnees() {
        chargerStats();
        chargerGraphiqueEvolution();
        chargerGraphiqueRepartition();
        chargerLeaveBalances();
    }



    private void chargerStats() {
        try {
            if (lblTotalConges != null)
                lblTotalConges.setText(String.valueOf(serviceConge.getAll().size()));
            if (lblTotalAbsences != null)
                lblTotalAbsences.setText(String.valueOf(serviceAbsence.getAll().size()));
            if (lblTotalTypes != null)
                lblTotalTypes.setText(String.valueOf(serviceTC.getAll().size() + serviceTA.getAll().size()));
        } catch (Exception e) {
            System.err.println("Erreur stats : " + e.getMessage());
        }
    }



    private void chargerGraphiqueEvolution() {
        if (chartEvolution == null) return;
        chartEvolution.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        try {
            List<Absence> absences = serviceAbsence.getAll();
            int[] parMois = new int[12];
            String[] labels = {"Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"};
            for (Absence a : absences)
                if (a.getDateDebut() != null) parMois[a.getDateDebut().getMonthValue() - 1]++;
            for (int i = 0; i < 12; i++)
                series.getData().add(new XYChart.Data<>(labels[i], parMois[i]));
        } catch (Exception e) {
            String[] m = {"Jan","Fév","Mar","Avr","Mai","Jun","Jul"};
            int[]    v = {4, 6, 3, 8, 5, 10, 12};
            for (int i = 0; i < m.length; i++)
                series.getData().add(new XYChart.Data<>(m[i], v[i]));
        }
        chartEvolution.getData().add(series);
    }

    private void chargerGraphiqueRepartition() {
        if (chartRepartition == null) return;
        chartRepartition.getData().clear();
        try {
            int tc = serviceConge.getAll().size();
            int ta = serviceAbsence.getAll().size();
            ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                    new PieChart.Data("Congés Payés (" + tc + ")", Math.max(tc, 1)),
                    new PieChart.Data("RTT (8)", 8),
                    new PieChart.Data("Maladie (" + ta + ")", Math.max(ta, 1)),
                    new PieChart.Data("Sans Solde (2)", 2)
            );
            chartRepartition.setData(data);
        } catch (Exception e) {
            System.err.println("Erreur graphique : " + e.getMessage());
        }
    }



    private void chargerLeaveBalances() {
        if (containerLeaveBalances == null) return;
        containerLeaveBalances.getChildren().clear();

        try {
            List<Utilisateur> utilisateurs = serviceUtilisateur.getAll();

            // Si aucun utilisateur, afficher démo
            if (utilisateurs.isEmpty()) {
                System.out.println("⚠️ Aucun utilisateur dans la BDD, affichage de démo");
                containerLeaveBalances.getChildren().add(
                        creerCarteEmploye("Sarah Johnson", "Manager", "RH", "SJ", 12, 8)
                );
                containerLeaveBalances.getChildren().add(
                        creerCarteEmploye("Mike Chen", "Développeur", "IT", "MC", 15, 5)
                );
                containerLeaveBalances.getChildren().add(
                        creerCarteEmploye("Emma Wilson", "Analyste", "Finance", "EW", 10, 10)
                );
                return;
            }

            // Afficher chaque utilisateur avec ses vrais soldes
            for (Utilisateur user : utilisateurs) {
                String nom = user.getNomComplet();
                String poste = user.getPoste() != null ? user.getPoste() : "Employé";
                String departement = user.getDepartement() != null ? user.getDepartement() : "Général";

                // Calculer les initiales
                String initiales = "";
                if (user.getPrenom() != null && !user.getPrenom().isEmpty()) {
                    initiales += user.getPrenom().substring(0, 1).toUpperCase();
                }
                if (user.getNom() != null && !user.getNom().isEmpty()) {
                    initiales += user.getNom().substring(0, 1).toUpperCase();
                }

                // Calculer les soldes
                int soldeConges = calculerSoldeConges(user.getId());
                int soldeMaladie = calculerSoldeMaladie(user.getId());

                // Créer la carte
                HBox carte = creerCarteEmploye(nom, poste, departement, initiales, soldeConges, soldeMaladie);
                containerLeaveBalances.getChildren().add(carte);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur chargerLeaveBalances : " + e.getMessage());
            e.printStackTrace();
            containerLeaveBalances.getChildren().add(
                    creerCarteEmploye("Sarah Johnson", "Manager", "RH", "SJ", 12, 8)
            );
            containerLeaveBalances.getChildren().add(
                    creerCarteEmploye("Mike Chen", "Développeur", "IT", "MC", 15, 5)
            );
        }
    }


    private HBox creerCarteEmploye(String nom, String poste, String departement,
                                   String initiales, int soldeConges, int soldeMaladie) {

        HBox carte = new HBox(14);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setStyle(
                "-fx-padding: 18px 20px; " +
                        "-fx-background-color: #f8fafc; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 8, 0, 0, 2);"
        );

        StackPane avatar = new StackPane();
        Region cercle = new Region();
        cercle.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2); " +
                        "-fx-background-radius: 24; " +
                        "-fx-min-width: 48; " +
                        "-fx-min-height: 48;"
        );
        Label lblInitiales = new Label(initiales);
        lblInitiales.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px;"
        );
        avatar.getChildren().addAll(cercle, lblInitiales);

        // ====== 2. INFO EMPLOYÉ ======
        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label lblNom = new Label(nom);
        lblNom.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-font-size: 15px; " +
                        "-fx-text-fill: #1e293b;"
        );

        Label lblPoste = new Label(poste + " • " + departement);
        lblPoste.setStyle(
                "-fx-text-fill: #64748b; " +
                        "-fx-font-size: 12px;"
        );

        infos.getChildren().addAll(lblNom, lblPoste);

        HBox badges = new HBox(14);

        // Badge Congés (vert) - LARGE
        VBox badgeConges = new VBox(5);
        badgeConges.setAlignment(Pos.CENTER);
        badgeConges.setPrefWidth(150);
        badgeConges.setStyle(
                "-fx-padding: 16px 24px; " +
                        "-fx-background-color: #dcfce7; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #86efac; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 12;"
        );

        Label lblCongesNombre = new Label(soldeConges + " jours");
        lblCongesNombre.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-font-size: 20px; " +
                        "-fx-text-fill: #059669;"
        );

        Label lblCongesLabel = new Label("Congés");
        lblCongesLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #047857; " +
                        "-fx-font-weight: 600;"
        );

        badgeConges.getChildren().addAll(lblCongesNombre, lblCongesLabel);


        VBox badgeMaladie = new VBox(5);
        badgeMaladie.setAlignment(Pos.CENTER);
        badgeMaladie.setPrefWidth(140);  // ✅ LARGEUR FIXE
        badgeMaladie.setStyle(
                "-fx-padding: 16px 24px; " +
                        "-fx-background-color: #fef3c7; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #fcd34d; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 12;"
        );

        Label lblMaladieNombre = new Label(soldeMaladie + " jours");
        lblMaladieNombre.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-font-size: 20px; " +
                        "-fx-text-fill: #d97706;"
        );

        Label lblMaladieLabel = new Label("Maladie");
        lblMaladieLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #b45309; " +
                        "-fx-font-weight: 600;"
        );

        badgeMaladie.getChildren().addAll(lblMaladieNombre, lblMaladieLabel);

        badges.getChildren().addAll(badgeConges, badgeMaladie);


        carte.getChildren().addAll(avatar, infos, badges);

        return carte;
    }

    /**
     * Calculer le solde de congés pour un utilisateur
     */
    private int calculerSoldeConges(int userId) {
        try {
            return serviceConge.getAll().stream()
                    .filter(c -> c.getUtilisateurId() == userId)
                    .filter(c -> "Approuvé".equalsIgnoreCase(c.getStatut()))
                    .mapToInt(Conge::getNbrJours)
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calculer le solde maladie pour un utilisateur
     */
    private int calculerSoldeMaladie(int userId) {
        try {
            return (int) serviceAbsence.getAll().stream()
                    .filter(a -> a.getUtilisateurId() == userId)
                    .filter(a -> "Approuvé".equalsIgnoreCase(a.getStatut()))
                    .mapToInt(Absence::getNbrJours)
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }



    private Runnable onNouvelleDemande;

    public void setOnNouvelleDemande(Runnable callback) {
        this.onNouvelleDemande = callback;
    }

    @FXML
    public void nouvelleDemandeAction() {
        if (onNouvelleDemande != null) onNouvelleDemande.run();
    }
}