package recrutement.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import recrutement.models.CandidatureInterne;
import recrutement.services.ServiceCandidatureInterne;

import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller pour Formcandidatureinterne.fxml.
 * Utilise uniquement le modèle CandidatureInterne (poste actuel, nouveau poste, salaire, date, motif).
 * Aucune intégration avec la table employé (pas d'employeId).
 */
public class FormCandidatureInterneController implements Initializable {

    @FXML private Label    formTitle;
    @FXML private Button   btnClose;
    @FXML private Button   btnAnnuler;
    @FXML private Button   btnEnregistrer;

    @FXML private TextField  txtPosteActuel;
    @FXML private TextField  txtNouveauPoste;
    @FXML private TextField  txtNouveauSalaire;
    @FXML private DatePicker datePickerDemande;
    @FXML private TextArea   txtMotif;

    private CandidatureInterneController parentController;
    private final ServiceCandidatureInterne service = new ServiceCandidatureInterne();
    private CandidatureInterne candidatureEnCours;

    public void setParentController(CandidatureInterneController parentController) {
        this.parentController = parentController;
    }

    public void setModeAjout() {
        this.candidatureEnCours = null;
        formTitle.setText("Ajouter une Candidature Interne");
        txtPosteActuel.clear();
        txtNouveauPoste.clear();
        txtNouveauSalaire.clear();
        datePickerDemande.setValue(null);
        txtMotif.clear();
    }

    /** Pré-remplit les champs pour éditer une candidature (sans employeId). */
    public void setModeEdition(CandidatureInterne c) {
        this.candidatureEnCours = c;
        formTitle.setText("Modifier la Candidature Interne");
        txtPosteActuel.setText(c.getPosteActuel() != null ? c.getPosteActuel() : "");
        txtNouveauPoste.setText(c.getNouveauPoste() != null ? c.getNouveauPoste() : "");
        txtNouveauSalaire.setText(c.getNouveauSalaire() != 0 ? String.valueOf(c.getNouveauSalaire()) : "");
        if (c.getDateDemande() != null) {
            datePickerDemande.setValue(c.getDateDemande().toLocalDate());
        } else {
            datePickerDemande.setValue(null);
        }
        txtMotif.setText(c.getMotif() != null ? c.getMotif() : "");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Plus de ComboBox type/statut/étape : formulaire aligné sur le modèle sans employeId
    }

    @FXML
    public void handleEnregistrer(ActionEvent event) {
        String posteActuel = txtPosteActuel.getText() != null ? txtPosteActuel.getText().trim() : "";
        String nouveauPoste = txtNouveauPoste.getText() != null ? txtNouveauPoste.getText().trim() : "";
        if (posteActuel.isEmpty() || nouveauPoste.isEmpty()) {
            showAlert("Champs obligatoires", "Veuillez remplir le poste actuel et le nouveau poste.");
            return;
        }
        double salaire = 0.0;
        String salStr = txtNouveauSalaire.getText();
        if (salStr != null && !salStr.trim().isEmpty()) {
            try {
                salaire = Double.parseDouble(salStr.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                showAlert("Salaire invalide", "Le nouveau salaire doit être un nombre.");
                return;
            }
        }
        LocalDate ldDemande = datePickerDemande.getValue();
        Date dateDemande = ldDemande != null ? Date.valueOf(ldDemande) : null;
        String motif = txtMotif.getText() != null ? txtMotif.getText().trim() : null;

        try {
            if (candidatureEnCours != null) {
                candidatureEnCours.setPosteActuel(posteActuel);
                candidatureEnCours.setNouveauPoste(nouveauPoste);
                candidatureEnCours.setNouveauSalaire(salaire);
                candidatureEnCours.setDateDemande(dateDemande);
                candidatureEnCours.setMotif(motif);
                candidatureEnCours.setDerniereModification(new Timestamp(System.currentTimeMillis()));
                service.update(candidatureEnCours);
            } else {
                CandidatureInterne nouvelle = new CandidatureInterne();
                nouvelle.setId(service.getNextId());
                nouvelle.setPosteActuel(posteActuel);
                nouvelle.setNouveauPoste(nouveauPoste);
                nouvelle.setNouveauSalaire(salaire);
                nouvelle.setDateDemande(dateDemande);
                nouvelle.setMotif(motif);
                nouvelle.setDerniereModification(new Timestamp(System.currentTimeMillis()));
                service.add(nouvelle);
            }
            if (parentController != null) {
                parentController.hideFormAndRefresh();
            }
            closeWindow();
        } catch (Exception ex) {
            showAlert("Erreur", "Impossible d'enregistrer : " + ex.getMessage());
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) (btnClose != null ? btnClose.getScene().getWindow() : btnAnnuler.getScene().getWindow());
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
