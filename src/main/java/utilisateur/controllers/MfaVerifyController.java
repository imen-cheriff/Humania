package utilisateur.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import test.MainFX;
import utilisateur.enums.Role;
import utilisateur.models.Utilisateur;
import utilisateur.services.MfaService;
import utils.Session;
import utils.UserSession;

public class MfaVerifyController {

    @FXML private Label     userLabel;
    @FXML private TextField codeField;
    @FXML private Label     errorLabel;
    @FXML private Button    verifyBtn;

    private Utilisateur pendingUser;
    private final MfaService mfaService = new MfaService();

    public void setPendingUser(Utilisateur user) {
        this.pendingUser = user;
        userLabel.setText("Connexion en tant que : " + user.getPrenom() + " " + user.getNom());
    }

    @FXML
    public void initialize() {
        errorLabel.setText("");
        codeField.setOnAction(e -> handleVerify());
        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*"))    codeField.setText(newVal.replaceAll("[^\\d]", ""));
            if (codeField.getText().length() > 6)
                codeField.setText(codeField.getText().substring(0, 6));
        });
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();
        if (code.length() != 6) {
            errorLabel.setText("Le code doit contenir exactement 6 chiffres.");
            return;
        }

        if (mfaService.verifyCode(pendingUser.getMfaSecret(), code)) {
            Session.setUtilisateurConnecte(pendingUser);
            UserSession.getInstance().syncFromUtilisateur(pendingUser);
            // ✅ FIX: déléguer le login à MainFX qui gère le plein écran
            MainFX.getInstance().onLoginSuccess(pendingUser.getUsername());
        } else {
            errorLabel.setText("Code incorrect ou expiré. Réessayez.");
            codeField.clear();
            codeField.requestFocus();
        }
    }

    @FXML
    private void handleCancel() {
        // ✅ FIX: retour login via MainFX
        MainFX.showLoginScreen();
    }
}