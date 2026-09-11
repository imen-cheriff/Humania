package recrutement.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import recrutement.models.PosteExterne;
import recrutement.services.ServicePosteExterne;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Formulaire Poste Externe (Formposteexterne.fxml).
 * Champs : titre, description, type contrat (CDI, CDD, Stagiaire), salaire,
 * compétences requises, expérience requise, niveau d'étude, statut,
 * date publication, date clôture, nombre employé, priorité.
 */
public class FormPosteExterneController implements Initializable {

    @FXML private Label    formTitle;
    @FXML private Button   btnClose;
    @FXML private Button   btnAnnuler;
    @FXML private Button   btnEnregistrer;

    @FXML private TextField txtTitre;
    @FXML private TextArea  txtDescription;
    @FXML private ComboBox<String> cmbTypeContrat;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private TextField txtSalaire;
    @FXML private TextField txtCompetencesRequises;
    @FXML private TextField txtExperienceRequise;
    @FXML private TextField txtNiveauEtudeRequis;
    @FXML private DatePicker datePickerPublication;
    @FXML private DatePicker datePickerCloture;
    @FXML private TextField txtNombreEmploye;
    @FXML private ComboBox<String> cmbPriorite;

    private PosteExterneController parentController;
    private final ServicePosteExterne service = new ServicePosteExterne();
    private PosteExterne posteEnCours;

    public void setParentController(PosteExterneController parentController) {
        this.parentController = parentController;
    }

    public void setModeAjout() {
        this.posteEnCours = null;
        formTitle.setText("Ajouter un Poste Externe");
        txtTitre.clear();
        txtDescription.clear();
        cmbTypeContrat.setValue("CDI");
        cmbStatut.setValue("Ouvert");
        txtSalaire.clear();
        txtCompetencesRequises.clear();
        txtExperienceRequise.clear();
        txtNiveauEtudeRequis.clear();
        datePickerPublication.setValue(null);
        datePickerCloture.setValue(null);
        txtNombreEmploye.clear();
        cmbPriorite.setValue("Moyenne");
    }

    public void setModeEdition(PosteExterne p) {
        this.posteEnCours = p;
        formTitle.setText("Modifier le Poste Externe");
        txtTitre.setText(p.getTitre() != null ? p.getTitre() : "");
        txtDescription.setText(p.getDescription() != null ? p.getDescription() : "");
        cmbTypeContrat.setValue(p.getTypeContrat() != null ? p.getTypeContrat() : "CDI");
        cmbStatut.setValue(p.getStatut() != null ? p.getStatut() : "Ouvert");
        txtSalaire.setText(p.getSalaire() != null && p.getSalaire() != 0 ? String.valueOf(p.getSalaire()) : "");
        txtCompetencesRequises.setText(p.getCompetences_Requises() != null ? p.getCompetences_Requises() : "");
        txtExperienceRequise.setText(p.getExperience_Requise() != 0 ? String.valueOf(p.getExperience_Requise()) : "");
        txtNiveauEtudeRequis.setText(p.getNiveau_Etude_Requis() != null ? p.getNiveau_Etude_Requis() : "");
        if (p.getDatePublication() != null) {
            datePickerPublication.setValue(((Date) p.getDatePublication()).toLocalDate());
        } else {
            datePickerPublication.setValue(null);
        }
        if (p.getDateCloture() != null) {
            datePickerCloture.setValue(((Date) p.getDateCloture()).toLocalDate());
        } else {
            datePickerCloture.setValue(null);
        }
        txtNombreEmploye.setText(p.getNombreEmploye() != 0 ? String.valueOf(p.getNombreEmploye()) : "");
        // Convertir la priorité (int) en String
        String prioriteStr = "Moyenne";
        if (p.getPriorite() == 1) prioriteStr = "Haute";
        else if (p.getPriorite() == 2) prioriteStr = "Moyenne";
        else if (p.getPriorite() == 3) prioriteStr = "Faible";
        cmbPriorite.setValue(prioriteStr);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbTypeContrat.setItems(FXCollections.observableArrayList("CDI", "CDD", "Stagiaire"));
        cmbStatut.setItems(FXCollections.observableArrayList("Ouvert", "Fermé"));
        cmbPriorite.setItems(FXCollections.observableArrayList("Haute", "Moyenne", "Faible"));
        cmbTypeContrat.setValue("CDI");
        cmbStatut.setValue("Ouvert");
        cmbPriorite.setValue("Moyenne");
    }

    @FXML
    public void handleEnregistrer(ActionEvent event) {
        String titre = txtTitre.getText() != null ? txtTitre.getText().trim() : "";
        if (titre.isEmpty()) {
            showAlert("Champs obligatoires", "Le titre du poste est requis.");
            return;
        }
        String description = txtDescription.getText() != null ? txtDescription.getText().trim() : null;
        String typeContrat = cmbTypeContrat.getValue() != null ? cmbTypeContrat.getValue() : "CDI";
        String statut = cmbStatut.getValue() != null ? cmbStatut.getValue() : "Ouvert";

        double salaire = 0.0;
        String sSal = txtSalaire.getText();
        if (sSal != null && !sSal.trim().isEmpty()) {
            try { salaire = Double.parseDouble(sSal.trim().replace(",", ".")); } catch (NumberFormatException e) {
                showAlert("Salaire invalide", "Saisissez un nombre.");
                return;
            }
        }
        int experienceRequise = 0;
        String sExp = txtExperienceRequise.getText();
        if (sExp != null && !sExp.trim().isEmpty()) {
            try { experienceRequise = Integer.parseInt(sExp.trim()); } catch (NumberFormatException e) {
                showAlert("Expérience requise invalide", "Saisissez un entier (années).");
                return;
            }
        }
        int nombreEmploye = 1;
        String sNb = txtNombreEmploye.getText();
        if (sNb != null && !sNb.trim().isEmpty()) {
            try { nombreEmploye = Integer.parseInt(sNb.trim()); if (nombreEmploye < 1) nombreEmploye = 1; } catch (NumberFormatException ignored) {}
        }
        int priorite = 2;  // Valeur par défaut = Moyenne
        String prioriteStr = cmbPriorite.getValue();
        if ("Haute".equals(prioriteStr)) priorite = 1;
        else if ("Moyenne".equals(prioriteStr)) priorite = 2;
        else if ("Faible".equals(prioriteStr)) priorite = 3;

        String competences = txtCompetencesRequises.getText() != null ? txtCompetencesRequises.getText().trim() : null;
        String niveauEtude = txtNiveauEtudeRequis.getText() != null ? txtNiveauEtudeRequis.getText().trim() : null;

        LocalDate ldPublication = datePickerPublication.getValue();
        Date datePublication = ldPublication != null ? Date.valueOf(ldPublication) : null;
        LocalDate ldCloture = datePickerCloture.getValue();
        Date dateCloture = ldCloture != null ? Date.valueOf(ldCloture) : null;

        try {
            if (posteEnCours != null) {
                posteEnCours.setTitre(titre);
                posteEnCours.setDescription(description);
                posteEnCours.setTypeContrat(typeContrat);
                posteEnCours.setStatut(statut);
                posteEnCours.setSalaire(salaire);
                posteEnCours.setCompetences_Requises(competences);
                posteEnCours.setExperience_Requise(experienceRequise);
                posteEnCours.setNiveau_Etude_Requis(niveauEtude);
                posteEnCours.setDatePublication(datePublication);
                posteEnCours.setDateCloture(dateCloture);
                posteEnCours.setNombreEmploye(nombreEmploye);
                posteEnCours.setPriorite(priorite);
                service.update(posteEnCours);
            } else {
                PosteExterne n = new PosteExterne();
                n.setId(service.getNextId());
                n.setTitre(titre);
                n.setDescription(description);
                n.setTypeContrat(typeContrat);
                n.setStatut(statut);
                n.setSalaire(salaire);
                n.setCompetences_Requises(competences);
                n.setExperience_Requise(experienceRequise);
                n.setNiveau_Etude_Requis(niveauEtude);
                n.setDatePublication(datePublication);
                n.setDateCloture(dateCloture);
                n.setNombreEmploye(nombreEmploye);
                n.setPriorite(priorite);
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
