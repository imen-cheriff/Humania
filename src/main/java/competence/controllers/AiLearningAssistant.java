package competence.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.Duration;
import utils.MyDataBase;
import utils.UserSession;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║          AI Learning Assistant — Fenêtre Flottante           ║
 * ║  Chatbot IA propulsé par Groq/Llama3 intégré au module       ║
 * ║  compétences. Analyse le profil employé et recommande        ║
 * ║  des formations, compétences et parcours personnalisés.      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * UTILISATION — depuis n'importe quel controller du module :
 *   AiLearningAssistant.getInstance().show(ownerStage);
 *
 * ou pour ajouter le bouton flottant dans une scène :
 *   AiLearningAssistant.addFloatingButton(rootPane, ownerStage);
 */
public class AiLearningAssistant {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static AiLearningAssistant instance;
    public static AiLearningAssistant getInstance() {
        if (instance == null) instance = new AiLearningAssistant();
        return instance;
    }

    // ── Config ─────────────────────────────────────────────────────────────
    private static final String GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL      = "llama-3.3-70b-versatile";
    private static final int    MAX_TOKENS = 1024;

    // ── State ──────────────────────────────────────────────────────────────
    private Stage          chatStage;
    private VBox           messagesBox;
    private TextField      inputField;
    private Button         sendBtn;
    private ScrollPane     scrollPane;
    private final List<Map<String,String>> conversationHistory = new ArrayList<>();
    private final ExecutorService          executor = Executors.newSingleThreadExecutor();
    private boolean        isTyping = false;
    private String         groqApiKey;
    private String         employeeContext;

    // ── Couleurs LIGHT ─────────────────────────────────────────────────────
    private static final String C_BG       = "#F8FAFF";
    private static final String C_SIDEBAR  = "#FFFFFF";
    private static final String C_CARD     = "#F1F5FE";
    private static final String C_BORDER   = "#E2E8F6";
    private static final String C_INPUT    = "#FFFFFF";
    private static final String C_ACCENT   = "#6366F1";
    private static final String C_ACCENT2  = "#8B5CF6";
    private static final String C_USER_BG  = "#6366F1";
    private static final String C_AI_BG    = "#F1F5FE";
    private static final String C_TEXT     = "#1E293B";
    private static final String C_MUTED    = "#64748B";
    private static final String C_SUCCESS  = "#10B981";

    // ── INIT ───────────────────────────────────────────────────────────────
    private AiLearningAssistant() {
        loadApiKey();
    }

    private void loadApiKey() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("anthropic.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                groqApiKey = p.getProperty("groq.key", "").trim();
            }
        } catch (Exception e) {
            System.err.println("⚠ AiLearningAssistant: clé Groq introuvable — " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BOUTON FLOTTANT — à injecter dans n'importe quel root pane
    // ══════════════════════════════════════════════════════════════════════
    public static void addFloatingButton(Pane rootPane, Stage ownerStage) {
        Button fab = new Button();
        fab.setPrefSize(56, 56);
        fab.setMinSize(56, 56);
        fab.setMaxSize(56, 56);

        // Icône IA stylisée
        Label icon = new Label("✦");
        icon.setStyle("-fx-font-size: 22px; -fx-text-fill: white;");

        // Pulse ring
        Circle pulse = new Circle(28);
        pulse.setFill(Color.TRANSPARENT);
        pulse.setStroke(Color.web("#6366F1", 0.4));
        pulse.setStrokeWidth(2);

        ScaleTransition pulseAnim = new ScaleTransition(Duration.seconds(1.5), pulse);
        pulseAnim.setFromX(1.0); pulseAnim.setFromY(1.0);
        pulseAnim.setToX(1.4);   pulseAnim.setToY(1.4);
        FadeTransition pulseAlpha = new FadeTransition(Duration.seconds(1.5), pulse);
        pulseAlpha.setFromValue(0.6); pulseAlpha.setToValue(0.0);
        ParallelTransition pulseGroup = new ParallelTransition(pulseAnim, pulseAlpha);
        pulseGroup.setCycleCount(Timeline.INDEFINITE);
        pulseGroup.play();

        StackPane fabContent = new StackPane(pulse, icon);

        fab.setGraphic(fabContent);
        fab.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #6366F1, #8B5CF6);" +
                        "-fx-background-radius: 28;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.5), 20, 0, 0, 4);"
        );

        // Tooltip
        Tooltip tip = new Tooltip("AI Learning Assistant — Demandez conseil à votre IA personnelle !");
        tip.setStyle("-fx-font-size: 12px;");
        fab.setTooltip(tip);

        // Hover
        fab.setOnMouseEntered(e -> fab.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #4F46E5, #7C3AED);" +
                        "-fx-background-radius: 28;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.7), 28, 0, 0, 6);"
        ));
        fab.setOnMouseExited(e -> fab.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #6366F1, #8B5CF6);" +
                        "-fx-background-radius: 28;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.5), 20, 0, 0, 4);"
        ));

        fab.setOnAction(e -> getInstance().show(ownerStage));

        // Position en bas à droite
        StackPane.setAlignment(fab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fab, new Insets(0, 24, 24, 0));

        if (rootPane instanceof StackPane sp) {
            sp.getChildren().add(fab);
        } else {
            // Wrap dans un StackPane overlay
            StackPane overlay = new StackPane();
            overlay.setPickOnBounds(false);
            overlay.getChildren().add(fab);
            StackPane.setAlignment(fab, Pos.BOTTOM_RIGHT);
            if (rootPane instanceof BorderPane bp) {
                // Injecter dans le center
                overlay.setStyle("-fx-pointer-events: none;");
            }
            // Ajouter à la scène root si possible
            rootPane.getChildren().add(overlay);
            overlay.setMouseTransparent(false);
            fab.setMouseTransparent(false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SHOW — ouvre / focus la fenêtre flottante
    // ══════════════════════════════════════════════════════════════════════
    public void show(Stage ownerStage) {
        if (chatStage != null && chatStage.isShowing()) {
            chatStage.requestFocus();
            animateBounce();
            return;
        }
        // Stage existe mais est caché → le ré-afficher directement
        if (chatStage != null) {
            chatStage.show();
            chatStage.requestFocus();
            animateOpen();
            return;
        }
        // Première ouverture
        buildEmployeeContext();
        buildChatWindow(ownerStage);
        chatStage.show();
        animateOpen();

        if (conversationHistory.isEmpty()) {
            sendWelcomeMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD WINDOW
    // ══════════════════════════════════════════════════════════════════════
    private void buildChatWindow(Stage owner) {
        chatStage = new Stage();
        chatStage.initStyle(StageStyle.TRANSPARENT);
        chatStage.initModality(Modality.NONE);
        if (owner != null) chatStage.initOwner(owner);
        chatStage.setAlwaysOnTop(true);
        chatStage.setTitle("AI Learning Assistant");

        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: " + C_BG + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.18), 40, 0, 0, 8);"
        );
        root.setPrefSize(420, 620);
        root.setMinSize(380, 500);

        root.getChildren().addAll(
                buildHeader(),
                buildQuickActions(),
                buildMessagesArea(),
                buildInputArea()
        );

        // Drag pour déplacer la fenêtre
        final double[] dragDelta = {0, 0};
        root.setOnMousePressed(e -> { dragDelta[0] = chatStage.getX() - e.getScreenX(); dragDelta[1] = chatStage.getY() - e.getScreenY(); });
        root.setOnMouseDragged(e -> { chatStage.setX(e.getScreenX() + dragDelta[0]); chatStage.setY(e.getScreenY() + dragDelta[1]); });

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(Color.TRANSPARENT);
        chatStage.setScene(scene);

        // Position — bas à droite de l'écran
        if (owner != null) {
            chatStage.setX(owner.getX() + owner.getWidth() - 440);
            chatStage.setY(owner.getY() + owner.getHeight() - 660);
        } else {
            chatStage.setX(javafx.stage.Screen.getPrimary().getBounds().getWidth() - 440);
            chatStage.setY(javafx.stage.Screen.getPrimary().getBounds().getHeight() - 660);
        }
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 16, 18));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #6366F1, #8B5CF6);" +
                        "-fx-background-radius: 20 20 0 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-width: 0;"
        );

        // Avatar IA animé
        StackPane avatar = new StackPane();
        avatar.setPrefSize(42, 42);
        Circle avatarBg = new Circle(21, Color.web("#4F46E5"));
        Circle glow = new Circle(21);
        glow.setFill(Color.TRANSPARENT);
        glow.setStroke(Color.web("#6366F1", 0.5));
        glow.setStrokeWidth(1.5);
        Label avatarIcon = new Label("✦");
        avatarIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");
        avatar.getChildren().addAll(avatarBg, glow, avatarIcon);

        // Pulse sur l'avatar
        ScaleTransition st = new ScaleTransition(Duration.seconds(2), glow);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.3); st.setToY(1.3);
        FadeTransition ft = new FadeTransition(Duration.seconds(2), glow);
        ft.setFromValue(0.5); ft.setToValue(0.0);
        ParallelTransition avatarPulse = new ParallelTransition(st, ft);
        avatarPulse.setCycleCount(Timeline.INDEFINITE);
        avatarPulse.play();

        VBox info = new VBox(2);
        Label name = new Label("AI Learning Assistant");
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox statusRow = new HBox(5);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Circle statusDot = new Circle(4, Color.WHITE);
        FadeTransition blink = new FadeTransition(Duration.seconds(1.2), statusDot);
        blink.setFromValue(1.0); blink.setToValue(0.5); blink.setAutoReverse(true); blink.setCycleCount(Timeline.INDEFINITE); blink.play();
        Label statusLbl = new Label("En ligne · Propulsé par Groq/Llama3");
        statusLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.8);");
        statusRow.getChildren().addAll(statusDot, statusLbl);
        info.getChildren().addAll(name, statusRow);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton fermer uniquement (masque, ne détruit pas)
        Button btnClose = makeIconButton("✕", "white");
        btnClose.setOnAction(e -> chatStage.hide());

        header.getChildren().addAll(avatar, info, spacer, btnClose);
        return header;
    }

    // ── Quick Actions ──────────────────────────────────────────────────────
    private HBox buildQuickActions() {
        HBox box = new HBox(8);
        box.setPadding(new Insets(10, 14, 8, 14));
        box.setStyle("-fx-background-color: #F0F4FF; -fx-border-color: " + C_BORDER + "; -fx-border-width: 0 0 1 0;");
        box.setAlignment(Pos.CENTER_LEFT);

        String[][] actions = {
                {"🎯", "Mes gaps"},
                {"📚", "Recommandations"},
                {"🗺", "Mon PDI"},
                {"📊", "Mon profil"}
        };

        for (String[] a : actions) {
            Button btn = new Button(a[0] + " " + a[1]);
            btn.setStyle(
                    "-fx-background-color: " + C_CARD + ";" +
                            "-fx-text-fill: " + C_MUTED + ";" +
                            "-fx-font-size: 10px;" +
                            "-fx-padding: 5 10;" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: " + C_BORDER + ";" +
                            "-fx-border-radius: 20;" +
                            "-fx-border-width: 1;" +
                            "-fx-cursor: hand;"
            );
            btn.setOnMouseEntered(ev -> btn.setStyle(
                    "-fx-background-color: " + C_ACCENT + "22;" +
                            "-fx-text-fill: " + C_TEXT + ";" +
                            "-fx-font-size: 10px;" +
                            "-fx-padding: 5 10;" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: " + C_ACCENT + ";" +
                            "-fx-border-radius: 20;" +
                            "-fx-border-width: 1;" +
                            "-fx-cursor: hand;"
            ));
            btn.setOnMouseExited(ev -> btn.setStyle(
                    "-fx-background-color: " + C_CARD + ";" +
                            "-fx-text-fill: " + C_MUTED + ";" +
                            "-fx-font-size: 10px;" +
                            "-fx-padding: 5 10;" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: " + C_BORDER + ";" +
                            "-fx-border-radius: 20;" +
                            "-fx-border-width: 1;" +
                            "-fx-cursor: hand;"
            ));
            final String prompt = buildQuickPrompt(a[1]);
            btn.setOnAction(e -> sendMessage(prompt));
            box.getChildren().add(btn);
        }

        return box;
    }

    private String buildQuickPrompt(String action) {
        return switch (action) {
            case "Mes gaps"        -> "Analyse mes écarts de compétences et dis-moi ce que je dois améliorer en priorité.";
            case "Recommandations" -> "Quelles formations me recommandes-tu en fonction de mon profil actuel ?";
            case "Mon PDI"         -> "Aide-moi à construire mon Plan de Développement Individuel pour cette année.";
            case "Mon profil"      -> "Fais-moi un résumé complet de mon profil de compétences actuel.";
            default                -> action;
        };
    }

    // ── Messages Area ──────────────────────────────────────────────────────
    private ScrollPane buildMessagesArea() {
        messagesBox = new VBox(12);
        messagesBox.setPadding(new Insets(14, 14, 14, 14));
        messagesBox.setStyle("-fx-background-color: " + C_BG + ";");
        messagesBox.setFillWidth(true);

        scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background-color: " + C_BG + ";" +
                        "-fx-background: " + C_BG + ";" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // CSS scrollbar sombre
        scrollPane.getStylesheets().add(buildScrollbarCss());

        return scrollPane;
    }

    // ── Input Area ─────────────────────────────────────────────────────────
    private VBox buildInputArea() {
        VBox area = new VBox(0);
        area.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 0 0 20 20;" +
                        "-fx-border-color: " + C_BORDER + " transparent transparent transparent;" +
                        "-fx-border-width: 1 0 0 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.08), 10, 0, 0, -2);"
        );
        area.setPadding(new Insets(12, 14, 14, 14));

        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER);

        inputField = new TextField();
        inputField.setPromptText("Posez votre question sur vos compétences...");
        inputField.setStyle(
                "-fx-background-color: " + C_INPUT + ";" +
                        "-fx-text-fill: " + C_TEXT + ";" +
                        "-fx-prompt-text-fill: " + C_MUTED + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 10 14;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 22;" +
                        "-fx-border-width: 1;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !isTyping) {
                handleSend();
            }
        });

        inputField.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                inputField.setStyle(
                        "-fx-background-color: " + C_INPUT + ";" +
                                "-fx-text-fill: " + C_TEXT + ";" +
                                "-fx-prompt-text-fill: " + C_MUTED + ";" +
                                "-fx-font-size: 13px;" +
                                "-fx-padding: 10 14;" +
                                "-fx-background-radius: 22;" +
                                "-fx-border-color: " + C_ACCENT + ";" +
                                "-fx-border-radius: 22;" +
                                "-fx-border-width: 1.5;"
                );
            } else {
                inputField.setStyle(
                        "-fx-background-color: " + C_INPUT + ";" +
                                "-fx-text-fill: " + C_TEXT + ";" +
                                "-fx-prompt-text-fill: " + C_MUTED + ";" +
                                "-fx-font-size: 13px;" +
                                "-fx-padding: 10 14;" +
                                "-fx-background-radius: 22;" +
                                "-fx-border-color: " + C_BORDER + ";" +
                                "-fx-border-radius: 22;" +
                                "-fx-border-width: 1;"
                );
            }
        });

        sendBtn = new Button("➤");
        sendBtn.setPrefSize(42, 42);
        sendBtn.setMinSize(42, 42);
        sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + C_ACCENT + ", " + C_ACCENT2 + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-background-radius: 21;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.4), 10, 0, 0, 2);"
        );
        sendBtn.setOnAction(e -> handleSend());
        sendBtn.setOnMouseEntered(ev -> sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #4F46E5, #7C3AED);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-background-radius: 21;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.6), 16, 0, 0, 4);"
        ));
        sendBtn.setOnMouseExited(ev -> sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + C_ACCENT + ", " + C_ACCENT2 + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-background-radius: 21;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.4), 10, 0, 0, 2);"
        ));

        inputRow.getChildren().addAll(inputField, sendBtn);

        Label hint = new Label("✦ Powered by Groq · Llama 3.3 · Votre IA RH personnelle");
        hint.setStyle("-fx-font-size: 9px; -fx-text-fill: #94A3B8; -fx-padding: 6 0 0 2;");

        area.getChildren().addAll(inputRow, hint);
        return area;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MESSAGES
    // ══════════════════════════════════════════════════════════════════════
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || isTyping) return;
        inputField.clear();
        sendMessage(text);
    }

    private void sendMessage(String text) {
        addUserMessage(text);
        conversationHistory.add(Map.of("role", "user", "content", text));
        showTypingIndicator();
        callGroqApi(text);
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(2, 0, 2, 60));

        VBox bubble = new VBox(4);
        bubble.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + C_ACCENT + ", " + C_ACCENT2 + ");" +
                        "-fx-background-radius: 18 18 4 18;" +
                        "-fx-padding: 10 14;"
        );
        bubble.setMaxWidth(280);

        Text msgText = new Text(text);
        msgText.setFont(Font.font("System", 13));
        msgText.setFill(Color.WHITE);
        msgText.setWrappingWidth(260);

        Label timeLabel = new Label(now());
        timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: rgba(255,255,255,0.6);");

        bubble.getChildren().addAll(msgText, timeLabel);
        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addAiMessage(String text) {
        Platform.runLater(() -> {
            // Supprimer l'indicateur de frappe
            removeTypingIndicator();

            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(2, 60, 2, 0));

            // Avatar IA mini
            StackPane miniAvatar = new StackPane();
            miniAvatar.setPrefSize(32, 32);
            miniAvatar.setMinSize(32, 32);
            Circle bg = new Circle(16, Color.web(C_ACCENT));
            Label ic = new Label("✦");
            ic.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");
            miniAvatar.getChildren().addAll(bg, ic);

            VBox bubble = new VBox(6);
            bubble.setStyle(
                    "-fx-background-color: " + C_CARD + ";" +
                            "-fx-background-radius: 4 18 18 18;" +
                            "-fx-padding: 12 14;" +
                            "-fx-border-color: " + C_BORDER + ";" +
                            "-fx-border-radius: 4 18 18 18;" +
                            "-fx-border-width: 1;"
            );
            bubble.setMaxWidth(290);

            // Rendu markdown simplifié
            VBox textContent = renderMarkdown(text);

            Label timeLabel = new Label("✦ AI · " + now());
            timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: " + C_MUTED + ";");

            // Boutons d'action sous le message
            HBox actionRow = buildMessageActions(text);

            bubble.getChildren().addAll(textContent, timeLabel, actionRow);
            row.getChildren().addAll(miniAvatar, bubble);

            // Animation d'apparition
            row.setOpacity(0);
            row.setTranslateY(10);
            messagesBox.getChildren().add(row);

            FadeTransition fade = new FadeTransition(Duration.millis(300), row);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), row);
            slide.setToY(0);
            ParallelTransition appearAnim = new ParallelTransition(fade, slide);
            appearAnim.play();

            scrollToBottom();
            isTyping = false;
            sendBtn.setDisable(false);
            inputField.setDisable(false);
        });
    }

    // ── Typing indicator ───────────────────────────────────────────────────
    private HBox typingIndicatorRow;

    private void showTypingIndicator() {
        isTyping = true;
        sendBtn.setDisable(true);
        inputField.setDisable(true);

        Platform.runLater(() -> {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 60, 2, 0));
            row.setId("typing-indicator");

            StackPane miniAvatar = new StackPane();
            miniAvatar.setPrefSize(32, 32);
            Circle bg = new Circle(16, Color.web(C_ACCENT));
            Label ic = new Label("✦");
            ic.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");
            miniAvatar.getChildren().addAll(bg, ic);

            HBox dots = new HBox(6);
            dots.setAlignment(Pos.CENTER);
            dots.setStyle(
                    "-fx-background-color: " + C_CARD + ";" +
                            "-fx-background-radius: 4 18 18 18;" +
                            "-fx-padding: 14 18;" +
                            "-fx-border-color: " + C_BORDER + ";" +
                            "-fx-border-radius: 4 18 18 18;" +
                            "-fx-border-width: 1;"
            );

            for (int i = 0; i < 3; i++) {
                Circle dot = new Circle(4, Color.web(C_ACCENT));
                int delay = i * 150;
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.millis(0),   e -> dot.setOpacity(0.3)),
                        new KeyFrame(Duration.millis(300 + delay), e -> dot.setOpacity(1.0)),
                        new KeyFrame(Duration.millis(600 + delay), e -> dot.setOpacity(0.3))
                );
                tl.setCycleCount(Timeline.INDEFINITE);
                tl.play();
                dots.getChildren().add(dot);
            }

            row.getChildren().addAll(miniAvatar, dots);
            typingIndicatorRow = row;
            messagesBox.getChildren().add(row);
            scrollToBottom();
        });
    }

    private void removeTypingIndicator() {
        if (typingIndicatorRow != null) {
            messagesBox.getChildren().remove(typingIndicatorRow);
            typingIndicatorRow = null;
        }
    }

    // ── Rendu Markdown simplifié ───────────────────────────────────────────
    private VBox renderMarkdown(String text) {
        VBox content = new VBox(4);

        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.isBlank()) {
                Region spacer = new Region();
                spacer.setPrefHeight(4);
                content.getChildren().add(spacer);
                continue;
            }

            if (line.startsWith("## ")) {
                Label h2 = new Label(line.substring(3));
                h2.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + C_ACCENT + "; -fx-padding: 4 0 2 0;");
                h2.setWrapText(true);
                content.getChildren().add(h2);
            } else if (line.startsWith("# ")) {
                Label h1 = new Label(line.substring(2));
                h1.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + C_TEXT + "; -fx-padding: 4 0 2 0;");
                h1.setWrapText(true);
                content.getChildren().add(h1);
            } else if (line.startsWith("- ") || line.startsWith("• ") || line.startsWith("* ")) {
                String item = line.substring(2);
                HBox bullet = new HBox(6);
                bullet.setAlignment(Pos.TOP_LEFT);
                Label dot = new Label("▸");
                dot.setStyle("-fx-text-fill: " + C_ACCENT + "; -fx-font-size: 11px;");
                Label txt = new Label(stripBold(item));
                txt.setStyle("-fx-font-size: 12px; -fx-text-fill: " + C_TEXT + "; -fx-wrap-text: true;");
                txt.setMaxWidth(240);
                bullet.getChildren().addAll(dot, txt);
                content.getChildren().add(bullet);
            } else if (line.startsWith("**") && line.endsWith("**")) {
                Label bold = new Label(line.substring(2, line.length() - 2));
                bold.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + C_TEXT + "; -fx-wrap-text: true;");
                bold.setMaxWidth(265);
                content.getChildren().add(bold);
            } else {
                Label normal = new Label(stripBold(line));
                normal.setStyle("-fx-font-size: 12px; -fx-text-fill: " + C_TEXT + "; -fx-wrap-text: true;");
                normal.setMaxWidth(265);
                content.getChildren().add(normal);
            }
        }
        return content;
    }

    private String stripBold(String text) {
        return text.replaceAll("\\*\\*(.*?)\\*\\*", "$1").replaceAll("\\*(.*?)\\*", "$1");
    }

    private HBox buildMessageActions(String text) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 0, 0, 0));

        Button btnCopy = makeSmallAction("📋", "Copier");
        btnCopy.setOnAction(e -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(text);
            cb.setContent(cc);
            btnCopy.setText("✓ Copié");
            new Timeline(new KeyFrame(Duration.seconds(2), ev -> btnCopy.setText("📋 Copier"))).play();
        });

        Button btnLike = makeSmallAction("👍", "Utile");
        Button btnRegenerate = makeSmallAction("↺", "Régénérer");
        btnRegenerate.setOnAction(e -> {
            if (!conversationHistory.isEmpty()) {
                String lastUser = conversationHistory.stream()
                        .filter(m -> "user".equals(m.get("role")))
                        .reduce((a, b) -> b)
                        .map(m -> m.get("content"))
                        .orElse("");
                if (!lastUser.isEmpty()) {
                    showTypingIndicator();
                    callGroqApi(lastUser);
                }
            }
        });

        row.getChildren().addAll(btnCopy, btnLike, btnRegenerate);
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GROQ API CALL
    // ══════════════════════════════════════════════════════════════════════
    private void callGroqApi(String userMessage) {
        executor.submit(() -> {
            try {
                String response = callGroq(userMessage);
                conversationHistory.add(Map.of("role", "assistant", "content", response));
                addAiMessage(response);
            } catch (Exception e) {
                addAiMessage("⚠️ Désolé, je n'ai pas pu contacter le serveur IA.\n\n" +
                        "Vérifiez votre clé Groq dans `anthropic.properties`.\n\n" +
                        "**Erreur :** " + e.getMessage());
            }
        });
    }

    private String callGroq(String userMessage) throws Exception {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new Exception("Clé API Groq manquante dans anthropic.properties (groq.key)");
        }

        // Construction du payload
        StringBuilder messages = new StringBuilder("[");
        // System prompt
        messages.append("{\"role\":\"system\",\"content\":").append(jsonStr(buildSystemPrompt())).append("}");

        // Historique (max 8 derniers messages pour rester dans la limite de tokens)
        List<Map<String,String>> history = conversationHistory;
        int start = Math.max(0, history.size() - 8);
        for (int i = start; i < history.size(); i++) {
            Map<String,String> m = history.get(i);
            messages.append(",{\"role\":\"").append(m.get("role")).append("\",\"content\":")
                    .append(jsonStr(m.get("content"))).append("}");
        }
        messages.append("]");

        String body = "{\"model\":\"" + MODEL + "\",\"messages\":" + messages +
                ",\"max_tokens\":" + MAX_TOKENS + ",\"temperature\":0.7,\"stream\":false}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Groq API error " + response.statusCode() + ": " + response.body());
        }

        return parseGroqResponse(response.body());
    }

    private String parseGroqResponse(String json) {
        // Parse simple sans librairie externe
        try {
            int idx = json.indexOf("\"content\":");
            if (idx == -1) throw new Exception("Réponse inattendue de Groq");
            int start = json.indexOf("\"", idx + 10) + 1;
            int end = json.indexOf("\"", start);
            // Gestion des guillemets échappés
            StringBuilder result = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case '"'  -> { result.append('"');  i++; }
                        case 'n'  -> { result.append('\n'); i++; }
                        case 't'  -> { result.append('\t'); i++; }
                        case '\\' -> { result.append('\\'); i++; }
                        default   -> result.append(c);
                    }
                } else if (c == '"') {
                    break;
                } else {
                    result.append(c);
                }
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Je n'ai pas pu analyser la réponse de l'IA. Réessayez.";
        }
    }

    // ── System Prompt ──────────────────────────────────────────────────────
    private String buildSystemPrompt() {
        return "Tu es l'AI Learning Assistant de Humania, une application RH innovante. " +
                "Tu es un coach IA expert en gestion des compétences, formations professionnelles et développement de carrière. " +
                "Tu parles en français, avec un ton professionnel mais chaleureux. " +
                "Tu utilises des emojis avec modération pour rendre tes réponses plus lisibles. " +
                "Tu structures tes réponses avec des titres et des listes à puces quand c'est pertinent.\n\n" +
                "PROFIL DE L'EMPLOYÉ CONNECTÉ :\n" + employeeContext + "\n\n" +
                "INSTRUCTIONS :\n" +
                "- Utilise ce profil pour personnaliser tes recommandations\n" +
                "- Recommande des formations concrètes adaptées aux gaps détectés\n" +
                "- Aide à construire des plans de développement (PDI) réalistes\n" +
                "- Explique l'importance de chaque compétence pour la carrière\n" +
                "- Donne des conseils actionnables et mesurables\n" +
                "- Sois encourageant et motivant\n" +
                "- Réponds en markdown (##, -, **bold**) pour une meilleure lisibilité";
    }

    // ── Employee Context Builder ───────────────────────────────────────────
    private void buildEmployeeContext() {
        StringBuilder ctx = new StringBuilder();
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();
            int userId = UserSession.getInstance().getUserId();
            String userName = UserSession.getInstance().getUser();

            ctx.append("Nom : ").append(userName).append("\n");
            ctx.append("ID : ").append(userId).append("\n");
            ctx.append("Rôle : ").append(UserSession.getInstance().getRole()).append("\n\n");

            // Compétences
            try {
                PreparedStatement ps = cnx.prepareStatement(
                        "SELECT c.libelle, ce.niveauActuel, c.niveauMax, ce.niveauValide, " +
                                "cat.libelle AS categorie " +
                                "FROM competenceEmploye ce " +
                                "JOIN competence c ON ce.competence_id = c.id " +
                                "LEFT JOIN categorieCompetence cat ON c.categorie_id = cat.id " +
                                "WHERE ce.employe_id = ? ORDER BY ce.niveauActuel DESC LIMIT 15"
                );
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                ctx.append("COMPÉTENCES ACTUELLES :\n");
                boolean hasSkills = false;
                while (rs.next()) {
                    hasSkills = true;
                    ctx.append("- ").append(rs.getString("libelle"))
                            .append(" : niveau ").append(rs.getInt("niveauActuel"))
                            .append("/").append(rs.getInt("niveauMax"));
                    if (rs.getBoolean("niveauValide")) ctx.append(" ✓ validé");
                    if (rs.getString("categorie") != null) ctx.append(" [").append(rs.getString("categorie")).append("]");
                    ctx.append("\n");
                }
                if (!hasSkills) ctx.append("Aucune compétence enregistrée pour l'instant.\n");
            } catch (SQLException e) {
                ctx.append("Compétences : non disponibles\n");
            }

            // Formations en cours
            try {
                PreparedStatement ps = cnx.prepareStatement(
                        "SELECT f.titre, inf.statut, inf.progression, cat.libelle AS categorie " +
                                "FROM inscriptionFormation inf " +
                                "JOIN sessionFormation sf ON inf.session_id = sf.id " +
                                "JOIN formation f ON sf.formation_id = f.id " +
                                "LEFT JOIN categorieFormation cat ON f.categorie_id = cat.id " +
                                "WHERE inf.employe_id = ? ORDER BY inf.dateInscription DESC LIMIT 10"
                );
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                ctx.append("\nFORMATIONS :\n");
                boolean hasFormations = false;
                while (rs.next()) {
                    hasFormations = true;
                    ctx.append("- ").append(rs.getString("titre"))
                            .append(" — ").append(rs.getString("statut"))
                            .append(" (").append(rs.getInt("progression")).append("%)");
                    if (rs.getString("categorie") != null) ctx.append(" [").append(rs.getString("categorie")).append("]");
                    ctx.append("\n");
                }
                if (!hasFormations) ctx.append("Aucune formation inscrite.\n");
            } catch (SQLException e) {
                ctx.append("Formations : non disponibles\n");
            }

            // PDI actif
            try {
                PreparedStatement ps = cnx.prepareStatement(
                        "SELECT p.annee, p.progressionGlobale, p.statut, COUNT(a.id) AS nb_actions " +
                                "FROM pdi p LEFT JOIN actionPDI a ON a.pdi_id = p.id " +
                                "WHERE p.employe_id = ? GROUP BY p.id ORDER BY p.annee DESC LIMIT 1"
                );
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ctx.append("\nPDI ACTIF : Année ").append(rs.getInt("annee"))
                            .append(", progression ").append(rs.getInt("progressionGlobale"))
                            .append("%, statut ").append(rs.getString("statut"))
                            .append(", ").append(rs.getInt("nb_actions")).append(" actions\n");
                } else {
                    ctx.append("\nPDI : Aucun PDI actif\n");
                }
            } catch (SQLException e) {
                ctx.append("PDI : colonne employe_id absente (schema à mettre à jour)\n");
            }

        } catch (Exception e) {
            ctx.append("Contexte employé non disponible.\n");
        }

        employeeContext = ctx.toString();
    }

    // ── Welcome Message ────────────────────────────────────────────────────
    private void sendWelcomeMessage() {
        String userName = UserSession.getInstance().getUser();
        String firstName = userName != null && userName.contains(" ") ?
                userName.split(" ")[0] : (userName != null ? userName : "");

        String welcome = "## Bonjour " + firstName + " ! 👋\n\n" +
                "Je suis votre **AI Learning Assistant** — votre coach IA personnel pour tout ce qui concerne vos compétences et formations.\n\n" +
                "Je connais déjà votre profil. Voici ce que je peux faire pour vous :\n\n" +
                "- **Analyser** vos écarts de compétences\n" +
                "- **Recommander** des formations adaptées\n" +
                "- **Construire** votre PDI personnalisé\n" +
                "- **Répondre** à toutes vos questions RH\n\n" +
                "Que souhaitez-vous explorer aujourd'hui ?";

        conversationHistory.add(Map.of("role", "assistant", "content", welcome));

        Platform.runLater(() -> addAiMessageDirect(welcome));
    }

    private void addAiMessageDirect(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(2, 60, 2, 0));

        StackPane miniAvatar = new StackPane();
        miniAvatar.setPrefSize(32, 32);
        Circle bg = new Circle(16, Color.web(C_ACCENT));
        Label ic = new Label("✦");
        ic.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");
        miniAvatar.getChildren().addAll(bg, ic);

        VBox bubble = new VBox(6);
        bubble.setStyle(
                "-fx-background-color: " + C_CARD + ";" +
                        "-fx-background-radius: 4 18 18 18;" +
                        "-fx-padding: 12 14;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 4 18 18 18;" +
                        "-fx-border-width: 1;"
        );
        bubble.setMaxWidth(290);

        VBox textContent = renderMarkdown(text);
        Label timeLabel = new Label("✦ AI · " + now());
        timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: " + C_MUTED + ";");

        bubble.getChildren().addAll(textContent, timeLabel);
        row.getChildren().addAll(miniAvatar, bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════
    private void animateOpen() {
        chatStage.getScene().getRoot().setOpacity(0);
        chatStage.getScene().getRoot().setScaleX(0.9);
        chatStage.getScene().getRoot().setScaleY(0.9);

        FadeTransition fade = new FadeTransition(Duration.millis(250), chatStage.getScene().getRoot());
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), chatStage.getScene().getRoot());
        scale.setToX(1.0); scale.setToY(1.0);
        ParallelTransition openAnim = new ParallelTransition(fade, scale);
        openAnim.play();
    }

    private void animateClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(200), chatStage.getScene().getRoot());
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), chatStage.getScene().getRoot());
        scale.setToX(0.95); scale.setToY(0.95);
        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.setOnFinished(e -> {
            chatStage.hide();
            // Réinitialise l'opacité pour la prochaine ouverture
            chatStage.getScene().getRoot().setOpacity(1);
            chatStage.getScene().getRoot().setScaleX(1.0);
            chatStage.getScene().getRoot().setScaleY(1.0);
        });
        pt.play();
    }

    private void animateBounce() {
        ScaleTransition bounce = new ScaleTransition(Duration.millis(80), chatStage.getScene().getRoot());
        bounce.setToX(1.03); bounce.setToY(1.03);
        bounce.setAutoReverse(true); bounce.setCycleCount(2);
        bounce.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILS
    // ══════════════════════════════════════════════════════════════════════
    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private String now() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", "\\t") + "\"";
    }

    private Button makeIconButton(String icon, String color) {
        Button btn = new Button(icon);
        String textColor = color.equals("white") ? "white" : color;
        String hoverBg   = color.equals("white") ? "rgba(255,255,255,0.2)" : color + "22";
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 4 8;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverBg + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 4 8;" +
                        "-fx-background-radius: 6;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 4 8;"
        ));
        return btn;
    }

    private Button makeSmallAction(String icon, String label) {
        Button btn = new Button(icon + " " + label);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + C_MUTED + ";" +
                        "-fx-font-size: 9px;" +
                        "-fx-padding: 2 6;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + C_BORDER + ";" +
                        "-fx-text-fill: " + C_TEXT + ";" +
                        "-fx-font-size: 9px;" +
                        "-fx-padding: 2 6;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + C_MUTED + ";" +
                        "-fx-font-size: 9px;" +
                        "-fx-padding: 2 6;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;"
        ));
        return btn;
    }

    private String buildScrollbarCss() {
        try {
            String css =
                    ".scroll-pane { -fx-background-color: " + C_BG + "; -fx-background: " + C_BG + "; }\n" +
                            ".scroll-pane > .viewport { -fx-background-color: " + C_BG + "; }\n" +
                            ".scroll-pane .scroll-bar:vertical { -fx-background-color: transparent; -fx-pref-width: 5px; }\n" +
                            ".scroll-pane .scroll-bar:vertical .track { -fx-background-color: #EEF2FF; -fx-background-radius: 5; }\n" +
                            ".scroll-pane .scroll-bar:vertical .thumb { -fx-background-color: #C7D2FE; -fx-background-radius: 5; }\n" +
                            ".scroll-pane .scroll-bar:vertical .thumb:hover { -fx-background-color: #6366F1; }\n" +
                            ".scroll-pane .scroll-bar:vertical .increment-button, " +
                            ".scroll-pane .scroll-bar:vertical .decrement-button { -fx-pref-height: 0; }";
            java.io.File tmp = java.io.File.createTempFile("ai_chat_sb", ".css");
            tmp.deleteOnExit();
            java.nio.file.Files.writeString(tmp.toPath(), css);
            return tmp.toURI().toURL().toExternalForm();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Réinitialise la conversation (efface l'historique).
     * Appelable depuis l'extérieur si besoin.
     */
    public void resetConversation() {
        conversationHistory.clear();
        if (messagesBox != null) {
            Platform.runLater(() -> {
                messagesBox.getChildren().clear();
                sendWelcomeMessage();
            });
        }
    }

    /**
     * Ferme et détruit la fenêtre.
     */
    public void close() {
        if (chatStage != null) {
            animateClose();
        }
    }
}