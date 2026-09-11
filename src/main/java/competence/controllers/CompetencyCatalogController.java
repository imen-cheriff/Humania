package competence.controllers;

import javafx.stage.Stage;
import utils.UserSession;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import utils.MyDataBase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class CompetencyCatalogController {

    @FXML private TextField  searchCompetenciesField;
    @FXML private MenuButton menuFilterCategory;
    @FXML private Button     btnGridView;
    @FXML private Button     btnListView;
    @FXML private GridPane   gridCompetencies;
    @FXML private VBox       vboxListView;
    @FXML private Button     btnAddCompetency;
    @FXML private Label      lblTotalCompetencies;
    @FXML private Label      lblTotalCategories;
    @FXML private Label      lblCriticalCompetencies;
    @FXML private Label      lblAvgMaxLevel;
    // Sous-titres et liens des 4 cards stats (mis à jour selon le mode)
    @FXML private Label      lblSubTotal;
    @FXML private Label      lblSubCategories;
    @FXML private Label      lblSubCritical;
    @FXML private Label      lblSubAvg;
    @FXML private Label      lblLinkTotal;
    @FXML private Label      lblLinkCategories;
    @FXML private Label      lblLinkCritical;
    @FXML private Label      lblLinkAvg;
    @FXML private Button     btnToggleMySkills;
    @FXML private VBox       vboxMySkills;

    private Runnable navigateToTraining;
    public void setNavigateToTraining(Runnable r) { this.navigateToTraining = r; }

    private Connection connection;
    private ObservableList<CompetenceRow> allData;
    private boolean isGridView    = true;
    private boolean showMySkills  = false;
    private String  selectedCategory = "All Categories";
    private String  activeTab        = "mine";

    // Cache formations : stable entre sections, réinitialisé si niveau modifié
    private final Map<Integer, FormationRec> formationCache = new HashMap<>();

    private final Map<String, String>   catColors    = new LinkedHashMap<>();
    private final Map<String, String[]> catGradients = new LinkedHashMap<>();

    private static final Map<String, String> LOGOS = buildLogosMap();
    private static Map<String, String> buildLogosMap() {
        String b = "https://raw.githubusercontent.com/devicons/devicon/master/icons/";
        Map<String, String> m = new LinkedHashMap<>();
        m.put("JavaScript", b+"javascript/javascript-original.png");
        m.put("TypeScript", b+"typescript/typescript-original.png");
        m.put("Python",     b+"python/python-original.png");
        m.put("Java",       b+"java/java-original.png");
        m.put("React",      b+"react/react-original.png");
        m.put("Angular",    b+"angularjs/angularjs-original.png");
        m.put("Vue.js",     b+"vuejs/vuejs-original.png");
        m.put("Docker",     b+"docker/docker-original.png");
        m.put("MySQL",      b+"mysql/mysql-original.png");
        m.put("MongoDB",    b+"mongodb/mongodb-original.png");
        m.put("Git",        b+"git/git-original.png");
        m.put("Spring",     b+"spring/spring-original.png");
        m.put("Spring Boot",b+"spring/spring-original.png");
        m.put("AWS",        b+"amazonwebservices/amazonwebservices-original.png");
        m.put("Azure",      b+"azure/azure-original.png");
        m.put("Figma",      b+"figma/figma-original.png");
        return m;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════════════════
    @FXML public void initialize() {
        connection = MyDataBase.getInstance().getCnx();
        allData    = FXCollections.observableArrayList();
        initCategoryStyles();
        loadCategoryColorsFromDB();
        loadCategories();
        loadStatistics();
        loadAll();
        applyRoleVisibility();
        if (searchCompetenciesField != null)
            searchCompetenciesField.textProperty().addListener((o, v, n) -> filterAndDisplay());
        if (btnToggleMySkills != null) {
            styleToggleBtn(false);
            btnToggleMySkills.setOnAction(e -> handleToggleMySkills());
        }
    }

    private boolean isRH() {
        String role = UserSession.getInstance().getRole();
        return role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("RH"));
    }

    private void applyRoleVisibility() {
        if (btnAddCompetency != null) {
            btnAddCompetency.setVisible(isRH());
            btnAddCompetency.setManaged(isRH());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TOGGLE — une seule méthode centrale qui gère tout
    // ═══════════════════════════════════════════════════════════════════════════
    @FXML private void handleToggleMySkills() {
        showMySkills = !showMySkills;
        styleToggleBtn(showMySkills);
        // Filtres toujours actifs dans les 2 vues
        if (searchCompetenciesField != null) {
            searchCompetenciesField.setDisable(false);
            searchCompetenciesField.setOpacity(1.0);
        }
        if (menuFilterCategory != null) {
            menuFilterCategory.setDisable(false);
            menuFilterCategory.setOpacity(1.0);
        }
        // Mettre à jour les 4 cards de stats selon le mode actif
        if (showMySkills) loadStatisticsMySkills();
        else              loadStatisticsCatalogue();
        filterAndDisplay(); // point d'entrée unique
    }

    private void styleToggleBtn(boolean active) {
        if (btnToggleMySkills == null) return;
        if (active) {
            btnToggleMySkills.setText("📋  Catalogue");
            btnToggleMySkills.setStyle(
                    "-fx-background-color:#4F46E5;-fx-text-fill:white;" +
                            "-fx-font-weight:bold;-fx-font-size:12;" +
                            "-fx-padding:10 18;-fx-background-radius:20;-fx-cursor:hand;");
        } else {
            btnToggleMySkills.setText("👤  Mes Compétences");
            btnToggleMySkills.setStyle(
                    "-fx-background-color:#EEEDFE;-fx-text-fill:#534AB7;" +
                            "-fx-font-weight:bold;-fx-font-size:12;" +
                            "-fx-padding:10 18;-fx-background-radius:20;-fx-cursor:hand;");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE UNIQUE : filterAndDisplay
    //  Gère catalogue ET mes compétences avec les mêmes filtres
    // ═══════════════════════════════════════════════════════════════════════════
    private void filterAndDisplay() {
        String q = searchCompetenciesField != null
                ? searchCompetenciesField.getText().toLowerCase().trim() : "";

        if (showMySkills) {
            // Cacher les vues catalogue
            if (gridCompetencies != null) { gridCompetencies.setVisible(false); gridCompetencies.setManaged(false); }
            if (vboxListView != null)     { vboxListView.setVisible(false);     vboxListView.setManaged(false); }
            buildMySkillsView(q, selectedCategory);
        } else {
            // Cacher la vue personnelle
            if (vboxMySkills != null) { vboxMySkills.setVisible(false); vboxMySkills.setManaged(false); }

            // Filtrer le catalogue
            ObservableList<CompetenceRow> list = FXCollections.observableArrayList();
            for (CompetenceRow c : allData) {
                boolean ms = q.isEmpty()
                        || c.getLibelle().toLowerCase().contains(q)
                        || (c.getCategorie() != null && c.getCategorie().toLowerCase().contains(q));
                boolean mc = "All Categories".equals(selectedCategory)
                        || (c.getCategorie() != null && c.getCategorie().equals(selectedCategory));
                if (ms && mc) list.add(c);
            }
            if (isGridView) displayGrid(list); else displayList(list);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  VUE "MES COMPÉTENCES"
    // ═══════════════════════════════════════════════════════════════════════════
    private void buildMySkillsView(String searchQuery, String catFilter) {
        if (vboxMySkills == null) return;
        vboxMySkills.getChildren().clear();
        vboxMySkills.setVisible(true);
        vboxMySkills.setManaged(true);
        vboxMySkills.setSpacing(16);

        int uid = UserSession.getInstance().getUserId();
        if (uid <= 0) uid = 1;

        // Charger TOUTES les compétences une fois
        List<MySkillRow> allSkills = loadMySkills(uid);

        // Appliquer filtres (search + catégorie) pour le contenu affiché
        List<MySkillRow> filtered = allSkills.stream()
                .filter(s -> {
                    boolean ms = searchQuery.isEmpty()
                            || s.libelle.toLowerCase().contains(searchQuery)
                            || s.categorie.toLowerCase().contains(searchQuery);
                    boolean mc = "All Categories".equals(catFilter) || s.categorie.equals(catFilter);
                    return ms && mc;
                })
                .collect(Collectors.toList());

        // KPI toujours calculés sur TOUTES les compétences (pas filtrées)
        vboxMySkills.getChildren().add(buildTabBar(allSkills));
        vboxMySkills.getChildren().add(buildTabContent(filtered, uid));
    }

    private VBox kpiCard(String val, String label, String textColor, String bg, String dotColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12, 14, 12, 14));
        String base = "-fx-background-color:white;-fx-background-radius:12;" +
                "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;";
        card.setStyle(base);

        HBox top = new HBox(8); top.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5, Color.web(dotColor));
        Label valLbl = new Label(val);
        valLbl.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:" + textColor + ";");
        top.getChildren().addAll(dot, valLbl);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10;-fx-text-fill:#6B7280;");
        card.getChildren().addAll(top, lbl);

        String hov = "-fx-background-color:" + bg + ";-fx-background-radius:12;" +
                "-fx-border-color:" + dotColor + ";-fx-border-width:1;-fx-border-radius:12;";
        card.setOnMouseEntered(e -> card.setStyle(hov));
        card.setOnMouseExited(e  -> card.setStyle(base));
        return card;
    }

    // ── Onglets ───────────────────────────────────────────────────────────────
    private HBox buildTabBar(List<MySkillRow> allSkills) {
        HBox bar = new HBox(8); bar.setAlignment(Pos.CENTER_LEFT);

        long withLevel = allSkills.stream().filter(s -> s.niveauActuel > 0).count();
        long gaps      = allSkills.stream().filter(s -> s.niveauMax > 0 && (double)s.niveauActuel/s.niveauMax < 0.6).count();
        long total     = allSkills.size();

        bar.getChildren().addAll(
                buildTabBtn("Mes niveaux", String.valueOf(withLevel), "mine",  "#4F46E5", false),
                buildTabBtn("Gaps",        String.valueOf(gaps),       "gaps",  "#E24B4A", false),
                buildTabBtn("Toutes",      String.valueOf(total),      "all",   "#4F46E5", false)
        );

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnAdd = new Button("+ Évaluer une compétence");
        btnAdd.setStyle("-fx-background-color:#EEEDFE;-fx-text-fill:#534AB7;" +
                "-fx-font-weight:bold;-fx-font-size:11;-fx-cursor:hand;" +
                "-fx-padding:7 14;-fx-background-radius:20;");
        btnAdd.setOnAction(e -> showSelfEvaluationDialog(loadMySkills(
                Math.max(1, UserSession.getInstance().getUserId()))));
        bar.getChildren().addAll(spacer, btnAdd);
        return bar;
    }

    private Button buildTabBtn(String label, String count, String tabKey, String color, boolean dummy) {
        boolean active = activeTab.equals(tabKey);

        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        Label cnt = new Label(count);

        if (active) {
            lbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:white;");
            cnt.setStyle("-fx-font-size:10;-fx-padding:1 7;-fx-background-radius:10;" +
                    "-fx-background-color:rgba(255,255,255,0.25);-fx-text-fill:white;");
        } else {
            String cntBg  = "gaps".equals(tabKey) ? "#FCEBEB" : "#F1EFE8";
            String cntFg  = "gaps".equals(tabKey) ? "#A32D2D" : "#5F5E5A";
            lbl.setStyle("-fx-font-size:12;-fx-text-fill:#6B7280;");
            cnt.setStyle("-fx-font-size:10;-fx-padding:1 7;-fx-background-radius:10;" +
                    "-fx-background-color:" + cntBg + ";-fx-text-fill:" + cntFg + ";");
        }
        content.getChildren().addAll(lbl, cnt);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.setStyle(active
                ? "-fx-background-color:#4F46E5;-fx-background-radius:20;-fx-padding:7 16;-fx-cursor:hand;"
                : "-fx-background-color:white;-fx-background-radius:20;-fx-padding:7 16;-fx-cursor:hand;" +
                "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:20;");
        btn.setOnAction(e -> { activeTab = tabKey; filterAndDisplay(); });
        return btn;
    }

    // ── Contenu selon onglet ──────────────────────────────────────────────────
    private VBox buildTabContent(List<MySkillRow> filtered, int uid) {
        // Filtrer selon l'onglet actif
        List<MySkillRow> toShow = switch (activeTab) {
            case "gaps" -> filtered.stream()
                    .filter(s -> s.niveauMax > 0 && (double)s.niveauActuel/s.niveauMax < 0.6)
                    .sorted(Comparator.comparingDouble(s -> (double)s.niveauActuel/s.niveauMax))
                    .collect(Collectors.toList());
            case "mine" -> filtered.stream()
                    .filter(s -> s.niveauActuel > 0)
                    .sorted(Comparator.comparingDouble((MySkillRow s) ->
                            s.niveauMax > 0 ? (double)s.niveauActuel/s.niveauMax : 0).reversed())
                    .collect(Collectors.toList());
            default     -> filtered; // "all"
        };

        VBox container = new VBox(20);

        if (toShow.isEmpty()) {
            VBox emptyBox = new VBox(8); emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(32));
            emptyBox.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                    "-fx-border-color:#F3F4F6;-fx-border-width:1;-fx-border-radius:14;");
            Label emptyIco = new Label("gaps".equals(activeTab) ? "✅" : "📋");
            emptyIco.setStyle("-fx-font-size:32;");
            Label emptyLbl = new Label(
                    "gaps".equals(activeTab)  ? "Aucun gap — vous êtes au niveau requis sur toutes vos compétences !" :
                            "mine".equals(activeTab)  ? "Aucune compétence évaluée. Cliquez « + Évaluer » pour commencer." :
                                    "Aucune compétence ne correspond à votre recherche.");
            emptyLbl.setStyle("-fx-font-size:13;-fx-text-fill:#9CA3AF;");
            emptyLbl.setWrapText(true); emptyLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            emptyBox.getChildren().addAll(emptyIco, emptyLbl);
            container.getChildren().add(emptyBox);
            return container;
        }

        // Grouper par catégorie
        Map<String, List<MySkillRow>> byCat = toShow.stream()
                .collect(Collectors.groupingBy(s -> s.categorie, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<MySkillRow>> entry : byCat.entrySet()) {
            String cat   = entry.getKey();
            String color = catColors.getOrDefault(cat, "#6366F1");
            List<MySkillRow> catSkills = entry.getValue();

            // Label catégorie — texte simple, sans border complexe
            HBox catHeader = new HBox(8); catHeader.setAlignment(Pos.CENTER_LEFT);
            catHeader.setPadding(new Insets(0, 0, 4, 0));
            Region catBar = new Region(); catBar.setPrefWidth(3); catBar.setPrefHeight(14);
            catBar.setStyle("-fx-background-color:" + color + ";-fx-background-radius:2;");
            Label catLbl = new Label(cat.toUpperCase());
            catLbl.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
            catHeader.getChildren().addAll(catBar, catLbl);

            // Grille 3 colonnes
            GridPane grid = new GridPane();
            grid.setHgap(12); grid.setVgap(12);
            for (int i = 0; i < 3; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(33.33); cc.setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().add(cc);
            }

            int col = 0, row = 0;
            for (MySkillRow skill : catSkills) {
                VBox card = buildSkillCard(skill, color, uid);
                grid.add(card, col, row);
                if (++col == 3) { col = 0; row++; }
            }

            VBox catBlock = new VBox(8, catHeader, grid);
            container.getChildren().add(catBlock);
        }
        return container;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CARTE COMPÉTENCE — CSS entièrement corrigé (plus de 0.5 ni dashed)
    // ═══════════════════════════════════════════════════════════════════════════
    private VBox buildSkillCard(MySkillRow skill, String catColor, int uid) {
        double ratio   = skill.niveauMax > 0 ? (double)skill.niveauActuel/skill.niveauMax : 0;
        boolean hasLevel = skill.niveauActuel > 0;

        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setMaxWidth(Double.MAX_VALUE);

        // CSS valide : border-width en integer uniquement, pas de "dashed" dans border-width
        String baseStyle = hasLevel
                ? "-fx-background-color:white;-fx-background-radius:12;" +
                "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;"
                : "-fx-background-color:#F9FAFB;-fx-background-radius:12;" +
                "-fx-border-color:#EBEBEB;-fx-border-width:1;-fx-border-radius:12;";
        card.setStyle(baseStyle);
        if (!hasLevel) card.setOpacity(0.82);

        // ── Top : avatar + nom + badge ─────────────────────────────────────
        HBox top = new HBox(10); top.setAlignment(Pos.CENTER_LEFT);
        StackPane avatar = buildAvatarCircle(skill.libelle, catColor, 38);

        VBox nameBlock = new VBox(2); HBox.setHgrow(nameBlock, Priority.ALWAYS);
        Label nameLbl = new Label(skill.libelle);
        nameLbl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:" +
                (hasLevel ? "#111827" : "#9CA3AF") + ";");
        nameLbl.setWrapText(false);
        nameLbl.setMaxWidth(145);
        Label catLbl2 = new Label(skill.categorie);
        catLbl2.setStyle("-fx-font-size:10;-fx-text-fill:#B4B2A9;");
        nameBlock.getChildren().addAll(nameLbl, catLbl2);

        Label badge = buildStateBadge(ratio, hasLevel);
        top.getChildren().addAll(avatar, nameBlock, badge);
        card.getChildren().add(top);

        if (hasLevel) {
            // ── Barre de progression ──────────────────────────────────────
            HBox progRow = buildProgressRow(ratio, skill.niveauActuel, skill.niveauMax);
            card.getChildren().add(progRow);

            // ── Formation recommandée (gaps < 60%) ────────────────────────
            if (ratio < 0.6) {
                FormationRec rec = findBestFormation(skill.competenceId, skill.libelle, skill.categorie, uid);
                if (rec != null) card.getChildren().add(buildFormationPill(rec));
            }

            // ── Séparateur fin + bouton modifier ──────────────────────────
            Region sep = new Region();
            sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
            sep.setStyle("-fx-background-color:#F3F4F6;");

            HBox btnRow = new HBox(); btnRow.setAlignment(Pos.CENTER_RIGHT);
            Button btnEdit = new Button("✏  Modifier");
            // CSS valide pour bouton : pas de 0.5 en border-width
            btnEdit.setStyle("-fx-background-color:transparent;-fx-text-fill:#888780;" +
                    "-fx-font-size:11;-fx-cursor:hand;-fx-padding:4 8;" +
                    "-fx-background-radius:6;-fx-border-color:#D3D1C7;" +
                    "-fx-border-width:1;-fx-border-radius:6;");
            btnEdit.setOnAction(e -> showUpdateLevelDialog(skill));
            btnRow.getChildren().add(btnEdit);
            card.getChildren().addAll(sep, btnRow);

        } else {
            // ── Pas de niveau → bouton déclarer ──────────────────────────
            Button btnDeclare = new Button("+ Déclarer mon niveau");
            btnDeclare.setMaxWidth(Double.MAX_VALUE);
            btnDeclare.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;" +
                    "-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;" +
                    "-fx-padding:8 12;-fx-background-radius:8;-fx-border-width:0;");
            btnDeclare.setOnAction(e -> showUpdateLevelDialogForNew(skill));
            card.getChildren().add(btnDeclare);
        }

        // Hover — style CSS valide
        if (hasLevel) {
            String hoverStyle = "-fx-background-color:white;-fx-background-radius:12;" +
                    "-fx-border-color:" + catColor + ";-fx-border-width:1;-fx-border-radius:12;";
            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
            card.setOnMouseExited(e  -> card.setStyle(baseStyle));
        }
        return card;
    }

    private HBox buildProgressRow(double ratio, int current, int max) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);

        StackPane trackWrap = new StackPane();
        trackWrap.setPrefHeight(8); HBox.setHgrow(trackWrap, Priority.ALWAYS);

        Region trackBg = new Region();
        trackBg.setPrefHeight(8);
        trackBg.setStyle("-fx-background-color:#F1EFE8;-fx-background-radius:4;");
        trackBg.prefWidthProperty().bind(trackWrap.widthProperty());

        String fillColor = ratio >= 0.8 ? "#639922"
                : ratio >= 0.6 ? "#378ADD"
                : ratio >= 0.4 ? "#EF9F27"
                : "#E24B4A";

        Region fillBar = new Region();
        fillBar.setPrefHeight(8);
        fillBar.setStyle("-fx-background-color:" + fillColor + ";-fx-background-radius:4;");
        StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);
        trackWrap.widthProperty().addListener((obs, o, nw) ->
                fillBar.setPrefWidth(Math.max(0, nw.doubleValue() * ratio)));
        trackWrap.getChildren().addAll(trackBg, fillBar);

        Label lvlLbl = new Label(current + "/" + max);
        lvlLbl.setStyle("-fx-font-size:11;-fx-text-fill:#5F5E5A;-fx-font-weight:bold;");
        row.getChildren().addAll(trackWrap, lvlLbl);
        return row;
    }

    // ── Pilule formation ──────────────────────────────────────────────────────
    private HBox buildFormationPill(FormationRec rec) {
        HBox pill = new HBox(8); pill.setAlignment(Pos.CENTER_LEFT);
        pill.setPadding(new Insets(7, 10, 7, 10));
        // CSS valide — border-width:1 (integer)
        pill.setStyle("-fx-background-color:#EEEDFE;-fx-background-radius:8;" +
                "-fx-border-color:#CECBF6;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");

        Label icon  = new Label("🎓"); icon.setStyle("-fx-font-size:13;");
        Label title = new Label(rec.titre.length() > 28 ? rec.titre.substring(0,26)+"…" : rec.titre);
        title.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#3C3489;");
        HBox.setHgrow(title, Priority.ALWAYS);
        Label arrow = new Label("→"); arrow.setStyle("-fx-font-size:12;-fx-text-fill:#534AB7;-fx-font-weight:bold;");
        pill.getChildren().addAll(icon, title, arrow);

        pill.setOnMouseEntered(e -> pill.setStyle(
                "-fx-background-color:#CECBF6;-fx-background-radius:8;" +
                        "-fx-border-color:#AFA9EC;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;"));
        pill.setOnMouseExited(e  -> pill.setStyle(
                "-fx-background-color:#EEEDFE;-fx-background-radius:8;" +
                        "-fx-border-color:#CECBF6;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;"));
        pill.setOnMouseClicked(e -> navigateToFormation());
        return pill;
    }

    private void navigateToFormation() {
        if (navigateToTraining != null) { navigateToTraining.run(); return; }
        try {
            javafx.scene.Scene scene = vboxMySkills != null ? vboxMySkills.getScene()
                    : gridCompetencies != null ? gridCompetencies.getScene() : null;
            if (scene != null && scene.getUserData() instanceof test.MainFX mfx) {
                mfx.navigateTo(test.MainFX.TRAINING_CATALOG); return;
            }
        } catch (Exception ignored) {}
        showInfo("Formation recommandée",
                "Naviguez vers « Formations » dans le menu pour voir toutes les formations disponibles.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FORMATIONS — logique déterministe + cache stable
    // ═══════════════════════════════════════════════════════════════════════════
    private FormationRec findBestFormation(int competenceId, String compLib, String categorie, int uid) {
        if (formationCache.containsKey(competenceId)) return formationCache.get(competenceId);

        FormationRec result = null;
        try {
            // Stratégie 1 : mots-clés du nom de compétence dans le titre de formation
            String[] words = compLib.toLowerCase().split("[\\s/\\-_,]+");
            for (String word : words) {
                if (word.length() < 3) continue;
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT f.id, f.titre, f.duree, IFNULL(cat.libelle,'Général') AS categorie, " +
                                "IFNULL(cat.couleur,'#6366F1') AS couleur, f.cout " +
                                "FROM formation f LEFT JOIN categorieFormation cat ON f.categorie_id=cat.id " +
                                "WHERE LOWER(f.titre) LIKE ? " +
                                "AND LOWER(IFNULL(statutFormation,'active')) NOT IN ('inactive','inactif') " +
                                "AND f.id NOT IN (" +
                                "  SELECT sf.formation_id FROM inscriptionFormation inf " +
                                "  JOIN sessionFormation sf ON inf.session_id=sf.id WHERE inf.employe_id=?" +
                                ") ORDER BY f.duree ASC LIMIT 1");
                ps.setString(1, "%" + word + "%"); ps.setInt(2, uid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    result = new FormationRec(rs.getInt("id"), rs.getString("titre"),
                            rs.getString("categorie"), rs.getString("couleur"),
                            rs.getInt("duree"), rs.getDouble("cout"));
                    break;
                }
            }

            // Stratégie 2 : même catégorie (ORDER BY id = stable)
            if (result == null) {
                PreparedStatement ps2 = connection.prepareStatement(
                        "SELECT f.id, f.titre, f.duree, IFNULL(cat.libelle,'Général') AS categorie, " +
                                "IFNULL(cat.couleur,'#6366F1') AS couleur, f.cout " +
                                "FROM formation f LEFT JOIN categorieFormation cat ON f.categorie_id=cat.id " +
                                "WHERE LOWER(IFNULL(cat.libelle,'')) LIKE ? " +
                                "AND LOWER(IFNULL(statutFormation,'active')) NOT IN ('inactive','inactif') " +
                                "AND f.id NOT IN (" +
                                "  SELECT sf.formation_id FROM inscriptionFormation inf " +
                                "  JOIN sessionFormation sf ON inf.session_id=sf.id WHERE inf.employe_id=?" +
                                ") ORDER BY f.id ASC LIMIT 1");
                ps2.setString(1, "%" + categorie.toLowerCase() + "%"); ps2.setInt(2, uid);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) result = new FormationRec(rs2.getInt("id"), rs2.getString("titre"),
                        rs2.getString("categorie"), rs2.getString("couleur"),
                        rs2.getInt("duree"), rs2.getDouble("cout"));
            }

            // Stratégie 3 : fallback première formation non suivie (ORDER BY id = stable, jamais RAND)
            if (result == null) {
                PreparedStatement ps3 = connection.prepareStatement(
                        "SELECT f.id, f.titre, f.duree, IFNULL(cat.libelle,'Général') AS categorie, " +
                                "IFNULL(cat.couleur,'#6366F1') AS couleur, f.cout " +
                                "FROM formation f LEFT JOIN categorieFormation cat ON f.categorie_id=cat.id " +
                                "WHERE LOWER(IFNULL(statutFormation,'active')) NOT IN ('inactive','inactif') " +
                                "AND f.id NOT IN (" +
                                "  SELECT sf.formation_id FROM inscriptionFormation inf " +
                                "  JOIN sessionFormation sf ON inf.session_id=sf.id WHERE inf.employe_id=?" +
                                ") ORDER BY f.id ASC LIMIT 1");
                ps3.setInt(1, uid);
                ResultSet rs3 = ps3.executeQuery();
                if (rs3.next()) result = new FormationRec(rs3.getInt("id"), rs3.getString("titre"),
                        rs3.getString("categorie"), rs3.getString("couleur"),
                        rs3.getInt("duree"), rs3.getDouble("cout"));
            }
        } catch (SQLException e) { System.err.println("findBestFormation: " + e.getMessage()); }

        formationCache.put(competenceId, result); // null aussi mis en cache → évite re-requête
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DIALOGUES NIVEAU
    // ═══════════════════════════════════════════════════════════════════════════
    private void showUpdateLevelDialog(MySkillRow skill) {
        buildAndShowLevelDialog(skill, skill.niveauActuel, "💾  Enregistrer");
    }
    private void showUpdateLevelDialogForNew(MySkillRow skill) {
        buildAndShowLevelDialog(skill, 0, "✅  Déclarer");
    }

    private void buildAndShowLevelDialog(MySkillRow skill, int initialLevel, String btnLabel) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Mon niveau — " + skill.libelle);
        ButtonType save = new ButtonType(btnLabel, ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        String catColor = catColors.getOrDefault(skill.categorie, "#6366F1");
        VBox content = new VBox(16); content.setPadding(new Insets(20));

        // ── Info compétence ────────────────────────────────────────────────
        HBox infoRow = new HBox(12); infoRow.setAlignment(Pos.CENTER_LEFT);
        StackPane logo = buildAvatarCircle(skill.libelle, catColor, 48);
        VBox nameInfo = new VBox(4);
        Label nameL = new Label(skill.libelle);
        nameL.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        Label catL = new Label(skill.categorie + "  ·  Niveau max " + skill.niveauMax);
        catL.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;");
        nameInfo.getChildren().addAll(nameL, catL);
        infoRow.getChildren().addAll(logo, nameInfo);

        // ── Slider ────────────────────────────────────────────────────────
        Label sliderLbl = new Label("Mon niveau actuel");
        sliderLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#374151;");

        Slider slider = new Slider(0, skill.niveauMax, initialLevel);
        slider.setShowTickLabels(true); slider.setShowTickMarks(true);
        slider.setMajorTickUnit(Math.max(1, skill.niveauMax / 5));
        slider.setSnapToTicks(true); slider.setPrefWidth(320);
        slider.setId("levelSlider");

        Label levelDisplay = new Label(initialLevel + " / " + skill.niveauMax);
        levelDisplay.setStyle("-fx-font-size:24;-fx-font-weight:bold;-fx-text-fill:#4F46E5;");
        slider.valueProperty().addListener((obs, o, nv) -> {
            int v = nv.intValue();
            levelDisplay.setText(v + " / " + skill.niveauMax);
            double r = skill.niveauMax > 0 ? (double)v/skill.niveauMax : 0;
            String c = r >= 0.8 ? "#3B6D11" : r >= 0.5 ? "#185FA5" : r > 0 ? "#633806" : "#9CA3AF";
            levelDisplay.setStyle("-fx-font-size:24;-fx-font-weight:bold;-fx-text-fill:"+c+";");
        });

        HBox sliderRow = new HBox(14); sliderRow.setAlignment(Pos.CENTER_LEFT);
        sliderRow.getChildren().addAll(slider, levelDisplay);

        // ── Description niveau (mise à jour en temps réel) ─────────────────
        VBox descBox = new VBox();
        descBox.getChildren().add(buildLevelDescLabel(initialLevel, skill.niveauMax));
        slider.valueProperty().addListener((obs, o, nv) -> {
            descBox.getChildren().clear();
            descBox.getChildren().add(buildLevelDescLabel(nv.intValue(), skill.niveauMax));
        });

        // ── Preuve URL ────────────────────────────────────────────────────
        Label preuveLabel = new Label("Lien preuve / certification (optionnel)");
        preuveLabel.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;");
        TextField preuveField = new TextField(skill.preuveUrl != null ? skill.preuveUrl : "");
        preuveField.setPromptText("https://certificate.exemple.com/...");
        preuveField.setId("preuveField");
        preuveField.setStyle("-fx-background-color:white;-fx-border-color:#E5E7EB;" +
                "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-padding:8 12;-fx-font-size:12;");

        content.getChildren().addAll(infoRow, sliderLbl, sliderRow, descBox, preuveLabel, preuveField);
        dlg.getDialogPane().setContent(content);

        javafx.application.Platform.runLater(() -> {
            Button ok = (Button) dlg.getDialogPane().lookupButton(save);
            if (ok != null) ok.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-font-size:13;-fx-background-radius:8;-fx-padding:8 20;");
        });

        dlg.setResultConverter(btn -> {
            if (btn == save) {
                Slider s = findSlider(content);
                TextField p = findTextField(content);
                if (s != null) saveMyLevel(skill.competenceId, (int)s.getValue(),
                        p != null ? p.getText().trim() : "");
            }
            return null;
        });
        dlg.showAndWait();
    }

    private Label buildLevelDescLabel(int niveau, int max) {
        double ratio = max > 0 ? (double)niveau/max : 0;
        String desc, bg, fg;
        if (niveau == 0)       { desc="Non évalué";             bg="#F1EFE8"; fg="#5F5E5A"; }
        else if (ratio >= 1.0) { desc="Expert — niveau max !";  bg="#EAF3DE"; fg="#27500A"; }
        else if (ratio >= 0.8) { desc="Avancé — très bon";      bg="#EAF3DE"; fg="#3B6D11"; }
        else if (ratio >= 0.6) { desc="Intermédiaire — bon";    bg="#E6F1FB"; fg="#185FA5"; }
        else if (ratio >= 0.4) { desc="Débutant avancé";        bg="#FAEEDA"; fg="#633806"; }
        else                   { desc="Débutant — gap fort";    bg="#FCEBEB"; fg="#791F1F"; }
        Label l = new Label(desc);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-font-size:11;-fx-text-fill:"+fg+";-fx-font-weight:bold;" +
                "-fx-background-color:"+bg+";-fx-padding:8 12;-fx-background-radius:8;");
        return l;
    }

    private Slider findSlider(VBox parent) {
        for (javafx.scene.Node n : parent.getChildren()) {
            if (n instanceof HBox hb) for (javafx.scene.Node c : hb.getChildren())
                if (c instanceof Slider) return (Slider)c;
        }
        return null;
    }
    private TextField findTextField(VBox parent) {
        for (javafx.scene.Node n : parent.getChildren())
            if (n instanceof TextField tf && "preuveField".equals(tf.getId())) return tf;
        return null;
    }

    private void showSelfEvaluationDialog(List<MySkillRow> mySkills) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Évaluer une compétence");
        ButtonType save = new ButtonType("✅  Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        VBox content = new VBox(14); content.setPadding(new Insets(20));
        Label intro = new Label("Sélectionnez une compétence et indiquez votre niveau actuel.");
        intro.setStyle("-fx-font-size:12;-fx-text-fill:#6B7280;"); intro.setWrapText(true);

        ComboBox<String> cbComp = new ComboBox<>();
        cbComp.setPromptText("Choisir une compétence…"); cbComp.setPrefWidth(370);
        Map<String,Integer> nameToId  = new LinkedHashMap<>();
        Map<String,Integer> nameToMax = new LinkedHashMap<>();
        Set<Integer> already = mySkills.stream().filter(s -> s.niveauActuel > 0)
                .map(s -> s.competenceId).collect(Collectors.toSet());

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT c.id, c.libelle, c.niveauMax FROM competence c " +
                            "WHERE IFNULL(c.statutCompetence,'ACTIF')='ACTIF' ORDER BY c.libelle");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id"); String lib = rs.getString("libelle"); int max = rs.getInt("niveauMax");
                String label = already.contains(id) ? lib + " (déjà évalué)" : lib;
                cbComp.getItems().add(label); nameToId.put(label, id); nameToMax.put(label, max);
            }
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }

        Spinner<Integer> sp = new Spinner<>(0, 10, 1); sp.setEditable(true); sp.setPrefWidth(370);
        Label maxHint = new Label(); maxHint.setStyle("-fx-font-size:10;-fx-text-fill:#9CA3AF;");
        cbComp.setOnAction(e -> {
            String sel = cbComp.getValue();
            if (sel != null) {
                int max = nameToMax.getOrDefault(sel, 10);
                sp.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, 1));
                maxHint.setText("Niveau max : " + max);
            }
        });
        TextField preuveField = new TextField(); preuveField.setPromptText("Lien certification (optionnel)");
        content.getChildren().addAll(intro, mkLbl("Compétence :"), cbComp,
                mkLbl("Mon niveau :"), sp, maxHint, mkLbl("Preuve :"), preuveField);
        dlg.getDialogPane().setContent(content);

        javafx.application.Platform.runLater(() -> {
            Button ok = (Button) dlg.getDialogPane().lookupButton(save);
            if (ok != null) ok.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-font-size:13;-fx-background-radius:8;-fx-padding:8 20;");
        });

        dlg.setResultConverter(btn -> {
            if (btn == save && cbComp.getValue() != null) {
                int compId = nameToId.getOrDefault(cbComp.getValue(), -1);
                if (compId > 0) saveMyLevel(compId, sp.getValue(), preuveField.getText().trim());
            }
            return null;
        });
        dlg.showAndWait();
    }

    private void saveMyLevel(int competenceId, int niveau, String preuveUrl) {
        int uid = UserSession.getInstance().getUserId();
        if (uid <= 0) uid = 1;
        try {
            PreparedStatement check = connection.prepareStatement(
                    "SELECT id FROM competenceemploye WHERE employe_id=? AND competence_id=? LIMIT 1");
            check.setInt(1, uid); check.setInt(2, competenceId);
            if (check.executeQuery().next()) {
                PreparedStatement ps = connection.prepareStatement(
                        "UPDATE competenceemploye SET niveauActuel=?,preuveUrl=?,dateEvaluation=CURDATE() " +
                                "WHERE employe_id=? AND competence_id=?");
                ps.setInt(1, niveau); ps.setString(2, preuveUrl.isBlank()?null:preuveUrl);
                ps.setInt(3, uid); ps.setInt(4, competenceId); ps.executeUpdate();
            } else {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO competenceemploye " +
                                "(niveauActuel,niveauValide,preuveUrl,dateEvaluation,employe_id,competence_id) " +
                                "VALUES (?,0,?,CURDATE(),?,?)");
                ps.setInt(1, niveau); ps.setString(2, preuveUrl.isBlank()?null:preuveUrl);
                ps.setInt(3, uid); ps.setInt(4, competenceId); ps.executeUpdate();
            }
            formationCache.remove(competenceId); // invalider le cache pour cette compétence
            showInfo("✅  Sauvegardé", "Votre niveau a été enregistré !");
            filterAndDisplay(); // rafraîchissement via le point d'entrée unique
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CHARGEMENT BDD
    // ═══════════════════════════════════════════════════════════════════════════
    private List<MySkillRow> loadMySkills(int uid) {
        List<MySkillRow> list = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT c.id, c.libelle, c.niveauMax, c.typeCompetence, " +
                            "IFNULL(cat.libelle,'Général') AS categorie, IFNULL(cat.couleur,'#6366F1') AS couleur, " +
                            "IFNULL(ce.niveauActuel,0) AS niveauActuel, IFNULL(ce.niveauValide,0) AS valide, " +
                            "IFNULL(ce.preuveUrl,'') AS preuveUrl " +
                            "FROM competence c " +
                            "LEFT JOIN categorieCompetence cat ON c.categorie_id = cat.id " +
                            "LEFT JOIN competenceemploye ce ON ce.competence_id=c.id AND ce.employe_id=? " +
                            "WHERE IFNULL(c.statutCompetence,'ACTIF')='ACTIF' " +
                            "ORDER BY cat.libelle, c.typeCompetence DESC, c.libelle");
            ps.setInt(1, uid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new MySkillRow(
                    rs.getInt("id"), rs.getString("libelle"),
                    rs.getString("categorie"), rs.getString("couleur"),
                    rs.getString("typeCompetence"),
                    rs.getInt("niveauActuel"), rs.getInt("niveauMax"),
                    rs.getBoolean("valide"), rs.getString("preuveUrl")));
        } catch (SQLException e) { System.err.println("loadMySkills: " + e.getMessage()); }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CATALOGUE (code original préservé)
    // ═══════════════════════════════════════════════════════════════════════════
    private void initCategoryStyles() {
        catColors.put("Technical",   "#3B82F6"); catGradients.put("Technical",   new String[]{"#1E3A8A","#2563EB"});
        catColors.put("Behavioral",  "#8B5CF6"); catGradients.put("Behavioral",  new String[]{"#4C1D95","#7C3AED"});
        catColors.put("Business",    "#10B981"); catGradients.put("Business",    new String[]{"#064E3B","#059669"});
        catColors.put("Transversal", "#F59E0B"); catGradients.put("Transversal", new String[]{"#78350F","#D97706"});
        catColors.put("Management",  "#059669"); catGradients.put("Management",  new String[]{"#065F46","#10B981"});
        catColors.put("Soft Skills", "#EC4899"); catGradients.put("Soft Skills", new String[]{"#831843","#DB2777"});
        catColors.put("DevOps",      "#F97316"); catGradients.put("DevOps",      new String[]{"#7C2D12","#EA580C"});
        catColors.put("Frontend",    "#0EA5E9"); catGradients.put("Frontend",    new String[]{"#0C4A6E","#0284C7"});
        catColors.put("Backend",     "#6366F1"); catGradients.put("Backend",     new String[]{"#1E1B4B","#4F46E5"});
        catColors.put("Data",        "#14B8A6"); catGradients.put("Data",        new String[]{"#134E4A","#0D9488"});
    }

    private void loadCategoryColorsFromDB() {
        try {
            ResultSet rs = connection.prepareStatement(
                    "SELECT libelle, couleur FROM categorieCompetence").executeQuery();
            while (rs.next()) {
                String l=rs.getString("libelle"), c=rs.getString("couleur");
                if (l!=null && c!=null && !c.isBlank()) catColors.put(l, c);
            }
        } catch (SQLException ignored) {}
    }

    private void loadStatistics() {
        loadStatisticsCatalogue();
    }

    /** Stats du catalogue (mode par défaut) */
    private void loadStatisticsCatalogue() {
        setStat(lblTotalCompetencies,    "SELECT COUNT(*) FROM competence WHERE IFNULL(statutCompetence,'ACTIF')='ACTIF'");
        setStat(lblTotalCategories,      "SELECT COUNT(*) FROM categorieCompetence");
        setStat(lblCriticalCompetencies, "SELECT COUNT(*) FROM competence WHERE typeCompetence='CRITIQUE' AND IFNULL(statutCompetence,'ACTIF')='ACTIF'");
        setAvg(lblAvgMaxLevel,           "SELECT AVG(niveauMax) FROM competence WHERE IFNULL(statutCompetence,'ACTIF')='ACTIF'");
        // Sous-titres catalogue
        setLabelText(lblSubTotal,       "Total Compétences");
        setLabelText(lblSubCategories,  "Catégories");
        setLabelText(lblSubCritical,    "Critiques");
        setLabelText(lblSubAvg,         "Niveau Max Moyen");
        setLabelText(lblLinkTotal,      "📋  Référentiel complet");
        setLabelText(lblLinkCategories, "🏷️  Domaines de compétence");
        setLabelText(lblLinkCritical,   "🔑  Compétences critiques");
        setLabelText(lblLinkAvg,        "⭐  Niveau moyen requis");
    }

    /** Stats personnelles (mode "Mes Compétences") — réutilise les 4 mêmes labels */
    private void loadStatisticsMySkills() {
        int uid = UserSession.getInstance().getUserId();
        if (uid <= 0) uid = 1;
        try {
            // Card 1 : compétences maîtrisées
            PreparedStatement ps1 = connection.prepareStatement(
                    "SELECT COUNT(*) FROM competenceemploye ce " +
                            "JOIN competence c ON ce.competence_id = c.id " +
                            "WHERE ce.employe_id = ? AND ce.niveauActuel >= c.niveauMax AND c.niveauMax > 0 " +
                            "AND IFNULL(c.statutCompetence,'ACTIF') = 'ACTIF'");
            ps1.setInt(1, uid);
            ResultSet rs1 = ps1.executeQuery();
            if (lblTotalCompetencies != null) lblTotalCompetencies.setText(rs1.next() ? String.valueOf(rs1.getInt(1)) : "0");

            // Card 2 : en progression
            PreparedStatement ps2 = connection.prepareStatement(
                    "SELECT COUNT(*) FROM competenceemploye ce " +
                            "JOIN competence c ON ce.competence_id = c.id " +
                            "WHERE ce.employe_id = ? AND ce.niveauActuel > 0 AND ce.niveauActuel < c.niveauMax " +
                            "AND IFNULL(c.statutCompetence,'ACTIF') = 'ACTIF'");
            ps2.setInt(1, uid);
            ResultSet rs2 = ps2.executeQuery();
            if (lblTotalCategories != null) lblTotalCategories.setText(rs2.next() ? String.valueOf(rs2.getInt(1)) : "0");

            // Card 3 : gaps (niveau actuel < 60% du niveau max)
            PreparedStatement ps3 = connection.prepareStatement(
                    "SELECT COUNT(*) FROM competence c " +
                            "LEFT JOIN competenceemploye ce ON ce.competence_id = c.id AND ce.employe_id = ? " +
                            "WHERE c.niveauMax > 0 AND IFNULL(c.statutCompetence,'ACTIF') = 'ACTIF' " +
                            "AND (ce.niveauActuel IS NULL OR ce.niveauActuel < c.niveauMax * 0.6)");
            ps3.setInt(1, uid);
            ResultSet rs3 = ps3.executeQuery();
            if (lblCriticalCompetencies != null) lblCriticalCompetencies.setText(rs3.next() ? String.valueOf(rs3.getInt(1)) : "0");

            // Card 4 : score global en %
            PreparedStatement ps4 = connection.prepareStatement(
                    "SELECT ROUND(AVG(CASE WHEN c.niveauMax > 0 THEN ce.niveauActuel * 100.0 / c.niveauMax ELSE 0 END)) " +
                            "FROM competence c " +
                            "LEFT JOIN competenceemploye ce ON ce.competence_id = c.id AND ce.employe_id = ? " +
                            "WHERE IFNULL(c.statutCompetence,'ACTIF') = 'ACTIF'");
            ps4.setInt(1, uid);
            ResultSet rs4 = ps4.executeQuery();
            if (lblAvgMaxLevel != null) lblAvgMaxLevel.setText(rs4.next() ? rs4.getInt(1) + "%" : "0%");

        } catch (SQLException e) {
            System.err.println("loadStatisticsMySkills: " + e.getMessage());
        }
        // Sous-titres mode "Mes Compétences"
        setLabelText(lblSubTotal,       "Maîtrisées");
        setLabelText(lblSubCategories,  "En progression");
        setLabelText(lblSubCritical,    "Gaps");
        setLabelText(lblSubAvg,         "Score global");
        setLabelText(lblLinkTotal,      "🏆  Compétences maîtrisées");
        setLabelText(lblLinkCategories, "⚡  Niveau atteint ≥ max");
        setLabelText(lblLinkCritical,   "⚠  Niveau < 60% requis");
        setLabelText(lblLinkAvg,        "📊  Progression globale");
    }
    private void setStat(Label l, String sql) {
        if (l==null) return;
        try { ResultSet rs=connection.prepareStatement(sql).executeQuery(); l.setText(rs.next()?String.valueOf(rs.getInt(1)):"0"); }
        catch (SQLException e) { l.setText("—"); }
    }
    private void setAvg(Label l, String sql) {
        if (l==null) return;
        try { ResultSet rs=connection.prepareStatement(sql).executeQuery(); l.setText(rs.next()?String.format("%.1f",rs.getDouble(1)):"0.0"); }
        catch (SQLException e) { l.setText("—"); }
    }
    private void setLabelText(Label l, String text) {
        if (l != null) l.setText(text);
    }

    private void loadCategories() {
        menuFilterCategory.getItems().clear();
        addCatItem("All Categories");
        menuFilterCategory.getItems().add(new SeparatorMenuItem());
        try {
            ResultSet rs=connection.prepareStatement("SELECT libelle FROM categorieCompetence ORDER BY libelle").executeQuery();
            while (rs.next()) addCatItem(rs.getString("libelle"));
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }
    }

    private void addCatItem(String label) {
        CheckMenuItem item = new CheckMenuItem(label);
        item.setSelected("All Categories".equals(label));
        item.setOnAction(e -> {
            selectedCategory = label;
            menuFilterCategory.setText(label);
            menuFilterCategory.getItems().forEach(i -> {
                if (i instanceof CheckMenuItem ci) ci.setSelected(i == item);
            });
            filterAndDisplay(); // fonctionne pour les 2 vues
        });
        menuFilterCategory.getItems().add(item);
    }

    private void loadAll() {
        allData.clear();
        try {
            ResultSet rs;
            try {
                rs = connection.prepareStatement(
                        "SELECT c.id,c.libelle,c.niveauMax,c.typeCompetence," +
                                "IFNULL(c.statutCompetence,'ACTIF') AS statutCompetence,cat.libelle AS categorie " +
                                "FROM competence c LEFT JOIN categorieCompetence cat ON c.categorie_id=cat.id " +
                                "ORDER BY cat.libelle,c.libelle").executeQuery();
            } catch (SQLException noCol) {
                rs = connection.prepareStatement(
                        "SELECT c.id,c.libelle,c.niveauMax,c.typeCompetence,'ACTIF' AS statutCompetence," +
                                "cat.libelle AS categorie FROM competence c " +
                                "LEFT JOIN categorieCompetence cat ON c.categorie_id=cat.id " +
                                "ORDER BY cat.libelle,c.libelle").executeQuery();
            }
            while (rs.next()) allData.add(new CompetenceRow(rs.getInt("id"),rs.getString("libelle"),
                    rs.getString("typeCompetence"),rs.getString("categorie"),
                    rs.getInt("niveauMax"),"ACTIF".equalsIgnoreCase(rs.getString("statutCompetence"))));
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }
        filterAndDisplay();
    }

    // ── Affichage catalogue — grille ──────────────────────────────────────────
    private void displayGrid(ObservableList<CompetenceRow> list) {
        gridCompetencies.getChildren().clear(); gridCompetencies.getColumnConstraints().clear();
        gridCompetencies.setVisible(true); gridCompetencies.setManaged(true);
        if (vboxListView!=null){vboxListView.setVisible(false);vboxListView.setManaged(false);}
        gridCompetencies.setHgap(18); gridCompetencies.setVgap(18);
        for (int i=0;i<3;i++){ColumnConstraints cc=new ColumnConstraints();cc.setPercentWidth(33.33);cc.setHgrow(Priority.ALWAYS);gridCompetencies.getColumnConstraints().add(cc);}
        int col=0,row=0;
        for (int i=0;i<list.size();i++){
            VBox card=buildGridCard(list.get(i)); card.setOpacity(0);
            gridCompetencies.add(card,col,row);
            FadeTransition ft=new FadeTransition(Duration.millis(200),card);
            ft.setDelay(Duration.millis(i*35L));ft.setFromValue(0);ft.setToValue(1);ft.play();
            if(++col==3){col=0;row++;}
        }
    }

    private VBox buildGridCard(CompetenceRow comp) {
        boolean actif=comp.isActif(); String cat=comp.getCategorie()!=null?comp.getCategorie():"Autre";
        String color=catColors.getOrDefault(cat,"#6B7280"); String[]grad=catGradients.getOrDefault(cat,new String[]{color,"#374151"});
        VBox card=new VBox(0); card.setMaxWidth(Double.MAX_VALUE);
        // CSS valide — border-width:1 (integer, pas 0.5 ni string)
        String baseStyle="-fx-background-color:"+(actif?"white":"#F9FAFB")+";-fx-background-radius:16;" +
                "-fx-border-color:"+(actif?"#E5E7EB":"#F3F4F6")+";-fx-border-width:1;-fx-border-radius:16;";
        card.setStyle(baseStyle); if(!actif)card.setOpacity(0.58);
        StackPane banner=new StackPane(); banner.setPrefHeight(96);
        Rectangle bg=new Rectangle(); bg.widthProperty().bind(banner.widthProperty()); bg.setHeight(96);
        try{bg.setFill(Color.web(grad[0]));}catch(Exception ex){bg.setFill(Color.web("#1F2937"));}
        banner.getChildren().add(bg);
        for(int i=0;i<3;i++){Circle deco=new Circle(12+i*18);deco.setFill(Color.web("rgba(255,255,255,0.05)"));StackPane.setAlignment(deco,Pos.BOTTOM_RIGHT);StackPane.setMargin(deco,new Insets(0,-8+i*7,-16+i*5,0));banner.getChildren().add(deco);}
        Rectangle clip=new Rectangle();clip.setArcWidth(32);clip.setArcHeight(32);clip.widthProperty().bind(banner.widthProperty());clip.setHeight(96);banner.setClip(clip);
        StackPane logoWrap=buildLogoWrap(comp.getLibelle(),color,52);StackPane.setAlignment(logoWrap,Pos.CENTER_LEFT);StackPane.setMargin(logoWrap,new Insets(0,0,0,16));
        VBox rightInfo=new VBox(5);rightInfo.setAlignment(Pos.CENTER_RIGHT);StackPane.setAlignment(rightInfo,Pos.CENTER_RIGHT);StackPane.setMargin(rightInfo,new Insets(0,12,0,0));
        rightInfo.getChildren().add(buildTypeBadge(comp.getTypeCompetence(),true));
        if(!actif){Label ib=new Label("⏸ Inactif");ib.setStyle("-fx-background-color:rgba(0,0,0,0.5);-fx-text-fill:white;-fx-font-size:9;-fx-font-weight:bold;-fx-padding:3 9;-fx-background-radius:20;");rightInfo.getChildren().add(ib);}
        banner.getChildren().addAll(logoWrap,rightInfo);
        VBox body=new VBox(8);body.setPadding(new Insets(12,14,12,14));
        Label nameLbl=new Label(comp.getLibelle());nameLbl.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:"+(actif?"#111827":"#9CA3AF")+";");nameLbl.setWrapText(true);
        Label catBadge=new Label(cat);catBadge.setStyle("-fx-background-color:"+color+"18;-fx-text-fill:"+color+";-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
        HBox levelRow=new HBox(3);levelRow.setAlignment(Pos.CENTER_LEFT);Label lvlLbl=new Label("Niveaux : ");lvlLbl.setStyle("-fx-font-size:10;-fx-text-fill:#9CA3AF;");levelRow.getChildren().add(lvlLbl);
        for(int i=1;i<=10;i++){Region bar=new Region();bar.setPrefWidth(13);bar.setPrefHeight(5);bar.setStyle("-fx-background-color:"+(i<=comp.getNiveauMax()?color:"#E5E7EB")+";-fx-background-radius:2;");levelRow.getChildren().add(bar);}
        Label maxLbl=new Label("  "+comp.getNiveauMax()+"/10");maxLbl.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#6B7280;");levelRow.getChildren().add(maxLbl);
        Region sep=new Region();sep.setPrefHeight(1);sep.setMaxWidth(Double.MAX_VALUE);sep.setStyle("-fx-background-color:#F3F4F6;");VBox.setMargin(sep,new Insets(2,0,2,0));
        HBox btnRow=new HBox(8);btnRow.setAlignment(Pos.CENTER_LEFT);
        if(isRH()){
            Button btnEdit=mkBtn("✏  Modifier","#EFF6FF","#2563EB","#BFDBFE");btnEdit.setOnAction(e->showDialog(comp));
            Region spc=new Region();HBox.setHgrow(spc,Priority.ALWAYS);
            Button btnToggle=actif?mkBtn("⏸  Désactiver","#FEF2F2","#DC2626","#FECACA"):mkBtn("▶  Activer","#F0FDF4","#059669","#BBF7D0");
            btnToggle.setOnAction(e->handleToggle(comp,!actif));
            btnRow.getChildren().addAll(btnEdit,spc,btnToggle);
        }
        body.getChildren().addAll(nameLbl,catBadge,levelRow,sep,btnRow);card.getChildren().addAll(banner,body);
        if(actif){
            String hoverStyle="-fx-background-color:white;-fx-background-radius:16;" +
                    "-fx-border-color:"+color+";-fx-border-width:1;-fx-border-radius:16;-fx-translate-y:-3;";
            card.setOnMouseEntered(e->card.setStyle(hoverStyle));card.setOnMouseExited(e->card.setStyle(baseStyle));
        }
        card.setOnMouseClicked(e->{if(e.getClickCount()==2&&actif)showDialog(comp);});
        return card;
    }

    private void displayList(ObservableList<CompetenceRow> list) {
        if(vboxListView==null)return;
        vboxListView.getChildren().clear();gridCompetencies.setVisible(false);gridCompetencies.setManaged(false);
        vboxListView.setVisible(true);vboxListView.setManaged(true);
        Map<String,List<CompetenceRow>> grouped=new LinkedHashMap<>();
        for(CompetenceRow c:list){String cat=c.getCategorie()!=null?c.getCategorie():"Autre";grouped.computeIfAbsent(cat,k->new ArrayList<>()).add(c);}
        grouped.forEach((cat,comps)->vboxListView.getChildren().add(buildCatSection(cat,comps)));
    }

    private VBox buildCatSection(String catName, List<CompetenceRow> comps) {
        String color=catColors.getOrDefault(catName,"#6B7280");String[]grad=catGradients.getOrDefault(catName,new String[]{color,"#374151"});
        VBox section=new VBox(0);section.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:14;");
        HBox hdr=new HBox(12);hdr.setAlignment(Pos.CENTER_LEFT);hdr.setPadding(new Insets(12,16,12,16));
        hdr.setStyle("-fx-background-color:"+grad[0]+"18;-fx-background-radius:14 14 0 0;" +
                "-fx-border-color:#F3F4F6;-fx-border-width:0 0 1 0;-fx-cursor:hand;");
        Region accent=new Region();accent.setPrefWidth(3);accent.setPrefHeight(20);accent.setStyle("-fx-background-color:"+color+";-fx-background-radius:2;");
        Label arrow=new Label("▼");arrow.setStyle("-fx-font-size:10;-fx-text-fill:#9CA3AF;");
        Label catLbl=new Label(catName);catLbl.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Label badge=new Label(comps.size()+" compétences");badge.setStyle("-fx-background-color:"+color+"18;-fx-text-fill:"+color+";-fx-font-size:10;-fx-padding:2 10;-fx-background-radius:20;");
        hdr.getChildren().addAll(accent,arrow,catLbl,badge);
        VBox items=new VBox(0);
        for(int i=0;i<comps.size();i++)items.getChildren().add(buildListItem(comps.get(i),color,i==comps.size()-1));
        final boolean[]exp={true};
        hdr.setOnMouseClicked(e->{exp[0]=!exp[0];arrow.setText(exp[0]?"▼":"▶");items.setVisible(exp[0]);items.setManaged(exp[0]);});
        section.getChildren().addAll(hdr,items);return section;
    }

    private HBox buildListItem(CompetenceRow comp, String color, boolean last) {
        boolean actif=comp.isActif();HBox row=new HBox(14);row.setAlignment(Pos.CENTER_LEFT);row.setPadding(new Insets(10,16,10,16));
        String bdr=last?"":"-fx-border-color:#F3F4F6;-fx-border-width:0 0 1 0;";
        row.setStyle("-fx-background-color:"+(actif?"white":"#FAFAFA")+";"+bdr);if(!actif)row.setOpacity(0.60);
        StackPane logo=buildLogoWrap(comp.getLibelle(),color,34);
        VBox info=new VBox(2);HBox.setHgrow(info,Priority.ALWAYS);
        Label nm=new Label(comp.getLibelle());nm.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:"+(actif?"#111827":"#9CA3AF")+";");
        Label meta=new Label("Niveaux 1–"+comp.getNiveauMax()+"  ·  "+comp.getTypeCompetence());meta.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;");
        info.getChildren().addAll(nm,meta);
        Label typeB=buildTypeBadge(comp.getTypeCompetence(),false);
        Label statB=actif?new Label("● Actif"):new Label("⏸ Inactif");
        statB.setStyle(actif?"-fx-background-color:#D1FAE5;-fx-text-fill:#059669;-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;":"-fx-background-color:#F3F4F6;-fx-text-fill:#9CA3AF;-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
        row.getChildren().addAll(logo,info,typeB,statB);
        if(isRH()){
            Button be=mkBtn("✏","#EFF6FF","#2563EB","#BFDBFE");be.setOnAction(e->showDialog(comp));
            Button bt=actif?mkBtn("⏸","#FEF2F2","#DC2626","#FECACA"):mkBtn("▶","#F0FDF4","#059669","#BBF7D0");
            bt.setOnAction(e->handleToggle(comp,!actif));
            row.getChildren().addAll(be,bt);
        }
        return row;
    }

    // ── Logo / Avatar ──────────────────────────────────────────────────────────
    private final java.util.concurrent.ExecutorService imgPool =
            java.util.concurrent.Executors.newFixedThreadPool(3, r->{Thread t=new Thread(r,"logo");t.setDaemon(true);return t;});

    private StackPane buildLogoWrap(String name, String color, int size) {
        StackPane sp=new StackPane(); sp.setPrefSize(size,size);sp.setMinSize(size,size);sp.setMaxSize(size,size);
        Circle bgC=new Circle(size/2.0);
        try{bgC.setFill(Color.web(color+"22"));}catch(Exception ex){bgC.setFill(Color.TRANSPARENT);}
        sp.getChildren().add(bgC); addInitials(sp,name,color,size);
        String url=findLogoUrl(name);
        if(url!=null){final int imgS=(int)(size*0.78);
            imgPool.submit(()->{try{Image img=new Image(url,imgS,imgS,true,true,false);
                if(!img.isError()&&img.getWidth()>0){ImageView iv=new ImageView(img);iv.setFitWidth(imgS);iv.setFitHeight(imgS);iv.setPreserveRatio(true);iv.setSmooth(true);
                    javafx.application.Platform.runLater(()->{sp.getChildren().removeIf(n->n!=bgC&&(n instanceof Circle||n instanceof Label||n instanceof Rectangle));Circle wb=new Circle(size*0.42);wb.setFill(Color.WHITE);sp.getChildren().addAll(wb,iv);});}}catch(Exception ignored){}});}
        return sp;
    }

    private StackPane buildAvatarCircle(String name, String color, int size) {
        StackPane sp=new StackPane();sp.setPrefSize(size,size);sp.setMinSize(size,size);sp.setMaxSize(size,size);
        Circle bg=new Circle(size/2.0);
        try{bg.setFill(Color.web(color+"22"));}catch(Exception ex){bg.setFill(Color.web("#E5E7EB"));}
        sp.getChildren().add(bg); addInitials(sp,name,color,size); return sp;
    }

    private void addInitials(StackPane sp, String name, String color, int size) {
        if(sp.getChildren().size()>1)return;
        Circle ic=new Circle(size*0.44);
        try{ic.setFill(Color.web(color));}catch(Exception e){ic.setFill(Color.web("#6B7280"));}
        ic.setStroke(Color.web("rgba(255,255,255,0.2)")); ic.setStrokeWidth(1.5);
        String txt; String[] parts=name!=null?name.trim().split("[\\s/.]+"):new String[]{"?"};
        txt=parts.length>=2?(parts[0].substring(0,1)+parts[parts.length-1].substring(0,1)).toUpperCase()
                :name!=null&&name.length()>=2?name.substring(0,2).toUpperCase()
                :name!=null&&!name.isEmpty()?name.substring(0,1).toUpperCase():"?";
        Label lbl=new Label(txt);lbl.setStyle("-fx-text-fill:white;-fx-font-size:"+(int)(size*0.28)+";-fx-font-weight:bold;");
        sp.getChildren().addAll(ic,lbl);
    }

    private String findLogoUrl(String name) {
        if(name==null)return null;if(LOGOS.containsKey(name))return LOGOS.get(name);
        String low=name.toLowerCase();
        for(Map.Entry<String,String>e:LOGOS.entrySet())if(low.contains(e.getKey().toLowerCase())||e.getKey().toLowerCase().contains(low))return e.getValue();
        return null;
    }

    private void handleToggle(CompetenceRow comp, boolean activer) {
        Alert c=new Alert(Alert.AlertType.CONFIRMATION);c.setTitle(activer?"Activer":"Désactiver");
        c.setHeaderText((activer?"✅  Réactiver : ":"⏸  Désactiver : ")+comp.getLibelle());
        c.setContentText(activer?"Réactiver \""+comp.getLibelle()+"\" ?":"Désactiver \""+comp.getLibelle()+"\" ?\n\n✅ Conservée en BDD · ✅ Réactivation possible");
        c.showAndWait().ifPresent(btn->{if(btn!=ButtonType.OK)return;
            try{PreparedStatement ps=connection.prepareStatement("UPDATE competence SET statutCompetence=? WHERE id=?");
                ps.setString(1,activer?"ACTIF":"INACTIF");ps.setInt(2,comp.getId());ps.executeUpdate();
                showInfo(activer?"✅ Activée":"⏸ Désactivée","\""+comp.getLibelle()+"\" "+(activer?"est active.":"a été désactivée."));
                loadAll();loadStatistics();}catch(SQLException e){showError("Erreur",e.getMessage());}});
    }

    @FXML private void handleAddCompetency() {
        if(!isRH()){showError("Accès refusé","Seuls ADMIN et RH peuvent ajouter des compétences.");return;}showDialog(null);
    }

    private void showDialog(CompetenceRow comp) {
        boolean edit=comp!=null;Dialog<ButtonType>dlg=new Dialog<>();dlg.setTitle(edit?"Modifier":"Nouvelle compétence");
        ButtonType ok=new ButtonType(edit?"Mettre à jour":"Créer",ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(ok,ButtonType.CANCEL);dlg.getDialogPane().setPrefWidth(420);
        GridPane g=new GridPane();g.setHgap(14);g.setVgap(12);g.setPadding(new Insets(24));
        TextField tf=new TextField(edit?comp.getLibelle():"");tf.setPromptText("ex: JavaScript, Leadership…");tf.setPrefWidth(270);
        ComboBox<String>cbType=new ComboBox<>();cbType.getItems().addAll("CRITIQUE","IMPORTANTE","SOUHAITEE","OPTIONNELLE");
        cbType.setValue(edit?comp.getTypeCompetence():"IMPORTANTE");cbType.setPrefWidth(270);
        Spinner<Integer>sp=new Spinner<>(1,10,edit?comp.getNiveauMax():5);sp.setEditable(true);sp.setPrefWidth(270);
        ComboBox<String>cbCat=new ComboBox<>();loadCatsInto(cbCat);if(edit&&comp.getCategorie()!=null)cbCat.setValue(comp.getCategorie());cbCat.setPrefWidth(270);
        g.add(mkLbl("Nom :"),0,0);g.add(tf,1,0);g.add(mkLbl("Type :"),0,1);g.add(cbType,1,1);
        g.add(mkLbl("Niveau max :"),0,2);g.add(sp,1,2);g.add(mkLbl("Catégorie :"),0,3);g.add(cbCat,1,3);
        dlg.getDialogPane().setContent(g);
        dlg.setResultConverter(btn->{
            if(btn==ok){try{
                if(edit){PreparedStatement ps=connection.prepareStatement("UPDATE competence SET libelle=?,niveauMax=?,typeCompetence=?,categorie_id=(SELECT id FROM categorieCompetence WHERE libelle=?) WHERE id=?");
                    ps.setString(1,tf.getText());ps.setInt(2,sp.getValue());ps.setString(3,cbType.getValue());ps.setString(4,cbCat.getValue());ps.setInt(5,comp.getId());ps.executeUpdate();showInfo("Mis à jour ✅","Compétence mise à jour !");}
                else{try{PreparedStatement ps=connection.prepareStatement("INSERT INTO competence (libelle,niveauMax,typeCompetence,categorie_id,statutCompetence) VALUES (?,?,?,(SELECT id FROM categorieCompetence WHERE libelle=?),'ACTIF')");
                    ps.setString(1,tf.getText());ps.setInt(2,sp.getValue());ps.setString(3,cbType.getValue());ps.setString(4,cbCat.getValue());ps.executeUpdate();}
                catch(SQLException ex){PreparedStatement ps=connection.prepareStatement("INSERT INTO competence (libelle,niveauMax,typeCompetence,categorie_id) VALUES (?,?,?,(SELECT id FROM categorieCompetence WHERE libelle=?))");
                    ps.setString(1,tf.getText());ps.setInt(2,sp.getValue());ps.setString(3,cbType.getValue());ps.setString(4,cbCat.getValue());ps.executeUpdate();}
                    showInfo("Créée ✅","Compétence ajoutée !");}
                loadAll();loadStatistics();}catch(SQLException e){showError("Erreur",e.getMessage());}}return null;});
        dlg.showAndWait();
    }

    private void loadCatsInto(ComboBox<String>combo){
        try{ResultSet rs=connection.prepareStatement("SELECT libelle FROM categorieCompetence ORDER BY libelle").executeQuery();
            while(rs.next())combo.getItems().add(rs.getString("libelle"));
            if(!combo.getItems().isEmpty())combo.setValue(combo.getItems().get(0));}
        catch(SQLException e){showError("Erreur",e.getMessage());}
    }

    @FXML private void handleGridView() {
        isGridView=true;
        btnGridView.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-padding:6 12;-fx-background-radius:5;-fx-cursor:hand;");
        btnListView.setStyle("-fx-background-color:transparent;-fx-text-fill:#6c757d;-fx-padding:6 12;-fx-background-radius:5;-fx-cursor:hand;");
        filterAndDisplay();
    }
    @FXML private void handleListView() {
        isGridView=false;
        btnListView.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-padding:6 12;-fx-background-radius:5;-fx-cursor:hand;");
        btnGridView.setStyle("-fx-background-color:transparent;-fx-text-fill:#6c757d;-fx-padding:6 12;-fx-background-radius:5;-fx-cursor:hand;");
        filterAndDisplay();
    }

    // ── Helpers UI ─────────────────────────────────────────────────────────────
    private Label buildStateBadge(double ratio, boolean hasLevel) {
        String text, bg, fg;
        if (!hasLevel)       { text="Non évalué"; bg="#F1EFE8"; fg="#5F5E5A"; }
        else if (ratio>=1.0) { text="Maîtrisé";   bg="#EAF3DE"; fg="#27500A"; }
        else if (ratio>=0.8) { text="Avancé";      bg="#EAF3DE"; fg="#3B6D11"; }
        else if (ratio>=0.6) { text="Bon";         bg="#E6F1FB"; fg="#185FA5"; }
        else if (ratio>=0.4) { text="Gap modéré";  bg="#FAEEDA"; fg="#633806"; }
        else if (ratio>0)    { text="Gap sévère";  bg="#FCEBEB"; fg="#791F1F"; }
        else                 { text="Non évalué";  bg="#F1EFE8"; fg="#5F5E5A"; }
        Label l=new Label(text);
        l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";" +
                "-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 8;-fx-background-radius:10;");
        return l;
    }

    private Label buildTypeBadge(String type, boolean lightBg) {
        if(type==null)return new Label();
        String[]c=switch(type){
            case"CRITIQUE"   -> lightBg?new String[]{"rgba(254,226,226,0.88)","#DC2626"}:new String[]{"#FEE2E2","#DC2626"};
            case"IMPORTANTE" -> lightBg?new String[]{"rgba(254,243,199,0.88)","#D97706"}:new String[]{"#FEF3C7","#D97706"};
            case"SOUHAITEE"  -> lightBg?new String[]{"rgba(209,250,229,0.88)","#059669"}:new String[]{"#D1FAE5","#059669"};
            default          -> lightBg?new String[]{"rgba(243,244,246,0.88)","#6B7280"}:new String[]{"#F3F4F6","#6B7280"};
        };
        Label l=new Label(type);l.setStyle("-fx-background-color:"+c[0]+";-fx-text-fill:"+c[1]+";" +
                "-fx-font-size:9;-fx-font-weight:bold;-fx-padding:3 9;-fx-background-radius:20;");return l;
    }

    private Button mkBtn(String t,String bg,String fg,String border){
        Button b=new Button(t);
        b.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";" +
                "-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;" +
                "-fx-padding:6 12;-fx-background-radius:8;" +
                "-fx-border-color:"+border+";-fx-border-width:1;-fx-border-radius:8;");
        return b;
    }
    private Label mkLbl(String t){Label l=new Label(t);l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");return l;}
    private void showError(String t,String m){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle(t);a.setContentText(m);a.showAndWait();}
    private void showInfo(String t,String m){Alert a=new Alert(Alert.AlertType.INFORMATION);a.setTitle(t);a.setContentText(m);a.showAndWait();}

    // ═══════════════════════════════════════════════════════════════════════════
    //  DTOs
    // ═══════════════════════════════════════════════════════════════════════════
    public static class CompetenceRow {
        private final int id,niveauMax;private final String libelle,typeCompetence,categorie;private final boolean actif;
        public CompetenceRow(int id,String lib,String type,String cat,int max,boolean actif){this.id=id;libelle=lib;typeCompetence=type;categorie=cat;niveauMax=max;this.actif=actif;}
        public int getId(){return id;}public String getLibelle(){return libelle;}
        public String getTypeCompetence(){return typeCompetence;}public String getCategorie(){return categorie;}
        public int getNiveauMax(){return niveauMax;}public boolean isActif(){return actif;}
    }

    private static class MySkillRow {
        final int competenceId,niveauActuel,niveauMax;
        final String libelle,categorie,couleur,typeCompetence,preuveUrl;
        final boolean valide;
        MySkillRow(int cid,String lib,String cat,String col,String type,int niv,int max,boolean val,String p){
            competenceId=cid;libelle=lib;categorie=cat;couleur=col;typeCompetence=type;
            niveauActuel=niv;niveauMax=max;valide=val;preuveUrl=p;}
    }

    private record FormationRec(int id,String titre,String categorie,String couleur,int duree,double cout){}
}