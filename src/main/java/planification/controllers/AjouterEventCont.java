package planification.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import planification.models.Evenement;
import planification.services.ServiceEvenement;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ResourceBundle;

public class AjouterEventCont implements Initializable {

    @FXML
    private TextField titreField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker datePicker;

    @FXML
    private Spinner<Integer> debutHeureSpinner;

    @FXML
    private Spinner<Integer> debutMinuteSpinner;

    @FXML
    private Spinner<Integer> finHeureSpinner;

    @FXML
    private Spinner<Integer> finMinuteSpinner;

    @FXML
    private TextField lieuField;

    @FXML
    private Spinner<Integer> participantsMaxSpinner;


    private final ServiceEvenement seve = new ServiceEvenement();
    private Evenement evenementToEdit = null;
    private Runnable onSaveCallback = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        SpinnerValueFactory<Integer> debutHeureFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9);
        debutHeureSpinner.setValueFactory(debutHeureFactory);

        SpinnerValueFactory<Integer> finHeureFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10);
        finHeureSpinner.setValueFactory(finHeureFactory);


        SpinnerValueFactory<Integer> debutMinuteFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        debutMinuteSpinner.setValueFactory(debutMinuteFactory);

        SpinnerValueFactory<Integer> finMinuteFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        finMinuteSpinner.setValueFactory(finMinuteFactory);


        SpinnerValueFactory<Integer> participantsFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10);
        participantsMaxSpinner.setValueFactory(participantsFactory);

        // Format date picker
        datePicker.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });
    }



    public void setEvenementToEdit(Evenement ev) {
        this.evenementToEdit = ev;
        if (ev == null) return;
        if (titreField != null) titreField.setText(ev.getTitre() != null ? ev.getTitre() : "");
        if (descriptionArea != null) descriptionArea.setText(ev.getDescription() != null ? ev.getDescription() : "");
        if (lieuField != null) lieuField.setText(ev.getLieu() != null ? ev.getLieu() : "");
        if (participantsMaxSpinner != null) participantsMaxSpinner.getValueFactory().setValue(ev.getNbParticipantsMax());
        if (ev.getDateEvenement() != null) {
            LocalDate ld = Instant.ofEpochMilli(ev.getDateEvenement().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (datePicker != null) datePicker.setValue(ld);
        }
        if (ev.getDateHeureDebut() != null) {
            LocalTime lt = Instant.ofEpochMilli(ev.getDateHeureDebut().getTime()).atZone(ZoneId.systemDefault()).toLocalTime();
            if (debutHeureSpinner != null) debutHeureSpinner.getValueFactory().setValue(lt.getHour());
            if (debutMinuteSpinner != null) debutMinuteSpinner.getValueFactory().setValue(lt.getMinute());
        }
        if (ev.getDateHeureFin() != null) {
            LocalTime lt = Instant.ofEpochMilli(ev.getDateHeureFin().getTime()).atZone(ZoneId.systemDefault()).toLocalTime();
            if (finHeureSpinner != null) finHeureSpinner.getValueFactory().setValue(lt.getHour());
            if (finMinuteSpinner != null) finMinuteSpinner.getValueFactory().setValue(lt.getMinute());
        }
    }

    @FXML
    private void enregistrerEvenement() {
        if (!validateForm()) {
            return;
        }
        try {
            String titre = titreField.getText().trim();
            String description = descriptionArea.getText().trim();
            LocalDate date = datePicker.getValue();
            LocalTime debut = LocalTime.of(
                    debutHeureSpinner.getValue(),
                    debutMinuteSpinner.getValue()
            );
            LocalTime fin = LocalTime.of(
                    finHeureSpinner.getValue(),
                    finMinuteSpinner.getValue()
            );
            String lieu = lieuField.getText().trim();
            int participantsMax = participantsMaxSpinner.getValue();

            Date dateEvenement = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date dateHeureDebut = Date.from(date.atTime(debut).atZone(ZoneId.systemDefault()).toInstant());
            Date dateHeureFin = Date.from(date.atTime(fin).atZone(ZoneId.systemDefault()).toInstant());

            Evenement evenement = new Evenement();
            evenement.setTitre(titre);
            evenement.setDescription(description);
            evenement.setDateEvenement(dateEvenement);
            evenement.setDateHeureDebut(dateHeureDebut);
            evenement.setDateHeureFin(dateHeureFin);
            evenement.setLieu(lieu);
            evenement.setNbParticipantsMax(participantsMax);
            evenement.setParticipantsInscrits(evenementToEdit != null ? evenementToEdit.getParticipantsInscrits() : "");
            evenement.setCreePar(evenementToEdit != null ? evenementToEdit.getCreePar() : "");
            evenement.setCreeLe(evenementToEdit != null ? evenementToEdit.getCreeLe() : new Date());

            if (evenementToEdit != null) {
                evenement.setId(evenementToEdit.getId());
                seve.update(evenement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement modifié avec succès.");
                evenementToEdit = null;
            } else {
                seve.add(evenement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement enregistré avec succès.");
            }
            clearForm();
            // Close overlay after successful save
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void afficherEvenement() {
        var evenements = seve.getAll();
        StringBuilder sb = new StringBuilder();
        for (Evenement e : evenements) {
            sb.append(e.toString()).append("\n");
        }
        showAlert(Alert.AlertType.INFORMATION, "Liste des événements",
                sb.length() > 0 ? sb.toString() : "Aucun événement enregistré.");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void annuler() {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous vraiment annuler? Les données non enregistrées seront perdues.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            clearForm();
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();


        if (titreField.getText().trim().isEmpty()) {
            errors.append("- Le titre est obligatoire\n");
            titreField.getStyleClass().add("error");
        } else {
            titreField.getStyleClass().remove("error");
        }


        if (datePicker.getValue() == null) {
            errors.append("- La date est obligatoire\n");
            datePicker.getStyleClass().add("error");
        } else {
            datePicker.getStyleClass().remove("error");
        }


        LocalTime debut = LocalTime.of(
                debutHeureSpinner.getValue(),
                debutMinuteSpinner.getValue()
        );

        LocalTime fin = LocalTime.of(
                finHeureSpinner.getValue(),
                finMinuteSpinner.getValue()
        );

        if (fin.isBefore(debut) || fin.equals(debut)) {
            errors.append("- L'heure de fin doit être après l'heure de début\n");
        }


        if (lieuField.getText().trim().isEmpty()) {
            errors.append("- Le lieu est obligatoire\n");
            lieuField.getStyleClass().add("error");
        } else {
            lieuField.getStyleClass().remove("error");
        }


        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    private void clearForm() {
        titreField.clear();
        descriptionArea.clear();
        datePicker.setValue(null);
        debutHeureSpinner.getValueFactory().setValue(9);
        debutMinuteSpinner.getValueFactory().setValue(0);
        finHeureSpinner.getValueFactory().setValue(10);
        finMinuteSpinner.getValueFactory().setValue(0);
        lieuField.clear();
        participantsMaxSpinner.getValueFactory().setValue(10);


        titreField.getStyleClass().remove("error");
        datePicker.getStyleClass().remove("error");
        lieuField.getStyleClass().remove("error");
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
}
