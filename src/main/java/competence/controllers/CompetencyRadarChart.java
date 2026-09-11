package competence.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.*;
import javafx.util.Duration;
import utils.MyDataBase;
import utils.UserSession;

import java.sql.*;
import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║           CompetencyRadarChart — Radar Chart Interactif      ║
 * ║  Visualise les compétences d'un employé sur un graphe        ║
 * ║  radar : niveau actuel vs niveau max, par catégorie.         ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * UTILISATION — popup depuis SkillsMatrix ou CompetencyCatalog :
 *   CompetencyRadarChart.show(employeId, employeNom, ownerStage);
 *
 * ou pour afficher le radar de l'utilisateur connecté :
 *   CompetencyRadarChart.showForCurrentUser(ownerStage);
 */
public class CompetencyRadarChart {

    // ── Dimensions ─────────────────────────────────────────────────────────
    private static final double W         = 560;
    private static final double H         = 520;
    private static final double CX        = W / 2;
    private static final double CY        = H / 2 - 20;
    private static final double RADIUS    = 180;
    private static final int    LEVELS    = 5;

    // ── Couleurs ───────────────────────────────────────────────────────────
    private static final String[] CATEGORY_COLORS = {
            "#6366F1", "#10B981", "#F59E0B", "#EF4444",
            "#3B82F6", "#8B5CF6", "#EC4899", "#14B8A6"
    };

    // ── Data ───────────────────────────────────────────────────────────────
    private final int    employeId;
    private final String employeNom;
    private final List<SkillPoint> skills    = new ArrayList<>();
    private final List<SkillPoint> avgSkills = new ArrayList<>(); // moyenne équipe

    // ── State ──────────────────────────────────────────────────────────────
    private Canvas        canvas;
    private Stage         stage;
    private double        animProgress = 0; // 0..1
    private boolean       showAverage  = false;
    private boolean       showLabels   = true;
    private String        filterCategory = "Toutes";
    private List<SkillPoint> displayedSkills = new ArrayList<>();
    private int           hoveredIndex = -1;

    // ── Model ──────────────────────────────────────────────────────────────
    record SkillPoint(String libelle, String categorie, double niveau, double niveauMax, boolean valide) {
        double ratio() { return niveauMax > 0 ? Math.min(niveau / niveauMax, 1.0) : 0; }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  STATIC FACTORIES
    // ══════════════════════════════════════════════════════════════════════
    public static void show(int employeId, String employeNom, Stage owner) {
        new CompetencyRadarChart(employeId, employeNom).buildAndShow(owner);
    }

    public static void showForCurrentUser(Stage owner) {
        int id = UserSession.getInstance().getUserId();
        if (id <= 0) id = 1;
        String name = UserSession.getInstance().getUser();
        if (name == null || name.isBlank()) name = "Employé";
        show(id, name, owner);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════
    private CompetencyRadarChart(int employeId, String employeNom) {
        this.employeId  = employeId;
        this.employeNom = employeNom;
        loadData();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ══════════════════════════════════════════════════════════════════════
    private void loadData() {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();

            // Compétences de l'employé
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT c.libelle, COALESCE(cat.libelle,'Général') AS categorie, " +
                            "ce.niveauActuel, c.niveauMax, ce.niveauValide " +
                            "FROM competenceemploye ce " +
                            "JOIN competence c ON ce.competence_id = c.id " +
                            "LEFT JOIN categorieCompetence cat ON c.categorie_id = cat.id " +
                            "WHERE ce.employe_id = ? " +
                            "ORDER BY cat.libelle, c.libelle"
            );
            ps.setInt(1, employeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                skills.add(new SkillPoint(
                        rs.getString("libelle"),
                        rs.getString("categorie"),
                        rs.getDouble("niveauActuel"),
                        rs.getDouble("niveauMax") > 0 ? rs.getDouble("niveauMax") : 5,
                        rs.getBoolean("niveauValide")
                ));
            }

            // Moyenne équipe (pour comparaison)
            PreparedStatement ps2 = cnx.prepareStatement(
                    "SELECT c.libelle, COALESCE(cat.libelle,'Général') AS categorie, " +
                            "AVG(ce.niveauActuel) AS avgNiveau, c.niveauMax " +
                            "FROM competenceemploye ce " +
                            "JOIN competence c ON ce.competence_id = c.id " +
                            "LEFT JOIN categorieCompetence cat ON c.categorie_id = cat.id " +
                            "GROUP BY c.id, c.libelle, cat.libelle, c.niveauMax " +
                            "ORDER BY cat.libelle, c.libelle"
            );
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                avgSkills.add(new SkillPoint(
                        rs2.getString("libelle"),
                        rs2.getString("categorie"),
                        rs2.getDouble("avgNiveau"),
                        rs2.getDouble("niveauMax") > 0 ? rs2.getDouble("niveauMax") : 5,
                        false
                ));
            }

        } catch (SQLException e) {
            System.err.println("RadarChart data error: " + e.getMessage());
            // Données de démo si la BDD est vide
            skills.add(new SkillPoint("Java",       "Technique",    4, 5, true));
            skills.add(new SkillPoint("SQL",         "Technique",    3, 5, true));
            skills.add(new SkillPoint("Leadership",  "Management",   2, 5, false));
            skills.add(new SkillPoint("Communication","Soft Skills", 4, 5, true));
            skills.add(new SkillPoint("Agilité",     "Management",   3, 5, false));
            skills.add(new SkillPoint("Python",      "Technique",    2, 5, false));
        }

        displayedSkills = new ArrayList<>(skills);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════════════════════════════════
    private void buildAndShow(Stage owner) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Radar de compétences — " + employeNom);
        stage.setResizable(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F8FAFF;");

        root.setTop(buildHeader());
        root.setCenter(buildChartArea());
        root.setBottom(buildFooter());
        root.setRight(buildLegendPanel());

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 820, 640);
        stage.setScene(scene);
        stage.show();

        applyFilters();
        animateIn();
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: transparent transparent #E2E8F6 transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(46, 46);
        String[] parts = employeNom.trim().split("\\s+");
        String initials = parts.length >= 2
                ? "" + parts[0].charAt(0) + parts[parts.length-1].charAt(0)
                : employeNom.substring(0, Math.min(2, employeNom.length()));
        Circle bg = new Circle(23, Color.web("#EEF2FF"));
        Label initLbl = new Label(initials.toUpperCase());
        initLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#4F46E5;");
        avatar.getChildren().addAll(bg, initLbl);

        VBox info = new VBox(2);
        Label name = new Label("Radar de compétences — " + employeNom);
        name.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Label sub = new Label(displayedSkills.size() + " compétences · cliquez sur un axe pour les détails");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        info.getChildren().addAll(name, sub);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Toggle moyenne équipe
        CheckBox toggleAvg = new CheckBox("Comparer à l'équipe");
        toggleAvg.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
        toggleAvg.setOnAction(e -> {
            showAverage = toggleAvg.isSelected();
            redraw();
        });

        // Bouton export PNG (placeholder)
        Button btnClose = new Button("✕  Fermer");
        btnClose.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#6B7280;" +
                        "-fx-font-size:12px;-fx-cursor:hand;-fx-border-color:#E5E7EB;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8 16;"
        );
        btnClose.setOnAction(e -> stage.close());

        header.getChildren().addAll(avatar, info, spacer, toggleAvg, btnClose);
        return header;
    }

    // ── Chart Area ─────────────────────────────────────────────────────────
    private StackPane buildChartArea() {
        canvas = new Canvas(W, H);
        canvas.setOnMouseMoved(e -> handleHover(e.getX(), e.getY()));
        canvas.setOnMouseExited(e -> { hoveredIndex = -1; redraw(); });

        StackPane pane = new StackPane(canvas);
        pane.setStyle("-fx-background-color: #F8FAFF;");
        pane.setPadding(new Insets(10, 0, 0, 0));
        return pane;
    }

    // ── Legend Panel ───────────────────────────────────────────────────────
    private javafx.scene.layout.StackPane buildLegendPanel() {
        // ── Contenu scrollable ──────────────────────────────────────────
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(20, 16, 20, 8));
        panel.setPrefWidth(204); // légèrement moins large pour la scrollbar

        Label title = new Label("Légende");
        title.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;-fx-padding:0 0 8 0;");
        panel.getChildren().add(title);

        // Niveau colours
        Label levelsTitle = new Label("NIVEAUX");
        levelsTitle.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;-fx-padding:4 0 4 0;");
        panel.getChildren().add(levelsTitle);

        String[][] levels = {
                {"1", "#EF4444", "Débutant"},
                {"2", "#F59E0B", "Élémentaire"},
                {"3", "#3B82F6", "Intermédiaire"},
                {"4", "#10B981", "Avancé"},
                {"5", "#6366F1", "Expert"}
        };
        for (String[] l : levels) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(7, Color.web(l[1]));
            Label lbl = new Label(l[0] + " — " + l[2]);
            lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
            row.getChildren().addAll(dot, lbl);
            panel.getChildren().add(row);
        }

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#E2E8F6;");
        VBox.setMargin(sep, new Insets(8, 0, 8, 0));
        panel.getChildren().add(sep);

        // Stats résumé
        Label statsTitle = new Label("RÉSUMÉ");
        statsTitle.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;-fx-padding:4 0 4 0;");
        panel.getChildren().add(statsTitle);

        // Calculs stats
        if (!skills.isEmpty()) {
            double avg = skills.stream().mapToDouble(SkillPoint::ratio).average().orElse(0) * 100;
            long validated = skills.stream().filter(SkillPoint::valide).count();
            long gaps = skills.stream().filter(s -> s.ratio() < 0.6).count();
            double maxRatio = skills.stream().mapToDouble(SkillPoint::ratio).max().orElse(0);
            String topSkill = skills.stream()
                    .filter(s -> s.ratio() == maxRatio)
                    .map(SkillPoint::libelle)
                    .findFirst().orElse("—");

            panel.getChildren().addAll(
                    buildStatRow("📊", "Score moyen", String.format("%.0f%%", avg)),
                    buildStatRow("✅", "Validées", validated + "/" + skills.size()),
                    buildStatRow("⚠️", "Gaps (< 60%)", String.valueOf(gaps)),
                    buildStatRow("🏆", "Top compétence", topSkill.length() > 14 ? topSkill.substring(0,14)+"…" : topSkill)
            );
        }

        // Séparateur catégories
        Region sep2 = new Region();
        sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color:#E2E8F6;");
        VBox.setMargin(sep2, new Insets(8, 0, 8, 0));
        panel.getChildren().add(sep2);

        // Filtres catégories
        Label catTitle = new Label("CATÉGORIES");
        catTitle.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;-fx-padding:4 0 4 0;");
        panel.getChildren().add(catTitle);

        Set<String> cats = new LinkedHashSet<>();
        cats.add("Toutes");
        skills.forEach(s -> cats.add(s.categorie()));

        ToggleGroup catGroup = new ToggleGroup();
        int colorIdx = 0;
        for (String cat : cats) {
            RadioButton rb = new RadioButton(cat);
            rb.setToggleGroup(catGroup);
            rb.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
            if (cat.equals(filterCategory)) rb.setSelected(true);
            final String finalCat = cat;
            final int ci = colorIdx;
            rb.setOnAction(e -> {
                filterCategory = finalCat;
                applyFilters();
                redraw();
            });
            if (!cat.equals("Toutes")) {
                String color = CATEGORY_COLORS[colorIdx % CATEGORY_COLORS.length];
                Circle catDot = new Circle(5, Color.web(color));
                HBox rbRow = new HBox(6, catDot, rb);
                rbRow.setAlignment(Pos.CENTER_LEFT);
                panel.getChildren().add(rbRow);
            } else {
                panel.getChildren().add(rb);
            }
            colorIdx++;
        }

        // ── ScrollPane wrapper ──────────────────────────────────────────
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefWidth(220);
        scroll.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background: white;" +
                        "-fx-border-color: transparent transparent transparent #E2E8F6;" +
                        "-fx-border-width: 0 0 0 1;"
        );

        // CSS scrollbar légère
        try {
            String css =
                    ".scroll-pane { -fx-background-color: white; -fx-background: white; }\n" +
                            ".scroll-pane > .viewport { -fx-background-color: white; }\n" +
                            ".scroll-pane .scroll-bar:vertical { -fx-background-color: transparent; -fx-pref-width: 6px; }\n" +
                            ".scroll-pane .scroll-bar:vertical .track { -fx-background-color: #F1F5F9; -fx-background-radius: 3; }\n" +
                            ".scroll-pane .scroll-bar:vertical .thumb { -fx-background-color: #C7D2FE; -fx-background-radius: 3; }\n" +
                            ".scroll-pane .scroll-bar:vertical .thumb:hover { -fx-background-color: #6366F1; }\n" +
                            ".scroll-pane .scroll-bar:vertical .increment-button," +
                            ".scroll-pane .scroll-bar:vertical .decrement-button { -fx-pref-height: 0; }";
            java.io.File tmp = java.io.File.createTempFile("radar_legend_sb", ".css");
            tmp.deleteOnExit();
            java.nio.file.Files.writeString(tmp.toPath(), css);
            scroll.getStylesheets().add(tmp.toURI().toURL().toExternalForm());
        } catch (Exception ignored) {}

        // Wrapper pour appliquer la bordure gauche sur le ScrollPane
        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(scroll);
        wrapper.setPrefWidth(220);
        wrapper.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: transparent transparent transparent #E2E8F6;" +
                        "-fx-border-width: 0 0 0 1;"
        );

        return wrapper;
    }

    private HBox buildStatRow(String icon, String label, String value) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 0, 3, 0));
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:11px;");
        VBox texts = new VBox(0);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        texts.getChildren().addAll(lbl, val);
        row.getChildren().addAll(ico, texts);
        return row;
    }

    // ── Footer ─────────────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12, 24, 16, 24));
        footer.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #E2E8F6 transparent transparent transparent;" +
                        "-fx-border-width: 1 0 0 0;"
        );

        Label hint = new Label("💡  Cliquez sur un point pour voir les détails · Survol = info-bulle · Cochez « Comparer à l'équipe » pour voir la moyenne");
        hint.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;");
        footer.getChildren().add(hint);
        return footer;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING — Radar Chart
    // ══════════════════════════════════════════════════════════════════════
    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, W, H);

        int n = displayedSkills.size();
        if (n < 3) {
            drawEmptyState(gc);
            return;
        }

        drawGridAndAxes(gc, n);
        if (showAverage) drawPolygon(gc, n, true);   // moyenne équipe (grisé)
        drawPolygon(gc, n, false);                     // données employé
        drawAxisLabels(gc, n);
        drawPoints(gc, n);
        if (hoveredIndex >= 0) drawTooltip(gc, n);
    }

    // ── Grille ────────────────────────────────────────────────────────────
    private void drawGridAndAxes(GraphicsContext gc, int n) {
        // Cercles de grille
        for (int level = 1; level <= LEVELS; level++) {
            double r = RADIUS * level / LEVELS;
            double alpha = 0.08 + level * 0.04;

            // Anneau coloré selon le niveau
            Color ringColor = levelColor(level);
            gc.setStroke(ringColor);
            gc.setLineWidth(level == LEVELS ? 1.5 : 0.8);
            gc.setGlobalAlpha(level == LEVELS ? 0.35 : 0.2);

            // Polygone de grille (pas un cercle — plus beau sur radar)
            double[] gx = new double[n];
            double[] gy = new double[n];
            for (int i = 0; i < n; i++) {
                double angle = Math.PI * 2 * i / n - Math.PI / 2;
                gx[i] = CX + r * Math.cos(angle);
                gy[i] = CY + r * Math.sin(angle);
            }
            gc.strokePolygon(gx, gy, n);

            // Étiquette niveau
            gc.setGlobalAlpha(0.55);
            gc.setFill(levelColor(level));
            gc.setFont(Font.font("System", FontWeight.NORMAL, 9));
            gc.fillText(String.valueOf(level), CX + 4, CY - r + 4);
        }
        gc.setGlobalAlpha(1.0);

        // Axes
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n - Math.PI / 2;
            double ex = CX + RADIUS * Math.cos(angle);
            double ey = CY + RADIUS * Math.sin(angle);

            gc.setStroke(Color.web("#CBD5E1"));
            gc.setLineWidth(0.8);
            gc.setGlobalAlpha(0.6);
            gc.strokeLine(CX, CY, ex, ey);
        }
        gc.setGlobalAlpha(1.0);

        // Centre
        gc.setFill(Color.web("#6366F1", 0.15));
        gc.fillOval(CX - 5, CY - 5, 10, 10);
        gc.setStroke(Color.web("#6366F1", 0.4));
        gc.setLineWidth(1);
        gc.strokeOval(CX - 5, CY - 5, 10, 10);
    }

    // ── Polygone données ──────────────────────────────────────────────────
    private void drawPolygon(GraphicsContext gc, int n, boolean isAverage) {
        List<SkillPoint> data = isAverage ? getMatchedAvgSkills() : displayedSkills;
        if (data.isEmpty()) return;

        double[] px = new double[n];
        double[] py = new double[n];

        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n - Math.PI / 2;
            double ratio = animProgress * (i < data.size() ? data.get(i).ratio() : 0);
            px[i] = CX + RADIUS * ratio * Math.cos(angle);
            py[i] = CY + RADIUS * ratio * Math.sin(angle);
        }

        if (isAverage) {
            // Moyenne équipe — gris transparent
            gc.setFill(Color.web("#94A3B8", 0.12));
            gc.fillPolygon(px, py, n);
            gc.setStroke(Color.web("#94A3B8", 0.5));
            gc.setLineWidth(1.5);
            gc.setLineDashes(6, 4);
            gc.strokePolygon(px, py, n);
            gc.setLineDashes();
        } else {
            // Dégradé radial pour l'employé
            RadialGradient grad = new RadialGradient(
                    0, 0, CX, CY, RADIUS,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#6366F1", 0.55)),
                    new Stop(1.0, Color.web("#8B5CF6", 0.10))
            );
            gc.setFill(grad);
            gc.fillPolygon(px, py, n);

            // Contour
            gc.setStroke(Color.web("#6366F1", 0.9));
            gc.setLineWidth(2.5);
            gc.strokePolygon(px, py, n);
        }
    }

    // ── Points sur les axes ───────────────────────────────────────────────
    private void drawPoints(GraphicsContext gc, int n) {
        for (int i = 0; i < n; i++) {
            SkillPoint sp = displayedSkills.get(i);
            double angle  = Math.PI * 2 * i / n - Math.PI / 2;
            double ratio  = animProgress * sp.ratio();
            double px     = CX + RADIUS * ratio * Math.cos(angle);
            double py     = CY + RADIUS * ratio * Math.sin(angle);

            boolean hovered = (i == hoveredIndex);
            double  r       = hovered ? 9 : 6;

            // Ombre
            gc.setFill(Color.web("#6366F1", 0.15));
            gc.fillOval(px - r - 1, py - r - 1, (r + 1) * 2, (r + 1) * 2);

            // Point
            Color ptColor = sp.valide() ? Color.web("#10B981") : Color.web("#6366F1");
            if (hovered) ptColor = Color.web("#F59E0B");

            gc.setFill(ptColor);
            gc.fillOval(px - r, py - r, r * 2, r * 2);

            // Bordure blanche
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeOval(px - r, py - r, r * 2, r * 2);

            // Badge validé
            if (sp.valide() && !hovered) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("System", FontWeight.BOLD, 7));
                gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                gc.fillText("✓", px, py + 3);
            }
        }
    }

    // ── Labels des axes ───────────────────────────────────────────────────
    private void drawAxisLabels(GraphicsContext gc, int n) {
        if (!showLabels) return;

        for (int i = 0; i < n; i++) {
            SkillPoint sp = displayedSkills.get(i);
            double angle  = Math.PI * 2 * i / n - Math.PI / 2;
            double padding = 28;
            double lx     = CX + (RADIUS + padding) * Math.cos(angle);
            double ly     = CY + (RADIUS + padding) * Math.sin(angle);

            boolean hovered = (i == hoveredIndex);

            // Fond du label
            String label  = sp.libelle().length() > 14 ? sp.libelle().substring(0, 13) + "…" : sp.libelle();
            double fw     = label.length() * 6.5 + 16;
            double fh     = 22;
            double fx     = lx - fw / 2;
            double fy     = ly - fh / 2;

            if (hovered) {
                gc.setFill(Color.web("#6366F1"));
                gc.setEffect(null);
            } else {
                gc.setFill(Color.web("#F1F5F9"));
            }
            gc.fillRoundRect(fx, fy, fw, fh, 11, 11);

            // Texte
            gc.setFill(hovered ? Color.WHITE : Color.web("#374151"));
            gc.setFont(Font.font("System", hovered ? FontWeight.BOLD : FontWeight.NORMAL, 10));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            gc.fillText(label, lx, ly);

            // Niveau sous le label
            gc.setFill(Color.web(levelColorHex(sp)));
            gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            gc.fillText((int) sp.niveau() + "/" + (int) sp.niveauMax(), lx, ly + 14);
        }
    }

    // ── Tooltip au survol ─────────────────────────────────────────────────
    private void drawTooltip(GraphicsContext gc, int n) {
        if (hoveredIndex < 0 || hoveredIndex >= displayedSkills.size()) return;
        SkillPoint sp = displayedSkills.get(hoveredIndex);

        double angle = Math.PI * 2 * hoveredIndex / n - Math.PI / 2;
        double ratio = sp.ratio();
        double px    = CX + RADIUS * ratio * Math.cos(angle);
        double py    = CY + RADIUS * ratio * Math.sin(angle);

        // Contenu tooltip
        String[] lines = {
                sp.libelle(),
                "Niveau : " + (int)sp.niveau() + " / " + (int)sp.niveauMax() + "  (" + String.format("%.0f%%", ratio*100) + ")",
                "Catégorie : " + sp.categorie(),
                sp.valide() ? "✓ Niveau validé" : "○ En attente de validation"
        };

        double tw = 190, th = lines.length * 18 + 16;
        double tx = px + 14;
        double ty = py - th / 2;
        if (tx + tw > W - 10)  tx = px - tw - 14;
        if (ty < 10)           ty = 10;
        if (ty + th > H - 10)  ty = H - th - 10;

        // Ombre tooltip
        gc.setFill(Color.web("#000000", 0.08));
        gc.fillRoundRect(tx + 2, ty + 2, tw, th, 12, 12);

        // Fond tooltip
        gc.setFill(Color.web("#1E293B", 0.95));
        gc.fillRoundRect(tx, ty, tw, th, 12, 12);

        // Accent coloré gauche
        gc.setFill(Color.web(levelColorHex(sp)));
        gc.fillRoundRect(tx, ty, 4, th, 2, 2);

        // Textes
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        for (int i = 0; i < lines.length; i++) {
            double textY = ty + 14 + i * 18;
            if (i == 0) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            } else if (i == lines.length - 1) {
                gc.setFill(Color.web(sp.valide() ? "#10B981" : "#94A3B8"));
                gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            } else {
                gc.setFill(Color.web("#CBD5E1"));
                gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            }
            gc.fillText(lines[i], tx + 10, textY);
        }
    }

    // ── Empty state ───────────────────────────────────────────────────────
    private void drawEmptyState(GraphicsContext gc) {
        gc.setFill(Color.web("#9CA3AF", 0.5));
        gc.setFont(Font.font("System", FontWeight.NORMAL, 14));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("Aucune compétence à afficher", CX, CY - 10);
        gc.setFont(Font.font("System", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#9CA3AF", 0.4));
        gc.fillText("Ajoutez des compétences dans la matrice", CX, CY + 14);

        // Cercle fantôme
        gc.setStroke(Color.web("#E2E8F6", 0.8));
        gc.setLineWidth(1);
        gc.setLineDashes(6, 4);
        gc.strokeOval(CX - RADIUS, CY - RADIUS, RADIUS * 2, RADIUS * 2);
        gc.setLineDashes();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INTERACTION
    // ══════════════════════════════════════════════════════════════════════
    private void handleHover(double mx, double my) {
        int n = displayedSkills.size();
        int newHovered = -1;
        double minDist = 20;

        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n - Math.PI / 2;
            double ratio = displayedSkills.get(i).ratio();
            double px = CX + RADIUS * ratio * Math.cos(angle);
            double py = CY + RADIUS * ratio * Math.sin(angle);
            double dist = Math.sqrt((mx - px) * (mx - px) + (my - py) * (my - py));
            if (dist < minDist) {
                minDist = dist;
                newHovered = i;
            }
        }

        if (newHovered != hoveredIndex) {
            hoveredIndex = newHovered;
            redraw();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANIMATION
    // ══════════════════════════════════════════════════════════════════════
    private void animateIn() {
        animProgress = 0;
        Timeline tl = new Timeline();
        int steps = 60;
        for (int i = 0; i <= steps; i++) {
            final double progress = easeOutBack((double) i / steps);
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * 14),
                    e -> { animProgress = progress; redraw(); }
            ));
        }
        tl.play();
    }

    private double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FILTERS
    // ══════════════════════════════════════════════════════════════════════
    private void applyFilters() {
        if ("Toutes".equals(filterCategory)) {
            displayedSkills = new ArrayList<>(skills);
        } else {
            displayedSkills = skills.stream()
                    .filter(s -> s.categorie().equals(filterCategory))
                    .collect(java.util.stream.Collectors.toList());
        }
        // Max 12 compétences pour lisibilité du radar
        if (displayedSkills.size() > 12) {
            displayedSkills = displayedSkills.subList(0, 12);
        }
    }

    private List<SkillPoint> getMatchedAvgSkills() {
        List<SkillPoint> matched = new ArrayList<>();
        for (SkillPoint ds : displayedSkills) {
            avgSkills.stream()
                    .filter(a -> a.libelle().equals(ds.libelle()))
                    .findFirst()
                    .ifPresentOrElse(matched::add,
                            () -> matched.add(new SkillPoint(ds.libelle(), ds.categorie(), 0, ds.niveauMax(), false)));
        }
        return matched;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COULEURS NIVEAU
    // ══════════════════════════════════════════════════════════════════════
    private Color levelColor(int level) {
        return switch (level) {
            case 1 -> Color.web("#EF4444");
            case 2 -> Color.web("#F59E0B");
            case 3 -> Color.web("#3B82F6");
            case 4 -> Color.web("#10B981");
            case 5 -> Color.web("#6366F1");
            default -> Color.web("#CBD5E1");
        };
    }

    private String levelColorHex(SkillPoint sp) {
        double ratio = sp.ratio();
        if (ratio >= 0.9) return "#6366F1";
        if (ratio >= 0.7) return "#10B981";
        if (ratio >= 0.5) return "#3B82F6";
        if (ratio >= 0.3) return "#F59E0B";
        return "#EF4444";
    }
}