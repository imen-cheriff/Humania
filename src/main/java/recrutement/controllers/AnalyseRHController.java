package recrutement.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import recrutement.services.ServiceCandidatureExterne;
import recrutement.services.ServiceCandidatureInterne;
import utils.EventBus;

public class AnalyseRHController {

    @FXML
    private BarChart<?, ?> barChart;

    @FXML
    private LineChart<?, ?> lineChart;

    @FXML
    private PieChart pieChart;

    @FXML
    private Label kpiInternes;

    @FXML
    private Label kpiExternes;

    @FXML
    private Label kpiTaux;

    @FXML
    private Label kpiScore;

    @FXML
    private Label kpiPostes;

    @FXML
    public void initialize() {
        // Charger les indicateurs à partir de la base
        updateCounts();

        // Se mettre à jour lorsque les candidatures changent (ajout / édition / suppression)
        EventBus.addCandidatureListener(() -> Platform.runLater(this::updateCounts));

        // Données exemples pour les graphiques (peuvent être reliées à la BDD plus tard)
        initCharts();
    }

    private void updateCounts() {
        try {
            ServiceCandidatureInterne sIntern = new ServiceCandidatureInterne();
            ServiceCandidatureExterne sExtern = new ServiceCandidatureExterne();

            int internCount = sIntern.getAll() != null ? sIntern.getAll().size() : 0;
            int externCount = sExtern.getAll() != null ? sExtern.getAll().size() : 0;

            kpiInternes.setText(String.valueOf(internCount));
            kpiExternes.setText(String.valueOf(externCount));

            // Valeurs placeholder pour les autres KPI, à affiner selon votre modèle
            kpiTaux.setText("30%");
            kpiScore.setText("73");
            kpiPostes.setText("8");
        } catch (Exception ex) {
            System.err.println("Erreur mise à jour KPI AnalyseRH: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initCharts() {
        // Bar chart par type de contrat
        XYChart.Series<String, Number> contratSeries = new XYChart.Series<>();
        contratSeries.setName("Candidatures");
        contratSeries.getData().add(new XYChart.Data<>("CDI", 4));
        contratSeries.getData().add(new XYChart.Data<>("CDD", 3));
        contratSeries.getData().add(new XYChart.Data<>("Stagiaire", 2));
        contratSeries.getData().add(new XYChart.Data<>("Mission", 1));

        BarChart<String, Number> bar = (BarChart<String, Number>) barChart;
        // Forcer l'ordre des catégories sous les barres : CDI, CDD, Stagiaire, Mission
        if (bar.getXAxis() instanceof CategoryAxis) {
            CategoryAxis xAxis = (CategoryAxis) bar.getXAxis();
            xAxis.setCategories(FXCollections.observableArrayList("CDI", "CDD", "Stagiaire", "Mission"));
        }
        bar.getData().clear();
        bar.getData().add(contratSeries);
        bar.lookupAll(".chart-bar").forEach(node -> node.setStyle("-fx-bar-fill: #2563eb;")); // bleu
        bar.lookupAll(".axis-label").forEach(node -> node.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;"));
        bar.lookupAll(".axis-tick-label").forEach(node -> node.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;"));

        // Pie chart de démonstration
        pieChart.getData().clear();
        PieChart.Data d1 = new PieChart.Data("Accepté", 3);
        PieChart.Data d2 = new PieChart.Data("En cours", 5);
        PieChart.Data d3 = new PieChart.Data("Refusé", 2);
        pieChart.getData().addAll(d1, d2, d3);
        // Couleurs personnalisées (bleu, vert, rouge)
        Platform.runLater(() -> {
            for (PieChart.Data data : pieChart.getData()) {
                String color = "#2563eb";
                if ("Accepté".equals(data.getName())) color = "#059669"; // vert
                else if ("Refusé".equals(data.getName())) color = "#ef4444"; // rouge
                else if ("En cours".equals(data.getName())) color = "#2563eb"; // bleu
                data.getNode().setStyle("-fx-pie-color: " + color + ";");
            }
        });
        pieChart.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Line chart de démonstration
        XYChart.Series<String, Number> evolution = new XYChart.Series<>();
        evolution.setName("Candidatures");
        evolution.getData().add(new XYChart.Data<>("Jan", 2));
        evolution.getData().add(new XYChart.Data<>("Fév", 4));
        evolution.getData().add(new XYChart.Data<>("Mar", 6));

        LineChart<String, Number> line = (LineChart<String, Number>) lineChart;
        line.getData().clear();
        line.getData().add(evolution);
        line.lookupAll(".chart-series-line").forEach(node -> node.setStyle("-fx-stroke: #2563eb; -fx-stroke-width: 2px;"));
        line.lookupAll(".axis-label").forEach(node -> node.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;"));
        line.lookupAll(".axis-tick-label").forEach(node -> node.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;"));

        // S'assure que les labels des axes sont bien visibles
        if (line.getXAxis() != null) {
            line.getXAxis().setLabel("Mois");
        }
        if (line.getYAxis() != null) {
            line.getYAxis().setLabel("Nombre de candidatures");
        }
    }
}
