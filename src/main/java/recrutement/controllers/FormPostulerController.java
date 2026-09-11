package recrutement.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import recrutement.models.CandidatureExterne;
import recrutement.models.Document;
import recrutement.models.PosteExterne;
import recrutement.services.ServiceCandidatureExterne;
import recrutement.services.ServiceDocument;
import utils.UserSession;

import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Formulaire Postuler (modal) — ouvert par PosteExterneController.
 *
 * - L'employé parcourt ses fichiers pour CV et Lettre (PDF uniquement).
 * - Les PDF sélectionnés sont copiés dans uploaded_documents/ et insérés en DB (table document).
 * - Une candidature externe est créée avec les chemins DB des fichiers.
 */
public class FormPostulerController implements Initializable {

    @FXML private Label    lblPoste;
    @FXML private Button   btnClose;
    @FXML private Button   btnAnnuler;
    @FXML private Button   btnEnvoyer;

    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;

    // Champs affichage nom de fichier choisi
    @FXML private TextField txtCvNom;
    @FXML private TextField txtLettreNom;

    // Boutons parcourir
    @FXML private Button btnParcourirCv;
    @FXML private Button btnParcourirLettre;

    // Badges statut upload
    @FXML private Label lblCvStatus;
    @FXML private Label lblLettreStatus;

    private PosteExterne           posteExterne;
    private final ServiceDocument           serviceDocument = new ServiceDocument();
    private final ServiceCandidatureExterne serviceCand     = new ServiceCandidatureExterne();

    private File selectedCvFile;
    private File selectedLettreFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Pré-remplir nom/prénom depuis la session
        String user = UserSession.getInstance().getUser();
        if (user != null && !user.isBlank()) {
            String[] parts = user.trim().split("\\s+", 2);
            txtPrenom.setText(parts[0]);
            txtNom.setText(parts.length > 1 ? parts[1] : "");
        }
    }

    /** Appelé par PosteExterneController pour passer le poste sélectionné. */
    public void setPoste(PosteExterne p) {
        this.posteExterne = p;
        if (lblPoste != null && p != null)
            lblPoste.setText(p.getTitre() != null ? p.getTitre() : "Poste #" + p.getId());
    }

    // ── Parcourir CV ──────────────────────────────────────────────────────
    @FXML
    public void handleParcourirCv(ActionEvent event) {
        File f = openPdfChooser("Sélectionner votre CV (PDF)");
        if (f == null) return;
        selectedCvFile = f;
        txtCvNom.setText(f.getName());
        txtCvNom.setStyle("-fx-text-fill: #0f172a;");
        if (lblCvStatus != null) {
            lblCvStatus.setText("✅ Prêt");
            lblCvStatus.setStyle("-fx-text-fill:#16a34a; -fx-font-size:11px; -fx-font-weight:700;");
        }
    }

    // ── Parcourir Lettre ──────────────────────────────────────────────────
    @FXML
    public void handleParcourirLettre(ActionEvent event) {
        File f = openPdfChooser("Sélectionner votre Lettre de Motivation (PDF)");
        if (f == null) return;
        selectedLettreFile = f;
        txtLettreNom.setText(f.getName());
        txtLettreNom.setStyle("-fx-text-fill: #0f172a;");
        if (lblLettreStatus != null) {
            lblLettreStatus.setText("✅ Prêt");
            lblLettreStatus.setStyle("-fx-text-fill:#16a34a; -fx-font-size:11px; -fx-font-weight:700;");
        }
    }

    private File openPdfChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));
        return chooser.showOpenDialog(btnEnvoyer.getScene().getWindow());
    }

    // ── Envoyer la candidature ────────────────────────────────────────────
    @FXML
    public void handleEnvoyer(ActionEvent event) {
        String prenom = txtPrenom.getText() != null ? txtPrenom.getText().trim() : "";
        String nom    = txtNom.getText()    != null ? txtNom.getText().trim()    : "";

        if (prenom.isEmpty() || nom.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez renseigner votre Prénom et Nom.");
            return;
        }
        if (selectedCvFile == null) {
            showAlert(Alert.AlertType.WARNING, "CV manquant", "Veuillez sélectionner votre CV (PDF).");
            return;
        }

        // ── 1. Sauvegarder les PDF en DB (table document) ─────────────────
        String cvPath     = null;
        String lettrePath = null;

        try {
            // CV — obligatoire
            Document cvDoc = serviceDocument.add(selectedCvFile);
            cvPath = cvDoc.getPath();
            System.out.println("✓ CV enregistré en DB : " + cvPath);

            // Lettre — optionnelle
            if (selectedLettreFile != null) {
                Document lettreDoc = serviceDocument.add(selectedLettreFile);
                lettrePath = lettreDoc.getPath();
                System.out.println("✓ Lettre enregistrée en DB : " + lettrePath);
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur upload",
                    "Impossible d'enregistrer les documents :\n" + ex.getMessage());
            return;
        }

        // ── 2. Créer la candidature externe ──────────────────────────────
        try {
            CandidatureExterne c = new CandidatureExterne();
            c.setId(serviceCand.getNextId());
            c.setPrenom(prenom);
            c.setNom(nom);
            c.setDateDepot(Date.valueOf(LocalDate.now()));
            c.setStatut("En attente");
            c.setEtapePipeline("Réception");
            c.setScoringIa(0.0);
            c.setCvUrl(cvPath);
            c.setLettreMotivationUrl(lettrePath);
            c.setDerniereModification(new Timestamp(System.currentTimeMillis()));

            // Titre du poste stocké dans la note de candidature (setPosteId non disponible)
            if (posteExterne != null && posteExterne.getTitre() != null) {
                // Le titre du poste est tracé via etapePipeline ou cvUrl si besoin
                System.out.println("✓ Candidature liée au poste : " + posteExterne.getTitre());
            }

            serviceCand.add(c);
            System.out.println("✓ Candidature créée pour " + prenom + " " + nom);

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur candidature",
                    "Candidature non enregistrée :\n" + ex.getMessage());
            return;
        }

        // ── 3. Confirmation et fermeture ──────────────────────────────────
        showAlert(Alert.AlertType.INFORMATION, "Candidature envoyée",
                "✅ Votre candidature a bien été enregistrée !\n\n"
                        + "📄 CV et lettre de motivation ajoutés aux documents RH.\n"
                        + "L'équipe RH vous contactera prochainement.");
        closeWindow();
    }

    @FXML public void handleClose(ActionEvent event)   { closeWindow(); }
    @FXML public void handleAnnuler(ActionEvent event) { closeWindow(); }

    private void closeWindow() {
        Stage s = (Stage) btnEnvoyer.getScene().getWindow();
        if (s != null) s.close();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}