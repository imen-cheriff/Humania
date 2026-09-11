package recrutement.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import recrutement.models.PosteInterne;
import recrutement.services.ServicePosteInterne;

import java.sql.Date;
import java.time.LocalDate;

public class PostIntAjoutController {

    @FXML private ComboBox<String> typePosteCombo;
    @FXML private TextField remunerationField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;

    private PostInterneController parentController;
    private final ServicePosteInterne servicePosteInterne = new ServicePosteInterne();
    private int currentPosteId = -1;
    private boolean editMode = false;

    public void setParentController(PostInterneController parentController) {
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        typePosteCombo.getItems().addAll(
                "Mission externe",
                "Mission interne",
                "Projet",
                "Remplacement",
                "Consulting",
                "Autre"
        );
    }

    public void initForAdd(int nextId) {
        editMode = false;
        currentPosteId = nextId;
        typePosteCombo.getSelectionModel().clearSelection();
        remunerationField.clear();
        dateDebutPicker.setValue(LocalDate.now());
        dateFinPicker.setValue(null);
    }

    public void initForEdit(PosteInterne p) {
        editMode = true;
        currentPosteId = p.getId();
        setCombo(typePosteCombo, p.getTypePoste());
        remunerationField.setText(String.valueOf(p.getRemuneration()));
        dateDebutPicker.setValue(toLocalDate(p.getDateDebut()));
        dateFinPicker.setValue(toLocalDate(p.getDateFin()));
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
        if (d == null) return null;
        return d.toLocalDate();
    }

    private static Date toSqlDate(LocalDate ld) {
        if (ld == null) return null;
        return Date.valueOf(ld);
    }

    @FXML
    public void enregistrerPoste() {
        String typePoste = typePosteCombo.getSelectionModel().getSelectedItem();
        if (typePoste == null || typePoste.trim().isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Le type de poste est obligatoire.", ButtonType.OK).showAndWait();
            return;
        }

        double remuneration = 0;
        try {
            String s = remunerationField.getText();
            if (s != null && !s.trim().isEmpty()) {
                remuneration = Double.parseDouble(s.trim());
                if (remuneration < 0) {
                    new Alert(Alert.AlertType.ERROR, "La rémunération doit être positive.", ButtonType.OK).showAndWait();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Rémunération invalide.", ButtonType.OK).showAndWait();
            return;
        }

        Date dateDebut = toSqlDate(dateDebutPicker.getValue());
        Date dateFin = toSqlDate(dateFinPicker.getValue());
        if (dateDebut != null && dateFin != null && dateFin.before(dateDebut)) {
            new Alert(Alert.AlertType.ERROR, "La date de fin ne peut pas être avant la date de début.", ButtonType.OK).showAndWait();
            return;
        }

        PosteInterne poste = new PosteInterne(currentPosteId, typePoste, remuneration, dateDebut, dateFin);

        try {
            if (editMode) {
                servicePosteInterne.update(poste);
            } else {
                servicePosteInterne.add(poste);
            }
            if (parentController != null) parentController.hideFormAndRefresh();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    public void afficherPostes() {
        if (parentController != null) parentController.hideFormAndRefresh();
    }
}
