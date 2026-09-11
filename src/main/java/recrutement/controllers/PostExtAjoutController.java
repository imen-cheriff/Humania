package recrutement.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import recrutement.models.PosteExterne;
import recrutement.services.ServicePosteExterne;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class PostExtAjoutController {

    @FXML private TextField nomField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> TypeCont;
    @FXML private TextField salaireField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField compRE;
    @FXML private Spinner<Integer> expReq;
    @FXML private ComboBox<String> niveauEtudeCombo;
    @FXML private CheckBox status;
    @FXML private Spinner<Integer> nbrEmploye;

    private PosteExterneController parentController;
    private final ServicePosteExterne servicePosteInterne = new ServicePosteExterne();
    private int currentPosteId = -1;
    private boolean editMode = false;
    // Contexte: true = poste externe, false = poste interne / mission temporaire
    private boolean externeContext = false;
    private int responsableRHid = 1;
    private int priorite = 1;

    public void setParentController(PosteExterneController parentController) {
        this.parentController = parentController;
    }

    public void setExterneContext(boolean externe) {
        this.externeContext = externe;
    }

    @FXML
    public void initialize() {
        // Si le formulaire contient des dates (postINT), alors il s'agit d'un "poste temporaire"
        // et on affiche une liste de "types de poste" (mission/projet/…).
        boolean isPostInterneTemporaireForm = (dateDebutPicker != null || dateFinPicker != null);
        if (isPostInterneTemporaireForm) {
            TypeCont.getItems().addAll(
                    "Mission externe",
                    "Mission interne",
                    "Projet",
                    "Remplacement",
                    "Consulting",
                    "Autre"
            );
        } else {
            // Formulaires postes externes / autres : type contrat
            TypeCont.getItems().addAll("CDI", "CDD", "Temps partiel");
        }
        niveauEtudeCombo.getItems().addAll("Bac", "Bac+2", "Bac+3", "Bac+5", "Master", "Doctorat");
        
        // Initialiser les spinners
        SpinnerValueFactory<Integer> expFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 0);
        expReq.setValueFactory(expFactory);
        
        SpinnerValueFactory<Integer> nbrFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        nbrEmploye.setValueFactory(nbrFactory);
    }

    public void initForAdd(int nextId) {
        editMode = false;
        currentPosteId = nextId;
        nomField.clear();
        descField.clear();
        TypeCont.getSelectionModel().clearSelection();
        salaireField.clear();
        if (dateDebutPicker != null) dateDebutPicker.setValue(LocalDate.now());
        if (dateFinPicker != null) dateFinPicker.setValue(null);
        compRE.clear();
        expReq.getValueFactory().setValue(0);
        niveauEtudeCombo.getSelectionModel().clearSelection();
        // Par défaut: poste disponible
        status.setSelected(true);
        nbrEmploye.getValueFactory().setValue(1);
    }

    public void initForEdit(PosteExterne p) {
        editMode = true;
        currentPosteId = p.getId();
        responsableRHid = p.getResponsableRHid();
        priorite = p.getPriorite();
        
        nomField.setText(p.getTitre() != null ? p.getTitre() : "");
        descField.setText(p.getDescription() != null ? p.getDescription() : "");
        setCombo(TypeCont, p.getTypeContrat());
        salaireField.setText(p.getSalaire() != null ? String.valueOf(p.getSalaire()) : "");
        if (dateDebutPicker != null) dateDebutPicker.setValue(toLocalDate(p.getDatePublication()));
        if (dateFinPicker != null) dateFinPicker.setValue(toLocalDate(p.getDateCloture()));
        compRE.setText(p.getCompetences_Requises() != null ? p.getCompetences_Requises() : "");
        expReq.getValueFactory().setValue(p.getExperience_Requise());
        setCombo(niveauEtudeCombo, p.getNiveau_Etude_Requis());
        status.setSelected("Disponible".equalsIgnoreCase(p.getStatut()));
        nbrEmploye.getValueFactory().setValue(p.getNombreEmploye());
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

    @FXML
    public void enregistrerSalle() {
        String titre = nomField.getText() != null ? nomField.getText().trim() : "";
        String description = descField.getText() != null ? descField.getText().trim() : "";
        String typeContrat = TypeCont.getSelectionModel().getSelectedItem();
        Double salaire = null;
        try {
            String s = salaireField.getText();
            if (s != null && !s.trim().isEmpty()) salaire = Double.parseDouble(s.trim());
        } catch (NumberFormatException ignored) {}
        
        String competences = compRE.getText() != null ? compRE.getText().trim() : "";
        int experience = expReq.getValue() != null ? expReq.getValue() : 0;
        String niveauEtude = niveauEtudeCombo.getSelectionModel().getSelectedItem();
        String statut = status.isSelected() ? "Disponible" : "Fermé";
        int nombreEmploye = nbrEmploye.getValue() != null ? nbrEmploye.getValue() : 1;

        Date dateDebut = toDate(dateDebutPicker != null ? dateDebutPicker.getValue() : null);
        Date dateFin = toDate(dateFinPicker != null ? dateFinPicker.getValue() : null);
        if (dateDebut != null && dateFin != null && dateFin.before(dateDebut)) {
            new Alert(Alert.AlertType.ERROR, "La date de fin ne peut pas être avant la date de début.", ButtonType.OK).showAndWait();
            return;
        }

        PosteExterne poste = new PosteExterne(
                currentPosteId,
                titre,
                description,
                typeContrat,
                salaire,
                competences,
                experience,
                niveauEtude != null ? niveauEtude : "",
                statut,
                dateDebut != null ? dateDebut : new Date(),
                dateFin,
                externeContext,
                responsableRHid,
                nombreEmploye,
                priorite
        );

        try {
            if (editMode) {
                servicePosteInterne.update(poste);
            } else {
                servicePosteInterne.add(poste);
            }
            if (parentController != null) parentController.hideFormAndRefresh();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur enregistrement: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    public void afficherSalle() {
        if (parentController != null) parentController.hideFormAndRefresh();
    }

    private static LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        if (d instanceof java.sql.Date) {
            return ((java.sql.Date) d).toLocalDate();
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate d) {
        if (d == null) return null;
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
