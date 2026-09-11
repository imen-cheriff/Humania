package test;

import attendance.gui.*;
import com.officeapp.controller.MainController;
import communication.controllers.FeedController;
import communication.services.UserService;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.DatabaseInitializer;
import utils.UserSession;
import utils.MyDataBase;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════════╗
 * ║                 HUMANIA — Application RH Unifiée (6 Modules)                      ║
 * ║  Utilisateur · Compétences · Congés · Social Media · Recrutement · Planification  ║
 * ╚═══════════════════════════════════════════════════════════════════════════════════╝
 */
public class MainFX extends Application {

    // ══ Singleton & Stage ═════════════════════════════════════════════════
    private static MainFX instance;
    public static MainFX getInstance() { return instance; }

    private static Stage primaryStage;
    public static Stage getPrimaryStage() { return primaryStage; }

    // ══ Layouts ═══════════════════════════════════════════════════════════
    private BorderPane mainLayout;   // mode RH principal
    private BorderPane socialLayout; // mode Social Media

    private Connection connection;
    private String loggedUser = "Admin";

    private static final double SIDEBAR_W = 262;

    // ── Social Media ───────────────────────────────────────────────────────
    private FXMLLoader  socialMediaLoader;
    private FeedController feedController;
    private UserService userService;

    // ── Attendance controllers ─────────────────────────────────────────────
    private AbsencesCongesController    absencesCongesController;
    private ValidationsSoldesController validationsSoldesController;

    // ── Sidebar RH ─────────────────────────────────────────────────────────
    private VBox mainSidebar;

    // ── Nav buttons ────────────────────────────────────────────────────────
    // Utilisateurs
    private Button btnAdminDashboard, btnProfil, btnGestionUtilisateur;
    private Button btnCandidatsAcceptes, btnArchiveUtilisateur;

    // Compétences
    private Button btnCompetencies, btnSkillsMatrix, btnTraining;
    private Button btnMyLearning, btnModulesView, btnModuleLearner, btnEvaluations, btnDevPlans, btnAIAssistant, btnHRDashboard;

    // Congés / Absences
    private Button btnDashboard, btnAbsences, btnConges, btnValidations, btnSoldes, btnRapportsRH;

    // Social Media
    private Button btnSocialFeed;

    // Recrutement
    private Button btnCandInterne, btnCandExterne, btnDocumentRecr;
    private Button btnPosteExterne, btnPosteInterne, btnAnalyse, btnAnalyseRH;

    // Planification / Coworking
    private Button btnReserverEspaces, btnParticiperEvenements;
    private Button btnReunion, btnInsideMap;
    private Button btnOnboarding, btnOffboarding;
    private Button btnGestionEspaces, btnGestionEvenements;

    private final List<Button> navButtons = new ArrayList<>();

    // ══ FXML Paths ════════════════════════════════════════════════════════

    // Utilisateur
    // Home dashboard — first page for ALL roles (built in code, no FXML)
    public static final String HOME_DASHBOARD      = "HOME_DASHBOARD";
    public static final String LOGIN             = "/views/user/Login.fxml";
    public static final String ADMIN_DASHBOARD     = "/views/user/AdminDashboard.fxml";
    public static final String PROFIL              = "/views/user/Profil.fxml";
    public static final String GESTION_UTILISATEUR = "/views/user/GestionUtilisateur.fxml";
    public static final String FORGOT_PASSWORD     = "/views/user/ForgotPassword.fxml";
    public static final String CANDIDATS_ACCEPTES  = "/views/user/CandidatsAcceptes.fxml";
    public static final String ARCHIVE_UTILISATEUR = "/views/user/archiveUtilisateur.fxml";

    // Compétences
    public static final String COMPETENCY_CATALOG = "/views/competence/CompetencyCatalog.fxml";
    public static final String SKILLS_MATRIX      = "/views/competence/SkillsMatrix.fxml";
    public static final String TRAINING_CATALOG   = "/views/competence/TrainingCatalog.fxml";
    public static final String MY_LEARNING        = "/views/competence/MyLearning.fxml";
    public static final String MODULES_VIEW       = "/views/competence/ModulesView.fxml";
    public static final String MODULES_LEARNER    = "/views/competence/ModuleLearner.fxml";
    public static final String EVALUATIONS_COMP   = "/views/competence/Evaluation.fxml";
    public static final String DEVELOPMENT_PLANS  = "/views/competence/DevelopmentPlans.fxml";

    // ── Nouvelles vues compétences (construites en Java) ─────────────────
    public static final String SKILL_TREE     = "SKILL_TREE";
    public static final String HR_DASHBOARD   = "HR_DASHBOARD";

    // Congés / Absences
    public static final String ABSENCES_CONGES_VIEW    = "/views/conge/AbsencesCongesView.fxml";
    public static final String VALIDATIONS_SOLDES_VIEW = "/views/conge/ValidationsSoldesView.fxml";
    public static final String CONFIG_VIEW             = "/views/conge/ConfigView.fxml";
    public static final String RAPPORTS_RH_VIEW        = "/views/conge/RapportsRHView.fxml";

    // Social Media
    public static final String SOCIAL_FEED_VIEW = "/views/communication/Feed.fxml";

    // Recrutement
    public static final String PAGE_CANDIDATURE_INTERNE = "/views/recrutement/CandidatureInterne.fxml";
    public static final String PAGE_CANDIDATURE_EXTERNE = "/views/recrutement/CandidatureExterne.fxml";
    public static final String PAGE_DOCUMENT            = "/views/recrutement/Document.fxml";
    public static final String PAGE_POSTE_EXTERNE       = "/views/recrutement/PosteExterne.fxml";
    public static final String PAGE_POSTE_INTERNE       = "/views/recrutement/PosteInterne.fxml";
    public static final String PAGE_ANALYSE_RH          = "/views/recrutement/AnalyseRH.fxml";

    // Planification / Coworking
    public static final String PAGE_RESERVER_ESPACES      = "/views/planification/ReserverEspaces.fxml";
    public static final String PAGE_PARTICIPER_EVENEMENTS = "/views/planification/participEvenements.fxml";
    public static final String PAGE_REUNION               = "/views/planification/ReunionCalendar.fxml";
    public static final String PAGE_INSIDE_MAP            = "/com/officeapp/MainView.fxml";
    public static final String PAGE_ONBOARDING            = "/views/planification/Onboarding.fxml";
    public static final String PAGE_OFFBOARDING           = "/views/planification/Offboarding.fxml";
    public static final String PAGE_GESTION_ESPACES       = "/views/planification/GestionEspaces.fxml";
    public static final String PAGE_GESTION_EVENEMENTS    = "/views/planification/GestionEvenements.fxml";

    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void start(Stage stage) {
        instance     = this;
        primaryStage = stage;
        primaryStage.setTitle("Humania · RH Management");

        // Init services au démarrage
        try {
            utilisateur.services.ServiceAdmin sa = new utilisateur.services.ServiceAdmin();
            sa.initializeAdmin();
            System.out.println("✓ ServiceAdmin initialisé");
        } catch (Exception e) { System.err.println("⚠ ServiceAdmin: " + e.getMessage()); }

        DatabaseInitializer.initialize();

        userService = new UserService();
        initializeDatabase();
        showLoginView();
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            cleanupSocial();
            stop();
            Platform.exit();
            System.exit(0);
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── LOGIN
    // ══════════════════════════════════════════════════════════════════════
    public void showLoginView() {
        try {
            java.net.URL url = getClass().getResource(LOGIN);
            if (url == null) throw new IOException("Login FXML introuvable.");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root, 900, 650));
            primaryStage.setMaximized(false);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("⚠ Login FXML introuvable: " + LOGIN);
            // Bypass login for dev — set admin session
            UserSession.getInstance().setUser("Admin");
            UserSession.getInstance().setRole("ADMIN");
            UserSession.getInstance().setEmail("admin@humania.com");
            onLoginSuccess("Admin");
        }
    }

    public void onLoginSuccess(String username) {
        // UserSession is already populated by LoginController via syncFromUtilisateur()
        String displayName = UserSession.getInstance().getUser();
        this.loggedUser = (displayName != null && !displayName.isBlank()) ? displayName : username;
        String role = UserSession.getInstance().getRole();
        System.out.println("✓ Login : " + loggedUser + " (" + role + ")");

        navButtons.clear();
        absencesCongesController = null; validationsSoldesController = null;
        socialMediaLoader = null; feedController = null;

        userService.setOnline(FeedController.CURRENT_USER_ID, true);

        buildMainLayout(role);
        buildSocialLayout();

        navigateTo("ADMIN".equalsIgnoreCase(role) ? GESTION_UTILISATEUR : HOME_DASHBOARD);

        Scene scene = new Scene(mainLayout, 1400, 800);
        applyScrollbarCSS(scene);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }

    public static void showLoginScreen()            { getInstance().showLoginView(); }

    // ══════════════════════════════════════════════════════════════════════
    // ── DB
    // ══════════════════════════════════════════════════════════════════════
    private void initializeDatabase() {
        try {
            connection = MyDataBase.getInstance().getCnx();
            System.out.println("✓ DB connectée");
        } catch (Exception e) { showError("DB Error", e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ██████████  LAYOUT RH PRINCIPAL  █████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════
    private void buildMainLayout(String userRole) {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f4f6fb;");
        mainSidebar = buildSidebar(userRole);
        mainLayout.setLeft(mainSidebar);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ██████████  LAYOUT SOCIAL MEDIA  █████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════
    private void buildSocialLayout() {
        socialLayout = new BorderPane();
        socialLayout.setStyle("-fx-background-color: #f0f4fb;");
        socialLayout.setTop(buildSocialTopBar());
    }

    private VBox buildSocialTopBar() {
        VBox wrapper = new VBox(0);

        // ── Main bar ──────────────────────────────────────────────────────
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinHeight(64); bar.setMaxHeight(64);
        bar.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-padding: 0 28 0 24;" +
                        "-fx-border-color: transparent transparent #e8edf5 transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        bar.setEffect(new DropShadow(10, 0, 2, Color.rgb(0, 20, 80, 0.05)));

        // ── Back button — minimal text, no background ─────────────────────
        Button btnBack = new Button("← RH");
        String backBase = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-border-color: transparent; -fx-padding: 6 10;";
        btnBack.setStyle(backBase);
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-background-radius: 8; -fx-border-color: transparent; -fx-padding: 6 10;"));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(backBase));
        btnBack.setOnAction(e -> switchToRhLayout());
        btnBack.setTooltip(new Tooltip("Revenir au menu RH principal"));

        // ── Thin vertical separator ───────────────────────────────────────
        Region vSep = new Region();
        vSep.setPrefWidth(1); vSep.setMinWidth(1); vSep.setMaxWidth(1);
        vSep.setPrefHeight(22);
        vSep.setStyle("-fx-background-color: #e2e8f0;");
        HBox.setMargin(vSep, new Insets(0, 20, 0, 12));

        // ── Brand block — centered in remaining space ─────────────────────
        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER);

        // Logo badge — solid blue, no gradient
        StackPane logoBadge = new StackPane();
        logoBadge.setPrefSize(34, 34); logoBadge.setMinSize(34, 34); logoBadge.setMaxSize(34, 34);
        Region logoBg = new Region();
        logoBg.setPrefSize(34, 34);
        logoBg.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 10;");
        Label logoIcon = new Label("◎");
        logoIcon.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        logoBadge.getChildren().addAll(logoBg, logoIcon);

        VBox brandText = new VBox(2);
        brandText.setAlignment(Pos.CENTER_LEFT);
        Label brandTitle = new Label("Social Media");
        brandTitle.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label brandSub = new Label("Fil d'actualité · Groupes · Messages");
        brandSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        brandText.getChildren().addAll(brandTitle, brandSub);

        brand.getChildren().addAll(logoBadge, brandText);

        // ── Spacers to center the brand ───────────────────────────────────
        Region leftSpacer = new Region(); HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        Region rightSpacer = new Region(); HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // ── 4-dot HUMANIA accent (right side) ─────────────────────────────
        HBox dots = new HBox(5);
        dots.setAlignment(Pos.CENTER);
        for (String col : new String[]{"#e57373", "#3b82f6", "#f59e0b", "#10b981"}) {
            Region d = new Region();
            d.setPrefSize(7, 7); d.setMinSize(7, 7); d.setMaxSize(7, 7);
            d.setStyle("-fx-background-color: " + col + "; -fx-background-radius: 3.5;");
            dots.getChildren().add(d);
        }

        bar.getChildren().addAll(btnBack, vSep, leftSpacer, brand, rightSpacer, dots);

        // ── Accent line — 3px solid blue at the very bottom ───────────────
        Region accentLine = new Region();
        accentLine.setPrefHeight(3); accentLine.setMinHeight(3); accentLine.setMaxHeight(3);
        accentLine.setStyle("-fx-background-color: #3b82f6;");

        wrapper.getChildren().addAll(bar, accentLine);
        return wrapper;
    }

    private Button makeTopBarBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        String base  = "-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;" +
                " -fx-border-color: transparent; -fx-background-radius: 20; -fx-padding: 6 14 6 14;";
        String hover = "-fx-background-color:" + fg + "; -fx-text-fill: #ffffff;" +
                " -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;" +
                " -fx-border-color: transparent; -fx-background-radius: 20; -fx-padding: 6 14 6 14;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private StackPane buildMiniAvatar(String initials) {
        StackPane avatar = new StackPane();
        Region ring = new Region();
        ring.setPrefSize(36, 36); ring.setMinSize(36, 36); ring.setMaxSize(36, 36);
        ring.setStyle("-fx-background-color: linear-gradient(to bottom right, #e57373, #5b8dee); -fx-background-radius: 18;");
        Region inner = new Region();
        inner.setPrefSize(30, 30); inner.setMinSize(30, 30); inner.setMaxSize(30, 30);
        inner.setStyle("-fx-background-color: #f0f4fb; -fx-background-radius: 15;");
        Label lbl = new Label(initials);
        lbl.setStyle("-fx-text-fill: #2d3a4a; -fx-font-size: 11px; -fx-font-weight: bold;");
        avatar.getChildren().addAll(ring, inner, lbl);
        return avatar;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── SWITCH ENTRE LES MODES
    // ══════════════════════════════════════════════════════════════════════
    private void switchToSocialLayout() {
        if (socialMediaLoader == null) {
            try {
                java.net.URL url = getClass().getResource(SOCIAL_FEED_VIEW);
                if (url == null) { showError("Social Media", "Introuvable : " + SOCIAL_FEED_VIEW); return; }
                socialMediaLoader = new FXMLLoader(url);
                Parent view = socialMediaLoader.load();
                feedController = socialMediaLoader.getController();
                userService.setOnline(FeedController.CURRENT_USER_ID, true);
                socialLayout.setCenter(view);
            } catch (IOException e) {
                e.printStackTrace();
                showError("Social Media", e.getMessage());
                return;
            }
        } else {
            socialLayout.setCenter((Parent) socialMediaLoader.getRoot());
        }
        Scene scene = primaryStage.getScene();
        socialLayout.setOpacity(0);
        scene.setRoot(socialLayout);
        FadeTransition ft = new FadeTransition(Duration.millis(220), socialLayout);
        ft.setToValue(1); ft.play();
    }

    public void returnToMainLayout() { switchToRhLayout(); }

    private void switchToRhLayout() {
        Scene scene = primaryStage.getScene();
        mainLayout.setOpacity(0);
        scene.setRoot(mainLayout);
        setActive(btnSocialFeed);
        FadeTransition ft = new FadeTransition(Duration.millis(220), mainLayout);
        ft.setToValue(1); ft.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ████████████████  SIDEBAR RH  ████████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildSidebar(String userRole) {
        VBox sb = new VBox();
        sb.setStyle("-fx-background-color: #f8fafc; -fx-border-color: transparent #e2e8f0 transparent transparent; -fx-border-width: 0 1 0 0;");
        sb.setPrefWidth(SIDEBAR_W); sb.setMaxWidth(SIDEBAR_W); sb.setMinWidth(SIDEBAR_W);
        sb.setEffect(new DropShadow(18, 4, 0, Color.rgb(0, 20, 80, 0.06)));

        sb.getChildren().add(buildLogoArea());
        sb.getChildren().add(gradientDivider());

        VBox menuItems = new VBox(2);
        menuItems.setStyle("-fx-padding: 8 10 18 10; -fx-background-color: #f8fafc;");
        buildMenuContent(userRole, menuItems);

        ScrollPane scroll = new ScrollPane(menuItems);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: #f8fafc; -fx-background: #f8fafc; -fx-border-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        sb.getChildren().add(scroll);

        sb.getChildren().add(gradientDivider());
        sb.getChildren().add(buildUserCard());
        return sb;
    }

    // ── Logo ──────────────────────────────────────────────────────────────
    private HBox buildLogoArea() {
        HBox area = new HBox(13);
        area.setAlignment(Pos.CENTER_LEFT);
        area.setStyle("-fx-padding: 22 16 22 20;");
        area.setMinHeight(82);

        StackPane logoPane = new StackPane();
        logoPane.setPrefSize(48, 48); logoPane.setMinSize(48, 48); logoPane.setMaxSize(48, 48);

        boolean logoLoaded = false;
        try {
            java.net.URL logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                javafx.scene.image.Image img = new javafx.scene.image.Image(logoUrl.toExternalForm(), 48, 48, true, true);
                if (!img.isError()) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                    iv.setFitWidth(48); iv.setFitHeight(48); iv.setPreserveRatio(true);
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(48, 48);
                    clip.setArcWidth(14); clip.setArcHeight(14);
                    iv.setClip(clip);
                    logoPane.getChildren().add(iv);
                    logoLoaded = true;
                }
            }
        } catch (Exception ignored) {}

        if (!logoLoaded) {
            Region bg = new Region();
            bg.setPrefSize(48, 48); bg.setMinSize(48, 48); bg.setMaxSize(48, 48);
            bg.setStyle("-fx-background-color: linear-gradient(to bottom right, #e57373, #5b8dee); -fx-background-radius: 14;");
            Label h = new Label("H");
            h.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold; -fx-font-family: 'Georgia';");
            logoPane.getChildren().addAll(bg, h);
        }

        VBox textBox = new VBox(3);
        textBox.setAlignment(Pos.CENTER_LEFT);
        Label appName = new Label("HUMANIA");
        appName.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox dots = new HBox(4);
        dots.setAlignment(Pos.CENTER_LEFT);
        for (String col : new String[]{"#e57373", "#5b8dee", "#f5c842", "#5db87a"}) {
            Region d = new Region();
            d.setPrefSize(6, 6); d.setMinSize(6, 6); d.setMaxSize(6, 6);
            d.setStyle("-fx-background-color:" + col + "; -fx-background-radius: 3;");
            dots.getChildren().add(d);
        }
        Label appSub = new Label("RH · Management");
        appSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        textBox.getChildren().addAll(appName, dots, appSub);
        area.getChildren().addAll(logoPane, textBox);
        return area;
    }

    // ── Menu RH ───────────────────────────────────────────────────────────
    private void buildMenuContent(String userRole, VBox menu) {

        boolean isAdmin   = "ADMIN".equalsIgnoreCase(userRole);
        boolean isManager = "MANAGER".equalsIgnoreCase(userRole);
        boolean isRH      = "RH".equalsIgnoreCase(userRole);
        boolean isCandidat = "CANDIDAT".equalsIgnoreCase(userRole);

        // ── ACCUEIL — tous les rôles sauf ADMIN ──────────────────────────
        btnAdminDashboard = makeNavButton("🏠", "Accueil", "blue");
        navButtons.add(btnAdminDashboard);
        btnAdminDashboard.setOnAction(e -> navigateTo(HOME_DASHBOARD));
        if (!isAdmin) {
            menu.getChildren().add(makeExpandableSection("NAVIGATION", btnAdminDashboard));
        }

        // ── UTILISATEURS — ADMIN seulement ────────────────────────────────
        if (isAdmin) {
            btnGestionUtilisateur = makeNavButton("👥", "Gestion Utilisateurs", "blue");
            btnCandidatsAcceptes  = makeNavButton("✅", "Candidats Acceptés",   "green");
            btnArchiveUtilisateur = makeNavButton("🗄", "Archives",             "yellow");
            navButtons.addAll(List.of(btnGestionUtilisateur, btnCandidatsAcceptes, btnArchiveUtilisateur));
            btnGestionUtilisateur.setOnAction(e -> navigateTo(GESTION_UTILISATEUR));
            btnCandidatsAcceptes .setOnAction(e -> navigateTo(CANDIDATS_ACCEPTES));
            btnArchiveUtilisateur.setOnAction(e -> navigateTo(ARCHIVE_UTILISATEUR));
            menu.getChildren().add(makeExpandableSection("UTILISATEURS",
                    btnGestionUtilisateur, btnCandidatsAcceptes, btnArchiveUtilisateur));
        }

        // btnProfil — créé pour setActive(), PAS dans le menu (user card)
        btnProfil = makeNavButton("👤", "Mon Profil", "blue");
        btnProfil.setOnAction(e -> navigateTo(PROFIL));
        navButtons.add(btnProfil);

        // ── 1. RECRUTEMENT — accès selon le rôle ─────────────────────────
        btnPosteExterne = makeNavButton("🌍", "Poste Externe", "green");
        btnPosteExterne.setOnAction(e -> navigateTo(PAGE_POSTE_EXTERNE));
        navButtons.add(btnPosteExterne);

        if (isAdmin || isRH) {
            // ADMIN & RH : accès complet
            btnCandInterne  = makeNavButton("📄", "Candidature Interne",  "blue");
            btnCandExterne  = makeNavButton("📑", "Candidature Externe",  "blue");
            btnDocumentRecr = makeNavButton("📁", "Documents",            "yellow");
            btnPosteInterne = makeNavButton("🏢", "Poste Interne",        "green");
            btnAnalyseRH    = makeNavButton("📊", "Analyse RH",           "red");
            navButtons.addAll(List.of(btnCandInterne, btnCandExterne, btnDocumentRecr,
                    btnPosteInterne, btnAnalyseRH));
            btnCandInterne .setOnAction(e -> navigateTo(PAGE_CANDIDATURE_INTERNE));
            btnCandExterne .setOnAction(e -> navigateTo(PAGE_CANDIDATURE_EXTERNE));
            btnDocumentRecr.setOnAction(e -> navigateTo(PAGE_DOCUMENT));
            btnPosteInterne.setOnAction(e -> navigateTo(PAGE_POSTE_INTERNE));
            btnAnalyseRH   .setOnAction(e -> navigateTo(PAGE_ANALYSE_RH));
            menu.getChildren().add(makeExpandableSection("RECRUTEMENT",
                    btnCandInterne, btnCandExterne, btnDocumentRecr,
                    btnPosteExterne, btnPosteInterne, btnAnalyseRH));
        } else {
            // MANAGER / EMPLOYE / CANDIDAT : seulement Poste Externe (candidature en lecture)
            menu.getChildren().add(makeExpandableSection("RECRUTEMENT", btnPosteExterne));
        }

        // ── 2. SOCIAL MEDIA ───────────────────────────────────────────────
        btnSocialFeed = makeNavButton("💬", "réseaux sociaux", "blue");
        navButtons.add(btnSocialFeed);
        btnSocialFeed.setOnAction(e -> { setActive(btnSocialFeed); switchToSocialLayout(); });
        menu.getChildren().add(makeExpandableSection("SOCIAL MEDIA", btnSocialFeed));

        // ── 3. COMPÉTENCES ─────────────────────────────────────────────────
        // Créer TOUS les boutons d'abord (chaque bouton = instance unique)
        btnCompetencies = makeNavButton("🎯", "Compétences",            "blue");
        btnSkillsMatrix = makeNavButton("📋", "Matrice des compétences", "blue");
        btnTraining     = makeNavButton("🎓", "Formations",              "green");
        btnMyLearning   = makeNavButton("📚", "Mes Leçons",              "green");
        btnEvaluations  = makeNavButton("📝", "Évaluations",             "yellow");
        btnDevPlans     = makeNavButton("🗺",  "Plans de développement",  "yellow");
        btnAIAssistant  = makeNavButton("🤖", "AI Learning Assistant",   "blue");

        btnCompetencies.setOnAction(e -> navigateTo(COMPETENCY_CATALOG));
        btnSkillsMatrix.setOnAction(e -> navigateTo(SKILLS_MATRIX));
        btnTraining    .setOnAction(e -> navigateTo(TRAINING_CATALOG));
        btnMyLearning  .setOnAction(e -> navigateTo(MY_LEARNING));
        btnEvaluations .setOnAction(e -> navigateTo(EVALUATIONS_COMP));
        btnDevPlans    .setOnAction(e -> navigateTo(DEVELOPMENT_PLANS));
        btnAIAssistant .setOnAction(e -> competence.controllers.AiLearningAssistant
                .getInstance().show(primaryStage));

        // Ajouter dans navButtons (tous les rôles)
        navButtons.addAll(List.of(btnCompetencies, btnSkillsMatrix,
                btnTraining, btnMyLearning, btnEvaluations, btnDevPlans, btnAIAssistant));

        // ── Construire la section sidebar selon le rôle ──────────────────
        // IMPORTANT : chaque bouton ne peut être dans UN SEUL parent VBox.
        // On construit la liste de boutons dynamiquement pour éviter tout conflit.
        if (isAdmin || isRH) {
            btnHRDashboard = makeNavButton("📈", "Analytics RH", "red");
            btnHRDashboard.setOnAction(e -> navigateTo(HR_DASHBOARD));
            navButtons.add(btnHRDashboard);
            // ADMIN/RH : ordre demandé — Mes Écarts en premier, Analytics RH en dernier
            addCompetencesSection(menu, "ADMIN",
                    btnCompetencies, btnSkillsMatrix,
                    btnTraining, btnMyLearning, btnEvaluations, btnDevPlans,
                    btnHRDashboard);
        } else if (isManager) {
            addCompetencesSection(menu, "MANAGER",
                    btnCompetencies, btnSkillsMatrix,
                    btnTraining, btnMyLearning, btnEvaluations, btnDevPlans);
        } else {
            // EMPLOYE / CANDIDAT
            addCompetencesSection(menu, "EMPLOYE",
                    btnCompetencies, btnTraining,
                    btnMyLearning, btnEvaluations, btnDevPlans);
        }

        // ── 4. ABSENCES & CONGÉS — accès selon le rôle ───────────────────
        btnAbsences    = makeNavButton("📅", "Mes Absences",    "red");
        btnConges      = makeNavButton("🌴", "Mes Congés",      "green");
        btnValidations = makeNavButton("✅", "Validations",     "green");
        btnSoldes      = makeNavButton("💰", "Mes Soldes",      "yellow");
        btnRapportsRH  = makeNavButton("📈", "Rapports RH",     "red");

        btnAbsences   .setOnAction(e -> showCongeView(ABSENCES_CONGES_VIEW, "absences"));
        btnConges     .setOnAction(e -> showCongeView(ABSENCES_CONGES_VIEW, "conges"));
        btnValidations.setOnAction(e -> showCongeView(VALIDATIONS_SOLDES_VIEW, "validations"));
        btnSoldes     .setOnAction(e -> showCongeView(VALIDATIONS_SOLDES_VIEW, "soldes"));
        btnRapportsRH .setOnAction(e -> navigateTo(RAPPORTS_RH_VIEW));

        if (isAdmin) {
            // ADMIN : supervision globale — pas ses propres absences/soldes
            navButtons.addAll(List.of(btnValidations, btnRapportsRH));
            menu.getChildren().add(makeExpandableSection("CONGÉS & RH",
                    btnValidations, btnRapportsRH));
        } else if (isManager) {
            // MANAGER : valide les demandes de son équipe
            navButtons.add(btnValidations);
            menu.getChildren().add(makeExpandableSection("ABSENCES & CONGÉS", btnValidations));
        } else if (isRH) {
            // RH : accès complet supervision + rapports
            navButtons.addAll(List.of(btnAbsences, btnConges, btnValidations, btnSoldes, btnRapportsRH));
            menu.getChildren().add(makeExpandableSection("ABSENCES & CONGÉS",
                    btnAbsences, btnConges, btnValidations, btnSoldes, btnRapportsRH));
        } else {
            // EMPLOYE / CANDIDAT : ses propres données seulement
            navButtons.addAll(List.of(btnAbsences, btnConges, btnSoldes));
            menu.getChildren().add(makeExpandableSection("ABSENCES & CONGÉS",
                    btnAbsences, btnConges, btnSoldes));
        }

        // ── 5. PLANIFICATION — accès selon le rôle ───────────────────────
        btnReserverEspaces      = makeNavButton("📌", "Réserver un espace",   "blue");
        btnParticiperEvenements = makeNavButton("🎟", "Participer événement",  "blue");
        btnReunion              = makeNavButton("📆", "Réunions",              "green");
        btnInsideMap            = makeNavButton("🗺",  "Carte intérieure",      "yellow");
        btnOnboarding           = makeNavButton("📥", "Onboarding",            "green");
        btnOffboarding          = makeNavButton("📤", "Offboarding",           "red");
        btnGestionEspaces       = makeNavButton("🏛", "Catalogue Espaces",     "blue");
        btnGestionEvenements    = makeNavButton("📅", "Gestion Événements",    "blue");

        btnReserverEspaces     .setOnAction(e -> navigateTo(PAGE_RESERVER_ESPACES));
        btnParticiperEvenements.setOnAction(e -> navigateTo(PAGE_PARTICIPER_EVENEMENTS));
        btnReunion             .setOnAction(e -> navigateTo(PAGE_REUNION));
        btnInsideMap           .setOnAction(e -> loadInsideMap());
        btnOnboarding          .setOnAction(e -> navigateTo(PAGE_ONBOARDING));
        btnOffboarding         .setOnAction(e -> navigateTo(PAGE_OFFBOARDING));
        btnGestionEspaces      .setOnAction(e -> navigateTo(PAGE_GESTION_ESPACES));
        btnGestionEvenements   .setOnAction(e -> navigateTo(PAGE_GESTION_EVENEMENTS));

        if (isAdmin || isRH) {
            // ADMIN & RH : accès complet (tous les boutons)
            navButtons.addAll(List.of(btnReserverEspaces, btnParticiperEvenements,
                    btnReunion, btnInsideMap, btnOnboarding, btnOffboarding,
                    btnGestionEspaces, btnGestionEvenements));
            menu.getChildren().add(makeExpandableSection("PLANIFICATION",
                    btnReserverEspaces, btnParticiperEvenements,
                    btnReunion, btnInsideMap,
                    btnOnboarding, btnOffboarding,
                    btnGestionEspaces, btnGestionEvenements));
        } else {
            // MANAGER / EMPLOYE / CANDIDAT : seulement les 4 boutons de base
            navButtons.addAll(List.of(btnReserverEspaces, btnParticiperEvenements,
                    btnReunion, btnInsideMap));
            menu.getChildren().add(makeExpandableSection("PLANIFICATION",
                    btnReserverEspaces, btnParticiperEvenements,
                    btnReunion, btnInsideMap));
        }
        menu.getChildren().add(gap(8));
    }

    // ══════════════════════════════════════════════════════════════════════
    // ████████████████  USER CARD (sidebar RH)  ████████████████████████████
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildUserCard() {
        String role = UserSession.getInstance().getRole() != null ? UserSession.getInstance().getRole() : "USER";

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        // Gradient accent bar top
        HBox accentBar = new HBox();
        accentBar.setMinHeight(2); accentBar.setPrefHeight(2);
        for (String col : new String[]{"#e57373", "#5b8dee", "#f5c842", "#5db87a"}) {
            Region seg = new Region(); seg.setPrefHeight(2); HBox.setHgrow(seg, Priority.ALWAYS);
            seg.setStyle("-fx-background-color:" + col + ";"); accentBar.getChildren().add(seg);
        }

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-padding: 12 14 13 16; -fx-cursor: hand;");

        // Avatar — fond bleu solide foncé, initiales blanches bien contrastées
        // Initiales : 1ère lettre prénom + 1ère lettre nom (ex: "Imen Cherif" → "IC")
        String[] nameParts = loggedUser.trim().split("\\s+");
        String initials;
        if (nameParts.length >= 2) {
            initials = ("" + nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase();
        } else {
            initials = loggedUser.length() >= 2 ? loggedUser.substring(0, 2).toUpperCase() : loggedUser.toUpperCase();
        }
        StackPane avatar = new StackPane();
        avatar.setPrefSize(44, 44); avatar.setMinSize(44, 44); avatar.setMaxSize(44, 44);
        Region avatarBg = new Region();
        avatarBg.setPrefSize(44, 44);
        avatarBg.setStyle(
                "-fx-background-color: #dbeafe;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #93c5fd; -fx-border-radius: 12; -fx-border-width: 1.5;"
        );
        Label initLbl = new Label(initials);
        initLbl.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;" +
                        "-fx-font-family: 'Georgia';"
        );
        avatar.getChildren().addAll(avatarBg, initLbl);

        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);
        Label userLbl = new Label(loggedUser);
        userLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String email = "";
        try { email = UserSession.getInstance().getEmail(); } catch (Exception ignored) {}
        if (email == null || email.isBlank()) email = loggedUser.toLowerCase() + "@humania.com";
        Label emailLbl = new Label(email);
        emailLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b;");
        emailLbl.setMaxWidth(130);

        // Role badge with light-friendly colors
        String[] roleColors = switch (role) {
            case "ADMIN"     -> new String[]{"#dc2626", "#fee2e2"};
            case "MANAGER"   -> new String[]{"#2563eb", "#dbeafe"};
            case "RH"        -> new String[]{"#7c3aed", "#ede9fe"};
            case "FORMATEUR" -> new String[]{"#059669", "#d1fae5"};
            case "CANDIDAT"  -> new String[]{"#d97706", "#fef3c7"};
            default          -> new String[]{"#64748b", "#f1f5f9"};
        };
        Label roleLbl = new Label(role);
        roleLbl.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill:" + roleColors[0] +
                "; -fx-background-color:" + roleColors[1] +
                "; -fx-padding: 2 6 2 6; -fx-background-radius: 6;" +
                "-fx-border-color:" + roleColors[0] + "55; -fx-border-radius: 6; -fx-border-width: 1;");
        info.getChildren().addAll(userLbl, emailLbl, roleLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Logout button
        Button logoutBtn = new Button("🚪");
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 15px; -fx-cursor: hand; -fx-text-fill: #64748b; -fx-border-color: transparent;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle("-fx-background-color: #fee2e2; -fx-font-size: 15px; -fx-cursor: hand; -fx-text-fill: #ef4444; -fx-background-radius: 8; -fx-border-color: transparent;"));
        logoutBtn.setOnMouseExited(e  -> logoutBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 15px; -fx-cursor: hand; -fx-text-fill: #64748b; -fx-border-color: transparent;"));
        logoutBtn.setOnAction(e -> handleLogout());
        logoutBtn.setTooltip(new Tooltip("Déconnexion"));

        // Click card → profile
        content.setOnMouseClicked(e -> {
            if (e.getTarget() != logoutBtn) navigateTo(PROFIL);
        });
        content.setOnMouseEntered(e -> content.setStyle("-fx-padding: 12 14 13 16; -fx-cursor: hand; -fx-background-color: #e2e8f0;"));
        content.setOnMouseExited(e  -> content.setStyle("-fx-padding: 12 14 13 16; -fx-cursor: hand;"));

        content.getChildren().addAll(avatar, info, logoutBtn);
        card.getChildren().addAll(accentBar, content);
        return card;
    }

    public void handleLogout() {
        cleanupSocial();
        UserSession.getInstance().clearSession();
        showLoginView();
    }

    private void cleanupSocial() {
        try {
            if (feedController != null) {
                userService.setOnline(FeedController.CURRENT_USER_ID, false);
                if (feedController.getNotificationPoller() != null)
                    feedController.getNotificationPoller().stop();
                feedController.cleanupChat();
                feedController.stopBackgroundChat();
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        socialMediaLoader = null; feedController = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── NAVIGATION RH
    // ══════════════════════════════════════════════════════════════════════
    public void navigateTo(String fxmlPath) {
        Button active = switch (fxmlPath) {
            case HOME_DASHBOARD            -> btnAdminDashboard; // bouton Accueil
            case ADMIN_DASHBOARD           -> btnAdminDashboard; // redirige aussi vers accueil
            case GESTION_UTILISATEUR       -> btnGestionUtilisateur;
            case CANDIDATS_ACCEPTES        -> btnCandidatsAcceptes;
            case ARCHIVE_UTILISATEUR       -> btnArchiveUtilisateur;
            case PROFIL                    -> btnProfil;
            case FORGOT_PASSWORD           -> null;
            case COMPETENCY_CATALOG        -> btnCompetencies;
            case SKILLS_MATRIX             -> btnSkillsMatrix;
            case TRAINING_CATALOG          -> btnTraining;
            case MY_LEARNING               -> btnMyLearning;
            case MODULES_VIEW              -> btnModulesView;
            case MODULES_LEARNER           -> null;
            case EVALUATIONS_COMP          -> btnEvaluations;
            case DEVELOPMENT_PLANS         -> btnDevPlans;
            case HR_DASHBOARD             -> btnHRDashboard;
            // Congés
            case ABSENCES_CONGES_VIEW    -> btnAbsences;
            case VALIDATIONS_SOLDES_VIEW -> btnValidations;
            case CONFIG_VIEW             -> null;
            case RAPPORTS_RH_VIEW        -> btnRapportsRH;
            // Recrutement
            case PAGE_CANDIDATURE_INTERNE  -> btnCandInterne;
            case PAGE_CANDIDATURE_EXTERNE  -> btnCandExterne;
            case PAGE_DOCUMENT             -> btnDocumentRecr;
            case PAGE_POSTE_EXTERNE        -> btnPosteExterne;
            case PAGE_POSTE_INTERNE        -> btnPosteInterne;
            case PAGE_ANALYSE_RH           -> btnAnalyseRH;
            // Planification
            case PAGE_RESERVER_ESPACES      -> btnReserverEspaces;
            case PAGE_PARTICIPER_EVENEMENTS -> btnParticiperEvenements;
            case PAGE_REUNION               -> btnReunion;
            case PAGE_INSIDE_MAP            -> btnInsideMap;
            case PAGE_ONBOARDING            -> btnOnboarding;
            case PAGE_OFFBOARDING           -> btnOffboarding;
            case PAGE_GESTION_ESPACES       -> btnGestionEspaces;
            case PAGE_GESTION_EVENEMENTS    -> btnGestionEvenements;
            default                        -> null;
        };
        setActive(active);
        loadView(fxmlPath);
    }

    /** Affiche une vue construite en Java (ScrollPane) avec fadeIn. */
    private void loadJavaView(javafx.scene.control.ScrollPane view) {
        view.setOpacity(0);
        mainLayout.setCenter(view);
        FadeTransition ft = new FadeTransition(Duration.millis(200), view);
        ft.setToValue(1); ft.play();
    }

    public void setCenter(Parent view) {
        if (mainLayout != null) {
            view.setOpacity(0);
            mainLayout.setCenter(view);
            FadeTransition ft = new FadeTransition(Duration.millis(200), view);
            ft.setToValue(1); ft.play();
        }
    }

    public void hideSidebar() { if (mainLayout != null) { var l = mainLayout.getLeft(); if (l != null) { l.setVisible(false); l.setManaged(false); } } }
    public void showSidebar() { if (mainLayout != null) { var l = mainLayout.getLeft(); if (l != null) { l.setVisible(true);  l.setManaged(true);  } } }

    // ── Congé : charge la vue et appelle la bonne méthode du contrôleur ──────
    private void showCongeView(String fxmlPath, String mode) {
        // Active le bon bouton sidebar
        Button active = switch (mode) {
            case "absences"    -> btnAbsences;
            case "conges"      -> btnConges;
            case "validations" -> btnValidations;
            case "soldes"      -> btnSoldes;
            default            -> null;
        };
        setActive(active);

        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) { showFallbackView(fxmlPath, "Introuvable"); return; }
            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();
            Object ctrl = loader.getController();

            // Appel de la méthode appropriée selon le mode
            if (ctrl instanceof AbsencesCongesController ac) {
                absencesCongesController = ac;
                if ("absences".equals(mode)) ac.afficherAbsences();
                else                         ac.afficherConges();
            } else if (ctrl instanceof ValidationsSoldesController vs) {
                validationsSoldesController = vs;
                if ("validations".equals(mode)) vs.afficherValidations();
                else                            vs.afficherSoldes();
            }

            // Wrap dans ScrollPane
            ScrollPane sp = new ScrollPane(view);
            sp.setFitToWidth(true);
            sp.setFitToHeight(false);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            mainLayout.setCenter(sp);

        } catch (IOException e) { e.printStackTrace(); showFallbackView(fxmlPath, e.getMessage()); }
    }

    // ── Carte intérieure (besoin de brancher les key listeners) ───────────
    private void loadInsideMap() {
        setActive(btnInsideMap);
        try {
            java.net.URL url = getClass().getResource(PAGE_INSIDE_MAP);
            if (url == null) { showFallbackView(PAGE_INSIDE_MAP, "Introuvable"); return; }
            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof MainController mc && primaryStage.getScene() != null) {
                mc.attachKeyListeners(primaryStage.getScene());
            }
            view.setOpacity(0);
            mainLayout.setCenter(view);
            FadeTransition ft = new FadeTransition(Duration.millis(200), view);
            ft.setToValue(1); ft.play();
        } catch (IOException e) { e.printStackTrace(); showFallbackView(PAGE_INSIDE_MAP, e.getMessage()); }
    }

    private void loadView(String fxmlPath) {
        // ✅ Dashboard d'accueil général — construit en Java, pas de FXML
        // ── Vues construites en Java (sans FXML) ─────────────────────────────
        if (HOME_DASHBOARD.equals(fxmlPath)) {
            utilisateur.controllers.HomeDashboardController hdc =
                    new utilisateur.controllers.HomeDashboardController();
            loadJavaView(hdc.buildView()); return;
        }
        if (HR_DASHBOARD.equals(fxmlPath)) {
            loadJavaView(competence.controllers.HRAnalyticsDashboard.buildView()); return;
        }
        if (SKILL_TREE.equals(fxmlPath)) {
            competence.controllers.SkillTreeRPG.show(primaryStage); return;
        }
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) { showFallbackView(fxmlPath, "Introuvable : src/main/resources" + fxmlPath); return; }
            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            // Wrap conge views in a ScrollPane to make them scrollable
            boolean isConge = fxmlPath.startsWith("/views/conge/");
            if (isConge) {
                ScrollPane sp = new ScrollPane(view);
                sp.setFitToWidth(true);
                sp.setFitToHeight(false);
                sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
                mainLayout.setCenter(sp);
            } else {
                // Fade only for non-conge views (avoids lag on heavy conge views)
                view.setOpacity(0);
                mainLayout.setCenter(view);
                FadeTransition ft = new FadeTransition(Duration.millis(150), view);
                ft.setToValue(1); ft.play();
            }
        } catch (IOException e) { e.printStackTrace(); showFallbackView(fxmlPath, e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── COMPOSANTS SIDEBAR RH
    // ══════════════════════════════════════════════════════════════════════
    private Button makeNavButton(String icon, String text, String color) {
        // Light tinted badge with emoji — soft color background, black icon
        StackPane iconBadge = new StackPane();
        iconBadge.setPrefSize(28, 28); iconBadge.setMinSize(28, 28); iconBadge.setMaxSize(28, 28);
        Region badgeBg = new Region();
        badgeBg.setPrefSize(28, 28);
        badgeBg.setStyle("-fx-background-color: " + accentColor(color) + "28; -fx-background-radius: 8;");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");
        iconBadge.getChildren().addAll(badgeBg, iconLbl);

        // Text — dark on light sidebar
        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px;");

        // Left accent bar (shown when active)
        Region colorBar = new Region();
        colorBar.setPrefSize(3, 16); colorBar.setMinSize(3, 16); colorBar.setMaxSize(3, 16);
        colorBar.setStyle("-fx-background-color: " + accentColor(color) + "; -fx-background-radius: 2; -fx-opacity: 0;");

        HBox inner = new HBox(8, colorBar, iconBadge, textLbl);
        inner.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();
        btn.setGraphic(inner);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(6, 12, 6, 10));
        btn.setUserData(color);

        String base = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: transparent;";
        btn.setStyle(base);

        btn.setOnMouseEntered(e -> {
            if (!"active".equals(btn.getProperties().get("state"))) {
                btn.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: transparent;");
                textLbl.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 12.5px;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!"active".equals(btn.getProperties().get("state"))) {
                btn.setStyle(base);
                textLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px;");
            }
        });
        btn.getProperties().put("state", "inactive");
        btn.getProperties().put("textLbl", textLbl);
        return btn;
    }

    private String activeBg(String c)    { return switch(c){ case "red"->"#fde8e8"; case "blue"->"#e3eeff"; case "yellow"->"#fff8e1"; case "green"->"#e6f4ea"; default->"#e3eeff"; }; }
    private String accentColor(String c) { return switch(c){ case "red"->"#e57373"; case "blue"->"#5b8dee"; case "yellow"->"#c9a227"; case "green"->"#5db87a"; default->"#5b8dee"; }; }
    private String hoverColor(String c)  { return switch(c){ case "red"->"#fff1f1"; case "blue"->"#f0f5ff"; case "yellow"->"#fffdf0"; case "green"->"#f0fbf4"; default->"#f0f5ff"; }; }

    private Label makeSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 9.5px; -fx-font-weight: bold; -fx-padding: 14 8 4 8; -fx-letter-spacing: 1.5;");
        return lbl;
    }


    /**
     * Construit et ajoute la section COMPÉTENCES dans le menu.
     * Chaque bouton étant un Node JavaFX, il ne peut appartenir qu'à un seul
     * parent — cette méthode garantit que chaque rôle reçoit sa propre section
     * avec exactement les boutons voulus dans le bon ordre.
     */
    private void addCompetencesSection(VBox menu, String role, Button... buttons) {
        VBox section = new VBox(0);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand; -fx-padding: 16 10 5 14;");

        Label titleLbl = new Label("COMPÉTENCES");
        titleLbl.setStyle(
                "-fx-text-fill: #94a3b8; -fx-font-size: 9px;" +
                        "-fx-font-weight: bold; -fx-letter-spacing: 1.8;"
        );
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label arrow = new Label("▾");
        arrow.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        header.getChildren().addAll(titleLbl, spacer, arrow);

        VBox contentBox = new VBox(1);
        contentBox.setStyle("-fx-padding: 2 0 4 0;");
        // Ajouter les boutons dans l'ordre exact passé en paramètre
        for (Button btn : buttons) {
            if (btn != null) contentBox.getChildren().add(btn);
        }

        final boolean[] open = {true};
        header.setOnMouseEntered(e -> header.setStyle(
                "-fx-cursor: hand; -fx-padding: 16 10 5 14;" +
                        "-fx-background-color: #e2e8f0; -fx-background-radius: 6;"));
        header.setOnMouseExited(e -> header.setStyle(
                "-fx-cursor: hand; -fx-padding: 16 10 5 14;"));
        header.setOnMouseClicked(e -> {
            open[0] = !open[0];
            javafx.animation.FadeTransition ft =
                    new javafx.animation.FadeTransition(Duration.millis(150), contentBox);
            ft.setFromValue(open[0] ? 0 : 1);
            ft.setToValue(open[0] ? 1 : 0);
            ft.setOnFinished(ev -> {
                contentBox.setVisible(open[0]);
                contentBox.setManaged(open[0]);
            });
            if (open[0]) { contentBox.setVisible(true); contentBox.setManaged(true); }
            ft.play();
            arrow.setText(open[0] ? "▾" : "›");
        });

        section.getChildren().addAll(header, contentBox);
        menu.getChildren().add(section);
    }

    private VBox makeExpandableSection(String title, Button... buttons) {
        VBox section = new VBox(0);

        // Section header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand; -fx-padding: 16 10 5 14;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 9px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-letter-spacing: 1.8;"
        );

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label arrow = new Label("▾");
        arrow.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        header.getChildren().addAll(titleLbl, spacer, arrow);

        VBox contentBox = new VBox(1);
        contentBox.setStyle("-fx-padding: 2 0 4 0;");
        contentBox.getChildren().addAll(buttons);

        final boolean[] open = {true};
        header.setOnMouseEntered(e -> header.setStyle("-fx-cursor: hand; -fx-padding: 16 10 5 14; -fx-background-color: #e2e8f0; -fx-background-radius: 6;"));
        header.setOnMouseExited(e  -> header.setStyle("-fx-cursor: hand; -fx-padding: 16 10 5 14;"));
        header.setOnMouseClicked(e -> {
            open[0] = !open[0];
            FadeTransition ft = new FadeTransition(Duration.millis(150), contentBox);
            ft.setFromValue(open[0] ? 0 : 1); ft.setToValue(open[0] ? 1 : 0);
            ft.setOnFinished(ev -> { contentBox.setVisible(open[0]); contentBox.setManaged(open[0]); });
            if (open[0]) { contentBox.setVisible(true); contentBox.setManaged(true); }
            ft.play();
            arrow.setText(open[0] ? "▾" : "›");
        });

        section.getChildren().addAll(header, contentBox);
        return section;
    }

    private void setActive(Button activeBtn) {
        String base = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: transparent;";
        for (Button btn : navButtons) {
            btn.setStyle(base);
            btn.getProperties().put("state", "inactive");
            if (btn.getGraphic() instanceof HBox hb && hb.getChildren().size() >= 3) {
                if (hb.getChildren().get(0) instanceof Region bar)
                    bar.setStyle(bar.getStyle().replace("-fx-opacity: 1;", "-fx-opacity: 0;"));
                if (hb.getChildren().get(2) instanceof Label lbl)
                    lbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px; -fx-font-weight: normal;");
            }
        }
        if (activeBtn != null) {
            String color = (String) activeBtn.getUserData();
            String accent = accentColor(color);
            // Dark active: subtle left-border glow + accent text
            activeBtn.setStyle(
                    "-fx-background-color: " + accent + "18" + ";" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;" +
                            "-fx-border-color: transparent;"
            );
            activeBtn.getProperties().put("state", "active");
            if (activeBtn.getGraphic() instanceof HBox hb && hb.getChildren().size() >= 3) {
                if (hb.getChildren().get(0) instanceof Region bar)
                    bar.setStyle("-fx-background-color:" + accent + "; -fx-background-radius: 2; -fx-opacity: 1;");
                if (hb.getChildren().get(2) instanceof Label lbl)
                    lbl.setStyle("-fx-text-fill:" + accent + "; -fx-font-size: 12.5px; -fx-font-weight: bold;");
            }
        }
    }

    private Region gradientDivider() {
        Region div = new Region(); div.setPrefHeight(1); div.setMaxHeight(1); div.setMinHeight(1);
        div.setStyle("-fx-background-color: linear-gradient(to right, transparent, #e2e8f0 20%, #cbd5e1 50%, #e2e8f0 80%, transparent);");
        return div;
    }
    private Region gap(double h) { Region r = new Region(); r.setPrefHeight(h); return r; }

    // ── CSS Scrollbar ──────────────────────────────────────────────────────
    private void applyScrollbarCSS(Scene scene) {
        String css =
                ".scroll-pane { -fx-background-color: transparent; -fx-background: transparent; }\n" +
                        ".scroll-pane > .viewport { -fx-background-color: transparent; }\n" +
                        ".scroll-pane .scroll-bar:vertical { -fx-background-color: transparent; -fx-pref-width: 6px; }\n" +
                        ".scroll-pane .scroll-bar:vertical .track { -fx-background-color: rgba(203,213,225,0.4); -fx-background-radius: 10; }\n" +
                        ".scroll-pane .scroll-bar:vertical .thumb { -fx-background-color: linear-gradient(to bottom,#e57373 0%,#5b8dee 33%,#f5c842 66%,#5db87a 100%); -fx-background-radius: 10; }\n" +
                        ".scroll-pane .scroll-bar:vertical .increment-button, .scroll-pane .scroll-bar:vertical .decrement-button { -fx-pref-height: 0; -fx-pref-width: 0; }\n" +
                        ".scroll-pane .scroll-bar:horizontal { -fx-pref-height: 0; }\n";
        try {
            java.io.File tmp = java.io.File.createTempFile("humania_sb", ".css");
            tmp.deleteOnExit();
            java.nio.file.Files.writeString(tmp.toPath(), css);
            scene.getStylesheets().add(tmp.toURI().toURL().toExternalForm());
        } catch (Exception e) { System.err.println("⚠ CSS: " + e.getMessage()); }
    }

    private void showFallbackView(String path, String err) {
        VBox fb = new VBox(20); fb.setPadding(new Insets(50)); fb.setAlignment(Pos.CENTER); fb.setStyle("-fx-background-color: #f4f6fb;");
        Label t = new Label("⚠️  Erreur de chargement"); t.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #e57373;");
        Label p = new Label(path); p.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        Label e = new Label(err);  e.setStyle("-fx-font-size: 11px; -fx-text-fill: #e57373; -fx-wrap-text: true;"); e.setMaxWidth(600);
        fb.getChildren().addAll(t, p, e); mainLayout.setCenter(fb);
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @Override
    public void stop() {
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) { launch(args); }
}