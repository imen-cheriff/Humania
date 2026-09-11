package competence.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.MyDataBase;
import utils.UserSession;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class SkillsMatrixController {

    @FXML private Label           lblTeamMembers;
    @FXML private Label           lblCriticalGaps;
    @FXML private Label           lblAvgLevel;
    @FXML private Label           lblTotalSkills;
    @FXML private TextField       searchSkillsField;
    @FXML private TextField       searchEmployeesField;
    @FXML private ComboBox<String> cbFilterCategory;
    @FXML private CheckBox        chkShowGapsOnly;
    @FXML private Button          btnExport;
    @FXML private Button          btnAddSkillAssessment;
    @FXML private Button          btnViewMyRadar;   // optionnel — déclaré en FXML ou ajouté dynamiquement
    @FXML private VBox            heatmapContainer;

    private int rowPageSize      = 5;
    private int skillPageSize    = 5;
    private int currentPage      = 0;
    private int currentSkillPage = 0;

    private Connection connection;
    private int        currentUserId = -1; // id du manager connecté, résolu depuis DB

    private List<String>           allCompetences      = new ArrayList<>();
    private List<String>           filteredCompetences = new ArrayList<>();
    private List<EmployeeSkillRow> allRows             = new ArrayList<>();
    private List<EmployeeSkillRow> filteredRows        = new ArrayList<>();

    private static final String[] DOT_COLORS = {
            "#F59E0B","#06B6D4","#3B82F6","#22C55E","#6366F1",
            "#F97316","#A855F7","#EC4899","#14B8A6","#EF4444"
    };
    private static final String[] PILL_BG = {
            "#F1F5F9","#FEE2E2","#FEF3C7","#FEF9C3","#DCFCE7","#BFDBFE"
    };
    private static final String[] PILL_FG = {
            "#94A3B8","#DC2626","#D97706","#CA8A04","#16A34A","#2563EB"
    };

    private static final double ROW_H    = 80;
    private static final double HEADER_H = 72;
    private static final double COL_W    = 110;
    private static final double EMP_W    = 240;

    // ── Helpers rôle ──────────────────────────────────────────────────────
    private boolean isRH() {
        String r = UserSession.getInstance().getRole();
        return r != null && (r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("RH"));
    }
    private boolean isManager() {
        String r = UserSession.getInstance().getRole();
        return r != null && r.equalsIgnoreCase("MANAGER");
    }
    private boolean isEmploye() {
        String r = UserSession.getInstance().getRole();
        return r != null && r.equalsIgnoreCase("EMPLOYE");
    }

    // ── INIT ──────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        connection    = MyDataBase.getInstance().getCnx();
        currentUserId = resolveCurrentUserId();

        // EMPLOYE → vue personnelle gaps uniquement, bouton adapté
        if (isEmploye()) {
            if (btnAddSkillAssessment != null) {
                btnAddSkillAssessment.setText("➕ Ajouter ma compétence");
                btnAddSkillAssessment.setOnAction(e -> handleEmployeAddSkill());
            }
            if (chkShowGapsOnly != null) {
                chkShowGapsOnly.setSelected(true);
            }
            if (searchEmployeesField != null) {
                searchEmployeesField.setVisible(false);
                searchEmployeesField.setManaged(false);
            }
        }

        loadStatistics();
        loadCategories();
        loadData();
        filteredRows        = new ArrayList<>(allRows);
        filteredCompetences = new ArrayList<>(allCompetences);
        if (isEmploye()) applyFilters();
        renderWidget();
        setupSearch();
        addDynamicHeaderButtons();
    }

    /**
     * Ajoute dynamiquement le bouton "Mon Radar" et le bouton "AI Assistant"
     * dans la zone de header si btnViewMyRadar n'est pas défini en FXML.
     */
    private void addDynamicHeaderButtons() {
        if (btnViewMyRadar != null) {
            btnViewMyRadar.setOnAction(e -> {
                Stage owner = (Stage) heatmapContainer.getScene().getWindow();
                CompetencyRadarChart.showForCurrentUser(owner);
            });
        }
    }

    /**
     * Résout l'id de l'utilisateur connecté depuis la table `utilisateur`
     * via le username (stocké dans UserSession) ou l'email en fallback.
     */
    private int resolveCurrentUserId() {
        String username = UserSession.getInstance().getUser();
        String email    = UserSession.getInstance().getEmail();
        try {
            if (username != null && !username.isBlank()) {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT id FROM utilisateur WHERE username = ? LIMIT 1");
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            if (email != null && !email.isBlank()) {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT id FROM utilisateur WHERE email = ? LIMIT 1");
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            showError("Erreur session", "Impossible de résoudre l'id : " + e.getMessage());
        }
        return -1;
    }

    private void showAccessDenied() {
        if (heatmapContainer == null) return;
        heatmapContainer.getChildren().clear();
        heatmapContainer.setAlignment(Pos.CENTER);

        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));

        Label icon  = new Label("⛔");
        icon.setStyle("-fx-font-size:48;");
        Label title = new Label("Accès refusé");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#DC2626;");
        Label msg   = new Label("La Skills Matrix est réservée aux Managers, RH, Administrateurs et Employés.");
        msg.setStyle("-fx-font-size:14;-fx-text-fill:#6B7280;");
        msg.setWrapText(true);
        msg.setMaxWidth(420);
        msg.setAlignment(Pos.CENTER);

        box.getChildren().addAll(icon, title, msg);
        heatmapContainer.getChildren().add(box);
    }

    // ── STATS ─────────────────────────────────────────────────────────────
    private void loadStatistics() {
        // Membres de l'équipe
        if (isEmploye() && currentUserId > 0) {
            // EMPLOYE : stats personnelles
            try {
                if (lblTeamMembers != null) lblTeamMembers.setText("Moi");
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT COUNT(*) FROM competenceemploye ce " +
                                "JOIN competence c ON ce.competence_id = c.id " +
                                "WHERE ce.employe_id = ? AND ce.niveauActuel < 3 AND c.statutCompetence = 'ACTIF'");
                ps.setInt(1, currentUserId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && lblCriticalGaps != null) lblCriticalGaps.setText(String.valueOf(rs.getInt(1)));
                PreparedStatement ps2 = connection.prepareStatement(
                        "SELECT AVG(ce.niveauActuel) FROM competenceemploye ce WHERE ce.employe_id = ?");
                ps2.setInt(1, currentUserId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next() && lblAvgLevel != null)
                    lblAvgLevel.setText(String.format("%.1f", rs2.getDouble(1)));
                PreparedStatement ps3 = connection.prepareStatement(
                        "SELECT COUNT(*) FROM competenceemploye WHERE employe_id = ?");
                ps3.setInt(1, currentUserId);
                ResultSet rs3 = ps3.executeQuery();
                if (rs3.next() && lblTotalSkills != null) lblTotalSkills.setText(String.valueOf(rs3.getInt(1)));
            } catch (SQLException e) { showError("Stats", e.getMessage()); }
        } else if (isManager() && currentUserId > 0) {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT COUNT(*) FROM utilisateur WHERE manager_id = ? AND statut = 'Actif'");
                ps.setInt(1, currentUserId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && lblTeamMembers != null)
                    lblTeamMembers.setText(String.valueOf(rs.getInt(1)));
            } catch (SQLException ignored) {}
        } else {
            runStat("SELECT COUNT(*) FROM utilisateur WHERE role = 'EMPLOYE' AND statut = 'Actif'", lblTeamMembers);
        }

        // Gaps critiques (niveauActuel < 3)
        if (isManager() && currentUserId > 0) {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT COUNT(DISTINCT ce.competence_id) FROM competenceemploye ce " +
                                "JOIN utilisateur u ON ce.employe_id = u.id " +
                                "WHERE u.manager_id = ? AND ce.niveauActuel < 3");
                ps.setInt(1, currentUserId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && lblCriticalGaps != null)
                    lblCriticalGaps.setText(String.valueOf(rs.getInt(1)));
            } catch (SQLException ignored) {}
        } else {
            runStat("SELECT COUNT(DISTINCT competence_id) FROM competenceemploye WHERE niveauActuel < 3", lblCriticalGaps);
        }

        // Niveau moyen
        try {
            String avgSql = (isManager() && currentUserId > 0)
                    ? "SELECT AVG(ce.niveauActuel) FROM competenceemploye ce " +
                    "JOIN utilisateur u ON ce.employe_id = u.id WHERE u.manager_id = " + currentUserId
                    : "SELECT AVG(niveauActuel) FROM competenceemploye";
            ResultSet rs = connection.prepareStatement(avgSql).executeQuery();
            if (rs.next() && lblAvgLevel != null)
                lblAvgLevel.setText(String.format("%.1f", rs.getDouble(1)));
        } catch (SQLException ignored) {}

        runStat("SELECT COUNT(*) FROM competence WHERE statutCompetence = 'ACTIF'", lblTotalSkills);
    }

    private void runStat(String sql, Label lbl) {
        if (lbl == null) return;
        try {
            ResultSet rs = connection.prepareStatement(sql).executeQuery();
            if (rs.next()) lbl.setText(String.valueOf(rs.getInt(1)));
        } catch (SQLException ignored) { lbl.setText("0"); }
    }

    // ── DATA ──────────────────────────────────────────────────────────────
    private void loadCategories() {
        ObservableList<String> cats = FXCollections.observableArrayList("Toutes catégories");
        try {
            ResultSet rs = connection.prepareStatement(
                    "SELECT DISTINCT libelle FROM categorieCompetence ORDER BY libelle").executeQuery();
            while (rs.next()) cats.add(rs.getString(1));
        } catch (SQLException ignored) {}
        if (cbFilterCategory != null) {
            cbFilterCategory.setItems(cats);
            cbFilterCategory.setValue("Toutes catégories");
            cbFilterCategory.setOnAction(e -> reload());
        }
    }

    private void loadData() {
        allCompetences.clear();
        allRows.clear();

        String  cat       = cbFilterCategory != null ? cbFilterCategory.getValue() : null;
        boolean filterCat = cat != null && !cat.equals("Toutes catégories");

        // ── Compétences ───────────────────────────────────────────────────
        try {
            String sql;
            PreparedStatement ps;
            if (filterCat) {
                sql = "SELECT DISTINCT c.libelle FROM competence c " +
                        "JOIN categorieCompetence cc ON c.categorie_id = cc.id " +
                        "WHERE cc.libelle = ? AND c.statutCompetence = 'ACTIF' ORDER BY c.libelle";
                ps = connection.prepareStatement(sql);
                ps.setString(1, cat);
            } else {
                sql = "SELECT DISTINCT libelle FROM competence WHERE statutCompetence = 'ACTIF' ORDER BY libelle";
                ps  = connection.prepareStatement(sql);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) allCompetences.add(rs.getString(1));
        } catch (SQLException e) {
            showError("Erreur compétences", e.getMessage());
            return;
        }

        // ── Employés ──────────────────────────────────────────────────────
        // MANAGER  → WHERE manager_id = currentUserId AND statut = 'Actif'
        // ADMIN/RH → tous les EMPLOYE actifs
        //
        // `fullName` = colonne générée CONCAT(prenom, ' ', nom)
        // ─────────────────────────────────────────────────────────────────
        try {
            PreparedStatement empPs;
            if (isEmploye() && currentUserId > 0) {
                // EMPLOYE → uniquement lui-même
                empPs = connection.prepareStatement(
                        "SELECT id, fullName, IFNULL(posteActuel, role) AS poste " +
                                "FROM utilisateur WHERE id = ?");
                empPs.setInt(1, currentUserId);
            } else if (isManager() && currentUserId > 0) {
                empPs = connection.prepareStatement(
                        "SELECT id, fullName, IFNULL(posteActuel, role) AS poste " +
                                "FROM utilisateur " +
                                "WHERE manager_id = ? AND statut = 'Actif' " +
                                "ORDER BY fullName");
                empPs.setInt(1, currentUserId);
            } else {
                empPs = connection.prepareStatement(
                        "SELECT id, fullName, IFNULL(posteActuel, role) AS poste " +
                                "FROM utilisateur " +
                                "WHERE role = 'EMPLOYE' AND statut = 'Actif' " +
                                "ORDER BY fullName");
            }

            ResultSet emp = empPs.executeQuery();
            while (emp.next()) {
                int    id    = emp.getInt("id");
                String name  = emp.getString("fullName");
                String poste = emp.getString("poste");

                EmployeeSkillRow row = new EmployeeSkillRow(
                        id,
                        name  != null ? name.trim()  : "—",
                        poste != null ? poste.trim() : "—");

                // Charger les compétences de cet employé
                PreparedStatement ps2 = connection.prepareStatement(
                        "SELECT c.libelle, ce.niveauActuel, c.niveauMax, ce.niveauValide " +
                                "FROM competenceemploye ce " +
                                "JOIN competence c ON ce.competence_id = c.id " +
                                "WHERE ce.employe_id = ? AND c.statutCompetence = 'ACTIF'");
                ps2.setInt(1, id);
                ResultSet sr = ps2.executeQuery();
                while (sr.next()) {
                    row.addSkill(sr.getString("libelle"),
                            new SkillLevel(
                                    sr.getInt("niveauActuel"),
                                    sr.getInt("niveauMax"),
                                    sr.getBoolean("niveauValide")));
                }
                allRows.add(row);
            }
        } catch (SQLException e) {
            showError("Erreur employés", e.getMessage());
        }
    }

    private void reload() { loadData(); applyFilters(); }

    // ── RENDER ────────────────────────────────────────────────────────────
    private void renderWidget() {
        if (heatmapContainer == null) return;
        heatmapContainer.getChildren().clear();

        if (filteredRows.isEmpty() || filteredCompetences.isEmpty()) {
            String msg = filteredRows.isEmpty()
                    ? (isEmploye() ? "Aucun gap identifié — ajoutez vos compétences." : isManager() ? "Aucun employé dans votre équipe." : "Aucun employé trouvé.")
                    : "Aucune compétence. Ajustez vos filtres.";
            Label empty = new Label(msg);
            empty.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:14;-fx-padding:40;");
            heatmapContainer.getChildren().add(empty);
            return;
        }

        // Pagination lignes
        int totalRowPages = (int) Math.ceil(filteredRows.size() / (double) rowPageSize);
        currentPage = Math.max(0, Math.min(currentPage, totalRowPages - 1));
        int fromRow = currentPage * rowPageSize;
        int toRow   = Math.min(fromRow + rowPageSize, filteredRows.size());
        List<EmployeeSkillRow> pageRows = filteredRows.subList(fromRow, toRow);

        // Pagination colonnes
        int totalSkillPages = (int) Math.ceil(filteredCompetences.size() / (double) skillPageSize);
        currentSkillPage = Math.max(0, Math.min(currentSkillPage, totalSkillPages - 1));
        int fromSkill = currentSkillPage * skillPageSize;
        int toSkill   = Math.min(fromSkill + skillPageSize, filteredCompetences.size());
        List<String> pageSkills = filteredCompetences.subList(fromSkill, toSkill);

        // ── Panneau gauche ─────────────────────────────────────────────
        VBox leftPanel = new VBox(0);
        leftPanel.setMinWidth(EMP_W); leftPanel.setPrefWidth(EMP_W); leftPanel.setMaxWidth(EMP_W);
        leftPanel.setStyle("-fx-background-color:white;-fx-border-color:transparent #E2E8F0 transparent transparent;-fx-border-width:0 1 0 0;");

        HBox corner = new HBox();
        corner.setMinHeight(HEADER_H); corner.setMaxHeight(HEADER_H); corner.setPrefHeight(HEADER_H);
        corner.setAlignment(Pos.CENTER_LEFT); corner.setPadding(new Insets(0, 0, 0, 18));
        corner.setStyle("-fx-background-color:#F8FAFC;-fx-border-color:#E2E8F0;-fx-border-width:0 0 2 0;");
        Label cLbl = new Label(isEmploye() ? "Mes compétences — Gaps" : isManager() ? "Mon équipe" : "Tous les employés");
        cLbl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#475569;");
        corner.getChildren().add(cLbl);
        leftPanel.getChildren().add(corner);

        for (int r = 0; r < pageRows.size(); r++)
            leftPanel.getChildren().add(buildEmployeeCell(pageRows.get(r), r % 2 == 0 ? "white" : "#F8FAFC"));

        // ── Grille droite ──────────────────────────────────────────────
        VBox rightGrid = new VBox(0);
        rightGrid.setStyle("-fx-background-color:white;");
        HBox.setHgrow(rightGrid, Priority.ALWAYS);

        HBox skillHeader = new HBox(0);
        skillHeader.setMinHeight(HEADER_H); skillHeader.setMaxHeight(HEADER_H); skillHeader.setPrefHeight(HEADER_H);
        skillHeader.setStyle("-fx-background-color:#F8FAFC;-fx-border-color:#E2E8F0;-fx-border-width:0 0 2 0;");
        for (int c = 0; c < pageSkills.size(); c++) {
            VBox sh = buildSkillHeader(pageSkills.get(c), DOT_COLORS[(fromSkill + c) % DOT_COLORS.length]);
            HBox.setHgrow(sh, Priority.ALWAYS);
            skillHeader.getChildren().add(sh);
        }
        rightGrid.getChildren().add(skillHeader);

        for (int r = 0; r < pageRows.size(); r++) {
            EmployeeSkillRow row = pageRows.get(r);
            String rowBg = r % 2 == 0 ? "white" : "#F8FAFC";
            HBox dataRow = new HBox(0);
            dataRow.setMinHeight(ROW_H); dataRow.setMaxHeight(ROW_H); dataRow.setPrefHeight(ROW_H);
            dataRow.setStyle("-fx-background-color:" + rowBg + ";-fx-border-color:#F1F5F9;-fx-border-width:0 0 1 0;");
            for (int c = 0; c < pageSkills.size(); c++) {
                VBox sc = buildSkillCell(row.getSkills().get(pageSkills.get(c)),
                        pageSkills.get(c), row, DOT_COLORS[(fromSkill + c) % DOT_COLORS.length]);
                HBox.setHgrow(sc, Priority.ALWAYS);
                dataRow.getChildren().add(sc);
            }
            rightGrid.getChildren().add(dataRow);
        }

        HBox skillPagTop = buildSkillPaginationBar(totalSkillPages, fromSkill, toSkill);
        skillPagTop.setStyle("-fx-background-color:white;-fx-border-color:#E2E8F0;-fx-border-width:0 0 1 0;-fx-padding:8 16;");

        HBox tableRow = new HBox(0);
        tableRow.setStyle("-fx-background-color:white;");
        tableRow.getChildren().addAll(leftPanel, rightGrid);

        HBox empPagBottom = buildRowPaginationBar(totalRowPages, fromRow, toRow);
        empPagBottom.setStyle("-fx-background-color:white;-fx-border-color:#E2E8F0;-fx-border-width:1 0 0 0;-fx-padding:10 16;");

        heatmapContainer.getChildren().addAll(skillPagTop, tableRow, empPagBottom);
    }

    // ── CELL BUILDERS ─────────────────────────────────────────────────────
    private HBox buildEmployeeCell(EmployeeSkillRow row, String rowBg) {
        HBox cell = new HBox(12);
        cell.setMinHeight(ROW_H); cell.setPrefHeight(ROW_H);
        cell.setMinWidth(EMP_W);  cell.setPrefWidth(EMP_W);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(0, 12, 0, 16));
        cell.setStyle("-fx-background-color:" + rowBg + ";-fx-border-color:#F1F5F9;-fx-border-width:0 0 1 0;");

        String[] ABG = {"#BFDBFE","#BBF7D0","#FDE68A","#FECACA","#DDD6FE","#BAE6FD","#FCE7F3"};
        String[] AFG = {"#1D4ED8","#065F46","#92400E","#991B1B","#5B21B6","#0369A1","#9D174D"};
        int ci = Math.abs(row.getName().hashCode()) % ABG.length;

        StackPane avatar = new StackPane();
        avatar.setMinSize(42, 42); avatar.setPrefSize(42, 42);
        Circle bg = new Circle(21, Color.web(ABG[ci]));
        Label ini = new Label(getInitials(row.getName()));
        ini.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:" + AFG[ci] + ";");
        avatar.getChildren().addAll(bg, ini);

        VBox nameBox = new VBox(3);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(row.getName());   // ← fullName depuis DB
        nameLbl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
        nameLbl.setMaxWidth(EMP_W - 80);
        Label posteLbl = new Label(row.getPoste());
        posteLbl.setStyle("-fx-font-size:11;-fx-text-fill:#94A3B8;");
        posteLbl.setMaxWidth(EMP_W - 80);
        nameBox.getChildren().addAll(nameLbl, posteLbl);

        // ── Bouton Radar Chart ─────────────────────────────────────────
        Region radarSpacer = new Region(); HBox.setHgrow(radarSpacer, Priority.ALWAYS);
        Button btnRadar = new Button("📊");
        btnRadar.setStyle(
                "-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;" +
                        "-fx-font-size:14;-fx-cursor:hand;-fx-padding:6 8;" +
                        "-fx-background-radius:8;-fx-border-color:transparent;"
        );
        btnRadar.setTooltip(new Tooltip("Voir le radar de compétences de " + row.getName()));
        btnRadar.setOnMouseEntered(e -> btnRadar.setStyle(
                "-fx-background-color:#6366F1;-fx-text-fill:white;" +
                        "-fx-font-size:14;-fx-cursor:hand;-fx-padding:6 8;" +
                        "-fx-background-radius:8;-fx-border-color:transparent;"
        ));
        btnRadar.setOnMouseExited(e -> btnRadar.setStyle(
                "-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;" +
                        "-fx-font-size:14;-fx-cursor:hand;-fx-padding:6 8;" +
                        "-fx-background-radius:8;-fx-border-color:transparent;"
        ));
        btnRadar.setOnAction(e -> {
            Stage owner = (Stage) btnRadar.getScene().getWindow();
            CompetencyRadarChart.show(row.getId(), row.getName(), owner);
        });

        cell.getChildren().addAll(avatar, nameBox, radarSpacer, btnRadar);
        return cell;
    }

    private VBox buildSkillHeader(String skill, String dotColor) {
        VBox hdr = new VBox(6);
        hdr.setMinWidth(COL_W); hdr.setPrefWidth(COL_W); hdr.setMaxWidth(Double.MAX_VALUE);
        hdr.setMinHeight(HEADER_H); hdr.setMaxHeight(HEADER_H); hdr.setPrefHeight(HEADER_H);
        hdr.setAlignment(Pos.CENTER); hdr.setPadding(new Insets(10, 6, 10, 6));
        hdr.setStyle("-fx-border-color:#E2E8F0;-fx-border-width:0 1 0 0;");
        Circle dot = new Circle(7, Color.web(dotColor));
        Label lbl = new Label(skill);
        lbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#374151;");
        lbl.setWrapText(true); lbl.setMaxWidth(COL_W - 8); lbl.setAlignment(Pos.CENTER);
        hdr.getChildren().addAll(dot, lbl);
        return hdr;
    }

    private VBox buildSkillCell(SkillLevel sl, String skill, EmployeeSkillRow row, String dotColor) {
        VBox wrapper = new VBox(4);
        wrapper.setMinWidth(COL_W); wrapper.setPrefWidth(COL_W); wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setMinHeight(ROW_H); wrapper.setMaxHeight(ROW_H); wrapper.setPrefHeight(ROW_H);
        wrapper.setAlignment(Pos.CENTER); wrapper.setPadding(new Insets(6));
        wrapper.setStyle("-fx-border-color:#F1F5F9;-fx-border-width:0 1 0 0;");

        if (sl == null) {
            Label dash = new Label("–");
            dash.setStyle("-fx-font-size:18;-fx-text-fill:#CBD5E1;");
            StackPane ep = new StackPane(dash);
            ep.setMinSize(48, 48); ep.setPrefSize(48, 48);
            ep.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:16;");
            wrapper.getChildren().add(ep);
            return wrapper;
        }

        int lv  = Math.min(5, Math.max(0, sl.getLevel()));
        int tgt = sl.getTargetLevel();
        boolean hasGap = lv < tgt;

        Label lvLbl = new Label(String.valueOf(lv));
        lvLbl.setStyle("-fx-font-size:17;-fx-font-weight:bold;-fx-text-fill:" + PILL_FG[lv] + ";");
        StackPane pill = new StackPane(lvLbl);
        pill.setMinSize(48, 48); pill.setPrefSize(48, 48);
        pill.setStyle("-fx-background-color:" + PILL_BG[lv] + ";-fx-background-radius:16;");
        wrapper.getChildren().add(pill);

        if (tgt > 0) {
            String gapColor = hasGap ? "#DC2626" : "#16A34A";
            Label gapLbl = new Label("→ " + tgt);
            gapLbl.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:" + gapColor + ";");
            wrapper.getChildren().add(gapLbl);
        }

        Tooltip.install(wrapper, new Tooltip(
                skill + "\nNiveau : " + lv + " / 5\nObjectif : " + tgt +
                        (hasGap ? "\nÉcart : " + (tgt - lv) + " niveau(x)" : "") +
                        (sl.isValidated() ? "\n✓ Validé" : "")));

        wrapper.setOnMouseClicked(e -> { if (e.getClickCount() == 2) handleEditSkillLevel(row, skill, sl); });
        wrapper.setOnMouseEntered(e -> wrapper.setStyle("-fx-background-color:#F0F9FF;-fx-border-color:#BAE6FD;-fx-border-width:0 1 0 0;-fx-cursor:hand;"));
        wrapper.setOnMouseExited(e  -> wrapper.setStyle("-fx-border-color:#F1F5F9;-fx-border-width:0 1 0 0;"));
        return wrapper;
    }

    // ── PAGINATION ────────────────────────────────────────────────────────
    private HBox buildRowPaginationBar(int totalPages, int from, int to) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_RIGHT);

        Label perPageLbl = new Label("Employés par page :");
        perPageLbl.setStyle("-fx-font-size:11;-fx-text-fill:#64748B;");
        ComboBox<Integer> perPageCb = new ComboBox<>();
        perPageCb.getItems().addAll(5, 10, 15, 20, 25, 50);
        perPageCb.setValue(rowPageSize);
        perPageCb.setStyle("-fx-font-size:11;-fx-pref-width:70;-fx-background-radius:6;");
        perPageCb.setOnAction(e -> { rowPageSize = perPageCb.getValue(); currentPage = 0; renderWidget(); });

        Label info = new Label((from + 1) + "–" + to + " / " + filteredRows.size());
        info.setStyle("-fx-font-size:11;-fx-text-fill:#64748B;");

        Region sp1 = new Region(); sp1.setPrefWidth(12);
        Region sp2 = new Region(); sp2.setPrefWidth(12);

        Button prev = navBtn("←");
        prev.setDisable(currentPage == 0);
        prev.setOnAction(e -> { currentPage--; renderWidget(); });

        HBox pages = new HBox(4); pages.setAlignment(Pos.CENTER);
        int startPage = Math.max(0, currentPage - 3);
        int endPage   = Math.min(totalPages, startPage + 7);
        if (endPage - startPage < 7) startPage = Math.max(0, endPage - 7);
        for (int p = startPage; p < endPage; p++) {
            final int pi = p;
            Button btn = new Button(String.valueOf(p + 1));
            btn.setMinWidth(28);
            if (p == currentPage)
                btn.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11;-fx-padding:4 8;-fx-background-radius:6;");
            else {
                btn.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:11;-fx-padding:4 8;-fx-background-radius:6;-fx-border-color:#E5E7EB;-fx-border-radius:6;-fx-cursor:hand;");
                btn.setOnAction(e -> { currentPage = pi; renderWidget(); });
            }
            pages.getChildren().add(btn);
        }

        Button next = navBtn("→");
        next.setDisable(currentPage >= totalPages - 1);
        next.setOnAction(e -> { currentPage++; renderWidget(); });

        bar.getChildren().addAll(perPageLbl, perPageCb, sp1, info, sp2, prev, pages, next);
        return bar;
    }

    private HBox buildSkillPaginationBar(int totalPages, int from, int to) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_RIGHT);

        Label perPageLbl = new Label("Skills par page :");
        perPageLbl.setStyle("-fx-font-size:11;-fx-text-fill:#64748B;");
        ComboBox<Integer> perPageCb = new ComboBox<>();
        perPageCb.getItems().addAll(4, 6, 8, 10, 12);
        perPageCb.setValue(skillPageSize);
        perPageCb.setStyle("-fx-font-size:11;-fx-pref-width:65;-fx-background-radius:6;");
        perPageCb.setOnAction(e -> { skillPageSize = perPageCb.getValue(); currentSkillPage = 0; renderWidget(); });

        Label info = new Label("Skills " + (from + 1) + "–" + to + " / " + filteredCompetences.size());
        info.setStyle("-fx-font-size:11;-fx-text-fill:#64748B;");

        Region sp1 = new Region(); sp1.setPrefWidth(12);
        Region sp2 = new Region(); sp2.setPrefWidth(12);

        Button prev = navBtn("← Skills");
        prev.setDisable(currentSkillPage == 0);
        prev.setOnAction(e -> { currentSkillPage--; renderWidget(); });

        HBox pages = new HBox(6); pages.setAlignment(Pos.CENTER);
        int startPage = Math.max(0, currentSkillPage - 3);
        int endPage   = Math.min(totalPages, startPage + 7);
        if (endPage - startPage < 7) startPage = Math.max(0, endPage - 7);
        for (int p = startPage; p < endPage; p++) {
            final int pi = p;
            Button btn = new Button(String.valueOf(p + 1));
            btn.setMinWidth(34);
            if (p == currentSkillPage)
                btn.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:6 10;-fx-background-radius:8;");
            else {
                btn.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:12;-fx-padding:6 10;-fx-background-radius:8;-fx-border-color:#E5E7EB;-fx-border-radius:8;-fx-cursor:hand;");
                btn.setOnAction(e -> { currentSkillPage = pi; renderWidget(); });
            }
            pages.getChildren().add(btn);
        }

        Button next = navBtn("Skills →");
        next.setDisable(currentSkillPage >= totalPages - 1);
        next.setOnAction(e -> { currentSkillPage++; renderWidget(); });

        bar.getChildren().addAll(perPageLbl, perPageCb, sp1, info, sp2, prev, pages, next);
        return bar;
    }

    private Button navBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:12;-fx-padding:7 16;-fx-background-radius:8;-fx-border-color:#E5E7EB;-fx-border-radius:8;-fx-cursor:hand;");
        return b;
    }

    // ── SEARCH ────────────────────────────────────────────────────────────
    private void setupSearch() {
        if (searchEmployeesField != null)
            searchEmployeesField.textProperty().addListener((o, ov, nv) -> applyFilters());
        if (searchSkillsField != null)
            searchSkillsField.textProperty().addListener((o, ov, nv) -> applyFilters());
        if (chkShowGapsOnly != null)
            chkShowGapsOnly.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String  empKw    = searchEmployeesField != null ? searchEmployeesField.getText().trim().toLowerCase() : "";
        boolean gapsOnly = chkShowGapsOnly != null && chkShowGapsOnly.isSelected();

        filteredRows = allRows.stream()
                .filter(r -> empKw.isEmpty()
                        || r.getName().toLowerCase().contains(empKw)
                        || r.getPoste().toLowerCase().contains(empKw))
                .filter(r -> {
                    if (!gapsOnly) return true;
                    // Gap = niveau actuel < target OU niveau actuel < 3 pour EMPLOYE
                    return r.getSkills().values().stream().anyMatch(s ->
                            s.getLevel() < s.getTargetLevel() || s.getLevel() < 3);
                })
                .collect(Collectors.toList());

        String skillKw = searchSkillsField != null ? searchSkillsField.getText().trim().toLowerCase() : "";

        // Pour EMPLOYE en mode gaps : afficher seulement les compétences où il a un gap
        if (isEmploye() && gapsOnly && !filteredRows.isEmpty()) {
            final Map<String, SkillLevel> mySkills =
                    filteredRows.get(0).getSkills();
            filteredCompetences = allCompetences.stream()
                    .filter(sk -> skillKw.isEmpty() || sk.toLowerCase().contains(skillKw))
                    .filter(sk -> {
                        SkillLevel sl = mySkills.get(sk);
                        return sl != null && (sl.getLevel() < sl.getTargetLevel() || sl.getLevel() < 3);
                    })
                    .collect(Collectors.toList());
        } else {
            filteredCompetences = allCompetences.stream()
                    .filter(sk -> skillKw.isEmpty() || sk.toLowerCase().contains(skillKw))
                    .collect(Collectors.toList());
        }

        currentPage = 0;
        currentSkillPage = 0;
        renderWidget();
    }

    @FXML private void handleShowGapsOnly() { applyFilters(); }

    // ── ADD / EDIT ────────────────────────────────────────────────────────

    /** Dialogue simplifié pour EMPLOYE : ajoute/met à jour ses propres compétences */
    private void handleEmployeAddSkill() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mes compétences");
        dialog.setHeaderText("Ajouter ou mettre à jour une de mes compétences");
        ButtonType addBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        grid.setPrefWidth(420);

        // Compétence — liste de toutes les compétences actives
        ComboBox<CompetenceItem> cbComp = new ComboBox<>();
        cbComp.setPrefWidth(260);
        cbComp.setPromptText("Choisir une compétence...");
        loadCompetencesForCombo(cbComp);

        // Pré-sélectionner les gaps (niveau < 3) si l'employé en a
        // → label informatif
        Label infoLbl = new Label("Seules vos compétences avec un niveau < 3 sont des gaps.");
        infoLbl.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        infoLbl.setWrapText(true);

        Spinner<Integer> spinLevel = new Spinner<>(1, 5, 1);
        spinLevel.setEditable(true);
        spinLevel.setPrefWidth(100);

        // Mettre à jour le spinner quand on choisit une compétence existante
        cbComp.setOnAction(e -> {
            CompetenceItem sel = cbComp.getValue();
            if (sel == null) return;
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT niveauActuel FROM competenceemploye WHERE employe_id = ? AND competence_id = ?");
                ps.setInt(1, currentUserId); ps.setInt(2, sel.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    spinLevel.getValueFactory().setValue(rs.getInt(1));
                }
            } catch (SQLException ex) { /* ignore */ }
        });

        DatePicker dp = new DatePicker(LocalDate.now());
        dp.setPrefWidth(180);

        Label lvlLbl = new Label();
        spinLevel.valueProperty().addListener((obs, o, n) -> {
            if (n < 3) lvlLbl.setText("⚠ Gap (niveau < 3)");
            else lvlLbl.setText("✅ Niveau satisfaisant");
            lvlLbl.setStyle("-fx-font-size:11;-fx-text-fill:" + (n < 3 ? "#DC2626" : "#059669") + ";");
        });
        spinLevel.getValueFactory().setValue(1);
        lvlLbl.setText("⚠ Gap (niveau < 3)");
        lvlLbl.setStyle("-fx-font-size:11;-fx-text-fill:#DC2626;");

        grid.addRow(0, new Label("Compétence :"), cbComp);
        grid.addRow(1, new Label("Mon niveau (1-5) :"), spinLevel);
        grid.add(lvlLbl, 1, 2);
        grid.addRow(3, new Label("Date d'auto-évaluation :"), dp);
        grid.add(infoLbl, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                CompetenceItem comp = cbComp.getValue();
                if (comp == null) {
                    showError("Erreur", "Sélectionnez une compétence.");
                    return null;
                }
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO competenceemploye(niveauActuel, niveauValide, dateEvaluation, employe_id, competence_id) " +
                                    "VALUES(?, 0, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE niveauActuel=?, dateEvaluation=?");
                    ps.setInt(1, spinLevel.getValue());
                    ps.setDate(2, Date.valueOf(dp.getValue()));
                    ps.setInt(3, currentUserId);
                    ps.setInt(4, comp.getId());
                    ps.setInt(5, spinLevel.getValue());
                    ps.setDate(6, Date.valueOf(dp.getValue()));
                    if (ps.executeUpdate() > 0) {
                        showInfo("Compétence enregistrée",
                                spinLevel.getValue() < 3
                                        ? "Gap identifié : " + comp + " (niveau " + spinLevel.getValue() + "/5). Continuez à progresser !"
                                        : "Compétence ajoutée : " + comp + " (niveau " + spinLevel.getValue() + "/5). Bravo !");
                        reload();
                        loadStatistics();
                    }
                } catch (SQLException e) { showError("Erreur", e.getMessage()); }
            }
            return null;
        });
        dialog.showAndWait();
    }

    @FXML
    private void handleAddSkillAssessment() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une évaluation");
        dialog.setHeaderText("Assigner un niveau de compétence");
        ButtonType addBtn = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        ComboBox<EmployeeItem>   cbEmp  = new ComboBox<>(); loadEmployeesForCombo(cbEmp);
        ComboBox<CompetenceItem> cbComp = new ComboBox<>(); loadCompetencesForCombo(cbComp);
        Spinner<Integer> spinLevel = new Spinner<>(1, 5, 1);
        CheckBox cbVal = new CheckBox("Validé");
        DatePicker dp  = new DatePicker(LocalDate.now());

        grid.addRow(0, new Label("Employé :"),      cbEmp);
        grid.addRow(1, new Label("Compétence :"),   cbComp);
        grid.addRow(2, new Label("Niveau (1-5) :"), spinLevel);
        grid.addRow(3, new Label("Date :"),          dp);
        grid.add(cbVal, 1, 4);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                EmployeeItem   emp  = cbEmp.getValue();
                CompetenceItem comp = cbComp.getValue();
                if (emp == null || comp == null) {
                    showError("Erreur", "Sélectionnez un employé et une compétence");
                    return null;
                }
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO competenceemploye(niveauActuel,niveauValide,dateEvaluation,employe_id,competence_id) " +
                                    "VALUES(?,?,?,?,?) " +
                                    "ON DUPLICATE KEY UPDATE niveauActuel=?,niveauValide=?,dateEvaluation=?");
                    ps.setInt(1, spinLevel.getValue()); ps.setBoolean(2, cbVal.isSelected());
                    ps.setDate(3, Date.valueOf(dp.getValue()));
                    ps.setInt(4, emp.getId()); ps.setInt(5, comp.getId());
                    ps.setInt(6, spinLevel.getValue()); ps.setBoolean(7, cbVal.isSelected());
                    ps.setDate(8, Date.valueOf(dp.getValue()));
                    if (ps.executeUpdate() > 0) { showInfo("Succès", "Évaluation ajoutée !"); reload(); loadStatistics(); }
                } catch (SQLException e) { showError("Erreur", e.getMessage()); }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void handleEditSkillLevel(EmployeeSkillRow row, String competence, SkillLevel sl) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le niveau");
        dialog.setHeaderText(row.getName() + "  ·  " + competence);
        ButtonType saveBtn = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        Spinner<Integer> spinLevel = new Spinner<>(1, 5, sl.getLevel());
        CheckBox cbVal = new CheckBox("Validé"); cbVal.setSelected(sl.isValidated());
        DatePicker dp = new DatePicker(LocalDate.now());

        grid.addRow(0, new Label("Niveau :"),   spinLevel);
        grid.addRow(1, new Label("Objectif :"), new Label(String.valueOf(sl.getTargetLevel())));
        grid.addRow(2, new Label("Date :"),     dp);
        grid.add(cbVal, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "UPDATE competenceemploye SET niveauActuel=?,niveauValide=?,dateEvaluation=? " +
                                    "WHERE employe_id=? AND competence_id=(SELECT id FROM competence WHERE libelle=?)");
                    ps.setInt(1, spinLevel.getValue()); ps.setBoolean(2, cbVal.isSelected());
                    ps.setDate(3, Date.valueOf(dp.getValue()));
                    ps.setInt(4, row.getId()); ps.setString(5, competence);
                    if (ps.executeUpdate() > 0) { showInfo("Succès", "Niveau mis à jour !"); reload(); loadStatistics(); }
                } catch (SQLException e) { showError("Erreur", e.getMessage()); }
            }
            return null;
        });
        dialog.showAndWait();
    }

    // ── EXPORT ────────────────────────────────────────────────────────────
    @FXML
    private void handleViewMyRadar() {
        Stage owner = (Stage) heatmapContainer.getScene().getWindow();
        CompetencyRadarChart.showForCurrentUser(owner);
    }

    @FXML
    private void handleExport() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Exporter la matrice");
        ButtonType btnPDF = new ButtonType("📄  PDF",  ButtonBar.ButtonData.OTHER);
        ButtonType btnCSV = new ButtonType("📊  CSV",  ButtonBar.ButtonData.OTHER);
        dlg.getDialogPane().getButtonTypes().addAll(btnPDF, btnCSV, ButtonType.CANCEL);
        VBox content = new VBox(8);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
                new Label(filteredRows.size() + " employés  •  " + filteredCompetences.size() + " compétences"));
        dlg.getDialogPane().setContent(content);
        dlg.showAndWait().ifPresent(r -> {
            if (r == btnPDF) exportToPDF();
            else if (r == btnCSV) exportToCSV();
        });
    }

    private void exportToCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("skills_matrix_" + LocalDate.now() + ".csv");
        Stage stage = (Stage) btnExport.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        // UTF-8 avec BOM → Excel/LibreOffice reconnaît les accents automatiquement
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write('\uFEFF'); // BOM UTF-8
            // En-tête
            w.write(csvCell("Employé") + "," + csvCell("Poste"));
            for (String s : allCompetences) w.write("," + csvCell(s));
            w.newLine();
            // Lignes de données
            for (EmployeeSkillRow row : filteredRows) {
                w.write(csvCell(row.getName()) + "," + csvCell(row.getPoste()));
                for (String s : allCompetences) {
                    SkillLevel sl = row.getSkills().get(s);
                    w.write("," + (sl != null ? csvCell(String.valueOf(sl.getLevel())) : csvCell("-")));
                }
                w.newLine();
            }
            w.flush();
            showInfo("Export CSV", "Exporté → " + file.getName());
        } catch (IOException e) { showError("Erreur", e.getMessage()); }
    }

    /**
     * Encapsule une valeur CSV entre guillemets doubles et échappe les guillemets internes.
     * Cela évite les décalages de colonnes si le texte contient des virgules ou sauts de ligne.
     */
    private String csvCell(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"\"") + "\"";
    }

    private void exportToPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("skills_matrix_" + LocalDate.now() + ".pdf");
        Stage stage = (Stage) btnExport.getScene().getWindow();
        File outFile = fc.showSaveDialog(stage);
        if (outFile == null) return;

        List<competence.pdf.SkillsMatrixPdfExporter.EmployeeRow> rows = new ArrayList<>();
        for (EmployeeSkillRow r : filteredRows) {
            List<competence.pdf.SkillsMatrixPdfExporter.SkillEntry> skills = new ArrayList<>();
            for (String comp : filteredCompetences) {
                SkillLevel sl = r.getSkills().get(comp);
                skills.add(new competence.pdf.SkillsMatrixPdfExporter.SkillEntry(comp, sl != null ? sl.getLevel() : 0));
            }
            rows.add(new competence.pdf.SkillsMatrixPdfExporter.EmployeeRow(r.getName(), r.getPoste(), skills));
        }
        if (btnExport != null) { btnExport.setDisable(true); btnExport.setText("⏳ Export…"); }
        File finalOut = outFile;
        List<String> finalComps = new ArrayList<>(filteredCompetences);
        new Thread(() -> {
            try {
                new competence.pdf.SkillsMatrixPdfExporter().exportToFile(rows, finalComps, finalOut);
                javafx.application.Platform.runLater(() -> {
                    if (btnExport != null) { btnExport.setDisable(false); btnExport.setText("⬇  Exporter"); }
                    showPDFSuccess(finalOut);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (btnExport != null) { btnExport.setDisable(false); btnExport.setText("⬇  Exporter"); }
                    showError("Export PDF", e.getMessage());
                });
            }
        }, "skills-pdf").start();
    }

    private void showPDFSuccess(File f) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("PDF exporté !"); a.setHeaderText("✅ Matrice exportée"); a.setContentText("Fichier : " + f.getName());
        ButtonType od = new ButtonType("📂 Dossier", ButtonBar.ButtonData.OTHER);
        ButtonType op = new ButtonType("👁 Ouvrir",  ButtonBar.ButtonData.OTHER);
        a.getButtonTypes().setAll(od, op, ButtonType.OK);
        a.showAndWait().ifPresent(btn -> {
            try {
                if (btn == od) java.awt.Desktop.getDesktop().open(f.getParentFile());
                else if (btn == op) java.awt.Desktop.getDesktop().open(f);
            } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
        });
    }

    // ── Combos ────────────────────────────────────────────────────────────
    private void loadEmployeesForCombo(ComboBox<EmployeeItem> cb) {
        try {
            PreparedStatement ps;
            if (isManager() && currentUserId > 0) {
                ps = connection.prepareStatement(
                        "SELECT id, fullName FROM utilisateur WHERE manager_id = ? AND statut = 'Actif' ORDER BY fullName");
                ps.setInt(1, currentUserId);
            } else {
                ps = connection.prepareStatement(
                        "SELECT id, fullName FROM utilisateur WHERE role = 'EMPLOYE' AND statut = 'Actif' ORDER BY fullName");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                cb.getItems().add(new EmployeeItem(rs.getInt("id"), rs.getString("fullName")));
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }
    }

    private void loadCompetencesForCombo(ComboBox<CompetenceItem> cb) {
        try {
            ResultSet rs = connection.prepareStatement(
                    "SELECT id, libelle FROM competence WHERE statutCompetence = 'ACTIF' ORDER BY libelle").executeQuery();
            while (rs.next())
                cb.getItems().add(new CompetenceItem(rs.getInt("id"), rs.getString("libelle")));
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }
    }

    // ── UTILS ─────────────────────────────────────────────────────────────
    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setContentText(m); a.showAndWait();
    }
    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(m); a.showAndWait();
    }

    // ── INNER CLASSES ─────────────────────────────────────────────────────
    public static class EmployeeSkillRow {
        private final int id;
        private final String name, poste;
        private final Map<String, SkillLevel> skills = new LinkedHashMap<>();
        public EmployeeSkillRow(int id, String name, String poste) { this.id = id; this.name = name; this.poste = poste; }
        public void addSkill(String c, SkillLevel l) { skills.put(c, l); }
        public int    getId()    { return id; }
        public String getName()  { return name; }
        public String getPoste() { return poste; }
        public Map<String, SkillLevel> getSkills() { return skills; }
    }

    public static class SkillLevel {
        private final int level, targetLevel;
        private final boolean validated;
        public SkillLevel(int l, int t, boolean v) { level = l; targetLevel = t; validated = v; }
        public int     getLevel()       { return level; }
        public int     getTargetLevel() { return targetLevel; }
        public boolean isValidated()    { return validated; }
    }

    public static class EmployeeItem {
        private final int id; private final String name;
        public EmployeeItem(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        @Override public String toString() { return name; }
    }

    public static class CompetenceItem {
        private final int id; private final String name;
        public CompetenceItem(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        @Override public String toString() { return name; }
    }
}