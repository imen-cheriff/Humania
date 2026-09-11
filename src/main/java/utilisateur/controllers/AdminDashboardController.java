package utilisateur.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import test.MainFX;
import utilisateur.models.Utilisateur;
import utils.UserSession;
import utilisateur.services.UtilisateurService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label avatarLabel;
    @FXML private Label totalLabel;
    @FXML private Label actifsLabel;
    @FXML private Label inactifsLabel;
    @FXML private VBox activityList;
    @FXML private HBox userBlock;

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bindCurrentUser();
        loadStats();
    }

    private void bindCurrentUser() {
        // Use UserSession (unified session — no Session.java dependency)
        UserSession session = UserSession.getInstance();
        String displayName = session.getUser();
        String email       = session.getEmail();

        if (usernameLabel != null)
            usernameLabel.setText(displayName != null && !displayName.isBlank() ? displayName : "—");
        if (emailLabel != null)
            emailLabel.setText(email != null && !email.isBlank() ? email : "—");

        // Build initials avatar from display name
        if (userBlock != null && avatarLabel != null) {
            String initials = "?";
            if (displayName != null && !displayName.isBlank()) {
                String[] parts = displayName.trim().split("\\s+");
                initials = parts.length >= 2
                        ? String.valueOf(parts[0].charAt(0)).toUpperCase() + String.valueOf(parts[1].charAt(0)).toUpperCase()
                        : displayName.substring(0, Math.min(2, displayName.length())).toUpperCase();
            }
            avatarLabel.setText(initials);
        }
    }

    private void loadStats() {
        try {
            List<Utilisateur> all = utilisateurService.recupererTous();
            long actifs = all.stream()
                    .filter(u -> u.getStatut() != null && u.getStatut().equalsIgnoreCase("Actif"))
                    .count();
            totalLabel.setText(String.valueOf(all.size()));
            actifsLabel.setText(String.valueOf(actifs));
            inactifsLabel.setText(String.valueOf(all.size() - actifs));
        } catch (Exception e) {
            totalLabel.setText("0");
            actifsLabel.setText("0");
            inactifsLabel.setText("0");
        }
    }

    @FXML
    private void openProfile(MouseEvent event) {
        if (MainFX.getInstance() != null) {
            MainFX.getInstance().navigateTo(MainFX.PROFIL);
        }
    }

    @FXML
    private void openGestionUtilisateurs(MouseEvent event) {
        if (MainFX.getInstance() != null) {
            MainFX.getInstance().navigateTo(MainFX.GESTION_UTILISATEUR);
        }
    }
}