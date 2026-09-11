package recrutement.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import recrutement.models.PosteInterne;
import recrutement.services.ServicePosteInterne;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Formulaire Poste Interne (Formpostinterne.fxml).
 * - Type de poste : liste déroulante (Mission interne, Mission externe, Renfort)
 * - Date début / Date fin : DatePicker (calendrier)
 * - Rémunération : champ numérique
 */
public class FormPosteInterneController implements Initializable {

    @FXML private Label    formTitle;
    @FXML private Button   btnClose;
    @FXML private Button   btnAnnuler;
    @FXML private Button   btnEnregistrer;

    @FXML private ComboBox<String> cmbTypePoste;
    @FXML private TextField        txtRemuneration;
    @FXML private DatePicker       datePickerDebut;
    @FXML private DatePicker       datePickerFin;

    private PostInterneController parentController;
    private final ServicePosteInterne service = new ServicePosteInterne();
    private PosteInterne posteEnCours;

    public void setParentController(PostInterneController parentController) {
        this.parentController = parentController;
    }

    public void setModeAjout() {
        this.posteEnCours = null;
        formTitle.setText("Ajouter un Poste Interne");
        cmbTypePoste.setValue("Mission interne");
        txtRemuneration.clear();
        datePickerDebut.setValue(null);
        datePickerFin.setValue(null);
    }

    public void setModeEdition(PosteInterne p) {
        this.posteEnCours = p;
        formTitle.setText("Modifier le Poste Interne");
        String type = p.getTypePoste() != null ? p.getTypePoste() : "";
        if (type.isEmpty() || (!type.equals("Mission interne") && !type.equals("Mission externe") && !type.equals("Renfort"))) {
            cmbTypePoste.setValue("Mission interne");
        } else {
            cmbTypePoste.setValue(type);
        }
        txtRemuneration.setText(p.getRemuneration() != 0 ? String.valueOf(p.getRemuneration()) : "");
        if (p.getDateDebut() != null) {
            datePickerDebut.setValue(p.getDateDebut().toLocalDate());
        } else {
            datePickerDebut.setValue(null);
        }
        if (p.getDateFin() != null) {
            datePickerFin.setValue(p.getDateFin().toLocalDate());
        } else {
            datePickerFin.setValue(null);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbTypePoste.setItems(FXCollections.observableArrayList(
                "Mission interne",
                "Mission externe",
                "Renfort"
        ));
        cmbTypePoste.setValue("Mission interne");
    }

    @FXML
    public void handleEnregistrer(ActionEvent event) {
        String typePoste = cmbTypePoste.getValue() != null ? cmbTypePoste.getValue().trim() : "";
        if (typePoste.isEmpty()) {
            showAlert("Champs obligatoires", "Veuillez sélectionner un type de poste.");
            return;
        }
        double rem = 0.0;
        String remStr = txtRemuneration.getText();
        if (remStr != null && !remStr.trim().isEmpty()) {
            try {
                rem = Double.parseDouble(remStr.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                showAlert("Rémunération invalide", "Saisissez un nombre.");
                return;
            }
        }
        LocalDate ldDebut = datePickerDebut.getValue();
        LocalDate ldFin = datePickerFin.getValue();
        Date dateDebut = ldDebut != null ? Date.valueOf(ldDebut) : null;
        Date dateFin = ldFin != null ? Date.valueOf(ldFin) : null;

        try {
            if (posteEnCours != null) {
                posteEnCours.setTypePoste(typePoste);
                posteEnCours.setRemuneration(rem);
                posteEnCours.setDateDebut(dateDebut);
                posteEnCours.setDateFin(dateFin);
                service.update(posteEnCours);
            } else {
                int id = service.getNextId();
                PosteInterne n = new PosteInterne(id, typePoste, rem, dateDebut, dateFin);
                n.setId(id);
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
