package recrutement.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import recrutement.models.CandidatureExterne;
import recrutement.services.ServiceCandidatureExterne;

import java.io.File;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

public class CandidatureExterneFormController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateDepotPicker;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<String> etapePipelineCombo;
    @FXML private TextField cvUrlField;
    @FXML private TextField lettreMotivationField;

    private CandidatureExterneController parentController;
    private final ServiceCandidatureExterne serviceCand = new ServiceCandidatureExterne();
    private int currentCandidatureId = -1;
    private boolean editMode = false;

    public void setParentController(CandidatureExterneController parentController) {
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("Nouvelle", "En cours", "Entretien", "Acceptée", "Refusée");
        etapePipelineCombo.getItems().addAll("Candidature reçue", "Pré-sélection", "Entretien", "Offre", "Embauche");
    }

    public void initForAdd(int nextId) {
        editMode = false;
        currentCandidatureId = nextId;
        nomField.clear();
        prenomField.clear();
        dateDepotPicker.setValue(LocalDate.now());
        statutCombo.getSelectionModel().clearSelection();
        etapePipelineCombo.getSelectionModel().clearSelection();
        cvUrlField.clear();
        lettreMotivationField.clear();
    }

    public void initForEdit(CandidatureExterne c) {
        editMode = true;
        currentCandidatureId = c.getId();
        nomField.setText(c.getNom() != null ? c.getNom() : "");
        prenomField.setText(c.getPrenom() != null ? c.getPrenom() : "");
        dateDepotPicker.setValue(toLocalDate(c.getDateDepot()));
        setCombo(statutCombo, c.getStatut());
        setCombo(etapePipelineCombo, c.getEtapePipeline());
        cvUrlField.setText(c.getCvUrl() != null ? c.getCvUrl() : "");
        lettreMotivationField.setText(c.getLettreMotivationUrl() != null ? c.getLettreMotivationUrl() : "");
    }

    private static void setCombo(ComboBox<String> combo, String value) {
        if (value != null && combo.getItems().contains(value)) {
            combo.getSelectionModel().select(value);
        } else {
            combo.getSelectionModel().clearSelection();
            if (value != null && !value.isEmpty()) {
                combo.getItems().add(value);
                combo.getSelectionModel().select(value);
            }
        }
    }

    private static LocalDate toLocalDate(Date d) {
        if (d == null) return LocalDate.now();
        return d.toLocalDate();
    }

    private static Date toSqlDate(LocalDate ld) {
        if (ld == null) return null;
        return Date.valueOf(ld);
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        boolean hasErrors = false;

        clearFieldErrors();

        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire.\n");
            highlightError(nomField);
            hasErrors = true;
        }

        if (prenomField.getText() == null || prenomField.getText().trim().isEmpty()) {
            errors.append("- Le prénom est obligatoire.\n");
            highlightError(prenomField);
            hasErrors = true;
        }

        if (dateDepotPicker.getValue() == null) {
            errors.append("- La date de dépôt est obligatoire.\n");
            highlightError(dateDepotPicker);
            hasErrors = true;
        }

        if (statutCombo.getValue() == null) {
            errors.append("- Le statut est obligatoire.\n");
            highlightError(statutCombo);
            hasErrors = true;
        }

        if (etapePipelineCombo.getValue() == null) {
            errors.append("- L'étape du pipeline est obligatoire.\n");
            highlightError(etapePipelineCombo);
            hasErrors = true;
        }

        if (cvUrlField.getText() == null || cvUrlField.getText().trim().isEmpty()) {
            errors.append("- Le CV est obligatoire.\n");
            highlightError(cvUrlField);
            hasErrors = true;
        }

        if (lettreMotivationField.getText() == null || lettreMotivationField.getText().trim().isEmpty()) {
            errors.append("- La lettre de motivation est obligatoire.\n");
            highlightError(lettreMotivationField);
            hasErrors = true;
        }

        if (hasErrors) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    private void highlightError(Control control) {
        if (control instanceof TextField) {
            control.getStyleClass().add("error");
        } else if (control instanceof ComboBox) {
            control.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else if (control instanceof DatePicker) {
            control.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        }
    }

    private void clearFieldErrors() {
        statutCombo.setStyle(null);
        etapePipelineCombo.setStyle(null);
        dateDepotPicker.setStyle(null);
        nomField.getStyleClass().remove("error");
        prenomField.getStyleClass().remove("error");
        cvUrlField.getStyleClass().remove("error");
        lettreMotivationField.getStyleClass().remove("error");
    }

    @FXML
    public void enregistrerCandidature() {
        if (!validateForm()) {
            return;
        }

        int posteExterneId = 1; // Valeur par défaut (sans sélection de poste)

        String nom = nomField.getText() != null ? nomField.getText().trim() : "";
        String prenom = prenomField.getText() != null ? prenomField.getText().trim() : "";
        Date dateDepot = toSqlDate(dateDepotPicker.getValue());
        String statut = statutCombo.getSelectionModel().getSelectedItem();
        String etape = etapePipelineCombo.getSelectionModel().getSelectedItem();
        String cv = cvUrlField.getText() != null ? cvUrlField.getText().trim() : "";
        String lettre = lettreMotivationField.getText() != null ? lettreMotivationField.getText().trim() : "";
        Timestamp derniereMod = new Timestamp(System.currentTimeMillis());

        CandidatureExterne c = new CandidatureExterne(
                currentCandidatureId,
                posteExterneId,
                nom,
                prenom,
                dateDepot,
                statut,
                etape,
                0.0, // scoring IA par défaut
                cv,
                lettre,
                derniereMod
        );

        try {
            if (editMode) {
                serviceCand.update(c);
            } else {
                serviceCand.add(c);
            }
            if (parentController != null) parentController.hideFormAndRefresh();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur enregistrement: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    public void afficherCandidatures() {
        if (parentController != null) parentController.hideFormAndRefresh();
    }

    @FXML
    public void annuler() {
        if (parentController != null) parentController.hideFormAndRefresh();
    }

    @FXML
    public void parcourirCV() {
        File f = chooseFile("Sélectionner le CV");
        if (f != null) cvUrlField.setText(f.getAbsolutePath());
    }

    @FXML
    public void parcourirLettre() {
        File f = chooseFile("Sélectionner la lettre de motivation");
        if (f != null) lettreMotivationField.setText(f.getAbsolutePath());
    }

    private File chooseFile(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        return fc.showOpenDialog(cvUrlField.getScene().getWindow());
    }
}
