package utilisateur.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import test.MainFX;
import utilisateur.enums.Role;
import utilisateur.models.Utilisateur;
import utilisateur.services.FaceRecognitionService;
import utilisateur.services.MfaService;
import utilisateur.services.UtilisateurService;
import utils.ImageUtil;
import utils.Session;
import utils.UserSession;

import java.io.File;
import java.sql.SQLException;

public class ProfilController {

    // ── Standard fields ───────────────────────────────────────────────────────
    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     emailField;
    @FXML private TextField     usernameField;
    @FXML private TextField     numtelField;
    @FXML private TextField     pdpField;
    @FXML private PasswordField nouveauMotDePasseField;
    @FXML private PasswordField confirmMotDePasseField;

    // ── Photo section ─────────────────────────────────────────────────────────
    @FXML private StackPane photoPane;
    @FXML private Label     photoInitialsLabel;
    @FXML private TextField urlImageField;
    @FXML private Button    deletePhotoBtn;

    // ── Face section ──────────────────────────────────────────────────────────
    @FXML private Label  faceStatusLabel;
    @FXML private Button enrollFaceBtn;
    @FXML private Button deleteFaceBtn;

    // ── MFA section ───────────────────────────────────────────────────────────
    @FXML private Label  mfaStatusLabel;
    @FXML private Button mfaToggleBtn;
    @FXML private Button retourBtn;  // null-safe: may not exist in all FXML versions

    private final UtilisateurService service    = new UtilisateurService();
    private final MfaService         mfaService = new MfaService();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        Utilisateur u = Session.getUtilisateurConnecte();
        if (u == null) return;

        nomField.setText(safe(u.getNom()));
        prenomField.setText(safe(u.getPrenom()));
        emailField.setText(safe(u.getEmail()));
        usernameField.setText(safe(u.getUsername()));
        numtelField.setText(safe(u.getNumtel()));

        String pdp = safe(u.getPdp());
        pdpField.setText(pdp);
        urlImageField.setText(pdp);

        refreshPhotoPane(pdp, u.getPrenom(), u.getNom());
        updateFaceUI(u);
        updateMfaUI(u);
        updateRetourBtn(u);
    }

    // ── Photo handling ────────────────────────────────────────────────────────

    @FXML
    private void handleParcourirImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(urlImageField.getScene().getWindow());
        if (file != null) {
            String uri = file.toURI().toString();
            urlImageField.setText(uri);
            pdpField.setText(uri);

            Utilisateur u = Session.getUtilisateurConnecte();
            refreshPhotoPane(uri,
                    u != null ? u.getPrenom() : "",
                    u != null ? u.getNom()    : "");
        }
    }

    @FXML
    private void handleSupprimerPhoto(ActionEvent event) {
        urlImageField.setText("");
        pdpField.setText("");
        Utilisateur u = Session.getUtilisateurConnecte();
        refreshPhotoPane("", u != null ? u.getPrenom() : "", u != null ? u.getNom() : "");
    }

    private void refreshPhotoPane(String pdpPath, String prenom, String nom) {
        if (photoPane == null) return;
        photoPane.getChildren().clear();

        Image img = ImageUtil.loadImage(pdpPath);
        double r = 50;

        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(r * 2); iv.setFitHeight(r * 2);
            iv.setPreserveRatio(false);
            Circle clip = new Circle(r, r, r);
            iv.setClip(clip);
            Circle border = new Circle(r, Color.web("#ede9fe"));
            photoPane.getChildren().addAll(border, iv);
        } else {
            Circle circle = new Circle(r, Color.web("#ede9fe"));
            String p = prenom != null && !prenom.isEmpty() ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
            String n = nom    != null && !nom.isEmpty()    ? String.valueOf(nom.charAt(0)).toUpperCase()    : "";
            String initials = (p + n).isEmpty() ? "?" : p + n;
            Label lbl = new Label(initials);
            lbl.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 28; -fx-font-weight: bold;");
            photoPane.getChildren().addAll(circle, lbl);
        }

        boolean hasPhoto = img != null;
        if (deletePhotoBtn != null) {
            deletePhotoBtn.setVisible(hasPhoto);
            deletePhotoBtn.setManaged(hasPhoto);
        }
        if (photoInitialsLabel != null) photoInitialsLabel.setVisible(false);
    }

    // ── Face UI ───────────────────────────────────────────────────────────────

    private void updateFaceUI(Utilisateur u) {
        if (faceStatusLabel == null) return;
        boolean hasFace = u.getDonneesFaciales() != null;
        if (hasFace) {
            faceStatusLabel.setText("✅ Visage enregistre — connexion par visage activee.");
            faceStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #38a169; -fx-font-weight: bold;");
            if (enrollFaceBtn != null) enrollFaceBtn.setText("Mettre a jour mon visage");
            if (deleteFaceBtn != null) { deleteFaceBtn.setVisible(true);  deleteFaceBtn.setManaged(true);  }
        } else {
            faceStatusLabel.setText("❌ Aucun visage enregistre. Cliquez le bouton pour en ajouter un.");
            faceStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e53e3e;");
            if (enrollFaceBtn != null) enrollFaceBtn.setText("Enregistrer mon visage");
            if (deleteFaceBtn != null) { deleteFaceBtn.setVisible(false); deleteFaceBtn.setManaged(false); }
        }
    }

    @FXML
    private void handleEnregistrerVisage(ActionEvent event) {
        Utilisateur u = Session.getUtilisateurConnecte();
        if (u == null) { showError("Session expiree."); return; }
        // FaceCaptureDialog s'ouvre en modal par-dessus la fenêtre principale —
        // pas de changement de scène, donc pas de problème de taille.
        new FaceCaptureDialog(
                nomField.getScene().getWindow(),
                FaceCaptureDialog.Mode.ENROLL,
                new FaceCaptureDialog.CaptureCallback() {
                    @Override
                    public void onSuccess(byte[] faceData, FaceRecognitionService fs) {
                        try {
                            service.mettreAJourDonneesFaciales(u.getId(), faceData);
                            u.setDonneesFaciales(faceData);
                            Session.setUtilisateurConnecte(u);
                            updateFaceUI(u);
                            showSuccess("Visage enregistre avec succes !\nVous pouvez maintenant vous connecter avec votre visage.");
                        } catch (SQLException e) { showError("Erreur lors de la sauvegarde: " + e.getMessage()); }
                    }
                    @Override public void onCancelled(String msg) { if (msg != null) showError(msg); }
                }
        ).show();
    }

    @FXML
    private void handleSupprimerVisage(ActionEvent event) {
        Utilisateur u = Session.getUtilisateurConnecte();
        if (u == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer les donnees faciales ? La connexion par visage sera desactivee.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.mettreAJourDonneesFaciales(u.getId(), null);
                    u.setDonneesFaciales(null);
                    Session.setUtilisateurConnecte(u);
                    updateFaceUI(u);
                    showSuccess("Donnees faciales supprimees.");
                } catch (SQLException e) { showError("Erreur: " + e.getMessage()); }
            }
        });
    }

    // ── MFA UI ────────────────────────────────────────────────────────────────

    private void updateMfaUI(Utilisateur u) {
        if (mfaStatusLabel == null || mfaToggleBtn == null) return;
        if (u.isMfaEnabled()) {
            mfaStatusLabel.setText("✅ Activee");
            mfaStatusLabel.setStyle("-fx-text-fill: #38a169; -fx-font-weight: bold;");
            mfaToggleBtn.setText("Desactiver MFA");
            mfaToggleBtn.setStyle("-fx-background-color: #fed7d7; -fx-text-fill: #c53030;" +
                    "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        } else {
            mfaStatusLabel.setText("❌ Desactivee");
            mfaStatusLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold;");
            mfaToggleBtn.setText("Activer MFA");
            mfaToggleBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2);" +
                    "-fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        }
    }

    @FXML
    private void handleMfaToggle(ActionEvent event) {
        Utilisateur u = Session.getUtilisateurConnecte();
        if (u == null) { showError("Session expiree."); return; }
        if (!u.isMfaEnabled()) openMfaSetup();
        else disableMfaWithConfirmation(u);
    }

    private void openMfaSetup() {
        // ✅ FIX: charge dans le centre du mainLayout au lieu de créer une nouvelle scène
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/views/user/MfaSetup.fxml"));
            MainFX.getInstance().setCenter(view);
        } catch (Exception e) {
            showError("Impossible d'ouvrir la configuration MFA: " + e.getMessage());
        }
    }

    private void disableMfaWithConfirmation(Utilisateur u) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Desactiver MFA");
        dialog.setHeaderText("Confirmez avec votre code d'authentification");
        dialog.setContentText("Code a 6 chiffres :");
        dialog.showAndWait().ifPresent(code -> {
            if (!mfaService.verifyCode(u.getMfaSecret(), code)) {
                showError("Code incorrect. Desactivation annulee."); return;
            }
            try {
                service.desactiverMfa(u.getId());
                u.setMfaEnabled(false); u.setMfaSecret(null);
                Session.setUtilisateurConnecte(u);
                UserSession.getInstance().syncFromUtilisateur(u);
                updateMfaUI(u);
                showSuccess("MFA desactive avec succes.");
            } catch (SQLException e) { showError("Erreur: " + e.getMessage()); }
        });
    }

    // ── Profile save ──────────────────────────────────────────────────────────

    @FXML
    private void handleRetour(ActionEvent event) {
        Utilisateur u = Session.getUtilisateurConnecte();
        if (u != null && u.getRole() == Role.ADMIN) {
            // ADMIN vient de GestionUtilisateur → on y retourne
            MainFX.getInstance().navigateTo(MainFX.GESTION_UTILISATEUR);
        } else {
            // EMPLOYE/MANAGER : pas de page "retour" pertinente — ce bouton
            // est normalement caché, mais s'il est cliqué on reste sur le profil.
            // (Ne rien faire, ou revenir au dashboard)
            MainFX.getInstance().navigateTo(MainFX.ADMIN_DASHBOARD);
        }
    }

    /**
     * Cache le bouton "← Retour" pour les non-admins :
     * un employé ou manager n'a pas de page d'où il vient dans le module utilisateur.
     */
    private void updateRetourBtn(Utilisateur u) {
        if (retourBtn == null) return;
        boolean isAdmin = u.getRole() == Role.ADMIN;
        retourBtn.setVisible(isAdmin);
        retourBtn.setManaged(isAdmin);
    }

    @FXML
    private void handleEnregistrer(ActionEvent event) {
        Utilisateur u = Session.getUtilisateurConnecte();
        if (u == null) { showError("Session expiree."); return; }

        String nom               = nomField.getText().trim();
        String prenom            = prenomField.getText().trim();
        String email             = emailField.getText().trim();
        String username          = usernameField.getText().trim();
        String nouveauMotDePasse = nouveauMotDePasseField.getText();
        String confirm           = confirmMotDePasseField.getText();

        if (nom.isEmpty())      { showError("Le nom est requis.");      return; }
        if (prenom.isEmpty())   { showError("Le prenom est requis.");   return; }
        if (email.isEmpty())    { showError("L'email est requis.");     return; }
        if (username.isEmpty()) { showError("Le username est requis."); return; }

        if (nouveauMotDePasse != null && !nouveauMotDePasse.isEmpty()
                && !nouveauMotDePasse.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas."); return;
        }

        try {
            if (service.emailExistePourAutre(u.getId(), email))       { showError("Cet email est deja utilise."); return; }
            if (service.usernameExistePourAutre(u.getId(), username)) { showError("Ce username est deja utilise."); return; }
        } catch (SQLException e) { showError("Verification echouee: " + e.getMessage()); return; }

        u.setNom(nom); u.setPrenom(prenom); u.setEmail(email); u.setUsername(username);
        u.setNumtel(numtelField.getText().trim().isEmpty() ? null : numtelField.getText().trim());

        String newPdp = urlImageField.getText().trim();
        u.setPdp(newPdp.isEmpty() ? null : newPdp);
        pdpField.setText(newPdp);

        try {
            service.modifierProfil(u, nouveauMotDePasse != null && !nouveauMotDePasse.isEmpty() ? nouveauMotDePasse : null);
            Session.setUtilisateurConnecte(u);
            UserSession.getInstance().syncFromUtilisateur(u);
            refreshPhotoPane(u.getPdp(), u.getPrenom(), u.getNom());
            showSuccess("Profil enregistre avec succes.");
        } catch (SQLException e) { showError("Erreur: " + e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String safe(String s) { return s != null ? s : ""; }
    private void showError(String msg)   { new Alert(Alert.AlertType.ERROR,       msg, ButtonType.OK).showAndWait(); }
    private void showSuccess(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}