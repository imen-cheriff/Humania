package recrutement.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import recrutement.models.CandidatureExterne;
import recrutement.services.ServiceCandidatureExterne;

import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Formulaire Candidature Externe (Formcandidatureexterne.fxml).
 * CRUD via ServiceCandidatureExterne.
 */
public class FormCandidatureExterneController implements Initializable {

    @FXML private Label    formTitle;
    @FXML private Button   btnClose;
    @FXML private Button   btnAnnuler;
    @FXML private Button   btnEnregistrer;

    @FXML private TextField        txtPrenom;
    @FXML private TextField        txtNom;
    @FXML private DatePicker       datePickerDepot;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private ComboBox<String> cmbEtapePipeline;
    @FXML private TextField        txtScoringIA;
    @FXML private TextField        txtCvUrl;
    @FXML private TextField        txtLettreUrl;

    private CandidatureExterneController parentController;
    private final ServiceCandidatureExterne service = new ServiceCandidatureExterne();
    private CandidatureExterne candidatureEnCours;

    public void setParentController(CandidatureExterneController parentController) {
        this.parentController = parentController;
    }

    public void setModeAjout() {
        this.candidatureEnCours = null;
        formTitle.setText("Ajouter une Candidature Externe");
        txtPrenom.clear();
        txtNom.clear();
        datePickerDepot.setValue(null);
        cmbStatut.setValue("En attente");
        cmbEtapePipeline.setValue(null);
        txtScoringIA.clear();
        txtCvUrl.clear();
        txtLettreUrl.clear();
    }

    public void setModeEdition(CandidatureExterne c) {
        this.candidatureEnCours = c;
        formTitle.setText("Modifier la Candidature Externe");
        txtPrenom.setText(c.getPrenom() != null ? c.getPrenom() : "");
        txtNom.setText(c.getNom() != null ? c.getNom() : "");
        if (c.getDateDepot() != null) {
            datePickerDepot.setValue(c.getDateDepot().toLocalDate());
        } else {
            datePickerDepot.setValue(null);
        }
        cmbStatut.setValue(c.getStatut() != null ? c.getStatut() : "En attente");
        cmbEtapePipeline.setValue(c.getEtapePipeline());
        txtScoringIA.setText(c.getScoringIa() != 0 ? String.valueOf(c.getScoringIa()) : "");
        txtCvUrl.setText(c.getCvUrl() != null ? c.getCvUrl() : "");
        txtLettreUrl.setText(c.getLettreMotivationUrl() != null ? c.getLettreMotivationUrl() : "");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbStatut.setItems(FXCollections.observableArrayList("En attente", "Acceptée", "Refusée"));
        cmbStatut.setValue("En attente");
        cmbEtapePipeline.setItems(FXCollections.observableArrayList(
                "Réception", "Présélection", "Test Technique", "Entretien", "Finalisé", "Offre"
        ));
    }

    @FXML
    public void handleEnregistrer(ActionEvent event) {
        String prenom = txtPrenom.getText() != null ? txtPrenom.getText().trim() : "";
        String nom = txtNom.getText() != null ? txtNom.getText().trim() : "";
        if (prenom.isEmpty() || nom.isEmpty()) {
            showAlert("Champs obligatoires", "Prénom et Nom sont requis.");
            return;
        }
        LocalDate ldDepot = datePickerDepot.getValue();
        Date dateDepot = ldDepot != null ? Date.valueOf(ldDepot) : null;
        String statut = cmbStatut.getValue() != null ? cmbStatut.getValue() : "En attente";
        String etape = cmbEtapePipeline.getValue();
        double scoring = 0.0;
        String sc = txtScoringIA.getText();
        if (sc != null && !sc.trim().isEmpty()) {
            try { scoring = Double.parseDouble(sc.trim().replace(",", ".")); } catch (NumberFormatException ignored) {}
        }
        String cvUrl = txtCvUrl.getText() != null ? txtCvUrl.getText().trim() : null;
        String lettreUrl = txtLettreUrl.getText() != null ? txtLettreUrl.getText().trim() : null;

        try {
            if (candidatureEnCours != null) {
                candidatureEnCours.setPrenom(prenom);
                candidatureEnCours.setNom(nom);
                candidatureEnCours.setDateDepot(dateDepot);
                candidatureEnCours.setStatut(statut);
                candidatureEnCours.setEtapePipeline(etape);
                candidatureEnCours.setScoringIa(scoring);
                candidatureEnCours.setCvUrl(cvUrl);
                candidatureEnCours.setLettreMotivationUrl(lettreUrl);
                candidatureEnCours.setDerniereModification(new Timestamp(System.currentTimeMillis()));
                service.update(candidatureEnCours);
            } else {
                CandidatureExterne n = new CandidatureExterne();
                n.setId(service.getNextId());
                n.setPrenom(prenom);
                n.setNom(nom);
                n.setDateDepot(dateDepot);
                n.setStatut(statut);
                n.setEtapePipeline(etape);
                n.setScoringIa(scoring);
                n.setCvUrl(cvUrl);
                n.setLettreMotivationUrl(lettreUrl);
                n.setDerniereModification(new Timestamp(System.currentTimeMillis()));
                service.add(n);
            }
            if (parentController != null) parentController.hideFormAndRefresh();
            closeWindow();
        } catch (Exception ex) {
            showAlert("Erreur", "Enregistrement impossible : " + ex.getMessage());
        }
    }

    @FXML
    public void handleClose(ActionEvent event) { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) (btnClose != null ? btnClose.getScene().getWindow() : btnAnnuler.getScene().getWindow());
        if (stage != null) stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
