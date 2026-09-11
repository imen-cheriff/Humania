package competence.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.Duration;
import utils.MyDataBase;
import utils.UserSession;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║          SkillTreeRPG — Arbre de Compétences RPG             ║
 * ║  Visualise les compétences comme un arbre de talent          ║
 * ║  style RPG : nœuds débloqués/verrouillés, prérequis,        ║
 * ║  animations, zoom, catégories colorées.                      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * UTILISATION :
 *   SkillTreeRPG.show(ownerStage);
 *   SkillTreeRPG.showForEmployee(employeId, ownerStage);
 */
public class SkillTreeRPG {

    // ── Layout constants ──────────────────────────────────────────────────
    private static final double NODE_R      = 36;
    private static final double H_GAP       = 160;
    private static final double V_GAP       = 120;
    private static final double CANVAS_PAD  = 80;

    // ── Colours ───────────────────────────────────────────────────────────
    private static final String BG_DARK     = "#F8FAFF";
    private static final String BG_GRID     = "#FFFFFF";
    private static final String[] CAT_COLS  = {
            "#6366F1","#10B981","#F59E0B","#EF4444",
            "#3B82F6","#EC4899","#14B8A6","#8B5CF6"
    };

    // ── State ─────────────────────────────────────────────────────────────
    private Stage            stage;
    private Canvas           canvas;
    private GraphicsContext  gc;
    private double           offsetX = 0, offsetY = 0;
    private double           scale   = 1.0;
    private double           dragStartX, dragStartY;
    private SkillNode        hoveredNode = null;
    private SkillNode        selectedNode = null;
    private VBox             detailPanel;

    private final int        employeId;
    private final List<SkillNode>  nodes    = new ArrayList<>();
    private final List<SkillEdge>  edges    = new ArrayList<>();
    private final Map<String,Integer> catMap = new LinkedHashMap<>();

    // Animation
    private final Map<Integer, Double> glowAnimValues = new HashMap<>();
    private Timeline globalAnim;

    // ── Tooltip overlay ───────────────────────────────────────────────────
    private double   tooltipX, tooltipY;
    private boolean  showTooltip = false;

    // ══════════════════════════════════════════════════════════════════════
    //  FACTORIES
    // ══════════════════════════════════════════════════════════════════════
    public static void show(Stage owner) {
        int uid = UserSession.getInstance().getUserId();
        new SkillTreeRPG(uid > 0 ? uid : 1).buildAndShow(owner);
    }

    public static void showForEmployee(int employeId, Stage owner) {
        new SkillTreeRPG(employeId).buildAndShow(owner);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════
    private SkillTreeRPG(int employeId) {
        this.employeId = employeId;
        loadData();
        buildTree();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DATA
    // ══════════════════════════════════════════════════════════════════════
    private void loadData() {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();

            // Catégories
            ResultSet rsCat = cnx.prepareStatement(
                    "SELECT id, libelle, IFNULL(couleur,'#6366F1') AS couleur " +
                            "FROM categorieCompetence ORDER BY libelle"
            ).executeQuery();
            int catIdx = 0;
            while (rsCat.next()) {
                catMap.put(rsCat.getString("libelle"), catIdx++);
            }

            // Compétences + niveau employé
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT c.id, c.libelle, c.niveauMax, c.typeCompetence, " +
                            "IFNULL(cat.libelle,'Général') AS categorie, " +
                            "IFNULL(ce.niveauActuel, 0) AS niveauActuel, " +
                            "IFNULL(ce.niveauValide, 0) AS niveauValide " +
                            "FROM competence c " +
                            "LEFT JOIN categorieCompetence cat ON c.categorie_id = cat.id " +
                            "LEFT JOIN competenceemploye ce ON ce.competence_id = c.id AND ce.employe_id = ? " +
                            "WHERE IFNULL(c.statutCompetence,'ACTIF') = 'ACTIF' " +
                            "ORDER BY cat.libelle, c.typeCompetence DESC, c.libelle"
            );
            ps.setInt(1, employeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int    niveauMax    = Math.max(1, rs.getInt("niveauMax"));
                int    niveauActuel = rs.getInt("niveauActuel");
                boolean valide      = rs.getBoolean("niveauValide");
                String  cat         = rs.getString("categorie");
                int     catIndex    = catMap.getOrDefault(cat, 0);
                String  color       = CAT_COLS[catIndex % CAT_COLS.length];
                String  type        = rs.getString("typeCompetence");

                NodeState state;
                if (niveauActuel >= niveauMax)       state = NodeState.MASTERED;
                else if (niveauActuel > 0)            state = NodeState.UNLOCKED;
                else if ("CRITIQUE".equals(type))     state = NodeState.AVAILABLE;
                else                                  state = NodeState.LOCKED;

                nodes.add(new SkillNode(
                        rs.getInt("id"),
                        rs.getString("libelle"),
                        cat, type, color,
                        niveauActuel, niveauMax, valide, state
                ));
            }
        } catch (SQLException e) {
            // Données démo si BDD vide
            loadDemoData();
        }

        if (nodes.isEmpty()) loadDemoData();
    }

    private void loadDemoData() {
        String[][] demo = {
                {"1","Java","Technique","CRITIQUE","#6366F1","4","5"},
                {"2","SQL","Technique","IMPORTANTE","#6366F1","3","5"},
                {"3","Spring Boot","Technique","IMPORTANTE","#6366F1","2","5"},
                {"4","Docker","Technique","UTILE","#6366F1","1","5"},
                {"5","Kubernetes","Technique","OPTIONNELLE","#6366F1","0","5"},
                {"6","Leadership","Management","CRITIQUE","#10B981","3","5"},
                {"7","Communication","Soft Skills","IMPORTANTE","#F59E0B","4","5"},
                {"8","Agilité","Management","UTILE","#10B981","2","5"},
                {"9","Python","Technique","UTILE","#3B82F6","2","5"},
                {"10","Machine Learning","Technique","OPTIONNELLE","#3B82F6","0","5"},
        };
        for (String[] d : demo) {
            int niv = Integer.parseInt(d[5]), max = Integer.parseInt(d[6]);
            NodeState st = niv >= max ? NodeState.MASTERED : niv > 0 ? NodeState.UNLOCKED
                    : "CRITIQUE".equals(d[3]) ? NodeState.AVAILABLE : NodeState.LOCKED;
            nodes.add(new SkillNode(Integer.parseInt(d[0]),d[1],d[2],d[3],d[4],niv,max,false,st));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TREE LAYOUT — radial par catégorie
    // ══════════════════════════════════════════════════════════════════════
    private void buildTree() {
        // Grouper par catégorie
        Map<String, List<SkillNode>> byCategory = nodes.stream()
                .collect(Collectors.groupingBy(n -> n.categorie, LinkedHashMap::new, Collectors.toList()));

        int numCats = byCategory.size();
        if (numCats == 0) return;

        // Centre de l'arbre
        double centerX = 0, centerY = 0;

        // Nœud racine "Mes Compétences"
        SkillNode root = new SkillNode(-1, "Mes\nCompétences", "ROOT", "ROOT",
                "#6366F1", 0, 1, false, NodeState.MASTERED);
        root.x = centerX; root.y = centerY;
        nodes.add(0, root);

        // Disposer les catégories en cercle autour du centre
        int catIdx = 0;
        for (Map.Entry<String, List<SkillNode>> entry : byCategory.entrySet()) {
            String catName        = entry.getKey();
            List<SkillNode> group = entry.getValue();

            // Angle de la branche de cette catégorie
            double catAngle = (2 * Math.PI * catIdx / numCats) - Math.PI / 2;

            // Nœud de catégorie
            double catX = centerX + Math.cos(catAngle) * 220;
            double catY = centerY + Math.sin(catAngle) * 220;

            SkillNode catNode = new SkillNode(
                    -100 - catIdx, catName, "CAT", "ROOT",
                    CAT_COLS[catIdx % CAT_COLS.length], 1, 1, false, NodeState.MASTERED
            );
            catNode.x = catX; catNode.y = catY;
            nodes.add(catNode);

            // Edge racine → catégorie
            edges.add(new SkillEdge(root, catNode, true));

            // Skill nodes disposés en éventail autour du nœud catégorie
            int n = group.size();
            double spreadAngle = Math.min(Math.PI * 0.7, n * 0.35);
            for (int i = 0; i < n; i++) {
                SkillNode skill = group.get(i);
                double spreadOffset = n > 1 ? (i / (double)(n-1) - 0.5) * spreadAngle : 0;
                double skillAngle   = catAngle + spreadOffset;

                // Niveaux : CRITIQUE plus proche, OPTIONNELLE plus loin
                double dist = switch (skill.typeCompetence) {
                    case "CRITIQUE"    -> 160;
                    case "IMPORTANTE"  -> 220;
                    case "UTILE"       -> 280;
                    default            -> 340;
                };

                skill.x = catX + Math.cos(skillAngle) * dist;
                skill.y = catY + Math.sin(skillAngle) * dist;
                edges.add(new SkillEdge(catNode, skill, skill.state != NodeState.LOCKED));
            }
            catIdx++;
        }

        // Centrer le layout
        double minX = nodes.stream().mapToDouble(n -> n.x).min().orElse(0);
        double minY = nodes.stream().mapToDouble(n -> n.y).min().orElse(0);
        double maxX = nodes.stream().mapToDouble(n -> n.x).max().orElse(0);
        double maxY = nodes.stream().mapToDouble(n -> n.y).max().orElse(0);

        double treeCenterX = (minX + maxX) / 2;
        double treeCenterY = (minY + maxY) / 2;
        for (SkillNode nd : nodes) { nd.x -= treeCenterX; nd.y -= treeCenterY; }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════════════════════════════════
    private void buildAndShow(Stage owner) {
        stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.initOwner(owner);
        stage.setTitle("Skill Tree — Arbre de Compétences RPG");
        stage.setMaximized(true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");
        root.setTop(buildTopBar());
        root.setCenter(buildCanvasArea());
        root.setRight(buildDetailPanel());
        root.setBottom(buildLegendBar());

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1280, 800);
        scene.setFill(Color.web("#F8FAFF"));
        stage.setScene(scene);
        stage.show();

        // Init offset = centre
        Platform.runLater(() -> {
            offsetX = canvas.getWidth() / 2;
            offsetY = canvas.getHeight() / 2;
            startGlobalAnimation();
            redraw();
        });
    }

    // ── Top Bar ────────────────────────────────────────────────────────────
    private HBox buildTopBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: transparent transparent #E2E8F6 transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Logo RPG
        StackPane logo = new StackPane();
        logo.setPrefSize(38, 38);
        Circle logoBg = new Circle(19, Color.web("#6366F1"));
        Label logoIco = new Label("✦");
        logoIco.setStyle("-fx-font-size: 18px; -fx-text-fill: #1E293B;");
        logo.getChildren().addAll(logoBg, logoIco);

        VBox titleBlock = new VBox(2);
        Label title = new Label("Skill Tree");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label sub = new Label("Arbre de compétences · Style RPG");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
        titleBlock.getChildren().addAll(title, sub);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stats rapides
        long unlocked = nodes.stream().filter(n -> n.state == NodeState.UNLOCKED || n.state == NodeState.MASTERED).filter(n -> n.id > 0).count();
        long mastered  = nodes.stream().filter(n -> n.state == NodeState.MASTERED).filter(n -> n.id > 0).count();
        long total     = nodes.stream().filter(n -> n.id > 0 && !"CAT".equals(n.typeCompetence)).count();

        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
                buildTopStat("✦ " + mastered, "Maîtrisées", "#6366F1"),
                buildTopStat("⚡ " + unlocked, "Débloquées", "#10B981"),
                buildTopStat("🔒 " + (total - unlocked), "Verrouillées", "#94A3B8"),
                buildTopStat("📊 " + (total > 0 ? (int)(mastered*100/total) : 0) + "%", "Progression", "#F59E0B")
        );

        // Boutons contrôles
        Button btnReset = makeTopBtn("⌖ Centrer", "#F1F5F9");
        btnReset.setOnAction(e -> {
            offsetX = canvas.getWidth() / 2;
            offsetY = canvas.getHeight() / 2;
            scale = 1.0;
            redraw();
        });

        Button btnZoomIn  = makeTopBtn("＋", "#F1F5F9");
        Button btnZoomOut = makeTopBtn("－", "#F1F5F9");
        btnZoomIn.setOnAction(e  -> { scale = Math.min(2.5, scale * 1.15); redraw(); });
        btnZoomOut.setOnAction(e -> { scale = Math.max(0.3, scale / 1.15); redraw(); });

        Button btnClose = makeTopBtn("✕ Fermer", "#F1F5F9");
        btnClose.setOnAction(e -> stage.close());

        bar.getChildren().addAll(logo, titleBlock, spacer, stats, btnReset, btnZoomIn, btnZoomOut, btnClose);
        return bar;
    }

    private HBox buildTopStat(String value, String label, String color) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(6, 12, 6, 12));
        box.setStyle(
                "-fx-background-color: " + color + "18;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + color + "33;" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"
        );
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private Button makeTopBtn(String text, String bg) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + bg + "; -fx-text-fill: #9CA3AF;" +
                        "-fx-font-size: 12px; -fx-cursor: hand;" +
                        "-fx-background-radius: 8; -fx-border-color: #E2E8F6;" +
                        "-fx-border-radius: 8; -fx-padding: 7 14;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #E2E8F6; -fx-text-fill: #1E293B;" +
                        "-fx-font-size: 12px; -fx-cursor: hand;" +
                        "-fx-background-radius: 8; -fx-border-color: #94A3B8;" +
                        "-fx-border-radius: 8; -fx-padding: 7 14;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bg + "; -fx-text-fill: #9CA3AF;" +
                        "-fx-font-size: 12px; -fx-cursor: hand;" +
                        "-fx-background-radius: 8; -fx-border-color: #E2E8F6;" +
                        "-fx-border-radius: 8; -fx-padding: 7 14;"
        ));
        return btn;
    }

    // ── Canvas Area ────────────────────────────────────────────────────────
    private Pane buildCanvasArea() {
        canvas = new Canvas(900, 700);
        gc     = canvas.getGraphicsContext2D();

        Pane pane = new Pane(canvas);
        pane.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Resize canvas with pane
        pane.widthProperty().addListener((obs, o, nw) -> {
            canvas.setWidth(nw.doubleValue());
            redraw();
        });
        pane.heightProperty().addListener((obs, o, nh) -> {
            canvas.setHeight(nh.doubleValue());
            redraw();
        });

        // Mouse interactions
        final double[] lastMouse = {0, 0};

        pane.setOnMousePressed(e -> {
            lastMouse[0] = e.getX(); lastMouse[1] = e.getY();
        });

        pane.setOnMouseDragged(e -> {
            offsetX += e.getX() - lastMouse[0];
            offsetY += e.getY() - lastMouse[1];
            lastMouse[0] = e.getX(); lastMouse[1] = e.getY();
            redraw();
        });

        pane.setOnMouseMoved(e -> {
            SkillNode hit = hitTest(e.getX(), e.getY());
            if (hit != hoveredNode) {
                hoveredNode = hit;
                tooltipX = e.getX(); tooltipY = e.getY();
                showTooltip = (hit != null && hit.id > 0);
                redraw();
            }
        });

        pane.setOnMouseClicked(e -> {
            SkillNode hit = hitTest(e.getX(), e.getY());
            if (hit != null && hit.id > 0) {
                selectedNode = hit;
                updateDetailPanel(hit);
                redraw();
            }
        });

        pane.setOnScroll((ScrollEvent e) -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            // Zoom vers la souris
            double mx = e.getX(), my = e.getY();
            offsetX = mx - (mx - offsetX) * factor;
            offsetY = my - (my - offsetY) * factor;
            scale   = Math.max(0.3, Math.min(2.5, scale * factor));
            redraw();
        });

        return pane;
    }

    // ── Detail Panel ────────────────────────────────────────────────────────
    private VBox buildDetailPanel() {
        detailPanel = new VBox(14);
        detailPanel.setPrefWidth(260);
        detailPanel.setPadding(new Insets(20, 16, 20, 16));
        detailPanel.setStyle(
                "-fx-background-color: #F8FAFF;" +
                        "-fx-border-color: #E2E8F6 transparent transparent transparent;" +
                        "-fx-border-width: 0 0 0 1;"
        );

        Label hint = new Label("Cliquez sur un\nnœud pour les détails");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-alignment: center;");
        hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        hint.setWrapText(true);
        detailPanel.getChildren().add(hint);

        return detailPanel;
    }

    private void updateDetailPanel(SkillNode node) {
        detailPanel.getChildren().clear();

        // Titre avec couleur du nœud
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(8, Color.web(node.color));
        Label name = new Label(node.libelle);
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        name.setWrapText(true);
        titleRow.getChildren().addAll(dot, name);
        detailPanel.getChildren().add(titleRow);

        // Catégorie badge
        Label catBadge = new Label("📂 " + node.categorie);
        catBadge.setStyle(
                "-fx-background-color: " + node.color + "22;" +
                        "-fx-text-fill: " + node.color + ";" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-padding: 4 10; -fx-background-radius: 12;"
        );
        detailPanel.getChildren().add(catBadge);

        // Séparateur
        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #E2E8F6; -fx-opacity: 0.8;");
        detailPanel.getChildren().add(sep);

        // État
        String stateLabel = switch (node.state) {
            case MASTERED  -> "🏆 Maîtrisée";
            case UNLOCKED  -> "⚡ En progression";
            case AVAILABLE -> "✨ Disponible";
            case LOCKED    -> "🔒 Verrouillée";
        };
        String stateColor = switch (node.state) {
            case MASTERED  -> "#F59E0B";
            case UNLOCKED  -> "#10B981";
            case AVAILABLE -> "#6366F1";
            case LOCKED    -> "#94A3B8";
        };
        Label stateLbl = new Label(stateLabel);
        stateLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + stateColor + ";");
        detailPanel.getChildren().add(stateLbl);

        // Niveau progress
        Label levelTitle = new Label("Niveau actuel");
        levelTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        detailPanel.getChildren().add(levelTitle);

        HBox levelRow = new HBox(6);
        levelRow.setAlignment(Pos.CENTER_LEFT);
        Label levelNum = new Label(node.niveauActuel + " / " + node.niveauMax);
        levelNum.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + node.color + ";");
        levelRow.getChildren().add(levelNum);
        detailPanel.getChildren().add(levelRow);

        // Barre de progression
        StackPane barWrap = new StackPane();
        barWrap.setPrefHeight(8); barWrap.setMaxWidth(Double.MAX_VALUE);
        Region track = new Region(); track.setPrefHeight(8);
        track.setStyle("-fx-background-color: #E2E8F0; -fx-background-radius: 4;");
        track.prefWidthProperty().bind(barWrap.widthProperty());
        double ratio = node.niveauMax > 0 ? (double)node.niveauActuel / node.niveauMax : 0;
        Region fill  = new Region(); fill.setPrefHeight(8);
        fill.setStyle("-fx-background-color: " + node.color + "; -fx-background-radius: 4;");
        barWrap.widthProperty().addListener((obs, o, nw) ->
                fill.setPrefWidth(nw.doubleValue() * ratio));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        barWrap.getChildren().addAll(track, fill);
        detailPanel.getChildren().add(barWrap);

        // Type importance
        String typeLabel = switch (node.typeCompetence) {
            case "CRITIQUE"   -> "🔴 Critique";
            case "IMPORTANTE" -> "🟠 Importante";
            case "UTILE"      -> "🟡 Utile";
            default           -> "⚪ Optionnelle";
        };
        Label typeLbl = new Label(typeLabel);
        typeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        detailPanel.getChildren().add(typeLbl);

        if (node.valide) {
            Label validLbl = new Label("✅ Niveau validé par RH");
            validLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981;");
            detailPanel.getChildren().add(validLbl);
        }

        // Étoiles de maîtrise
        HBox stars = new HBox(4);
        stars.setAlignment(Pos.CENTER_LEFT);
        Label starsTitle = new Label("Maîtrise : ");
        starsTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        stars.getChildren().add(starsTitle);
        for (int i = 1; i <= node.niveauMax; i++) {
            Label star = new Label(i <= node.niveauActuel ? "★" : "☆");
            star.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (i <= node.niveauActuel ? "#F59E0B" : "#E2E8F6") + ";");
            stars.getChildren().add(star);
        }
        detailPanel.getChildren().add(stars);

        // Conseil IA-like
        Region sep2 = new Region(); sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color: #E2E8F6; -fx-opacity: 0.8;");
        detailPanel.getChildren().add(sep2);

        String conseil = generateAdvice(node);
        Label adviceBox = new Label(conseil);
        adviceBox.setWrapText(true);
        adviceBox.setStyle(
                "-fx-background-color: #EEF2FF;" +
                        "-fx-text-fill: #4F46E5;" +
                        "-fx-font-size: 11px; -fx-padding: 10;" +
                        "-fx-background-radius: 8;"
        );
        detailPanel.getChildren().add(adviceBox);
    }

    private String generateAdvice(SkillNode node) {
        return switch (node.state) {
            case MASTERED  -> "🏆 Félicitations ! Vous avez maîtrisé cette compétence. Pensez à la partager avec votre équipe !";
            case UNLOCKED  -> "⚡ Bonne progression ! Continuez vos formations pour atteindre le niveau maximal.";
            case AVAILABLE -> "✨ Cette compétence est disponible. Inscrivez-vous à une formation pour la débloquer !";
            case LOCKED    -> "🔒 Progressez d'abord dans les compétences de niveau inférieur pour débloquer celle-ci.";
        };
    }

    // ── Legend Bar ──────────────────────────────────────────────────────────
    private HBox buildLegendBar() {
        HBox bar = new HBox(24);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(8, 24, 8, 24));
        bar.setStyle(
                "-fx-background-color: #F1F5F9;" +
                        "-fx-border-color: #E2E8F6 transparent transparent transparent;" +
                        "-fx-border-width: 1 0 0 0;"
        );

        String[][] legend = {
                {"#F59E0B", "Maîtrisée"},
                {"#6366F1", "En progression"},
                {"#10B981", "Disponible"},
                {"#94A3B8", "Verrouillée"}
        };
        for (String[] l : legend) {
            HBox item = new HBox(6);
            item.setAlignment(Pos.CENTER);
            Circle dot = new Circle(7, Color.web(l[0]));
            Label lbl = new Label(l[1]);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
            item.getChildren().addAll(dot, lbl);
            bar.getChildren().add(item);
        }

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label hint = new Label("🖱 Glisser pour déplacer  ·  Molette pour zoomer  ·  Clic sur un nœud pour les détails");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        bar.getChildren().addAll(sp, hint);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING
    // ══════════════════════════════════════════════════════════════════════
    private void redraw() {
        double W = canvas.getWidth(), H = canvas.getHeight();
        gc.clearRect(0, 0, W, H);

        // ── Background ────────────────────────────────────────────────
        gc.setFill(Color.web("#F8FAFF"));
        gc.fillRect(0, 0, W, H);
        drawGrid(W, H);

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        // ── Edges ─────────────────────────────────────────────────────
        for (SkillEdge edge : edges) drawEdge(edge);

        // ── Nodes ─────────────────────────────────────────────────────
        // Draw non-selected first, then selected on top
        for (SkillNode node : nodes) if (node != selectedNode) drawNode(node, false);
        if (selectedNode != null) drawNode(selectedNode, true);

        gc.restore();

        // ── Tooltip ───────────────────────────────────────────────────
        if (showTooltip && hoveredNode != null) drawTooltip(W, H);
    }

    private void drawGrid(double W, double H) {
        gc.setStroke(Color.web("#CBD5E1", 0.5));
        gc.setLineWidth(0.5);
        double gridSize = 40 * scale;
        double startX   = offsetX % gridSize;
        double startY   = offsetY % gridSize;
        for (double x = startX; x < W; x += gridSize) gc.strokeLine(x, 0, x, H);
        for (double y = startY; y < H; y += gridSize) gc.strokeLine(0, y, W, y);
    }

    private void drawEdge(SkillEdge edge) {
        double x1 = edge.from.x, y1 = edge.from.y;
        double x2 = edge.to.x,   y2 = edge.to.y;

        // Courbe de Bézier
        double mx = (x1 + x2) / 2, my = (y1 + y2) / 2;

        Color edgeColor = edge.active
                ? Color.web(edge.to.color, 0.5)
                : Color.web("#94A3B8", 0.35);

        gc.setStroke(edgeColor);
        gc.setLineWidth(edge.active ? 2 : 1);
        gc.setLineDashes(edge.active ? 0 : 6, 4);

        gc.beginPath();
        gc.moveTo(x1, y1);
        gc.bezierCurveTo(mx, y1, mx, y2, x2, y2);
        gc.stroke();
        gc.setLineDashes();

        // Flèche au bout si actif
        if (edge.active) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            double arrowX = x2 - Math.cos(angle) * NODE_R;
            double arrowY = y2 - Math.sin(angle) * NODE_R;
            gc.setFill(edgeColor);
            gc.beginPath();
            gc.moveTo(arrowX, arrowY);
            gc.lineTo(arrowX - 8 * Math.cos(angle - 0.4), arrowY - 8 * Math.sin(angle - 0.4));
            gc.lineTo(arrowX - 8 * Math.cos(angle + 0.4), arrowY - 8 * Math.sin(angle + 0.4));
            gc.closePath();
            gc.fill();
        }
    }

    private void drawNode(SkillNode node, boolean selected) {
        double x = node.x, y = node.y;
        boolean isRoot    = node.id < 0 && !"CAT".equals(node.typeCompetence);
        boolean isCat     = "CAT".equals(node.typeCompetence);
        boolean isHovered = (node == hoveredNode);
        double  r         = isRoot ? NODE_R * 1.4 : isCat ? NODE_R * 0.85 : NODE_R;

        // Glow animation
        double glowAlpha = glowAnimValues.getOrDefault(node.id, 0.0);

        // ── Outer glow ────────────────────────────────────────────────
        if (node.state == NodeState.MASTERED || node.state == NodeState.UNLOCKED || isRoot || isCat) {
            for (int g = 5; g >= 1; g--) {
                double glowR    = r + g * 6 + glowAlpha * 4;
                double glowOpacity = (0.08 - g * 0.012) + glowAlpha * 0.06;
                gc.setFill(Color.web(node.color, Math.max(0, glowOpacity)));
                gc.fillOval(x - glowR, y - glowR, glowR * 2, glowR * 2);
            }
        }

        // ── Selection ring ────────────────────────────────────────────
        if (selected) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2.5);
            gc.setLineDashes(5, 4);
            gc.strokeOval(x - r - 8, y - r - 8, (r + 8) * 2, (r + 8) * 2);
            gc.setLineDashes();
        }

        // ── Shadow ────────────────────────────────────────────────────
        gc.setFill(Color.web("#000000", 0.4));
        gc.fillOval(x - r + 3, y - r + 4, r * 2, r * 2);

        // ── Background fill ───────────────────────────────────────────
        Color fillColor = switch (node.state) {
            case MASTERED  -> Color.web(node.color);
            case UNLOCKED  -> Color.web(node.color, 0.65);
            case AVAILABLE -> Color.web(node.color, 0.35);
            case LOCKED    -> Color.web("#E5E9F0");
        };

        if (isRoot) fillColor = Color.web("#4F46E5");
        if (isCat)  fillColor = Color.web(node.color, 0.8);

        // Gradient radial
        RadialGradient grad = new RadialGradient(
                0, 0, x - r * 0.3, y - r * 0.3, r * 1.2,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, fillColor.brighter()),
                new Stop(1, fillColor.darker())
        );
        gc.setFill(grad);
        gc.fillOval(x - r, y - r, r * 2, r * 2);

        // ── Border ────────────────────────────────────────────────────
        Color borderColor = isHovered ? Color.WHITE :
                node.state == NodeState.LOCKED ? Color.web("#94A3B8") : Color.web(node.color);
        gc.setStroke(borderColor);
        gc.setLineWidth(isHovered ? 2.5 : 1.5);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);

        // ── Icon / Text ───────────────────────────────────────────────
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        if (isRoot) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.fillText("✦", x, y - 6);
            gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            gc.fillText("SKILLS", x, y + 7);
        } else if (isCat) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            String catShort = node.libelle.length() > 10 ? node.libelle.substring(0, 9) + "…" : node.libelle;
            gc.fillText(catShort, x, y);
        } else {
            // Icône selon état
            String ico = switch (node.state) {
                case MASTERED  -> "★";
                case UNLOCKED  -> "◈";
                case AVAILABLE -> "◇";
                case LOCKED    -> "🔒";
            };
            Color textColor = node.state == NodeState.LOCKED ? Color.web("#64748B") : Color.WHITE;
            gc.setFill(textColor);
            gc.setFont(Font.font("System", FontWeight.BOLD, 16));
            gc.fillText(ico, x, y - 8);

            // Niveau
            if (node.state != NodeState.LOCKED) {
                gc.setFont(Font.font("System", FontWeight.BOLD, 9));
                gc.setFill(Color.web("#FFFFFF", 0.85));
                gc.fillText(node.niveauActuel + "/" + node.niveauMax, x, y + 10);
            }
        }

        // ── Label sous le nœud ────────────────────────────────────────
        if (!isRoot) {
            String labelText = node.libelle.length() > 16
                    ? node.libelle.substring(0, 14) + "…" : node.libelle;

            // Fond du label
            double lw = labelText.length() * 6.0 + 14;
            double lh = 18;
            double lx = x - lw / 2;
            double ly = y + r + 6;

            Color labelBg = node.state == NodeState.LOCKED
                    ? Color.web("#F1F5F9", 0.85) : Color.web(node.color, 0.2);
            gc.setFill(labelBg);
            gc.fillRoundRect(lx, ly, lw, lh, 9, 9);

            gc.setFill(node.state == NodeState.LOCKED ? Color.web("#94A3B8") : Color.WHITE);
            gc.setFont(Font.font("System", node.state == NodeState.LOCKED ? FontWeight.NORMAL : FontWeight.BOLD, 10));
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(labelText, x, ly + lh / 2);
        }
    }

    private void drawTooltip(double W, double H) {
        SkillNode node = hoveredNode;
        if (node == null || node.id <= 0) return;

        String[] lines = {
                node.libelle,
                "Catégorie : " + node.categorie,
                "Niveau : " + node.niveauActuel + " / " + node.niveauMax,
                "Type : " + node.typeCompetence,
                node.valide ? "✓ Validé" : "En attente de validation"
        };

        double tw = 200, th = lines.length * 17 + 16;
        double tx = tooltipX + 14, ty = tooltipY - th / 2;
        if (tx + tw > W - 10) tx = tooltipX - tw - 14;
        if (ty < 10)          ty = 10;
        if (ty + th > H - 10) ty = H - th - 10;

        // Shadow
        gc.setFill(Color.web("#000000", 0.2));
        gc.fillRoundRect(tx + 2, ty + 2, tw, th, 10, 10);

        // Background
        gc.setFill(Color.web("#FFFFFF", 0.98));
        gc.fillRoundRect(tx, ty, tw, th, 10, 10);

        // Accent strip
        gc.setFill(Color.web(node.color));
        gc.fillRoundRect(tx, ty, 4, th, 2, 2);

        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        for (int i = 0; i < lines.length; i++) {
            double textY = ty + 13 + i * 17;
            if (i == 0) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("System", FontWeight.BOLD, 12));
            } else if (i == lines.length - 1) {
                gc.setFill(Color.web(node.valide ? "#10B981" : "#94A3B8"));
                gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            } else {
                gc.setFill(Color.web("#94A3B8"));
                gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            }
            gc.fillText(lines[i], tx + 10, textY);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANIMATION
    // ══════════════════════════════════════════════════════════════════════
    private void startGlobalAnimation() {
        globalAnim = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            long t = System.currentTimeMillis();
            // Glow pulsation sur les nœuds maîtrisés
            for (SkillNode node : nodes) {
                if (node.state == NodeState.MASTERED || node.id == -1) {
                    double phase = (t % 2000) / 2000.0;
                    double glow  = Math.sin(phase * Math.PI * 2 + node.id * 0.5) * 0.5 + 0.5;
                    glowAnimValues.put(node.id, glow);
                }
            }
            redraw();
        }));
        globalAnim.setCycleCount(Timeline.INDEFINITE);
        globalAnim.play();

        stage.setOnCloseRequest(e -> globalAnim.stop());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HIT TEST
    // ══════════════════════════════════════════════════════════════════════
    private SkillNode hitTest(double mouseX, double mouseY) {
        // Convert screen coords to world coords
        double wx = (mouseX - offsetX) / scale;
        double wy = (mouseY - offsetY) / scale;

        SkillNode best = null;
        double minDist = NODE_R * 1.6;
        for (SkillNode node : nodes) {
            double dist = Math.sqrt((wx - node.x) * (wx - node.x) + (wy - node.y) * (wy - node.y));
            if (dist < minDist) { minDist = dist; best = node; }
        }
        return best;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DATA CLASSES
    // ══════════════════════════════════════════════════════════════════════
    enum NodeState { LOCKED, AVAILABLE, UNLOCKED, MASTERED }

    private static class SkillNode {
        int id, niveauActuel, niveauMax;
        String libelle, categorie, typeCompetence, color;
        boolean valide;
        NodeState state;
        double x, y;

        SkillNode(int id, String libelle, String categorie, String type, String color,
                  int niveauActuel, int niveauMax, boolean valide, NodeState state) {
            this.id = id; this.libelle = libelle; this.categorie = categorie;
            this.typeCompetence = type; this.color = color;
            this.niveauActuel = niveauActuel; this.niveauMax = niveauMax;
            this.valide = valide; this.state = state;
        }
    }

    private static class SkillEdge {
        final SkillNode from, to;
        final boolean   active;
        SkillEdge(SkillNode from, SkillNode to, boolean active) {
            this.from = from; this.to = to; this.active = active;
        }
    }
}