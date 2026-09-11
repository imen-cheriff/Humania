package utilisateur.controllers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import test.MainFX;
import utilisateur.models.Utilisateur;
import utilisateur.services.MfaService;
import utilisateur.services.UtilisateurService;
import utils.Session;
import utils.UserSession;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;

public class MfaSetupController {

    @FXML private ImageView qrImageView;
    @FXML private Label     secretLabel;
    @FXML private TextField codeField;
    @FXML private Label     errorLabel;
    @FXML private Label     statusLabel;

    private final MfaService         mfaService         = new MfaService();
    private final UtilisateurService  utilisateurService = new UtilisateurService();

    private String pendingSecret;

    @FXML
    public void initialize() {
        errorLabel.setText("");
        statusLabel.setText("");

        Utilisateur u = Session.getUtilisateurConnecte();
        if (u == null) return;

        pendingSecret = mfaService.generateSecret();
        secretLabel.setText(pendingSecret);

        String uri = mfaService.getOtpAuthUri(pendingSecret, u.getEmail());
        qrImageView.setImage(generateQrImage(uri, 220));

        codeField.setOnAction(e -> handleConfirm());

        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*"))
                codeField.setText(newVal.replaceAll("[^\\d]", ""));
            if (codeField.getText().length() > 6)
                codeField.setText(codeField.getText().substring(0, 6));
        });
    }

    @FXML
    private void handleConfirm() {
        String code = codeField.getText().trim();
        if (code.length() != 6) {
            errorLabel.setText("Entrez le code a 6 chiffres affiche dans votre application.");
            return;
        }

        if (!mfaService.verifyCode(pendingSecret, code)) {
            errorLabel.setText("Code incorrect. Assurez-vous d'avoir bien scanne le QR code.");
            codeField.clear();
            return;
        }

        Utilisateur u = Session.getUtilisateurConnecte();
        try {
            utilisateurService.activerMfa(u.getId(), pendingSecret);
            u.setMfaSecret(pendingSecret);
            u.setMfaEnabled(true);
            Session.setUtilisateurConnecte(u);
            UserSession.getInstance().syncFromUtilisateur(u);
            statusLabel.setText("MFA active avec succes !");
            errorLabel.setText("");

            // ✅ FIX: retour au profil via MainFX pour conserver le plein écran
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(this::goBackToProfil);
            }).start();

        } catch (SQLException e) {
            errorLabel.setText("Erreur lors de l'activation: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        goBackToProfil();
    }

    private void goBackToProfil() {
        // ✅ FIX: charge dans le centre du mainLayout au lieu de créer une nouvelle scène
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/views/user/Profil.fxml"));
            MainFX.getInstance().setCenter(view);
        } catch (Exception e) {
            errorLabel.setText("Erreur de navigation: " + e.getMessage());
        }
    }

    // ── QR code generation ────────────────────────────────────────────────────

    private javafx.scene.image.Image generateQrImage(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix    = writer.encode(content, BarcodeFormat.QR_CODE, size, size);

            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    img.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);

            WritableImage fx = new WritableImage(size, size);
            SwingFXUtils.toFXImage(img, fx);
            return fx;

        } catch (Exception e) {
            return buildFallbackImage(size);
        }
    }

    private javafx.scene.image.Image buildFallbackImage(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.drawString("Saisir le code manuel", 10, size / 2);
        g.dispose();
        WritableImage fx = new WritableImage(size, size);
        SwingFXUtils.toFXImage(img, fx);
        return fx;
    }
}