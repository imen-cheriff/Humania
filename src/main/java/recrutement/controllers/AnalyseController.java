package recrutement.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import recrutement.services.ServiceCandidatureExterne;
import recrutement.services.ServiceCandidatureInterne;
import utils.EventBus;

public class AnalyseController {

    @FXML
    private BarChart<?, ?> barChartDepartement;

    @FXML
    private Label kpiExternes;

    @FXML
    private Label kpiInternes;

    @FXML
    private Label kpiPostes;

    @FXML
    private Label kpiScore;

    @FXML
    private Label kpiTaux;

    @FXML
    private LineChart<?, ?> lineChartEvolution;

    @FXML
    private PieChart pieChartStatut;

    @FXML
    public void initialize() {
        // KPIs (will be populated from DB)
        updateCounts();

        // Register to listen for candidature changes (add/update/delete)
        EventBus.addCandidatureListener(() -> Platform.runLater(this::updateCounts));

        // Bar chart sample data
        XYChart.Series<String, Number> deptSeries = new XYChart.Series<>();
        deptSeries.setName("Candidatures");
        deptSeries.getData().add(new XYChart.Data<>("IT", 5));
        deptSeries.getData().add(new XYChart.Data<>("Ventes", 3));
        deptSeries.getData().add(new XYChart.Data<>("RH", 2));
        ((BarChart) barChartDepartement).getData().clear();
        ((BarChart) barChartDepartement).getData().add(deptSeries);

        // Pie chart sample data
        pieChartStatut.getData().clear();
        pieChartStatut.getData().add(new PieChart.Data("Accepté", 3));
        pieChartStatut.getData().add(new PieChart.Data("En cours", 5));
        pieChartStatut.getData().add(new PieChart.Data("Refusé", 2));

        // Line chart sample data (evolution)
        XYChart.Series<String, Number> evolution = new XYChart.Series<>();
        evolution.setName("Candidatures");
        evolution.getData().add(new XYChart.Data<>("Jan", 2));
        evolution.getData().add(new XYChart.Data<>("Fév", 4));
        evolution.getData().add(new XYChart.Data<>("Mar", 6));
        ((LineChart) lineChartEvolution).getData().clear();
        ((LineChart) lineChartEvolution).getData().add(evolution);
    }

    private void updateCounts() {
        try {
            ServiceCandidatureInterne sIntern = new ServiceCandidatureInterne();
            ServiceCandidatureExterne sExtern = new ServiceCandidatureExterne();
            int internCount = sIntern.getAll() != null ? sIntern.getAll().size() : 0;
            int externCount = sExtern.getAll() != null ? sExtern.getAll().size() : 0;
            kpiInternes.setText(String.valueOf(internCount));
            kpiExternes.setText(String.valueOf(externCount));
        } catch (Exception ex) {
            // keep placeholders on error
            System.err.println("Erreur mise à jour KPI Analyse: " + ex.getMessage());
        }
    }

}
