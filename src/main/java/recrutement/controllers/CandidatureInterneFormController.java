package recrutement.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import recrutement.models.CandidatureInterne;
import recrutement.services.ServiceCandidatureInterne;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Controller du formulaire d'ajout / édition d'une candidature interne.
 * FXML : CandidatureInterneForm.fxml
 */
public class CandidatureInterneFormController {

    @FXML private Label     formTitle;
    @FXML private Button    btnClose;
    @FXML private Button    btnAnnuler;
    @FXML private Button    btnEnregistrer;

    @FXML private TextField txtPosteActuel;
    @FXML private TextField txtNouveauPoste;
    @FXML private TextField txtNouveauSalaire;
    @FXML private TextField txtDateDemande;
    @FXML private TextArea  txtMotif;

    private CandidatureInterneController parentController;
    private final ServiceCandidatureInterne service = new ServiceCandidatureInterne();
    /** En mode édition, candidature à modifier ; en mode ajout, null. */
    private CandidatureInterne candidatureEnCours;

    /** À appeler avant d'afficher la fenêtre pour rafraîchir la liste après enregistrement. */
    public void setParentController(CandidatureInterneController parentController) {
        this.parentController = parentController;
    }

    /** Passe en mode ajout (champs vides). */
    public void setModeAjout() {
        this.candidatureEnCours = null;
        formTitle.setText("Ajouter une Candidature Interne");
        txtPosteActuel.clear();
        txtNouveauPoste.clear();
        txtNouveauSalaire.clear();
        txtDateDemande.clear();
        txtMotif.clear();
    }

    /** Passe en mode édition et pré-remplit les champs. */
    public void setModeEdition(CandidatureInterne c) {
        this.candidatureEnCours = c;
        formTitle.setText("Modifier la Candidature Interne");
        txtPosteActuel.setText(c.getPosteActuel() != null ? c.getPosteActuel() : "");
        txtNouveauPoste.setText(c.getNouveauPoste() != null ? c.getNouveauPoste() : "");
        txtNouveauSalaire.setText(c.getNouveauSalaire() != 0 ? String.valueOf(c.getNouveauSalaire()) : "");
        if (c.getDateDemande() != null) {
            txtDateDemande.setText(new SimpleDateFormat("yyyy-MM-dd").format(c.getDateDemande()));
        } else {
            txtDateDemande.clear();
        }
        txtMotif.setText(c.getMotif() != null ? c.getMotif() : "");
    }

    @FXML
    public void handleEnregistrer(javafx.event.ActionEvent event) {
        String posteActuel = txtPosteActuel.getText() != null ? txtPosteActuel.getText().trim() : "";
        String nouveauPoste = txtNouveauPoste.getText() != null ? txtNouveauPoste.getText().trim() : "";
        if (posteActuel.isEmpty() || nouveauPoste.isEmpty()) {
            showAlert("Champs obligatoires", "Veuillez remplir au moins le poste actuel et le nouveau poste.");
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
        Date dateDemande = null;
        String dateStr = txtDateDemande.getText();
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                LocalDate ld = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                dateDemande = Date.valueOf(ld);
            } catch (DateTimeParseException e) {
                showAlert("Date invalide", "Utilisez le format AAAA-MM-JJ (ex: 2026-02-23).");
                return;
            }
        }
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
    public void handleClose(javafx.event.ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) (btnClose != null ? btnClose.getScene().getWindow() : btnAnnuler.getScene().getWindow());
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
