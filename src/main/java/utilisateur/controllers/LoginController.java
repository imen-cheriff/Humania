package utilisateur.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import test.MainFX;
import utilisateur.models.Utilisateur;
import utils.Session;
import utils.UserSession;
import utilisateur.services.FaceRecognitionService;
import utilisateur.services.GoogleAuthService;
import utilisateur.services.UtilisateurService;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;    // fx:id added in Login.fxml
    @FXML private Label         attemptsLabel;  // orange: "X tentative(s) restante(s)"
    @FXML private Label         lockoutLabel;   // red:    live countdown during lockout

    private final UtilisateurService service           = new UtilisateurService();
    private final GoogleAuthService  googleAuthService = new GoogleAuthService();

    // ── Brute-force state (instance-level — resets when the scene reloads) ────
    private static final int MAX_ATTEMPTS    = 3;
    private static final int LOCKOUT_SECONDS = 60;

    private int     failedAttempts = 0;
    private boolean locked         = false;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?>        countdownTask;

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        passwordField.setOnAction(e -> login());
        // Hide feedback labels initially (FXML sets visible=false but this is a safety net)
        if (attemptsLabel != null) { attemptsLabel.setVisible(false); attemptsLabel.setManaged(false); }
        if (lockoutLabel  != null) { lockoutLabel.setVisible(false);  lockoutLabel.setManaged(false);  }
    }

    // ── Login classique ───────────────────────────────────────────────────────

    @FXML
    public void login() {
        // Locked out — ignore (button is disabled, but guard anyway)
        if (locked) return;

        String email      = emailField.getText().trim();
        String motDePasse = passwordField.getText().trim();

        if (email.isEmpty() || motDePasse.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs."); return;
        }

        try {
            Utilisateur u = service.login(email, motDePasse);

            if (u == null) {
                // ── Wrong credentials ────────────────────────────────────────
                failedAttempts++;

                if (failedAttempts >= MAX_ATTEMPTS) {
                    // Reached the limit → trigger lockout
                    demarrerLockout();
                } else {
                    // Show how many tries remain
                    int remaining = MAX_ATTEMPTS - failedAttempts;
                    showAttemptsWarning(remaining);
                }
                return;
            }

            // ── Correct credentials — reset counter and proceed ──────────────
            failedAttempts = 0;
            hideAllFeedback();
            verifierStatutEtNaviguer(u);

        } catch (SQLException e) {
            // login() throws for non-active accounts (bloqué, suspendu, archivé…)
            afficherErreur(e.getMessage());
        }
    }

    // ── Lockout mechanics ─────────────────────────────────────────────────────

    /** Disables the login button and counts down LOCKOUT_SECONDS seconds visually. */
    private void demarrerLockout() {
        locked = true;

        if (loginButton    != null) loginButton.setDisable(true);
        if (attemptsLabel  != null) { attemptsLabel.setVisible(false); attemptsLabel.setManaged(false); }
        showLockoutLabel(LOCKOUT_SECONDS); // first tick immediately

        // Daemon scheduler — won't prevent JVM shutdown
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "login-lockout-countdown");
                t.setDaemon(true);
                return t;
            });
        }

        final int[] remaining = {LOCKOUT_SECONDS};

        countdownTask = scheduler.scheduleAtFixedRate(() -> {
            remaining[0]--;

            if (remaining[0] <= 0) {
                // Lockout expired
                Platform.runLater(this::terminerLockout);
                countdownTask.cancel(false);
            } else {
                final int r = remaining[0];
                Platform.runLater(() -> showLockoutLabel(r));
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /** Re-enables login after the lockout period ends. */
    private void terminerLockout() {
        locked         = false;
        failedAttempts = 0;
        if (loginButton   != null) loginButton.setDisable(false);
        hideAllFeedback();
        passwordField.clear(); // clear the wrong password for convenience
    }

    // ── UI helpers for feedback labels ────────────────────────────────────────

    private void showAttemptsWarning(int remaining) {
        if (attemptsLabel == null) {
            // Fallback if FXML doesn't have the label yet
            afficherErreur("Email ou mot de passe incorrect.\n"
                    + remaining + " tentative(s) restante(s) avant blocage (1 min).");
            return;
        }
        String icon = remaining == 1 ? "⚠️" : "⚠️";
        attemptsLabel.setText(icon + "  Email ou mot de passe incorrect. "
                + "Il vous reste " + remaining + " tentative(s) avant blocage temporaire.");
        attemptsLabel.setVisible(true);
        attemptsLabel.setManaged(true);
        if (lockoutLabel != null) { lockoutLabel.setVisible(false); lockoutLabel.setManaged(false); }
    }

    private void showLockoutLabel(int secondsLeft) {
        if (lockoutLabel == null) return;
        lockoutLabel.setText("🔒  Trop de tentatives échouées. "
                + "Réessayez dans " + secondsLeft + " seconde(s).");
        lockoutLabel.setVisible(true);
        lockoutLabel.setManaged(true);
    }

    private void hideAllFeedback() {
        if (attemptsLabel != null) { attemptsLabel.setVisible(false); attemptsLabel.setManaged(false); }
        if (lockoutLabel  != null) { lockoutLabel.setVisible(false);  lockoutLabel.setManaged(false);  }
    }

    // ── Mot de passe oublié ───────────────────────────────────────────────────

    @FXML
    public void forgotPassword() {
        changerScene("/views/user/ForgotPassword.fxml", "Mot de passe oublie");
    }

    // ── Google login ──────────────────────────────────────────────────────────

    @FXML
    public void googleLogin() {
        new Thread(() -> {
            try {
                GoogleAuthService.GoogleUser g = googleAuthService.authenticate(email -> {
                    try {
                        service.connecterViaGoogle(email);
                        return null;
                    } catch (Exception ex) {
                        return ex.getMessage();
                    }
                });

                Utilisateur u = service.connecterViaGoogle(g.email());
                Platform.runLater(() -> {
                    Session.setUtilisateurConnecte(u);
                    UserSession.getInstance().syncFromUtilisateur(u);
                    afficherInfo("Bienvenue " + u.getPrenom() + " " + u.getNom() + " !");
                    naviguerSelonRole(u);
                });

            } catch (Exception e) {
                Platform.runLater(() -> afficherErreur(
                        "Connexion Google refusee:\n\n" + e.getMessage()));
            }
        }).start();
    }

    // ── Face login ────────────────────────────────────────────────────────────

    @FXML
    public void faceLogin() {
        new FaceCaptureDialog(
                emailField.getScene().getWindow(),
                FaceCaptureDialog.Mode.LOGIN,
                new FaceCaptureDialog.CaptureCallback() {

                    @Override
                    public void onSuccess(byte[] faceData, FaceRecognitionService faceService) {
                        new Thread(() -> {
                            try {
                                List<Utilisateur> candidates = service.getUtilisateursAvecDonneesFaciales();
                                Utilisateur reconnu = faceService.trouverUtilisateur(faceData, candidates);

                                Platform.runLater(() -> {
                                    if (reconnu == null) {
                                        afficherErreur(
                                                "Visage non reconnu.\n\n"
                                                        + "Si c'est votre premiere fois, enregistrez d'abord\n"
                                                        + "votre visage depuis votre profil.");
                                        return;
                                    }
                                    String statut = reconnu.getStatut();
                                    if ("bloque".equalsIgnoreCase(statut))   { afficherErreur("Votre compte est bloque."); return; }
                                    if ("suspendu".equalsIgnoreCase(statut)) { afficherErreur("Votre compte est suspendu."); return; }
                                    // Face login bypasses MFA (biometric = second factor)
                                    Session.setUtilisateurConnecte(reconnu);
                                    UserSession.getInstance().syncFromUtilisateur(reconnu);
                                    afficherInfo("Bienvenue " + reconnu.getPrenom() + " " + reconnu.getNom() + " !");
                                    naviguerSelonRole(reconnu);
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() ->
                                        afficherErreur("Erreur lors de la reconnaissance: " + ex.getMessage()));
                            }
                        }).start();
                    }

                    @Override
                    public void onCancelled(String msg) {
                        if (msg != null) afficherErreur(msg);
                    }
                }
        ).show();
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private void verifierStatutEtNaviguer(Utilisateur u) {
        String statut = u.getStatut();
        if (statut != null) {
            if (statut.equalsIgnoreCase("bloque"))   { afficherErreur("Votre compte est bloque.");   return; }
            if (statut.equalsIgnoreCase("suspendu")) { afficherErreur("Votre compte est suspendu."); return; }
        }
        if (u.isMfaEnabled()) { ouvrirMfaVerify(u); return; }
        // Synchroniser les deux sessions
        Session.setUtilisateurConnecte(u);
        UserSession.getInstance().syncFromUtilisateur(u);
        naviguerSelonRole(u);
    }

    private void ouvrirMfaVerify(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/user/MfaVerify.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Verification MFA");
            MfaVerifyController ctrl = loader.getController();
            ctrl.setPendingUser(u);
            stage.show();
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir l'ecran MFA.\n" + e.getMessage());
        }
    }

    private void naviguerSelonRole(Utilisateur u) {
        // Delegate entirely to MainFX — it owns the stage and all navigation
        String displayName = UserSession.getInstance().getUser();
        if (displayName == null || displayName.isBlank())
            displayName = emailField.getText() != null ? emailField.getText().trim() : "user";

        if (MainFX.getInstance() != null) {
            final String name = displayName;
            Platform.runLater(() -> MainFX.getInstance().onLoginSuccess(name));
        } else {
            afficherErreur("Erreur interne : application non initialisée.");
        }
    }

    private void changerScene(String fxml, String titre) {
        try {
            java.net.URL resource = getClass().getResource(fxml);
            if (resource == null) { afficherErreur("Ressource introuvable: " + fxml); return; }
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(FXMLLoader.load(resource)));
            stage.setTitle(titre);
            stage.show();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            afficherErreur("Impossible de charger la page.\n" +
                    (cause.getMessage() != null ? cause.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private void afficherErreur(String msg) { new Alert(Alert.AlertType.ERROR,       msg, ButtonType.OK).showAndWait(); }
    private void afficherInfo(String msg)   { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}