package planification.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import planification.models.Espace;
import planification.services.ServiceEspace;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterEspaceContr implements Initializable {

    // ── Type d'espace — injectés depuis fx:define + RadioButtons ──
    @FXML private ToggleGroup  typeGroup;      // injecté via <fx:define><ToggleGroup fx:id="typeGroup"/>
    @FXML private RadioButton  radioReunion;
    @FXML private RadioButton  radioCoworking;

    // ── Champs du formulaire ──────────────────────────────
    @FXML private TextField        idField;
    @FXML private TextField        nomField;
    @FXML private Spinner<Integer> capaciteSpinner;
    @FXML private Spinner<Integer> etageSpinner;
    @FXML private TextField        urlImageField;
    @FXML private CheckBox         disponibleCheckbox;
    @FXML private CheckBox         equipement1;
    @FXML private CheckBox         equipement2;
    @FXML private CheckBox         equipement3;
    @FXML private CheckBox         equipement4;
    @FXML private CheckBox         equipement5;
    @FXML private CheckBox         equipement6;
    @FXML private CheckBox         equipement7;
    @FXML private CheckBox         equipement8;

    private final ServiceEspace serviceEspace  = new ServiceEspace();
    private Espace               espaceToEdit   = null;
    private Runnable             onSaveCallback = null;

    // ── Init ──────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        capaciteSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 10));
        etageSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0));

        // S'assurer que "Réunion" est sélectionné par défaut
        if (typeGroup != null && typeGroup.getSelectedToggle() == null) {
            if (radioReunion != null) radioReunion.setSelected(true);
        }
    }

    // ── Pré-remplissage en mode édition ───────────────────
    public void setEspaceToEdit(Espace e) {
        this.espaceToEdit = e;
        if (e == null) return;

        if (idField            != null) idField.setText(String.valueOf(e.getId()));
        if (nomField           != null) nomField.setText(e.getNom() != null ? e.getNom() : "");
        if (capaciteSpinner    != null) capaciteSpinner.getValueFactory().setValue(e.getCapacite());
        if (etageSpinner       != null) etageSpinner.getValueFactory().setValue(e.getEtage());
        if (urlImageField      != null) urlImageField.setText(e.getUrlImage() != null ? e.getUrlImage() : "");
        if (disponibleCheckbox != null) disponibleCheckbox.setSelected(e.isDisponible());

        // Restaurer le type via le ToggleGroup (méthode fiable)
        if (e.isCoworking()) {
            if (radioCoworking != null) radioCoworking.setSelected(true);
        } else {
            if (radioReunion   != null) radioReunion.setSelected(true);
        }

        // Restaurer les équipements
        if (e.getListeEquipements() != null) {
            List<String> eq = e.getListeEquipements();
            if (equipement1 != null) equipement1.setSelected(eq.contains("Projecteur"));
            if (equipement2 != null) equipement2.setSelected(eq.contains("Tableau blanc"));
            if (equipement3 != null) equipement3.setSelected(eq.contains("Ordinateur"));
            if (equipement4 != null) equipement4.setSelected(eq.contains("Climatisation"));
            if (equipement5 != null) equipement5.setSelected(eq.contains("Wifi"));
            if (equipement6 != null) equipement6.setSelected(eq.contains("Imprimante"));
            if (equipement7 != null) equipement7.setSelected(eq.contains("Téléphone"));
            if (equipement8 != null) equipement8.setSelected(eq.contains("Visioconférence"));
        }
    }

    // ── Lire le type sélectionné ──────────────────────────
    /**
     * Lit le type depuis le ToggleGroup (source de vérité unique).
     * Comparaison par référence objet — indépendant du texte affiché.
     */
    private String getSelectedType() {
        if (typeGroup != null) {
            Toggle selected = typeGroup.getSelectedToggle();
            // Comparaison par référence : si le bouton sélectionné EST radioCoworking
            if (selected != null && selected == radioCoworking) return "coworking";
            return "reunion";
        }
        // Fallback si typeGroup non injecté
        if (radioCoworking != null && radioCoworking.isSelected()) return "coworking";
        return "reunion";
    }

    // ── Parcourir image ───────────────────────────────────
    @FXML
    private void parcourirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(urlImageField.getScene().getWindow());
        if (file != null) urlImageField.setText(file.toURI().toString());
    }

    // ── Enregistrer / Modifier ────────────────────────────
    @FXML
    private void enregistrerSalle() {
        try {
            String  nom        = nomField.getText().trim();
            int     capacite   = capaciteSpinner.getValue();
            int     etage      = etageSpinner.getValue();
            String  urlImage   = urlImageField.getText().trim();
            boolean disponible = disponibleCheckbox.isSelected();
            String  typeEspace = getSelectedType();   // ← lecture correcte via ToggleGroup

            List<String> equipements = new ArrayList<>();
            if (equipement1.isSelected()) equipements.add("Projecteur");
            if (equipement2.isSelected()) equipements.add("Tableau blanc");
            if (equipement3.isSelected()) equipements.add("Ordinateur");
            if (equipement4.isSelected()) equipements.add("Climatisation");
            if (equipement5.isSelected()) equipements.add("Wifi");
            if (equipement6.isSelected()) equipements.add("Imprimante");
            if (equipement7.isSelected()) equipements.add("Téléphone");
            if (equipement8.isSelected()) equipements.add("Visioconférence");

            if (espaceToEdit != null) {
                Espace espace = new Espace(espaceToEdit.getId(), nom, capacite, etage,
                        equipements, urlImage, disponible, typeEspace);
                serviceEspace.update(espace);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Espace modifié avec succès.");
                espaceToEdit = null;
            } else {
                Espace espace = new Espace(nom, capacite, etage, equipements,
                        urlImage, disponible, typeEspace);
                serviceEspace.add(espace);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Salle enregistrée avec succès.");
            }

            if (onSaveCallback != null) onSaveCallback.run();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void afficherSalle() {
        List<Espace> espaces = serviceEspace.getAll();
        StringBuilder sb = new StringBuilder();
        for (Espace e : espaces) sb.append(e.toString()).append("\n");
        showAlert(Alert.AlertType.INFORMATION, "Liste des salles",
                sb.length() > 0 ? sb.toString() : "Aucune salle enregistrée.");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
}