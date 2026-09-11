package attendance.gui;

import attendance.interfaces.service;
import attendance.models.*;
import attendance.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Controller groupé : gère Types de Congé ET Types d'Absence dans le même FXML.
 */
public class ConfigController {


    @FXML private VBox viewTypeConge;
    @FXML private VBox viewTypeAbsence;


    @FXML private TableView<TypeConge>              tableTC;
    @FXML private TableColumn<TypeConge, Integer>   colIdTC;
    @FXML private TableColumn<TypeConge, String>    colLibelleTC;
    @FXML private TableColumn<TypeConge, String>    colDescriptionTC;
    @FXML private TextField txtLibelleTC;
    @FXML private TextField txtDescriptionTC;
    private final ObservableList<TypeConge> listTC = FXCollections.observableArrayList();


    @FXML private TableView<TypeAbsence>            tableTA;
    @FXML private TableColumn<TypeAbsence, Integer> colIdTA;
    @FXML private TableColumn<TypeAbsence, String>  colLibelleTA;
    @FXML private TableColumn<TypeAbsence, String>  colDescriptionTA;
    @FXML private TextField txtLibelleTA;
    @FXML private TextField txtDescriptionTA;
    private final ObservableList<TypeAbsence> listTA = FXCollections.observableArrayList();


    private final service<TypeConge>  serviceTC = new ServiceTypeConge();
    private final service<TypeAbsence> serviceTA = new ServiceTypeAbsence();


    @FXML
    public void initialize() {
        initTableTC();
        initTableTA();
        afficherTypeConge(); // vue par défaut
    }

    private void initTableTC() {
        if (tableTC == null) return;
        colIdTC.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLibelleTC.setCellValueFactory(new PropertyValueFactory<>("libelle"));
        colDescriptionTC.setCellValueFactory(new PropertyValueFactory<>("description"));
        tableTC.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                if (txtLibelleTC != null)    txtLibelleTC.setText(sel.getLibelle());
                if (txtDescriptionTC != null) txtDescriptionTC.setText(sel.getDescription());
            }
        });
    }

    private void initTableTA() {
        if (tableTA == null) return;
        colIdTA.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLibelleTA.setCellValueFactory(new PropertyValueFactory<>("libelle"));
        colDescriptionTA.setCellValueFactory(new PropertyValueFactory<>("description"));
        tableTA.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                if (txtLibelleTA != null)    txtLibelleTA.setText(sel.getLibelle());
                if (txtDescriptionTA != null) txtDescriptionTA.setText(sel.getDescription());
            }
        });
    }



    public void afficherTypeConge() {
        if (viewTypeConge != null)   { viewTypeConge.setVisible(true);   viewTypeConge.setManaged(true); }
        if (viewTypeAbsence != null) { viewTypeAbsence.setVisible(false); viewTypeAbsence.setManaged(false); }
        chargerTC();
    }

    public void afficherTypeAbsence() {
        if (viewTypeAbsence != null) { viewTypeAbsence.setVisible(true);  viewTypeAbsence.setManaged(true); }
        if (viewTypeConge != null)   { viewTypeConge.setVisible(false);   viewTypeConge.setManaged(false); }
        chargerTA();
    }

    // TYPE CONGE - CRUD


    private void chargerTC() {
        if (tableTC == null) return;
        listTC.clear();
        try { listTC.addAll(serviceTC.getAll()); }
        catch (Exception e) { System.err.println("Erreur chargement TypeConge : " + e.getMessage()); }
        tableTC.setItems(listTC);
    }

    @FXML public void ajouterTC() {
        String lib = txtLibelleTC != null ? txtLibelleTC.getText().trim() : "";
        if (lib.isEmpty()) { alert("Erreur", "Le libellé est obligatoire !", Alert.AlertType.ERROR); return; }
        try {
            String desc = txtDescriptionTC != null ? txtDescriptionTC.getText().trim() : "";
            serviceTC.add(new TypeConge(0, lib, desc));
            chargerTC(); viderTC();
            alert("Succès", "Type de congé ajouté !", Alert.AlertType.INFORMATION);
        } catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
    }

    @FXML public void modifierTC() {
        TypeConge sel = tableTC != null ? tableTC.getSelectionModel().getSelectedItem() : null;
        if (sel == null) { alert("Attention", "Sélectionnez un type !", Alert.AlertType.WARNING); return; }
        try {
            sel.setLibelle(txtLibelleTC != null ? txtLibelleTC.getText().trim() : sel.getLibelle());
            sel.setDescription(txtDescriptionTC != null ? txtDescriptionTC.getText().trim() : "");
            serviceTC.update(sel);
            chargerTC(); viderTC();
            alert("Succès", "Type de congé modifié !", Alert.AlertType.INFORMATION);
        } catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
    }

    @FXML public void supprimerTC() {
        TypeConge sel = tableTC != null ? tableTC.getSelectionModel().getSelectedItem() : null;
        if (sel == null) { alert("Attention", "Sélectionnez un type !", Alert.AlertType.WARNING); return; }
        if (confirmer("Supprimer \"" + sel.getLibelle() + "\" ?")) {
            try { serviceTC.delete(sel); chargerTC(); viderTC(); alert("Succès", "Supprimé !", Alert.AlertType.INFORMATION); }
            catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
        }
    }

    @FXML public void actualiserTC() { chargerTC(); viderTC(); }

    private void viderTC() {
        if (txtLibelleTC != null)    txtLibelleTC.clear();
        if (txtDescriptionTC != null) txtDescriptionTC.clear();
        if (tableTC != null)         tableTC.getSelectionModel().clearSelection();
    }


    // TYPE ABSENCE - CRUD


    private void chargerTA() {
        if (tableTA == null) return;
        listTA.clear();
        try { listTA.addAll(serviceTA.getAll()); }
        catch (Exception e) { System.err.println("Erreur chargement TypeAbsence : " + e.getMessage()); }
        tableTA.setItems(listTA);
    }

    @FXML public void ajouterTA() {
        String lib = txtLibelleTA != null ? txtLibelleTA.getText().trim() : "";
        if (lib.isEmpty()) { alert("Erreur", "Le libellé est obligatoire !", Alert.AlertType.ERROR); return; }
        try {
            String desc = txtDescriptionTA != null ? txtDescriptionTA.getText().trim() : "";
            serviceTA.add(new TypeAbsence(0, lib, desc));
            chargerTA(); viderTA();
            alert("Succès", "Type d'absence ajouté !", Alert.AlertType.INFORMATION);
        } catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
    }

    @FXML public void modifierTA() {
        TypeAbsence sel = tableTA != null ? tableTA.getSelectionModel().getSelectedItem() : null;
        if (sel == null) { alert("Attention", "Sélectionnez un type !", Alert.AlertType.WARNING); return; }
        try {
            sel.setLibelle(txtLibelleTA != null ? txtLibelleTA.getText().trim() : sel.getLibelle());
            sel.setDescription(txtDescriptionTA != null ? txtDescriptionTA.getText().trim() : "");
            serviceTA.update(sel);
            chargerTA(); viderTA();
            alert("Succès", "Type d'absence modifié !", Alert.AlertType.INFORMATION);
        } catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
    }

    @FXML public void supprimerTA() {
        TypeAbsence sel = tableTA != null ? tableTA.getSelectionModel().getSelectedItem() : null;
        if (sel == null) { alert("Attention", "Sélectionnez un type !", Alert.AlertType.WARNING); return; }
        if (confirmer("Supprimer \"" + sel.getLibelle() + "\" ?")) {
            try { serviceTA.delete(sel); chargerTA(); viderTA(); alert("Succès", "Supprimé !", Alert.AlertType.INFORMATION); }
            catch (Exception e) { alert("Erreur", e.getMessage(), Alert.AlertType.ERROR); }
        }
    }

    @FXML public void actualiserTA() { chargerTA(); viderTA(); }

    private void viderTA() {
        if (txtLibelleTA != null)    txtLibelleTA.clear();
        if (txtDescriptionTA != null) txtDescriptionTA.clear();
        if (tableTA != null)         tableTA.getSelectionModel().clearSelection();
    }


    // UTILITAIRES

    private void alert(String titre, String msg, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private boolean confirmer(String msg) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        c.setHeaderText(null);
        return c.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
