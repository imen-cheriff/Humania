package competence.controllers;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.*;
import javafx.util.Duration;
import utils.MyDataBase;
import utils.UserSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║        HRAnalyticsDashboard — Dashboard RH Analytics         ║
 * ║  Pour ADMIN/RH uniquement : KPIs globaux, compétences        ║
 * ║  populaires, formations les plus suivies, gaps équipe,       ║
 * ║  répartition par rôle, progression globale.                  ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * UTILISATION :
 *   HRAnalyticsDashboard.show(ownerStage);
 *
 *   // Intégrer directement dans MainFX
 *   ScrollPane view = HRAnalyticsDashboard.buildView();
 *   mainLayout.setCenter(view);
 */
public class HRAnalyticsDashboard {

    // ── Static factory ──────────────────────────────────────────────────
    public static void show(Stage owner) {
        Stage s = new Stage();
        s.initModality(Modality.NONE);
        s.initOwner(owner);
        s.setTitle("Dashboard RH Analytics");
        s.setMaximized(true);
        javafx.scene.Scene scene = new javafx.scene.Scene(buildView(), 1400, 850);
        s.setScene(scene);
        s.show();
    }

    public static ScrollPane buildView() {
        return new HRAnalyticsDashboard().build();
    }

    // ── Data containers ─────────────────────────────────────────────────
    private int totalUsers, totalEmployes, totalManagers, totalFormations;
    private int totalCompetences, totalPDIs, totalInscriptions;
    private double avgCompetenceLevel, avgProgression;
    private final List<BarEntry>    topCompetences  = new ArrayList<>();
    private final List<BarEntry>    topFormations   = new ArrayList<>();
    private final List<BarEntry>    gapsByCategory  = new ArrayList<>();
    private final List<PieEntry>    roleDistrib     = new ArrayList<>();
    private final List<BarEntry>    monthlyActivity = new ArrayList<>();
    private final List<UserSkillRow> topEmployees   = new ArrayList<>();

    private static final String[] PALETTE = {
            "#6366F1","#10B981","#F59E0B","#EF4444",
            "#3B82F6","#EC4899","#14B8A6","#8B5CF6","#F97316"
    };

    private HRAnalyticsDashboard() { loadAllData(); }

    // ══════════════════════════════════════════════════════════════════════
    //  DATA
    // ══════════════════════════════════════════════════════════════════════
    private void loadAllData() {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();
            loadKpis(cnx);
            loadTopCompetences(cnx);
            loadTopFormations(cnx);
            loadGapsByCategory(cnx);
            loadRoleDistrib(cnx);
            loadMonthlyActivity(cnx);
            loadTopEmployees(cnx);
        } catch (Exception e) {
            System.err.println("HRAnalyticsDashboard: " + e.getMessage());
        }
    }

    private void loadKpis(Connection cnx) throws SQLException {
        totalUsers       = queryInt(cnx, "SELECT COUNT(*) FROM utilisateur");
        totalEmployes    = queryInt(cnx, "SELECT COUNT(*) FROM utilisateur WHERE role='EMPLOYE'");
        totalManagers    = queryInt(cnx, "SELECT COUNT(*) FROM utilisateur WHERE role='MANAGER'");
        totalFormations  = queryInt(cnx, "SELECT COUNT(*) FROM formation");
        totalCompetences = queryInt(cnx, "SELECT COUNT(*) FROM competence WHERE IFNULL(statutCompetence,'ACTIF')='ACTIF'");
        totalPDIs        = queryInt(cnx, "SELECT COUNT(*) FROM pdi");
        totalInscriptions= queryInt(cnx, "SELECT COUNT(*) FROM inscriptionFormation");

        ResultSet rs = cnx.prepareStatement(
                "SELECT AVG(niveauActuel) FROM competenceemploye").executeQuery();
        if (rs.next()) avgCompetenceLevel = rs.getDouble(1);

        ResultSet rs2 = cnx.prepareStatement(
                "SELECT AVG(progression) FROM inscriptionFormation WHERE statut='In Progress'").executeQuery();
        if (rs2.next()) avgProgression = rs2.getDouble(1);
    }

    private void loadTopCompetences(Connection cnx) throws SQLException {
        // Top compétences les plus possédées + niveau moyen atteint
        ResultSet rs = cnx.prepareStatement(
                "SELECT c.libelle, COUNT(ce.employe_id) AS nb_employes, " +
                        "AVG(ce.niveauActuel) AS avg_niveau, c.niveauMax, " +
                        "IFNULL(cat.couleur,'#6366F1') AS couleur " +
                        "FROM competenceemploye ce " +
                        "JOIN competence c ON ce.competence_id = c.id " +
                        "LEFT JOIN categorieCompetence cat ON c.categorie_id = cat.id " +
                        "GROUP BY c.id, c.libelle, c.niveauMax, cat.couleur " +
                        "ORDER BY nb_employes DESC, avg_niveau DESC LIMIT 8"
        ).executeQuery();
        while (rs.next()) {
            double avgNiv = rs.getDouble("avg_niveau");
            int    maxNiv = Math.max(1, rs.getInt("niveauMax"));
            topCompetences.add(new BarEntry(
                    rs.getString("libelle"),
                    rs.getInt("nb_employes"),
                    avgNiv / maxNiv,
                    rs.getString("couleur")
            ));
        }
    }

    private void loadTopFormations(Connection cnx) throws SQLException {
        ResultSet rs = cnx.prepareStatement(
                "SELECT f.titre, COUNT(inf.id) AS nb_inscrits, " +
                        "AVG(inf.progression) AS avg_prog, " +
                        "IFNULL(cat.couleur,'#10B981') AS couleur " +
                        "FROM inscriptionFormation inf " +
                        "JOIN sessionFormation sf ON inf.session_id = sf.id " +
                        "JOIN formation f ON sf.formation_id = f.id " +
                        "LEFT JOIN categorieFormation catf ON f.categorie_id = catf.id " +
                        "LEFT JOIN categorieCompetence cat ON cat.libelle = catf.libelle " +
                        "GROUP BY f.id, f.titre, cat.couleur " +
                        "ORDER BY nb_inscrits DESC LIMIT 6"
        ).executeQuery();
        while (rs.next()) {
            topFormations.add(new BarEntry(
                    rs.getString("titre"),
                    rs.getInt("nb_inscrits"),
                    rs.getDouble("avg_prog") / 100.0,
                    rs.getString("couleur")
            ));
        }
    }

    private void loadGapsByCategory(Connection cnx) throws SQLException {
        ResultSet rs = cnx.prepareStatement(
                "SELECT IFNULL(cat.libelle,'Général') AS cat, " +
                        "COUNT(CASE WHEN ce.niveauActuel < c.niveauMax * 0.6 THEN 1 END) AS gaps, " +
                        "COUNT(ce.id) AS total, IFNULL(cat.couleur,'#EF4444') AS couleur " +
                        "FROM competenceemploye ce " +
                        "JOIN competence c ON ce.competence_id = c.id " +
                        "LEFT JOIN categorieCompetence cat ON c.categorie_id = cat.id " +
                        "GROUP BY cat.libelle, cat.couleur " +
                        "ORDER BY gaps DESC LIMIT 7"
        ).executeQuery();
        while (rs.next()) {
            int gaps  = rs.getInt("gaps");
            int total = rs.getInt("total");
            if (total > 0) {
                gapsByCategory.add(new BarEntry(
                        rs.getString("cat"),
                        gaps,
                        total > 0 ? (double)gaps / total : 0,
                        rs.getString("couleur")
                ));
            }
        }
    }

    private void loadRoleDistrib(Connection cnx) throws SQLException {
        ResultSet rs = cnx.prepareStatement(
                "SELECT role, COUNT(*) AS n FROM utilisateur GROUP BY role ORDER BY n DESC"
        ).executeQuery();
        int i = 0;
        while (rs.next()) {
            roleDistrib.add(new PieEntry(
                    rs.getString("role"), rs.getInt("n"),
                    PALETTE[i % PALETTE.length]
            ));
            i++;
        }
    }

    private void loadMonthlyActivity(Connection cnx) throws SQLException {
        ResultSet rs = cnx.prepareStatement(
                "SELECT DATE_FORMAT(dateInscription,'%b %Y') AS mois, " +
                        "COUNT(*) AS n, MIN(dateInscription) AS d " +
                        "FROM inscriptionFormation " +
                        "WHERE dateInscription >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
                        "GROUP BY mois, YEAR(dateInscription), MONTH(dateInscription) " +
                        "ORDER BY d ASC"
        ).executeQuery();
        while (rs.next()) {
            monthlyActivity.add(new BarEntry(
                    rs.getString("mois"), rs.getInt("n"), 0, "#6366F1"
            ));
        }
    }

    private void loadTopEmployees(Connection cnx) throws SQLException {
        ResultSet rs = cnx.prepareStatement(
                "SELECT u.id, IFNULL(u.fullName, u.username) AS nom, u.role, " +
                        "COUNT(ce.id) AS nb_comp, " +
                        "ROUND(AVG(ce.niveauActuel / GREATEST(c.niveauMax,1) * 100)) AS score " +
                        "FROM utilisateur u " +
                        "LEFT JOIN competenceemploye ce ON ce.employe_id = u.id " +
                        "LEFT JOIN competence c ON ce.competence_id = c.id " +
                        "WHERE u.role IN ('EMPLOYE','MANAGER') " +
                        "GROUP BY u.id, u.fullName, u.username, u.role " +
                        "ORDER BY score DESC NULLS LAST LIMIT 8"
        ).executeQuery();
        while (rs.next()) {
            topEmployees.add(new UserSkillRow(
                    rs.getString("nom"),
                    rs.getString("role"),
                    rs.getInt("nb_comp"),
                    rs.getInt("score")
            ));
        }
    }

    private int queryInt(Connection cnx, String sql) throws SQLException {
        ResultSet rs = cnx.prepareStatement(sql).executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD
    // ══════════════════════════════════════════════════════════════════════
    private ScrollPane build() {
        VBox page = new VBox(0);
        page.setStyle("-fx-background-color: #F1F5F9;");
        page.getChildren().addAll(
                buildHeader(),
                buildKpiRow(),
                buildRow1(),
                buildRow2(),
                buildRow3()
        );

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background:#F1F5F9;-fx-background-color:#F1F5F9;-fx-border-color:transparent;");
        return scroll;
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox(0);
        header.setPadding(new Insets(28, 36, 24, 36));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:linear-gradient(to right,#1E293B,#334155);");

        VBox left = new VBox(4);
        Label title = new Label("Dashboard RH Analytics");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label sub = new Label("Vue globale · " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) +
                " · " + totalUsers + " utilisateurs · Rôle : " + UserSession.getInstance().getRole());
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.6);");
        left.getChildren().addAll(title, sub);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Badge admin
        Label adminBadge = new Label("⚙ " + UserSession.getInstance().getRole());
        adminBadge.setStyle(
                "-fx-background-color:rgba(99,102,241,0.3);-fx-text-fill:#A5B4FC;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 14;" +
                        "-fx-background-radius:20;-fx-border-color:#6366F133;-fx-border-radius:20;-fx-border-width:1;"
        );
        header.getChildren().addAll(left, sp, adminBadge);
        return header;
    }

    // ── KPI Row ────────────────────────────────────────────────────────────
    private HBox buildKpiRow() {
        HBox row = new HBox(16);
        row.setPadding(new Insets(24, 36, 0, 36));
        row.setStyle("-fx-background-color:#F1F5F9;");

        String[][] kpis = {
                {"👥", String.valueOf(totalUsers),        "Utilisateurs",    "#4F46E5","#EEF2FF","#C7D2FE"},
                {"🎓", String.valueOf(totalFormations),    "Formations",      "#059669","#ECFDF5","#BBF7D0"},
                {"🧠", String.valueOf(totalCompetences),   "Compétences",     "#0EA5E9","#F0F9FF","#BAE6FD"},
                {"📋", String.valueOf(totalPDIs),          "PDI actifs",      "#7C3AED","#F5F3FF","#DDD6FE"},
                {"📊", String.format("%.1f",avgCompetenceLevel),"Niv. moyen","#D97706","#FFFBEB","#FDE68A"},
                {"🏆", String.valueOf(totalInscriptions),  "Inscriptions",   "#DC2626","#FEF2F2","#FECACA"},
        };

        for (String[] k : kpis) {
            VBox card = buildKpiCard(k[0],k[1],k[2],k[3],k[4],k[5]);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private VBox buildKpiCard(String icon, String value, String label,
                              String accent, String bg, String border) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:" + border + ";-fx-border-radius:14;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);"
        );

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        StackPane ico = new StackPane();
        ico.setPrefSize(38, 38);
        ico.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:10;");
        Label icoLbl = new Label(icon);
        icoLbl.setStyle("-fx-font-size:16px;");
        ico.getChildren().add(icoLbl);

        Label val = new Label(value);
        val.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:" + accent + ";");
        top.getChildren().addAll(ico, val);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");

        Region trendBar = new Region();
        trendBar.setPrefHeight(3); trendBar.setMaxWidth(Double.MAX_VALUE);
        trendBar.setStyle("-fx-background-color:" + accent + "30;-fx-background-radius:2;");

        card.getChildren().addAll(top, lbl, trendBar);
        return card;
    }

    // ── Row 1 : Top compétences + Pie rôles ───────────────────────────────
    private HBox buildRow1() {
        HBox row = new HBox(20);
        row.setPadding(new Insets(20, 36, 0, 36));
        row.setStyle("-fx-background-color:#F1F5F9;");

        VBox topComp = buildBarChart(
                "🧠  Top compétences populaires",
                "Nombre d'employés ayant la compétence",
                topCompetences, "employes"
        );
        HBox.setHgrow(topComp, Priority.ALWAYS);

        VBox pieCard = buildPieChart();
        pieCard.setPrefWidth(320);
        pieCard.setMinWidth(280);

        row.getChildren().addAll(topComp, pieCard);
        return row;
    }

    // ── Row 2 : Top formations + Gaps par catégorie ───────────────────────
    private HBox buildRow2() {
        HBox row = new HBox(20);
        row.setPadding(new Insets(20, 36, 0, 36));
        row.setStyle("-fx-background-color:#F1F5F9;");

        VBox topForm = buildBarChart(
                "📚  Formations les plus suivies",
                "Nombre d'inscriptions",
                topFormations, "inscrits"
        );
        HBox.setHgrow(topForm, Priority.ALWAYS);

        VBox gaps = buildGapsChart();
        HBox.setHgrow(gaps, Priority.ALWAYS);

        row.getChildren().addAll(topForm, gaps);
        return row;
    }

    // ── Row 3 : Activité mensuelle + Top employés ─────────────────────────
    private HBox buildRow3() {
        HBox row = new HBox(20);
        row.setPadding(new Insets(20, 36, 36, 36));
        row.setStyle("-fx-background-color:#F1F5F9;");

        VBox activity = buildActivityChart();
        HBox.setHgrow(activity, Priority.ALWAYS);

        VBox topEmp = buildTopEmployeesTable();
        topEmp.setPrefWidth(380);
        topEmp.setMinWidth(320);

        row.getChildren().addAll(activity, topEmp);
        return row;
    }

    // ── Bar Chart Builder ──────────────────────────────────────────────────
    private VBox buildBarChart(String title, String subtitle,
                               List<BarEntry> data, String unit) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#E2E8F6;-fx-border-radius:14;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);"
        );

        Label t = new Label(title);
        t.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Label s = new Label(subtitle);
        s.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
        card.getChildren().addAll(t, s);

        if (data.isEmpty()) {
            Label empty = new Label("Aucune donnée disponible");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#9CA3AF;-fx-padding:20;");
            card.getChildren().add(empty);
            return card;
        }

        int maxVal = data.stream().mapToInt(e -> e.value).max().orElse(1);

        for (int i = 0; i < data.size(); i++) {
            BarEntry e = data.get(i);
            String color = e.color != null && !e.color.isBlank() ? e.color : PALETTE[i % PALETTE.length];

            HBox barRow = new HBox(10);
            barRow.setAlignment(Pos.CENTER_LEFT);

            // Rank badge
            Label rank = new Label(String.valueOf(i + 1));
            rank.setPrefWidth(22); rank.setMinWidth(22);
            rank.setAlignment(Pos.CENTER);
            rank.setStyle(
                    "-fx-background-color:" + color + "20;-fx-text-fill:" + color + ";" +
                            "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-padding:2 6;-fx-background-radius:6;"
            );

            // Label
            String label = e.label.length() > 22 ? e.label.substring(0, 20) + "…" : e.label;
            Label nameLbl = new Label(label);
            nameLbl.setPrefWidth(190); nameLbl.setMinWidth(140);
            nameLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");

            // Bar
            StackPane barWrap = new StackPane();
            barWrap.setPrefHeight(24); HBox.setHgrow(barWrap, Priority.ALWAYS);
            Region track = new Region();
            track.setPrefHeight(24);
            track.setStyle("-fx-background-color:#F8FAFF;-fx-background-radius:6;");
            track.prefWidthProperty().bind(barWrap.widthProperty());

            Region fill = new Region();
            fill.setPrefHeight(24);
            fill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;");
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            double ratio = maxVal > 0 ? (double) e.value / maxVal : 0;
            barWrap.widthProperty().addListener((obs, o, nw) ->
                    fill.setPrefWidth(Math.max(ratio > 0 ? 24 : 0, nw.doubleValue() * ratio)));
            barWrap.getChildren().addAll(track, fill);

            // Value
            Label valLbl = new Label(e.value + " " + unit);
            valLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
            valLbl.setPrefWidth(70); valLbl.setMinWidth(60);

            barRow.getChildren().addAll(rank, nameLbl, barWrap, valLbl);
            card.getChildren().add(barRow);
        }
        return card;
    }

    // ── Pie Chart ──────────────────────────────────────────────────────────
    private VBox buildPieChart() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#E2E8F6;-fx-border-radius:14;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);"
        );

        Label t = new Label("👤  Répartition des rôles");
        t.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        card.getChildren().add(t);

        Canvas pie = new Canvas(260, 220);
        GraphicsContext gc = pie.getGraphicsContext2D();
        drawPie(gc, 260, 220);
        card.getChildren().add(pie);

        // Légende
        VBox legend = new VBox(6);
        int total = roleDistrib.stream().mapToInt(e -> e.value).sum();
        for (PieEntry e : roleDistrib) {
            HBox item = new HBox(8);
            item.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(6, Color.web(e.color));
            Label lbl = new Label(e.label);
            lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            int pct = total > 0 ? (int)(e.value * 100.0 / total) : 0;
            Label valLbl = new Label(e.value + "  (" + pct + "%)");
            valLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + e.color + ";");
            item.getChildren().addAll(dot, lbl, sp, valLbl);
            legend.getChildren().add(item);
        }
        card.getChildren().add(legend);
        return card;
    }

    private void drawPie(GraphicsContext gc, double W, double H) {
        double cx = W / 2, cy = H / 2 - 10, r = 80;
        int total = roleDistrib.stream().mapToInt(e -> e.value).sum();
        if (total == 0) return;

        double angle = -90;
        for (PieEntry e : roleDistrib) {
            double sweep = 360.0 * e.value / total;
            gc.setFill(Color.web(e.color));
            gc.fillArc(cx - r, cy - r, r * 2, r * 2, angle, sweep,
                    javafx.scene.shape.ArcType.ROUND);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, angle, sweep,
                    javafx.scene.shape.ArcType.ROUND);

            // Label %
            double midAngle = Math.toRadians(angle + sweep / 2);
            double lx = cx + (r * 0.65) * Math.cos(midAngle);
            double ly = cy + (r * 0.65) * Math.sin(midAngle);
            int pct = (int)(e.value * 100.0 / total);
            if (pct > 6) {
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("System",
                        javafx.scene.text.FontWeight.BOLD, 10));
                gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                gc.setTextBaseline(javafx.geometry.VPos.CENTER);
                gc.fillText(pct + "%", lx, ly);
            }
            angle += sweep;
        }

        // Centre donut
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 40, cy - 40, 80, 80);
        gc.setFill(Color.web("#374151"));
        gc.setFont(javafx.scene.text.Font.font("System",
                javafx.scene.text.FontWeight.BOLD, 16));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(String.valueOf(total), cx, cy - 4);
        gc.setFont(javafx.scene.text.Font.font("System", 9));
        gc.setFill(Color.web("#9CA3AF"));
        gc.fillText("utilisateurs", cx, cy + 12);
    }

    // ── Gaps Chart ─────────────────────────────────────────────────────────
    private VBox buildGapsChart() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#E2E8F6;-fx-border-radius:14;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);"
        );

        Label t = new Label("⚠  Gaps par catégorie");
        t.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Label s = new Label("% de compétences sous 60% du niveau requis");
        s.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
        card.getChildren().addAll(t, s);

        if (gapsByCategory.isEmpty()) {
            Label empty = new Label("🎉  Aucun gap critique détecté !");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#10B981;-fx-padding:20;");
            card.getChildren().add(empty);
            return card;
        }

        for (BarEntry e : gapsByCategory) {
            VBox item = new VBox(4);

            HBox labelRow = new HBox(6);
            labelRow.setAlignment(Pos.CENTER_LEFT);
            Label catLbl = new Label(e.label);
            catLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            int pct = (int)(e.ratio * 100);
            String gapColor = pct >= 50 ? "#DC2626" : pct >= 30 ? "#F59E0B" : "#10B981";
            Label pctLbl = new Label(pct + "% gaps  ·  " + e.value + " compétences");
            pctLbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + gapColor + ";");
            labelRow.getChildren().addAll(catLbl, sp, pctLbl);

            StackPane barWrap = new StackPane();
            barWrap.setPrefHeight(12);
            Region track = new Region();
            track.setPrefHeight(12);
            track.setStyle("-fx-background-color:#F1F5F9;-fx-background-radius:6;");
            track.prefWidthProperty().bind(barWrap.widthProperty());
            Region fill = new Region();
            fill.setPrefHeight(12);
            fill.setStyle("-fx-background-color:" + gapColor + ";-fx-background-radius:6;");
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            barWrap.widthProperty().addListener((obs, o, nw) ->
                    fill.setPrefWidth(nw.doubleValue() * e.ratio));
            barWrap.getChildren().addAll(track, fill);

            item.getChildren().addAll(labelRow, barWrap);
            card.getChildren().add(item);
        }
        return card;
    }

    // ── Activity Chart ─────────────────────────────────────────────────────
    private VBox buildActivityChart() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#E2E8F6;-fx-border-radius:14;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);"
        );

        Label t = new Label("📅  Activité mensuelle — Inscriptions formations");
        t.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        card.getChildren().add(t);

        if (monthlyActivity.isEmpty()) {
            Label empty = new Label("Aucune activité sur les 6 derniers mois");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#9CA3AF;-fx-padding:20;");
            card.getChildren().add(empty);
            return card;
        }

        Canvas c = new Canvas(600, 160);
        GraphicsContext gc = c.getGraphicsContext2D();
        drawActivityBars(gc, 600, 160);
        c.widthProperty().addListener((obs, o, nw) -> {
            c.setWidth(nw.doubleValue());
            drawActivityBars(gc, nw.doubleValue(), 160);
        });
        card.getChildren().add(c);
        return card;
    }

    private void drawActivityBars(GraphicsContext gc, double W, double H) {
        gc.clearRect(0, 0, W, H);
        if (monthlyActivity.isEmpty()) return;

        int n   = monthlyActivity.size();
        int max = monthlyActivity.stream().mapToInt(e -> e.value).max().orElse(1);
        double barW  = (W - 40) / n - 10;
        double chartH = H - 40;

        // Grid lines
        for (int i = 0; i <= 4; i++) {
            double y = 10 + chartH * (1 - i / 4.0);
            gc.setStroke(Color.web("#F1F5F9")); gc.setLineWidth(1);
            gc.strokeLine(40, y, W, y);
            gc.setFill(Color.web("#9CA3AF"));
            gc.setFont(javafx.scene.text.Font.font("System", 8));
            gc.setTextAlign(javafx.scene.text.TextAlignment.RIGHT);
            gc.fillText(String.valueOf(max * i / 4), 36, y + 3);
        }

        for (int i = 0; i < n; i++) {
            BarEntry e = monthlyActivity.get(i);
            double x = 44 + i * (barW + 10);
            double barH = chartH * e.value / max;
            double y = 10 + chartH - barH;

            // Bar avec gradient
            LinearGradient grad = new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#6366F1")), new Stop(1, Color.web("#8B5CF6")));
            gc.setFill(grad);
            // Coins arrondis
            double radius = 4;
            gc.beginPath();
            gc.moveTo(x + radius, y);
            gc.lineTo(x + barW - radius, y);
            gc.quadraticCurveTo(x + barW, y, x + barW, y + radius);
            gc.lineTo(x + barW, y + barH);
            gc.lineTo(x, y + barH);
            gc.lineTo(x, y + radius);
            gc.quadraticCurveTo(x, y, x + radius, y);
            gc.closePath();
            gc.fill();

            // Value on top
            gc.setFill(Color.web("#374151"));
            gc.setFont(javafx.scene.text.Font.font("System",
                    javafx.scene.text.FontWeight.BOLD, 9));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText(String.valueOf(e.value), x + barW / 2, y - 4);

            // Month label
            gc.setFont(javafx.scene.text.Font.font("System", 9));
            gc.setFill(Color.web("#6B7280"));
            gc.fillText(e.label, x + barW / 2, H - 4);
        }
    }

    // ── Top Employees Table ────────────────────────────────────────────────
    private VBox buildTopEmployeesTable() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 18, 18, 18));
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#E2E8F6;-fx-border-radius:14;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);"
        );

        Label t = new Label("🏆  Top employés — Score de compétences");
        t.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        card.getChildren().add(t);

        if (topEmployees.isEmpty()) {
            Label empty = new Label("Aucun employé avec des compétences enregistrées");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#9CA3AF;");
            card.getChildren().add(empty);
            return card;
        }

        for (int i = 0; i < topEmployees.size(); i++) {
            UserSkillRow u = topEmployees.get(i);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            String rowBg = i % 2 == 0 ? "white" : "#F8FAFF";
            row.setStyle("-fx-background-color:" + rowBg + ";-fx-background-radius:8;");

            // Rank
            String rankColor = i == 0 ? "#F59E0B" : i == 1 ? "#94A3B8" : i == 2 ? "#CD7C2F" : "#E2E8F6";
            Label rankLbl = new Label("#" + (i+1));
            rankLbl.setPrefWidth(28);
            rankLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + rankColor + ";");

            // Avatar
            StackPane avatar = new StackPane();
            avatar.setPrefSize(32, 32);
            String[] parts = (u.nom != null ? u.nom : "?").trim().split("\\s+");
            String ini = parts.length >= 2
                    ? "" + parts[0].charAt(0) + parts[parts.length-1].charAt(0)
                    : (u.nom != null && u.nom.length() >= 2 ? u.nom.substring(0,2) : "??");
            String avCol = PALETTE[i % PALETTE.length];
            Circle avBg = new Circle(16, Color.web(avCol + "30"));
            Label avLbl = new Label(ini.toUpperCase());
            avLbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + avCol + ";");
            avatar.getChildren().addAll(avBg, avLbl);

            // Info
            VBox info = new VBox(1); HBox.setHgrow(info, Priority.ALWAYS);
            Label nameLbl = new Label(u.nom != null ? u.nom : "—");
            nameLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
            nameLbl.setMaxWidth(140);
            Label roleLbl = new Label(u.role + " · " + u.nbComp + " compétences");
            roleLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;");
            info.getChildren().addAll(nameLbl, roleLbl);

            // Score bar
            VBox scoreBlock = new VBox(2);
            scoreBlock.setAlignment(Pos.CENTER_RIGHT);
            Label scoreLbl = new Label(u.score + "%");
            scoreLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + avCol + ";");
            StackPane miniBar = new StackPane();
            miniBar.setPrefHeight(4); miniBar.setPrefWidth(70);
            Region miniTrack = new Region();
            miniTrack.setPrefHeight(4); miniTrack.setPrefWidth(70);
            miniTrack.setStyle("-fx-background-color:#F1F5F9;-fx-background-radius:2;");
            Region miniFill = new Region();
            miniFill.setPrefHeight(4);
            miniFill.setPrefWidth(70.0 * u.score / 100.0);
            miniFill.setStyle("-fx-background-color:" + avCol + ";-fx-background-radius:2;");
            StackPane.setAlignment(miniFill, Pos.CENTER_LEFT);
            miniBar.getChildren().addAll(miniTrack, miniFill);
            scoreBlock.getChildren().addAll(scoreLbl, miniBar);

            row.getChildren().addAll(rankLbl, avatar, info, scoreBlock);
            card.getChildren().add(row);
        }
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DATA CLASSES
    // ══════════════════════════════════════════════════════════════════════
    record BarEntry(String label, int value, double ratio, String color) {}
    record PieEntry(String label, int value, String color) {}
    record UserSkillRow(String nom, String role, int nbComp, int score) {}
}