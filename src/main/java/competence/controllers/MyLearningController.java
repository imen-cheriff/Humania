package competence.controllers;

import test.MainFX;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import utils.MyDataBase;
import utils.UserSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MyLearningController {

    // ── FXML refs ─────────────────────────────────────────────────────────────
    @FXML private Label  lblInProgress;
    @FXML private Label  lblCompleted;
    @FXML private Label  lblTotalHours;
    @FXML private Label  lblCertificates;
    @FXML private VBox      vboxContinueLearning;
    @FXML private VBox      vboxCompleted;
    @FXML private Button    btnBrowseCatalog;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private ComboBox<String> cbFilterCategory;

    // ── DB / data ─────────────────────────────────────────────────────────────
    private Connection connection;
    private ObservableList<InscriptionItem> allInscriptions;
    private int currentUserId = -1;

    private boolean isRH() {
        String r = UserSession.getInstance().getRole();
        return r != null && (r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("RH"));
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

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Category accent colours ───────────────────────────────────────────────
    private final java.util.Map<String, String> categoryAccents =
            new java.util.HashMap<String, String>() {{
                put("Technical Training",   "#2563EB");
                put("Management Training",  "#059669");
                put("Soft Skills",          "#7C3AED");
                put("DevOps",               "#D97706");
                put("Frontend",             "#0284C7");
                put("Cloud",                "#4F46E5");
                put("Soft Skills Avancées", "#DB2777");
                put("Autre",                "#6B7280");
            }};

    private final java.util.Map<String, String[]> categoryGradients =
            new java.util.HashMap<String, String[]>() {{
                put("Technical Training",   new String[]{"#1e3a5f", "#2563EB"});
                put("Management Training",  new String[]{"#064e3b", "#059669"});
                put("Soft Skills",          new String[]{"#3b0764", "#7C3AED"});
                put("DevOps",               new String[]{"#451a03", "#D97706"});
                put("Frontend",             new String[]{"#0c2a3d", "#0284C7"});
                put("Cloud",                new String[]{"#1e1b4b", "#4F46E5"});
                put("Soft Skills Avancées", new String[]{"#500724", "#DB2777"});
                put("Autre",                new String[]{"#1f2937", "#6B7280"});
            }};

    private final java.util.Map<String, String> categoryImages =
            new java.util.HashMap<String, String>() {{
                put("Technical Training",   "/images/categories/technical.jpg");
                put("Management Training",  "/images/categories/management.jpg");
                put("Soft Skills",          "/images/categories/softskills.jpg");
                put("DevOps",               "/images/categories/devops.jpg");
                put("Frontend",             "/images/categories/frontend.jpg");
                put("Cloud",                "/images/categories/cloud.jpg");
                put("Soft Skills Avancées", "/images/categories/softskills_adv.jpg");
                put("Autre",                "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=320&q=80");
            }};

    // ═════════════════════════════════════════════════════════════════════════
    //  INIT
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        connection      = MyDataBase.getInstance().getCnx();
        allInscriptions = FXCollections.observableArrayList();
        currentUserId   = resolveCurrentUserId();

        if (cbFilterStatus != null) {
            cbFilterStatus.getItems().addAll("All Statuses", "In Progress", "Completed");
            cbFilterStatus.setValue("All Statuses");
        }

        loadStatistics();
        loadInscriptions();

        if (cbFilterCategory != null) {
            cbFilterCategory.getItems().add("All Categories");
            allInscriptions.stream()
                    .map(InscriptionItem::getCategorie)
                    .filter(c -> c != null && !c.isEmpty())
                    .distinct().sorted()
                    .forEach(cbFilterCategory.getItems()::add);
            cbFilterCategory.setValue("All Categories");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  STATISTICS — progression dynamique depuis module_progression
    // ═════════════════════════════════════════════════════════════════════════
    private void loadStatistics() {
        try {
            setStatLabelFiltered(lblInProgress,
                    "SELECT COUNT(*) FROM inscriptionFormation WHERE statut = 'In Progress' AND employe_id = ?");
            setStatLabelFiltered(lblCompleted,
                    "SELECT COUNT(*) FROM inscriptionFormation WHERE statut = 'Completed' AND employe_id = ?");
            try {
                String sql = "SELECT COALESCE(SUM(f.duree),0) " +
                        "FROM inscriptionFormation inf " +
                        "JOIN sessionFormation sf ON inf.session_id = sf.id " +
                        "JOIN formation f ON sf.formation_id = f.id " +
                        "WHERE inf.statut='Completed' AND inf.employe_id = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, currentUserId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && lblTotalHours != null)
                    lblTotalHours.setText(rs.getInt(1) + "h");
            } catch (SQLException ex) {
                if (lblTotalHours != null) lblTotalHours.setText("0h");
            }
            setStatLabelFiltered(lblCertificates,
                    "SELECT COUNT(*) FROM inscriptionFormation " +
                            "WHERE statut='Completed' AND noteFinale>=70 AND employe_id = ?");
        } catch (SQLException e) { showError("Stats", e.getMessage()); }
    }

    private void setStatLabelFiltered(Label l, String sql) throws SQLException {
        if (l == null) return;
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) l.setText(rs.getInt(1) + "");
        } catch (SQLException ignored) { l.setText("0"); }
    }

    private void setStatLabel(Label l, String sql) throws SQLException { setStatLabel(l, sql, ""); }
    private void setStatLabel(Label l, String sql, String suffix) throws SQLException {
        if (l == null) return;
        try {
            ResultSet rs = connection.prepareStatement(sql).executeQuery();
            if (rs.next()) l.setText(rs.getInt(1) + suffix);
        } catch (SQLException ignored) { l.setText("0" + suffix); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LOAD DATA — récupère la progression dynamique depuis module_progression
    // ═════════════════════════════════════════════════════════════════════════
    private void loadInscriptions() {
        allInscriptions.clear();

        // On récupère formation_id et session_id pour pouvoir calculer la progression
        String sql =
                "SELECT inf.id, inf.statut, inf.progression, inf.noteFinale, inf.dateInscription," +
                        " f.id AS formation_id, f.titre, f.duree, cat.libelle AS categorie" +
                        " FROM inscriptionFormation inf" +
                        " JOIN sessionFormation sf ON inf.session_id = sf.id" +
                        " JOIN formation f ON sf.formation_id = f.id" +
                        " LEFT JOIN categorieFormation cat ON f.categorie_id = cat.id" +
                        " WHERE inf.employe_id = " + currentUserId +
                        " ORDER BY inf.dateInscription DESC";

        // Fallback si session_id n'existe pas (ancienne structure)
        String sqlFallback =
                "SELECT inf.id, inf.statut, inf.progression, inf.noteFinale, inf.dateInscription," +
                        " f.id AS formation_id, f.titre, f.duree, cat.libelle AS categorie" +
                        " FROM inscriptionFormation inf" +
                        " JOIN formation f ON inf.formation_id = f.id" +
                        " LEFT JOIN categorieFormation cat ON f.categorie_id = cat.id" +
                        " WHERE inf.employe_id = " + currentUserId +
                        " ORDER BY inf.dateInscription DESC";

        ResultSet rs = null;
        try { rs = connection.prepareStatement(sql).executeQuery(); }
        catch (SQLException e1) {
            try { rs = connection.prepareStatement(sqlFallback).executeQuery(); }
            catch (SQLException e2) { showError("Erreur", e2.getMessage()); return; }
        }

        try {
            while (rs.next()) {
                int formationId = rs.getInt("formation_id");
                int inscId      = rs.getInt("id");
                Date d = rs.getDate("dateInscription");

                // ── Calcul dynamique de la progression depuis module_progression ──
                int dynamicPct = computeDynamicProgress(formationId);

                // Mettre à jour si différent de ce qui est en BDD
                int storedPct = rs.getInt("progression");
                if (dynamicPct != storedPct) {
                    updateInscriptionProgress(inscId, dynamicPct);
                }

                allInscriptions.add(new InscriptionItem(
                        inscId,
                        rs.getString("titre"),
                        rs.getString("statut"),
                        dynamicPct,           // ← progression dynamique
                        rs.getDouble("noteFinale"),
                        d != null ? d.toLocalDate() : null,
                        rs.getInt("duree"),
                        rs.getString("categorie"),
                        formationId           // ← on stocke formation_id
                ));
            }
        } catch (SQLException e) { showError("Erreur", e.getMessage()); }
        displayInscriptions();
    }

    /**
     * Calcule la progression en % = modules_completed / total_modules * 100
     * Retourne la valeur stockée si la table module n'existe pas encore.
     */
    private int computeDynamicProgress(int formationId) {
        try {
            // Total modules pour cette formation
            PreparedStatement ps1 = connection.prepareStatement(
                    "SELECT COUNT(*) FROM module WHERE formation_id=?");
            ps1.setInt(1, formationId);
            ResultSet rs1 = ps1.executeQuery();
            int total = rs1.next() ? rs1.getInt(1) : 0;
            if (total == 0) return 0;

            // Modules completed par cet employé
            PreparedStatement ps2 = connection.prepareStatement(
                    "SELECT COUNT(*) FROM module_progression mp " +
                            "JOIN module m ON mp.module_id=m.id " +
                            "WHERE m.formation_id=? AND mp.employe_id=? AND mp.statut='completed'");
            ps2.setInt(1, formationId);
            ps2.setInt(2, currentUserId);
            ResultSet rs2 = ps2.executeQuery();
            int completed = rs2.next() ? rs2.getInt(1) : 0;

            return completed * 100 / total;
        } catch (SQLException e) {
            return 0; // Table module pas encore créée
        }
    }

    private void updateInscriptionProgress(int inscriptionId, int pct) {
        try {
            String statut = pct >= 100 ? "Completed" : "In Progress";
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE inscriptionFormation SET progression=?, statut=? WHERE id=?");
            ps.setInt(1, pct);
            ps.setString(2, statut);
            ps.setInt(3, inscriptionId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DISPLAY
    // ═════════════════════════════════════════════════════════════════════════
    private void displayInscriptions() {
        displayInscriptions(allInscriptions);
    }

    private void displayInscriptions(java.util.List<InscriptionItem> list) {
        if (vboxContinueLearning != null) vboxContinueLearning.getChildren().clear();
        if (vboxCompleted        != null) vboxCompleted.getChildren().clear();

        HBox row = null;
        int count = 0;
        for (InscriptionItem item : list) {
            if (!"In Progress".equals(item.getStatut())) continue;
            if (count % 2 == 0) {
                row = new HBox(20);
                row.setAlignment(Pos.TOP_LEFT);
                if (vboxContinueLearning != null)
                    vboxContinueLearning.getChildren().add(row);
            }
            if (row != null) {
                HBox card = buildCard(item);
                HBox.setHgrow(card, Priority.ALWAYS);
                card.setMaxWidth(Double.MAX_VALUE);
                row.getChildren().add(card);
            }
            count++;
        }
        if (count % 2 == 1 && row != null) {
            Region filler = new Region();
            HBox.setHgrow(filler, Priority.ALWAYS);
            row.getChildren().add(filler);
        }
        if (count == 0 && vboxContinueLearning != null) {
            Label empty = new Label("No trainings in progress.");
            empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:13;-fx-padding:20 0;");
            vboxContinueLearning.getChildren().add(empty);
        }

        int compCount = 0;
        for (InscriptionItem item : list) {
            if (!"Completed".equals(item.getStatut())) continue;
            if (vboxCompleted != null)
                vboxCompleted.getChildren().add(buildCompletedRow(item));
            compCount++;
        }
        if (compCount == 0 && vboxCompleted != null) {
            Label empty = new Label("No completed trainings.");
            empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:13;-fx-padding:20 0;");
            vboxCompleted.getChildren().add(empty);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CARD — progression dynamique
    // ═════════════════════════════════════════════════════════════════════════
    private HBox buildCard(InscriptionItem item) {
        String cat    = item.getCategorie() != null ? item.getCategorie() : "Autre";
        String accent = categoryAccents.getOrDefault(cat, "#6B7280");
        String[] grad = categoryGradients.getOrDefault(cat, new String[]{"#1f2937","#6B7280"});

        HBox card = new HBox(0);
        card.setPrefHeight(200); card.setMinHeight(200); card.setMaxHeight(200);
        card.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2);");

        // Thumbnail
        StackPane thumb = new StackPane();
        thumb.setPrefWidth(160); thumb.setMinWidth(160); thumb.setMaxWidth(160); thumb.setPrefHeight(200);
        Rectangle gradBg = new Rectangle(160,200);
        try {
            gradBg.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0,Color.web(grad[0])),new Stop(1,Color.web(grad[1]))));
        } catch (Exception e) { gradBg.setFill(Color.web(accent)); }
        ImageView imgView = new ImageView();
        imgView.setFitWidth(160); imgView.setFitHeight(200); imgView.setPreserveRatio(false);
        try { imgView.setImage(new Image(categoryImages.getOrDefault(cat,""), true)); } catch (Exception ignored) {}
        Rectangle clip = new Rectangle(160,200); clip.setArcWidth(28); clip.setArcHeight(28);
        thumb.setClip(clip);
        Rectangle rightSquare = new Rectangle(14,200); rightSquare.setFill(Color.WHITE);
        StackPane.setAlignment(rightSquare, Pos.CENTER_RIGHT);
        thumb.getChildren().addAll(gradBg,imgView,rightSquare);

        // Content
        VBox content = new VBox(0);
        content.setPadding(new Insets(16,20,14,18));
        HBox.setHgrow(content, Priority.ALWAYS);

        Label catBadge = new Label(cat);
        catBadge.setStyle("-fx-background-color:"+accent+"50;-fx-text-fill:white;" +
                "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:6;");
        HBox badgeRow = new HBox(catBadge); badgeRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(badgeRow, new Insets(0,0,6,0));

        Label titleLbl = new Label(item.getTitre());
        titleLbl.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#111827;");
        titleLbl.setWrapText(true);
        VBox.setMargin(titleLbl, new Insets(0,0,10,0));

        // Progress with dynamic %
        HBox progHeader = new HBox();
        progHeader.setAlignment(Pos.CENTER_LEFT);
        Label progLbl = new Label("Progress"); progLbl.setStyle("-fx-font-size:12;-fx-text-fill:#6B7280;");
        Region ph = new Region(); HBox.setHgrow(ph,Priority.ALWAYS);
        Label progPct = new Label(item.getProgression()+"%");
        progPct.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#111827;");
        progHeader.getChildren().addAll(progLbl,ph,progPct);

        StackPane barWrap = new StackPane(); barWrap.setPrefHeight(6); barWrap.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(barWrap, new Insets(5,0,10,0));
        Rectangle track = new Rectangle(0,6); track.setArcWidth(6); track.setArcHeight(6);
        track.setFill(Color.web("#E5E7EB")); track.widthProperty().bind(barWrap.widthProperty());
        Rectangle fillBar = new Rectangle(0,6); fillBar.setArcWidth(6); fillBar.setArcHeight(6);
        fillBar.setFill(Color.web(accent)); StackPane.setAlignment(fillBar,Pos.CENTER_LEFT);
        barWrap.widthProperty().addListener((obs,o,newW)->
                fillBar.setWidth(newW.doubleValue()*item.getProgression()/100.0));
        barWrap.getChildren().addAll(track,fillBar);

        // Module count from DB
        int totalMods = getModuleCount(item.getFormationId());
        int donesMods = getDoneModuleCount(item.getFormationId());
        HBox metaRow = new HBox(18); metaRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(metaRow, new Insets(0,0,8,0));
        int totalMins = item.getDuree()*60, elapsed=(int)(totalMins*item.getProgression()/100.0);
        Label timeLbl = new Label("⏱  "+elapsed/60+"h "+elapsed%60+"m / "+item.getDuree()+"h");
        timeLbl.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        Label modLbl = new Label("☰  "+donesMods+"/"+totalMods+" modules");
        modLbl.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        metaRow.getChildren().addAll(timeLbl,modLbl);

        Region vSpacer = new Region(); VBox.setVgrow(vSpacer,Priority.ALWAYS);

        HBox footer = new HBox(); footer.setAlignment(Pos.CENTER_LEFT);
        Label nextLbl = new Label("Next: continue training"); nextLbl.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;");
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer,Priority.ALWAYS);
        Button btnResume = new Button("▶  Resume");
        btnResume.setStyle("-fx-background-color:"+accent+";-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-font-size:12;-fx-padding:9 24;" +
                "-fx-background-radius:22;-fx-cursor:hand;");
        btnResume.setOnMouseEntered(e->btnResume.setStyle("-fx-background-color:derive("+accent+",-12%);" +
                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;" +
                "-fx-padding:9 24;-fx-background-radius:22;-fx-cursor:hand;"));
        btnResume.setOnMouseExited(e->btnResume.setStyle("-fx-background-color:"+accent+";" +
                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;" +
                "-fx-padding:9 24;-fx-background-radius:22;-fx-cursor:hand;"));
        // ── NAVIGATION VERS MODULESVIEW ──
        btnResume.setOnAction(e -> handleResume(item));
        footer.getChildren().addAll(nextLbl,hSpacer,btnResume);

        content.getChildren().addAll(badgeRow,titleLbl,progHeader,barWrap,vSpacer,metaRow,footer);
        card.getChildren().addAll(thumb,content);

        String cardBase  = "-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2);";
        String cardHover = "-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:"+accent+";-fx-border-width:1;-fx-border-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.13),16,0,0,3);";
        card.setStyle(cardBase);
        card.setOnMouseEntered(e->card.setStyle(cardHover));
        card.setOnMouseExited(e->card.setStyle(cardBase));
        return card;
    }

    private int getModuleCount(int formationId) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM module WHERE formation_id=?");
            ps.setInt(1,formationId); ResultSet rs=ps.executeQuery();
            return rs.next()?rs.getInt(1):0;
        } catch (SQLException e) { return 0; }
    }

    private int getDoneModuleCount(int formationId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM module_progression mp JOIN module m ON mp.module_id=m.id " +
                            "WHERE m.formation_id=? AND mp.employe_id=? AND mp.statut='completed'");
            ps.setInt(1,formationId); ps.setInt(2,currentUserId);
            ResultSet rs=ps.executeQuery();
            return rs.next()?rs.getInt(1):0;
        } catch (SQLException e) { return 0; }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HANDLE RESUME — ouvre ModulesView
    // ═════════════════════════════════════════════════════════════════════════
    private void handleResume(InscriptionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/competence/ModulesView.fxml"));
            Parent view = loader.load();

            ModulesViewController ctrl = loader.getController();
            ctrl.setEmployeId(currentUserId);   // ← CORRECTION : transmettre l'utilisateur connecté
            ctrl.setFormation(item.getFormationId(), item.getId(), item.getTitre());

            // Remplacer le contenu central dans la même fenêtre
            MainFX main = MainFX.getInstance();
            if (main != null) {
                main.setCenter(view);
            }
        } catch (Exception e) {
            showError("Navigation", "Impossible d'ouvrir les modules: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  COMPLETED ROW
    // ═════════════════════════════════════════════════════════════════════════
    private HBox buildCompletedRow(InscriptionItem item) {
        String cat    = item.getCategorie() != null ? item.getCategorie() : "Autre";
        String accent = categoryAccents.getOrDefault(cat, "#6B7280");

        HBox row = new HBox(16); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16,24,16,16));
        row.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
                "-fx-border-color:#F3F4F6;-fx-border-width:1;-fx-border-radius:12;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),8,0,0,1);");

        StackPane chk = new StackPane(); chk.setPrefSize(40,40); chk.setMinSize(40,40);
        Circle bg = new Circle(20, Color.web("#D1FAE5"));
        Label icon = new Label("✓"); icon.setStyle("-fx-text-fill:#059669;-fx-font-size:16;-fx-font-weight:bold;");
        chk.getChildren().addAll(bg,icon);

        VBox info = new VBox(4); HBox.setHgrow(info,Priority.ALWAYS);
        Label titleLbl = new Label(item.getTitre());
        titleLbl.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#111827;");
        HBox meta = new HBox(12); meta.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(cat);
        catBadge.setStyle("-fx-background-color:"+accent+"18;-fx-text-fill:"+accent+";" +
                "-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:6;");
        String dateStr = item.getDateInscription()!=null ? "📅 "+item.getDateInscription().format(DATE_FMT) : "";
        Label dateLbl = new Label(dateStr); dateLbl.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:11;");
        meta.getChildren().addAll(catBadge,dateLbl);
        info.getChildren().addAll(titleLbl,meta);

        double note = item.getNoteFinale();
        String gc=note>=90?"#059669":note>=70?"#2563EB":"#DC2626";
        String gb=note>=90?"#D1FAE5":note>=70?"#DBEAFE":"#FEE2E2";
        VBox gradeBox = new VBox(1); gradeBox.setAlignment(Pos.CENTER);
        gradeBox.setPadding(new Insets(8,16,8,16));
        gradeBox.setStyle("-fx-background-color:"+gb+";-fx-background-radius:10;");
        Label pctLbl = new Label((int)note+"%");
        pctLbl.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:"+gc+";");
        Label mentionLbl = new Label(getMention(note));
        mentionLbl.setStyle("-fx-font-size:10;-fx-text-fill:"+gc+";");
        gradeBox.getChildren().addAll(pctLbl,mentionLbl);

        Button btnCert = new Button("🏆  Certificate");
        btnCert.setStyle("-fx-background-color:transparent;-fx-text-fill:"+accent+";" +
                "-fx-font-weight:bold;-fx-font-size:12;-fx-padding:8 18;" +
                "-fx-border-color:"+accent+";-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
        btnCert.setOnAction(e->handleCertificate(item));

        row.getChildren().addAll(chk,info,gradeBox,btnCert);
        String rowBase  = "-fx-background-color:white;-fx-background-radius:12;" +
                "-fx-border-color:#F3F4F6;-fx-border-width:1;-fx-border-radius:12;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),8,0,0,1);";
        String rowHover = "-fx-background-color:#FAFAFA;-fx-background-radius:12;" +
                "-fx-border-color:"+accent+";-fx-border-width:1;-fx-border-radius:12;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),14,0,0,3);";
        row.setOnMouseEntered(e->row.setStyle(rowHover));
        row.setOnMouseExited(e->row.setStyle(rowBase));
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FILTERS & HANDLERS
    // ═════════════════════════════════════════════════════════════════════════
    @FXML private void handleSearch() {
        String keyword  = searchField!=null?searchField.getText().trim().toLowerCase():"";
        String status   = cbFilterStatus!=null?cbFilterStatus.getValue():"All Statuses";
        String category = cbFilterCategory!=null?cbFilterCategory.getValue():"All Categories";
        java.util.List<InscriptionItem> filtered = allInscriptions.stream().filter(item->{
            if(!keyword.isEmpty()&&!item.getTitre().toLowerCase().contains(keyword)) return false;
            if(status!=null&&!status.equals("All Statuses")&&!status.equals(item.getStatut())) return false;
            if(category!=null&&!category.equals("All Categories")&&!category.equals(item.getCategorie())) return false;
            return true;
        }).collect(java.util.stream.Collectors.toList());
        displayInscriptions(filtered);
    }

    @FXML private void handleClearFilters() {
        if(searchField!=null) searchField.clear();
        if(cbFilterStatus!=null) cbFilterStatus.setValue("All Statuses");
        if(cbFilterCategory!=null) cbFilterCategory.setValue("All Categories");
        displayInscriptions(allInscriptions);
    }

    @FXML private void handleBrowseCatalog() {
        MainFX main = MainFX.getInstance();
        if(main!=null) main.navigateTo(MainFX.TRAINING_CATALOG);
        else showError("Navigation Error","MainFX instance not found.");
    }

    private void handleCertificate(InscriptionItem item) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Certificate"); a.setHeaderText("🏆  "+item.getTitre());
        a.setContentText("Category: "+item.getCategorie()+"\nGrade: "+(int)item.getNoteFinale()+
                "%\nMention: "+getMention(item.getNoteFinale())+"\nDate: "+
                (item.getDateInscription()!=null?item.getDateInscription().format(DATE_FMT):"N/A"));
        a.showAndWait();
    }

    private String getMention(double note) {
        if(note>=90) return "Excellent"; if(note>=80) return "Very Good";
        if(note>=70) return "Good";      if(note>=60) return "Pass";
        return "Insufficient";
    }
    private void showError(String t,String m){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle(t);a.setContentText(m);a.showAndWait();}
    private void showInfo(String t,String m) {Alert a=new Alert(Alert.AlertType.INFORMATION);a.setTitle(t);a.setContentText(m);a.showAndWait();}

    // ═════════════════════════════════════════════════════════════════════════
    //  INNER CLASS — ajout de formationId
    // ═════════════════════════════════════════════════════════════════════════
    public static class InscriptionItem {
        private final int id,duree,progression,formationId;
        private final double noteFinale;
        private final String titre,statut,categorie;
        private final LocalDate dateInscription;

        public InscriptionItem(int id,String titre,String statut,int progression,
                               double noteFinale,LocalDate dateInscription,
                               int duree,String categorie,int formationId) {
            this.id=id;this.titre=titre;this.statut=statut;this.progression=progression;
            this.noteFinale=noteFinale;this.dateInscription=dateInscription;
            this.duree=duree;this.categorie=categorie;this.formationId=formationId;
        }
        public int       getId()              { return id; }
        public String    getTitre()           { return titre; }
        public String    getStatut()          { return statut; }
        public int       getProgression()     { return progression; }
        public double    getNoteFinale()      { return noteFinale; }
        public LocalDate getDateInscription() { return dateInscription; }
        public int       getDuree()           { return duree; }
        public String    getCategorie()       { return categorie; }
        public int       getFormationId()     { return formationId; }
    }
}