package utilisateur.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import test.MainFX;
import utilisateur.services.MailService;
import utilisateur.services.UtilisateurService;

import java.security.SecureRandom;
import java.sql.SQLException;

/**
 * 3-step forgot password flow:
 *  Step 1 — user enters their email
 *  Step 2 — user enters the 6-digit OTP sent to their email
 *  Step 3 — user sets a new password
 */
public class ForgotPasswordController {

    @FXML private BorderPane rootPane;

    // Step 1 — Email
    @FXML private VBox        step1Pane;
    @FXML private TextField   emailField;
    @FXML private Label       step1ErrorLabel;

    // Step 2 — OTP
    @FXML private VBox        step2Pane;
    @FXML private TextField   otpField;
    @FXML private Label       step2ErrorLabel;
    @FXML private Label       otpEmailLabel;

    // Step 3 — New password
    @FXML private VBox          step3Pane;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         step3ErrorLabel;

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final MailService         mailService        = new MailService();

    private String pendingEmail;
    private String generatedOtp;

    @FXML
    public void initialize() {
        showStep(1);
        otpField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*"))        otpField.setText(n.replaceAll("[^\\d]", ""));
            if (otpField.getText().length() > 6)
                otpField.setText(otpField.getText().substring(0, 6));
        });
    }

    // ── Step 1: Send OTP ─────────────────────────────────────────────────────

    @FXML
    private void handleSendOtp() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) { step1ErrorLabel.setText("Veuillez saisir votre email."); return; }

        try {
            if (!utilisateurService.emailExiste(email)) {
                step1ErrorLabel.setText("Aucun compte associe a cet email.");
                return;
            }
        } catch (SQLException e) {
            step1ErrorLabel.setText("Erreur: " + e.getMessage()); return;
        }

        generatedOtp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        pendingEmail = email;

        try {
            mailService.envoyerOtp(email, generatedOtp);
        } catch (Exception e) {
            step1ErrorLabel.setText("Echec d'envoi de l'email: " + e.getMessage()); return;
        }

        otpEmailLabel.setText("Code envoye a : " + email);
        step1ErrorLabel.setText("");
        showStep(2);
    }

    // ── Step 2: Verify OTP ───────────────────────────────────────────────────

    @FXML
    private void handleVerifyOtp() {
        String entered = otpField.getText().trim();
        if (entered.length() != 6) {
            step2ErrorLabel.setText("Le code doit contenir 6 chiffres."); return;
        }
        if (!entered.equals(generatedOtp)) {
            step2ErrorLabel.setText("Code incorrect. Verifiez votre email.");
            otpField.clear(); return;
        }
        step2ErrorLabel.setText("");
        showStep(3);
    }

    @FXML
    private void handleResendOtp() {
        try {
            generatedOtp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            mailService.envoyerOtp(pendingEmail, generatedOtp);
            step2ErrorLabel.setText("Nouveau code envoye !");
        } catch (Exception e) {
            step2ErrorLabel.setText("Echec: " + e.getMessage());
        }
    }

    // ── Step 3: Set new password ──────────────────────────────────────────────

    @FXML
    private void handleResetPassword() {
        String newPwd     = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        if (newPwd.length() < 6) {
            step3ErrorLabel.setText("Le mot de passe doit contenir au moins 6 caracteres."); return;
        }
        if (!newPwd.equals(confirmPwd)) {
            step3ErrorLabel.setText("Les mots de passe ne correspondent pas."); return;
        }

        try {
            utilisateurService.reinitialiserMotDePasse(pendingEmail, newPwd);
            showSuccess("Mot de passe reinitialise avec succes !\nVous pouvez maintenant vous connecter.");
            goToLogin();
        } catch (SQLException e) {
            step3ErrorLabel.setText("Erreur: " + e.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleBackToLogin() { goToLogin(); }

    @FXML
    private void handleBackToStep1() { showStep(1); }

    private void showStep(int step) {
        step1Pane.setVisible(step == 1); step1Pane.setManaged(step == 1);
        step2Pane.setVisible(step == 2); step2Pane.setManaged(step == 2);
        step3Pane.setVisible(step == 3); step3Pane.setManaged(step == 3);
    }

    private void goToLogin() {
        // ✅ FIX: utilise MainFX.showLoginScreen() pour revenir proprement à l'écran de login
        // showLoginView() remet la scène à 900x650 (taille login normale), pas de problème de plein écran
        MainFX.showLoginScreen();
    }

    private void showError(String msg)   { new Alert(Alert.AlertType.ERROR,       msg, ButtonType.OK).showAndWait(); }
    private void showSuccess(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}