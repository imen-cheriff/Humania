package competence.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import utils.MyDataBase;
import utils.UserSession;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DevelopmentPlansController {

    @FXML private Label  lblActivePlans;
    @FXML private Label  lblAvgProgress;
    @FXML private Label  lblActionsDue;
    @FXML private Label  lblCompleted;
    @FXML private Button btnCreatePDI;
    @FXML private VBox   vboxPDIList;
    @FXML private VBox   vboxEmptyPDI;

    private Connection connection;
    private ObservableList<PDIItem> pdiData;

    // ── Drag & Drop ───────────────────────────────────────────────────────────
    private static ActionItem draggedAction  = null;
    private static PDIItem    draggedFromPDI = null;

    // ── Couleurs Kanban ────────────────────────────────────────────────────────
    private static final String COL_TODO_COLOR  = "#6B7280";
    private static final String COL_TODO_BG     = "#F9FAFB";
    private static final String COL_TODO_BORDER = "#E5E7EB";
    private static final String COL_PROG_COLOR  = "#F59E0B";
    private static final String COL_PROG_BG     = "#FFFBEB";
    private static final String COL_PROG_BORDER = "#FDE68A";
    private static final String COL_DONE_COLOR  = "#10B981";
    private static final String COL_DONE_BG     = "#F0FDF4";
    private static final String COL_DONE_BORDER = "#BBF7D0";

    private int currentUserId = -1;

    private boolean isRH() {
        String r = UserSession.getInstance().getRole();
        return r != null && (r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("RH"));
    }
    private boolean isManager() {
        String r = UserSession.getInstance().getRole();
        return r != null && r.equalsIgnoreCase("MANAGER");
    }
    private int resolveCurrentUserId() {
        String username = UserSession.getInstance().getUser();
        String email    = UserSession.getInstance().getEmail();
        try {
            if (username != null && !username.isBlank()) {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT id FROM utilisateur WHERE username = ? LIMIT 1");
                ps.setString(1, username); ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            if (email != null && !email.isBlank()) {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT id FROM utilisateur WHERE email = ? LIMIT 1");
                ps.setString(1, email); ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException ignored) {}
        return 1;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        connection    = MyDataBase.getInstance().getCnx();
        pdiData       = FXCollections.observableArrayList();
        currentUserId = resolveCurrentUserId();
        // Sync auto au chargement : marquer les formations 100% comme Completed
        syncAllFormationActions();
        loadStatistics();
        loadPDIList();
    }

    public void refreshIfVisible() {
        syncAllFormationActions();
        loadStatistics();
        loadPDIList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SYNC FORMATION → PDI ACTION
    //  Quand tous les modules d'une formation sont terminés,
    //  l'action PDI correspondante passe à "Completed" et la progression s'actualise
    // ══════════════════════════════════════════════════════════════════════════
    private void syncAllFormationActions() {
        try {
            // Trouver toutes les actions PDI de type "Formation : ..." qui ne sont pas Completed
            PreparedStatement psActions = connection.prepareStatement(
                    "SELECT a.id, a.typeAction, a.pdi_id, p.employe_id " +
                            "FROM actionPDI a " +
                            "JOIN pdi p ON a.pdi_id = p.id " +
                            "WHERE a.typeAction LIKE 'Formation : %' " +
                            "AND a.statut != 'Completed'"
            );
            ResultSet rsActions = psActions.executeQuery();
            List<int[]> toComplete = new ArrayList<>(); // [actionId, pdiId]

            while (rsActions.next()) {
                int    actionId  = rsActions.getInt("id");
                int    pdiId     = rsActions.getInt("pdi_id");
                int    employeId = rsActions.getInt("employe_id");
                String label     = rsActions.getString("typeAction");
                // "Formation : Titre" → extraire le titre
                String titre = label.startsWith("Formation : ")
                        ? label.substring("Formation : ".length()).trim() : label;

                // Vérifier si tous les modules sont complétés
                if (isFormationFullyCompleted(employeId, titre)) {
                    toComplete.add(new int[]{actionId, pdiId});
                }
            }

            // Marquer comme Completed et recalculer progression PDI
            for (int[] pair : toComplete) {
                int actionId = pair[0], pdiId = pair[1];
                PreparedStatement upd = connection.prepareStatement(
                        "UPDATE actionPDI SET statut='Completed' WHERE id=?"
                );
                upd.setInt(1, actionId);
                upd.executeUpdate();
                recalcPDIProgression(pdiId);
            }
        } catch (SQLException e) {
            System.err.println("[DevelopmentPlans] syncAllFormationActions: " + e.getMessage());
        }
    }

    /**
     * Vérifie si tous les modules d'une formation (par titre) sont complétés
     * par cet employé. Retourne true seulement si total > 0 et done == total.
     */
    private boolean isFormationFullyCompleted(int employeId, String formationTitre) {
        try {
            // Chercher la formation par titre
            PreparedStatement psF = connection.prepareStatement(
                    "SELECT f.id FROM formation f WHERE f.titre = ? LIMIT 1"
            );
            psF.setString(1, formationTitre);
            ResultSet rsF = psF.executeQuery();
            if (!rsF.next()) {
                // Essayer avec LIKE (cas de troncature)
                psF = connection.prepareStatement(
                        "SELECT f.id FROM formation f WHERE f.titre LIKE ? LIMIT 1"
                );
                psF.setString(1, formationTitre + "%");
                rsF = psF.executeQuery();
                if (!rsF.next()) return false;
            }
            int formationId = rsF.getInt("id");

            PreparedStatement psM = connection.prepareStatement(
                    "SELECT COUNT(m.id) AS total, " +
                            "SUM(CASE WHEN mp.statut='completed' THEN 1 ELSE 0 END) AS done " +
                            "FROM module m " +
                            "LEFT JOIN module_progression mp ON mp.module_id = m.id AND mp.employe_id = ? " +
                            "WHERE m.formation_id = ?"
            );
            psM.setInt(1, employeId);
            psM.setInt(2, formationId);
            ResultSet rsM = psM.executeQuery();
            if (rsM.next()) {
                int total = rsM.getInt("total");
                int done  = rsM.getInt("done");
                return total > 0 && done >= total;
            }
        } catch (SQLException e) {
            System.err.println("[DevelopmentPlans] isFormationFullyCompleted: " + e.getMessage());
        }
        return false;
    }

    /**
     * Recalcule progressionGlobale d'un PDI basé sur le % d'actions Completed.
     * Met aussi à jour le statut (Active / Completed).
     */
    private void recalcPDIProgression(int pdiId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) AS total, " +
                            "SUM(CASE WHEN statut='Completed' THEN 1 ELSE 0 END) AS done " +
                            "FROM actionPDI WHERE pdi_id=?"
            );
            ps.setInt(1, pdiId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;
            int total = rs.getInt("total");
            int done  = rs.getInt("done");
            int pct   = total == 0 ? 0 : done * 100 / total;
            String newStatut = pct >= 100 ? "Completed" : "Active";

            PreparedStatement upd = connection.prepareStatement(
                    "UPDATE pdi SET progressionGlobale=?, statut=? WHERE id=?"
            );
            upd.setInt(1, pct); upd.setString(2, newStatut); upd.setInt(3, pdiId);
            upd.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DevelopmentPlans] recalcPDIProgression: " + e.getMessage());
        }
    }

    // Méthode statique utilisée par ModulesViewController
    public static void recalcPDIStatic(Connection cnx, int pdiId) {
        try {
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT COUNT(*) AS total, " +
                            "SUM(CASE WHEN statut='Completed' THEN 1 ELSE 0 END) AS done " +
                            "FROM actionPDI WHERE pdi_id=?"
            );
            ps.setInt(1, pdiId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;
            int total = rs.getInt("total"), done = rs.getInt("done");
            int pct = total == 0 ? 0 : done * 100 / total;
            PreparedStatement upd = cnx.prepareStatement(
                    "UPDATE pdi SET progressionGlobale=?, statut=? WHERE id=?"
            );
            upd.setInt(1, pct); upd.setString(2, pct >= 100 ? "Completed" : "Active");
            upd.setInt(3, pdiId); upd.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════════════════════════════════
    private void loadStatistics() {
        try {
            if (lblActivePlans != null)
                lblActivePlans.setText(String.valueOf(
                        queryInt("SELECT COUNT(*) FROM pdi WHERE statut='Active'")));

            // Progression moyenne calculée dynamiquement
            int avg = computeAvgDynamicProgress();
            if (lblAvgProgress != null) lblAvgProgress.setText(avg + "%");

            if (lblActionsDue != null)
                lblActionsDue.setText(String.valueOf(queryInt(
                        "SELECT COUNT(*) FROM actionPDI " +
                                "WHERE statut IN ('Pending','In Progress') " +
                                "AND dateFinPrevue <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)")));

            if (lblCompleted != null)
                lblCompleted.setText(String.valueOf(
                        queryInt("SELECT COUNT(*) FROM actionPDI WHERE statut='Completed'")));

        } catch (SQLException e) { showError("Stats", e.getMessage()); }
    }

    private int computeAvgDynamicProgress() throws SQLException {
        ResultSet rs = connection.prepareStatement(
                "SELECT p.id, COUNT(a.id) AS total, " +
                        "SUM(CASE WHEN a.statut='Completed' THEN 1 ELSE 0 END) AS done " +
                        "FROM pdi p LEFT JOIN actionPDI a ON a.pdi_id=p.id " +
                        "WHERE p.statut='Active' GROUP BY p.id").executeQuery();
        int sum = 0, count = 0;
        while (rs.next()) {
            int total = rs.getInt("total");
            if (total > 0) sum += rs.getInt("done") * 100 / total;
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    private int queryInt(String sql) throws SQLException {
        ResultSet rs = connection.prepareStatement(sql).executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CHARGEMENT PDI
    // ══════════════════════════════════════════════════════════════════════════
    private void loadPDIList() {
        if (vboxPDIList == null) return;
        vboxPDIList.getChildren().clear();
        pdiData.clear();

        try {
            PreparedStatement ps;
            if (isRH()) {
                ps = connection.prepareStatement(
                        "SELECT p.id, p.annee, p.progressionGlobale, p.dateCreation, p.statut, " +
                                "IFNULL(u.fullName, IFNULL(u.username, 'Employé')) AS employe_nom " +
                                "FROM pdi p LEFT JOIN utilisateur u ON p.employe_id = u.id " +
                                "ORDER BY p.annee DESC, p.dateCreation DESC");
            } else if (isManager() && currentUserId > 0) {
                ps = connection.prepareStatement(
                        "SELECT p.id, p.annee, p.progressionGlobale, p.dateCreation, p.statut, " +
                                "IFNULL(u.fullName, IFNULL(u.username, 'Employé')) AS employe_nom " +
                                "FROM pdi p LEFT JOIN utilisateur u ON p.employe_id = u.id " +
                                "WHERE p.employe_id = ? OR u.manager_id = ? " +
                                "ORDER BY p.annee DESC, p.dateCreation DESC");
                ps.setInt(1, currentUserId); ps.setInt(2, currentUserId);
            } else {
                ps = connection.prepareStatement(
                        "SELECT p.id, p.annee, p.progressionGlobale, p.dateCreation, p.statut, " +
                                "IFNULL(u.fullName, IFNULL(u.username, 'Moi')) AS employe_nom " +
                                "FROM pdi p LEFT JOIN utilisateur u ON p.employe_id = u.id " +
                                "WHERE p.employe_id = ? ORDER BY p.annee DESC, p.dateCreation DESC");
                ps.setInt(1, currentUserId > 0 ? currentUserId : -1);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date dc = rs.getDate("dateCreation");
                PDIItem pdi = new PDIItem(
                        rs.getInt("id"), rs.getInt("annee"),
                        rs.getInt("progressionGlobale"),
                        dc != null ? dc.toLocalDate() : LocalDate.now(),
                        rs.getString("statut"), rs.getString("employe_nom"));
                loadActionsForPDI(pdi);
                // Recalculer progression dynamiquement
                int dynPct = pdi.computeDynamicProgress();
                if (dynPct != rs.getInt("progressionGlobale")) {
                    recalcPDIProgression(pdi.getId());
                    pdi.setProgressionGlobale(dynPct);
                }
                pdiData.add(pdi);
                vboxPDIList.getChildren().add(createPDICard(pdi));
            }
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }

        boolean empty = pdiData.isEmpty();
        if (vboxEmptyPDI != null) { vboxEmptyPDI.setVisible(empty); vboxEmptyPDI.setManaged(empty); }
    }

    private void loadActionsForPDI(PDIItem pdi) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, typeAction, statut, dateDebut, dateFinPrevue, priorite, " +
                            "IFNULL(formation_id, 0) AS formation_id " +
                            "FROM actionPDI WHERE pdi_id=? ORDER BY priorite DESC, dateFinPrevue ASC");
            ps.setInt(1, pdi.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date dd = rs.getDate("dateDebut"), df = rs.getDate("dateFinPrevue");
                int fid = 0;
                try { fid = rs.getInt("formation_id"); } catch (SQLException ignored2) {}
                pdi.getActions().add(new ActionItem(
                        rs.getInt("id"), rs.getString("typeAction"), rs.getString("statut"),
                        dd != null ? dd.toLocalDate() : null,
                        df != null ? df.toLocalDate() : null,
                        rs.getInt("priorite"), pdi.getId(), fid));
            }
        } catch (SQLException e) { showError("Actions", e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARTE PDI
    // ══════════════════════════════════════════════════════════════════════════
    private VBox createPDICard(PDIItem pdi) {
        int pct = pdi.getProgressionGlobale();
        String[] colors = getStatusColors(pdi.getStatut());

        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.09),14,0,0,3);"
        );

        // ── Banner ─────────────────────────────────────────────────────────
        HBox banner = new HBox(16);
        banner.setPadding(new Insets(18, 22, 18, 22));
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle(
                "-fx-background-color:linear-gradient(to right," + colors[0] + "," + colors[1] + ");" +
                        "-fx-background-radius:16 16 0 0;"
        );

        StackPane avatar = new StackPane();
        avatar.setPrefSize(50, 50);
        Circle aBg = new Circle(25, Color.web("rgba(255,255,255,0.25)"));
        String initials = pdi.getEmployeNom() != null && !pdi.getEmployeNom().isBlank()
                ? String.valueOf(pdi.getEmployeNom().charAt(0)).toUpperCase() : "E";
        Label aLbl = new Label(initials);
        aLbl.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:white;");
        avatar.getChildren().addAll(aBg, aLbl);

        VBox bannerInfo = new VBox(4); HBox.setHgrow(bannerInfo, Priority.ALWAYS);
        Label empLbl  = new Label(pdi.getEmployeNom());
        empLbl.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:white;");
        Label metaLbl = new Label("PDI " + pdi.getAnnee() + "  ·  Créé le " + pdi.getDateCreation());
        metaLbl.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.75);");
        bannerInfo.getChildren().addAll(empLbl, metaLbl);

        Label statusBadge = new Label(statusIcon(pdi.getStatut()) + "  " + pdi.getStatut().toUpperCase());
        statusBadge.setStyle(
                "-fx-background-color:rgba(255,255,255,0.25);-fx-text-fill:white;" +
                        "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:5 14;-fx-background-radius:20;"
        );

        StackPane circProg = buildCircularProgress(pct);
        banner.getChildren().addAll(avatar, bannerInfo, statusBadge, circProg);

        // ── Progress bar mince sous le banner ──────────────────────────────
        StackPane thinBar = new StackPane();
        thinBar.setPrefHeight(6); thinBar.setMaxWidth(Double.MAX_VALUE);
        Region barTrack = new Region();
        barTrack.setPrefHeight(6); barTrack.setMaxWidth(Double.MAX_VALUE);
        barTrack.setStyle("-fx-background-color:#E5E7EB;");
        Region barFill = new Region();
        barFill.setPrefHeight(6);
        barFill.setStyle("-fx-background-color:" + getProgressColor(pct) + ";");
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        thinBar.widthProperty().addListener((obs, o, nw) ->
                barFill.setPrefWidth(nw.doubleValue() * pct / 100.0));
        thinBar.getChildren().addAll(barTrack, barFill);

        // ── Body ───────────────────────────────────────────────────────────
        HBox body = new HBox(20);
        body.setPadding(new Insets(20, 22, 20, 22));

        // Colonne gauche : objectifs + compétences + boutons
        VBox leftCol = new VBox(14);
        leftCol.setPrefWidth(280); leftCol.setMinWidth(240);

        Label objTitle = new Label("🎯  Objectifs");
        objTitle.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        VBox objBox = new VBox(6);
        for (ActionItem a : pdi.getActions()) {
            boolean done  = "Completed".equals(a.getStatut());
            boolean inPr  = "In Progress".equals(a.getStatut());
            String ic  = done ? "✅  " : inPr ? "🔄  " : "🎯  ";
            String fg  = done ? "#059669" : inPr ? "#D97706" : "#374151";
            String bg  = done ? "#F0FDF4" : inPr ? "#FFFBEB" : "#F9FAFB";
            Label obj = new Label(ic + a.getTypeAction());
            obj.setStyle("-fx-font-size:12;-fx-text-fill:" + fg + ";-fx-background-color:" + bg +
                    ";-fx-padding:6 12;-fx-background-radius:8;");
            obj.setMaxWidth(Double.MAX_VALUE);
            if (a.getTypeAction() != null && a.getTypeAction().startsWith("Formation :"))
                obj.setStyle(obj.getStyle() + "-fx-border-color:#E0E7FF;-fx-border-width:0 0 0 3;-fx-border-radius:0;");
            objBox.getChildren().add(obj);
        }
        if (pdi.getActions().isEmpty()) {
            Label noObj = new Label("Aucun objectif. Inscrivez-vous à une formation !");
            noObj.setStyle("-fx-font-size:12;-fx-text-fill:#9CA3AF;");
            objBox.getChildren().add(noObj);
        }

        Label compTitle = new Label("🏆  Compétences cibles");
        compTitle.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        FlowPane compTags = new FlowPane(8, 6);
        Set<String> seen = new LinkedHashSet<>();
        for (ActionItem a : pdi.getActions()) if (a.getTypeAction() != null) seen.add(a.getTypeAction());
        for (String t : seen) compTags.getChildren().add(makeCompTag(t));

        HBox btns = new HBox(10);
        Button btnEdit = new Button("✏  Edit PDI");
        btnEdit.setStyle(
                "-fx-background-color:#FFF7ED;-fx-text-fill:#EA580C;" +
                        "-fx-font-size:12;-fx-padding:9 18;-fx-background-radius:10;" +
                        "-fx-border-color:#FDBA74;-fx-border-radius:10;-fx-cursor:hand;"
        );
        btnEdit.setOnAction(e -> handleEditPDI(pdi));

        Button btnDel = new Button("🗑  Supprimer");
        btnDel.setStyle(
                "-fx-background-color:#FEF2F2;-fx-text-fill:#DC2626;" +
                        "-fx-font-size:12;-fx-padding:9 18;-fx-background-radius:10;" +
                        "-fx-border-color:#FECACA;-fx-border-radius:10;-fx-cursor:hand;"
        );
        btnDel.setOnAction(e -> handleDeletePDI(pdi));

        boolean ownPdi    = currentUserId > 0 && currentUserId == resolvePdiOwnerId(pdi.getId());
        boolean canEdit   = isRH() || isManager() || ownPdi;
        btnEdit.setVisible(canEdit); btnEdit.setManaged(canEdit);
        btnDel.setVisible(isRH());   btnDel.setManaged(isRH());
        btns.getChildren().addAll(btnEdit, btnDel);

        leftCol.getChildren().addAll(objTitle, objBox, compTitle, compTags, btns);

        // Colonne droite : Kanban intégré
        VBox rightCol = new VBox(12); HBox.setHgrow(rightCol, Priority.ALWAYS);

        // Header Kanban
        HBox kanbanHeader = new HBox(10);
        kanbanHeader.setAlignment(Pos.CENTER_LEFT);
        Label kanbanTitle = new Label("⚡  Development Actions");
        kanbanTitle.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        Region ks = new Region(); HBox.setHgrow(ks, Priority.ALWAYS);

        // Stats rapides
        long todoCount = pdi.getActions().stream().filter(a -> "Pending".equals(a.getStatut())).count();
        long progCount = pdi.getActions().stream().filter(a -> "In Progress".equals(a.getStatut())).count();
        long doneCount = pdi.getActions().stream().filter(a -> "Completed".equals(a.getStatut())).count();
        Label statsTodo = makeKanbanStat(String.valueOf(todoCount), COL_TODO_COLOR);
        Label statsProg = makeKanbanStat(String.valueOf(progCount), COL_PROG_COLOR);
        Label statsDone = makeKanbanStat(String.valueOf(doneCount), COL_DONE_COLOR);

        Button btnAddAction = new Button("＋  Ajouter");
        btnAddAction.setStyle(
                "-fx-background-color:#4F46E5;-fx-text-fill:white;" +
                        "-fx-font-size:11;-fx-padding:7 14;-fx-background-radius:8;-fx-cursor:hand;"
        );
        btnAddAction.setOnAction(e -> handleAddAction(pdi));
        boolean canAdd = isRH() || isManager() || ownPdi;
        btnAddAction.setVisible(canAdd); btnAddAction.setManaged(canAdd);

        kanbanHeader.getChildren().addAll(kanbanTitle, ks, statsTodo, statsProg, statsDone, btnAddAction);

        // Colonnes Kanban
        List<ActionItem> todo   = new ArrayList<>();
        List<ActionItem> inProg = new ArrayList<>();
        List<ActionItem> done   = new ArrayList<>();
        for (ActionItem a : pdi.getActions()) {
            if ("Completed".equals(a.getStatut()))        done.add(a);
            else if ("In Progress".equals(a.getStatut())) inProg.add(a);
            else                                           todo.add(a);
        }

        HBox kanban = new HBox(12);
        kanban.getChildren().addAll(
                makeKanbanCol("📌  À faire",  COL_TODO_COLOR,  COL_TODO_BG,  COL_TODO_BORDER,  "Pending",     todo,   pdi),
                makeKanbanCol("🔄  En cours", COL_PROG_COLOR,  COL_PROG_BG,  COL_PROG_BORDER,  "In Progress", inProg, pdi),
                makeKanbanCol("✅  Terminé",  COL_DONE_COLOR,  COL_DONE_BG,  COL_DONE_BORDER,  "Completed",   done,   pdi)
        );
        rightCol.getChildren().addAll(kanbanHeader, kanban);

        body.getChildren().addAll(leftCol, rightCol);
        card.getChildren().addAll(banner, thinBar, body);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  KANBAN COLONNE — style photo 1
    // ══════════════════════════════════════════════════════════════════════════
    private VBox makeKanbanCol(String title, String color, String bgColor,
                               String borderColor, String targetStatut,
                               List<ActionItem> actions, PDIItem pdi) {
        VBox col = new VBox(0);
        HBox.setHgrow(col, Priority.ALWAYS);
        col.setMinWidth(200);
        col.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + borderColor + ";" +
                        "-fx-border-radius:14;-fx-border-width:1;"
        );

        // En-tête de colonne
        HBox colHeader = new HBox(8);
        colHeader.setAlignment(Pos.CENTER_LEFT);
        colHeader.setPadding(new Insets(10, 14, 10, 14));
        colHeader.setStyle(
                "-fx-background-color:" + color + "18;" +
                        "-fx-background-radius:14 14 0 0;" +
                        "-fx-border-color:transparent transparent " + borderColor + " transparent;" +
                        "-fx-border-width:0 0 1 0;"
        );
        Label colTitleLbl = new Label(title);
        colTitleLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label countBadge = new Label(String.valueOf(actions.size()));
        countBadge.setStyle(
                "-fx-background-color:" + color + ";" +
                        "-fx-text-fill:white;-fx-font-size:10;-fx-font-weight:bold;" +
                        "-fx-padding:2 7;-fx-background-radius:10;"
        );
        colHeader.getChildren().addAll(colTitleLbl, countBadge);

        // Cartes
        VBox cardsBox = new VBox(10);
        cardsBox.setPadding(new Insets(10, 10, 10, 10));
        cardsBox.setMinHeight(80);
        for (ActionItem a : actions) cardsBox.getChildren().add(makeActionCard(a, pdi, color, borderColor));

        if (actions.isEmpty()) {
            Label empty = new Label("—");
            empty.setStyle("-fx-text-fill:#D1D5DB;-fx-font-size:20;");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(14, 0, 8, 0));
            cardsBox.getChildren().add(empty);
        }

        col.getChildren().addAll(colHeader, cardsBox);

        // ── Drag & Drop ────────────────────────────────────────────────────
        col.setOnDragOver(event -> {
            if (draggedAction != null && !targetStatut.equals(draggedAction.getStatut())) {
                event.acceptTransferModes(TransferMode.MOVE);
                col.setStyle(
                        "-fx-background-color:" + color + "22;" +
                                "-fx-background-radius:14;" +
                                "-fx-border-color:" + color + ";" +
                                "-fx-border-radius:14;-fx-border-width:2;"
                );
            }
            event.consume();
        });
        col.setOnDragExited(event -> {
            col.setStyle(
                    "-fx-background-color:" + bgColor + ";" +
                            "-fx-background-radius:14;" +
                            "-fx-border-color:" + borderColor + ";" +
                            "-fx-border-radius:14;-fx-border-width:1;"
            );
            event.consume();
        });
        col.setOnDragDropped(event -> {
            boolean success = false;
            if (draggedAction != null && !targetStatut.equals(draggedAction.getStatut())) {
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "UPDATE actionPDI SET statut=? WHERE id=?");
                    ps.setString(1, targetStatut); ps.setInt(2, draggedAction.getId());
                    ps.executeUpdate();
                    // Recalculer progression PDI immédiatement
                    recalcPDIProgression(draggedAction.getPdiId());
                    success = true;
                } catch (SQLException e) { showError("Drag & Drop", e.getMessage()); }
            }
            event.setDropCompleted(success);
            event.consume();
            if (success) {
                draggedAction = null; draggedFromPDI = null;
                syncAllFormationActions();
                loadStatistics();
                loadPDIList();
            }
        });

        return col;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARTE ACTION — style photo 1
    // ══════════════════════════════════════════════════════════════════════════
    private VBox makeActionCard(ActionItem a, PDIItem pdi, String colColor, String colBorder) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(11, 13, 11, 13));
        boolean isFormation = a.getTypeAction() != null && a.getTypeAction().startsWith("Formation :");
        boolean isOverdue   = a.getDateFinPrevue() != null
                && LocalDate.now().isAfter(a.getDateFinPrevue())
                && !"Completed".equals(a.getStatut());

        String leftBorder = isFormation
                ? "-fx-border-color:#4F46E5;-fx-border-width:0 0 0 4;-fx-border-radius:0 10 10 0;"
                : isOverdue
                ? "-fx-border-color:#EF4444;-fx-border-width:0 0 0 4;-fx-border-radius:0 10 10 0;"
                : "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:10;";

        String baseStyle =
                "-fx-background-color:white;" +
                        "-fx-background-radius:10;" +
                        leftBorder +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);" +
                        "-fx-cursor:open-hand;";
        card.setStyle(baseStyle);

        // ── Titre ──────────────────────────────────────────────────────────
        HBox titleRow = new HBox(7);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label typeIco = new Label(getActionEmoji(a.getTypeAction()));
        typeIco.setStyle("-fx-font-size:13;");
        Label typeLbl = new Label(a.getTypeAction() != null
                ? (a.getTypeAction().length() > 26 ? a.getTypeAction().substring(0, 24) + "…" : a.getTypeAction())
                : "—");
        typeLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
        typeLbl.setWrapText(true); HBox.setHgrow(typeLbl, Priority.ALWAYS);
        titleRow.getChildren().addAll(typeIco, typeLbl);
        card.getChildren().add(titleRow);

        // ── Badges ─────────────────────────────────────────────────────────
        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        if (isOverdue) {
            Label ob = makeBadge("⚠ En retard", "#DC2626", "#FEF2F2");
            badges.getChildren().add(ob);
        }
        if (isFormation) {
            Label fb = makeBadge("🔗 Formation", "#4F46E5", "#EEF2FF");
            badges.getChildren().add(fb);
        }
        if (a.getPriorite() >= 4) {
            Label pb = makeBadge("🔥 Priorité " + a.getPriorite(), "#D97706", "#FFFBEB");
            badges.getChildren().add(pb);
        }
        if (!badges.getChildren().isEmpty()) card.getChildren().add(badges);

        // ── PDI info ────────────────────────────────────────────────────────
        Label pdiLbl = new Label("PDI " + pdi.getAnnee() + " · " + pdi.getEmployeNom());
        pdiLbl.setStyle("-fx-font-size:10;-fx-text-fill:#94A3B8;");
        card.getChildren().add(pdiLbl);

        // ── Date ────────────────────────────────────────────────────────────
        if (a.getDateFinPrevue() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), a.getDateFinPrevue());
            String dateColor = daysLeft < 0 ? "#DC2626" : daysLeft <= 7 ? "#F59E0B" : "#64748B";
            String dateText = daysLeft < 0
                    ? "⏰ " + Math.abs(daysLeft) + "j de retard"
                    : daysLeft == 0 ? "⏰ Aujourd'hui !"
                    : daysLeft <= 7 ? "📅 Dans " + daysLeft + "j"
                    : "📅 " + a.getDateFinPrevue().toString().substring(0, 7) + "/" + a.getDateFinPrevue().getDayOfMonth();
            Label dateLbl = new Label(dateText);
            dateLbl.setStyle("-fx-font-size:10;-fx-text-fill:" + dateColor + ";" +
                    (daysLeft <= 0 ? "-fx-font-weight:bold;" : ""));
            card.getChildren().add(dateLbl);
        }

        // ── Priorité étoiles ────────────────────────────────────────────────
        HBox prioRow = new HBox(2);
        prioRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 5; i++) {
            Label dot = new Label("★");
            dot.setStyle("-fx-font-size:9;-fx-text-fill:" + (i <= a.getPriorite() ? "#F59E0B" : "#E2E8F0") + ";");
            prioRow.getChildren().add(dot);
        }
        Label prioTxt = new Label("  Priorité " + a.getPriorite() + "/5");
        prioTxt.setStyle("-fx-font-size:9;-fx-text-fill:#94A3B8;");
        prioRow.getChildren().add(prioTxt);
        card.getChildren().add(prioRow);

        // ── Hint glisser ────────────────────────────────────────────────────
        Label dragHint = new Label("⠿ glisser pour changer le statut");
        dragHint.setStyle("-fx-font-size:9;-fx-text-fill:#C4B5FD;");
        card.getChildren().add(dragHint);

        // ── Boutons éditer / supprimer ───────────────────────────────────────
        HBox actRow = new HBox(6);
        actRow.setAlignment(Pos.CENTER_RIGHT);
        Button be = new Button("✏");
        be.setStyle("-fx-background-color:#FFF7ED;-fx-text-fill:#EA580C;" +
                "-fx-font-size:11;-fx-padding:4 8;-fx-background-radius:6;-fx-cursor:hand;");
        be.setOnAction(e -> handleEditAction(a, pdi));
        Button bd = new Button("🗑");
        bd.setStyle("-fx-background-color:#FEF2F2;-fx-text-fill:#DC2626;" +
                "-fx-font-size:11;-fx-padding:4 8;-fx-background-radius:6;-fx-cursor:hand;");
        bd.setOnAction(e -> handleDeleteAction(a, pdi));
        actRow.getChildren().addAll(be, bd);
        card.getChildren().add(actRow);

        // ── Drag source ─────────────────────────────────────────────────────
        final String finalLeftBorder = leftBorder;
        card.setOnDragDetected(event -> {
            draggedAction  = a;
            draggedFromPDI = pdi;
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(a.getId()));
            db.setContent(content);
            card.setStyle("-fx-background-color:#F8F9FF;-fx-background-radius:10;" +
                    finalLeftBorder + "-fx-opacity:0.55;-fx-cursor:closed-hand;");
            event.consume();
        });
        card.setOnDragDone(event -> {
            card.setStyle(baseStyle);
            event.consume();
        });
        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FAFBFF;-fx-background-radius:10;" + finalLeftBorder +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.12),12,0,0,3);" +
                        "-fx-cursor:open-hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ══════════════════════════════════════════════════════════════════════════
    private Label makeKanbanStat(String value, String color) {
        Label lbl = new Label(value);
        lbl.setStyle(
                "-fx-background-color:" + color + "18;" +
                        "-fx-text-fill:" + color + ";" +
                        "-fx-font-size:11;-fx-font-weight:bold;" +
                        "-fx-padding:3 9;-fx-background-radius:10;" +
                        "-fx-border-color:" + color + "33;-fx-border-radius:10;-fx-border-width:1;"
        );
        return lbl;
    }

    private Label makeBadge(String text, String fg, String bg) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:9;-fx-font-weight:bold;-fx-padding:2 7;-fx-background-radius:10;");
        return lbl;
    }

    private Label makeCompTag(String text) {
        String[] colors = {"#4F46E5","#10B981","#F59E0B","#EC4899","#0EA5E9","#8B5CF6"};
        String c = colors[Math.abs(text.hashCode()) % colors.length];
        Label lbl = new Label(text.length() > 22 ? text.substring(0, 20) + "…" : text);
        lbl.setStyle("-fx-background-color:" + c + "20;-fx-text-fill:" + c + ";" +
                "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:5 12;-fx-background-radius:20;");
        return lbl;
    }

    private StackPane buildCircularProgress(int pct) {
        final int SIZE = 60, STROKE = 5;
        Canvas canvas = new Canvas(SIZE, SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.web("rgba(255,255,255,0.25)"));
        gc.setLineWidth(STROKE);
        gc.strokeOval(STROKE/2.0, STROKE/2.0, SIZE-STROKE, SIZE-STROKE);
        if (pct > 0) {
            gc.setStroke(Color.WHITE); gc.setLineWidth(STROKE);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeArc(STROKE/2.0, STROKE/2.0, SIZE-STROKE, SIZE-STROKE, -90, -(pct*3.6), ArcType.OPEN);
        }
        Label pctLbl = new Label(pct + "%");
        pctLbl.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:white;");
        StackPane sp = new StackPane(canvas, pctLbl);
        sp.setPrefSize(SIZE, SIZE); sp.setMinSize(SIZE, SIZE); sp.setMaxSize(SIZE, SIZE);
        StackPane.setAlignment(pctLbl, Pos.CENTER);
        return sp;
    }

    private String statusIcon(String s) {
        if (s == null) return "•";
        return switch (s) {
            case "Active"    -> "🟢";
            case "Completed" -> "✅";
            case "Draft"     -> "📝";
            default          -> "📦";
        };
    }

    private String[] getStatusColors(String s) {
        if (s == null) return new String[]{"#6B7280","#9CA3AF"};
        return switch (s) {
            case "Active"    -> new String[]{"#2C3E8C","#4A6CF7"};
            case "Completed" -> new String[]{"#059669","#10B981"};
            case "Draft"     -> new String[]{"#6B7280","#9CA3AF"};
            default          -> new String[]{"#92400E","#D97706"};
        };
    }

    private String getProgressColor(int pct) {
        if (pct >= 80) return "#059669";
        if (pct >= 50) return "#2563EB";
        if (pct >= 30) return "#F59E0B";
        return "#EF4444";
    }

    private String getActionEmoji(String type) {
        if (type == null) return "📋";
        String t = type.toLowerCase();
        if (t.startsWith("formation"))              return "📚";
        if (t.contains("certif"))                   return "🏆";
        if (t.contains("projet") || t.contains("project")) return "🏗";
        if (t.contains("mentor"))                   return "🤝";
        if (t.contains("coaching"))                 return "💡";
        return "📋";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD PDI
    // ══════════════════════════════════════════════════════════════════════════
    private int resolvePdiOwnerId(int pdiId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT employe_id FROM pdi WHERE id = ? LIMIT 1");
            ps.setInt(1, pdiId); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("employe_id");
        } catch (SQLException ignored) {}
        return -1;
    }

    @FXML
    private void handleCreatePDI() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Créer un PDI");
        ButtonType ok = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        Spinner<Integer> spAnnee = new Spinner<>(2020, 2035, LocalDate.now().getYear());
        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("Active","Draft","Completed","Archived"); cbStatut.setValue("Draft");
        g.add(new Label("Année :"), 0, 0); g.add(spAnnee, 1, 0);
        g.add(new Label("Statut :"), 0, 1); g.add(cbStatut, 1, 1);

        ComboBox<EmployeeItem> cbEmployee = null;
        if (isRH() || isManager()) {
            cbEmployee = new ComboBox<>();
            try {
                PreparedStatement ps = isRH()
                        ? connection.prepareStatement("SELECT id, fullName FROM utilisateur WHERE role='EMPLOYE' AND statut='Actif' ORDER BY fullName")
                        : connection.prepareStatement("SELECT id, fullName FROM utilisateur WHERE (manager_id=? OR id=?) AND statut='Actif' ORDER BY fullName");
                if (!isRH()) { ps.setInt(1, currentUserId); ps.setInt(2, currentUserId); }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) cbEmployee.getItems().add(new EmployeeItem(rs.getInt("id"), rs.getString("fullName")));
            } catch (SQLException ignored) {}
            g.add(new Label("Employé :"), 0, 2); g.add(cbEmployee, 1, 2);
        }
        Label info = new Label("💡  Un PDI Draft peut être activé ultérieurement.");
        info.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        g.add(info, 0, (isRH() || isManager()) ? 3 : 2, 2, 1);
        d.getDialogPane().setContent(g);

        final ComboBox<EmployeeItem> finalCb = cbEmployee;
        d.setResultConverter(btn -> {
            if (btn == ok) {
                try {
                    int targetId = currentUserId;
                    if (finalCb != null && finalCb.getValue() != null) targetId = finalCb.getValue().getId();
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO pdi (annee, progressionGlobale, dateCreation, statut, employe_id) VALUES (?,0,?,?,?)");
                    ps.setInt(1, spAnnee.getValue()); ps.setDate(2, Date.valueOf(LocalDate.now()));
                    ps.setString(3, cbStatut.getValue()); ps.setInt(4, targetId);
                    ps.executeUpdate();
                    showInfo("Succès","PDI créé !"); loadStatistics(); loadPDIList();
                } catch (Exception e) { showError("Erreur", e.getMessage()); }
            }
            return null;
        });
        d.showAndWait();
    }

    private void handleEditPDI(PDIItem pdi) {
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Modifier PDI");
        ButtonType ok = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        Spinner<Integer> spAnnee = new Spinner<>(2020, 2035, pdi.getAnnee());
        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("Active","Draft","Completed","Archived"); cbStatut.setValue(pdi.getStatut());
        g.add(new Label("Année :"), 0, 0); g.add(spAnnee, 1, 0);
        g.add(new Label("Statut :"), 0, 1); g.add(cbStatut, 1, 1);
        d.getDialogPane().setContent(g);
        d.setResultConverter(btn -> {
            if (btn == ok) {
                try {
                    PreparedStatement ps = connection.prepareStatement("UPDATE pdi SET annee=?,statut=? WHERE id=?");
                    ps.setInt(1, spAnnee.getValue()); ps.setString(2, cbStatut.getValue()); ps.setInt(3, pdi.getId());
                    ps.executeUpdate(); showInfo("Succès","PDI mis à jour !"); loadStatistics(); loadPDIList();
                } catch (Exception e) { showError("Erreur", e.getMessage()); }
            }
            return null;
        });
        d.showAndWait();
    }

    private void handleDeletePDI(PDIItem pdi) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Supprimer"); a.setHeaderText("Supprimer PDI " + pdi.getAnnee() + " ?");
        a.setContentText("Toutes les actions associées seront supprimées.");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                connection.prepareStatement("DELETE FROM actionPDI WHERE pdi_id=" + pdi.getId()).executeUpdate();
                connection.prepareStatement("DELETE FROM pdi WHERE id=" + pdi.getId()).executeUpdate();
                showInfo("Supprimé","PDI supprimé."); loadStatistics(); loadPDIList();
            } catch (SQLException e) { showError("Erreur", e.getMessage()); }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD ACTIONS
    // ══════════════════════════════════════════════════════════════════════════
    private void handleAddAction(PDIItem pdi) {
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Ajouter une action");
        ButtonType ok = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfType = new TextField(); tfType.setPromptText("ex: Formation React, Certification AWS…"); tfType.setPrefWidth(280);
        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("Pending","In Progress","Completed"); cbStatut.setValue("Pending");
        DatePicker dpDebut = new DatePicker(LocalDate.now());
        DatePicker dpFin   = new DatePicker(LocalDate.now().plusMonths(1));
        Spinner<Integer> spPrio = new Spinner<>(1, 5, 3); spPrio.setEditable(true);
        g.add(new Label("Action :"),0,0);   g.add(tfType,1,0);
        g.add(new Label("Statut :"),0,1);   g.add(cbStatut,1,1);
        g.add(new Label("Début :"),0,2);    g.add(dpDebut,1,2);
        g.add(new Label("Échéance :"),0,3); g.add(dpFin,1,3);
        g.add(new Label("Priorité :"),0,4); g.add(spPrio,1,4);
        d.getDialogPane().setContent(g);
        d.setResultConverter(btn -> {
            if (btn == ok) {
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO actionPDI (pdi_id,typeAction,statut,dateDebut,dateFinPrevue,priorite) VALUES (?,?,?,?,?,?)");
                    ps.setInt(1,pdi.getId()); ps.setString(2,tfType.getText()); ps.setString(3,cbStatut.getValue());
                    ps.setDate(4,dpDebut.getValue()!=null?Date.valueOf(dpDebut.getValue()):null);
                    ps.setDate(5,dpFin.getValue()!=null?Date.valueOf(dpFin.getValue()):null);
                    ps.setInt(6,spPrio.getValue()); ps.executeUpdate();
                    recalcPDIProgression(pdi.getId());
                    showInfo("Succès","Action ajoutée !"); loadStatistics(); loadPDIList();
                } catch (Exception e) { showError("Erreur", e.getMessage()); }
            }
            return null;
        });
        d.showAndWait();
    }

    private void handleEditAction(ActionItem a, PDIItem pdi) {
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Modifier l'action");
        ButtonType ok = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfType = new TextField(a.getTypeAction()); tfType.setPrefWidth(280);
        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("Pending","In Progress","Completed"); cbStatut.setValue(a.getStatut());
        DatePicker dpDebut = new DatePicker(a.getDateDebut()!=null?a.getDateDebut():LocalDate.now());
        DatePicker dpFin   = new DatePicker(a.getDateFinPrevue()!=null?a.getDateFinPrevue():LocalDate.now().plusMonths(1));
        Spinner<Integer> spPrio = new Spinner<>(1,5,a.getPriorite()); spPrio.setEditable(true);
        g.add(new Label("Action :"),0,0);   g.add(tfType,1,0);
        g.add(new Label("Statut :"),0,1);   g.add(cbStatut,1,1);
        g.add(new Label("Début :"),0,2);    g.add(dpDebut,1,2);
        g.add(new Label("Échéance :"),0,3); g.add(dpFin,1,3);
        g.add(new Label("Priorité :"),0,4); g.add(spPrio,1,4);
        d.getDialogPane().setContent(g);
        d.setResultConverter(btn -> {
            if (btn == ok) {
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "UPDATE actionPDI SET typeAction=?,statut=?,dateDebut=?,dateFinPrevue=?,priorite=? WHERE id=?");
                    ps.setString(1,tfType.getText()); ps.setString(2,cbStatut.getValue());
                    ps.setDate(3,dpDebut.getValue()!=null?Date.valueOf(dpDebut.getValue()):null);
                    ps.setDate(4,dpFin.getValue()!=null?Date.valueOf(dpFin.getValue()):null);
                    ps.setInt(5,spPrio.getValue()); ps.setInt(6,a.getId()); ps.executeUpdate();
                    recalcPDIProgression(pdi.getId());
                    showInfo("Succès","Action mise à jour !"); loadStatistics(); loadPDIList();
                } catch (Exception e) { showError("Erreur", e.getMessage()); }
            }
            return null;
        });
        d.showAndWait();
    }

    private void handleDeleteAction(ActionItem a, PDIItem pdi) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Supprimer"); conf.setHeaderText("Supprimer cette action ?");
        Optional<ButtonType> r = conf.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                connection.prepareStatement("DELETE FROM actionPDI WHERE id=" + a.getId()).executeUpdate();
                recalcPDIProgression(pdi.getId());
                showInfo("Supprimé","Action supprimée."); loadStatistics(); loadPDIList();
            } catch (SQLException e) { showError("Erreur", e.getMessage()); }
        }
    }

    private void showError(String t, String m) { Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setContentText(m); a.showAndWait(); }
    private void showInfo(String t, String m)  { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(m); a.showAndWait(); }

    // ══════════════════════════════════════════════════════════════════════════
    //  INNER CLASSES
    // ══════════════════════════════════════════════════════════════════════════
    private static class EmployeeItem {
        private final int id; private final String name;
        EmployeeItem(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        @Override public String toString() { return name != null ? name : "?"; }
    }

    public static class PDIItem {
        private final int id, annee;
        private int progressionGlobale;
        private final LocalDate dateCreation;
        private final String statut, employeNom;
        private final List<ActionItem> actions = new ArrayList<>();

        public PDIItem(int id, int annee, int prog, LocalDate dc, String statut, String nom) {
            this.id=id; this.annee=annee; this.progressionGlobale=prog;
            this.dateCreation=dc; this.statut=statut; this.employeNom=nom;
        }
        public int computeDynamicProgress() {
            if (actions.isEmpty()) return 0;
            long done = actions.stream().filter(a -> "Completed".equals(a.getStatut())).count();
            return (int)(done * 100 / actions.size());
        }
        public int       getId()                     { return id; }
        public int       getAnnee()                  { return annee; }
        public int       getProgressionGlobale()     { return progressionGlobale; }
        public void      setProgressionGlobale(int p){ this.progressionGlobale = p; }
        public LocalDate getDateCreation()           { return dateCreation; }
        public String    getStatut()                 { return statut; }
        public String    getEmployeNom()             { return employeNom; }
        public List<ActionItem> getActions()         { return actions; }
    }

    public static class ActionItem {
        private final int id, priorite, pdiId, formationId;
        private final String typeAction, statut;
        private final LocalDate dateDebut, dateFinPrevue;

        public ActionItem(int id, String type, String statut, LocalDate debut, LocalDate fin,
                          int prio, int pdiId) {
            this(id, type, statut, debut, fin, prio, pdiId, 0);
        }
        public ActionItem(int id, String type, String statut, LocalDate debut, LocalDate fin,
                          int prio, int pdiId, int formationId) {
            this.id=id; this.typeAction=type; this.statut=statut;
            this.dateDebut=debut; this.dateFinPrevue=fin; this.priorite=prio;
            this.pdiId=pdiId; this.formationId=formationId;
        }
        public int       getId()             { return id; }
        public String    getTypeAction()     { return typeAction; }
        public String    getStatut()         { return statut; }
        public LocalDate getDateDebut()      { return dateDebut; }
        public LocalDate getDateFinPrevue()  { return dateFinPrevue; }
        public int       getPriorite()       { return priorite; }
        public int       getPdiId()          { return pdiId; }
        public int       getFormationId()    { return formationId; }
        public boolean   isFormationLinked() { return formationId > 0; }
    }
}