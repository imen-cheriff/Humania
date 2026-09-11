package competence.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import utils.MyDataBase;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class ModulesViewController {

    @FXML private Label  lblFormationTitre;
    @FXML private Label  lblProgressPct;
    @FXML private Label  lblModuleCount;
    @FXML private Label  lblFormateur;
    @FXML private Label  lblTypeFormation;
    @FXML private HBox   hboxFormateur;
    @FXML private Region barFill;
    @FXML private Region barTrack;
    @FXML private VBox   vboxModules;
    @FXML private Button btnBack;
    // Nouveaux boutons IA + YouTube
    @FXML private Button btnSkillsGap;
    @FXML private Button btnGeneratePDI;
    @FXML private Button btnYoutubeSuggest;
    @FXML private Label  lblAiStatus;

    private Connection connection;
    private int    formationId;
    private int    inscriptionId;
    private int    employeId = 1;
    private String formationTitre;
    private VBox   currentExpandedContent = null;
    private Stage  mainStage;
    private Parent modulesViewRoot;

    // ── API Keys ──────────────────────────────────────────────────────────
    private String groqApiKey;    // Groq (principal)
    private String grokApiKey;    // Grok xAI (fallback)
    private String youtubeApiKey;
    // rétrocompat : cohere.key → utilisé si aucune clé disponible
    private String cohereApiKey;

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROK_URL =
            "https://api.x.ai/v1/chat/completions";

    @FXML public void initialize() {
        connection = MyDataBase.getInstance().getCnx();
        loadApiKeys();
    }

    private void loadApiKeys() {
        String[] paths = {
                "src/main/resources/config.properties",
                "config.properties",
                "src/main/resources/anthropic.properties",
                "anthropic.properties"
        };
        for (String path : paths) {
            try (java.io.InputStream is = new java.io.FileInputStream(path)) {
                java.util.Properties p = new java.util.Properties();
                p.load(is);
                if (groqApiKey    == null) groqApiKey    = p.getProperty("groq.key");
                if (grokApiKey    == null) grokApiKey    = p.getProperty("grok.key");
                if (youtubeApiKey == null) youtubeApiKey = p.getProperty("youtube.key", p.getProperty("youtube.api.key"));
                if (cohereApiKey  == null) cohereApiKey  = p.getProperty("cohere.key");
                if (groqApiKey != null && youtubeApiKey != null) break;
            } catch (Exception ignored) {}
        }
        // Fallback classloader
        try (java.io.InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                java.util.Properties p = new java.util.Properties(); p.load(is);
                if (groqApiKey    == null) groqApiKey    = p.getProperty("groq.key");
                if (grokApiKey    == null) grokApiKey    = p.getProperty("grok.key");
                if (youtubeApiKey == null) youtubeApiKey = p.getProperty("youtube.key");
            }
        } catch (Exception ignored) {}
        System.out.println("[ModulesVC] Groq: " + (groqApiKey != null ? "✓" : "✗") +
                "  Grok: " + (grokApiKey != null ? "✓" : "✗") +
                "  YouTube: " + (youtubeApiKey != null ? "✓" : "✗"));
    }

    /** Appelé par le parent pour injecter la fenêtre principale */
    public void setMainStage(Stage stage) { this.mainStage = stage; }

    /** Appelé par le parent pour stocker la racine FXML (retour depuis ModuleLearner) */
    public void setViewRoot(Parent root)  { this.modulesViewRoot = root; }

    /** Doit être appelé AVANT setFormation() pour injecter l'utilisateur connecté */
    public void setEmployeId(int employeId) {
        this.employeId = employeId;
    }

    public void setFormation(int formationId, int inscriptionId, String titre) {
        this.formationId    = formationId;
        this.inscriptionId  = inscriptionId;
        this.formationTitre = titre;
        // Ne pas écraser employeId ici — il doit être passé via setEmployeId() avant cet appel
        if (lblFormationTitre != null) lblFormationTitre.setText(titre);
        loadFormationMeta();
        loadAndDisplay();
    }

    private void loadFormationMeta() {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT f.typeFormation, CONCAT(fo.prenom,' ',fo.nom) AS formateur_nom, fo.type AS formateur_type " +
                            "FROM formation f LEFT JOIN formateur fo ON fo.id=f.formateur_id WHERE f.id=?");
            ps.setInt(1, formationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { applyMeta(rs.getString("typeFormation"), rs.getString("formateur_nom"), rs.getString("formateur_type")); return; }
        } catch (SQLException ignored) {}
        try {
            PreparedStatement ps2 = connection.prepareStatement("SELECT typeFormation, formateur_nom, formateur_type FROM formation WHERE id=?");
            ps2.setInt(1, formationId); ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) applyMeta(rs2.getString("typeFormation"), rs2.getString("formateur_nom"), rs2.getString("formateur_type"));
        } catch (SQLException e2) {
            try {
                PreparedStatement ps3 = connection.prepareStatement("SELECT typeFormation FROM formation WHERE id=?");
                ps3.setInt(1, formationId); ResultSet rs3 = ps3.executeQuery();
                if (rs3.next()) applyMeta(rs3.getString("typeFormation"), null, null);
            } catch (SQLException ignored3) {}
        }
        if (hboxFormateur != null) { hboxFormateur.setVisible(false); hboxFormateur.setManaged(false); }
    }

    private void applyMeta(String type, String formateurNom, String formateurType) {
        if (lblTypeFormation != null && type != null) lblTypeFormation.setText(getTypeIcon(type) + "  " + type);
        if (lblFormateur != null && formateurNom != null && !formateurNom.isBlank()
                && !formateurNom.trim().equalsIgnoreCase("null null")) {
            lblFormateur.setText(formateurNom + ("EXTERNE".equalsIgnoreCase(formateurType) ? " · 🌐 Formateur externe" : " · 🏢 Formateur interne"));
            if (hboxFormateur != null) { hboxFormateur.setVisible(true); hboxFormateur.setManaged(true); }
        } else if (hboxFormateur != null) { hboxFormateur.setVisible(false); hboxFormateur.setManaged(false); }
    }

    private String getTypeIcon(String t) {
        if (t == null) return "📋";
        switch (t.toUpperCase()) {
            case "E_LEARNING": return "💻";
            case "CLASSROOM":  return "🏫";
            case "COACHING":   return "🤝";
            case "BLENDED":    return "🔀";
            default:           return "📋";
        }
    }

    private void loadAndDisplay() {
        List<ModuleItem> modules = loadModules();
        int total = modules.size();
        int completed = (int) modules.stream().filter(m -> "completed".equals(m.statut)).count();
        int pct = total == 0 ? 0 : completed * 100 / total;

        if (lblProgressPct != null) lblProgressPct.setText(pct + "%");
        if (lblModuleCount != null) lblModuleCount.setText(completed + "/" + total + " modules");
        updateProgressBar(pct);
        updateInscriptionProgress(pct);

        if (vboxModules != null) {
            vboxModules.getChildren().clear();
            currentExpandedContent = null;
            if (modules.isEmpty()) { vboxModules.getChildren().add(buildEmpty()); }
            else {
                for (int i = 0; i < modules.size(); i++) {
                    VBox card = buildModuleCard(modules.get(i), modules, i);
                    vboxModules.getChildren().add(card);
                    card.setOpacity(0);
                    FadeTransition ft = new FadeTransition(Duration.millis(280), card);
                    ft.setDelay(Duration.millis(i * 70)); ft.setFromValue(0); ft.setToValue(1); ft.play();
                }
            }
        }
    }

    private VBox buildEmpty() {
        VBox box = new VBox(14); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(60));
        box.setStyle("-fx-background-color:white;-fx-background-radius:16;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:16;");
        Label ic = new Label("📭"); ic.setStyle("-fx-font-size:48;");
        Label t = new Label("Aucun module disponible"); t.setStyle("-fx-font-size:17;-fx-font-weight:bold;-fx-text-fill:#374151;");
        Label s = new Label("Cette formation ne contient pas encore de modules."); s.setStyle("-fx-font-size:13;-fx-text-fill:#9CA3AF;");
        box.getChildren().addAll(ic, t, s); return box;
    }

    private void updateProgressBar(int pct) {
        if (barFill == null || barTrack == null) return;
        barTrack.widthProperty().addListener((obs, o, n) -> barFill.setPrefWidth(n.doubleValue() * pct / 100.0));
        barFill.setPrefWidth(0);
    }

    private List<ModuleItem> loadModules() {
        List<ModuleItem> list = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT m.id, m.titre, m.description, m.type_contenu, m.duree_minutes, m.ordre," +
                            " IFNULL(m.contenu_texte,'') AS contenu_texte, IFNULL(m.url_ressource,'') AS url_ressource," +
                            " IFNULL(mp.statut,'not_started') AS statut, IFNULL(mp.score_quiz,-1) AS score" +
                            " FROM module m LEFT JOIN module_progression mp ON mp.module_id=m.id AND mp.employe_id=?" +
                            " WHERE m.formation_id=? ORDER BY m.ordre");
            ps.setInt(1, employeId); ps.setInt(2, formationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new ModuleItem(rs));
        } catch (SQLException e) {
            try {
                PreparedStatement ps2 = connection.prepareStatement(
                        "SELECT m.id, m.titre, m.description, m.type_contenu, m.duree_minutes, m.ordre," +
                                " IFNULL(mp.statut,'not_started') AS statut, IFNULL(mp.score_quiz,-1) AS score" +
                                " FROM module m LEFT JOIN module_progression mp ON mp.module_id=m.id AND mp.employe_id=?" +
                                " WHERE m.formation_id=? ORDER BY m.ordre");
                ps2.setInt(1, employeId); ps2.setInt(2, formationId);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) list.add(new ModuleItem(rs2, true));
            } catch (SQLException e2) { showError("Modules", e2.getMessage()); }
        }
        return list;
    }

    private void updateInscriptionProgress(int pct) {
        if (inscriptionId <= 0) return;
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE inscriptionFormation SET progression=?, statut=? WHERE id=?");
            ps.setInt(1, pct); ps.setString(2, pct >= 100 ? "Completed" : "In Progress"); ps.setInt(3, inscriptionId); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CARD BUILDER
    // ═════════════════════════════════════════════════════════════════════════
    private VBox buildModuleCard(ModuleItem m, List<ModuleItem> all, int idx) {
        boolean done   = "completed".equals(m.statut);
        boolean inProg = "in_progress".equals(m.statut);
        boolean open   = isAccessible(m, all);

        VBox card = new VBox(0); card.setMaxWidth(Double.MAX_VALUE);
        String cardBg = done ? "#F0FDF4" : inProg ? "#EFF6FF" : "white";
        String border = done ? "#86EFAC" : inProg ? "#93C5FD" : "#E5E7EB";
        String normalStyle = "-fx-background-color:" + cardBg + ";-fx-background-radius:14;" +
                "-fx-border-color:" + border + ";-fx-border-width:1;-fx-border-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);";
        card.setStyle(normalStyle);

        HBox header = new HBox(14); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(16, 20, 16, 18));
        StackPane badge = new StackPane(); badge.setPrefSize(42, 42); badge.setMinSize(42, 42);
        Circle bgC = new Circle(21);
        bgC.setFill(Color.web(done ? "#D1FAE5" : inProg ? "#DBEAFE" : open ? "#EDE9FE" : "#F3F4F6"));
        Label badgeLbl = new Label(done ? "✓" : String.valueOf(m.ordre));
        badgeLbl.setStyle("-fx-font-size:" + (done?"16":"15") + ";-fx-font-weight:bold;-fx-text-fill:" +
                (done?"#059669":inProg?"#1D4ED8":open?"#5B21B6":"#9CA3AF") + ";");
        badge.getChildren().addAll(bgC, badgeLbl);

        Label emoji = new Label(typeEmoji(m.typeContenu, done)); emoji.setStyle("-fx-font-size:20;-fx-min-width:28;");

        VBox info = new VBox(5); HBox.setHgrow(info, Priority.ALWAYS);
        HBox titleRow = new HBox(8); titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(m.titre);
        title.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:" + (done?"#166534":inProg?"#1E40AF":"#111827") + ";");
        title.setWrapText(true); HBox.setHgrow(title, Priority.ALWAYS);
        titleRow.getChildren().add(title);
        if (done)        titleRow.getChildren().add(chip("✓ Terminé", "#D1FAE5", "#166534"));
        else if (inProg) titleRow.getChildren().add(chip("En cours",  "#DBEAFE", "#1D4ED8"));

        HBox meta = new HBox(10); meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(typeChip(m.typeContenu), metaLabel("⏱ " + m.dureeMinutes + " min"));
        if (done && m.scoreQuiz >= 0) meta.getChildren().add(chip("⭐ " + m.scoreQuiz + "%", "#FEF3C7", "#92400E"));

        if (m.description != null && !m.description.isBlank()) {
            Label desc = new Label(m.description); desc.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;"); desc.setWrapText(true);
            info.getChildren().addAll(titleRow, desc, meta);
        } else info.getChildren().addAll(titleRow, meta);

        HBox action = new HBox(8); action.setAlignment(Pos.CENTER_RIGHT); action.setMinWidth(180);
        VBox contentArea = new VBox(0); contentArea.setVisible(false); contentArea.setManaged(false);

        if (done) {
            if (!"quiz".equals(m.typeContenu)) {
                // Bouton Expand : preview du contenu dans la carte
                Button btnExpand = new Button("⌄  Aperçu");
                btnExpand.setStyle("-fx-background-color:white;-fx-text-fill:#6B7280;" +
                        "-fx-font-size:11;-fx-padding:7 14;-fx-background-radius:8;" +
                        "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
                btnExpand.setOnAction(e -> {
                    boolean expanding = !contentArea.isVisible();
                    toggle(contentArea, m, false);
                    btnExpand.setText(expanding ? "⌃  Fermer" : "⌄  Aperçu");
                });

                // Bouton Revoir : ouvre ModuleLearner complet
                Button btnReview = actionBtn("▶  Revoir", "#4F46E5", "white", null, false);
                btnReview.setOnAction(e -> openModuleLearner(m));

                action.getChildren().addAll(btnExpand, btnReview);
            }
        } else if (open) {
            // Bouton Expand : preview du contenu dans la carte (pour les non-video)
            if (!"quiz".equals(m.typeContenu) && !"video".equals(m.typeContenu)) {
                Button btnExpand = new Button("⌄  Aperçu");
                btnExpand.setStyle("-fx-background-color:white;-fx-text-fill:#6B7280;" +
                        "-fx-font-size:11;-fx-padding:7 14;-fx-background-radius:8;" +
                        "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
                btnExpand.setOnAction(e -> {
                    boolean expanding = !contentArea.isVisible();
                    toggle(contentArea, m, false);
                    btnExpand.setText(expanding ? "⌃  Fermer" : "⌄  Aperçu");
                });
                action.getChildren().add(btnExpand);
            }
            Button btnStart = actionBtn(inProg ? "▶  Continuer" : "▶  Démarrer",
                    inProg ? "#2563EB" : "#4F46E5", "white", null, false);
            if ("quiz".equals(m.typeContenu)) btnStart.setOnAction(e -> openModuleLearner(m));
            else                              btnStart.setOnAction(e -> openModuleLearner(m));
            action.getChildren().add(btnStart);
        } else {
            VBox locked = new VBox(3); locked.setAlignment(Pos.CENTER);
            Label lkIc = new Label("🔒"); lkIc.setStyle("-fx-font-size:18;-fx-text-fill:#D1D5DB;");
            Label lkLb = new Label("Verrouillé"); lkLb.setStyle("-fx-font-size:10;-fx-text-fill:#D1D5DB;");
            locked.getChildren().addAll(lkIc, lkLb);
            action.getChildren().add(locked);
        }

        header.getChildren().addAll(badge, emoji, info, action);
        card.getChildren().addAll(header, contentArea);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:"+cardBg+";-fx-background-radius:14;-fx-border-color:"+border+";-fx-border-width:1;-fx-border-radius:14;-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.12),14,0,0,4);-fx-translate-y:-1;"));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CONTENT EXPAND / COLLAPSE
    // ═════════════════════════════════════════════════════════════════════════
    private void toggle(VBox area, ModuleItem m, boolean markComplete) {
        if (currentExpandedContent != null && currentExpandedContent != area && currentExpandedContent.isVisible()) collapse(currentExpandedContent);
        if (area.isVisible()) { collapse(area); return; }
        buildRichContent(area, m, markComplete); expand(area); currentExpandedContent = area;
    }
    private void expand(VBox c) { c.setVisible(true); c.setManaged(true); FadeTransition ft = new FadeTransition(Duration.millis(220), c); ft.setFromValue(0); ft.setToValue(1); ft.play(); }
    private void collapse(VBox c) { FadeTransition ft = new FadeTransition(Duration.millis(160), c); ft.setFromValue(1); ft.setToValue(0); ft.setOnFinished(e -> { c.setVisible(false); c.setManaged(false); }); ft.play(); }

    // ══════════════════════════════════════════════════════════════════════════
    //  BUILD CONTENT — switch strict sur type_contenu
    //  "reading"  → texte formaté, JAMAIS de vidéo
    //  "video"    → player YouTube thumbnail
    //  "exercise" → zone de rendu + correction IA
    // ══════════════════════════════════════════════════════════════════════════
    private void buildRichContent(VBox area, ModuleItem m, boolean markComplete) {
        area.getChildren().clear();
        Region sep = new Region(); sep.setPrefHeight(1); sep.setStyle("-fx-background-color:#E5E7EB;");
        VBox inner = new VBox(16); inner.setPadding(new Insets(20, 24, 20, 24));
        inner.setStyle("-fx-background-color:#FAFAFA;-fx-background-radius:0 0 14 14;");

        String url  = m.urlRessource != null ? m.urlRessource.trim() : "";
        String text = m.contenuTexte != null ? m.contenuTexte.trim() : "";
        String type = m.typeContenu  != null ? m.typeContenu         : "";

        switch (type) {

            case "reading":
                // ── LECTURE : texte mis en forme (titres, listes, code), JAMAIS de vidéo
                inner.getChildren().add(sectionHeader("📖  Contenu du cours"));
                if (!text.isEmpty()) inner.getChildren().add(buildReadingContent(text));
                if (!url.isEmpty())  inner.getChildren().add(buildUrlCard(url));
                if (text.isEmpty() && url.isEmpty()) inner.getChildren().add(noMedia("📖", "Aucun contenu de lecture disponible."));
                break;

            case "video":
                // ── VIDÉO : thumbnail cliquable → navigateur (fix Error 153)
                inner.getChildren().add(sectionHeader("🎬  Vidéo du cours"));
                if (!url.isEmpty()) {
                    boolean isYtV = url.contains("youtube.com") || url.contains("youtu.be");
                    boolean isVmV = url.contains("vimeo.com");
                    inner.getChildren().add(buildVideoPlayer(url, isYtV, isVmV));
                } else inner.getChildren().add(noMedia("🎬", "Aucune vidéo associée à ce module."));
                if (!text.isEmpty()) inner.getChildren().add(buildReadingContent(text));
                break;

            case "exercise":
                // ── EXERCICE : instructions + zone de rendu + correction IA
                inner.getChildren().add(sectionHeader("🏋  Exercice pratique"));
                if (!text.isEmpty()) inner.getChildren().add(buildExerciseSubmission(text, m.id, -1));
                else inner.getChildren().add(noMedia("🏋", "Aucune instruction disponible."));
                if (!url.isEmpty()) inner.getChildren().add(buildUrlCard(url));
                break;

            case "quiz":
                inner.getChildren().add(noMedia("📝", "Ouvrez le quiz via le bouton Démarrer."));
                break;

            default:
                // Fallback legacy : détecter depuis l'URL
                boolean isYt    = url.contains("youtube.com") || url.contains("youtu.be");
                boolean isVm    = url.contains("vimeo.com");
                boolean isVideo = isYt || isVm || url.endsWith(".mp4") || url.endsWith(".webm");
                boolean isPdf   = url.endsWith(".pdf");

                if (isVideo) { inner.getChildren().add(sectionHeader("🎬  Vidéo du cours")); inner.getChildren().add(buildVideoPlayer(url, isYt, isVm)); }
                else if (isPdf && !url.isEmpty()) { inner.getChildren().add(sectionHeader("📄  Document PDF")); inner.getChildren().add(buildPdfViewer(url)); }
                else {
                    if (!text.isEmpty()) inner.getChildren().add(buildReadingContent(text));
                    if (!url.isEmpty())  inner.getChildren().add(buildUrlCard(url));
                    if (text.isEmpty() && url.isEmpty()) inner.getChildren().add(noMedia("📄", "Aucun support disponible."));
                }
                break;
        }

        if (markComplete) {
            HBox btnRow = new HBox(); btnRow.setAlignment(Pos.CENTER_RIGHT);
            Button done = actionBtn("✓  Marquer comme terminé", "#059669", "white", null, false);
            done.setOnAction(e -> { markModuleCompleted(m.id, -1); showInfo("Module terminé", "\"" + m.titre + "\" marqué comme terminé !"); loadAndDisplay(); });
            btnRow.getChildren().add(done); inner.getChildren().add(btnRow);
        }
        area.getChildren().addAll(sep, inner);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READING CONTENT — texte formaté en HTML avec titres/listes/code
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildReadingContent(String text) {
        VBox box = new VBox(0);
        box.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;");

        String html = convertTextToHtml(text);
        int h = Math.min(600, Math.max(180, 80 + text.length() / 4));

        WebView wv = new WebView(); wv.setPrefHeight(h); wv.setContextMenuEnabled(false);
        wv.getEngine().loadContent(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                        "*{margin:0;padding:0;box-sizing:border-box;}" +
                        "body{font-family:'Segoe UI',Arial,sans-serif;font-size:14px;color:#1F2937;padding:24px 28px;line-height:1.85;background:white;}" +
                        "h1{font-size:22px;font-weight:700;color:#111827;margin:0 0 18px;padding-bottom:10px;border-bottom:2px solid #E5E7EB;}" +
                        "h2{font-size:18px;font-weight:600;color:#1F2937;margin:24px 0 12px;padding-left:12px;border-left:4px solid #4F46E5;}" +
                        "h3{font-size:15px;font-weight:600;color:#374151;margin:18px 0 8px;}" +
                        "p{margin:0 0 14px;color:#374151;}" +
                        "ul,ol{margin:8px 0 14px 24px;}" +
                        "li{margin:5px 0;color:#374151;}" +
                        "code{background:#F3F4F6;color:#DC2626;padding:2px 7px;border-radius:5px;font-family:'Consolas','Courier New',monospace;font-size:13px;}" +
                        "pre{background:#1E293B;color:#E2E8F0;padding:16px 20px;border-radius:10px;overflow-x:auto;font-size:13px;margin:14px 0;line-height:1.6;}" +
                        "pre code{background:none;color:inherit;padding:0;}" +
                        "blockquote{background:#EEF2FF;border-left:4px solid #4F46E5;padding:12px 16px;margin:14px 0;border-radius:0 8px 8px 0;color:#3730A3;}" +
                        "hr{border:none;border-top:1px solid #E5E7EB;margin:20px 0;}" +
                        "strong{font-weight:700;color:#111827;}em{font-style:italic;color:#6B7280;}" +
                        "</style></head><body>" + html + "</body></html>", "text/html");
        box.getChildren().add(wv);
        return box;
    }

    private String convertTextToHtml(String text) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n");
        boolean inCode = false, inList = false;

        for (String raw : lines) {
            String t = raw.trim();

            if (t.startsWith("```")) {
                if (inList) { sb.append("</ul>"); inList = false; }
                if (!inCode) { sb.append("<pre><code>"); inCode = true; }
                else { sb.append("</code></pre>"); inCode = false; }
                continue;
            }
            if (inCode) { sb.append(escHtml(raw)).append("\n"); continue; }
            if (t.isEmpty()) { if (inList) { sb.append("</ul>"); inList = false; } sb.append("<br/>"); continue; }

            if (t.startsWith("### ")) { if(inList){sb.append("</ul>");inList=false;} sb.append("<h3>").append(fmt(t.substring(4))).append("</h3>"); continue; }
            if (t.startsWith("## "))  { if(inList){sb.append("</ul>");inList=false;} sb.append("<h2>").append(fmt(t.substring(3))).append("</h2>"); continue; }
            if (t.startsWith("# "))   { if(inList){sb.append("</ul>");inList=false;} sb.append("<h1>").append(fmt(t.substring(2))).append("</h1>"); continue; }
            if (t.startsWith("> "))   { if(inList){sb.append("</ul>");inList=false;} sb.append("<blockquote>").append(fmt(t.substring(2))).append("</blockquote>"); continue; }
            if (t.equals("---"))      { if(inList){sb.append("</ul>");inList=false;} sb.append("<hr/>"); continue; }

            if (t.startsWith("- ") || t.startsWith("* ")) {
                if (!inList) { sb.append("<ul>"); inList = true; }
                sb.append("<li>").append(fmt(t.substring(2))).append("</li>"); continue;
            }
            // Ligne courte en MAJUSCULES = titre H2
            if (t.length() < 60 && t.equals(t.toUpperCase()) && t.matches(".*[A-Z].*") && !t.contains(".")) {
                if(inList){sb.append("</ul>");inList=false;}
                sb.append("<h2>").append(escHtml(t)).append("</h2>"); continue;
            }
            if (inList) { sb.append("</ul>"); inList = false; }
            sb.append("<p>").append(fmt(t)).append("</p>");
        }
        if (inList) sb.append("</ul>");
        if (inCode) sb.append("</code></pre>");
        return sb.toString();
    }

    private String fmt(String s) {
        return escHtml(s)
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("\\*(.+?)\\*",        "<em>$1</em>")
                .replaceAll("`(.+?)`",            "<code>$1</code>");
    }
    private String escHtml(String s) { return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }

    // ══════════════════════════════════════════════════════════════════════════
    //  VIDEO PLAYER — YouTube thumbnail via WebView HTML (sans Image/ImageView)
    //  Cliquer sur le thumbnail ouvre YouTube dans le navigateur système.
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildVideoPlayer(String url, boolean isYoutube, boolean isVimeo) {
        VBox box = new VBox(0); box.setStyle("-fx-background-color:#0F0F0F;-fx-background-radius:12;");

        if (isYoutube) {
            String videoId  = extractYouTubeId(url);
            String thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

            WebView thumbWv = new WebView(); thumbWv.setPrefHeight(340);
            thumbWv.setContextMenuEnabled(false);

            // Spoof user-agent pour charger le thumbnail YouTube sans blocage
            thumbWv.getEngine().setUserAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

            thumbWv.getEngine().loadContent(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                            "*{margin:0;padding:0;box-sizing:border-box;}" +
                            "html,body{width:100%;height:340px;background:#111;overflow:hidden;}" +
                            ".wrap{position:relative;width:100%;height:340px;cursor:pointer;}" +
                            ".wrap img{width:100%;height:340px;object-fit:cover;display:block;}" +
                            ".overlay{position:absolute;inset:0;background:rgba(0,0,0,0.3);" +
                            "  display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;" +
                            "  transition:background .2s;}" +
                            ".overlay:hover{background:rgba(0,0,0,0.1);}" +
                            ".play{width:72px;height:72px;background:rgba(255,0,0,0.9);border-radius:50%;" +
                            "  display:flex;align-items:center;justify-content:center;" +
                            "  box-shadow:0 4px 20px rgba(255,0,0,0.5);}" +
                            ".play svg{fill:white;width:28px;height:28px;margin-left:4px;}" +
                            ".lbl{color:white;font-family:Segoe UI,Arial,sans-serif;font-size:14px;" +
                            "  font-weight:600;text-shadow:0 1px 6px rgba(0,0,0,0.9);}" +
                            "</style></head><body>" +
                            "<div class='wrap' onclick='void(0)'>" +
                            "<img src='" + thumbUrl + "' onerror=\"this.style.background='#222';this.style.display='block';\">" +
                            "<div class='overlay'>" +
                            "<div class='play'><svg viewBox='0 0 24 24'><path d='M8 5v14l11-7z'/></svg></div>" +
                            "<div class='lbl'>▶  Lancer la vidéo</div>" +
                            "</div></div></body></html>", "text/html");

            // Clic sur la WebView → ouvre YouTube dans Chrome en mode app (fenêtre dédiée)
            thumbWv.setOnMouseClicked(e -> openInChromeApp(url));
            // Backup : si la WebView tente de naviguer
            thumbWv.getEngine().locationProperty().addListener((obs, oldLoc, newLoc) -> {
                if (newLoc != null && !newLoc.isEmpty() && !newLoc.startsWith("about:")) {
                    Platform.runLater(() -> {
                        thumbWv.getEngine().loadContent("");
                        openInChromeApp(url);
                    });
                }
            });

            box.getChildren().add(thumbWv);

            // Barre basse avec boutons
            HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
            bar.setPadding(new Insets(10, 16, 10, 16));
            bar.setStyle("-fx-background-color:#1A1A1A;");
            Label urlLbl = new Label("🔗  " + url);
            urlLbl.setStyle("-fx-font-size:10;-fx-text-fill:#9CA3AF;");
            HBox.setHgrow(urlLbl, Priority.ALWAYS);

            Button btnApp = new Button("▶  Regarder ici");
            btnApp.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-font-size:11;-fx-padding:6 16;-fx-background-radius:6;-fx-cursor:hand;");
            btnApp.setOnAction(e -> openInChromeApp(url));

            Button btnYt = new Button("↗  YouTube");
            btnYt.setStyle("-fx-background-color:#FF0000;-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-font-size:11;-fx-padding:6 14;-fx-background-radius:6;-fx-cursor:hand;");
            btnYt.setOnAction(e -> openInBrowser(url));

            bar.getChildren().addAll(urlLbl, btnApp, btnYt);
            box.getChildren().add(bar);

        } else if (isVimeo) {
            String videoId = url.replaceAll(".*vimeo\\.com/(\\d+).*", "$1");
            WebView wv = new WebView(); wv.setPrefHeight(340);
            wv.setContextMenuEnabled(false);
            wv.setOnMouseClicked(e -> openInBrowser(url));
            wv.getEngine().loadContent(
                    "<html><head><style>body{margin:0;background:#111;cursor:pointer;height:340px;display:flex;align-items:center;justify-content:center;}" +
                            ".c{text-align:center;color:white;font-family:Arial;}" +
                            ".p{font-size:48px;background:rgba(0,150,255,0.88);border-radius:50%;width:80px;height:80px;line-height:80px;margin:0 auto 10px;}" +
                            "</style></head><body onclick=\"location.href='" + url + "'\">" +
                            "<div class='c'><div class='p'>&#9654;</div><div>Regarder sur Vimeo</div></div></body></html>", "text/html");
            box.getChildren().add(wv);

        } else {
            // Fichier MP4 local
            WebView wv = new WebView(); wv.setPrefHeight(360);
            String safe = url.startsWith("file://") ? url : "file:///" + url.replace("\\", "/");
            wv.getEngine().loadContent(
                    "<html><body style='margin:0;background:#000;'><video width='100%' height='360' controls style='display:block;'>" +
                            "<source src='" + safe + "'>Non supporté.</video></body></html>");
            box.getChildren().add(wv);
            Label info = new Label("📂  " + url); info.setStyle("-fx-font-size:10;-fx-text-fill:#6B7280;-fx-padding:6 12;-fx-background-color:#1A1A1A;");
            box.getChildren().add(info);
        }
        return box;
    }

    private String extractYouTubeId(String url) {
        if (url.contains("youtu.be/")) return url.replaceAll(".*youtu\\.be/([^?&]+).*", "$1");
        if (url.contains("v="))        return url.replaceAll(".*[?&]v=([^&]+).*", "$1");
        if (url.contains("embed/"))    return url.replaceAll(".*embed/([^?&]+).*", "$1");
        return url;
    }

    /**
     * Ouvre l'URL dans Chrome en mode "application" (fenêtre sans barre d'adresse).
     * C'est la seule solution fiable pour YouTube dans JavaFX (contourne Error 153).
     * Essaie Chrome, puis Edge, puis le navigateur système par défaut.
     */
    private void openInChromeApp(String url) {
        // Chemins Chrome Windows courants
        String[] chromePaths = {
                System.getenv("ProgramFiles")        + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("ProgramFiles(x86)")   + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("LOCALAPPDATA")        + "\\Google\\Chrome\\Application\\chrome.exe",
        };
        // Chemins Edge (fallback)
        String[] edgePaths = {
                System.getenv("ProgramFiles")        + "\\Microsoft\\Edge\\Application\\msedge.exe",
                System.getenv("ProgramFiles(x86)")   + "\\Microsoft\\Edge\\Application\\msedge.exe",
        };

        // --app= ouvre dans une fenêtre sans chrome (barre d'adresse, onglets)
        // --new-window force une nouvelle fenêtre
        for (String path : chromePaths) {
            if (path != null && new java.io.File(path).exists()) {
                try {
                    new ProcessBuilder(path, "--app=" + url, "--new-window",
                            "--disable-extensions", "--disable-translate")
                            .start();
                    System.out.println("✓ Chrome app launched: " + url);
                    return;
                } catch (Exception ex) { System.err.println("Chrome launch error: " + ex.getMessage()); }
            }
        }
        for (String path : edgePaths) {
            if (path != null && new java.io.File(path).exists()) {
                try {
                    new ProcessBuilder(path, "--app=" + url, "--new-window").start();
                    System.out.println("✓ Edge app launched: " + url);
                    return;
                } catch (Exception ex) { System.err.println("Edge launch error: " + ex.getMessage()); }
            }
        }
        // Dernier recours : navigateur système
        openInBrowser(url);
    }

    private void openInBrowser(String url) {
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); }
        catch (Exception ex) { showError("Ouverture", ex.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXERCISE + CORRECTION IA
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildExerciseSubmission(String instructions, int moduleId, int sousSectionId) {
        VBox box = new VBox(12);
        boolean apiOk = (grokApiKey != null && !grokApiKey.isBlank()) || (cohereApiKey != null && !cohereApiKey.isBlank());

        VBox instrBox = new VBox(8); instrBox.setPadding(new Insets(14, 16, 14, 16));
        instrBox.setStyle("-fx-background-color:#FFFBEB;-fx-background-radius:10;-fx-border-color:#FDE68A;-fx-border-width:1;-fx-border-radius:10;");
        Label instrTitle = new Label("📋  Instructions"); instrTitle.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#92400E;");
        Label instrText = new Label(instructions); instrText.setStyle("-fx-font-size:12;-fx-text-fill:#78350F;"); instrText.setWrapText(true);
        instrBox.getChildren().addAll(instrTitle, instrText);

        if (!apiOk) {
            HBox warn = new HBox(10); warn.setAlignment(Pos.CENTER_LEFT); warn.setPadding(new Insets(10, 14, 10, 14));
            warn.setStyle("-fx-background-color:#FFF7ED;-fx-background-radius:8;-fx-border-color:#FDE68A;-fx-border-width:1;-fx-border-radius:8;");
            Label wTxt = new Label("⚙️  Correction IA non configurée. Ajoutez groq.key=gsk_... dans anthropic.properties (clé gratuite sur console.groq.com)");
            wTxt.setStyle("-fx-font-size:11;-fx-text-fill:#92400E;"); wTxt.setWrapText(true); HBox.setHgrow(wTxt, Priority.ALWAYS);
            warn.getChildren().add(wTxt); box.getChildren().add(warn);
        }

        Label submitTitle = new Label("✍️  Votre réponse / Rendu"); submitTitle.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        TextArea ta = new TextArea();
        ta.setPromptText("Écrivez votre code ou votre réponse ici..."); ta.setPrefRowCount(10); ta.setWrapText(true);
        ta.setStyle("-fx-font-family:'Consolas','Courier New',monospace;-fx-font-size:12;-fx-background-color:white;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1;");

        ExerciceResult existing = loadExistingSubmission(moduleId, sousSectionId);
        VBox resultBox = new VBox(0);

        HBox actionRow = new HBox(10); actionRow.setAlignment(Pos.CENTER_LEFT);
        Label statusLbl = new Label(""); statusLbl.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;"); HBox.setHgrow(statusLbl, Priority.ALWAYS);
        Button btnSubmit  = actionBtn(apiOk ? "🤖  Soumettre pour correction IA" : "📤  Soumettre", apiOk ? "#7C3AED" : "#4F46E5", "white", null, false);
        Button btnHistory = actionBtn("📜  Voir historique", "white", "#4F46E5", "#C7D2FE", true);

        if (existing != null) {
            ta.setText(existing.contenu);
            statusLbl.setText("Dernière soumission" + (existing.noteIa > 0 ? " · Note : " + existing.noteIa + "/100" : ""));
            showExerciceResult(resultBox, existing);
        }

        btnSubmit.setOnAction(e -> {
            String response = ta.getText().trim();
            if (response.isEmpty()) { showError("Rendu vide", "Veuillez écrire votre réponse."); return; }
            btnSubmit.setText("⏳  En cours..."); btnSubmit.setDisable(true);
            new Thread(() -> {
                int sid  = saveSubmission(moduleId, sousSectionId, response);
                String ai = apiOk ? callAnthropicForCorrection(instructions, response) : null;
                int note  = (apiOk && ai != null) ? extractNote(ai) : -1;
                updateSubmissionWithAI(sid, note, ai);
                Platform.runLater(() -> {
                    ExerciceResult r = new ExerciceResult();
                    r.contenu = response; r.feedbackIa = ai; r.noteIa = note;
                    r.dateSoumission = LocalDateTime.now().toString().replace("T"," ").substring(0,16);
                    showExerciceResult(resultBox, r);
                    btnSubmit.setText(apiOk ? "🤖  Soumettre pour correction IA" : "📤  Soumettre");
                    btnSubmit.setDisable(false);
                    statusLbl.setText(note >= 0 ? "Correction reçue · Note : " + note + "/100" : "Réponse soumise.");
                });
            }).start();
        });
        btnHistory.setOnAction(e -> showSubmissionHistory(moduleId, sousSectionId));
        actionRow.getChildren().addAll(statusLbl, btnHistory, btnSubmit);
        box.getChildren().addAll(instrBox, submitTitle, ta, actionRow, resultBox);
        return box;
    }

    private void showExerciceResult(VBox container, ExerciceResult result) {
        container.getChildren().clear();
        if (result == null) return;
        if (result.noteIa < 0 || result.feedbackIa == null) {
            HBox ok = new HBox(10); ok.setAlignment(Pos.CENTER_LEFT); ok.setPadding(new Insets(12,16,12,16));
            ok.setStyle("-fx-background-color:#F0FDF4;-fx-background-radius:10;-fx-border-color:#86EFAC;-fx-border-width:1;-fx-border-radius:10;");
            Label ic = new Label("✓"); ic.setStyle("-fx-font-size:18;-fx-text-fill:#059669;");
            Label msg = new Label("Réponse soumise avec succès."); msg.setStyle("-fx-font-size:12;-fx-text-fill:#166534;");
            ok.getChildren().addAll(ic, msg); container.getChildren().add(ok); return;
        }
        boolean passed = result.noteIa >= 70;
        VBox card = new VBox(10); card.setPadding(new Insets(16,18,16,18));
        card.setStyle("-fx-background-color:"+(passed?"#F0FDF4":"#FFF7ED")+";-fx-background-radius:10;-fx-border-color:"+(passed?"#86EFAC":"#FDE68A")+";-fx-border-width:1;-fx-border-radius:10;");

        HBox scoreRow = new HBox(14); scoreRow.setAlignment(Pos.CENTER_LEFT);
        Label scoreIc = new Label(passed ? "🎉" : "📚"); scoreIc.setStyle("-fx-font-size:28;");
        VBox scoreInfo = new VBox(4); HBox.setHgrow(scoreInfo, Priority.ALWAYS);
        Label scoreT = new Label(passed ? "Exercice réussi !" : "Peut mieux faire");
        scoreT.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:"+(passed?"#166534":"#92400E")+";");
        Label scoreSub = new Label("Score IA : " + result.noteIa + "/100  ·  Min requis : 70/100");
        scoreSub.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        StackPane barStack = new StackPane(); barStack.setAlignment(Pos.CENTER_LEFT);
        Region track = new Region(); track.setPrefHeight(8); track.setMaxWidth(Double.MAX_VALUE); track.setStyle("-fx-background-color:#E5E7EB;-fx-background-radius:4;");
        Region fill = new Region(); fill.setPrefHeight(8); fill.setStyle("-fx-background-color:"+(passed?"#10B981":"#F59E0B")+";-fx-background-radius:4;");
        track.widthProperty().addListener((obs,o,n) -> fill.setPrefWidth(n.doubleValue() * result.noteIa / 100.0));
        barStack.getChildren().addAll(track, fill);
        scoreInfo.getChildren().addAll(scoreT, scoreSub, barStack);
        scoreRow.getChildren().addAll(scoreIc, scoreInfo);

        Label feedT = new Label("💬  Feedback de l'IA"); feedT.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#374151;");
        TextArea feedArea = new TextArea(result.feedbackIa != null ? result.feedbackIa : "");
        feedArea.setEditable(false); feedArea.setWrapText(true); feedArea.setPrefRowCount(8);
        feedArea.setStyle("-fx-font-size:12;-fx-background-color:white;-fx-border-color:#E5E7EB;-fx-border-radius:8;-fx-background-radius:8;");
        card.getChildren().addAll(scoreRow, feedT, feedArea);
        container.getChildren().add(card);
    }

    private String callAnthropicForCorrection(String instructions, String rendu) {
        // Essayer Groq d'abord, puis Grok en fallback, puis Cohere, puis local
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            return callGrokForCorrection(instructions, rendu);
        }
        if (grokApiKey != null && !grokApiKey.isBlank()) {
            return callGrokForCorrection(instructions, rendu);
        }
        if (cohereApiKey != null && !cohereApiKey.isBlank()) {
            return callCohereForCorrection(instructions, rendu);
        }
        return callLocalCorrection(instructions, rendu);
    }

    /** Correction via Grok API */
    private String callGrokForCorrection(String instructions, String rendu) {
        String prompt = "Tu es un formateur expert. Évalue cet exercice soumis par un apprenant.\n\n" +
                "INSTRUCTIONS DE L'EXERCICE :\n" + instructions + "\n\n" +
                "RÉPONSE DE L'APPRENANT :\n" + rendu + "\n\n" +
                "═══ CONSIGNES ═══\n" +
                "1. Lis les instructions et analyse si la réponse y répond SPÉCIFIQUEMENT\n" +
                "2. Évalue la qualité, la pertinence et la complétude de la réponse\n" +
                "3. Donne une note honnête sur 100 (seuil de réussite = 70)\n\n" +
                "RÉPONDS EXACTEMENT dans ce format (en français) :\n\n" +
                "NOTE: [nombre entre 0 et 100]\n\n" +
                "POINTS POSITIFS:\n- [point positif]\n- [autre point]\n\n" +
                "POINTS À AMÉLIORER:\n- [point à améliorer]\n- [suggestion]\n\n" +
                "CONCLUSION:\n[2-3 phrases sur la qualité par rapport à l'exercice]";

        String result = callGeminiAPI(prompt);
        if (result != null && !result.isBlank()) return result;
        return callLocalCorrection(instructions, rendu);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════════
    //  COHERE API — fallback si Grok non configuré
    // ═══════════════════════════════════════════════════════════════════════════
    private String callCohereForCorrection(String instructions, String rendu) {
        if (cohereApiKey == null) return callLocalCorrection(instructions, rendu);
        try {
            String prompt =
                    "Tu es un formateur expert et bienveillant. Évalue la réponse d'un apprenant.\n\n" +
                            "═══ INSTRUCTIONS DE L'EXERCICE ═══\n" + instructions + "\n\n" +
                            "═══ RÉPONSE DE L'APPRENANT ═══\n" + rendu + "\n\n" +
                            "═══ CONSIGNES ═══\n" +
                            "1. Lis les instructions et analyse si la réponse y répond SPÉCIFIQUEMENT\n" +
                            "2. Évalue la qualité, la pertinence et la complétude de la réponse\n" +
                            "3. Donne une note honnête sur 100 (seuil de réussite = 70)\n\n" +
                            "RÉPONDS EXACTEMENT dans ce format (en français) :\n\n" +
                            "NOTE: [nombre entre 0 et 100]\n\n" +
                            "POINTS POSITIFS:\n- [point positif spécifique]\n- [autre point]\n\n" +
                            "POINTS À AMÉLIORER:\n- [point à améliorer spécifique]\n- [suggestion]\n\n" +
                            "CONCLUSION:\n[2-3 phrases sur la qualité par rapport à l'exercice]";

            // Cohere API v2 — endpoint /v2/chat
            String body = "{\"model\":\"command-r\"," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" + escapeJson(prompt) + "}]," +
                    "\"max_tokens\":1000,\"temperature\":0.2}";

            HttpURLConnection conn = (HttpURLConnection)
                    new URL("https://api.cohere.com/v2/chat").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + cohereApiKey);
            conn.setRequestProperty("X-Client-Name", "humania-rh");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println(">>> Cohere → HTTP " + code);

            if (code != 200) {
                InputStream err = conn.getErrorStream();
                String errBody = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
                System.out.println(">>> Cohere erreur " + code + " : " + errBody);
                if (code == 401) return "⚠️ Clé Cohere invalide.\nAjoutez cohere.key=co-... dans anthropic.properties\n(Clé gratuite sur dashboard.cohere.com/api-keys)";
                if (code == 429) return "⚠️ Quota Cohere momentanément dépassé.\nAttendez 1 minute et réessayez.";
                return callLocalCorrection(instructions, rendu);
            }

            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(">>> Cohere correction reçue ✅");
            System.out.println(">>> Réponse JSON : " + json.substring(0, Math.min(200, json.length())));

            // Parser Cohere v2 : {"message":{"content":[{"type":"text","text":"..."}]}}
            int textIdx = json.indexOf("\"text\":\"");
            if (textIdx < 0) {
                System.out.println(">>> Cohere : champ 'text' non trouvé dans : " + json.substring(0, Math.min(300, json.length())));
                return callLocalCorrection(instructions, rendu);
            }

            StringBuilder res = new StringBuilder();
            int i = textIdx + 8;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) break;
                if (c == '\\' && i + 1 < json.length()) {
                    char nx = json.charAt(i + 1);
                    if (nx == 'n')  { res.append('\n'); i += 2; continue; }
                    if (nx == '"')  { res.append('"');  i += 2; continue; }
                    if (nx == '\\') { res.append('\\'); i += 2; continue; }
                    if (nx == 't')  { res.append('\t'); i += 2; continue; }
                    if (nx == 'r')  { i += 2; continue; }
                }
                res.append(c); i++;
            }
            String result = res.toString().trim();
            return result.isEmpty() ? callLocalCorrection(instructions, rendu) : result;

        } catch (Exception e) {
            System.out.println(">>> Cohere exception : " + e.getMessage());
            return callLocalCorrection(instructions, rendu);
        }
    }

    /** Correction hors-ligne — si aucune clé API disponible */
    private String callLocalCorrection(String instructions, String rendu) {
        int len = rendu == null ? 0 : rendu.trim().length();
        int note; String points, ameliorer, conclusion;
        if (len < 20) {
            note = 10;
            points    = "- Une tentative de réponse a été faite";
            ameliorer = "- La réponse est trop courte\n- Développez votre réponse en suivant les instructions";
            conclusion = "Réponse insuffisante. Les instructions demandent plus de développement.";
        } else if (len < 100) {
            note = 35;
            points    = "- Des éléments de réponse sont présents";
            ameliorer = "- La réponse est trop succincte\n- Couvrez tous les points des instructions";
            conclusion = "Réponse partielle. Ajoutez une clé Cohere pour une correction IA précise.";
        } else if (len < 300) {
            note = 60;
            points    = "- La réponse aborde les points principaux\n- L'effort fourni est visible";
            ameliorer = "- Certains aspects méritent plus de développement\n- Vérifiez que tous les critères sont traités";
            conclusion = "Réponse correcte mais incomplète. Ajoutez une clé Cohere pour une correction précise.";
        } else {
            note = 78;
            points    = "- Réponse complète et bien développée\n- Les instructions semblent bien suivies";
            ameliorer = "- Relisez les instructions pour ne rien oublier";
            conclusion = "Bonne réponse. Ajoutez une clé Cohere pour une correction IA précise.";
        }
        return "NOTE: " + note + "/100\n\n" +
                "⚠️ [Correction automatique — ajoutez cohere.key=co-... dans anthropic.properties pour la correction IA]\n\n" +
                "POINTS POSITIFS:\n" + points + "\n\n" +
                "POINTS À AMÉLIORER:\n" + ameliorer + "\n\n" +
                "CONCLUSION:\n" + conclusion;
    }


    private int extractNote(String feedback) {
        if (feedback == null) return -1;
        try {
            int idx = feedback.indexOf("NOTE:");
            if (idx >= 0) {
                String rest = feedback.substring(idx+5).trim();
                String num = rest.split("[\n\r /]")[0].trim().replaceAll("[^0-9]","");
                if (!num.isEmpty()) return Math.min(100, Math.max(0, Integer.parseInt(num)));
            }
        } catch (Exception ignored) {}
        return 50;
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t") + "\"";
    }

    private int saveSubmission(int moduleId, int sousSectionId, String contenu) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO exercice_soumission (employe_id, module_id, sous_section_id, contenu_rendu, statut) VALUES (?,?,?,?,'soumis')", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, employeId); ps.setInt(2, moduleId);
            if (sousSectionId > 0) ps.setInt(3, sousSectionId); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, contenu); ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys(); if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { showError("Sauvegarde", e.getMessage()); }
        return -1;
    }

    private void updateSubmissionWithAI(int sid, int note, String feedback) {
        if (sid <= 0) return;
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE exercice_soumission SET note_ia=?, feedback_ia=?, statut=?, date_correction=NOW() WHERE id=?");
            if (note >= 0) ps.setInt(1, note); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, feedback); ps.setString(3, feedback != null && note >= 0 ? "corrige" : "soumis"); ps.setInt(4, sid); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private ExerciceResult loadExistingSubmission(int moduleId, int sousSectionId) {
        try {
            String sql = "SELECT contenu_rendu, note_ia, feedback_ia, date_soumission FROM exercice_soumission WHERE employe_id=? AND module_id=?" +
                    (sousSectionId > 0 ? " AND sous_section_id=?" : "") + " ORDER BY date_soumission DESC LIMIT 1";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, employeId); ps.setInt(2, moduleId); if (sousSectionId > 0) ps.setInt(3, sousSectionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { ExerciceResult r = new ExerciceResult(); r.contenu=rs.getString("contenu_rendu"); r.noteIa=rs.getInt("note_ia"); r.feedbackIa=rs.getString("feedback_ia"); r.dateSoumission=rs.getString("date_soumission"); return r; }
        } catch (SQLException ignored) {}
        return null;
    }

    private void showSubmissionHistory(int moduleId, int sousSectionId) {
        List<ExerciceResult> history = new ArrayList<>();
        try {
            String sql = "SELECT contenu_rendu, note_ia, feedback_ia, date_soumission FROM exercice_soumission WHERE employe_id=? AND module_id=?" +
                    (sousSectionId > 0 ? " AND sous_section_id=?" : "") + " ORDER BY date_soumission DESC LIMIT 10";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, employeId); ps.setInt(2, moduleId); if (sousSectionId > 0) ps.setInt(3, sousSectionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) { ExerciceResult r = new ExerciceResult(); r.contenu=rs.getString("contenu_rendu"); r.noteIa=rs.getInt("note_ia"); r.feedbackIa=rs.getString("feedback_ia"); r.dateSoumission=rs.getString("date_soumission"); history.add(r); }
        } catch (SQLException ignored) {}
        if (history.isEmpty()) { showInfo("Historique", "Aucune soumission précédente."); return; }
        Dialog<Void> dlg = new Dialog<>(); dlg.setTitle("Historique"); dlg.getDialogPane().setPrefWidth(640); dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox content = new VBox(12); content.setPadding(new Insets(20));
        for (int i = 0; i < history.size(); i++) {
            ExerciceResult r = history.get(i); boolean passed = r.noteIa >= 70;
            VBox c = new VBox(6); c.setPadding(new Insets(12,14,12,14));
            c.setStyle("-fx-background-color:"+(passed?"#F0FDF4":"#FFF7ED")+";-fx-background-radius:8;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;");
            Label hdrLbl = new Label("#"+(i+1)+"  ·  "+r.dateSoumission+(r.noteIa>=0?"  ·  Note : "+r.noteIa+"/100":"  ·  Non corrigé"));
            hdrLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:"+(passed?"#166534":"#92400E")+";");
            c.getChildren().add(hdrLbl);
            content.getChildren().add(c);
        }
        ScrollPane histSp = new ScrollPane(content); histSp.setFitToWidth(true);
        dlg.getDialogPane().setContent(histSp); dlg.showAndWait();
    }

    // ─── PDF + URL card ───────────────────────────────────────────────────────
    private VBox buildPdfViewer(String url) {
        VBox box = new VBox(8);
        HBox toolbar = new HBox(12); toolbar.setAlignment(Pos.CENTER_LEFT); toolbar.setPadding(new Insets(10,14,10,14));
        toolbar.setStyle("-fx-background-color:#F3F4F6;-fx-background-radius:10 10 0 0;");
        Label pdfName = new Label("📄  " + url.substring(url.lastIndexOf('/')+1)); pdfName.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#374151;"); HBox.setHgrow(pdfName, Priority.ALWAYS);
        Button btnOpen = actionBtn("⬡  Ouvrir", "#4F46E5", "white", null, false);
        btnOpen.setOnAction(e -> openInBrowser(url.startsWith("http") ? url : "file:///"+url.replace("\\","/")));
        toolbar.getChildren().addAll(pdfName, btnOpen);
        WebView wv = new WebView(); wv.setPrefHeight(480);
        wv.getEngine().load(url.startsWith("http") ? url : "file:///"+url.replace("\\","/"));
        VBox frame = new VBox(0); frame.setStyle("-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;");
        frame.getChildren().addAll(toolbar, wv); box.getChildren().add(frame); return box;
    }

    private HBox buildUrlCard(String url) {
        HBox card = new HBox(12); card.setAlignment(Pos.CENTER_LEFT); card.setPadding(new Insets(12,16,12,16));
        card.setStyle("-fx-background-color:#EFF6FF;-fx-background-radius:10;-fx-border-color:#BFDBFE;-fx-border-width:1;-fx-border-radius:10;");
        Label ic = new Label("🔗"); ic.setStyle("-fx-font-size:16;");
        VBox info = new VBox(2); HBox.setHgrow(info, Priority.ALWAYS);
        Label t = new Label("Ressource en ligne"); t.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1E40AF;");
        Label u = new Label(url); u.setStyle("-fx-font-size:11;-fx-text-fill:#60A5FA;"); u.setWrapText(true);
        info.getChildren().addAll(t, u);
        Button open = actionBtn("Ouvrir", "#2563EB", "white", null, false); open.setOnAction(e -> openInBrowser(url));
        card.getChildren().addAll(ic, info, open); return card;
    }

    // ─── Quiz ─────────────────────────────────────────────────────────────────
    private void openQuiz(ModuleItem m) {
        List<QuizQuestion> questions = loadQuizQuestions(m.id);
        if (questions.isEmpty()) { showInfo("Quiz vide", "📭  Aucune question disponible."); return; }
        Dialog<Integer> dialog = new Dialog<>(); dialog.setTitle("Quiz — " + m.titre);
        dialog.getDialogPane().setPrefWidth(600); dialog.getDialogPane().setPrefHeight(600);
        dialog.getDialogPane().setStyle("-fx-background-color:white;");
        ButtonType submit = new ButtonType("✓  Soumettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);
        VBox content = new VBox(16); content.setPadding(new Insets(24)); content.setStyle("-fx-background-color:white;");
        Label quizHdr = new Label("📝  " + m.titre); quizHdr.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#111827;");
        content.getChildren().add(quizHdr);
        List<ToggleGroup> groups = new ArrayList<>(); int n = 1;
        for (QuizQuestion q : questions) {
            VBox qBox = new VBox(10); qBox.setPadding(new Insets(16,18,16,18));
            qBox.setStyle("-fx-background-color:#FAFAFA;-fx-background-radius:12;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;");
            Label qLbl = new Label(n + ". " + q.question); qLbl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1F2937;"); qLbl.setWrapText(true);
            qBox.getChildren().add(qLbl); ToggleGroup tg = new ToggleGroup(); groups.add(tg);
            addQuizOption(qBox, tg, "A", q.optionA); addQuizOption(qBox, tg, "B", q.optionB);
            if (q.optionC != null && !q.optionC.isEmpty()) addQuizOption(qBox, tg, "C", q.optionC);
            if (q.optionD != null && !q.optionD.isEmpty()) addQuizOption(qBox, tg, "D", q.optionD);
            content.getChildren().add(qBox); n++;
        }
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setStyle("-fx-background:white;-fx-background-color:white;");
        dialog.getDialogPane().setContent(scroll);
        dialog.setResultConverter(btn -> {
            if (btn != submit) return null;
            int correct = 0;
            for (int i = 0; i < questions.size(); i++) {
                RadioButton sel = (RadioButton) groups.get(i).getSelectedToggle();
                if (sel != null && sel.getUserData().equals(questions.get(i).bonneReponse)) correct++;
            }
            return correct * 100 / questions.size();
        });
        dialog.showAndWait().ifPresent(score -> {
            if (score == null) return; markModuleCompleted(m.id, score);
            Alert r = new Alert(score>=70 ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
            r.setTitle("Résultat"); r.setHeaderText(score>=70 ? "🎉  Réussi !" : "📚  Score insuffisant");
            r.setContentText("Score : " + score + "%\n" + (score>=70 ? "Module validé !" : "Score requis : 70%.")); r.showAndWait(); loadAndDisplay();
        });
    }

    private void addQuizOption(VBox parent, ToggleGroup tg, String letter, String text) {
        RadioButton rb = new RadioButton(text); rb.setToggleGroup(tg); rb.setUserData(letter); rb.setStyle("-fx-font-size:12;-fx-text-fill:#374151;");
        HBox opt = new HBox(12); opt.setAlignment(Pos.CENTER_LEFT); opt.setPadding(new Insets(10,14,10,14));
        opt.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
        Label ltr = new Label(letter); ltr.setPrefSize(26,26); ltr.setAlignment(Pos.CENTER);
        ltr.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#6B7280;-fx-background-color:#F3F4F6;-fx-background-radius:50;");
        opt.getChildren().addAll(ltr, rb);
        rb.selectedProperty().addListener((obs,o,sel) -> {
            if (sel) { opt.setStyle("-fx-background-color:#EDE9FE;-fx-background-radius:8;-fx-border-color:#7C3AED;-fx-border-width:1.5;-fx-border-radius:8;"); ltr.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:white;-fx-background-color:#7C3AED;-fx-background-radius:50;"); }
            else { opt.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;"); ltr.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#6B7280;-fx-background-color:#F3F4F6;-fx-background-radius:50;"); }
        });
        parent.getChildren().add(opt);
    }

    private List<QuizQuestion> loadQuizQuestions(int moduleId) {
        List<QuizQuestion> list = new ArrayList<>();
        try { PreparedStatement ps = connection.prepareStatement("SELECT * FROM quiz_question WHERE module_id=? ORDER BY id"); ps.setInt(1,moduleId); ResultSet rs = ps.executeQuery(); while (rs.next()) list.add(new QuizQuestion(rs)); }
        catch (SQLException e) { showError("Quiz", e.getMessage()); }
        return list;
    }

    private void markModuleCompleted(int moduleId, int score) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO module_progression (employe_id,module_id,statut,score_quiz,date_completion) " +
                            "VALUES (?,?,'completed',?,?) " +
                            "ON DUPLICATE KEY UPDATE statut='completed',score_quiz=?,date_completion=?");
            ps.setInt(1,employeId); ps.setInt(2,moduleId);
            ps.setObject(3, score<0 ? null : score);
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            ps.setObject(5, score<0 ? null : score);
            ps.setDate(6, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();

            // ── Vérifier si la formation est maintenant à 100% ──────────────
            checkAndSyncPDI();

        } catch (SQLException e) { showError("Progression", e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ModuleLearner
    // ══════════════════════════════════════════════════════════════════════════

    private void openModuleLearner(ModuleItem module) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/competence/ModuleLearner.fxml"));
            Parent root = loader.load();

            ModuleLearnerController controller = loader.getController();

            // Construire la liste ordonnée de tous les modules
            List<ModuleItem> allMods = loadModules();
            java.util.List<int[]> allModuleIds = new java.util.ArrayList<>();
            int currentIdx = 0;
            for (int i = 0; i < allMods.size(); i++) {
                allModuleIds.add(new int[]{allMods.get(i).id});
                if (allMods.get(i).id == module.id) currentIdx = i;
            }

            test.MainFX mainFX = test.MainFX.getInstance();

            // Récupérer le Stage depuis mainStage (déjà stocké) ou vboxModules si disponible
            Stage stage = mainStage;
            if (stage == null && vboxModules != null && vboxModules.getScene() != null) {
                stage = (Stage) vboxModules.getScene().getWindow();
            }
            final Stage finalStage = stage;

            // Callback "Retour" : réafficher la vue Modules + sidebar
            controller.setOnCloseRefresh(() -> Platform.runLater(() -> {
                if (mainFX != null) mainFX.showSidebar();

                if (mainFX != null) {
                    try {
                        FXMLLoader mvLoader = new FXMLLoader(
                                getClass().getResource("/views/competence/ModulesView.fxml"));
                        Parent mvRoot = mvLoader.load();
                        ModulesViewController mvc = mvLoader.getController();
                        mvc.setEmployeId(employeId);   // ← CORRECTION : transmettre l'employeId
                        mvc.setFormation(formationId, inscriptionId, formationTitre);
                        mvc.setMainStage(finalStage);
                        mvc.setViewRoot(mvRoot);
                        mainFX.setCenter(mvRoot);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        mainFX.navigateTo(test.MainFX.MY_LEARNING);
                    }
                }
            }));

            controller.setStage(finalStage);

            controller.loadModule(
                    module.id, inscriptionId, employeId,
                    module.titre, module.typeContenu,
                    module.videoUrl, module.contenuTexte,
                    allModuleIds, currentIdx);

            // Cacher la sidebar AVANT d'afficher ModuleLearner
            if (mainFX != null) {
                mainFX.hideSidebar();
                mainFX.setCenter(root);
            } else if (finalStage != null) {
                finalStage.getScene().setRoot(root);
            }

            System.out.println("✓ ModuleLearner (plein écran): " + module.titre);

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    /**
     * Appelée après chaque module terminé.
     * Si tous les modules sont complétés → marque l'action PDI comme "Completed"
     * et met à jour progressionGlobale + statut du PDI.
     */
    private void checkAndSyncPDI() {
        try {
            // 1. Calculer % réel de la formation
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(m.id) AS total, " +
                            "SUM(CASE WHEN mp.statut='completed' THEN 1 ELSE 0 END) AS done " +
                            "FROM module m " +
                            "LEFT JOIN module_progression mp ON mp.module_id=m.id AND mp.employe_id=? " +
                            "WHERE m.formation_id=?");
            ps.setInt(1, employeId); ps.setInt(2, formationId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            int total = rs.getInt("total");
            int done  = rs.getInt("done");
            if (total == 0 || done < total) return; // Pas encore 100%

            // 2. Formation 100% → synchroniser le PDI
            TrainingCatalogController.markFormationCompletedInPDI(connection, employeId, formationTitre);

            // 3. Recalculer la progression du PDI concerné
            recalcPDIForFormation();

        } catch (SQLException e) {
            System.err.println("[ModulesVC] checkAndSyncPDI error: " + e.getMessage());
        }
    }

    /**
     * Recalcule progressionGlobale et statut du PDI contenant
     * l'action liée à cette formation.
     */
    private void recalcPDIForFormation() {
        try {
            String label = "Formation : " + formationTitre;

            // Trouver le pdi_id contenant cette action
            PreparedStatement findPdi;
            try {
                findPdi = connection.prepareStatement(
                        "SELECT a.pdi_id FROM actionPDI a " +
                                "JOIN pdi p ON p.id = a.pdi_id " +
                                "WHERE a.typeAction = ? AND (p.employe_id = ? OR p.employe_id IS NULL) LIMIT 1");
                findPdi.setString(1, label);
                findPdi.setInt(2, employeId);
            } catch (SQLException ex) {
                findPdi = connection.prepareStatement(
                        "SELECT pdi_id FROM actionPDI WHERE typeAction = ? LIMIT 1");
                findPdi.setString(1, label);
            }

            ResultSet rs = findPdi.executeQuery();
            if (!rs.next()) return;
            int pdiId = rs.getInt("pdi_id");

            // Recalculer % PDI basé sur toutes ses actions
            PreparedStatement calc = connection.prepareStatement(
                    "SELECT COUNT(*) AS total, " +
                            "SUM(CASE WHEN statut='Completed' THEN 1 ELSE 0 END) AS done " +
                            "FROM actionPDI WHERE pdi_id=?");
            calc.setInt(1, pdiId);
            ResultSet r2 = calc.executeQuery();
            if (!r2.next()) return;

            int total = r2.getInt("total");
            int pct   = total == 0 ? 0 : r2.getInt("done") * 100 / total;
            String newStatut = pct >= 100 ? "Completed" : "Active";

            PreparedStatement upd = connection.prepareStatement(
                    "UPDATE pdi SET progressionGlobale=?, statut=? WHERE id=?");
            upd.setInt(1, pct); upd.setString(2, newStatut); upd.setInt(3, pdiId);
            upd.executeUpdate();

            System.out.println("[ModulesVC] PDI #" + pdiId + " mis à jour : " + pct + "% → " + newStatut);

        } catch (SQLException e) {
            System.err.println("[ModulesVC] recalcPDI error: " + e.getMessage());
        }
    }

    // ─── Helpers UI ───────────────────────────────────────────────────────────
    private boolean isAccessible(ModuleItem m, List<ModuleItem> all) {
        if (m.ordre <= 1) return true;
        return all.stream().filter(x -> x.ordre == m.ordre-1).allMatch(x -> "completed".equals(x.statut));
    }
    private String typeEmoji(String t, boolean done) {
        if (done) return "✅"; if (t==null) return "📄";
        switch (t) {
            case "video":    return "🎬";
            case "reading":  return "📖";
            case "exercise": return "🏋";
            case "quiz":     return "📝";
            default:         return "📄";
        }
    }
    private Label chip(String text, String bg, String fg) { Label l=new Label(text); l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:10;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;"); return l; }
    private Label typeChip(String t) {
        String[] c;
        if (t == null) { c = new String[]{"#F3F4F6","#6B7280"}; }
        else { switch(t) {
            case "video":    c = new String[]{"#DBEAFE","#1D4ED8"}; break;
            case "reading":  c = new String[]{"#D1FAE5","#065F46"}; break;
            case "exercise": c = new String[]{"#FEF3C7","#92400E"}; break;
            case "quiz":     c = new String[]{"#EDE9FE","#5B21B6"}; break;
            default:         c = new String[]{"#F3F4F6","#6B7280"}; break;
        }}
        String label;
        if (t == null) { label = "Contenu"; }
        else { switch(t) {
            case "video":    label = "Vidéo";    break;
            case "reading":  label = "Lecture";  break;
            case "exercise": label = "Exercice"; break;
            case "quiz":     label = "Quiz";     break;
            default:         label = "Contenu";  break;
        }}
        return chip(label, c[0], c[1]);
    }
    private Label metaLabel(String text) { Label l=new Label(text); l.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;"); return l; }
    private Label sectionHeader(String text) { Label l=new Label(text); l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1F2937;"); return l; }
    private HBox noMedia(String icon, String msg) {
        HBox box=new HBox(10); box.setAlignment(Pos.CENTER_LEFT); box.setPadding(new Insets(14,18,14,18));
        box.setStyle("-fx-background-color:#F9FAFB;-fx-background-radius:10;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:10;");
        Label ic=new Label(icon); ic.setStyle("-fx-font-size:20;-fx-text-fill:#9CA3AF;");
        Label lbl=new Label(msg); lbl.setStyle("-fx-font-size:12;-fx-text-fill:#9CA3AF;");
        box.getChildren().addAll(ic,lbl); return box;
    }
    private Button actionBtn(String text, String bg, String fg, String border, boolean outline) {
        Button b=new Button(text);
        b.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;" +
                "-fx-padding:"+(outline?"7 16":"8 18")+";-fx-background-radius:8;" +
                (border!=null?"-fx-border-color:"+border+";-fx-border-width:1;-fx-border-radius:8;":"") +
                (outline?"":"-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),6,0,0,2);"));
        return b;
    }

// ══════════════════════════════════════════════════════════════════════════
//  🤖 GROK — Skills Gap Analysis
// ══════════════════════════════════════════════════════════════════════════

    @FXML private void handleSkillsGap() {
        if ((groqApiKey == null || groqApiKey.isBlank()) && (grokApiKey == null || grokApiKey.isBlank())) {
            showError("Clé manquante", "Ajoutez groq.key=gsk_... dans anthropic.properties\n(Clé gratuite sur console.groq.com)");
            return;
        }
        // Récupérer les modules de cette formation pour contexte
        List<ModuleItem> modules = loadModules();
        if (modules.isEmpty()) { showInfo("Formation vide", "Aucun module à analyser."); return; }

        setAIBarStatus("🤖 Analyse en cours…", true);
        new Thread(() -> {
            String prompt = buildSkillsGapPrompt(modules);
            String result = callGeminiAPI(prompt);
            Platform.runLater(() -> {
                setAIBarStatus("", false);
                if (result != null) showAIResultDialog("🤖 Analyse des Compétences — " + formationTitre, result);
                else showError("Grok", "Erreur d'appel API. Vérifiez votre clé grok.key.");
            });
        }, "grok-skillsgap").start();
    }

    private String buildSkillsGapPrompt(List<ModuleItem> modules) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert RH et formateur. Analyse cette formation et génère une Skills Gap Analysis complète en français.\n\n");
        sb.append("FORMATION : ").append(formationTitre).append("\n");
        sb.append("NOMBRE DE MODULES : ").append(modules.size()).append("\n\n");
        sb.append("MODULES :\n");
        for (ModuleItem m : modules) {
            sb.append("- ").append(m.ordre).append(". ").append(m.titre)
                    .append(" [").append(m.typeContenu).append(", ").append(m.dureeMinutes).append(" min]");
            if (m.description != null && !m.description.isBlank()) sb.append(" — ").append(m.description);
            sb.append(" [statut: ").append(m.statut).append("]\n");
        }
        sb.append("\nGénère :\n1. Compétences développées par cette formation\n");
        sb.append("2. Lacunes identifiées à combler\n");
        sb.append("3. Recommandations de formations complémentaires\n");
        sb.append("4. Niveau de maturité des compétences (1-5)\n");
        sb.append("5. Suggestions personnalisées pour accélérer la montée en compétence\n");
        sb.append("Format bien structuré avec sections et emojis.");
        return sb.toString();
    }

// ══════════════════════════════════════════════════════════════════════════
//  🎬 YOUTUBE DATA API — Suggestions de vidéos pour la formation
// ══════════════════════════════════════════════════════════════════════════

    @FXML private void handleYoutubeSuggest() {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            showError("Clé manquante", "Ajoutez youtube.key=... dans config.properties\nConsole : console.cloud.google.com → YouTube Data API v3");
            return;
        }
        setAIBarStatus("🎬 Recherche YouTube en cours…", true);
        String query = formationTitre + " formation tutoriel";

        new Thread(() -> {
            List<String[]> videos = searchYouTubeVideos(query);
            Platform.runLater(() -> {
                setAIBarStatus("", false);
                if (videos.isEmpty()) { showInfo("YouTube", "Aucune vidéo trouvée pour : " + query); return; }
                showYouTubeResultsDialog(query, videos);
            });
        }, "youtube-suggest").start();
    }

    private List<String[]> searchYouTubeVideos(String query) {
        List<String[]> results = new ArrayList<>();
        try {
            String encoded = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlStr = "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet&type=video&maxResults=6&relevanceLanguage=fr" +
                    "&videoEmbeddable=true&safeSearch=strict" +
                    "&q=" + encoded + "&key=" + youtubeApiKey;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code != 200) {
                System.err.println("[YouTube] HTTP " + code);
                return results;
            }

            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // Parser items
            int pos = 0;
            while (true) {
                int vidIdx = json.indexOf("\"videoId\":", pos);
                if (vidIdx < 0) break;
                String videoId = parseJsonStringMVC(json, vidIdx + 10);
                int titleIdx = json.indexOf("\"title\":", vidIdx);
                String title = titleIdx >= 0 ? parseJsonStringMVC(json, titleIdx + 8) : "Sans titre";
                int chanIdx = json.indexOf("\"channelTitle\":", vidIdx);
                String channel = chanIdx >= 0 ? parseJsonStringMVC(json, chanIdx + 15) : "";
                if (videoId != null && !videoId.isBlank())
                    results.add(new String[]{ videoId, title, channel });
                pos = vidIdx + 10;
            }
        } catch (Exception e) {
            System.err.println("[YouTube] " + e.getMessage());
        }
        return results;
    }

    private void showYouTubeResultsDialog(String query, List<String[]> videos) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("🎬 Vidéos YouTube — " + query);
        dlg.getDialogPane().setPrefWidth(680);
        dlg.getDialogPane().setPrefHeight(560);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label hdr = new Label("🎬  Vidéos trouvées pour « " + query + " »");
        hdr.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#111827;");
        content.getChildren().add(hdr);

        for (String[] v : videos) {
            String videoId = v[0], title = v[1], channel = v[2];
            String ytUrl = "https://www.youtube.com/watch?v=" + videoId;
            String thumb = "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg";

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
                    "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;");

            // Mini thumbnail WebView
            WebView tw = new WebView(); tw.setPrefSize(120, 68); tw.setContextMenuEnabled(false);
            tw.getEngine().setUserAgent("Mozilla/5.0 Chrome/124.0");
            tw.getEngine().loadContent("<html><body style='margin:0;background:#000;overflow:hidden;'>" +
                    "<img src='" + thumb + "' width='120' height='68' style='object-fit:cover;display:block;'>" +
                    "</body></html>", "text/html");

            VBox info = new VBox(4); HBox.setHgrow(info, Priority.ALWAYS);
            Label t = new Label(title); t.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1F2937;"); t.setWrapText(true);
            Label c = new Label("📺  " + channel); c.setStyle("-fx-font-size:10;-fx-text-fill:#6B7280;");
            Label u = new Label(ytUrl); u.setStyle("-fx-font-size:9;-fx-text-fill:#9CA3AF;");
            info.getChildren().addAll(t, c, u);

            Button btnWatch = new Button("▶");
            btnWatch.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-font-size:14;" +
                    "-fx-padding:8 12;-fx-background-radius:8;-fx-cursor:hand;");
            btnWatch.setOnAction(e -> openInBrowser(ytUrl));

            Button btnCopy = new Button("🔗");
            btnCopy.setStyle("-fx-background-color:#EDE9FE;-fx-text-fill:#5B21B6;-fx-font-size:14;" +
                    "-fx-padding:8 12;-fx-background-radius:8;-fx-cursor:hand;");
            btnCopy.setOnAction(e -> {
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(ytUrl); javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                btnCopy.setText("✓");
            });

            row.getChildren().addAll(tw, info, btnWatch, btnCopy);
            content.getChildren().add(row);
        }

        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:white;-fx-background-color:white;");
        dlg.getDialogPane().setContent(sp);
        dlg.showAndWait();
    }

    // ── Groq API call (principal) + Grok fallback (OpenAI-compatible) ──────
    private String callGeminiAPI(String prompt) {
        // Garde le nom callGeminiAPI pour compatibilité avec tous les appels existants
        // Essaie Groq d'abord, puis Grok xAI en fallback
        String apiKey = (groqApiKey != null && !groqApiKey.isBlank()) ? groqApiKey : grokApiKey;
        String apiUrl = (groqApiKey != null && !groqApiKey.isBlank()) ? GROQ_URL : GROK_URL;
        String model  = (groqApiKey != null && !groqApiKey.isBlank()) ? "llama-3.3-70b-versatile" : "grok-3-mini";

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[AI] Aucune clé API disponible (groq.key ou grok.key)");
            return null;
        }
        System.out.println("[AI] Utilisation de: " + (groqApiKey != null && !groqApiKey.isBlank() ? "Groq" : "Grok") + " / " + model);
        try {
            String body = "{\"model\":\"" + model + "\"," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" + escapeJson(prompt) + "}]," +
                    "\"max_tokens\":1500,\"temperature\":0.7}";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("[AI] HTTP " + code);
            if (code != 200) {
                InputStream err = conn.getErrorStream();
                String errBody = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "(no body)";
                System.err.println("[AI] Error: " + errBody);
                // Si Groq échoue, retenter avec Grok
                if (groqApiKey != null && !groqApiKey.isBlank() && grokApiKey != null && !grokApiKey.isBlank()) {
                    System.out.println("[AI] Groq KO → fallback Grok...");
                    return callGrokFallback(prompt);
                }
                return null;
            }

            // Réponse OpenAI : {"choices":[{"message":{"content":"..."}}]}
            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int idx = json.indexOf("\"content\":");
            if (idx < 0) return null;
            return parseJsonStringMVC(json, idx + 10);

        } catch (Exception e) {
            System.err.println("[AI] " + e.getMessage());
            // Si Groq lève une exception, retenter avec Grok
            if (groqApiKey != null && !groqApiKey.isBlank() && grokApiKey != null && !grokApiKey.isBlank()) {
                System.out.println("[AI] Groq exception → fallback Grok...");
                return callGrokFallback(prompt);
            }
            return null;
        }
    }

    private String callGrokFallback(String prompt) {
        try {
            String body = "{\"model\":\"grok-3-mini\"," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" + escapeJson(prompt) + "}]," +
                    "\"max_tokens\":1500,\"temperature\":0.7}";
            HttpURLConnection conn = (HttpURLConnection) new URL(GROK_URL).openConnection();
            conn.setRequestMethod("POST"); conn.setDoOutput(true);
            conn.setConnectTimeout(15000); conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + grokApiKey);
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            int code = conn.getResponseCode();
            if (code != 200) { System.err.println("[Grok-fallback] HTTP " + code); return null; }
            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int idx = json.indexOf("\"content\":");
            if (idx < 0) return null;
            return parseJsonStringMVC(json, idx + 10);
        } catch (Exception e) { System.err.println("[Grok-fallback] " + e.getMessage()); return null; }
    }
    private String parseJsonStringMVC(String json, int start) {
        while (start < json.length() && json.charAt(start) != '"') start++;
        if (start >= json.length()) return null;
        start++;
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i-1) != '\\')) break;
            if (c == '\\' && i+1 < json.length()) {
                char nx = json.charAt(i+1);
                if (nx=='n')  { sb.append('\n'); i+=2; continue; }
                if (nx=='"')  { sb.append('"');  i+=2; continue; }
                if (nx=='\\') { sb.append('\\'); i+=2; continue; }
                if (nx=='t')  { sb.append('\t'); i+=2; continue; }
                if (nx=='r')  { i+=2; continue; }
            }
            sb.append(c); i++;
        }
        return sb.toString();
    }

    private void showAIResultDialog(String title, String result) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.getDialogPane().setPrefWidth(700);
        dlg.getDialogPane().setPrefHeight(600);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        Label hdr = new Label(title);
        hdr.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#1F2937;");

        // Résultat rendu en HTML WebView
        WebView wv = new WebView();
        wv.setPrefHeight(450);
        VBox.setVgrow(wv, Priority.ALWAYS);
        String htmlBody = result.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\n","<br>")
                .replaceAll("\\*\\*(.+?)\\*\\*","<strong>$1</strong>")
                .replaceAll("#{3}\\s(.+?)(<br>|$)","<h3>$1</h3>")
                .replaceAll("#{2}\\s(.+?)(<br>|$)","<h2>$1</h2>")
                .replaceAll("#\\s(.+?)(<br>|$)","<h1>$1</h1>");
        String html = "<!DOCTYPE html><html><head><meta charset=\'UTF-8\'>" +
                "<meta http-equiv=\'Content-Type\' content=\'text/html; charset=utf-8\'><style>" +
                "*{margin:0;padding:0;box-sizing:border-box;}" +
                "body{font-family:\'Segoe UI\',Arial,sans-serif;font-size:14px;color:#1F2937;padding:24px;line-height:1.8;background:white;}" +
                "h1,h2,h3{color:#4F46E5;margin:16px 0 8px;}" +
                "p{margin:0 0 12px;}" +
                "ul{margin:8px 0 12px 20px;}" +
                "li{margin:4px 0;}" +
                "strong{color:#111827;font-weight:700;}" +
                "</style></head><body>" + htmlBody + "</body></html>";
        // Charger en base64 pour garantir l'encodage UTF-8 (fix emojis)
        String b64 = java.util.Base64.getEncoder().encodeToString(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        wv.getEngine().load("data:text/html;charset=utf-8;base64," + b64);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button btnCopy = new Button("📋 Copier tout");
        btnCopy.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-padding:8 18;-fx-background-radius:8;-fx-cursor:hand;");
        btnCopy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(result); javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            btnCopy.setText("✓ Copié !");
        });
        btnRow.getChildren().add(btnCopy);

        root.getChildren().addAll(hdr, wv, btnRow);
        dlg.getDialogPane().setContent(root);
        dlg.showAndWait();
    }

    private void setAIBarStatus(String msg, boolean disable) {
        if (lblAiStatus != null) lblAiStatus.setText(msg);
        if (btnSkillsGap    != null) btnSkillsGap.setDisable(disable);
        if (btnGeneratePDI  != null) btnGeneratePDI.setDisable(disable);
        if (btnYoutubeSuggest != null) btnYoutubeSuggest.setDisable(disable);
    }

    @FXML private void handleBack() {
        try { test.MainFX main = test.MainFX.getInstance(); if (main!=null) main.navigateTo(test.MainFX.MY_LEARNING); }
        catch (Exception e) { showError("Navigation", e.getMessage()); }
    }

    private void showError(String t, String m) { Alert a=new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setContentText(m); a.showAndWait(); }
    private void showInfo(String t, String m)  { Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(m); a.showAndWait(); }

// ══════════════════════════════════════════════════════════════════════════
//  PDF EXPORT — Sélection de modules + génération 100% Java
// ══════════════════════════════════════════════════════════════════════════

    /** Bouton @FXML à ajouter dans le FXML : onAction="#handleExportPDF" */
    @FXML
    public void handleExportPDF() {
        List<ModuleItem> allModules = loadModules();
        if (allModules.isEmpty()) { showInfo("Export PDF", "Aucun module à exporter."); return; }
        showModuleSelectionDialog(allModules);
    }

    private void showModuleSelectionDialog(List<ModuleItem> allModules) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("📄 Exporter en PDF");
        dlg.getDialogPane().setPrefWidth(560);
        dlg.getDialogPane().setPrefHeight(580);

        ButtonType exportBtn = new ButtonType("📄  Exporter", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(exportBtn, ButtonType.CANCEL);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F5F7FF;");

        // En-tête
        VBox header = new VBox(6);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle("-fx-background-color:white;-fx-border-color:#E0E7FF;-fx-border-width:0 0 1.5 0;");
        Label hdrTitle = new Label("📄  Export PDF — " + (formationTitre != null ? formationTitre : "Formation"));
        hdrTitle.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1E1B4B;");
        Label hdrSub = new Label("Sélectionnez les modules à inclure dans le rapport.");
        hdrSub.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        header.getChildren().addAll(hdrTitle, hdrSub);

        // Barre d'options
        List<CheckBox> checkBoxes = new ArrayList<>();
        HBox selBar = new HBox(12);
        selBar.setPadding(new Insets(10, 20, 8, 20));
        selBar.setAlignment(Pos.CENTER_LEFT);
        selBar.setStyle("-fx-background-color:#EEF2FF;-fx-border-color:#E0E7FF;-fx-border-width:0 0 1 0;");

        Button btnAll  = new Button("✓  Tout");
        Button btnNone = new Button("✗  Aucun");
        String bs = "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:5 12;-fx-background-radius:8;-fx-cursor:hand;";
        btnAll.setStyle(bs + "-fx-background-color:#4F46E5;-fx-text-fill:white;");
        btnNone.setStyle(bs + "-fx-background-color:white;-fx-text-fill:#6B7280;-fx-border-color:#D1D5DB;-fx-border-width:1;-fx-border-radius:8;");

        CheckBox chkContent = new CheckBox("Inclure le contenu texte");
        CheckBox chkDesc    = new CheckBox("Inclure les descriptions");
        chkContent.setSelected(true); chkDesc.setSelected(true);
        chkContent.setStyle("-fx-font-size:11;"); chkDesc.setStyle("-fx-font-size:11;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        selBar.getChildren().addAll(btnAll, btnNone, sp, chkContent, chkDesc);

        btnAll.setOnAction(e  -> checkBoxes.forEach(cb -> cb.setSelected(true)));
        btnNone.setOnAction(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));

        // Liste des modules
        VBox listBox = new VBox(6);
        listBox.setPadding(new Insets(12, 16, 12, 16));

        for (ModuleItem m : allModules) {
            boolean done   = "completed".equals(m.statut);
            boolean active = "in_progress".equals(m.statut);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            String rowBg = done ? "#F0FDF4" : active ? "#EFF6FF" : "white";
            String rowBd = done ? "#86EFAC" : active ? "#93C5FD" : "#E5E7EB";
            row.setStyle("-fx-background-color:" + rowBg + ";-fx-background-radius:10;" +
                    "-fx-border-color:" + rowBd + ";-fx-border-width:1;-fx-border-radius:10;");

            CheckBox cb = new CheckBox(); cb.setSelected(true); checkBoxes.add(cb);

            Label ordLbl = new Label(String.valueOf(m.ordre));
            ordLbl.setPrefSize(26, 26); ordLbl.setMinSize(26, 26); ordLbl.setAlignment(Pos.CENTER);
            String oBg = done ? "#D1FAE5" : active ? "#DBEAFE" : "#F3F4F6";
            String oFg = done ? "#166534" : active ? "#1D4ED8" : "#6B7280";
            ordLbl.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:" + oFg +
                    ";-fx-background-color:" + oBg + ";-fx-background-radius:50;");

            Label typeIco = new Label(getContentTypeEmoji(m.typeContenu));
            typeIco.setStyle("-fx-font-size:16;");

            VBox info = new VBox(2); HBox.setHgrow(info, Priority.ALWAYS);
            Label titleLbl = new Label(m.titre);
            titleLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" +
                    (done ? "#166534" : active ? "#1E40AF" : "#111827") + ";");
            String metaText = m.typeContenu + "   •   " + m.dureeMinutes + " min" +
                    (done && m.scoreQuiz >= 0 ? "   •   ⭐ " + m.scoreQuiz + "%" : "");
            Label metaLbl = new Label(metaText);
            metaLbl.setStyle("-fx-font-size:10;-fx-text-fill:#9CA3AF;");
            info.getChildren().addAll(titleLbl, metaLbl);

            String chipTxt = done ? "✓ Terminé" : active ? "En cours" : "Non démarré";
            String chipBg  = done ? "#D1FAE5" : active ? "#DBEAFE" : "#F3F4F6";
            String chipFg  = done ? "#166534" : active ? "#1D4ED8" : "#6B7280";
            Label chip = new Label(chipTxt);
            chip.setStyle("-fx-font-size:9;-fx-font-weight:bold;-fx-text-fill:" + chipFg +
                    ";-fx-background-color:" + chipBg + ";-fx-padding:3 8;-fx-background-radius:20;");

            row.getChildren().addAll(cb, ordLbl, typeIco, info, chip);
            listBox.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().addAll(header, selBar, scroll);
        dlg.getDialogPane().setContent(root);

        Platform.runLater(() -> {
            javafx.scene.Node node = dlg.getDialogPane().lookupButton(exportBtn);
            if (node != null) node.setStyle("-fx-background-color:linear-gradient(to right,#4F46E5,#7C3AED);" +
                    "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;" +
                    "-fx-padding:10 24;-fx-background-radius:10;-fx-cursor:hand;");
        });

        dlg.showAndWait().ifPresent(result -> {
            if (result != exportBtn) return;
            List<ModuleItem> selected = new ArrayList<>();
            for (int i = 0; i < allModules.size(); i++) {
                if (i < checkBoxes.size() && checkBoxes.get(i).isSelected())
                    selected.add(allModules.get(i));
            }
            if (selected.isEmpty()) { showInfo("Export PDF", "Aucun module sélectionné."); return; }
            launchPdfExport(selected, chkContent.isSelected(), chkDesc.isSelected());
        });
    }

    private String getContentTypeEmoji(String type) {
        if (type == null) return "📄";
        switch (type.toLowerCase()) {
            case "video":    return "🎥";
            case "cours": case "lecture": return "📖";
            case "exercice": case "exercise": return "🏋";
            case "quiz":     return "❓";
            default:         return "📄";
        }
    }

    private void launchPdfExport(List<ModuleItem> modules, boolean includeContent, boolean includeDesc) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        String safeName = (formationTitre != null ? formationTitre.replaceAll("[^a-zA-Z0-9_-]", "_") : "formation");
        fc.setInitialFileName("rapport_" + safeName + "_" + java.time.LocalDate.now() + ".pdf");
        java.io.File outFile = fc.showSaveDialog(vboxModules.getScene().getWindow());
        if (outFile == null) return;

        // Récupérer le formateur
        String formateurNom = "";
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT CONCAT(fo.prenom,' ',fo.nom) AS nom FROM formation f LEFT JOIN formateur fo ON fo.id=f.formateur_id WHERE f.id=?");
            ps.setInt(1, formationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getString("nom") != null) formateurNom = rs.getString("nom");
        } catch (Exception ignored) {}

        int completed = (int) modules.stream().filter(m -> "completed".equals(m.statut)).count();
        int progress  = modules.isEmpty() ? 0 : completed * 100 / modules.size();

        // Construire ExportData
        competence.pdf.ModulePdfExporter.ExportData data =
                new competence.pdf.ModulePdfExporter.ExportData(
                        formationTitre != null ? formationTitre : "Formation",
                        formateurNom, progress, includeContent, includeDesc);

        for (ModuleItem m : modules) {
            data.modules.add(new competence.pdf.ModulePdfExporter.ModuleData(
                    m.ordre, m.titre, m.typeContenu, m.dureeMinutes,
                    m.statut != null ? m.statut : "not_started",
                    m.scoreQuiz,
                    includeDesc ? (m.description != null ? m.description : "") : "",
                    includeContent ? (m.contenuTexte != null ? m.contenuTexte : "") : ""
            ));
        }

        if (lblAiStatus != null) lblAiStatus.setText("⏳ Génération du PDF…");

        java.io.File finalOut = outFile;
        int finalCount = modules.size();
        new Thread(() -> {
            try {
                new competence.pdf.ModulePdfExporter().exportToFile(data, finalOut);
                Platform.runLater(() -> {
                    if (lblAiStatus != null) lblAiStatus.setText("");
                    showPDFSuccessDialog(finalOut, finalCount);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblAiStatus != null) lblAiStatus.setText("");
                    showError("Export PDF", "Erreur : " + e.getMessage());
                });
            }
        }, "pdf-export").start();
    }

    private void showPDFSuccessDialog(java.io.File outFile, int count) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("PDF exporté !");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox content = new VBox(14);
        content.setPadding(new Insets(28, 32, 20, 32));
        content.setAlignment(Pos.CENTER);
        Label ic = new Label("✅"); ic.setStyle("-fx-font-size:42;");
        Label title = new Label("PDF généré avec succès !");
        title.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#166534;");
        Label sub = new Label(count + " modules  →  " + outFile.getName());
        sub.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
        HBox btnRow = new HBox(12); btnRow.setAlignment(Pos.CENTER);
        Button btnOpen = new Button("📂  Ouvrir dossier");
        btnOpen.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#4F46E5;-fx-font-weight:bold;" +
                "-fx-font-size:11;-fx-padding:9 18;-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1.5;-fx-border-radius:10;");
        Button btnView = new Button("👁  Ouvrir PDF");
        btnView.setStyle("-fx-background-color:linear-gradient(to right,#4F46E5,#7C3AED);" +
                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11;" +
                "-fx-padding:9 18;-fx-background-radius:10;-fx-cursor:hand;");
        btnOpen.setOnAction(e -> { try { java.awt.Desktop.getDesktop().open(outFile.getParentFile()); } catch (Exception ex) { showError("Erreur", ex.getMessage()); } });
        btnView.setOnAction(e -> { try { java.awt.Desktop.getDesktop().open(outFile); } catch (Exception ex) { showError("Erreur", ex.getMessage()); } });
        btnRow.getChildren().addAll(btnOpen, btnView);
        content.getChildren().addAll(ic, title, sub, btnRow);
        dlg.getDialogPane().setContent(content);
        dlg.showAndWait();
    }

    // ─── Inner classes ────────────────────────────────────────────────────────
    private static class ModuleItem {
        public String videoUrl;
        int id, dureeMinutes, ordre, scoreQuiz; String titre, description, typeContenu, statut, contenuTexte, urlRessource;
        ModuleItem(ResultSet rs) throws SQLException {
            id=rs.getInt("id"); titre=rs.getString("titre"); description=rs.getString("description");
            typeContenu=rs.getString("type_contenu"); dureeMinutes=rs.getInt("duree_minutes"); ordre=rs.getInt("ordre");
            statut=rs.getString("statut"); scoreQuiz=rs.getInt("score");
            try { contenuTexte=rs.getString("contenu_texte"); urlRessource=rs.getString("url_ressource"); }
            catch (SQLException e) { contenuTexte=""; urlRessource=""; }
            videoUrl = (urlRessource != null && !urlRessource.isBlank()) ? urlRessource : "";
        }
        ModuleItem(ResultSet rs, boolean noContent) throws SQLException { this(rs); contenuTexte=""; urlRessource=""; }
    }
    private static class ExerciceResult { String contenu, feedbackIa, dateSoumission; int noteIa; }
    private static class QuizQuestion {
        int id; String question, optionA, optionB, optionC, optionD, bonneReponse;
        QuizQuestion(ResultSet rs) throws SQLException {
            id=rs.getInt("id"); question=rs.getString("question"); optionA=rs.getString("option_a");
            optionB=rs.getString("option_b"); optionC=rs.getString("option_c"); optionD=rs.getString("option_d"); bonneReponse=rs.getString("bonne_reponse");
        }
    }
}