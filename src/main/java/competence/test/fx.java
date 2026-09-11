package competence.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import utils.MyDataBase;

import java.io.IOException;
import java.sql.Connection;

import java.util.List;
import java.util.ArrayList;

public class fx extends Application {

    // ── Singleton instance — lets any controller call MainFX.getInstance() ──
    private static fx instance;
    public static fx getInstance() { return instance; }

    private Stage primaryStage;
    private BorderPane mainLayout;
    private Connection connection;

    // ── Nav buttons ────────────────────────────────────────────────────────
    private Button btnCompetencies;
    private Button btnSkillsMatrix;
    private Button btnTraining;
    private Button btnMyLearning;
    private Button btnModulesView;
    private Button btnEvaluations;
    private Button btnDevPlans;

    private List<Button> navButtons = new ArrayList<>();

    // ── FXML paths ────────────────────────────────────────────────────────
    public static final String COMPETENCY_CATALOG = "/views/competence/CompetencyCatalog.fxml";
    public static final String SKILLS_MATRIX      = "/views/competence/SkillsMatrix.fxml";
    public static final String TRAINING_CATALOG   = "/views/competence/TrainingCatalog.fxml";
    public static final String MY_LEARNING        = "/views/competence/MyLearning.fxml";
    public static final String MODULES_VIEW       = "/views/competence/ModulesView.fxml";
    public static final String EVALUATIONS        = "/views/Evaluations.fxml";
    public static final String DEVELOPMENT_PLANS  = "/views/competence/DevelopmentPlans.fxml";

    @Override
    public void start(Stage primaryStage) {
        instance = this;   // ← register singleton on startup
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Skills Management System - Humania");

        initializeDatabase();
        createMainLayout();
        loadCompetencyCatalog();

        Scene scene = new Scene(mainLayout, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        System.out.println("✓ Application démarrée - Catalogue de Compétences chargé!");
    }

    // ── Public navigation API (callable from any controller) ───────────────
    /**
     * Navigate to the given FXML path and update the sidebar active state.
     * Use the public constants: MainFX.TRAINING_CATALOG, MainFX.MY_LEARNING, etc.
     */
    public void navigateTo(String fxmlPath) {
        // Update active button
        if (fxmlPath.equals(COMPETENCY_CATALOG)) setActive(btnCompetencies);
        else if (fxmlPath.equals(SKILLS_MATRIX))  setActive(btnSkillsMatrix);
        else if (fxmlPath.equals(TRAINING_CATALOG)) setActive(btnTraining);
        else if (fxmlPath.equals(MY_LEARNING))    setActive(btnMyLearning);
        else if (fxmlPath.equals(MODULES_VIEW))   setActive(btnModulesView);
        else if (fxmlPath.equals(EVALUATIONS))    setActive(btnEvaluations);
        else if (fxmlPath.equals(DEVELOPMENT_PLANS)) setActive(btnDevPlans);

        // Load the view into the center
        loadView(fxmlPath, "");
    }

    // ── DB ─────────────────────────────────────────────────────────────────
    private void initializeDatabase() {
        try {
            connection = MyDataBase.getInstance().getCnx();
            System.out.println("✓ Connexion à la base de données établie");
        } catch (Exception e) {
            showError("Erreur de connexion",
                    "Impossible de se connecter à la base de données: " + e.getMessage());
        }
    }

    private void createMainLayout() {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #ecf0f1;");
        mainLayout.setLeft(createSidebar());
    }

    // ── SIDEBAR ────────────────────────────────────────────────────────────
    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setStyle("-fx-background-color: #1e2a3a; -fx-padding: 0;");
        sidebar.setPrefWidth(200);

        // Logo
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setStyle("-fx-padding: 20;");

        javafx.scene.image.Image logoImg = null;
        try {
            java.net.URL logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null)
                logoImg = new javafx.scene.image.Image(logoUrl.toExternalForm(), 36, 36, true, true);
        } catch (Exception ignored) {}

        if (logoImg != null && !logoImg.isError()) {
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logoImg);
            logoView.setFitWidth(36); logoView.setFitHeight(36); logoView.setPreserveRatio(true);
            logoBox.getChildren().add(logoView);
        } else {
            Label logoIcon = new Label("🎯");
            logoIcon.setStyle("-fx-font-size: 24;");
            logoBox.getChildren().add(logoIcon);
        }

        Label logoText = new Label("HUMANIA");
        logoText.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");
        logoBox.getChildren().add(logoText);

        // Nav buttons
        VBox menuItems = new VBox(5);
        menuItems.setStyle("-fx-padding: 10;");

        btnCompetencies = createNavButton("🎯", "Competencies");
        btnSkillsMatrix = createNavButton("👥", "Skills Matrix");
        btnTraining     = createNavButton("🎓", "Training Catalog");
        btnMyLearning   = createNavButton("📚", "My Learning");
        btnModulesView  = createNavButton("📦", "Modules");
        btnEvaluations  = createNavButton("📋", "Evaluations");
        btnDevPlans     = createNavButton("🗺", "Development Plans");

        navButtons.addAll(List.of(btnCompetencies, btnSkillsMatrix, btnTraining,
                btnMyLearning, btnModulesView, btnEvaluations, btnDevPlans));

        // Actions — use navigateTo() so active state is always in sync
        btnCompetencies.setOnAction(e -> navigateTo(COMPETENCY_CATALOG));
        btnSkillsMatrix.setOnAction(e -> navigateTo(SKILLS_MATRIX));
        btnTraining    .setOnAction(e -> navigateTo(TRAINING_CATALOG));
        btnMyLearning  .setOnAction(e -> navigateTo(MY_LEARNING));
        btnModulesView .setOnAction(e -> navigateTo(MODULES_VIEW));
        btnEvaluations .setOnAction(e -> navigateTo(EVALUATIONS));
        btnDevPlans    .setOnAction(e -> navigateTo(DEVELOPMENT_PLANS));

        menuItems.getChildren().addAll(
                btnCompetencies, btnSkillsMatrix, btnTraining,
                btnMyLearning, btnModulesView, btnEvaluations, btnDevPlans);

        sidebar.getChildren().addAll(logoBox, menuItems);
        return sidebar;
    }

    // ── ACTIVE STATE ───────────────────────────────────────────────────────
    private void setActive(Button activeBtn) {
        String inactiveStyle =
                "-fx-background-color: transparent; -fx-text-fill: #95aac9; " +
                        "-fx-alignment: CENTER_LEFT; -fx-padding: 12; " +
                        "-fx-font-size: 14; -fx-cursor: hand;";
        String activeStyle =
                "-fx-background-color: #2c7be5; -fx-text-fill: white; " +
                        "-fx-alignment: CENTER_LEFT; -fx-padding: 12; " +
                        "-fx-font-size: 14; -fx-cursor: hand;";

        for (Button btn : navButtons) {
            Label icon = (Label) btn.getGraphic();
            btn.setStyle(inactiveStyle);
            if (icon != null) icon.setStyle("-fx-text-fill: #95aac9;");
        }
        if (activeBtn == null) return;
        activeBtn.setStyle(activeStyle);
        Label activeIcon = (Label) activeBtn.getGraphic();
        if (activeIcon != null) activeIcon.setStyle("-fx-text-fill: white;");
    }

    private Button createNavButton(String icon, String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: #95aac9;");
        button.setGraphic(iconLabel);

        button.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #95aac9; " +
                        "-fx-alignment: CENTER_LEFT; -fx-padding: 12; " +
                        "-fx-font-size: 14; -fx-cursor: hand;");

        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains("#2c7be5")) {
                button.setStyle("-fx-background-color: #253648; -fx-text-fill: white; " +
                        "-fx-alignment: CENTER_LEFT; -fx-padding: 12; " +
                        "-fx-font-size: 14; -fx-cursor: hand;");
                iconLabel.setStyle("-fx-text-fill: white;");
            }
        });
        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains("#2c7be5")) {
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: #95aac9; " +
                        "-fx-alignment: CENTER_LEFT; -fx-padding: 12; " +
                        "-fx-font-size: 14; -fx-cursor: hand;");
                iconLabel.setStyle("-fx-text-fill: #95aac9;");
            }
        });

        return button;
    }

    // ── NAVIGATION ─────────────────────────────────────────────────────────
    private void loadCompetencyCatalog() {
        navigateTo(COMPETENCY_CATALOG);
    }

    private void loadView(String fxmlPath, String title) {
        try {
            System.out.println("Tentative de chargement de: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainLayout.setCenter(view);
            System.out.println("✓ Vue chargée avec succès: " + title);
        } catch (IOException e) {
            System.err.println("✗ Erreur lors du chargement de: " + fxmlPath);
            e.printStackTrace();
            showFallbackView(title.isEmpty() ? fxmlPath : title, fxmlPath, e.getMessage());
        }
    }

    private void showFallbackView(String moduleName, String fxmlPath, String errorMessage) {
        VBox fallbackView = new VBox(20);
        fallbackView.setPadding(new Insets(40));
        fallbackView.setAlignment(Pos.CENTER);
        fallbackView.setStyle("-fx-background-color: #ecf0f1;");

        Label titleLabel = new Label("⚠️ " + moduleName);
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        Label messageLabel = new Label("Impossible de charger le fichier FXML");
        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        Label pathLabel = new Label("Chemin: " + fxmlPath);
        pathLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6;");
        Label errorLabel = new Label("Erreur: " + errorMessage);
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b; -fx-wrap-text: true;");
        errorLabel.setMaxWidth(600);

        fallbackView.getChildren().addAll(titleLabel, messageLabel, pathLabel, errorLabel);
        mainLayout.setCenter(fallbackView);
    }

    // ── UTILITIES ──────────────────────────────────────────────────────────
    private String getTableCount(String tableName) {
        try {
            if (connection != null && !connection.isClosed()) {
                var stmt = connection.createStatement();
                var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM " + tableName);
                if (rs.next()) return String.valueOf(rs.getInt("count"));
                rs.close(); stmt.close();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du comptage de " + tableName + ": " + e.getMessage());
        }
        return "0";
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Connexion à la base de données fermée");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) { launch(args); }
}