package competence.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import utils.MyDataBase;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contrôleur pour la fenêtre de lecture de module
 * Features: YouTube intégré, highlights, notes, progression,
 *           Gemini AI (résumé, explication, quiz), YouTube Data API suggestions
 */
public class ModuleLearnerController {

    @FXML private Label lblModuleTitle;
    @FXML private Label lblModuleType;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblProgress;

    // Contenu principal
    @FXML private StackPane contentContainer;
    @FXML private VBox      sidebarPanel;    // sidebar droite (Notes/IA/Vidéos)
    @FXML private WebView videoPlayer;
    @FXML private ScrollPane courseScrollPane;
    @FXML private VBox courseContent;

    // Sidebar — onglets
    @FXML private Button btnTabNotes;
    @FXML private Button btnTabAI;
    @FXML private Button btnTabVideos;
    @FXML private VBox panelNotes;
    @FXML private VBox panelAI;
    @FXML private VBox panelVideos;

    // Notes
    @FXML private TextArea txtNotes;
    @FXML private VBox notesHistory;
    @FXML private Button btnSaveNote;

    // Contrôles
    @FXML private Button btnMarkComplete;
    @FXML private Button btnClose;
    @FXML private Button btnPrevious;
    @FXML private Button btnNext;

    // IA
    @FXML private Button btnAISummary;
    @FXML private Button btnAIExplain;
    @FXML private Button btnAIQuiz;
    @FXML private Label  lblAIStatus;
    @FXML private VBox   aiResponseContainer;

    // YouTube
    @FXML private Button btnSearchVideos;
    @FXML private Label  lblVideoStatus;
    @FXML private VBox   videoResultsContainer;

    // ─── Keys ────────────────────────────────────────────────────────────────
    private String groqApiKey;         // groq.key (principal)
    private String grokApiKey;         // grok.key (fallback xAI)
    private String youtubeApiKey;      // youtube.key dans config.properties

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROK_URL =
            "https://api.x.ai/v1/chat/completions";
    private static final String MYMEMORY_URL =
            "https://api.mymemory.translated.net/get";

    // ── VLC Player actif ─────────────────────────────────────────────────
    private VlcVideoPlayer activeVlcPlayer = null;

    private Connection connection;
    private Stage stage;
    private int moduleId;
    private int inscriptionId;
    private int employeId;
    private int currentIndex;
    private java.util.List<int[]> allModules = new java.util.ArrayList<>();
    private Runnable onCloseRefresh;
    private String moduleTitle;
    private String typeContenu;
    private String videoUrl;
    private String contenuCours;
    private Map<String, String> highlights = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        connection = MyDataBase.getInstance().getCnx();
        loadApiKeys();
        setupNotesSaving();
        setupVideoPlayer();
        // Sidebar toujours visible — Notes / IA / Vidéos
        if (sidebarPanel != null) {
            sidebarPanel.setVisible(true);
            sidebarPanel.setManaged(true);
        }
        showPanel(panelNotes, btnTabNotes);
        // Injecter le divider redimensionnable après que le FXML soit chargé
        Platform.runLater(this::injectResizableDivider);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RESIZABLE DIVIDER — drag entre vidéo et sidebar IA
    // ══════════════════════════════════════════════════════════════════════
    private void injectResizableDivider() {
        if (contentContainer == null || sidebarPanel == null) return;
        javafx.scene.Parent parent = contentContainer.getParent();
        if (!(parent instanceof javafx.scene.layout.HBox)) return;
        javafx.scene.layout.HBox mainRow = (javafx.scene.layout.HBox) parent;

        // Trouver les indices
        int contentIdx = mainRow.getChildren().indexOf(contentContainer);
        int sidebarIdx = mainRow.getChildren().indexOf(sidebarPanel);
        if (contentIdx < 0 || sidebarIdx < 0) return;

        // Créer le divider
        javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
        divider.setPrefWidth(5);
        divider.setMinWidth(5);
        divider.setMaxWidth(5);
        divider.setCursor(javafx.scene.Cursor.H_RESIZE);
        divider.setStyle(
                "-fx-background-color: #CBD5E1;" +
                        "-fx-cursor: h-resize;"
        );

        // Hover effect
        divider.setOnMouseEntered(e -> divider.setStyle(
                "-fx-background-color: #6366F1;" +
                        "-fx-cursor: h-resize;"
        ));
        divider.setOnMouseExited(e -> divider.setStyle(
                "-fx-background-color: #CBD5E1;" +
                        "-fx-cursor: h-resize;"
        ));

        // Insérer le divider entre content et sidebar
        int insertIdx = Math.min(contentIdx, sidebarIdx) + 1;
        mainRow.getChildren().add(insertIdx, divider);

        // Fixer les contraintes initiales
        javafx.scene.layout.HBox.setHgrow(contentContainer, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(sidebarPanel,    javafx.scene.layout.Priority.NEVER);
        javafx.scene.layout.HBox.setHgrow(divider,         javafx.scene.layout.Priority.NEVER);
        sidebarPanel.setPrefWidth(320);
        sidebarPanel.setMinWidth(180);
        sidebarPanel.setMaxWidth(600);

        // Drag logic
        final double[] dragStartX    = {0};
        final double[] dragStartSide = {0};

        divider.setOnMousePressed(e -> {
            dragStartX[0]    = e.getScreenX();
            dragStartSide[0] = sidebarPanel.getWidth();
            divider.setStyle("-fx-background-color: #4F46E5; -fx-cursor: h-resize;");
            e.consume();
        });

        divider.setOnMouseDragged(e -> {
            double delta     = dragStartX[0] - e.getScreenX(); // négatif = agrandir sidebar
            double newWidth  = Math.max(180, Math.min(700, dragStartSide[0] + delta));
            sidebarPanel.setPrefWidth(newWidth);
            e.consume();
        });

        divider.setOnMouseReleased(e -> {
            divider.setStyle("-fx-background-color: #CBD5E1; -fx-cursor: h-resize;");
            e.consume();
        });

        // Double-clic : reset sidebar à 320px
        divider.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                sidebarPanel.setPrefWidth(320);
            }
        });

        // Tooltip
        Tooltip tip = new Tooltip("Glisser pour redimensionner · Double-clic pour réinitialiser");
        tip.setStyle("-fx-font-size:11;-fx-background-color:#1E293B;-fx-text-fill:white;");
        Tooltip.install(divider, tip);

        System.out.println("✓ Resizable divider injected");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  API KEY LOADING
    // ══════════════════════════════════════════════════════════════════════
    private void loadApiKeys() {
        // Chercher dans src/main/resources/config.properties
        String[] paths = {
                "src/main/resources/config.properties",
                "config.properties",
                "src/main/resources/anthropic.properties",
                "anthropic.properties"
        };
        for (String path : paths) {
            try (InputStream is = new FileInputStream(path)) {
                Properties p = new Properties();
                p.load(is);
                if (groqApiKey    == null) groqApiKey    = p.getProperty("groq.key");
                if (grokApiKey    == null) grokApiKey    = p.getProperty("grok.key", p.getProperty("gemini.key"));
                if (youtubeApiKey == null) youtubeApiKey = p.getProperty("youtube.key", p.getProperty("youtube.api.key"));
                if (groqApiKey != null && youtubeApiKey != null) break;
            } catch (Exception ignored) {}
        }
        // Essayer aussi via classloader
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                Properties p = new Properties(); p.load(is);
                if (groqApiKey    == null) groqApiKey    = p.getProperty("groq.key");
                if (grokApiKey    == null) grokApiKey    = p.getProperty("grok.key", p.getProperty("gemini.key"));
                if (youtubeApiKey == null) youtubeApiKey = p.getProperty("youtube.key");
            }
        } catch (Exception ignored) {}

        System.out.println("Groq key:    " + (groqApiKey    != null ? "✓ chargée" : "✗ manquante"));
        System.out.println("Grok key:    " + (grokApiKey    != null ? "✓ chargée" : "✗ manquante"));
        System.out.println("YouTube key: " + (youtubeApiKey != null ? "✓ chargée" : "✗ manquante"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIDEBAR TAB SWITCHING
    // ══════════════════════════════════════════════════════════════════════
    @FXML private void switchToNotes()  { showPanel(panelNotes,  btnTabNotes);  }
    @FXML private void switchToAI()     { showPanel(panelAI,     btnTabAI);     }
    @FXML private void switchToVideos() { showPanel(panelVideos, btnTabVideos); }

    private void showPanel(VBox target, Button activeBtn) {
        VBox[]   panels = { panelNotes, panelAI, panelVideos };
        Button[] tabs   = { btnTabNotes, btnTabAI, btnTabVideos };
        String styleOn  = "-fx-background-color:#6366F1;-fx-text-fill:white;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:13 0;-fx-background-radius:0;-fx-cursor:hand;";
        String styleOff = "-fx-background-color:#F8F9FF;-fx-text-fill:#94A3B8;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:13 0;-fx-background-radius:0;-fx-cursor:hand;";
        for (int i = 0; i < panels.length; i++) {
            boolean on = panels[i] == target;
            if (panels[i] != null) {
                panels[i].setVisible(on);
                panels[i].setManaged(on);
                if (on) {
                    panels[i].setMaxWidth(Double.MAX_VALUE);
                    VBox.setVgrow(panels[i], Priority.ALWAYS);
                }
            }
            if (tabs[i]   != null) tabs[i].setStyle(on ? styleOn : styleOff);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnCloseRefresh(Runnable callback) {
        this.onCloseRefresh = callback;
    }

    /**
     * @param allModuleIds  liste ordonnée de [moduleId, ordre] de tous les modules de la formation
     * @param currentIdx    index du module courant dans allModuleIds
     */
    public void loadModule(int moduleId, int inscriptionId, int employeId,
                           String title, String type, String videoUrl, String contenu,
                           java.util.List<int[]> allModuleIds, int currentIdx) {
        this.moduleId      = moduleId;
        this.inscriptionId = inscriptionId;
        this.employeId     = employeId;
        this.moduleTitle   = title;
        this.typeContenu   = type;
        this.videoUrl      = videoUrl;
        this.contenuCours  = contenu;
        this.allModules    = allModuleIds != null ? allModuleIds : new java.util.ArrayList<>();
        this.currentIndex  = currentIdx;

        // Sidebar toujours visible
        if (sidebarPanel != null) {
            sidebarPanel.setVisible(true);
            sidebarPanel.setManaged(true);
        }

        if (lblModuleTitle != null) lblModuleTitle.setText(title);
        if (lblModuleType  != null) lblModuleType.setText(getTypeIcon(type) + "  " + type);

        updateNavButtons();
        loadProgress();
        loadExistingNotes();
        loadExistingHighlights();
        renderContent();
    }

    /** Rétrocompatibilité — appelé si on n'a pas la liste complète */
    public void loadModule(int moduleId, int inscriptionId, String title, String type,
                           String videoUrl, String contenu) {
        loadModule(moduleId, inscriptionId, 1, title, type, videoUrl, contenu, null, 0);
    }

    private boolean isCompleted = false; // true après avoir cliqué "Terminer"

    private void updateNavButtons() {
        if (btnPrevious != null) {
            btnPrevious.setDisable(currentIndex <= 0);
            btnPrevious.setOpacity(currentIndex <= 0 ? 0.4 : 1.0);
        }
        if (btnNext != null) {
            boolean isLast   = currentIndex >= allModules.size() - 1;
            boolean locked   = !isCompleted; // verrouillé jusqu'au clic sur Terminer
            btnNext.setDisable(isLast || locked);
            btnNext.setOpacity((isLast || locked) ? 0.4 : 1.0);
            btnNext.setText(locked ? "🔒  Suivant" : "Suivant →");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONTENT RENDERING
    // ══════════════════════════════════════════════════════════════════════

    private void updateSidebarVisibility(String type) {
        if (sidebarPanel == null) return;
        sidebarPanel.setVisible(true);
        sidebarPanel.setManaged(true);
    }

    private void renderContent() {
        String type = (typeContenu != null) ? typeContenu.toLowerCase().trim() : "";
        boolean hasVideo = videoUrl != null && !videoUrl.isBlank();
        boolean hasText  = contenuCours != null && !contenuCours.isBlank();

        // Sidebar visible uniquement pour video et quiz (pas lecture)
        updateSidebarVisibility(type);

        switch (type) {
            case "video":
                if (hasVideo) { renderVideo(); }
                else if (hasText) { renderCourse(); }  // fallback: texte si pas d'URL
                else { renderEmpty("🎬", "Aucune vidéo associée à ce module."); }
                break;
            case "reading":
            case "cours":
            case "lecture":
                if (hasText) { renderCourse(); }
                else if (hasVideo) { renderVideo(); }
                else { renderEmpty("📖", "Aucun contenu de lecture disponible."); }
                break;
            case "quiz":
                renderQuiz();
                break;
            case "exercise":
            case "exercice":
                renderExercise();
                break;
            default:
                // Détection automatique par contenu
                if (hasVideo) { renderVideo(); }
                else if (hasText) { renderCourse(); }
                else { renderEmpty("📄", "Aucun contenu disponible pour ce module."); }
                break;
        }
    }

    private void renderEmpty(String icon, String msg) {
        if (courseScrollPane == null || courseContent == null) return;
        if (videoPlayer != null) { videoPlayer.setVisible(false); videoPlayer.setManaged(false); }
        courseScrollPane.setVisible(true); courseScrollPane.setManaged(true);
        courseScrollPane.setStyle("-fx-background:#F5F7FF;-fx-background-color:#F5F7FF;");
        courseContent.getChildren().clear();
        javafx.scene.layout.VBox emptyBox = new javafx.scene.layout.VBox(16);
        emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
        emptyBox.setPadding(new javafx.geometry.Insets(80));
        Label ic = new Label(icon); ic.setStyle("-fx-font-size:50;");
        Label lbl = new Label(msg); lbl.setStyle("-fx-font-size:15;-fx-text-fill:#94A3B8;-fx-font-style:italic;");
        emptyBox.getChildren().addAll(ic, lbl);
        courseContent.getChildren().add(emptyBox);
    }

    // ── VIDEO PLAYER ──────────────────────────────────────────────────────

    private void setupVideoPlayer() {
        if (videoPlayer != null) { videoPlayer.setVisible(false); videoPlayer.setManaged(false); }
    }

    private void renderVideo() {
        if (contentContainer == null) return;

        // Stopper VLC précédent
        if (activeVlcPlayer != null) {
            activeVlcPlayer.stopAndClear();
            contentContainer.getChildren().removeIf(n -> n instanceof VlcVideoPlayer);
            final VlcVideoPlayer toDispose = activeVlcPlayer;
            activeVlcPlayer = null;
            new Thread(() -> toDispose.dispose(), "vlc-dispose-thread").start();
        }

        // Masquer les autres contenus (NE PAS les supprimer)
        if (videoPlayer      != null) { videoPlayer.setVisible(false);      videoPlayer.setManaged(false); }
        if (courseScrollPane != null) { courseScrollPane.setVisible(false); courseScrollPane.setManaged(false); }

        // Créer et ajouter le player VLC
        VlcVideoPlayer vlc = new VlcVideoPlayer(videoUrl, moduleTitle);
        vlc.setMaxWidth(Double.MAX_VALUE);
        vlc.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(vlc, javafx.geometry.Pos.TOP_LEFT);
        activeVlcPlayer = vlc;
        contentContainer.getChildren().add(vlc);

        Platform.runLater(() -> {
            if (activeVlcPlayer == vlc) {
                vlc.prefWidthProperty().bind(contentContainer.widthProperty());
                vlc.prefHeightProperty().bind(contentContainer.heightProperty());
            }
        });
        System.out.println("✓ VLCJ Player: " + videoUrl);
    }

    private String extractYouTubeId(String url) {
        if (url == null || url.isBlank()) return null;
        Pattern p = Pattern.compile("(?:youtube\\.com/(?:watch\\?v=|embed/)|youtu\\.be/)([^&\\?/]+)");
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private String extractYouTubeEmbedUrl(String url) {
        if (url == null || url.isBlank()) return "";
        // Vimeo
        Pattern p = Pattern.compile("vimeo\\.com/(\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) return "https://player.vimeo.com/video/" + m.group(1) + "?autoplay=1&dnt=1";
        return url;
    }

    // ── COURSE CONTENT (with highlighting) ───────────────────────────────

    private void renderCourse() {
        if (courseScrollPane == null || courseContent == null) return;

        // Stopper VLC si actif (sans toucher au container)
        if (activeVlcPlayer != null) {
            activeVlcPlayer.stopAndClear();
            contentContainer.getChildren().removeIf(n -> n instanceof VlcVideoPlayer);
            final VlcVideoPlayer toDispose = activeVlcPlayer;
            activeVlcPlayer = null;
            new Thread(() -> toDispose.dispose(), "vlc-dispose-thread").start();
        }

        if (videoPlayer != null) { videoPlayer.setVisible(false); videoPlayer.setManaged(false); }
        courseScrollPane.setVisible(true);
        courseScrollPane.setManaged(true);
        courseScrollPane.setStyle("-fx-background:#F5F7FF;-fx-background-color:#F5F7FF;");
        courseScrollPane.setFitToWidth(true);
        courseScrollPane.setFitToHeight(false);
        VBox.setVgrow(courseScrollPane, Priority.ALWAYS);

        courseContent.getChildren().clear();
        courseContent.setStyle("-fx-background-color:#F5F7FF;-fx-padding:0;");

        if (contenuCours == null || contenuCours.isBlank()) {
            Label empty = new Label("Aucun contenu disponible");
            empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:14;-fx-padding:40;");
            courseContent.getChildren().add(empty);
            return;
        }

        // WebView créé en premier pour être passé à la barre de traduction
        WebView wv = new WebView();
        wv.setContextMenuEnabled(false);
        VBox.setVgrow(wv, Priority.ALWAYS);
        wv.setMaxHeight(Double.MAX_VALUE);
        wv.setMaxWidth(Double.MAX_VALUE);
        String htmlForCourse = buildHtml(contenuCours);
        String b64course = java.util.Base64.getEncoder().encodeToString(htmlForCourse.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        wv.getEngine().load("data:text/html;charset=utf-8;base64," + b64course);

        // Barre de traduction FR ↔ EN
        HBox translateBar = buildTranslateBar(contenuCours, null, wv, "cours");
        courseContent.getChildren().addAll(translateBar, wv);
        VBox.setVgrow(wv, Priority.ALWAYS);
    }

    /**
     * Converts Markdown / plain text → rich, colourful HTML — bright light theme.
     */
    private String buildHtml(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>")
                .append("*{margin:0;padding:0;box-sizing:border-box;}")
                .append("html,body{height:100%;}")
                .append("body{")
                .append("  font-family:'Segoe UI',system-ui,sans-serif;")
                .append("  font-size:15px;color:#1E293B;line-height:1.9;")
                .append("  padding:48px 12% 80px;background:#F5F7FF;")
                .append("}")
                // H1
                .append("h1{font-size:26px;font-weight:800;color:#1E1B4B;")
                .append("  margin:0 0 32px;padding:24px 28px;")
                .append("  background:linear-gradient(135deg,#EEF2FF 0%,#F5F3FF 100%);")
                .append("  border-radius:16px;border-left:5px solid #6366F1;")
                .append("  box-shadow:0 4px 20px rgba(99,102,241,0.12);}")
                // H2
                .append("h2{font-size:18px;font-weight:700;color:#312E81;")
                .append("  margin:40px 0 14px;padding:11px 18px;")
                .append("  background:linear-gradient(to right,#EEF2FF,transparent);")
                .append("  border-left:4px solid #818CF8;border-radius:0 12px 12px 0;}")
                // H3
                .append("h3{font-size:15.5px;font-weight:700;color:#4338CA;")
                .append("  margin:28px 0 10px;padding-left:12px;border-left:3px solid #A5B4FC;}")
                // paragraphes
                .append("p{margin:0 0 16px;color:#374151;line-height:1.9;}")
                // listes non ordonnées
                .append("ul{margin:8px 0 20px 0;padding:0;list-style:none;}")
                .append("ul li{")
                .append("  padding:9px 14px 9px 38px;margin:5px 0;color:#374151;")
                .append("  position:relative;background:white;border-radius:10px;")
                .append("  border:1px solid #E0E7FF;box-shadow:0 1px 4px rgba(99,102,241,0.06);}")
                .append("ul li::before{content:'▸';position:absolute;left:13px;")
                .append("  color:#6366F1;font-weight:700;font-size:14px;}")
                // listes ordonnées
                .append("ol{margin:8px 0 20px 0;padding:0;list-style:none;counter-reset:li;}")
                .append("ol li{counter-increment:li;")
                .append("  padding:9px 14px 9px 46px;margin:5px 0;color:#374151;")
                .append("  position:relative;background:white;border-radius:10px;")
                .append("  border:1px solid #E0E7FF;}")
                .append("ol li::before{content:counter(li);position:absolute;left:12px;")
                .append("  width:24px;height:24px;top:50%;transform:translateY(-50%);")
                .append("  background:#6366F1;color:white;border-radius:50%;")
                .append("  font-size:11px;font-weight:700;text-align:center;line-height:24px;}")
                // code inline
                .append("code{background:#F3F0FF;color:#7C3AED;padding:2px 8px;")
                .append("  border-radius:6px;font-family:'Consolas','Courier New',monospace;")
                .append("  font-size:13px;border:1px solid #DDD6FE;}")
                // bloc code
                .append("pre{background:#1E1B4B;color:#E2E8F0;padding:22px 26px;")
                .append("  border-radius:14px;overflow-x:auto;font-size:13px;")
                .append("  margin:20px 0;line-height:1.7;")
                .append("  box-shadow:0 8px 32px rgba(30,27,75,0.2);}")
                .append("pre code{background:none;color:#A5B4FC;padding:0;border:none;}")
                // blockquote
                .append("blockquote{background:linear-gradient(135deg,#FFF7ED,#FFFBEB);")
                .append("  border-left:4px solid #F59E0B;padding:16px 20px;")
                .append("  margin:20px 0;border-radius:0 14px 14px 0;color:#92400E;")
                .append("  font-style:italic;box-shadow:0 2px 12px rgba(245,158,11,0.1);}")
                // hr
                .append("hr{border:none;height:2px;")
                .append("  background:linear-gradient(to right,transparent,#C7D2FE,transparent);")
                .append("  margin:36px 0;}")
                .append("strong{font-weight:700;color:#1E1B4B;}")
                .append("em{font-style:italic;color:#6366F1;}")
                .append("</style></head><body>");

        String[] lines = text.split("\n");
        boolean inCode = false, inUl = false, inOl = false;
        for (String raw : lines) {
            String t = raw.trim();
            if (t.startsWith("```")) {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (inOl) { sb.append("</ol>"); inOl = false; }
                sb.append(inCode ? "</code></pre>" : "<pre><code>");
                inCode = !inCode; continue;
            }
            if (inCode) { sb.append(esc(raw)).append("\n"); continue; }
            if (t.isEmpty()) {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (inOl) { sb.append("</ol>"); inOl = false; }
                sb.append("<br/>"); continue;
            }
            if (t.startsWith("### ")) { closeListHtml(sb,inUl,inOl); inUl=inOl=false; sb.append("<h3>").append(fmt(t.substring(4))).append("</h3>"); continue; }
            if (t.startsWith("## "))  { closeListHtml(sb,inUl,inOl); inUl=inOl=false; sb.append("<h2>").append(fmt(t.substring(3))).append("</h2>"); continue; }
            if (t.startsWith("# "))   { closeListHtml(sb,inUl,inOl); inUl=inOl=false; sb.append("<h1>").append(fmt(t.substring(2))).append("</h1>"); continue; }
            if (t.startsWith("> "))   { closeListHtml(sb,inUl,inOl); inUl=inOl=false; sb.append("<blockquote>").append(fmt(t.substring(2))).append("</blockquote>"); continue; }
            if (t.equals("---"))      { closeListHtml(sb,inUl,inOl); inUl=inOl=false; sb.append("<hr/>"); continue; }
            if (t.startsWith("- ") || t.startsWith("* ")) {
                if (inOl) { sb.append("</ol>"); inOl = false; }
                if (!inUl) { sb.append("<ul>"); inUl = true; }
                sb.append("<li>").append(fmt(t.substring(2))).append("</li>"); continue;
            }
            if (t.matches("^\\d+\\.\\s+.*")) {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (!inOl) { sb.append("<ol>"); inOl = true; }
                sb.append("<li>").append(fmt(t.replaceFirst("^\\d+\\.\\s+",""))).append("</li>"); continue;
            }
            if (t.length() < 60 && t.equals(t.toUpperCase()) && t.matches(".*[A-Z].*") && !t.contains(".")) {
                closeListHtml(sb,inUl,inOl); inUl=inOl=false;
                sb.append("<h2>").append(esc(t)).append("</h2>"); continue;
            }
            closeListHtml(sb,inUl,inOl); inUl=inOl=false;
            sb.append("<p>").append(fmt(t)).append("</p>");
        }
        closeListHtml(sb,inUl,inOl);
        if (inCode) sb.append("</code></pre>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void closeListHtml(StringBuilder sb, boolean inUl, boolean inOl) {
        if (inUl) sb.append("</ul>");
        if (inOl) sb.append("</ol>");
    }

    private void cl(StringBuilder sb, boolean inList) { if (inList) sb.append("</ul>"); }
    private String esc(String s) { return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    private String fmt(String s) {
        return esc(s)
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("\\*(.+?)\\*",        "<em>$1</em>")
                .replaceAll("`(.+?)`",            "<code>$1</code>");
    }

    /**
     * Créer un label sélectionnable pour permettre le highlight
     */
    private Label createSelectableLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:14;-fx-text-fill:#374151;-fx-line-spacing:1.6;" +
                "-fx-wrap-text:true;-fx-max-width:680;");
        lbl.setWrapText(true);

        // Ajouter menu contextuel pour highlight
        ContextMenu contextMenu = new ContextMenu();

        MenuItem highlightYellow = new MenuItem("🟡 Highlight Yellow");
        highlightYellow.setOnAction(e -> highlightText(lbl, "#FEF3C7"));

        MenuItem highlightGreen = new MenuItem("🟢 Highlight Green");
        highlightGreen.setOnAction(e -> highlightText(lbl, "#D1FAE5"));

        MenuItem highlightBlue = new MenuItem("🔵 Highlight Blue");
        highlightBlue.setOnAction(e -> highlightText(lbl, "#DBEAFE"));

        MenuItem removeHighlight = new MenuItem("❌ Remove Highlight");
        removeHighlight.setOnAction(e -> removeHighlight(lbl));

        contextMenu.getItems().addAll(highlightYellow, highlightGreen, highlightBlue,
                new SeparatorMenuItem(), removeHighlight);

        lbl.setContextMenu(contextMenu);

        // Appliquer highlights existants
        String key = text.substring(0, Math.min(50, text.length()));
        if (highlights.containsKey(key)) {
            lbl.setStyle(lbl.getStyle() + "-fx-background-color:" + highlights.get(key) + ";" +
                    "-fx-background-radius:4;-fx-padding:4 8;");
        }

        return lbl;
    }

    private void highlightText(Label lbl, String color) {
        String text = lbl.getText();
        String key = text.substring(0, Math.min(50, text.length()));
        highlights.put(key, color);

        lbl.setStyle(lbl.getStyle() + "-fx-background-color:" + color + ";" +
                "-fx-background-radius:4;-fx-padding:4 8;");

        saveHighlight(key, color);
    }

    private void removeHighlight(Label lbl) {
        String text = lbl.getText();
        String key = text.substring(0, Math.min(50, text.length()));
        highlights.remove(key);

        lbl.setStyle("-fx-font-size:14;-fx-text-fill:#374151;-fx-line-spacing:1.6;" +
                "-fx-wrap-text:true;-fx-max-width:680;");

        deleteHighlight(key);
    }

    // ── EXERCISE ──────────────────────────────────────────────────────────

    // ══════════════════════════════════════════════════════════════════════
    //  EXERCISE — Correction IA + Score + Feedback + Sauvegarde DB
    // ══════════════════════════════════════════════════════════════════════

    private void renderExercise() {
        if (courseScrollPane == null || courseContent == null) return;

        // Stopper VLC si actif (sans toucher au container)
        if (activeVlcPlayer != null) {
            activeVlcPlayer.stopAndClear();
            contentContainer.getChildren().removeIf(n -> n instanceof VlcVideoPlayer);
            final VlcVideoPlayer toDispose = activeVlcPlayer;
            activeVlcPlayer = null;
            new Thread(() -> toDispose.dispose(), "vlc-dispose-thread").start();
        }

        if (videoPlayer != null) { videoPlayer.setVisible(false); videoPlayer.setManaged(false); }
        courseScrollPane.setVisible(true);
        courseScrollPane.setManaged(true);
        courseScrollPane.setFitToWidth(true);
        courseScrollPane.setFitToHeight(false); // false = scrollable verticalement
        VBox.setVgrow(courseScrollPane, Priority.ALWAYS);
        courseScrollPane.setStyle("-fx-background:#F8FAFC;-fx-background-color:#F8FAFC;");
        courseContent.getChildren().clear();
        courseContent.setStyle("-fx-background-color:#F5F7FF;-fx-padding:40 10%;");
        courseContent.setSpacing(20);

        String instructions = contenuCours != null ? contenuCours.trim() : "Suivez les instructions.";
        boolean hasAI = (groqApiKey != null && !groqApiKey.isBlank()) || (grokApiKey != null && !grokApiKey.isBlank());

        // ── En-tête ────────────────────────────────────────────────────────
        VBox headerBox = new VBox(10);
        headerBox.setPadding(new Insets(24, 28, 24, 28));
        headerBox.setStyle("-fx-background-color:linear-gradient(135deg,#EEF2FF,#F5F3FF);" +
                "-fx-background-radius:20;-fx-border-color:#C7D2FE;" +
                "-fx-border-width:1.5;-fx-border-radius:20;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.12),14,0,0,4);");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleIco = new Label("🏋");
        titleIco.setStyle("-fx-font-size:28;");
        VBox titleInfo = new VBox(4);
        Label title = new Label("Exercice pratique");
        title.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#1E1B4B;");
        Label titleSub = new Label(moduleTitle != null ? moduleTitle : "Module");
        titleSub.setStyle("-fx-font-size:12;-fx-text-fill:#6366F1;");
        titleInfo.getChildren().addAll(title, titleSub);
        titleRow.getChildren().addAll(titleIco, titleInfo);

        // Badge IA
        Label aiChip = new Label(hasAI ? "🤖  Correction IA activée" : "⚙️  Correction locale (pas de clé Grok)");
        aiChip.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-padding:5 14;-fx-background-radius:20;" +
                (hasAI ? "-fx-background-color:#EEF2FF;-fx-text-fill:#6366F1;" +
                        "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:20;"
                        : "-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;" +
                        "-fx-border-color:#FDE68A;-fx-border-width:1;-fx-border-radius:20;"));
        headerBox.getChildren().addAll(titleRow, aiChip);

        // ── Instructions ───────────────────────────────────────────────────
        VBox instrBox = new VBox(12);
        instrBox.setPadding(new Insets(20, 24, 20, 24));
        instrBox.setStyle("-fx-background-color:#FFFBEB;-fx-background-radius:16;" +
                "-fx-border-color:#FDE68A;-fx-border-width:1.5;-fx-border-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(245,158,11,0.1),8,0,0,3);");
        HBox instrTitleRow = new HBox(8);
        instrTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label instrIco = new Label("📋");
        instrIco.setStyle("-fx-font-size:16;");
        Label instrTitle = new Label("Instructions");
        instrTitle.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#92400E;");
        instrTitleRow.getChildren().addAll(instrIco, instrTitle);
        Label instrText = new Label(instructions);
        instrText.setStyle("-fx-font-size:13.5;-fx-text-fill:#78350F;-fx-wrap-text:true;-fx-line-spacing:4;");
        instrText.setWrapText(true);
        instrBox.getChildren().addAll(instrTitleRow, instrText);

        // Barre de traduction des instructions
        HBox translateInstrBar = buildTranslateBar(instructions, instrText, null, null);

        // ── Zone réponse ───────────────────────────────────────────────────
        VBox answerSection = new VBox(10);
        HBox answerTitleRow = new HBox(8);
        answerTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label answerIco = new Label("✍️");
        answerIco.setStyle("-fx-font-size:16;");
        Label answerLabel = new Label("Votre réponse");
        answerLabel.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        answerTitleRow.getChildren().addAll(answerIco, answerLabel);

        TextArea ta = new TextArea();
        ta.setPromptText("Écrivez votre réponse, votre code ou votre analyse ici…");
        ta.setPrefRowCount(10);
        ta.setWrapText(true);
        ta.setStyle("-fx-font-family:'Consolas','Segoe UI',monospace;-fx-font-size:13.5;" +
                "-fx-background-color:white;-fx-border-color:#D1D5DB;" +
                "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:14;");
        answerSection.getChildren().addAll(answerTitleRow, ta);

        // Charger une soumission existante
        ExerciceSubmission existing = loadLastSubmission();
        if (existing != null) ta.setText(existing.contenu);

        // ── Barre d'action ─────────────────────────────────────────────────
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(4, 0, 0, 0));

        Label statusLbl = new Label(existing != null ? "✓ Réponse précédente chargée" : "");
        statusLbl.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;-fx-font-style:italic;");
        HBox.setHgrow(statusLbl, Priority.ALWAYS);

        Button btnHistory = new Button("📜  Historique");
        btnHistory.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6366F1;-fx-font-weight:bold;" +
                "-fx-font-size:12;-fx-padding:10 20;-fx-background-radius:10;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1.5;-fx-border-radius:10;-fx-cursor:hand;");

        Button btnSubmit = new Button(hasAI ? "🤖  Soumettre et corriger" : "📤  Soumettre");
        btnSubmit.setStyle("-fx-background-color:" + (hasAI ? "linear-gradient(to right,#6366F1,#8B5CF6)" : "#6366F1") +
                ";-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-font-size:13;-fx-padding:11 28;-fx-background-radius:12;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.35),10,0,0,3);");

        actionRow.getChildren().addAll(statusLbl, btnHistory, btnSubmit);

        // ── Zone résultat IA (apparaît après soumission) ────────────────────
        VBox resultBox = new VBox(0);
        resultBox.setPadding(new Insets(8, 0, 24, 0));

        // Afficher résultat existant si déjà corrigé
        if (existing != null && existing.noteIa >= 0 && existing.feedbackIa != null) {
            buildResultCard(resultBox, existing.noteIa, existing.feedbackIa, existing.dateSoumission);
        }

        // ── Actions ────────────────────────────────────────────────────────
        String finalInstructions = instructions;
        btnSubmit.setOnAction(e -> {
            String reponse = ta.getText().trim();
            if (reponse.isEmpty()) {
                showInlineError(resultBox, "⚠️ Veuillez écrire votre réponse avant de soumettre.");
                return;
            }
            btnSubmit.setText("⏳  Correction en cours…");
            btnSubmit.setDisable(true);
            btnHistory.setDisable(true);
            statusLbl.setText("🤖 IA analyse votre réponse…");
            resultBox.getChildren().clear();

            new Thread(() -> {
                // 1. Sauvegarder en DB
                int sid = saveSubmissionToDB(reponse);

                // 2. Appel Gemini (ou correction locale)
                String feedback = hasAI
                        ? callGrokCorrection(finalInstructions, reponse)
                        : buildLocalFeedback(reponse);

                int score = extractScore(feedback);

                // 3. Mettre à jour DB avec score + feedback
                if (sid > 0) updateSubmissionScore(sid, score, feedback);

                Platform.runLater(() -> {
                    btnSubmit.setText(hasAI ? "🤖  Soumettre et corriger" : "📤  Soumettre");
                    btnSubmit.setStyle("-fx-background-color:" + (hasAI ? "linear-gradient(to right,#6366F1,#8B5CF6)" : "#6366F1") +
                            ";-fx-text-fill:white;-fx-font-weight:bold;" +
                            "-fx-font-size:13;-fx-padding:10 24;-fx-background-radius:12;-fx-cursor:hand;" +
                            "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.35),8,0,0,3);");
                    btnSubmit.setDisable(false);
                    btnHistory.setDisable(false);
                    statusLbl.setText(score >= 0
                            ? "✓ Corrigé · Note : " + score + "/100"
                            : "✓ Réponse enregistrée");
                    buildResultCard(resultBox, score, feedback,
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
                });
            }, "exercise-correction").start();
        });

        btnHistory.setOnAction(e -> showSubmissionHistory());

        courseContent.getChildren().addAll(headerBox, translateInstrBar, instrBox, answerSection, actionRow, resultBox);
    }

    // ── Construire la carte de résultat ────────────────────────────────────
    private void buildResultCard(VBox container, int score, String feedback, String date) {
        container.getChildren().clear();
        if (feedback == null) return;

        boolean passed  = score >= 70;
        boolean scored  = score >= 0;

        VBox card = new VBox(16);
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setStyle("-fx-background-color:" + (passed ? "#F0FDF4" : scored ? "#FFF7ED" : "#EEF2FF") + ";" +
                "-fx-background-radius:18;-fx-border-color:" +
                (passed ? "#4ADE80" : scored ? "#FCD34D" : "#A5B4FC") +
                ";-fx-border-width:2;-fx-border-radius:18;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),14,0,0,5);");

        // ── Score badge ───────────────────────────────────────────────────
        if (scored) {
            HBox scoreRow = new HBox(16);
            scoreRow.setAlignment(Pos.CENTER_LEFT);

            Label scoreIco = new Label(passed ? "🎉" : "📚");
            scoreIco.setStyle("-fx-font-size:32;");

            VBox scoreInfo = new VBox(6);
            HBox.setHgrow(scoreInfo, Priority.ALWAYS);

            Label scoreTitle = new Label(passed ? "Exercice réussi !" : score >= 50 ? "Peut mieux faire" : "À retravailler");
            scoreTitle.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:" +
                    (passed ? "#166534" : "#92400E") + ";");

            HBox scoreNumRow = new HBox(10);
            scoreNumRow.setAlignment(Pos.CENTER_LEFT);
            Label scoreNum = new Label(score + " / 100");
            scoreNum.setStyle("-fx-font-size:28;-fx-font-weight:bold;-fx-text-fill:" +
                    (passed ? "#059669" : "#D97706") + ";");
            Label scoreMin = new Label("(seuil : 70/100)");
            scoreMin.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;-fx-translate-y:6;");
            scoreNumRow.getChildren().addAll(scoreNum, scoreMin);

            // Barre de progression
            StackPane barStack = new StackPane();
            barStack.setAlignment(Pos.CENTER_LEFT);
            Region track = new Region();
            track.setPrefHeight(10); track.setMaxWidth(Double.MAX_VALUE);
            track.setStyle("-fx-background-color:#E5E7EB;-fx-background-radius:5;");
            Region fill = new Region();
            fill.setPrefHeight(10);
            fill.setStyle("-fx-background-color:" + (passed ? "linear-gradient(to right,#10B981,#059669)"
                    : "linear-gradient(to right,#F59E0B,#D97706)") + ";-fx-background-radius:5;");
            track.widthProperty().addListener((obs, o, n) ->
                    fill.setPrefWidth(n.doubleValue() * score / 100.0));
            barStack.getChildren().addAll(track, fill);

            scoreInfo.getChildren().addAll(scoreTitle, scoreNumRow, barStack);
            scoreRow.getChildren().addAll(scoreIco, scoreInfo);

            if (date != null) {
                Label dateLbl = new Label("🕐  " + date);
                dateLbl.setStyle("-fx-font-size:10;-fx-text-fill:#9CA3AF;");
                scoreRow.getChildren().add(dateLbl);
            }
            card.getChildren().add(scoreRow);

            // Séparateur
            Region sep = new Region(); sep.setPrefHeight(1);
            sep.setStyle("-fx-background-color:" + (passed ? "#86EFAC" : "#FDE68A") + ";");
            card.getChildren().add(sep);
        }

        // ── Feedback IA ────────────────────────────────────────────────────
        Label feedTitle = new Label("💬  Feedback" + (scored ? " de l'IA" : ""));
        feedTitle.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");

        // Rendu HTML du feedback pour un meilleur affichage
        String cleanFeedback = feedback
                .replaceAll("NOTE:\\s*\\d+(/100)?\\s*\\n?", "")
                .trim();

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                "*{margin:0;padding:0;box-sizing:border-box;}" +
                "body{font-family:'Segoe UI',Arial,sans-serif;font-size:13px;" +
                "color:#1F2937;padding:8px 0;line-height:1.75;background:transparent;}" +
                "p{margin:0 0 10px;}" +
                "strong{font-weight:700;color:#111827;}" +
                ".section{font-weight:700;color:" + (passed ? "#166534" : "#92400E") + ";margin:12px 0 4px;font-size:13px;}" +
                "ul{margin:4px 0 10px 18px;}" +
                "li{margin:3px 0;}" +
                "</style></head><body>" +
                cleanFeedback
                        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                        .replaceAll("POINTS POSITIFS:", "<div class='section'>✅ Points positifs</div>")
                        .replaceAll("POINTS À AMÉLIORER:", "<div class='section'>📌 Points à améliorer</div>")
                        .replaceAll("CONCLUSION:", "<div class='section'>📝 Conclusion</div>")
                        .replaceAll("(?m)^- (.+)$", "<li>$1</li>")
                        .replace("\n", "<br>")
                + "</body></html>";

        int lines = feedback.split("\n").length;
        WebView feedWv = new WebView();
        feedWv.setPrefHeight(Math.min(500, Math.max(140, lines * 21 + 40)));
        feedWv.setContextMenuEnabled(false);
        // Charger en base64 pour garantir l'encodage UTF-8 (fix emojis)
        String b64feed = java.util.Base64.getEncoder().encodeToString(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        feedWv.getEngine().load("data:text/html;charset=utf-8;base64," + b64feed);

        // Bouton copier
        // Barre de traduction du feedback
        HBox translateFeedBar = buildTranslateBar(cleanFeedback, null, feedWv, "feedback");

        Button btnCopy = new Button("📋  Copier le feedback");
        btnCopy.setStyle("-fx-background-color:white;-fx-text-fill:#4F46E5;-fx-font-size:11;" +
                "-fx-font-weight:bold;-fx-padding:6 16;-fx-background-radius:8;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
        btnCopy.setOnAction(ev -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(feedback);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            btnCopy.setText("✓  Copié !");
        });
        HBox copyRow = new HBox(btnCopy); copyRow.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(feedTitle, translateFeedBar, feedWv, copyRow);
        container.getChildren().add(card);
    }

    private void showInlineError(VBox container, String msg) {
        container.getChildren().clear();
        HBox err = new HBox(10); err.setAlignment(Pos.CENTER_LEFT);
        err.setPadding(new Insets(12, 16, 12, 16));
        err.setStyle("-fx-background-color:#FFF1F2;-fx-background-radius:12;" +
                "-fx-border-color:#FECDD3;-fx-border-width:1.5;-fx-border-radius:12;" +
                "-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.1),6,0,0,2);");
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size:12;-fx-text-fill:#BE123C;-fx-font-weight:bold;");
        err.getChildren().add(lbl);
        container.getChildren().add(err);
    }

    // ── Grok correction ─────────────────────────────────────────────────
    private String callGrokCorrection(String instructions, String reponse) {
        String prompt =
                "Tu es un formateur expert et bienveillant. Évalue précisément cette réponse d'exercice.\n\n" +
                        "═══ INSTRUCTIONS DE L'EXERCICE ═══\n" + instructions + "\n\n" +
                        "═══ RÉPONSE DE L'APPRENANT ═══\n" + reponse + "\n\n" +
                        "RÉPONDS EXACTEMENT dans ce format (en français) :\n\n" +
                        "NOTE: [nombre entier entre 0 et 100]\n\n" +
                        "POINTS POSITIFS:\n" +
                        "- [ce qui est bien fait, spécifique]\n" +
                        "- [autre point fort]\n\n" +
                        "POINTS À AMÉLIORER:\n" +
                        "- [ce qui manque ou est incorrect, spécifique]\n" +
                        "- [suggestion concrète]\n\n" +
                        "CONCLUSION:\n" +
                        "[2-3 phrases sur la qualité globale et comment progresser]";

        String result = callGeminiAPI(prompt);
        return result != null ? result : buildLocalFeedback(reponse);
    }

    private int extractScore(String feedback) {
        if (feedback == null) return -1;
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("NOTE:\\s*(\\d{1,3})").matcher(feedback);
            if (m.find()) return Math.min(100, Math.max(0, Integer.parseInt(m.group(1))));
        } catch (Exception ignored) {}
        return -1;
    }

    private String buildLocalFeedback(String reponse) {
        int len = reponse == null ? 0 : reponse.trim().length();
        int note;
        String pos, ameliorer, conclusion;
        if (len < 20) {
            note = 10; pos = "- Une tentative a été faite";
            ameliorer = "- Réponse trop courte\n- Développez en suivant les instructions";
            conclusion = "Réponse insuffisante. Configurez groq.key pour une correction IA précise.";
        } else if (len < 100) {
            note = 35; pos = "- Des éléments de réponse sont présents";
            ameliorer = "- Trop succinct\n- Couvrez tous les points demandés";
            conclusion = "Réponse partielle. Ajoutez groq.key=gsk_... dans anthropic.properties.";
        } else if (len < 400) {
            note = 62; pos = "- Les points principaux sont abordés\n- L'effort est visible";
            ameliorer = "- Certains aspects méritent plus de développement";
            conclusion = "Réponse correcte mais incomplète. Activez Groq pour une correction précise.";
        } else {
            note = 80; pos = "- Réponse complète et bien développée\n- Les instructions semblent suivies";
            ameliorer = "- Relisez les instructions pour ne rien oublier";
            conclusion = "Bonne réponse. Activez Groq pour une correction IA précise et personnalisée.";
        }
        return "NOTE: " + note + "\n\n" +
                "POINTS POSITIFS:\n" + pos + "\n\n" +
                "POINTS À AMÉLIORER:\n" + ameliorer + "\n\n" +
                "CONCLUSION:\n" + conclusion +
                "\n\n⚠️ [Correction automatique — ajoutez grok.key= dans config.properties]";
    }

    // ── DB — Sauvegarde et chargement ──────────────────────────────────────

    /**
     * Résout l'ID utilisateur (table utilisateur) correspondant à cet employé.
     * La FK fk_exsoum_utilisateur référence utilisateur.id, pas employe.id.
     * Essaie plusieurs stratégies pour trouver le bon ID.
     */

    private int saveSubmissionToDB(String contenu) {
        int uid = getEmployeId(); // utilisateur.id == employe_id dans toute la DB


        // Tentative 1 : schéma réel DB (sans inscription_id)
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO exercice_soumission " +
                            "(employe_id, module_id, contenu_rendu, statut, date_soumission) " +
                            "VALUES (?,?,?,'soumis',NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, uid);
            ps.setInt(2, moduleId);
            ps.setString(3, contenu);
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) {
                int sid = k.getInt(1);
                System.out.println("✓ Soumission sauvegardée id=" + sid);
                return sid;
            }
        } catch (SQLException e) {
            System.err.println("saveSubmission (tentative 1): " + e.getMessage());
        }

        // Tentative 2 : avec inscription_id (schéma alternatif)
        try {
            PreparedStatement ps2 = connection.prepareStatement(
                    "INSERT INTO exercice_soumission " +
                            "(employe_id, module_id, inscription_id, contenu_rendu, statut, date_soumission) " +
                            "VALUES (?,?,?,?,'soumis',NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            ps2.setInt(1, uid);
            ps2.setInt(2, moduleId);
            ps2.setInt(3, inscriptionId);
            ps2.setString(4, contenu);
            ps2.executeUpdate();
            ResultSet k = ps2.getGeneratedKeys();
            if (k.next()) {
                int sid = k.getInt(1);
                System.out.println("✓ Soumission sauvegardée (avec inscription) id=" + sid);
                return sid;
            }
        } catch (SQLException e2) {
            System.err.println("saveSubmission (tentative 2): " + e2.getMessage());
        }

        // Tentative 3 : INSERT IGNORE
        try {
            PreparedStatement ps3 = connection.prepareStatement(
                    "INSERT IGNORE INTO exercice_soumission " +
                            "(employe_id, module_id, contenu_rendu, statut, date_soumission) " +
                            "VALUES (?,?,?,'soumis',NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            ps3.setInt(1, uid);
            ps3.setInt(2, moduleId);
            ps3.setString(3, contenu);
            ps3.executeUpdate();
            ResultSet k = ps3.getGeneratedKeys();
            if (k.next() && k.getInt(1) > 0) {
                System.out.println("✓ Soumission sauvegardée (INSERT IGNORE) id=" + k.getInt(1));
                return k.getInt(1);
            }
        } catch (SQLException e3) {
            System.err.println("saveSubmission (tentative 3 INSERT IGNORE): " + e3.getMessage());
        }

        System.err.println("[saveSubmission] Impossible de sauvegarder — la correction IA continue quand même");
        return -1;
    }

    private void createExerciceTable() {
        try {
            connection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS exercice_soumission (" +
                            "  id INT AUTO_INCREMENT PRIMARY KEY," +
                            "  employe_id INT NOT NULL," +
                            "  module_id INT NOT NULL," +
                            "  inscription_id INT DEFAULT NULL," +
                            "  contenu_rendu TEXT NOT NULL," +
                            "  note_ia INT DEFAULT NULL," +
                            "  feedback_ia TEXT DEFAULT NULL," +
                            "  statut VARCHAR(20) DEFAULT 'soumis'," +
                            "  date_soumission DATETIME DEFAULT CURRENT_TIMESTAMP," +
                            "  date_correction DATETIME DEFAULT NULL" +
                            ")");
            System.out.println("✓ Table exercice_soumission créée");
        } catch (SQLException e) {
            System.err.println("createExerciceTable: " + e.getMessage());
        }
    }

    private void updateSubmissionScore(int sid, int score, String feedback) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE exercice_soumission " +
                            "SET note_ia=?, feedback_ia=?, statut=?, date_correction=NOW() WHERE id=?");
            if (score >= 0) ps.setInt(1, score); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, feedback);
            ps.setString(3, score >= 0 ? "corrige" : "soumis");
            ps.setInt(4, sid);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("updateScore: " + e.getMessage()); }
    }

    private ExerciceSubmission loadLastSubmission() {
        try {
            int uid = getEmployeId(); // utilisateur.id == employe_id dans toute la DB
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT contenu_rendu, note_ia, feedback_ia, date_soumission " +
                            "FROM exercice_soumission " +
                            "WHERE employe_id=? AND module_id=? " +
                            "ORDER BY date_soumission DESC LIMIT 1");
            ps.setInt(1, uid); ps.setInt(2, moduleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ExerciceSubmission s = new ExerciceSubmission();
                s.contenu  = rs.getString("contenu_rendu");
                s.noteIa   = rs.getObject("note_ia") != null ? rs.getInt("note_ia") : -1;
                s.feedbackIa = rs.getString("feedback_ia");
                s.dateSoumission = rs.getString("date_soumission");
                return s;
            }
        } catch (SQLException e) { System.err.println("loadLastSubmission: " + e.getMessage()); }
        return null;
    }

    private void showSubmissionHistory() {
        List<ExerciceSubmission> history = new ArrayList<>();
        try {
            int uid = getEmployeId(); // utilisateur.id == employe_id dans toute la DB
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT contenu_rendu, note_ia, feedback_ia, date_soumission " +
                            "FROM exercice_soumission WHERE employe_id=? AND module_id=? " +
                            "ORDER BY date_soumission DESC LIMIT 10");
            ps.setInt(1, uid); ps.setInt(2, moduleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ExerciceSubmission s = new ExerciceSubmission();
                s.contenu = rs.getString("contenu_rendu");
                s.noteIa  = rs.getObject("note_ia") != null ? rs.getInt("note_ia") : -1;
                s.feedbackIa = rs.getString("feedback_ia");
                s.dateSoumission = rs.getString("date_soumission");
                history.add(s);
            }
        } catch (SQLException e) { System.err.println("history: " + e.getMessage()); }

        if (history.isEmpty()) { showInfo("Historique", "Aucune soumission pour ce module."); return; }

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("📜  Historique des soumissions — " + moduleTitle);
        dlg.getDialogPane().setPrefWidth(680);
        dlg.getDialogPane().setPrefHeight(580);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        for (int i = 0; i < history.size(); i++) {
            ExerciceSubmission s = history.get(i);
            boolean passed = s.noteIa >= 70;

            VBox c = new VBox(8);
            c.setPadding(new Insets(14, 16, 14, 16));
            c.setStyle("-fx-background-color:" + (s.noteIa >= 0 ? (passed ? "#F0FDF4" : "#FFF7ED") : "#EFF6FF") + ";" +
                    "-fx-background-radius:12;-fx-border-color:" +
                    (s.noteIa >= 0 ? (passed ? "#86EFAC" : "#FDE68A") : "#BFDBFE") +
                    ";-fx-border-width:1;-fx-border-radius:12;");

            HBox hdr = new HBox(12); hdr.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label("#" + (i+1));
            num.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#374151;");
            Label dateL = new Label("🕐  " + s.dateSoumission);
            dateL.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label scoreL = new Label(s.noteIa >= 0 ? "📊 " + s.noteIa + "/100" : "⏳ Non corrigé");
            scoreL.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:" +
                    (s.noteIa >= 0 ? (passed ? "#059669" : "#D97706") : "#6B7280") + ";");
            hdr.getChildren().addAll(num, dateL, sp, scoreL);

            // Extrait de la réponse
            String preview = s.contenu != null && s.contenu.length() > 120
                    ? s.contenu.substring(0, 120) + "…" : s.contenu;
            Label prevLbl = new Label(preview);
            prevLbl.setStyle("-fx-font-size:11;-fx-text-fill:#6B7280;-fx-font-style:italic;");
            prevLbl.setWrapText(true);

            c.getChildren().addAll(hdr, prevLbl);
            content.getChildren().add(c);
        }

        ScrollPane sp2 = new ScrollPane(content); sp2.setFitToWidth(true);
        sp2.setStyle("-fx-background:white;-fx-background-color:white;");
        dlg.getDialogPane().setContent(sp2);
        dlg.showAndWait();
    }

    // ── Inner class ───────────────────────────────────────────────────────
    private static class ExerciceSubmission {
        String contenu, feedbackIa, dateSoumission;
        int noteIa = -1;
    }

    private void renderGeneric() {
        if (courseContent == null) return;
        courseContent.getChildren().clear();
        Label lbl = new Label(contenuCours != null ? contenuCours : "Contenu du module");
        lbl.setStyle("-fx-font-size:14;-fx-text-fill:#374151;-fx-wrap-text:true;");
        lbl.setWrapText(true);
        courseContent.getChildren().add(lbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NOTES MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════

    private void setupNotesSaving() {
        if (btnSaveNote != null) {
            btnSaveNote.setOnAction(e -> saveNote());
        }
    }

    @FXML
    private void saveNote() {
        if (txtNotes == null) return;
        String noteText = txtNotes.getText().trim();
        if (noteText.isEmpty()) return;

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO module_notes (module_id, inscription_id, note_text, created_at) VALUES (?,?,?,?)");
            ps.setInt(1, moduleId);
            ps.setInt(2, inscriptionId);
            ps.setString(3, noteText);
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            txtNotes.clear();
            loadExistingNotes();

        } catch (SQLException e) {
            // Table module_notes n'existe pas — affichage local uniquement
            System.err.println("saveNote (table absente, mode local): " + e.getMessage());
            addNoteLocally(noteText);
            txtNotes.clear();
        }
    }

    private final java.util.List<String[]> localNotes = new java.util.ArrayList<>();

    private void addNoteLocally(String text) {
        localNotes.add(0, new String[]{text, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))});
        refreshLocalNotes();
    }

    private void refreshLocalNotes() {
        if (notesHistory == null) return;
        notesHistory.getChildren().clear();
        for (String[] n : localNotes) {
            notesHistory.getChildren().add(createNoteCard(n[0], n[1]));
        }
    }

    private javafx.scene.layout.VBox createNoteCard(String text, String dateStr) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(8);
        card.setStyle(
                "-fx-background-color:white;-fx-padding:13 14;-fx-background-radius:12;" +
                        "-fx-border-color:#E0E7FF;-fx-border-width:1.5;-fx-border-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.07),6,0,0,2);");
        // Badge date
        HBox topRow = new HBox(6); topRow.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label("📌"); ico.setStyle("-fx-font-size:11;");
        Label date = new Label(dateStr);
        date.setStyle("-fx-font-size:10;-fx-text-fill:#6366F1;-fx-font-weight:bold;");
        topRow.getChildren().addAll(ico, date);
        // Texte
        Label content = new Label(text);
        content.setStyle("-fx-font-size:13;-fx-text-fill:#374151;-fx-wrap-text:true;-fx-line-spacing:2;");
        content.setWrapText(true);
        card.getChildren().addAll(topRow, content);
        return card;
    }

    private void loadExistingNotes() {
        if (notesHistory == null) return;
        notesHistory.getChildren().clear();

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT note_text, created_at FROM module_notes WHERE module_id=? AND inscription_id=? ORDER BY created_at DESC");
            ps.setInt(1, moduleId);
            ps.setInt(2, inscriptionId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String dateStr = rs.getTimestamp("created_at").toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
                notesHistory.getChildren().add(createNoteCard(rs.getString("note_text"), dateStr));
            }
        } catch (SQLException e) {
            // Table absente — afficher les notes locales
            System.err.println("loadExistingNotes (table absente): " + e.getMessage());
            refreshLocalNotes();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HIGHLIGHTS PERSISTENCE
    // ══════════════════════════════════════════════════════════════════════

    private void loadExistingHighlights() {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT text_key, color FROM module_highlights WHERE module_id=? AND inscription_id=?");
            ps.setInt(1, moduleId);
            ps.setInt(2, inscriptionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                highlights.put(rs.getString("text_key"), rs.getString("color"));
            }
        } catch (SQLException e) {
            System.err.println("loadExistingHighlights (table absente, mode local): " + e.getMessage());
        }
    }

    private void saveHighlight(String textKey, String color) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO module_highlights (module_id, inscription_id, text_key, color) VALUES (?,?,?,?) " +
                            "ON DUPLICATE KEY UPDATE color=?");
            ps.setInt(1, moduleId);
            ps.setInt(2, inscriptionId);
            ps.setString(3, textKey);
            ps.setString(4, color);
            ps.setString(5, color);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("saveHighlight (table absente, mode local): " + e.getMessage());
        }
    }

    private void deleteHighlight(String textKey) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM module_highlights WHERE module_id=? AND inscription_id=? AND text_key=?");
            ps.setInt(1, moduleId);
            ps.setInt(2, inscriptionId);
            ps.setString(3, textKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteHighlight (table absente, mode local): " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PROGRESS TRACKING
    // ══════════════════════════════════════════════════════════════════════

    private void loadProgress() {
        try {
            int uid = getEmployeId(); // utilisateur.id — identique à employe_id dans toute la DB
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT statut FROM module_progression WHERE module_id=? AND employe_id=?");
            ps.setInt(1, moduleId);
            ps.setInt(2, uid);
            ResultSet rs = ps.executeQuery();
            int progress = 0;
            if (rs.next()) {
                String statut = rs.getString("statut");
                if ("completed".equalsIgnoreCase(statut)) {
                    progress    = 100;
                    isCompleted = true;
                } else if ("in_progress".equalsIgnoreCase(statut)) {
                    progress = 50;
                }
            }
            updateProgressUI(progress);
            // Synchro bouton Terminer si déjà complété
            if (isCompleted && btnMarkComplete != null) {
                btnMarkComplete.setText("✓ Terminé !");
                btnMarkComplete.setDisable(true);
                btnMarkComplete.setStyle("-fx-background-color:linear-gradient(to right,#10B981,#059669);" +
                        "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:9 22;" +
                        "-fx-background-radius:12;-fx-cursor:default;" +
                        "-fx-effect:dropshadow(gaussian,rgba(16,185,129,0.35),10,0,0,3);");
            }
            updateNavButtons();
        } catch (SQLException e) {
            System.err.println("loadProgress: " + e.getMessage());
            updateProgressUI(0);
            updateNavButtons();
        }
    }

    private void updateProgressUI(int progress) {
        if (progressBar != null) progressBar.setProgress(progress / 100.0);
        if (lblProgress != null) lblProgress.setText(progress + " % complété"
                + (progress >= 100 ? " ✓" : ""));
    }

    @FXML
    private void handleMarkComplete() {
        markModuleCompleted();
        isCompleted = true;          // ← déverrouille le bouton Suivant
        updateProgressUI(100);
        if (btnMarkComplete != null) {
            btnMarkComplete.setText("✓ Terminé !");
            btnMarkComplete.setDisable(true);
            btnMarkComplete.setStyle("-fx-background-color:#10B981;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-font-size:13;-fx-padding:9 22;" +
                    "-fx-background-radius:10;-fx-cursor:default;");
        }
        Platform.runLater(() -> {
            if (lblProgress != null) lblProgress.setText("100 % complété ✓");
            updateNavButtons(); // rafraîchit Suivant
        });
    }

    private void markModuleCompleted() {
        int uid = getEmployeId(); // utilisateur.id — identique à employe_id dans toute la DB
        System.out.println("[markComplete] employe_id=" + uid);

        // Tentative 1 : avec date_completion
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO module_progression (module_id, employe_id, statut, date_completion) " +
                            "VALUES (?,?,'completed',?) " +
                            "ON DUPLICATE KEY UPDATE statut='completed', date_completion=?");
            java.sql.Timestamp now = java.sql.Timestamp.valueOf(LocalDateTime.now());
            ps.setInt(1, moduleId);
            ps.setInt(2, uid);
            ps.setTimestamp(3, now);
            ps.setTimestamp(4, now);
            ps.executeUpdate();
            System.out.println("✓ Module progression saved (avec date)");
            return;
        } catch (SQLException e) {
            System.err.println("markModuleCompleted (tentative 1): " + e.getMessage());
        }

        // Tentative 2 : sans date_completion
        try {
            PreparedStatement ps2 = connection.prepareStatement(
                    "INSERT INTO module_progression (module_id, employe_id, statut) " +
                            "VALUES (?,?,'completed') " +
                            "ON DUPLICATE KEY UPDATE statut='completed'");
            ps2.setInt(1, moduleId);
            ps2.setInt(2, uid);
            ps2.executeUpdate();
            System.out.println("✓ Module progression saved (sans date)");
        } catch (SQLException e2) {
            System.err.println("markModuleCompleted (tentative 2): " + e2.getMessage());
        }
    }

    /** Retourne l'employe_id directement (passé par ModulesViewController) */
    private int getEmployeId() {
        return employeId > 0 ? employeId : 1;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GROK AI — Résumé / Explication / Quiz
    // ══════════════════════════════════════════════════════════════════════

    @FXML private void handleAISummary()            { callGrokAction("summary");   }
    @FXML private void handleAIExplain()            { callGrokAction("explain");   }
    @FXML private void handleAIGenerateQuestions()  { callGrokAction("quiz");      }

    private void callGrokAction(String action) {
        if ((groqApiKey == null || groqApiKey.isBlank()) && (grokApiKey == null || grokApiKey.isBlank())) {
            showAIMessage("⚠️ Clé IA manquante.\n\nAjoutez dans anthropic.properties :\ngroq.key=gsk_...\n\nClé gratuite : console.groq.com", "#FEF3C7", "#92400E");
            return;
        }
        String context = buildContext();
        if (context.isBlank()) {
            showAIMessage("⚠️ Ce module n'a pas de contenu textuel à analyser.", "#FEF3C7", "#92400E");
            return;
        }

        String prompt;
        String btnLabel;
        switch (action) {
            case "summary":
                prompt = "Tu es un assistant pédagogique. Fais un résumé clair et structuré du cours suivant en français.\n" +
                        "Format : points clés avec emoji, max 200 mots.\n\nCOURS :\n" + context;
                btnLabel = "⏳  Résumé en cours…";
                break;
            case "explain":
                prompt = "Explique le contenu suivant de façon très simple, comme si tu l'expliquais à un débutant complet.\n" +
                        "Utilise des analogies et exemples concrets. Réponds en français, max 250 mots.\n\nCONTENU :\n" + context;
                btnLabel = "⏳  Explication en cours…";
                break;
            case "quiz":
                prompt = "Génère 5 questions de compréhension (QCM) basées sur ce cours, avec 4 choix chacune et la bonne réponse indiquée.\n" +
                        "Format : Q1. [question] \\na) [choix] b) [choix] c) [choix] d) [choix] \\n✅ Réponse: [lettre]\n" +
                        "Réponds en français.\n\nCOURS :\n" + context;
                btnLabel = "⏳  Génération en cours…";
                break;
            default:
                return;
        }

        // Désactiver boutons pendant l'appel
        setAIButtonsDisabled(true, btnLabel, action);
        if (lblAIStatus != null) lblAIStatus.setText("🤖 IA en cours…");

        new Thread(() -> {
            String result = callGeminiAPI(prompt);
            Platform.runLater(() -> {
                setAIButtonsDisabled(false, null, null);
                if (lblAIStatus != null) lblAIStatus.setText("✓ Réponse reçue");
                if (result != null) showAIMessage(result, "#EEF2FF", "#1E1B4B");
                else showAIMessage("❌ Erreur lors de l'appel Gemini. Vérifiez votre clé API.", "#FEF2F2", "#991B1B");
            });
        }, "grok-thread").start();
    }

    private String buildContext() {
        StringBuilder sb = new StringBuilder();
        if (moduleTitle != null) sb.append("Module : ").append(moduleTitle).append("\n\n");
        if (contenuCours != null && !contenuCours.isBlank())
            sb.append(contenuCours.substring(0, Math.min(3000, contenuCours.length())));
        return sb.toString().trim();
    }

    private void setAIButtonsDisabled(boolean disabled, String loadingLabel, String action) {
        if (btnAISummary  != null) { btnAISummary.setDisable(disabled);  if (disabled && "summary".equals(action)) btnAISummary.setText(loadingLabel); else if (!disabled) btnAISummary.setText("✨  Résumer ce module"); }
        if (btnAIExplain  != null) { btnAIExplain.setDisable(disabled);  if (disabled && "explain".equals(action)) btnAIExplain.setText(loadingLabel); else if (!disabled) btnAIExplain.setText("💡  Expliquer simplement"); }
        if (btnAIQuiz     != null) { btnAIQuiz.setDisable(disabled);     if (disabled && "quiz".equals(action))    btnAIQuiz.setText(loadingLabel);    else if (!disabled) btnAIQuiz.setText("❓  Générer des questions"); }
    }

    private void showAIMessage(String text, String bgColor, String textColor) {
        if (aiResponseContainer == null) return;
        aiResponseContainer.getChildren().clear();
        aiResponseContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(aiResponseContainer, Priority.ALWAYS);

        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(card, Priority.ALWAYS);
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#E0E7FF;-fx-border-width:1.5;-fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.1),10,0,0,3);");

        // Label type
        Label lbl = new Label("🤖  Réponse de l'IA");
        lbl.setStyle("-fx-font-size:10.5;-fx-font-weight:bold;-fx-text-fill:#6366F1;" +
                "-fx-background-color:#EEF2FF;-fx-padding:3 10;-fx-background-radius:20;");

        String htmlBody1 = text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\n", "<br>")
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'><style>" +
                "*{margin:0;padding:0;box-sizing:border-box;}" +
                "body{font-family:'Segoe UI','Helvetica Neue',Arial,sans-serif;font-size:14.5px;color:#1E293B;" +
                "  line-height:1.9;padding:12px 10px;background:transparent;word-break:break-word;}" +
                "p{margin:0 0 12px;}" +
                "strong{font-weight:700;color:#1E1B4B;}" +
                "li{margin:4px 0 4px 18px;display:list-item;}" +
                "br{display:block;margin:2px 0;}" +
                ".kv{display:flex;gap:8px;margin:4px 0;}" +
                ".emoji{font-size:16px;line-height:1.4;}" +
                "</style></head><body>" + htmlBody1 + "</body></html>";

        WebView wv = new WebView();
        int lines = text.split("\n").length;
        wv.setPrefHeight(Math.min(700, Math.max(180, lines * 22 + 60)));
        wv.setMaxWidth(Double.MAX_VALUE);
        wv.setContextMenuEnabled(false);
        // Lier la largeur du WebView à celle de la sidebar (redimensionnable)
        if (sidebarPanel != null) {
            wv.prefWidthProperty().bind(sidebarPanel.widthProperty().subtract(40));
        }
        // Charger en base64 pour garantir l'encodage UTF-8 (fix emojis)
        String b64ai = java.util.Base64.getEncoder().encodeToString(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        wv.getEngine().load("data:text/html;charset=utf-8;base64," + b64ai);

        Button btnCopy = new Button("📋  Copier");
        btnCopy.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6366F1;-fx-font-size:11;" +
                "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:8;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
        btnCopy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(text);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            btnCopy.setText("✓  Copié !");
        });

        Button btnExpand = new Button("⛶  Agrandir");
        btnExpand.setStyle("-fx-background-color:#6366F1;-fx-text-fill:white;-fx-font-size:11;" +
                "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.25),6,0,0,2);");
        btnExpand.setOnMouseEntered(e -> btnExpand.setStyle(
                "-fx-background-color:#4F46E5;-fx-text-fill:white;-fx-font-size:11;" +
                        "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.4),8,0,0,3);"));
        btnExpand.setOnMouseExited(e -> btnExpand.setStyle(
                "-fx-background-color:#6366F1;-fx-text-fill:white;-fx-font-size:11;" +
                        "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.25),6,0,0,2);"));
        btnExpand.setOnAction(e -> showAIExpandedDialog(text, html));

        HBox btnRow = new HBox(8, btnExpand, btnCopy);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().addAll(lbl, wv, btnRow);
        aiResponseContainer.getChildren().add(card);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DIALOG PLEIN ÉCRAN — Réponse IA agrandie
    // ══════════════════════════════════════════════════════════════════════
    private void showAIExpandedDialog(String rawText, String html) {
        Stage dialog = new Stage();
        dialog.setTitle("🤖  Assistant IA — " + (moduleTitle != null ? moduleTitle : "Module"));
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (stage != null) dialog.initOwner(stage);

        // ── Header ────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color:linear-gradient(to right,#6366F1,#8B5CF6);");

        Label ico = new Label("🤖");
        ico.setStyle("-fx-font-size:22;");
        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLbl = new Label("Assistant IA");
        titleLbl.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:white;");
        Label subtitleLbl = new Label(moduleTitle != null ? moduleTitle : "Réponse complète");
        subtitleLbl.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.75);");
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);

        // Boutons header
        Button btnCopyH = new Button("📋  Copier");
        btnCopyH.setStyle("-fx-background-color:rgba(255,255,255,0.2);-fx-text-fill:white;" +
                "-fx-font-size:12;-fx-font-weight:bold;-fx-padding:7 16;-fx-background-radius:8;" +
                "-fx-border-color:rgba(255,255,255,0.35);-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
        btnCopyH.setOnAction(ev -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(rawText);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            btnCopyH.setText("✓  Copié !");
        });

        Button btnClose2 = new Button("✕");
        btnClose2.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;" +
                "-fx-font-size:13;-fx-font-weight:bold;-fx-padding:6 12;-fx-background-radius:8;" +
                "-fx-cursor:hand;");
        btnClose2.setOnAction(ev -> dialog.close());

        header.getChildren().addAll(ico, titleBox, btnCopyH, btnClose2);

        // ── WebView plein écran ────────────────────────────────────────────
        // Reconstruire le HTML avec une plus grande police pour la dialog
        String bigHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'><style>" +
                "*{margin:0;padding:0;box-sizing:border-box;}" +
                "body{font-family:'Segoe UI','Helvetica Neue',Arial,sans-serif;font-size:16px;color:#1E293B;" +
                "  line-height:2.0;padding:28px 36px;background:#FAFBFF;word-break:break-word;}" +
                "p{margin:0 0 14px;}" +
                "strong{font-weight:700;color:#1E1B4B;}" +
                "li{margin:6px 0 6px 22px;display:list-item;}" +
                "br{display:block;margin:3px 0;}" +
                "h1,h2,h3{color:#4F46E5;margin:20px 0 10px;}" +
                "h2{font-size:18px;border-bottom:2px solid #E0E7FF;padding-bottom:6px;}" +
                "h3{font-size:15px;}" +
                "blockquote{background:#EEF2FF;border-left:4px solid #6366F1;" +
                "  padding:12px 16px;margin:14px 0;border-radius:0 8px 8px 0;color:#3730A3;}" +
                "</style></head><body>" +
                rawText.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                        .replace("\n","<br>")
                        .replaceAll("\\*\\*(.+?)\\*\\*","<strong>$1</strong>")
                        .replaceAll("#{3}\s(.+?)(<br>|$)","<h3>$1</h3>")
                        .replaceAll("#{2}\s(.+?)(<br>|$)","<h2>$1</h2>")
                        .replaceAll("#\s(.+?)(<br>|$)","<h1>$1</h1>")
                + "</body></html>";

        WebView bigWv = new WebView();
        bigWv.setContextMenuEnabled(false);
        VBox.setVgrow(bigWv, Priority.ALWAYS);
        String b64big = java.util.Base64.getEncoder()
                .encodeToString(bigHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        bigWv.getEngine().load("data:text/html;charset=utf-8;base64," + b64big);

        // ── Barre de contrôle taille police ──────────────────────────────
        HBox sizeBar = new HBox(10);
        sizeBar.setAlignment(Pos.CENTER_LEFT);
        sizeBar.setPadding(new Insets(10, 20, 10, 20));
        sizeBar.setStyle("-fx-background-color:#F8F9FF;-fx-border-color:#E0E7FF;" +
                "-fx-border-width:0 0 1 0;");

        Label sizeLbl = new Label("Taille :");
        sizeLbl.setStyle("-fx-font-size:12;-fx-text-fill:#6B7280;");

        final int[] currentSize = {16};

        Button btnSmall  = makeSizeBtn("A",  13, currentSize, bigWv, rawText);
        Button btnMedium = makeSizeBtn("A",  16, currentSize, bigWv, rawText);
        Button btnLarge  = makeSizeBtn("A",  20, currentSize, bigWv, rawText);
        Button btnXLarge = makeSizeBtn("A",  24, currentSize, bigWv, rawText);
        btnSmall.setStyle(btnSmall.getStyle()   + "-fx-font-size:11;");
        btnMedium.setStyle(btnMedium.getStyle() + "-fx-font-size:13;");
        btnLarge.setStyle(btnLarge.getStyle()   + "-fx-font-size:15;");
        btnXLarge.setStyle(btnXLarge.getStyle() + "-fx-font-size:18;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label countLbl = new Label(rawText.split("\n").length + " lignes  ·  " +
                rawText.length() + " caractères");
        countLbl.setStyle("-fx-font-size:11;-fx-text-fill:#9CA3AF;-fx-font-style:italic;");

        sizeBar.getChildren().addAll(sizeLbl, btnSmall, btnMedium, btnLarge, btnXLarge,
                spacer, countLbl);

        // ── Layout principal ──────────────────────────────────────────────
        VBox root = new VBox(0, header, sizeBar, bigWv);
        VBox.setVgrow(bigWv, Priority.ALWAYS);
        root.setStyle("-fx-background-color:#FAFBFF;");

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 860, 640);
        dialog.setScene(scene);
        dialog.setMinWidth(500);
        dialog.setMinHeight(400);
        dialog.show();
    }

    private Button makeSizeBtn(String label, int size, int[] currentSize,
                               WebView wv, String rawText) {
        Button btn = new Button(label);
        String base = "-fx-background-color:#EEF2FF;-fx-text-fill:#6366F1;-fx-font-weight:bold;" +
                "-fx-padding:4 10;-fx-background-radius:6;-fx-cursor:hand;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:6;";
        btn.setStyle(base);
        btn.setOnAction(e -> {
            currentSize[0] = size;
            String newHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                    "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'><style>" +
                    "*{margin:0;padding:0;box-sizing:border-box;}" +
                    "body{font-family:'Segoe UI','Helvetica Neue',Arial,sans-serif;" +
                    "  font-size:" + size + "px;color:#1E293B;" +
                    "  line-height:2.0;padding:28px 36px;background:#FAFBFF;word-break:break-word;}" +
                    "p{margin:0 0 14px;}" +
                    "strong{font-weight:700;color:#1E1B4B;}" +
                    "li{margin:6px 0 6px 22px;display:list-item;}" +
                    "br{display:block;margin:3px 0;}" +
                    "h1,h2,h3{color:#4F46E5;margin:20px 0 10px;}" +
                    "h2{font-size:" + (size + 2) + "px;border-bottom:2px solid #E0E7FF;padding-bottom:6px;}" +
                    "h3{font-size:" + (size + 1) + "px;}" +
                    "blockquote{background:#EEF2FF;border-left:4px solid #6366F1;" +
                    "  padding:12px 16px;margin:14px 0;border-radius:0 8px 8px 0;color:#3730A3;}" +
                    "</style></head><body>" +
                    rawText.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                            .replace("\n","<br>")
                            .replaceAll("\\*\\*(.+?)\\*\\*","<strong>$1</strong>")
                            .replaceAll("#{3}\s(.+?)(<br>|$)","<h3>$1</h3>")
                            .replaceAll("#{2}\s(.+?)(<br>|$)","<h2>$1</h2>")
                            .replaceAll("#\s(.+?)(<br>|$)","<h1>$1</h1>")
                    + "</body></html>";
            String b64 = java.util.Base64.getEncoder()
                    .encodeToString(newHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            wv.getEngine().load("data:text/html;charset=utf-8;base64," + b64);
        });
        btn.setOnMouseEntered(ev -> btn.setStyle(base.replace("#EEF2FF","#6366F1")
                .replace("#6366F1;-fx-font-weight","white;-fx-font-weight")));
        btn.setOnMouseExited(ev -> btn.setStyle(base));
        return btn;
    }

    /**
     * Groq API (principal) + Grok xAI (fallback) — compatible OpenAI Chat Completions.
     * Garde le nom callGeminiAPI pour compatibilité avec tous les appels existants.
     */
    private String callGeminiAPI(String prompt) {
        // Choisir Groq (gratuit, rapide) ou Grok (fallback)
        String apiKey = (groqApiKey != null && !groqApiKey.isBlank()) ? groqApiKey : grokApiKey;
        String apiUrl = (groqApiKey != null && !groqApiKey.isBlank()) ? GROQ_URL : GROK_URL;
        String model  = (groqApiKey != null && !groqApiKey.isBlank()) ? "llama-3.3-70b-versatile" : "grok-3-mini";

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[AI] Aucune clé API disponible (groq.key ou grok.key)");
            return null;
        }
        System.out.println("[AI] " + (groqApiKey != null && !groqApiKey.isBlank() ? "Groq" : "Grok") + " → " + model);
        try {
            String body = "{\"model\":\"" + model + "\"," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" + escapeJson(prompt) + "}]," +
                    "\"max_tokens\":1000,\"temperature\":0.7}";

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
                // Si Groq échoue → fallback Grok
                if (groqApiKey != null && !groqApiKey.isBlank() && grokApiKey != null && !grokApiKey.isBlank()) {
                    System.out.println("[AI] Groq KO → fallback Grok...");
                    return callGrokFallbackAI(prompt);
                }
                if (code == 429) return "⚠️ Quota IA dépassé. Attendez 1 minute.";
                return null;
            }

            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int idx = json.indexOf("\"content\":");
            if (idx < 0) return null;
            return parseJsonString(json, idx + 10);

        } catch (Exception e) {
            System.err.println("[AI] Exception: " + e.getMessage());
            if (groqApiKey != null && !groqApiKey.isBlank() && grokApiKey != null && !grokApiKey.isBlank()) {
                System.out.println("[AI] Groq exception → fallback Grok...");
                return callGrokFallbackAI(prompt);
            }
            return null;
        }
    }

    private String callGrokFallbackAI(String prompt) {
        try {
            String body = "{\"model\":\"grok-3-mini\"," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" + escapeJson(prompt) + "}]," +
                    "\"max_tokens\":1000,\"temperature\":0.7}";
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
            return parseJsonString(json, idx + 10);
        } catch (Exception e) { System.err.println("[Grok-fallback] " + e.getMessage()); return null; }
    }
    // ══════════════════════════════════════════════════════════════════════
    //  YOUTUBE DATA API v3 — Recherche vidéos liées au module
    // ══════════════════════════════════════════════════════════════════════

    @FXML private void handleSearchVideos() {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            if (lblVideoStatus != null)
                lblVideoStatus.setText("⚠️ Clé YouTube manquante.\nAjoutez youtube.key=... dans config.properties\n(console.cloud.google.com → YouTube Data API v3)");
            return;
        }

        String query = buildYouTubeQuery();
        if (lblVideoStatus != null) lblVideoStatus.setText("🔍 Recherche : " + query + "…");
        if (btnSearchVideos != null) { btnSearchVideos.setText("⏳ Recherche…"); btnSearchVideos.setDisable(true); }
        if (videoResultsContainer != null) videoResultsContainer.getChildren().clear();

        new Thread(() -> {
            List<YouTubeVideo> videos = searchYouTubeVideos(query);
            Platform.runLater(() -> {
                if (btnSearchVideos != null) { btnSearchVideos.setText("🔍  Chercher des vidéos"); btnSearchVideos.setDisable(false); }
                if (videos.isEmpty()) {
                    if (lblVideoStatus != null) lblVideoStatus.setText("Aucun résultat trouvé.");
                    return;
                }
                if (lblVideoStatus != null) lblVideoStatus.setText(videos.size() + " vidéos trouvées pour « " + query + " »");
                displayYouTubeResults(videos);
            });
        }, "youtube-search-thread").start();
    }

    private String buildYouTubeQuery() {
        // Construire une requête intelligente depuis le titre du module
        String q = moduleTitle != null ? moduleTitle : "formation professionnelle";
        // Nettoyer les mots trop génériques
        q = q.replaceAll("(?i)module|cours|formation|chapitre", "").trim();
        if (q.isBlank()) q = moduleTitle != null ? moduleTitle : "développement professionnel";
        return q + " tutoriel français";
    }

    private List<YouTubeVideo> searchYouTubeVideos(String query) {
        List<YouTubeVideo> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlStr = "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet&type=video&maxResults=8&relevanceLanguage=fr" +
                    "&videoEmbeddable=true&safeSearch=strict" +
                    "&q=" + encoded + "&key=" + youtubeApiKey;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            System.out.println("YouTube API → HTTP " + code);

            if (code != 200) {
                InputStream err = conn.getErrorStream();
                String errBody = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
                System.err.println("YouTube error: " + errBody.substring(0, Math.min(300, errBody.length())));
                return results;
            }

            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            results = parseYouTubeResults(json);

        } catch (Exception e) {
            System.err.println("YouTube search exception: " + e.getMessage());
        }
        return results;
    }

    private List<YouTubeVideo> parseYouTubeResults(String json) {
        List<YouTubeVideo> list = new ArrayList<>();
        try {
            // Parser les "items" du JSON YouTube
            int pos = 0;
            while (true) {
                int videoIdIdx = json.indexOf("\"videoId\":", pos);
                if (videoIdIdx < 0) break;
                String videoId = parseJsonString(json, videoIdIdx + 10);
                if (videoId == null || videoId.isBlank()) { pos = videoIdIdx + 10; continue; }

                // Titre
                int titleIdx = json.indexOf("\"title\":", videoIdIdx);
                String title = titleIdx >= 0 ? parseJsonString(json, titleIdx + 8) : "Sans titre";

                // Description
                int descIdx = json.indexOf("\"description\":", videoIdIdx);
                String desc = descIdx >= 0 ? parseJsonString(json, descIdx + 14) : "";
                if (desc != null && desc.length() > 120) desc = desc.substring(0, 120) + "…";

                // Channel
                int chanIdx = json.indexOf("\"channelTitle\":", videoIdIdx);
                String channel = chanIdx >= 0 ? parseJsonString(json, chanIdx + 15) : "";

                // Thumbnail
                int thumbIdx = json.indexOf("\"high\":", videoIdIdx);
                int urlIdx = thumbIdx >= 0 ? json.indexOf("\"url\":", thumbIdx) : -1;
                String thumb = urlIdx >= 0 ? parseJsonString(json, urlIdx + 6) : null;

                if (videoId != null && title != null) {
                    list.add(new YouTubeVideo(videoId, title, desc, channel, thumb));
                }
                pos = videoIdIdx + 10;
            }
        } catch (Exception e) {
            System.err.println("YouTube parse error: " + e.getMessage());
        }
        return list;
    }

    private void displayYouTubeResults(List<YouTubeVideo> videos) {
        if (videoResultsContainer == null) return;
        videoResultsContainer.getChildren().clear();

        for (YouTubeVideo v : videos) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
                    "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;" +
                    "-fx-cursor:hand;");

            // Thumbnail via WebView
            String thumbUrl = v.thumbnailUrl != null ? v.thumbnailUrl
                    : "https://img.youtube.com/vi/" + v.videoId + "/mqdefault.jpg";
            WebView thumb = new WebView();
            thumb.setPrefHeight(140);
            thumb.setContextMenuEnabled(false);
            thumb.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36");
            thumb.getEngine().loadContent(
                    "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;}" +
                            "html,body{width:100%;height:140px;background:#111;overflow:hidden;cursor:pointer;}" +
                            "img{width:100%;height:140px;object-fit:cover;display:block;}" +
                            ".overlay{position:absolute;inset:0;background:rgba(0,0,0,0.2);display:flex;align-items:center;justify-content:center;}" +
                            ".play{font-size:36px;opacity:.9;}" +
                            "</style></head><body>" +
                            "<div style='position:relative;'>" +
                            "<img src='" + thumbUrl + "'>" +
                            "<div class='overlay'><div class='play'>▶</div></div>" +
                            "</div></body></html>", "text/html");
            thumb.setOnMouseClicked(e -> openVideoInChromeApp("https://www.youtube.com/watch?v=" + v.videoId));

            // Titre
            Label titleLbl = new Label(v.title);
            titleLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#111827;-fx-wrap-text:true;");
            titleLbl.setWrapText(true);

            // Chaîne
            Label chanLbl = new Label("📺  " + v.channelTitle);
            chanLbl.setStyle("-fx-font-size:10;-fx-text-fill:#6B7280;");

            // Boutons
            HBox btns = new HBox(8);
            Button btnOpen = new Button("▶  Regarder");
            btnOpen.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-font-size:11;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;");
            btnOpen.setOnAction(e -> openVideoInChromeApp("https://www.youtube.com/watch?v=" + v.videoId));

            Button btnLink = new Button("🔗  Intégrer");
            btnLink.setStyle("-fx-background-color:#EDE9FE;-fx-text-fill:#5B21B6;-fx-font-weight:bold;" +
                    "-fx-font-size:11;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;");
            btnLink.setOnAction(e -> copyVideoLinkToClipboard(v));

            btns.getChildren().addAll(btnOpen, btnLink);
            card.getChildren().addAll(thumb, titleLbl, chanLbl, btns);

            // Hover
            card.setOnMouseEntered(evt -> card.setStyle("-fx-background-color:#FAFAFA;-fx-background-radius:12;" +
                    "-fx-border-color:#A5B4FC;-fx-border-width:1.5;-fx-border-radius:12;-fx-cursor:hand;"));
            card.setOnMouseExited(evt -> card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
                    "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:12;-fx-cursor:hand;"));

            videoResultsContainer.getChildren().add(card);
        }
    }

    private void copyVideoLinkToClipboard(YouTubeVideo v) {
        String link = "https://www.youtube.com/watch?v=" + v.videoId;
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(link);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        if (lblVideoStatus != null) lblVideoStatus.setText("✓ Lien copié : " + v.title);
    }

    private void openVideoInChromeApp(String url) {
        String[] chromePaths = {
                System.getenv("ProgramFiles")      + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("ProgramFiles(x86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("LOCALAPPDATA")      + "\\Google\\Chrome\\Application\\chrome.exe",
        };
        String[] edgePaths = {
                System.getenv("ProgramFiles")      + "\\Microsoft\\Edge\\Application\\msedge.exe",
                System.getenv("ProgramFiles(x86)") + "\\Microsoft\\Edge\\Application\\msedge.exe",
        };
        for (String path : chromePaths) {
            if (path != null && new java.io.File(path).exists()) {
                try { new ProcessBuilder(path, "--app=" + url, "--new-window").start(); return; }
                catch (Exception ignored) {}
            }
        }
        for (String path : edgePaths) {
            if (path != null && new java.io.File(path).exists()) {
                try { new ProcessBuilder(path, "--app=" + url, "--new-window").start(); return; }
                catch (Exception ignored) {}
            }
        }
        try { java.awt.Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) {}
    }

    // ── Utility: JSON helpers ─────────────────────────────────────────────
    private String parseJsonString(String json, int start) {
        // Skip whitespace
        while (start < json.length() && json.charAt(start) != '"') start++;
        if (start >= json.length()) return null;
        start++; // skip opening "
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i-1) != '\\')) break;
            if (c == '\\' && i+1 < json.length()) {
                char nx = json.charAt(i+1);
                if (nx == 'n')  { sb.append('\n'); i += 2; continue; }
                if (nx == '"')  { sb.append('"');  i += 2; continue; }
                if (nx == '\\') { sb.append('\\'); i += 2; continue; }
                if (nx == 't')  { sb.append('\t'); i += 2; continue; }
                if (nx == 'r')  { i += 2; continue; }
            }
            sb.append(c); i++;
        }
        return sb.toString();
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r").replace("\t","\\t") + "\"";
    }

    // ── Inner class ───────────────────────────────────────────────────────
    private static class YouTubeVideo {
        String videoId, title, description, channelTitle, thumbnailUrl;
        YouTubeVideo(String id, String t, String d, String ch, String th) {
            videoId=id; title=t; description=d; channelTitle=ch; thumbnailUrl=th;
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    //  QUIZ — Rendu intégré dans ModuleLearner
    // ══════════════════════════════════════════════════════════════════════

    // Inner class
    private static class QuizQuestion {
        int id; String question, optionA, optionB, optionC, optionD, bonneReponse;
        QuizQuestion(ResultSet rs) throws SQLException {
            id = rs.getInt("id"); question = rs.getString("question");
            optionA = rs.getString("option_a"); optionB = rs.getString("option_b");
            optionC = rs.getString("option_c"); optionD = rs.getString("option_d");
            bonneReponse = rs.getString("bonne_reponse");
        }
    }

    private List<QuizQuestion> loadQuizQuestions() {
        List<QuizQuestion> list = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM quiz_question WHERE module_id=? ORDER BY id");
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new QuizQuestion(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private void renderQuiz() {
        if (courseScrollPane == null || courseContent == null) return;

        // Stopper VLC si actif
        if (activeVlcPlayer != null) {
            activeVlcPlayer.stopAndClear();
            contentContainer.getChildren().removeIf(n -> n instanceof VlcVideoPlayer);
            final VlcVideoPlayer toDispose = activeVlcPlayer;
            activeVlcPlayer = null;
            new Thread(() -> toDispose.dispose(), "vlc-dispose-thread").start();
        }

        if (videoPlayer != null) { videoPlayer.setVisible(false); videoPlayer.setManaged(false); }
        courseScrollPane.setVisible(true);
        courseScrollPane.setManaged(true);
        courseScrollPane.setFitToWidth(true);
        courseScrollPane.setFitToHeight(false);
        VBox.setVgrow(courseScrollPane, Priority.ALWAYS);
        courseScrollPane.setStyle("-fx-background:#F8FAFC;-fx-background-color:#F8FAFC;");

        courseContent.getChildren().clear();
        courseContent.setStyle("-fx-background-color:#F5F7FF;-fx-padding:40 10%;");
        courseContent.setSpacing(20);

        List<QuizQuestion> questions = loadQuizQuestions();

        if (questions.isEmpty()) {
            renderEmpty("❓", "Aucune question disponible pour ce quiz.");
            return;
        }

        // ── En-tête ──────────────────────────────────────────────────────
        VBox header = new VBox(12);
        header.setPadding(new Insets(24, 28, 24, 28));
        header.setStyle("-fx-background-color:linear-gradient(135deg,#EEF2FF,#F5F3FF);" +
                "-fx-background-radius:20;-fx-border-color:#C7D2FE;" +
                "-fx-border-width:1.5;-fx-border-radius:20;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.13),14,0,0,4);");

        HBox titleRow = new HBox(14);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size:32;");
        VBox titleInfo = new VBox(4);
        Label title = new Label(moduleTitle);
        title.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#1E1B4B;");
        Label sub = new Label(questions.size() + " question" + (questions.size() > 1 ? "s" : "") + "   •   Score requis : 70 %");
        sub.setStyle("-fx-font-size:11.5;-fx-text-fill:#6366F1;-fx-font-weight:bold;" +
                "-fx-background-color:#EEF2FF;-fx-padding:4 14;-fx-background-radius:20;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:20;");
        titleInfo.getChildren().addAll(title, sub);
        titleRow.getChildren().addAll(icon, titleInfo);
        header.getChildren().add(titleRow);

        // ── Questions ────────────────────────────────────────────────────
        List<ToggleGroup> groups = new ArrayList<>();
        VBox questionsBox = new VBox(18);

        for (int qi = 0; qi < questions.size(); qi++) {
            QuizQuestion q = questions.get(qi);
            ToggleGroup tg = new ToggleGroup();
            groups.add(tg);

            // Numéro de question
            HBox qHeaderRow = new HBox(10);
            qHeaderRow.setAlignment(Pos.CENTER_LEFT);
            Label qNum = new Label("Q" + (qi + 1));
            qNum.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:white;" +
                    "-fx-background-color:#6366F1;-fx-background-radius:50;" +
                    "-fx-min-width:26;-fx-min-height:26;-fx-max-width:26;-fx-max-height:26;");
            qNum.setAlignment(Pos.CENTER);
            Label qLbl = new Label(q.question);
            qLbl.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1E1B4B;");
            qLbl.setWrapText(true);
            HBox.setHgrow(qLbl, Priority.ALWAYS);
            qHeaderRow.getChildren().addAll(qNum, qLbl);

            VBox optBox = new VBox(10);
            optBox.setPadding(new Insets(8, 0, 0, 0));
            addQuizOpt(optBox, tg, "A", q.optionA);
            addQuizOpt(optBox, tg, "B", q.optionB);
            if (q.optionC != null && !q.optionC.isBlank()) addQuizOpt(optBox, tg, "C", q.optionC);
            if (q.optionD != null && !q.optionD.isBlank()) addQuizOpt(optBox, tg, "D", q.optionD);

            VBox qCard = new VBox(14, qHeaderRow, optBox);
            qCard.setPadding(new Insets(20, 24, 22, 24));
            qCard.setStyle("-fx-background-color:white;-fx-background-radius:18;" +
                    "-fx-border-color:#E0E7FF;-fx-border-width:1.5;-fx-border-radius:18;" +
                    "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.09),10,0,0,4);");
            questionsBox.getChildren().add(qCard);
        }

        // ── Bouton Soumettre ─────────────────────────────────────────────
        Button btnSubmit = new Button("✅  Soumettre mes réponses");
        btnSubmit.setStyle("-fx-background-color:linear-gradient(to right,#6366F1,#8B5CF6);-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-font-size:14;-fx-padding:14 36;-fx-background-radius:16;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.4),14,0,0,5);");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);

        VBox resultBox = new VBox(0);
        resultBox.setPadding(new Insets(4, 0, 28, 0));

        btnSubmit.setOnAction(e -> {
            // Vérifier que toutes les questions sont répondues
            long unanswered = groups.stream().filter(tg -> tg.getSelectedToggle() == null).count();
            if (unanswered > 0) {
                Label warn = new Label("⚠️  Veuillez répondre à toutes les questions (" + unanswered + " manquante" + (unanswered > 1 ? "s" : "") + ")");
                warn.setStyle("-fx-text-fill:#DC2626;-fx-font-size:12;-fx-font-weight:bold;-fx-padding:8 0;");
                resultBox.getChildren().setAll(warn);
                return;
            }

            // Calculer le score
            int correct = 0;
            List<Boolean> results = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                RadioButton sel = (RadioButton) groups.get(i).getSelectedToggle();
                boolean ok = sel != null && sel.getUserData().equals(questions.get(i).bonneReponse);
                results.add(ok);
                if (ok) correct++;
            }
            int score = correct * 100 / questions.size();
            boolean passed = score >= 70;

            // Désactiver toutes les options + montrer correction
            showQuizCorrection(questionsBox, questions, groups, results);
            btnSubmit.setDisable(true);
            btnSubmit.setStyle("-fx-background-color:#9CA3AF;-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-font-size:14;-fx-padding:12 32;-fx-background-radius:12;");

            // Afficher le résultat
            VBox resultCard = buildQuizResult(score, correct, questions.size(), passed);
            resultBox.getChildren().setAll(resultCard);

            // Sauvegarder en DB
            saveQuizScore(score);

            // Scroll vers le résultat
            Platform.runLater(() -> courseScrollPane.setVvalue(1.0));
        });

        courseContent.getChildren().addAll(header, questionsBox, btnSubmit, resultBox);
    }

    private void addQuizOpt(VBox parent, ToggleGroup tg, String letter, String text) {
        if (text == null || text.isBlank()) return;
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(tg);
        rb.setUserData(letter);
        rb.setStyle("-fx-font-size:13;-fx-text-fill:#374151;");
        rb.setWrapText(true);

        // Badge lettre coloré
        Label ltr = new Label(letter);
        ltr.setPrefSize(30, 30); ltr.setMinSize(30, 30);
        ltr.setAlignment(Pos.CENTER);
        ltr.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#6366F1;" +
                "-fx-background-color:#EEF2FF;-fx-background-radius:50;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:50;");

        HBox opt = new HBox(14, ltr, rb);
        opt.setAlignment(Pos.CENTER_LEFT);
        opt.setPadding(new Insets(13, 18, 13, 16));
        opt.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                "-fx-border-color:#E0E7FF;-fx-border-width:1.5;-fx-border-radius:14;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.06),4,0,0,1);");

        opt.setOnMouseClicked(e -> rb.setSelected(true));

        rb.selectedProperty().addListener((obs, o, sel) -> {
            if (sel) {
                opt.setStyle("-fx-background-color:#EEF2FF;-fx-background-radius:14;" +
                        "-fx-border-color:#6366F1;-fx-border-width:2;-fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.18),8,0,0,2);");
                ltr.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:white;" +
                        "-fx-background-color:#6366F1;-fx-background-radius:50;" +
                        "-fx-border-color:#4F46E5;-fx-border-width:1;-fx-border-radius:50;");
            } else {
                opt.setStyle("-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-border-color:#E0E7FF;-fx-border-width:1.5;-fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.06),4,0,0,1);");
                ltr.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#6366F1;" +
                        "-fx-background-color:#EEF2FF;-fx-background-radius:50;" +
                        "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:50;");
            }
        });
        parent.getChildren().add(opt);
    }

    private void showQuizCorrection(VBox questionsBox, List<QuizQuestion> questions,
                                    List<ToggleGroup> groups, List<Boolean> results) {
        for (int i = 0; i < questions.size(); i++) {
            VBox qCard = (VBox) questionsBox.getChildren().get(i);
            VBox optBox = (VBox) qCard.getChildren().get(1);
            QuizQuestion q = questions.get(i);
            boolean correct = results.get(i);

            // Colorer la carte question
            qCard.setStyle("-fx-background-color:" + (correct ? "#F0FDF4" : "#FFF5F5") + ";-fx-background-radius:16;" +
                    "-fx-border-color:" + (correct ? "#4ADE80" : "#F87171") + ";-fx-border-width:2.5;-fx-border-radius:16;" +
                    "-fx-effect:dropshadow(gaussian," + (correct ? "rgba(34,197,94" : "rgba(239,68,68") + ",0.12),10,0,0,3);");

            // Indicateur résultat sur la question
            Label badge = new Label(correct ? "✓  Correct" : "✗  Incorrect");
            badge.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:" +
                    (correct ? "#16A34A" : "#DC2626") + ";-fx-padding:2 8;-fx-background-color:" +
                    (correct ? "#DCFCE7" : "#FEE2E2") + ";-fx-background-radius:20;");
            HBox badgeRow = new HBox(badge); badgeRow.setAlignment(Pos.CENTER_RIGHT);
            qCard.getChildren().add(0, badgeRow);

            // Colorer les options
            for (javafx.scene.Node node : optBox.getChildren()) {
                if (!(node instanceof HBox)) continue;
                HBox opt = (HBox) node;
                if (!(opt.getChildren().get(0) instanceof Label)) continue;
                Label ltr = (Label) opt.getChildren().get(0);
                String l = ltr.getText();
                boolean isCorrectAnswer = l.equals(q.bonneReponse);
                RadioButton rb = (RadioButton) opt.getChildren().get(1);
                boolean wasSelected = rb.isSelected();
                rb.setDisable(true);
                if (isCorrectAnswer) {
                    opt.setStyle("-fx-background-color:#DCFCE7;-fx-background-radius:10;" +
                            "-fx-border-color:#22C55E;-fx-border-width:2;-fx-border-radius:10;");
                    ltr.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:white;" +
                            "-fx-background-color:#22C55E;-fx-background-radius:50;");
                } else if (wasSelected) {
                    opt.setStyle("-fx-background-color:#FEE2E2;-fx-background-radius:10;" +
                            "-fx-border-color:#EF4444;-fx-border-width:2;-fx-border-radius:10;");
                    ltr.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:white;" +
                            "-fx-background-color:#EF4444;-fx-background-radius:50;");
                }
            }
        }
    }

    private VBox buildQuizResult(int score, int correct, int total, boolean passed) {
        VBox card = new VBox(18);
        card.setPadding(new Insets(36, 40, 36, 40));
        String bg    = passed ? "linear-gradient(135deg,#F0FDF4,#DCFCE7)" : "linear-gradient(135deg,#FFF5F5,#FEE2E2)";
        String border= passed ? "#4ADE80" : "#FCA5A5";
        card.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:20;" +
                "-fx-border-color:" + border + ";-fx-border-width:2;-fx-border-radius:20;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),16,0,0,5);");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(Double.MAX_VALUE);

        // Emoji principal grand
        Label emoji = new Label(passed ? "🎉" : "📚");
        emoji.setStyle("-fx-font-size:52;");

        // Titre
        Label titleLbl = new Label(passed ? "Bravo, quiz réussi !" : "Encore un effort !");
        titleLbl.setStyle("-fx-font-size:21;-fx-font-weight:bold;-fx-text-fill:" +
                (passed ? "#166534" : "#991B1B") + ";");

        // Score en gros
        Label scoreLbl = new Label(score + "%");
        scoreLbl.setStyle("-fx-font-size:52;-fx-font-weight:bold;-fx-text-fill:" +
                (passed ? "#16A34A" : "#DC2626") + ";");

        // Détail petit
        Label detailLbl = new Label(correct + " / " + total + " bonnes réponses");
        detailLbl.setStyle("-fx-font-size:14;-fx-text-fill:#6B7280;");

        // Badge statut
        Label reqLbl = new Label(passed ? "  ✓  Module validé et enregistré  " : "  Score requis : 70 % — Réessayez !  ");
        reqLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-padding:6 16;" +
                "-fx-background-radius:20;-fx-text-fill:white;" +
                "-fx-background-color:" + (passed ? "#16A34A" : "#DC2626") + ";");

        card.getChildren().addAll(emoji, titleLbl, scoreLbl, detailLbl, reqLbl);

        if (!passed) {
            Button btnRetry = new Button("🔄  Réessayer le quiz");
            btnRetry.setStyle("-fx-background-color:linear-gradient(to right,#6366F1,#8B5CF6);" +
                    "-fx-text-fill:white;-fx-font-weight:bold;" +
                    "-fx-font-size:13;-fx-padding:11 28;-fx-background-radius:12;-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.35),10,0,0,3);");
            btnRetry.setOnAction(e -> renderQuiz());
            card.getChildren().add(btnRetry);
        }
        return card;
    }

    private void saveQuizScore(int score) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO module_progression (employe_id, module_id, statut, score_quiz, date_completion) " +
                            "VALUES (?, ?, 'completed', ?, ?) " +
                            "ON DUPLICATE KEY UPDATE statut='completed', score_quiz=?, date_completion=?");
            ps.setInt(1, employeId);
            ps.setInt(2, moduleId);
            ps.setInt(3, score);
            ps.setDate(4, java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(5, score);
            ps.setDate(6, java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps.executeUpdate();

            // Mettre à jour la barre de progression affichée
            Platform.runLater(() -> loadProgress());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GOOGLE TRANSLATE — MyMemory API (gratuit, sans clé)
    // ══════════════════════════════════════════════════════════════════════

    private HBox buildTranslateBar(String originalText, Label targetLabel,
                                   WebView targetWebView, String webViewMode) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color:#F0F4FF;-fx-border-color:#E0E7FF;-fx-border-width:0 0 1.5 0;");

        Label langLabel = new Label("🌍  Langue :");
        langLabel.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#4F46E5;");

        Button btnFR = new Button("🇫🇷 FR");
        Button btnEN = new Button("🇬🇧 EN");

        String activeStyle   = "-fx-background-color:#4F46E5;-fx-text-fill:white;-fx-font-size:11;" +
                "-fx-font-weight:bold;-fx-padding:5 14;-fx-background-radius:20;-fx-cursor:hand;";
        String inactiveStyle = "-fx-background-color:white;-fx-text-fill:#4F46E5;-fx-font-size:11;" +
                "-fx-font-weight:bold;-fx-padding:5 14;-fx-background-radius:20;" +
                "-fx-border-color:#C7D2FE;-fx-border-width:1;-fx-border-radius:20;-fx-cursor:hand;";

        btnFR.setStyle(activeStyle);
        btnEN.setStyle(inactiveStyle);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size:10;-fx-text-fill:#6B7280;-fx-font-style:italic;");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        final String[] currentText = { originalText };
        final String[] currentLang = { "fr" };

        // Capturer les références finales pour les lambdas
        final WebView capturedWebView = targetWebView;
        final Label   capturedLabel   = targetLabel;
        final String  capturedMode    = webViewMode;

        btnFR.setOnAction(e -> {
            if ("fr".equals(currentLang[0])) return;
            btnFR.setStyle(activeStyle);
            btnEN.setStyle(inactiveStyle);
            currentLang[0] = "fr";
            currentText[0] = originalText;
            statusLabel.setText("");
            if (capturedLabel != null) capturedLabel.setText(originalText);
            if (capturedWebView != null) {
                String html = "feedback".equals(capturedMode)
                        ? buildFeedbackHtml(originalText, false)
                        : buildHtml(originalText);
                capturedWebView.getEngine().loadContent("", "text/html");
                Platform.runLater(() -> {
                    String b64t = java.util.Base64.getEncoder().encodeToString(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    capturedWebView.getEngine().load("data:text/html;charset=utf-8;base64," + b64t);
                });
            }
        });

        btnEN.setOnAction(e -> {
            if ("en".equals(currentLang[0])) return;
            btnEN.setDisable(true);
            btnFR.setDisable(true);
            statusLabel.setText("⏳ Traduction en cours…");

            new Thread(() -> {
                String result = callMyMemoryAPI(currentText[0], "fr", "en");
                Platform.runLater(() -> {
                    btnEN.setDisable(false);
                    btnFR.setDisable(false);
                    if (result == null || result.isBlank()) {
                        statusLabel.setText("❌ Erreur traduction");
                        return;
                    }
                    btnEN.setStyle(activeStyle);
                    btnFR.setStyle(inactiveStyle);
                    currentLang[0] = "en";
                    currentText[0] = result;
                    statusLabel.setText("✓ Traduit en anglais");
                    if (capturedLabel != null) capturedLabel.setText(result);
                    if (capturedWebView != null) {
                        String html = "feedback".equals(capturedMode)
                                ? buildFeedbackHtml(result, true)
                                : buildHtml(result);
                        capturedWebView.getEngine().loadContent("", "text/html");
                        Platform.runLater(() -> {
                            String b64t = java.util.Base64.getEncoder().encodeToString(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            capturedWebView.getEngine().load("data:text/html;charset=utf-8;base64," + b64t);
                        });
                    }
                });
            }, "translate-thread").start();
        });

        bar.getChildren().addAll(langLabel, btnFR, btnEN, statusLabel);
        return bar;
    }

    private String buildFeedbackHtml(String text, boolean translated) {
        String clean = text.replaceAll("NOTE:\\s*\\d+(/100)?\\s*\\n?", "").trim();
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                "*{margin:0;padding:0;box-sizing:border-box;}" +
                "body{font-family:'Segoe UI',sans-serif;font-size:13px;" +
                "  color:#1E293B;padding:8px 2px;line-height:1.8;background:transparent;}" +
                "p{margin:0 0 10px;}" +
                "strong{font-weight:700;color:#1E1B4B;}" +
                ".section{font-weight:700;color:#6366F1;margin:14px 0 6px;font-size:13px;" +
                "  border-left:3px solid #A5B4FC;padding-left:8px;}" +
                "ul{margin:4px 0 10px 0;list-style:none;}" +
                "li{padding:5px 0 5px 20px;position:relative;color:#374151;}" +
                "li::before{content:'▸';position:absolute;left:2px;color:#6366F1;}" +
                (translated ? ".badge{font-size:10px;color:#6366F1;font-weight:bold;font-style:italic;margin-bottom:10px;}" : "") +
                "</style></head><body>" +
                (translated ? "<div class='badge'>🌍 Traduit en anglais</div>" : "") +
                clean.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                        .replaceAll("\\*\\*(.+?)\\*\\*","<strong>$1</strong>")
                        .replaceAll("POINTS POSITIFS:","<div class='section'>✅ Points positifs</div>")
                        .replaceAll("POINTS À AMÉLIORER:","<div class='section'>📌 Points à améliorer</div>")
                        .replaceAll("CONCLUSION:","<div class='section'>📝 Conclusion</div>")
                        .replaceAll("(?m)^- (.+)$","<li>$1</li>")
                        .replace("\n","<br>")
                + "</body></html>";
    }

    private String callMyMemoryAPI(String text, String from, String to) {
        if (text == null || text.isBlank()) return null;
        if (text.length() > 500) return translateInChunks(text, from, to);
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlStr  = MYMEMORY_URL + "?q=" + encoded + "&langpair=" + from + "|" + to;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            if (conn.getResponseCode() != 200) return null;
            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int idx = json.indexOf("\"translatedText\":");
            if (idx < 0) return null;
            String result = parseJsonString(json, idx + 17);
            if (result != null && result.startsWith("QUERY LENGTH")) return null;
            if (result != null) result = result
                    .replace("&amp;","&").replace("&lt;","<").replace("&gt;",">")
                    .replace("&quot;","\"").replace("&#39;","'").replace("&nbsp;"," ");
            return result;
        } catch (Exception e) {
            System.err.println("[MyMemory] " + e.getMessage());
            return null;
        }
    }

    private String translateInChunks(String text, String from, String to) {
        String[] sentences = text.split("(?<=[.!?\\n])\\s*");
        StringBuilder result = new StringBuilder();
        StringBuilder chunk  = new StringBuilder();
        for (String s : sentences) {
            if (chunk.length() + s.length() > 450) {
                String t = callMyMemoryAPI(chunk.toString().trim(), from, to);
                if (t == null) return null;
                result.append(t).append(" ");
                chunk = new StringBuilder();
            }
            chunk.append(s).append(" ");
        }
        if (!chunk.toString().isBlank()) {
            String t = callMyMemoryAPI(chunk.toString().trim(), from, to);
            if (t != null) result.append(t);
        }
        return result.toString().trim();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NAVIGATION + CLOSE
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleClose() {
        if (activeVlcPlayer != null) {
            final VlcVideoPlayer toDispose = activeVlcPlayer;
            activeVlcPlayer = null;
            new Thread(() -> toDispose.dispose(), "vlc-dispose-thread").start();
        }
        if (onCloseRefresh != null) Platform.runLater(onCloseRefresh);
    }

    @FXML
    private void handlePrevious() {
        if (currentIndex <= 0 || allModules.isEmpty()) return;
        navigateTo(currentIndex - 1);
    }

    @FXML
    private void handleNext() {
        if (currentIndex >= allModules.size() - 1 || allModules.isEmpty()) return;
        navigateTo(currentIndex + 1);
    }

    private void navigateTo(int newIndex) {
        int[] moduleInfo = allModules.get(newIndex); // [moduleId]
        int newModuleId = moduleInfo[0];

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, titre, type_contenu, duree_minutes, ordre, " +
                            "IFNULL(contenu_texte,'') AS contenu_texte, IFNULL(url_ressource,'') AS url_ressource " +
                            "FROM module WHERE id=?");
            ps.setInt(1, newModuleId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            String newTitle   = rs.getString("titre");
            String newType    = rs.getString("type_contenu");
            String newUrl     = rs.getString("url_ressource");
            String newContenu = rs.getString("contenu_texte");

            // Mettre à jour le module courant en place (pas de nouvelle fenêtre)
            this.moduleId     = newModuleId;
            this.currentIndex = newIndex;
            this.moduleTitle  = newTitle;
            this.typeContenu  = newType;
            this.videoUrl     = (newUrl != null && !newUrl.isBlank()) ? newUrl : "";
            this.contenuCours = newContenu;
            this.isCompleted  = false; // sera mis à jour par loadProgress()

            if (lblModuleTitle != null) lblModuleTitle.setText(newTitle);
            if (lblModuleType  != null) lblModuleType.setText(getTypeIcon(newType) + "  " + newType);
            if (stage != null) stage.setTitle(newTitle);

            // Réinitialiser le bouton Terminer
            if (btnMarkComplete != null) {
                btnMarkComplete.setText("✓ Terminer");
                btnMarkComplete.setDisable(false);
                btnMarkComplete.setStyle("-fx-background-color:#4F46E5;-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-font-size:13;-fx-padding:9 22;" +
                        "-fx-background-radius:10;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.35),10,0,0,3);");
            }

            highlights.clear();
            updateNavButtons();
            loadProgress();
            loadExistingNotes();
            loadExistingHighlights();

            // Stopper VLC immédiatement, dispose en background
            if (activeVlcPlayer != null) {
                activeVlcPlayer.stopAndClear();
                // Supprimer UNIQUEMENT le node VLC, pas tout le container
                contentContainer.getChildren().removeIf(n -> n instanceof VlcVideoPlayer);
                final VlcVideoPlayer toDispose = activeVlcPlayer;
                activeVlcPlayer = null;
                new Thread(() -> toDispose.dispose(), "vlc-dispose-thread").start();
            }

            renderContent();

        } catch (SQLException e) {
            showError("Erreur navigation : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════════════════

    private String getTypeIcon(String type) {
        if (type == null) return "📄";
        switch (type.toLowerCase()) {
            case "video": return "🎥";
            case "cours": return "📖";
            case "exercice": return "📝";
            case "quiz": return "❓";
            default: return "📄";
        }
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}