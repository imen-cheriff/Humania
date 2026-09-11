package competence.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;

/**
 * EnrollHelper — à appeler depuis ton FormationController
 * pour afficher le bouton Enroll et gérer l'inscription.
 *
 * UTILISATION dans buildFormationCard() ou createFormationCard() :
 *   HBox enrollRow = EnrollHelper.buildEnrollSection(formation_id, employe_id, connection);
 *   card.getChildren().add(enrollRow);
 */
public class EnrollHelper {

    private static final int EMPLOYE_ID = 1; // TODO: remplacer par session réelle

    /**
     * Construit la section formateur + bouton enroll pour une carte formation.
     */
    public static VBox buildFormationExtra(int formationId, Connection connection) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(12, 16, 14, 16));
        container.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 0 0 14 14;" +
                "-fx-border-color: #F3F4F6; -fx-border-width: 1 0 0 0;");

        // ── Formateur row ──────────────────────────────────────────────────
        HBox formateurRow = buildFormateurRow(formationId, connection);

        // ── Enroll button row ──────────────────────────────────────────────
        HBox enrollRow = buildEnrollRow(formationId, connection);

        container.getChildren().addAll(formateurRow, enrollRow);
        return container;
    }

    // ── FORMATEUR ──────────────────────────────────────────────────────────────
    private static HBox buildFormateurRow(int formationId, Connection connection) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT formateur_nom, formateur_type FROM formation WHERE id = ?");
            ps.setInt(1, formationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String nom  = rs.getString("formateur_nom");
                String type = rs.getString("formateur_type");

                if (nom != null && !nom.isBlank()) {
                    // Avatar circle with initials
                    StackPane avatar = new StackPane();
                    avatar.setPrefSize(32, 32); avatar.setMinSize(32, 32);
                    Circle bg = new Circle(16, Color.web(
                            "INTERNE".equals(type) ? "#DBEAFE" : "#FEF3C7"));
                    String initial = nom.substring(0, 1).toUpperCase();
                    Label initLbl = new Label(initial);
                    initLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " +
                            ("INTERNE".equals(type) ? "#1D4ED8" : "#92400E") + ";");
                    avatar.getChildren().addAll(bg, initLbl);

                    // Info
                    VBox info = new VBox(1);
                    Label nameLbl = new Label(nom);
                    nameLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #374151;");
                    Label typeLbl = new Label("INTERNE".equals(type) ? "🏢 Formateur interne" : "🌐 Formateur externe");
                    typeLbl.setStyle("-fx-font-size: 10; -fx-text-fill: " +
                            ("INTERNE".equals(type) ? "#2563EB" : "#D97706") + ";");
                    info.getChildren().addAll(nameLbl, typeLbl);

                    row.getChildren().addAll(avatar, info);
                    return row;
                }
            }
        } catch (SQLException ignored) {}

        // Fallback — no formateur info
        Label noFormateur = new Label("👤  Formateur non assigné");
        noFormateur.setStyle("-fx-font-size: 11; -fx-text-fill: #9CA3AF;");
        row.getChildren().add(noFormateur);
        return row;
    }

    // ── ENROLL ROW ─────────────────────────────────────────────────────────────
    private static HBox buildEnrollRow(int formationId, Connection connection) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Check enrollment status
        EnrollStatus status = checkEnrollStatus(formationId, connection);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        switch (status) {
            case ENROLLED_IN_PROGRESS -> {
                Label badge = new Label("✅ Inscrit · En cours");
                badge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;" +
                        "-fx-font-size: 11; -fx-font-weight: bold;" +
                        "-fx-padding: 6 14; -fx-background-radius: 20;");

                Button btnContinue = new Button("▶ Continuer");
                btnContinue.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;" +
                        "-fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand;" +
                        "-fx-padding: 8 18; -fx-background-radius: 20;");
                row.getChildren().addAll(badge, spacer, btnContinue);
            }
            case ENROLLED_COMPLETED -> {
                Label badge = new Label("🏆 Complété");
                badge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;" +
                        "-fx-font-size: 11; -fx-font-weight: bold;" +
                        "-fx-padding: 6 14; -fx-background-radius: 20;");
                row.getChildren().addAll(spacer, badge);
            }
            case NOT_ENROLLED -> {
                // Find available session
                int sessionId = findOpenSession(formationId, connection);

                if (sessionId > 0) {
                    Button btnEnroll = buildEnrollButton(formationId, sessionId, connection, row);
                    row.getChildren().addAll(spacer, btnEnroll);
                } else {
                    Label noSession = new Label("🔒 Aucune session ouverte");
                    noSession.setStyle("-fx-font-size: 11; -fx-text-fill: #9CA3AF;");
                    row.getChildren().addAll(spacer, noSession);
                }
            }
        }

        return row;
    }

    private static Button buildEnrollButton(int formationId, int sessionId,
                                            Connection connection, HBox parentRow) {
        Button btn = new Button("+ S'inscrire");
        btn.setStyle("-fx-background-color: linear-gradient(to right, #4F46E5, #7C3AED);" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;" +
                "-fx-cursor: hand; -fx-padding: 9 22; -fx-background-radius: 22;" +
                "-fx-effect: dropshadow(gaussian,rgba(79,70,229,0.35),10,0,0,3);");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(to right, #4338CA, #6D28D9);" +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;" +
                        "-fx-cursor: hand; -fx-padding: 9 22; -fx-background-radius: 22;" +
                        "-fx-effect: dropshadow(gaussian,rgba(79,70,229,0.5),14,0,0,4);"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(to right, #4F46E5, #7C3AED);" +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;" +
                        "-fx-cursor: hand; -fx-padding: 9 22; -fx-background-radius: 22;" +
                        "-fx-effect: dropshadow(gaussian,rgba(79,70,229,0.35),10,0,0,3);"));

        btn.setOnAction(e -> handleEnroll(formationId, sessionId, connection, btn, parentRow));
        return btn;
    }

    private static void handleEnroll(int formationId, int sessionId, Connection connection,
                                     Button btn, HBox parentRow) {
        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation d'inscription");
        confirm.setHeaderText("S'inscrire à cette formation ?");
        confirm.setContentText("Vous allez vous inscrire à cette session de formation.\nVous pourrez accéder au contenu immédiatement.");

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            try {
                // Check already enrolled (race condition guard)
                PreparedStatement check = connection.prepareStatement(
                        "SELECT id FROM inscriptionFormation WHERE session_id=? AND employe_id=?");
                check.setInt(1, sessionId); check.setInt(2, EMPLOYE_ID);
                if (check.executeQuery().next()) {
                    showInfo("Déjà inscrit", "Vous êtes déjà inscrit à cette formation !");
                    return;
                }

                // Insert inscription
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO inscriptionFormation (dateInscription, statut, progression, session_id, employe_id) " +
                                "VALUES (?, 'In Progress', 0, ?, ?)");
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                ps.setInt(2, sessionId);
                ps.setInt(3, EMPLOYE_ID);
                ps.executeUpdate();

                // Update button to show enrolled
                btn.setText("✅ Inscrit");
                btn.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;" +
                        "-fx-font-weight: bold; -fx-font-size: 12;" +
                        "-fx-padding: 9 22; -fx-background-radius: 22;" +
                        "-fx-border-color: #6EE7B7; -fx-border-width: 1; -fx-border-radius: 22;");
                btn.setDisable(true);

                showInfo("Inscription réussie ! 🎉",
                        "Vous êtes maintenant inscrit à cette formation.\nRetrouvez-la dans « My Learning ».");

            } catch (SQLException ex) {
                showError("Erreur d'inscription", ex.getMessage());
            }
        });
    }

    // ── HELPERS ────────────────────────────────────────────────────────────────
    private static EnrollStatus checkEnrollStatus(int formationId, Connection connection) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT i.statut FROM inscriptionFormation i " +
                            "JOIN sessionFormation s ON i.session_id = s.id " +
                            "WHERE s.formation_id = ? AND i.employe_id = ? LIMIT 1");
            ps.setInt(1, formationId); ps.setInt(2, EMPLOYE_ID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String statut = rs.getString("statut");
                return "Completed".equals(statut) ? EnrollStatus.ENROLLED_COMPLETED
                        : EnrollStatus.ENROLLED_IN_PROGRESS;
            }
        } catch (SQLException ignored) {}
        return EnrollStatus.NOT_ENROLLED;
    }

    private static int findOpenSession(int formationId, Connection connection) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT id FROM sessionFormation " +
                            "WHERE formation_id = ? " +
                            "AND statut IN ('Open','Active','Planned') " +
                            "AND dateFin >= CURDATE() " +
                            "ORDER BY dateDebut ASC LIMIT 1");
            ps.setInt(1, formationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException ignored) {}
        return -1;
    }

    private static void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    private static void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }

    public enum EnrollStatus { NOT_ENROLLED, ENROLLED_IN_PROGRESS, ENROLLED_COMPLETED }
}