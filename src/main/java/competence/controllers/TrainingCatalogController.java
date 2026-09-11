package competence.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.MyDataBase;
import utils.UserSession;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class TrainingCatalogController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TextField        searchTrainingsField;
    @FXML private ComboBox<String> cbFilterType;
    @FXML private ComboBox<String> cbFilterCategory;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private Button           btnCreateTraining;
    @FXML private Label            lblTotalTrainings;
    @FXML private Label            lblELearning;
    @FXML private Label            lblClassroom;
    @FXML private Label            lblCategories;

    // ── ★ NOUVEAU : conteneur principal remplacé par 3 sections ──────────────
    @FXML private VBox             vboxMyEnrolled;      // section "Mes Formations"
    @FXML private VBox             vboxAllTrainings;    // section "Toutes les formations"
    @FXML private VBox             vboxTrainingsList;   // fallback (compat. FXML)
    @FXML private VBox             vboxEmpty;
    @FXML private Label            lblSectionTitle;
    @FXML private Label            lblResultCount;

    // ── State ─────────────────────────────────────────────────────────────────
    private Connection connection;
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
        } catch (SQLException ignored) {}
        int sid = UserSession.getInstance().getUserId();
        return sid > 0 ? sid : 1;
    }

    private final ObservableList<TrainingItem> allTrainings = FXCollections.observableArrayList();
    private String selectedType     = "All Types";
    private String selectedCategory = "All Categories";
    private String selectedStatus   = "All Statuses";

    // ── Maps couleurs ─────────────────────────────────────────────────────────
    private final Map<String, String>   categoryColors    = new LinkedHashMap<>();
    private final Map<String, String[]> categoryGradients = new LinkedHashMap<>();

    // ── Icons par type ────────────────────────────────────────────────────────
    private static final Map<String, String> TYPE_ICONS = Map.of(
            "E-Learning",  "💻",
            "E_LEARNING",  "💻",
            "Classroom",   "🏫",
            "CLASSROOM",   "🏫",
            "Blended",     "🔀",
            "Coaching",    "🎯",
            "Mentoring",   "🤝"
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        connection    = MyDataBase.getInstance().getCnx();
        currentUserId = resolveCurrentUserId();
        initMaps();
        loadCategoryColorsFromDB();
        loadStatistics();
        setupFilters();
        loadTrainings();
        applyRoleVisibility();
        if (searchTrainingsField != null)
            searchTrainingsField.textProperty().addListener((obs, o, v) -> applyFilters());
        // ★ AI Assistant NE S'OUVRE PLUS automatiquement — uniquement au clic bouton
    }

    private void applyRoleVisibility() {
        if (btnCreateTraining != null) {
            btnCreateTraining.setVisible(isRH());
            btnCreateTraining.setManaged(isRH());
        }
    }

    private void initMaps() {
        categoryGradients.put("Technical Training",   new String[]{"#4F46E5","#7C3AED"});
        categoryGradients.put("Management Training",  new String[]{"#059669","#047857"});
        categoryGradients.put("Soft Skills",          new String[]{"#8B5CF6","#6D28D9"});
        categoryGradients.put("DevOps",               new String[]{"#F59E0B","#B45309"});
        categoryGradients.put("Frontend",             new String[]{"#0EA5E9","#0369A1"});
        categoryGradients.put("Cloud",                new String[]{"#6366F1","#4338CA"});
        categoryGradients.put("Soft Skills Avancées", new String[]{"#EC4899","#BE185D"});
        categoryGradients.put("Autre",                new String[]{"#6B7280","#374151"});

        categoryColors.put("Technical Training",   "#4F46E5");
        categoryColors.put("Management Training",  "#059669");
        categoryColors.put("Soft Skills",          "#8B5CF6");
        categoryColors.put("DevOps",               "#F59E0B");
        categoryColors.put("Frontend",             "#0EA5E9");
        categoryColors.put("Cloud",                "#6366F1");
        categoryColors.put("Soft Skills Avancées", "#EC4899");
        categoryColors.put("Autre",                "#6B7280");
    }

    private void loadCategoryColorsFromDB() {
        try {
            ResultSet rs = connection.prepareStatement(
                    "SELECT libelle, couleur FROM categorieFormation").executeQuery();
            while (rs.next()) {
                String l = rs.getString("libelle"), c = rs.getString("couleur");
                if (l != null && c != null && !c.isBlank()) categoryColors.put(l, c);
            }
        } catch (SQLException ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ═══════════════════════════════════════════════════════════════════════════
    private void loadStatistics() {
        setStatLabel(lblTotalTrainings,
                "SELECT COUNT(*) FROM formation WHERE LOWER(IFNULL(statutFormation,'active')) NOT IN ('inactive','inactif','désactivé','desactive')");
        setStatLabel(lblELearning,
                "SELECT COUNT(*) FROM formation WHERE typeFormation IN ('E-Learning','E_LEARNING','E_Learning') " +
                        "AND LOWER(IFNULL(statutFormation,'active')) NOT IN ('inactive','inactif')");
        setStatLabel(lblClassroom,
                "SELECT COUNT(*) FROM formation WHERE typeFormation IN ('Classroom','CLASSROOM') " +
                        "AND LOWER(IFNULL(statutFormation,'active')) NOT IN ('inactive','inactif')");
        setStatLabel(lblCategories, "SELECT COUNT(DISTINCT libelle) FROM categorieFormation");
    }

    private void setStatLabel(Label lbl, String sql) {
        if (lbl == null) return;
        try {
            ResultSet rs = connection.prepareStatement(sql).executeQuery();
            lbl.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
        } catch (SQLException e) { lbl.setText("—"); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FILTRES
    // ═══════════════════════════════════════════════════════════════════════════
    private void setupFilters() {
        if (cbFilterType != null) {
            cbFilterType.setItems(FXCollections.observableArrayList(
                    "All Types","Classroom","E-Learning","Blended","Coaching","Mentoring"));
            cbFilterType.setValue("All Types");
            cbFilterType.setOnAction(e -> { selectedType = cbFilterType.getValue(); applyFilters(); });
        }
        if (cbFilterCategory != null) {
            ObservableList<String> cats = FXCollections.observableArrayList("All Categories");
            try {
                ResultSet rs = connection.prepareStatement(
                        "SELECT libelle FROM categorieFormation ORDER BY libelle").executeQuery();
                while (rs.next()) cats.add(rs.getString("libelle"));
            } catch (SQLException ignored) {}
            cbFilterCategory.setItems(cats); cbFilterCategory.setValue("All Categories");
            cbFilterCategory.setOnAction(e -> { selectedCategory = cbFilterCategory.getValue(); applyFilters(); });
        }
        if (cbFilterStatus != null) {
            cbFilterStatus.setItems(FXCollections.observableArrayList("All Statuses","Active","Inactive"));
            cbFilterStatus.setValue("All Statuses");
            cbFilterStatus.setOnAction(e -> { selectedStatus = cbFilterStatus.getValue(); applyFilters(); });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CHARGEMENT BDD
    // ═══════════════════════════════════════════════════════════════════════════
    private void loadTrainings() {
        allTrainings.clear();
        try {
            ResultSet rs = connection.prepareStatement(
                    "SELECT f.id, f.titre, f.description, f.duree, f.cout, f.typeFormation, f.statutFormation, " +
                            "cat.libelle AS categorie, " +
                            "IFNULL(u.fullName, CONCAT(IFNULL(u.prenom,''),' ',IFNULL(u.nom,''))) AS formateur_nom, " +
                            "IFNULL(u.typeFormateur,'interne') AS formateur_type " +
                            "FROM formation f LEFT JOIN categorieFormation cat ON cat.id=f.categorie_id " +
                            "LEFT JOIN utilisateur u ON u.id=f.formateur_id ORDER BY f.titre").executeQuery();
            while (rs.next()) allTrainings.add(new TrainingItem(
                    rs.getInt("id"), rs.getString("titre"), rs.getString("description"),
                    rs.getInt("duree"), rs.getDouble("cout"), rs.getString("typeFormation"),
                    rs.getString("statutFormation"), rs.getString("categorie"),
                    rs.getString("formateur_nom"), rs.getString("formateur_type")));
        } catch (SQLException e) {
            try {
                ResultSet rs2 = connection.prepareStatement(
                        "SELECT f.id,f.titre,f.description,f.duree,f.cout,f.typeFormation,f.statutFormation," +
                                "cat.libelle AS categorie FROM formation f " +
                                "LEFT JOIN categorieFormation cat ON f.categorie_id=cat.id ORDER BY f.titre").executeQuery();
                while (rs2.next()) allTrainings.add(new TrainingItem(
                        rs2.getInt("id"), rs2.getString("titre"), rs2.getString("description"),
                        rs2.getInt("duree"), rs2.getDouble("cout"), rs2.getString("typeFormation"),
                        rs2.getString("statutFormation"), rs2.getString("categorie"), null, null));
            } catch (SQLException e2) { showError("Erreur BDD", e2.getMessage()); }
        }
        applyFilters();
    }

    private void applyFilters() {
        String s = searchTrainingsField != null ? searchTrainingsField.getText().trim().toLowerCase() : "";
        ObservableList<TrainingItem> enrolled  = FXCollections.observableArrayList();
        ObservableList<TrainingItem> catalogue = FXCollections.observableArrayList();

        for (TrainingItem t : allTrainings) {
            boolean ms  = s.isEmpty()
                    || (t.getTitre()!=null && t.getTitre().toLowerCase().contains(s))
                    || (t.getDescription()!=null && t.getDescription().toLowerCase().contains(s));
            boolean mt  = selectedType==null || selectedType.equals("All Types")
                    || (t.getTypeFormation()!=null && norm(t.getTypeFormation()).equalsIgnoreCase(norm(selectedType)));
            boolean mc  = selectedCategory==null || selectedCategory.equals("All Categories")
                    || (t.getCategorie()!=null && t.getCategorie().equals(selectedCategory));
            boolean mst = matchesStatus(t.getStatutFormation());
            if (!ms || !mt || !mc || !mst) continue;

            if (isAlreadyEnrolled(t.getId())) enrolled.add(t);
            else catalogue.add(t);
        }
        displaySections(enrolled, catalogue);
    }

    private boolean matchesStatus(String statut) {
        if (selectedStatus == null || selectedStatus.equals("All Statuses")) return true;
        boolean inactive = statut != null &&
                (statut.equalsIgnoreCase("inactive") || statut.equalsIgnoreCase("inactif") ||
                        statut.equalsIgnoreCase("désactivé") || statut.equalsIgnoreCase("desactive"));
        if (selectedStatus.equals("Inactive")) return inactive;
        if (selectedStatus.equals("Active"))   return !inactive;
        return true;
    }

    private String norm(String s) { return s.toLowerCase().replaceAll("[_\\-\\s]",""); }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ★ AFFICHAGE EN 2 SECTIONS : Mes Formations + Toutes les formations
    // ═══════════════════════════════════════════════════════════════════════════
    private void displaySections(ObservableList<TrainingItem> enrolled,
                                 ObservableList<TrainingItem> catalogue) {
        // Utiliser vboxTrainingsList comme conteneur principal (compat. FXML)
        VBox container = vboxTrainingsList != null ? vboxTrainingsList :
                vboxAllTrainings  != null ? vboxAllTrainings  : new VBox();
        container.getChildren().clear();
        container.setSpacing(24);

        // ── Section 1 : Mes Formations (inscrites) ────────────────────────────
        if (!enrolled.isEmpty()) {
            container.getChildren().add(buildSectionHeader(
                    "🎓  Mes Formations",
                    enrolled.size() + " formation" + (enrolled.size()>1?"s":"") + " en cours",
                    "#4F46E5", "#EEF2FF"
            ));
            container.getChildren().add(buildGrid(enrolled, true));
        }

        // ── Section 2 : Toutes les formations ────────────────────────────────
        String allLabel = catalogue.isEmpty() && enrolled.isEmpty()
                ? "Aucune formation disponible" : catalogue.size() + " formation" + (catalogue.size()!=1?"s":"");

        container.getChildren().add(buildSectionHeader(
                enrolled.isEmpty() ? "📚  Toutes les formations" : "📚  Découvrir d'autres formations",
                allLabel,
                "#059669", "#ECFDF5"
        ));

        if (catalogue.isEmpty() && enrolled.isEmpty()) {
            Label empty = new Label("🔍  Aucune formation ne correspond à vos critères.");
            empty.setStyle("-fx-font-size:13;-fx-text-fill:#9CA3AF;-fx-padding:20;");
            container.getChildren().add(empty);
        } else if (catalogue.isEmpty()) {
            Label allEnrolled = new Label("✅  Vous êtes inscrit à toutes les formations disponibles !");
            allEnrolled.setStyle("-fx-font-size:13;-fx-text-fill:#059669;-fx-font-weight:bold;-fx-padding:16;");
            container.getChildren().add(allEnrolled);
        } else {
            container.getChildren().add(buildGrid(catalogue, false));
        }
    }

    // ── En-tête de section ────────────────────────────────────────────────────
    private HBox buildSectionHeader(String title, String subtitle, String color, String bg) {
        HBox hdr = new HBox(12);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(12, 18, 12, 18));
        hdr.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:" + color + "28;" +
                        "-fx-border-radius:12;-fx-border-width:1;"
        );
        // Barre colorée gauche
        Region bar = new Region();
        bar.setPrefWidth(4); bar.setPrefHeight(32);
        bar.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");

        VBox texts = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#111827;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size:11;-fx-text-fill:" + color + ";-fx-font-weight:bold;");
        texts.getChildren().addAll(titleLbl, subLbl);
        hdr.getChildren().addAll(bar, texts);
        return hdr;
    }

    // ── Grille 2 colonnes ─────────────────────────────────────────────────────
    private GridPane buildGrid(ObservableList<TrainingItem> list, boolean isEnrolledSection) {
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50); col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50); col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        for (int i = 0; i < list.size(); i++) {
            int col = i % 2, row = i / 2;
            HBox card = buildCard(list.get(i), isEnrolledSection);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setMinHeight(185); card.setPrefHeight(185);
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setFillWidth(card, true);
            card.setOpacity(0);
            grid.add(card, col, row);
            FadeTransition ft = new FadeTransition(Duration.millis(250), card);
            ft.setDelay(Duration.millis(i * 40L));
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
        return grid;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ★ CARTE FORMATION — redesignée : pas d'accent gauche, image claire
    // ═══════════════════════════════════════════════════════════════════════════
    private HBox buildCard(TrainingItem t, boolean isEnrolled) {
        String cat      = t.getCategorie() != null ? t.getCategorie() : "Autre";
        String color    = categoryColors.getOrDefault(cat, "#4F46E5");
        String[] grad   = categoryGradients.getOrDefault(cat, new String[]{color,"#374151"});
        String typeRaw  = t.getTypeFormation() != null ? t.getTypeFormation() : "";
        boolean inactive = isInactive(t.getStatutFormation());

        // ── Shell ──────────────────────────────────────────────────────────────
        HBox card = new HBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(185);
        String baseStyle = inactive
                ? "-fx-background-color:#F9FAFB;-fx-background-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);" +
                "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:14;"
                : "-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);" +
                "-fx-border-color:#F3F4F6;-fx-border-width:1;-fx-border-radius:14;";
        card.setStyle(baseStyle);
        if (inactive) card.setOpacity(0.75);

        // ★ Plus d'accent strip gauche coloré —
        //   On garde uniquement la vignette à droite + contenu à gauche

        // ── ★ Vignette CLAIRE (fond pastel + icone type) ──────────────────────
        StackPane thumb = new StackPane();
        thumb.setPrefWidth(120); thumb.setMinWidth(120); thumb.setMaxWidth(120);

        Rectangle thumbBg = new Rectangle(120, 185);
        thumbBg.heightProperty().bind(thumb.heightProperty());
        // Fond clair : version très atténuée de la couleur catégorie
        thumbBg.setStyle("-fx-fill: " + color + "10;");
        try {
            // Fond pastel dégradé clair (pas sombre, pas de vignette)
            thumbBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(color + "18")),
                    new Stop(1, Color.web(color + "08"))));
        } catch (Exception ignored) {
            thumbBg.setFill(Color.web("#F3F4F6"));
        }
        thumb.getChildren().add(thumbBg);

        // Cercle décoratif de fond (subtil)
        Circle decoCircle = new Circle(55);
        decoCircle.setFill(Color.web(color + "10"));
        StackPane.setAlignment(decoCircle, Pos.CENTER);
        thumb.getChildren().add(decoCircle);

        // Icône principale (grand, centré, couleur catégorie)
        String typeEmoji = TYPE_ICONS.getOrDefault(typeRaw, "📚");
        Label iconLbl = new Label(typeEmoji);
        iconLbl.setStyle("-fx-font-size:38;");
        StackPane.setAlignment(iconLbl, Pos.CENTER);
        thumb.getChildren().add(iconLbl);

        // Badge catégorie en bas de vignette
        Label catMini = new Label(cat.length() > 12 ? cat.substring(0,11)+"…" : cat);
        catMini.setStyle(
                "-fx-font-size:9;-fx-font-weight:bold;-fx-text-fill:" + color + ";" +
                        "-fx-background-color:white;-fx-padding:3 7;-fx-background-radius:8;"
        );
        StackPane.setAlignment(catMini, Pos.BOTTOM_CENTER);
        StackPane.setMargin(catMini, new Insets(0, 0, 10, 0));
        thumb.getChildren().add(catMini);

        // ★ Badge "Inscrit" si section enrolled
        if (isEnrolled) {
            Label enrolledBadge = new Label("✓ Inscrit");
            enrolledBadge.setStyle(
                    "-fx-background-color:#059669;-fx-text-fill:white;" +
                            "-fx-font-size:9;-fx-font-weight:bold;-fx-padding:3 8;-fx-background-radius:8;"
            );
            StackPane.setAlignment(enrolledBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(enrolledBadge, new Insets(8, 8, 0, 0));
            thumb.getChildren().add(enrolledBadge);
        }

        // Clip arrondi seulement à gauche
        Rectangle thumbClip = new Rectangle(120, 185);
        thumbClip.setArcWidth(28); thumbClip.setArcHeight(28);
        thumbClip.heightProperty().bind(thumb.heightProperty());
        thumb.setClip(thumbClip);

        // ── Contenu ────────────────────────────────────────────────────────────
        VBox content = new VBox(0);
        content.setPadding(new Insets(14, 18, 12, 16));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Ligne 1 — badges type + coût
        HBox topRow = new HBox(7); topRow.setAlignment(Pos.CENTER_LEFT);

        if (inactive) topRow.getChildren().add(buildStatusChip("Inactive"));

        Label typeBadge = new Label(typeRaw.replace("_","-"));
        typeBadge.setStyle(
                "-fx-background-color:" + (inactive?"#E5E7EB":color+"18") + ";" +
                        "-fx-text-fill:" + (inactive?"#9CA3AF":color) + ";" +
                        "-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;"
        );
        topRow.getChildren().add(typeBadge);
        Region topSpacer = new Region(); HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label coutLbl = new Label(t.getCout() > 0
                ? String.format("%.0f DTN", t.getCout()) : "Gratuit");
        coutLbl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:" +
                (inactive?"#9CA3AF":color)+";");
        topRow.getChildren().addAll(topSpacer, coutLbl);

        // Titre
        Label titleLbl = new Label(t.getTitre() != null ? t.getTitre() : "—");
        titleLbl.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:" +
                (inactive?"#9CA3AF":"#111827")+";");
        titleLbl.setWrapText(true);
        VBox.setMargin(titleLbl, new Insets(6,0,4,0));

        // Description courte
        String desc = t.getDescription() != null ? t.getDescription() : "";
        if (desc.length() > 80) desc = desc.substring(0, 78) + "…";
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        descLbl.setWrapText(true);

        // ★ Meta row — durée ESTIMÉE + formateur
        HBox metaRow = new HBox(14); metaRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(metaRow, new Insets(8,0,0,0));

        // Durée en heures + durée humaine estimée
        String dureeEstimee = formatDureeEstimee(t.getDuree());
        Label durLbl = new Label("⏱  " + t.getDuree() + "h  ·  " + dureeEstimee);
        durLbl.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;");
        metaRow.getChildren().add(durLbl);

        String fNom = t.getFormateurNom();
        if (fNom != null && !fNom.isBlank() && !fNom.trim().equalsIgnoreCase("null null")) {
            boolean ext = "EXTERNE".equalsIgnoreCase(t.getFormateurType());
            Label fLbl = new Label((ext?"🌐  ":"🏢  ") + fNom.trim());
            fLbl.setStyle("-fx-font-size:11;-fx-text-fill:#4F46E5;-fx-font-weight:bold;");
            metaRow.getChildren().add(fLbl);
        }

        // Séparateur
        Region sep = new Region(); sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#F3F4F6;");
        VBox.setMargin(sep, new Insets(10,0,10,0));

        // ── BOUTONS ────────────────────────────────────────────────────────────
        HBox btnRow = new HBox(8); btnRow.setAlignment(Pos.CENTER_LEFT);

        Button btnView = btn("👁  Détails", "#F3F4F6", "#374151", "#E5E7EB", true);
        btnView.setOnAction(e -> handleViewDetails(t));

        Button btnEdit = btn("✏", "#FFF7ED", "#EA580C", "#FDBA74", true);
        btnEdit.setOnAction(e -> handleEditTraining(t));
        btnEdit.setVisible(isRH()); btnEdit.setManaged(isRH());
        if (inactive) btnEdit.setDisable(true);

        Button btnToggle;
        if (inactive) {
            btnToggle = btn("✅  Réactiver", "#D1FAE5", "#059669", "#6EE7B7", true);
            btnToggle.setOnAction(e -> handleToggleActive(t, true));
        } else {
            btnToggle = btn("⏸  Désactiver", "#FEF3C7", "#B45309", "#FCD34D", true);
            btnToggle.setOnAction(e -> handleToggleActive(t, false));
        }
        btnToggle.setVisible(isRH()); btnToggle.setManaged(isRH());

        Region btnSpacer = new Region(); HBox.setHgrow(btnSpacer, Priority.ALWAYS);

        // Bouton S'inscrire / Inscrit / Continuer
        Button btnEnroll;
        if (inactive) {
            btnEnroll = btn("🚫  Indisponible", "#F3F4F6", "#9CA3AF", null, false);
            btnEnroll.setDisable(true);
        } else if (isEnrolled) {
            btnEnroll = btn("▶  Continuer", color, "white", null, false);
            btnEnroll.setOnAction(e -> showInfo("Ma formation", "Retrouvez cette formation dans « Mes Leçons »."));
        } else {
            btnEnroll = btn("🎓  S'inscrire", color, "white", null, false);
            Button ref = btnEnroll;
            btnEnroll.setOnAction(e -> handleEnroll(t, ref));
        }

        btnRow.getChildren().addAll(btnView, btnEdit, btnToggle, btnSpacer, btnEnroll);
        content.getChildren().addAll(topRow, titleLbl, descLbl, metaRow, sep, btnRow);

        // ★ Ordre : contenu (à gauche) + vignette (à droite)
        card.getChildren().addAll(content, thumb);

        // Hover (uniquement si actif)
        if (!inactive) {
            String hoverSt = "-fx-background-color:white;-fx-background-radius:14;" +
                    "-fx-effect:dropshadow(gaussian," + color + "35,20,0,0,6);" +
                    "-fx-border-color:" + color + "40;-fx-border-width:1.5;-fx-border-radius:14;" +
                    "-fx-translate-y:-2;";
            card.setOnMouseEntered(e -> card.setStyle(hoverSt));
            card.setOnMouseExited(e  -> card.setStyle(baseStyle));
            card.setOnMouseClicked(e -> { if (e.getClickCount() == 2) handleViewDetails(t); });
        }
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ★ Durée estimée en langage naturel
    // ═══════════════════════════════════════════════════════════════════════════
    private String formatDureeEstimee(int heures) {
        if (heures <= 0)   return "Durée variable";
        if (heures <= 4)   return "Demi-journée";
        if (heures <= 8)   return "1 journée";
        if (heures <= 16)  return "2 jours";
        if (heures <= 24)  return "3 jours";
        if (heures <= 40)  return "1 semaine";
        if (heures <= 80)  return "2 semaines";
        if (heures <= 120) return "3 semaines";
        if (heures <= 160) return "1 mois";
        if (heures <= 320) return "2 mois";
        if (heures <= 480) return "3 mois";
        return Math.round(heures / 160.0) + " mois";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DÉSACTIVER / RÉACTIVER
    // ═══════════════════════════════════════════════════════════════════════════
    private void handleToggleActive(TrainingItem t, boolean activate) {
        String newStatut = activate ? "Active" : "Inactive";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle((activate?"Réactiver":"Désactiver") + " formation");
        confirm.setHeaderText((activate?"✅  Réactiver : ":"⏸  Désactiver : ") + t.getTitre());
        confirm.setContentText(activate
                ? "Réactiver cette formation ? Elle redeviendra disponible à l'inscription."
                : "Désactiver cette formation ?\nElle sera grisée mais aucune donnée ne sera supprimée.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "UPDATE formation SET statutFormation=? WHERE id=?");
                ps.setString(1, newStatut); ps.setInt(2, t.getId()); ps.executeUpdate();
                showInfo((activate?"✅  Réactivée":"⏸  Désactivée"),
                        "\"" + t.getTitre() + "\" " + (activate?"est de nouveau active.":"a été désactivée."));
                loadTrainings(); loadStatistics();
            } catch (SQLException e) { showError("Erreur", e.getMessage()); }
        });
    }

    private boolean isInactive(String statut) {
        if (statut == null) return false;
        String s = statut.toLowerCase();
        return s.equals("inactive") || s.equals("inactif") ||
                s.equals("désactivé") || s.equals("desactive");
    }

    private Label buildStatusChip(String statut) {
        Label l = new Label("● " + statut);
        l.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-font-size:10;" +
                "-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
        return l;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  POPUP DÉTAILS (inchangé, avec durée estimée ajoutée dans le kpiStrip)
    // ═══════════════════════════════════════════════════════════════════════════
    private void handleViewDetails(TrainingItem t) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("");
        dlg.getDialogPane().setPrefWidth(600);
        dlg.getDialogPane().setPrefHeight(700);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setStyle("-fx-padding:0;-fx-background-color:transparent;-fx-background-radius:24;");

        String cat   = t.getCategorie()!=null ? t.getCategorie() : "Autre";
        String color = categoryColors.getOrDefault(cat,"#4F46E5");
        String[] grad = categoryGradients.getOrDefault(cat,new String[]{color,"#374151"});
        String typeRaw = t.getTypeFormation()!=null ? t.getTypeFormation() : "";

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:20;");

        // Hero banner (fond dégradé propre, pas d'image sombre)
        StackPane hero = new StackPane(); hero.setPrefHeight(160);
        Rectangle heroBg = new Rectangle();
        heroBg.widthProperty().bind(hero.widthProperty()); heroBg.setHeight(160);
        try {
            heroBg.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0,Color.web(grad[0])),new Stop(1,Color.web(grad[1]))));
        } catch(Exception ex) { heroBg.setFill(Color.web(color)); }
        hero.getChildren().add(heroBg);

        // Cercles décoratifs
        for (int i=0;i<3;i++) {
            Circle c = new Circle(30+i*25, Color.web("rgba(255,255,255,0.06)"));
            StackPane.setAlignment(c, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(c, new Insets(0,i*30-20,i*15-30,0));
            hero.getChildren().add(c);
        }

        Rectangle heroClip = new Rectangle();
        heroClip.widthProperty().bind(hero.widthProperty());
        heroClip.setHeight(160); heroClip.setArcWidth(40); heroClip.setArcHeight(40);
        hero.setClip(heroClip);

        VBox heroContent = new VBox(8); heroContent.setPadding(new Insets(22,24,18,24));
        heroContent.setAlignment(Pos.BOTTOM_LEFT);
        heroContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox badges = new HBox(8); badges.setAlignment(Pos.CENTER_LEFT);
        String typeEmoji = TYPE_ICONS.getOrDefault(typeRaw,"📚");
        Label tBadge = new Label(typeEmoji + "  " + typeRaw.replace("_","-"));
        tBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:5 14;-fx-background-radius:20;");
        Label cBadge = new Label("📂  "+cat);
        cBadge.setStyle("-fx-background-color:rgba(255,255,255,0.14);-fx-text-fill:rgba(255,255,255,0.9);" +
                "-fx-font-size:11;-fx-padding:5 14;-fx-background-radius:20;");
        badges.getChildren().addAll(tBadge,cBadge);

        Label heroTitle = new Label(t.getTitre());
        heroTitle.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:white;");
        heroTitle.setWrapText(true);
        heroContent.getChildren().addAll(badges,heroTitle);
        hero.getChildren().add(heroContent);

        // KPI strip avec durée estimée
        HBox kpiStrip = new HBox(0);
        kpiStrip.setStyle("-fx-background-color:#FAFAFA;-fx-border-color:#F0F0F0;-fx-border-width:0 0 1 0;");
        kpiStrip.getChildren().addAll(
                kpiBlock("⏱",t.getDuree()+"h","Durée",color), kpiDivider(),
                kpiBlock("📅",formatDureeEstimee(t.getDuree()),"Estimation",color), kpiDivider(),
                kpiBlock("💰",t.getCout()>0?String.format("%.0f DTN",t.getCout()):"Gratuit","Coût",color), kpiDivider(),
                kpiBlock("📊",isInactive(t.getStatutFormation())?"Inactive":"Active","Statut",color)
        );

        // Corps
        VBox body = new VBox(0); body.setStyle("-fx-background-color:white;");
        if (t.getDescription()!=null&&!t.getDescription().isBlank()) {
            body.getChildren().add(detailSection("📝  Description"));
            Label dt = new Label(t.getDescription());
            dt.setStyle("-fx-font-size:13;-fx-text-fill:#4B5563;-fx-line-spacing:4;");
            dt.setWrapText(true); VBox.setMargin(dt,new Insets(6,24,20,24));
            body.getChildren().addAll(dt,thinSep());
        }
        String fNomD = t.getFormateurNom();
        if (fNomD!=null&&!fNomD.isBlank()&&!fNomD.trim().equalsIgnoreCase("null null")) {
            body.getChildren().add(detailSection("👤  Formateur"));
            body.getChildren().addAll(buildFormateurCard(t,color),thinSep());
        }
        List<String> modules = loadModuleTitres(t.getId());
        if (!modules.isEmpty()) {
            body.getChildren().add(detailSection("📑  Programme — "+modules.size()+" modules"));
            VBox modList = new VBox(8); VBox.setMargin(modList,new Insets(8,24,20,24));
            for (int i=0;i<modules.size();i++) {
                HBox mRow = new HBox(12); mRow.setAlignment(Pos.CENTER_LEFT);
                mRow.setPadding(new Insets(10,14,10,14));
                mRow.setStyle("-fx-background-color:#F9FAFB;-fx-background-radius:10;" +
                        "-fx-border-color:#EAEAEA;-fx-border-width:1;-fx-border-radius:10;");
                Label mNum = new Label(String.valueOf(i+1));
                mNum.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:white;" +
                        "-fx-background-color:"+color+";-fx-background-radius:50;" +
                        "-fx-min-width:28;-fx-min-height:28;-fx-alignment:center;");
                Label mLbl = new Label(modules.get(i));
                mLbl.setStyle("-fx-font-size:13;-fx-text-fill:#374151;");
                mRow.getChildren().addAll(mNum,mLbl); modList.getChildren().add(mRow);
            }
            body.getChildren().addAll(modList,thinSep());
        }
        if (!isInactive(t.getStatutFormation()) && !isAlreadyEnrolled(t.getId())) {
            HBox ctaBox = new HBox(); ctaBox.setPadding(new Insets(16,24,24,24));
            Button cta = new Button("🎓  S'inscrire à cette formation");
            cta.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(cta,Priority.ALWAYS);
            cta.setStyle("-fx-background-color:"+color+";-fx-text-fill:white;" +
                    "-fx-font-size:14;-fx-font-weight:bold;-fx-padding:14 30;" +
                    "-fx-background-radius:14;-fx-cursor:hand;");
            cta.setOnAction(e -> { dlg.close(); handleEnroll(t,new Button()); loadTrainings(); });
            ctaBox.getChildren().add(cta);
            body.getChildren().add(ctaBox);
        }

        ScrollPane sp = new ScrollPane(body); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:white;-fx-background-color:white;-fx-border-color:transparent;");
        root.getChildren().addAll(hero,kpiStrip,sp);
        dlg.getDialogPane().setContent(root);
        dlg.showAndWait();
    }

    private HBox kpiBlock(String icon,String value,String label,String color) {
        VBox cell=new VBox(3); cell.setAlignment(Pos.CENTER); cell.setPadding(new Insets(14,0,14,0));
        HBox.setHgrow(cell, Priority.ALWAYS);
        Label ic=new Label(icon); ic.setStyle("-fx-font-size:20;");
        Label val=new Label(value); val.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        Label lbl=new Label(label); lbl.setStyle("-fx-font-size:10;-fx-text-fill:#9CA3AF;");
        cell.getChildren().addAll(ic,val,lbl);
        HBox wrap=new HBox(cell); wrap.setAlignment(Pos.CENTER); HBox.setHgrow(wrap,Priority.ALWAYS);
        return wrap;
    }
    private Region kpiDivider(){Region r=new Region();r.setPrefWidth(1);r.setPrefHeight(44);r.setStyle("-fx-background-color:#EBEBEB;");return r;}
    private Label detailSection(String text){Label l=new Label(text);l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1F2937;");VBox.setMargin(l,new Insets(16,24,0,24));return l;}
    private Region thinSep(){Region r=new Region();r.setPrefHeight(1);r.setMaxWidth(Double.MAX_VALUE);r.setStyle("-fx-background-color:#F3F4F6;");VBox.setMargin(r,new Insets(4,0,0,0));return r;}

    private HBox buildFormateurCard(TrainingItem t, String color) {
        HBox card=new HBox(14); card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14,18,14,18));
        card.setStyle("-fx-background-color:#EEF2FF;-fx-background-radius:14;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:14;");
        VBox.setMargin(card,new Insets(8,24,4,24));
        boolean ext="EXTERNE".equalsIgnoreCase(t.getFormateurType());
        StackPane av=new StackPane(); Circle av_c=new Circle(24,Color.web(color));
        String init=(t.getFormateurNom()!=null&&!t.getFormateurNom().isEmpty())
                ?String.valueOf(t.getFormateurNom().trim().charAt(0)).toUpperCase():"F";
        Label av_l=new Label(init); av_l.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:white;");
        av.getChildren().addAll(av_c,av_l);
        VBox info=new VBox(3);
        Label role=new Label(ext?"🌐  Formateur externe":"🏢  Formateur interne");
        role.setStyle("-fx-font-size:10;-fx-text-fill:#6366F1;-fx-font-weight:bold;");
        Label name=new Label(t.getFormateurNom());
        name.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#3730A3;");
        info.getChildren().addAll(role,name); card.getChildren().addAll(av,info);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  INSCRIPTION + SYNC PDI (inchangée)
    // ═══════════════════════════════════════════════════════════════════════════
    private boolean isAlreadyEnrolled(int formationId) {
        try {
            PreparedStatement ps=connection.prepareStatement(
                    "SELECT COUNT(*) FROM inscriptionFormation i " +
                            "JOIN sessionFormation s ON s.id=i.session_id " +
                            "WHERE i.employe_id=? AND s.formation_id=?");
            ps.setInt(1,currentUserId); ps.setInt(2,formationId);
            ResultSet rs=ps.executeQuery(); return rs.next()&&rs.getInt(1)>0;
        } catch(SQLException e){return false;}
    }

    private void handleEnroll(TrainingItem t, Button btn) {
        String fInfo=""; String nom=t.getFormateurNom();
        if(nom!=null&&!nom.isBlank()&&!nom.trim().equalsIgnoreCase("null null"))
            fInfo="\n👤  Formateur : "+nom;
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Inscription"); confirm.setHeaderText("🎓  "+t.getTitre());
        confirm.setContentText("Voulez-vous vous inscrire ?\n\n⏱  "+t.getDuree()+"h  ("+
                formatDureeEstimee(t.getDuree())+")  ·  💰  "+String.format("%.0f DTN",t.getCout())+fInfo+
                "\n\n📋  Un objectif sera ajouté à votre PDI automatiquement.");
        confirm.showAndWait().ifPresent(res -> {
            if(res!=ButtonType.OK) return;
            try {
                int sessionId=getOrCreateSession(t.getId());
                if(sessionId<=0){showError("Erreur","Impossible de trouver/créer une session.");return;}
                PreparedStatement ps=connection.prepareStatement(
                        "INSERT INTO inscriptionFormation (dateInscription,statut,progression,employe_id,session_id) " +
                                "VALUES (?,'In Progress',0,?,?)");
                ps.setDate(1,Date.valueOf(LocalDate.now()));
                ps.setInt(2,currentUserId); ps.setInt(3,sessionId); ps.executeUpdate();
                syncWithPDI(t.getTitre(),"In Progress");
                if(btn!=null&&btn.getScene()!=null){
                    btn.setText("✓  Inscrit");
                    btn.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#059669;" +
                            "-fx-font-size:12;-fx-font-weight:bold;-fx-padding:9 18;-fx-background-radius:10;");
                    btn.setDisable(true);
                }
                showInfo("Inscription réussie ! 🎉",
                        "Vous êtes inscrit à \""+t.getTitre()+"\".\n\n" +
                                "✅  Un objectif a été ajouté à votre PDI.\n" +
                                "📚  Retrouvez cette formation dans « Mes Leçons ».");
                loadTrainings(); loadStatistics();
            } catch(SQLException e){
                if(e.getMessage()!=null&&e.getMessage().contains("Duplicate"))
                    showInfo("Déjà inscrit","Vous êtes déjà inscrit à cette formation.");
                else showError("Erreur d'inscription",e.getMessage());
            }
        });
    }

    private void syncWithPDI(String formationTitre, String actionStatut) {
        try {
            int pdiId=getOrCreatePDI(); if(pdiId<=0) return;
            String label="Formation : "+formationTitre;
            PreparedStatement check=connection.prepareStatement(
                    "SELECT id FROM actionPDI WHERE pdi_id=? AND typeAction=?");
            check.setInt(1,pdiId); check.setString(2,label);
            ResultSet rs=check.executeQuery();
            if(rs.next()){
                PreparedStatement upd=connection.prepareStatement(
                        "UPDATE actionPDI SET statut=? WHERE id=?");
                upd.setString(1,actionStatut); upd.setInt(2,rs.getInt("id")); upd.executeUpdate();
            } else {
                PreparedStatement ins=connection.prepareStatement(
                        "INSERT INTO actionPDI (pdi_id,typeAction,statut,dateDebut,dateFinPrevue,priorite) " +
                                "VALUES (?,?,?,?,?,3)");
                ins.setInt(1,pdiId); ins.setString(2,label); ins.setString(3,actionStatut);
                ins.setDate(4,Date.valueOf(LocalDate.now()));
                ins.setDate(5,Date.valueOf(LocalDate.now().plusMonths(1)));
                ins.executeUpdate();
            }
            recalcPDIProgression(pdiId);
        } catch(SQLException e){ System.err.println("[PDI] Erreur sync: "+e.getMessage()); }
    }

    private int getOrCreatePDI() throws SQLException {
        int year=LocalDate.now().getYear();
        try {
            PreparedStatement ps=connection.prepareStatement(
                    "SELECT id FROM pdi WHERE employe_id=? AND annee=? " +
                            "AND statut IN ('Active','Draft') ORDER BY CASE statut WHEN 'Active' THEN 1 ELSE 2 END LIMIT 1");
            ps.setInt(1,currentUserId); ps.setInt(2,year);
            ResultSet rs=ps.executeQuery(); if(rs.next()) return rs.getInt("id");
        } catch(SQLException ignored) {
            try {
                PreparedStatement ps2=connection.prepareStatement(
                        "SELECT id FROM pdi WHERE annee=? AND statut IN ('Active','Draft') LIMIT 1");
                ps2.setInt(1,year);
                ResultSet rs2=ps2.executeQuery(); if(rs2.next()) return rs2.getInt("id");
            } catch(SQLException ignored2) {}
        }
        return createDraftPDI(year);
    }

    private int createDraftPDI(int year) {
        try {
            PreparedStatement ps=connection.prepareStatement(
                    "INSERT INTO pdi (annee,progressionGlobale,dateCreation,statut,employe_id) VALUES (?,0,?,'Draft',?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1,year); ps.setDate(2,Date.valueOf(LocalDate.now())); ps.setInt(3,currentUserId);
            ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) return k.getInt(1);
        } catch(SQLException e1) {
            try {
                PreparedStatement ps2=connection.prepareStatement(
                        "INSERT INTO pdi (annee,progressionGlobale,dateCreation,statut) VALUES (?,0,?,'Draft')",
                        Statement.RETURN_GENERATED_KEYS);
                ps2.setInt(1,year); ps2.setDate(2,Date.valueOf(LocalDate.now())); ps2.executeUpdate();
                ResultSet k2=ps2.getGeneratedKeys(); if(k2.next()) return k2.getInt(1);
            } catch(SQLException e2) { System.err.println("[PDI] Impossible créer PDI: "+e2.getMessage()); }
        }
        return -1;
    }

    private void recalcPDIProgression(int pdiId) {
        try {
            PreparedStatement ps=connection.prepareStatement(
                    "SELECT COUNT(*) AS total,SUM(CASE WHEN statut='Completed' THEN 1 ELSE 0 END) AS done " +
                            "FROM actionPDI WHERE pdi_id=?");
            ps.setInt(1,pdiId); ResultSet rs=ps.executeQuery();
            if(rs.next()){
                int total=rs.getInt("total"); int pct=total>0?rs.getInt("done")*100/total:0;
                PreparedStatement upd=connection.prepareStatement(
                        "UPDATE pdi SET progressionGlobale=? WHERE id=?");
                upd.setInt(1,pct); upd.setInt(2,pdiId); upd.executeUpdate();
            }
        } catch(SQLException ignored) {}
    }

    public static void recalcPDIStatic(Connection conn, int pdiId) {
        try {
            PreparedStatement ps=conn.prepareStatement(
                    "SELECT COUNT(*) AS total,SUM(CASE WHEN statut='Completed' THEN 1 ELSE 0 END) AS done " +
                            "FROM actionPDI WHERE pdi_id=?");
            ps.setInt(1,pdiId); ResultSet rs=ps.executeQuery();
            if(rs.next()){
                int t=rs.getInt("total"); int pct=t>0?rs.getInt("done")*100/t:0;
                PreparedStatement upd=conn.prepareStatement(
                        "UPDATE pdi SET progressionGlobale=?,statut=? WHERE id=?");
                String newSt=pct>=100?"Completed":"Active";
                upd.setInt(1,pct); upd.setString(2,newSt); upd.setInt(3,pdiId); upd.executeUpdate();
            }
        } catch(SQLException ignored) {}
    }

    public static void markFormationCompletedInPDI(Connection conn, int employeId, String formationTitre) {
        try {
            String label="Formation : "+formationTitre;
            try {
                PreparedStatement ps=conn.prepareStatement(
                        "UPDATE actionPDI SET statut='Completed' WHERE typeAction=? " +
                                "AND pdi_id IN (SELECT id FROM pdi WHERE employe_id=?)");
                ps.setString(1,label); ps.setInt(2,employeId); ps.executeUpdate();
            } catch(SQLException e) {
                PreparedStatement ps2=conn.prepareStatement(
                        "UPDATE actionPDI SET statut='Completed' WHERE typeAction=?");
                ps2.setString(1,label); ps2.executeUpdate();
            }
        } catch(SQLException e){ System.err.println("[PDI] markCompleted: "+e.getMessage()); }
    }

    private int getOrCreateSession(int formationId) throws SQLException {
        PreparedStatement ps=connection.prepareStatement(
                "SELECT id FROM sessionFormation WHERE formation_id=? AND dateFin>=CURDATE() " +
                        "ORDER BY dateDebut LIMIT 1");
        ps.setInt(1,formationId); ResultSet rs=ps.executeQuery();
        if(rs.next()) return rs.getInt("id");
        PreparedStatement ps2=connection.prepareStatement(
                "INSERT INTO sessionFormation (formation_id,dateDebut,dateFin,lieu,statut) " +
                        "VALUES (?,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 30 DAY),'En ligne','Planifiée')",
                Statement.RETURN_GENERATED_KEYS);
        ps2.setInt(1,formationId); ps2.executeUpdate();
        ResultSet k=ps2.getGeneratedKeys(); return k.next()?k.getInt(1):-1;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CRUD (inchangé)
    // ═══════════════════════════════════════════════════════════════════════════
    @FXML private void handleCreateTraining() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Nouvelle Formation");
        ButtonType okBtn = new ButtonType("✚  Créer la formation", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(580);

        VBox root = new VBox(0);
        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20,24,16,24));
        header.setStyle("-fx-background-color:linear-gradient(to right,#4F46E5,#7C3AED);-fx-background-radius:8 8 0 0;");
        Label ico = new Label("🎓"); ico.setStyle("-fx-font-size:28;");
        VBox ht = new VBox(2);
        Label htl = new Label("Nouvelle Formation");
        htl.setStyle("-fx-font-size:17;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hts = new Label("Renseignez les informations de la formation");
        hts.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.75);");
        ht.getChildren().addAll(htl,hts); header.getChildren().addAll(ico,ht);

        VBox form = new VBox(14); form.setPadding(new Insets(22,24,8,24));
        java.util.function.Function<String,Label> sLbl = txt -> {
            Label l=new Label(txt); l.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#6B7280;"); return l;
        };
        String inputSt="-fx-background-color:white;-fx-border-color:#D1D5DB;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;-fx-font-size:13;";

        TextField tfTitle = new TextField(); tfTitle.setPromptText("Ex : Angular Advanced…"); tfTitle.setMaxWidth(Double.MAX_VALUE); tfTitle.setStyle(inputSt);
        TextArea taDesc = new TextArea(); taDesc.setPromptText("Décrivez les objectifs…"); taDesc.setPrefRowCount(3); taDesc.setMaxWidth(Double.MAX_VALUE);
        taDesc.setStyle("-fx-background-color:white;-fx-border-color:#D1D5DB;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;-fx-font-size:13;");

        Spinner<Integer> spDur = new Spinner<>(1,500,8); spDur.setEditable(true); spDur.setPrefWidth(120);
        TextField tfCost = new TextField("0"); tfCost.setMaxWidth(Double.MAX_VALUE); tfCost.setStyle(inputSt);
        HBox durCostRow = new HBox(16);
        VBox durBlock=new VBox(4,sLbl.apply("DURÉE (HEURES)"),spDur);
        VBox costBlock=new VBox(4,sLbl.apply("COÛT (DTN)"),tfCost); HBox.setHgrow(costBlock,Priority.ALWAYS);
        durCostRow.getChildren().addAll(durBlock,costBlock);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("E-Learning","Classroom","Blended","Coaching","Mentoring");
        cbType.setValue("E-Learning"); cbType.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> cbCat = new ComboBox<>();
        loadCategoriesInto(cbCat); cbCat.setMaxWidth(Double.MAX_VALUE);
        HBox typeCatRow = new HBox(16);
        VBox typeBlock=new VBox(4,sLbl.apply("TYPE DE FORMATION"),cbType); HBox.setHgrow(typeBlock,Priority.ALWAYS);
        VBox catBlock=new VBox(4,sLbl.apply("CATÉGORIE"),cbCat); HBox.setHgrow(catBlock,Priority.ALWAYS);
        typeCatRow.getChildren().addAll(typeBlock,catBlock);

        form.getChildren().addAll(
                new VBox(4,sLbl.apply("TITRE *"),tfTitle),
                new VBox(4,sLbl.apply("DESCRIPTION"),taDesc),
                durCostRow, typeCatRow
        );
        root.getChildren().addAll(header,form);
        dlg.getDialogPane().setContent(root);

        Platform.runLater(() -> {
            Button bOk=(Button)dlg.getDialogPane().lookupButton(okBtn);
            if(bOk!=null) bOk.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13;-fx-background-radius:8;-fx-padding:8 20;");
        });

        dlg.setResultConverter(btn -> {
            if(btn==okBtn){
                String titre=tfTitle.getText().trim();
                if(titre.isEmpty()){showError("Champ requis","Le titre est obligatoire.");return null;}
                double cost=0;
                try{cost=Double.parseDouble(tfCost.getText().trim());}catch(Exception ignored){}
                try {
                    PreparedStatement ps=connection.prepareStatement(
                            "INSERT INTO formation(titre,description,duree,cout,typeFormation,statutFormation,categorie_id) " +
                                    "VALUES(?,?,?,?,?,'Active',(SELECT id FROM categorieFormation WHERE libelle=?))");
                    ps.setString(1,titre); ps.setString(2,taDesc.getText());
                    ps.setInt(3,spDur.getValue()); ps.setDouble(4,cost);
                    ps.setString(5,cbType.getValue()); ps.setString(6,cbCat.getValue());
                    ps.executeUpdate();
                    showInfo("Formation créée","« "+titre+" » a été ajoutée au catalogue.");
                    loadTrainings(); loadStatistics();
                } catch(Exception e){showError("Erreur",e.getMessage());}
            }
            return null;
        });
        dlg.showAndWait();
    }

    private void handleEditTraining(TrainingItem t) {
        Dialog<ButtonType> dlg=new Dialog<>(); dlg.setTitle("Modifier Formation");
        ButtonType ok=new ButtonType("Mettre à jour",ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(ok,ButtonType.CANCEL);
        GridPane g=new GridPane(); g.setHgap(12); g.setVgap(12); g.setPadding(new Insets(20)); g.setPrefWidth(480);
        TextField tfTitle=new TextField(t.getTitre()); tfTitle.setPrefWidth(300);
        TextArea taDesc=new TextArea(t.getDescription()); taDesc.setPrefRowCount(3);
        Spinner<Integer> spDur=new Spinner<>(1,500,t.getDuree()); spDur.setEditable(true);
        TextField tfCost=new TextField(String.valueOf(t.getCout()));
        ComboBox<String> cbType=new ComboBox<>();
        cbType.getItems().addAll("Classroom","E-Learning","Blended","Coaching","Mentoring");
        cbType.setValue(t.getTypeFormation()); cbType.setPrefWidth(300);
        ComboBox<String> cbCat=new ComboBox<>();
        loadCategoriesInto(cbCat); cbCat.setValue(t.getCategorie()); cbCat.setPrefWidth(300);
        g.add(new Label("Titre :"),0,0);      g.add(tfTitle,1,0);
        g.add(new Label("Description :"),0,1);g.add(taDesc,1,1);
        g.add(new Label("Durée (h) :"),0,2);  g.add(spDur,1,2);
        g.add(new Label("Coût (DTN) :"),0,3); g.add(tfCost,1,3);
        g.add(new Label("Type :"),0,4);        g.add(cbType,1,4);
        g.add(new Label("Catégorie :"),0,5);   g.add(cbCat,1,5);
        dlg.getDialogPane().setContent(g);
        dlg.setResultConverter(btn -> {
            if(btn==ok){
                try {
                    PreparedStatement ps=connection.prepareStatement(
                            "UPDATE formation SET titre=?,description=?,duree=?,cout=?,typeFormation=?," +
                                    "categorie_id=(SELECT id FROM categorieFormation WHERE libelle=?) WHERE id=?");
                    ps.setString(1,tfTitle.getText()); ps.setString(2,taDesc.getText());
                    ps.setInt(3,spDur.getValue()); ps.setDouble(4,Double.parseDouble(tfCost.getText()));
                    ps.setString(5,cbType.getValue()); ps.setString(6,cbCat.getValue()); ps.setInt(7,t.getId());
                    ps.executeUpdate();
                    showInfo("Mis à jour","Formation mise à jour."); loadTrainings(); loadStatistics();
                } catch(Exception e){showError("Erreur",e.getMessage());}
            }
            return null;
        });
        dlg.showAndWait();
    }

    @FXML private void handleClearFilters() {
        if(searchTrainingsField!=null) searchTrainingsField.clear();
        if(cbFilterType!=null)     cbFilterType.setValue("All Types");
        if(cbFilterCategory!=null) cbFilterCategory.setValue("All Categories");
        if(cbFilterStatus!=null)   cbFilterStatus.setValue("All Statuses");
        selectedType="All Types"; selectedCategory="All Categories"; selectedStatus="All Statuses";
        applyFilters();
    }

    // ★ AI Assistant — UNIQUEMENT au clic bouton, jamais automatique
    @FXML private void handleAiAssistant() {
        Stage stage = (Stage) vboxTrainingsList.getScene().getWindow();
        AiLearningAssistant.getInstance().show(stage);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private List<String> loadModuleTitres(int formationId) {
        List<String> list=new ArrayList<>();
        try {
            PreparedStatement ps=connection.prepareStatement(
                    "SELECT titre FROM module WHERE formation_id=? ORDER BY ordre");
            ps.setInt(1,formationId); ResultSet rs=ps.executeQuery();
            while(rs.next()) list.add(rs.getString("titre"));
        } catch(SQLException ignored) {}
        return list;
    }

    private void loadCategoriesInto(ComboBox<String> combo) {
        try {
            ResultSet rs=connection.prepareStatement(
                    "SELECT libelle FROM categorieFormation ORDER BY libelle").executeQuery();
            while(rs.next()) combo.getItems().add(rs.getString("libelle"));
            if(!combo.getItems().isEmpty()) combo.setValue(combo.getItems().get(0));
        } catch(SQLException e){showError("Erreur",e.getMessage());}
    }

    private Button btn(String text,String bg,String fg,String border,boolean outline){
        Button b=new Button(text);
        b.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:12;" +
                "-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:"+(outline?"8 14":"9 18")+";" +
                "-fx-background-radius:10;"+(border!=null?"-fx-border-color:"+border+";-fx-border-width:1;-fx-border-radius:10;":""));
        return b;
    }

    private void showError(String t,String m){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle(t);a.setContentText(m);a.showAndWait();}
    private void showInfo(String t,String m) {Alert a=new Alert(Alert.AlertType.INFORMATION);a.setTitle(t);a.setContentText(m);a.showAndWait();}

    // ── DTO ───────────────────────────────────────────────────────────────────
    public static class TrainingItem {
        private final int id,duree; private final double cout;
        private final String titre,description,typeFormation,statutFormation,categorie,formateurNom,formateurType;
        public TrainingItem(int id,String titre,String desc,int duree,double cout,String type,String statut,String cat,String fNom,String fType){
            this.id=id;this.titre=titre;this.description=desc;this.duree=duree;this.cout=cout;
            this.typeFormation=type;this.statutFormation=statut;this.categorie=cat;this.formateurNom=fNom;this.formateurType=fType;}
        public int    getId()              {return id;}
        public String getTitre()           {return titre;}
        public String getDescription()     {return description;}
        public int    getDuree()           {return duree;}
        public double getCout()            {return cout;}
        public String getTypeFormation()   {return typeFormation;}
        public String getStatutFormation() {return statutFormation;}
        public String getCategorie()       {return categorie;}
        public String getFormateurNom()    {return formateurNom;}
        public String getFormateurType()   {return formateurType;}
    }
}