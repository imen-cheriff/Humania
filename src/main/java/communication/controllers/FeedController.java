package communication.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.stage.Popup;
import javafx.util.Duration;

import communication.services.*;
import communication.models.*;
import communication.models.enums.*;
import utils.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class FeedController {

    @FXML
    private TextField searchField;
    @FXML
    private TextField postContentField;
    @FXML
    private Button publishBtn;
    // ── Sidebar programmatique ──────────────────────────────────────────────
    @FXML private VBox sidebarContainer;
    private Button homeBtn;
    private Button savedBtn;
    private Button popularBtn;
    private Button profileBtn;
    @FXML
    private Button imageBtn;
    @FXML
    private Button pollBtn;
    @FXML
    private Button removeImageBtn;
    @FXML
    private VBox publicationsList;
    @FXML
    private VBox suggestionsList;
    @FXML
    private VBox groupsList;
    @FXML
    private ScrollPane feedScrollPane;
    @FXML
    private ScrollPane groupsScrollPane;
    @FXML
    private HBox imagePreviewBox;
    @FXML
    private ImageView imagePreview;
    @FXML
    private Label imageUrlLabel;
    @FXML
    private Label createPostAvatar;
    private Label sidebarUserInitials;
    private Label sidebarUserName;
    private Label sidebarUserEmail;
    @FXML
    private ComboBox<VisibilityOption> visibilitySelector;

    @FXML
    private VBox rightSidebar;

    private Label visibilityBadgePreview;
    @FXML
    private Button notificationBtn;
    @FXML
    private Label notificationBadge;
    @FXML
    private StackPane notificationPopupContainer;

    private ServicesPublication publicationService;
    private CommentaireService commentaireService;
    private ReactionService reactionService;
    private UserService userService;
    private SavedPostService savedPostService;
    private GroupService groupService;
    private FollowService followService;
    private NotificationPoller notificationPoller;
    private Button networkBtn;
    private Label networkBadgeLabel;
    private Button notifBellBtn;
    private Label notifBadgeLabel;
    private Button chatBtn;
    private Button logoutBtn;

    @FXML private BorderPane feedRootPane;

    private NotificationService notificationService;
    private Popup notificationPopup;
    private UserProfileService userProfileService;
    private ChatController chatController;
    private javafx.scene.Node savedRightNode;

    public static int CURRENT_USER_ID = UserSession.getInstance().getUserId();
    private String pendingImageUrl = null;
    private final GroqService groqService = new GroqService();
    private Button activeSidebarBtn;
    private final java.util.List<Button> sidebarNavButtons = new java.util.ArrayList<>();
    private User currentUser;
    private BorderPane mainRoot;
    private ScrollPane feedPane;
    private ChatService backgroundChatService;

    public void cleanupChat() {
        if (chatController != null) chatController.cleanup();
    }

    public void stopBackgroundChat() {
        if (backgroundChatService != null) {
            backgroundChatService.disconnect();
            System.out.println("✅ Background chat stoppé");
        }
    }

    public NotificationPoller getNotificationPoller() {
        return notificationPoller;
    }

    @FXML
    public void initialize() {
        System.out.println("🚀 Initializing FeedController...");

        mainRoot = feedRootPane;

        buildSidebar();

        publicationService = new ServicesPublication();
        commentaireService = new CommentaireService();
        reactionService = new ReactionService();
        userService = new UserService();
        savedPostService = new SavedPostService();
        groupService = new GroupService();
        followService = new FollowService();
        notificationService = new NotificationService();

        CURRENT_USER_ID = UserSession.getInstance().getUserId();

        currentUser = userService.findById(CURRENT_USER_ID);

        FollowStateManager.getInstance().loadFromDb(CURRENT_USER_ID, followService);

        userProfileService = new UserProfileService();

        loadVisibilityOptions();

        publishBtn.setOnAction(e -> handlePublish());
        imageBtn.setOnAction(e -> handleImageButton());
        pollBtn.setOnAction(e -> handlePollButton());

        Button aiBtn = new Button("✨ IA");
        aiBtn.setStyle(
                "-fx-background-color: #EEF4FF; -fx-text-fill: #3D7EE8; " +
                        "-fx-font-weight: 700; -fx-font-size: 12px; " +
                        "-fx-background-radius: 8px; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 6 12;"
        );
        aiBtn.setOnAction(e -> handleImproveText());

        if (pollBtn.getParent() instanceof HBox) {
            HBox btnBar = (HBox) pollBtn.getParent();
            btnBar.getChildren().add(aiBtn);
        }
        removeImageBtn.setOnAction(e -> removeImagePreview());

        backgroundChatService = new ChatService();
        backgroundChatService.startPolling(CURRENT_USER_ID, this::onBackgroundMessageReceived);

        searchField.textProperty().addListener((obs, old, newVal) -> handleSearch(newVal));

        feedPane = feedScrollPane;

        loadCurrentUserInfo();
        loadPublications();
        loadSuggestions();
        loadUserGroups();
        setupNotificationButton();
        updateNotificationBadge();
        updateNetworkBadge();

        feedScrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin instanceof javafx.stage.Stage) {
                        javafx.stage.Stage stage = (javafx.stage.Stage) newWin;
                        notificationPoller = new NotificationPoller(
                                notificationService,
                                CURRENT_USER_ID,
                                stage,
                                this::updateNotificationBadge,
                                this::showInAppToast
                        );
                        notificationPoller.start();
                    }
                });
            }
        });

        System.out.println("✅ FeedController initialized!");
    }

    // ============================================================
    // VISIBILITY OPTIONS
    // ============================================================
    private void loadVisibilityOptions() {
        visibilitySelector.getItems().clear();
        visibilitySelector.getItems().add(new VisibilityOption("🌍 Public", "PUBLIC", null));

        List<Group> userGroups = groupService.findByUserId(CURRENT_USER_ID);
        for (Group group : userGroups) {
            visibilitySelector.getItems().add(
                    new VisibilityOption("👥 " + group.getName(), "GROUP", group.getId()));
        }
        visibilitySelector.getSelectionModel().selectFirst();

        visibilityBadgePreview = new Label();
        visibilityBadgePreview.setStyle(
                "-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-padding: 4 12;" +
                        "-fx-background-radius: 9999px;");
        updateVisibilityBadge(visibilitySelector.getSelectionModel().getSelectedItem());

        if (visibilitySelector.getParent() instanceof HBox) {
            HBox parent = (HBox) visibilitySelector.getParent();
            boolean badgeExists = parent.getChildren().stream()
                    .anyMatch(n -> "visibilityBadge".equals(n.getId()));
            if (!badgeExists) {
                visibilityBadgePreview.setId("visibilityBadge");
                parent.getChildren().add(visibilityBadgePreview);
            }
        }

        visibilitySelector.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> updateVisibilityBadge(newVal));
    }

    private void updateVisibilityBadge(VisibilityOption option) {
        if (visibilityBadgePreview == null || option == null) return;
        if ("GROUP".equals(option.getVisibility())) {
            String[] colors = {"#3D7EE8", "#2DAA63", "#F5A623", "#E8392A"};
            String[] tints  = {"#EEF4FF", "#EDFBF4", "#FFFBF0", "#FEF2F1"};
            int idx = option.getGroupId() != null ? option.getGroupId() % colors.length : 0;
            visibilityBadgePreview.setText("👥 " + option.getLabel().replace("👥 ", ""));
            visibilityBadgePreview.setStyle(
                    "-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-padding: 4 12;" +
                            "-fx-background-radius: 9999px; -fx-background-color: " + tints[idx] +
                            "; -fx-text-fill: " + colors[idx] + ";");
        } else {
            visibilityBadgePreview.setText("🌍 Public");
            visibilityBadgePreview.setStyle(
                    "-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-padding: 4 12;" +
                            "-fx-background-radius: 9999px; -fx-background-color: #F1F5F9; -fx-text-fill: #64748B;");
        }
        visibilityBadgePreview.setVisible(true);
        visibilityBadgePreview.setManaged(true);
    }

    public static class VisibilityOption {
        private String label;
        private String visibility;
        private Integer groupId;

        public VisibilityOption(String label, String visibility, Integer groupId) {
            this.label = label;
            this.visibility = visibility;
            this.groupId = groupId;
        }

        public String getLabel() { return label; }
        public String getVisibility() { return visibility; }
        public Integer getGroupId() { return groupId; }

        @Override
        public String toString() { return label; }
    }

    // ============================================================
    // CURRENT USER INFO
    // ============================================================
    private void loadCurrentUserInfo() {
        if (currentUser != null) {
            String initials = currentUser.getInitials();
            if (createPostAvatar != null)
                createPostAvatar.setText(initials);
            if (sidebarUserInitials != null)
                sidebarUserInitials.setText(initials);
            if (sidebarUserName != null)
                sidebarUserName.setText(currentUser.getFullName());
            if (sidebarUserEmail != null)
                sidebarUserEmail.setText(currentUser.getEmail());
            System.out.println("👤 Current user: " + currentUser.getFullName());

            if (userProfileService != null) {
                loadAvatarIntoLabel(createPostAvatar, CURRENT_USER_ID, 44);
                loadAvatarIntoLabel(sidebarUserInitials, CURRENT_USER_ID, 32);
            }
        }
    }

    // ============================================================
    // FEED LOADING
    // ============================================================
    public void loadPublications() {
        publicationsList.getChildren().clear();
        List<Publication> publications = publicationService.getAll();

        Set<Integer> followingIds = followService.findFollowing(CURRENT_USER_ID)
                .stream()
                .map(f -> f.getFollowedId())
                .collect(java.util.stream.Collectors.toSet());

        for (Publication pub : publications) {
            boolean canSee = false;
            if (pub.isPublic()) {
                canSee = pub.getAuthorId() == CURRENT_USER_ID
                        || followingIds.contains(pub.getAuthorId());
            } else if (pub.isGroupPost()) {
                canSee = groupService.isMember(pub.getGroupId(), CURRENT_USER_ID);
            }
            if (canSee) {
                publicationsList.getChildren().add(createPublicationCard(pub));
            }
        }
    }

    private void loadPopularPublications() {
        publicationsList.getChildren().clear();

        Set<Integer> followingIds = followService.findFollowing(CURRENT_USER_ID)
                .stream()
                .map(f -> f.getFollowedId())
                .collect(java.util.stream.Collectors.toSet());

        List<Publication> publications = publicationService.getAll();
        publications.sort((a, b) -> Integer.compare(b.getNombreReactions(), a.getNombreReactions()));

        for (Publication pub : publications) {
            boolean canSee = false;
            if (pub.isPublic()) {
                canSee = pub.getAuthorId() == CURRENT_USER_ID
                        || followingIds.contains(pub.getAuthorId());
            } else if (pub.isGroupPost()) {
                canSee = groupService.isMember(pub.getGroupId(), CURRENT_USER_ID);
            }
            if (canSee) {
                publicationsList.getChildren().add(createPublicationCard(pub));
            }
        }
        feedScrollPane.setVvalue(0);
    }

    private void loadSavedPublications() {
        publicationsList.getChildren().clear();
        List<SavedPost> savedPosts = savedPostService.findByUserId(CURRENT_USER_ID);

        for (SavedPost savedPost : savedPosts) {
            Publication pub = publicationService.findById(savedPost.getPublicationId());
            if (pub != null) {
                VBox card = createPublicationCard(pub);
                publicationsList.getChildren().add(card);
            }
        }
        feedScrollPane.setVvalue(0);
    }

    private VBox createPublicationCard(Publication pub) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/communication/PostCard.fxml"));
            VBox card = loader.load();

            PostCardController controller = loader.getController();
            controller.setMainController(this);
            controller.setPublication(pub);
            controller.setCurrentUserId(CURRENT_USER_ID);
            controller.initialize();

            return card;
        } catch (Exception e) {
            System.err.println("❌ Error creating post card: " + e.getMessage());
            e.printStackTrace();
            return new VBox();
        }
    }

    // ============================================================
    // PUBLISH
    // ============================================================
    private void handlePublish() {
        String content = postContentField.getText().trim();

        if (content.isEmpty() && pendingImageUrl == null) {
            showAlert("Veuillez écrire quelque chose ou ajouter une image !");
            return;
        }

        VisibilityOption selected = visibilitySelector.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner où publier !");
            return;
        }

        Publication newPub = new Publication();
        newPub.setContenu(content);
        newPub.setAuthorId(CURRENT_USER_ID);
        newPub.setDateCreation(LocalDateTime.now());
        newPub.setStatut(Statut.ACTIF);
        newPub.setImageUrl(pendingImageUrl);
        newPub.setVisibility(selected.getVisibility());
        newPub.setGroupId(selected.getGroupId());

        publicationService.add(newPub);

        postContentField.clear();
        removeImagePreview();
        visibilitySelector.getSelectionModel().selectFirst();

        loadPublications();
        feedScrollPane.setVvalue(0);

        System.out.println("✅ Post published in: " + selected.getLabel());
    }

    // ============================================================
    // IMAGE HANDLING
    // ============================================================
    private void handleImageButton() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🖼️ Ajouter une image");
        dialog.setHeaderText("Entrez l'URL de l'image");

        ButtonType addButtonType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(10));

        TextField urlField = new TextField();
        urlField.setPromptText("https://example.com/image.jpg");
        urlField.setPrefWidth(400);

        content.getChildren().add(urlField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(450);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType)
                return urlField.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(url -> {
            if (url != null && !url.trim().isEmpty())
                showImagePreview(url.trim());
        });
    }

    private void showImagePreview(String imageUrl) {
        try {
            Image img = new Image(imageUrl, true);

            img.errorProperty().addListener((obs, old, newVal) -> {
                if (newVal) {
                    showAlert("❌ Échec du chargement de l'image.");
                    removeImagePreview();
                }
            });

            img.progressProperty().addListener((obs, old, newVal) -> {
                if (newVal.doubleValue() == 1.0 && !img.isError()) {
                    pendingImageUrl = imageUrl;
                    imagePreview.setImage(img);
                    String displayUrl = imageUrl.length() > 40 ? imageUrl.substring(0, 40) + "..." : imageUrl;
                    imageUrlLabel.setText(displayUrl);
                    imagePreviewBox.setVisible(true);
                    imagePreviewBox.setManaged(true);
                }
            });
        } catch (Exception e) {
            showAlert("❌ URL d'image invalide");
        }
    }

    private void removeImagePreview() {
        pendingImageUrl = null;
        imagePreview.setImage(null);
        imageUrlLabel.setText("");
        imagePreviewBox.setVisible(false);
        imagePreviewBox.setManaged(false);
    }

    // ============================================================
    // SEARCH
    // ============================================================
    private void handleSearch(String query) {
        publicationsList.getChildren().clear();

        if (query == null || query.trim().isEmpty()) {
            loadPublications();
            return;
        }

        Set<Integer> followingIds = followService.findFollowing(CURRENT_USER_ID)
                .stream()
                .map(f -> f.getFollowedId())
                .collect(java.util.stream.Collectors.toSet());

        String search = query.toLowerCase();
        List<Publication> allPubs = publicationService.getAll();

        for (Publication pub : allPubs) {
            User author = userService.findById(pub.getAuthorId());
            boolean matches = false;

            if (pub.getContenu() != null && pub.getContenu().toLowerCase().contains(search))
                matches = true;
            if (pub.getShareMessage() != null && pub.getShareMessage().toLowerCase().contains(search))
                matches = true;
            if (author != null && author.getFullName().toLowerCase().contains(search))
                matches = true;

            if (matches) {
                boolean canSee = false;
                if (pub.isPublic()) {
                    canSee = pub.getAuthorId() == CURRENT_USER_ID
                            || followingIds.contains(pub.getAuthorId());
                } else if (pub.isGroupPost()) {
                    canSee = groupService.isMember(pub.getGroupId(), CURRENT_USER_ID);
                }
                if (canSee) {
                    publicationsList.getChildren().add(createPublicationCard(pub));
                }
            }
        }
    }

    // ============================================================
    // IA ASSISTANCE
    // ============================================================
    private void handleImproveText() {
        String content = postContentField.getText().trim();
        if (content.isEmpty()) {
            openAIGenerateDialog();
            return;
        }

        javafx.stage.Stage loadingStage = new javafx.stage.Stage();
        loadingStage.initModality(javafx.stage.Modality.NONE);
        loadingStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        loadingStage.setAlwaysOnTop(true);
        Label loadingLabel = new Label("✨ Amélioration en cours...");
        loadingLabel.setStyle("-fx-padding: 20 32; -fx-font-size: 13px; -fx-font-weight: 600;");
        VBox loadingBox = new VBox(loadingLabel);
        loadingBox.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );
        loadingStage.setScene(new javafx.scene.Scene(loadingBox));
        loadingStage.show();

        new Thread(() -> {
            String improved = groqService.improveText(content);
            Platform.runLater(() -> {
                loadingStage.close();
                if (improved == null || improved.isEmpty()) {
                    showAlert("❌ Erreur IA. Vérifie ta clé Groq.");
                    return;
                }

                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("✨ Texte amélioré");
                dialog.setHeaderText("Voulez-vous remplacer votre texte ?");
                dialog.getDialogPane().getButtonTypes().addAll(
                        new ButtonType("✅ Utiliser", ButtonBar.ButtonData.OK_DONE),
                        ButtonType.CANCEL
                );

                VBox content2 = new VBox(12);
                content2.setPadding(new javafx.geometry.Insets(12));

                Label original = new Label("Original :\n" + content);
                original.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
                original.setWrapText(true);

                Label result = new Label("✨ Amélioré :\n" + improved);
                result.setStyle("-fx-text-fill: #111827; -fx-font-size: 13px; -fx-font-weight: 600;");
                result.setWrapText(true);
                result.setMaxWidth(400);

                content2.getChildren().addAll(original, new Separator(), result);
                dialog.getDialogPane().setContent(content2);
                dialog.getDialogPane().setPrefWidth(440);

                dialog.showAndWait().ifPresent(r -> {
                    if (r.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                        postContentField.setText(improved);
                    }
                });
            });
        }).start();
    }

    private void openAIGenerateDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🤖 Générer un post avec l'IA");
        dialog.setHeaderText(null);
        ButtonType generateType = new ButtonType("✨ Générer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(460);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFF;");

        VBox content = new VBox(14);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("🤖 Génération de post par IA");
        title.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: #111827;");

        Label subtitle = new Label("Décrivez le sujet et l'IA rédige un post professionnel pour vous.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        subtitle.setWrapText(true);

        Separator sep = new Separator();

        Label subjectLabel = new Label("📌 Sujet ou contexte *");
        subjectLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #374151;");

        TextArea promptField = new TextArea();
        promptField.setPromptText("Ex: Annoncer une formation sur Excel pour toute l'équipe vendredi prochain à 14h...");
        promptField.setWrapText(true);
        promptField.setPrefRowCount(3);
        promptField.setStyle(
                "-fx-font-size: 13px; -fx-padding: 10; " +
                        "-fx-background-radius: 8px; -fx-border-color: #E5E7EB; " +
                        "-fx-border-radius: 8px; -fx-border-width: 1.5;"
        );

        Label toneLabel = new Label("🎭 Ton du post");
        toneLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #374151;");

        ToggleGroup toneGroup = new ToggleGroup();
        HBox toneBox = new HBox(8);

        String[][] tones = {
                {"💼 Professionnel", "professionnel et formel"},
                {"😊 Amical", "amical et décontracté"},
                {"🔥 Motivant", "motivant et énergique"},
                {"📢 Informatif", "informatif et factuel"}
        };

        for (String[] tone : tones) {
            ToggleButton tb = new ToggleButton(tone[0]);
            tb.setToggleGroup(toneGroup);
            tb.setUserData(tone[1]);
            tb.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #374151; " +
                            "-fx-font-size: 11px; -fx-background-radius: 20px; " +
                            "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; " +
                            "-fx-border-radius: 20px; -fx-cursor: hand; -fx-padding: 5 10;"
            );
            tb.selectedProperty().addListener((obs, old, selected) -> {
                tb.setStyle(selected ?
                        "-fx-background-color: #3D7EE8; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-background-radius: 20px; " +
                                "-fx-border-color: #3D7EE8; -fx-border-width: 1.5; " +
                                "-fx-border-radius: 20px; -fx-cursor: hand; -fx-padding: 5 10;" :
                        "-fx-background-color: white; -fx-text-fill: #374151; " +
                                "-fx-font-size: 11px; -fx-background-radius: 20px; " +
                                "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; " +
                                "-fx-border-radius: 20px; -fx-cursor: hand; -fx-padding: 5 10;"
                );
            });
            toneBox.getChildren().add(tb);
        }

        ((ToggleButton) toneGroup.getToggles().get(0)).setSelected(true);

        content.getChildren().addAll(title, subtitle, sep, subjectLabel, promptField, toneLabel, toneBox);
        dialog.getDialogPane().setContent(content);
        Platform.runLater(promptField::requestFocus);

        Platform.runLater(() -> {
            Button okBtn = (Button) dialog.getDialogPane().lookupButton(generateType);
            if (okBtn != null) {
                okBtn.setStyle(
                        "-fx-background-color: #3D7EE8; -fx-text-fill: white; " +
                                "-fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 20;"
                );
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != generateType) return;

            String prompt = promptField.getText().trim();
            if (prompt.isEmpty()) {
                showAlert("⚠️ Décrivez d'abord le sujet !");
                return;
            }

            String tone = toneGroup.getSelectedToggle() != null ?
                    (String) toneGroup.getSelectedToggle().getUserData() : "professionnel";

            javafx.stage.Stage loadingStage = new javafx.stage.Stage();
            loadingStage.initModality(javafx.stage.Modality.NONE);
            loadingStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            loadingStage.setAlwaysOnTop(true);
            Label loadingLabel = new Label("🤖 Génération en cours...");
            loadingLabel.setStyle("-fx-padding: 20 32; -fx-font-size: 13px; -fx-font-weight: 600;");
            VBox loadingBox = new VBox(loadingLabel);
            loadingBox.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 12px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
            );
            loadingStage.setScene(new javafx.scene.Scene(loadingBox));
            loadingStage.show();

            new Thread(() -> {
                String generated = groqService.generatePost(prompt, tone);
                Platform.runLater(() -> {
                    loadingStage.close();
                    if (generated == null || generated.isEmpty()) {
                        showAlert("❌ Erreur IA. Vérifie ta connexion.");
                        return;
                    }
                    postContentField.setText(generated);
                    postContentField.requestFocus();
                });
            }).start();
        });
    }

    // ============================================================
    // SUGGESTIONS (Right sidebar)
    // ============================================================
    public void loadSuggestions() {
        if (suggestionsList == null) return;
        suggestionsList.getChildren().clear();

        List<User> users = userService.getAll();
        int count = 0;
        for (User user : users) {
            boolean activelyFollowing = followService.isActiveFollowing(CURRENT_USER_ID, user.getId());
            if (user.getId() != CURRENT_USER_ID && !activelyFollowing && count < 5) {
                HBox card = createSuggestionCard(user);
                suggestionsList.getChildren().add(card);
                count++;
            }
        }
    }

    private HBox createSuggestionCard(User user) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-padding: 6 0; -fx-cursor: default;");

        StackPane avatar = createAvatarCircle(user, 36);
        avatar.setStyle("-fx-cursor: hand;");
        avatar.setOnMouseClicked(e -> openUserProfile(user.getId()));
        if (avatar.getChildren().size() > 0 && avatar.getChildren().get(0) instanceof Label) {
            loadAvatarIntoLabel((Label) avatar.getChildren().get(0), user.getId(), 36);
        }

        VBox info = new VBox(2);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-cursor: hand;");
        nameLabel.setOnMouseClicked(e -> openUserProfile(user.getId()));
        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
        info.getChildren().addAll(nameLabel, usernameLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        String status = followService.getFollowStatus(CURRENT_USER_ID, user.getId());
        Button followBtn = buildFollowButton(user, status);

        card.getChildren().addAll(avatar, info, followBtn);
        return card;
    }

    private Button buildFollowButton(User targetUser, String status) {
        Button btn = new Button();
        applyFollowBtnState(btn, status);

        btn.setOnAction(null);
        btn.setOnAction(e -> {
            String currentStatus = followService.getFollowStatus(CURRENT_USER_ID, targetUser.getId());

            if ("ACTIVE".equals(currentStatus)) {
                followService.unfollow(CURRENT_USER_ID, targetUser.getId());
                FollowStateManager.getInstance().markUnfollowing(targetUser.getId());
                applyFollowBtnState(btn, null);
            } else if ("PENDING".equals(currentStatus)) {
                followService.unfollow(CURRENT_USER_ID, targetUser.getId());
                FollowStateManager.getInstance().markUnfollowing(targetUser.getId());
                applyFollowBtnState(btn, null);
            } else {
                Follow follow = new Follow();
                follow.setFollowerId(CURRENT_USER_ID);
                follow.setFollowedId(targetUser.getId());
                follow.setFollowedAt(LocalDateTime.now());
                followService.add(follow);

                if (currentUser != null && currentUser.isManager()) {
                    FollowStateManager.getInstance().markActivated(targetUser.getId());
                    applyFollowBtnState(btn, "ACTIVE");
                } else {
                    FollowStateManager.getInstance().markFollowing(targetUser.getId());
                    applyFollowBtnState(btn, "PENDING");

                    if (notificationService != null) {
                        Notification notif = new Notification();
                        notif.setUserId(targetUser.getId());
                        notif.setType("FOLLOW_REQUEST");
                        notif.setTitre("Demande de suivi");
                        notif.setMessage(currentUser.getFullName() + " souhaite vous suivre.");
                        notif.setRelatedUserId(CURRENT_USER_ID);
                        notif.setSeen(false);
                        notif.setDateCreation(LocalDateTime.now());
                        notificationService.add(notif);
                    }
                }

                loadSuggestions();
                updateNetworkBadge();
            }
        });

        return btn;
    }

    private void applyFollowBtnState(Button btn, String status) {
        if ("ACTIVE".equals(status)) {
            btn.setText("✓ Suivi");
            btn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-size: 11px; " +
                    "-fx-padding: 4 10; -fx-background-radius: 9999px; -fx-border-width: 0; -fx-cursor: hand;");
        } else if ("PENDING".equals(status)) {
            btn.setText("⏳ En attente");
            btn.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-font-size: 11px; " +
                    "-fx-padding: 4 10; -fx-background-radius: 9999px; -fx-border-width: 0; -fx-cursor: hand;");
        } else {
            btn.setText("Suivre");
            btn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-padding: 4 10; -fx-background-radius: 9999px; -fx-border-width: 0; -fx-cursor: hand;");
        }
    }

    // ============================================================
    // GROUPS
    // ============================================================
    private void loadUserGroups() {
        VBox target = (sbGroupsList != null) ? sbGroupsList : groupsList;
        if (target == null) return;
        target.getChildren().clear();

        if (currentUser != null && currentUser.isManager()) {
            Button createGroupBtn = new Button("+ Créer un groupe");
            createGroupBtn.setStyle(
                    "-fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; " +
                            "-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 12; " +
                            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-border-width: 0;");
            createGroupBtn.setMaxWidth(Double.MAX_VALUE);
            createGroupBtn.setOnAction(e -> showCreateGroupDialog());
            target.getChildren().add(createGroupBtn);
        }

        List<Group> groups = groupService.findByUserId(CURRENT_USER_ID);
        for (Group group : groups) {
            HBox item = createGroupSidebarItem(group);
            target.getChildren().add(item);
        }
    }

    private HBox createGroupSidebarItem(Group group) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-padding: 7 12; -fx-cursor: hand; -fx-background-radius: 8px;");

        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-padding: 7 12; -fx-cursor: hand; -fx-background-radius: 8px; -fx-background-color: #e2e8f0;"));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-padding: 7 12; -fx-cursor: hand; -fx-background-radius: 8px;"));

        // Colored mini badge (matching MainFX nav badge style)
        StackPane avatar = new StackPane();
        avatar.setMinSize(26, 26); avatar.setMaxSize(26, 26); avatar.setPrefSize(26, 26);
        String[] groupColors = {"#5b8dee", "#5db87a", "#f59e0b", "#e57373"};
        String[] groupLightBgs = {"#e3eeff", "#e6f4ea", "#fffbeb", "#fef2f2"};
        int idx = group.getId() % groupColors.length;
        avatar.setStyle("-fx-background-color: " + groupLightBgs[idx] + "; -fx-background-radius: 7;");
        Label initial = new Label(String.valueOf(group.getName().charAt(0)).toUpperCase());
        initial.setStyle("-fx-text-fill: " + groupColors[idx] + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        avatar.getChildren().add(initial);

        Label name = new Label(group.getName());
        name.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569;");
        HBox.setHgrow(name, Priority.ALWAYS);

        item.getChildren().addAll(avatar, name);
        item.setOnMouseClicked(e -> navigateToGroup(group));

        return item;
    }

    public void navigateToGroup(Group group) {
        try {
            if (mainRoot == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/communication/GroupDetailView.fxml"));
            BorderPane groupView = loader.load();

            GroupDetailController controller = loader.getController();
            controller.setMainController(this);
            controller.setGroup(group);

            mainRoot.setCenter(groupView);
            hideRightSidebar();

        } catch (Exception e) {
            System.err.println("❌ Error navigating to group: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showCreateGroupDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Créer un groupe");
        dialog.setHeaderText("Nouveau groupe d'équipe");

        ButtonType createType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(16));
        content.setPrefWidth(380);

        TextField nameField = new TextField();
        nameField.setPromptText("Nom du groupe *");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description (optionnel)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        TextField imgField = new TextField();
        imgField.setPromptText("URL image (optionnel)");

        content.getChildren().addAll(
                new Label("Nom *"), nameField,
                new Label("Description"), descArea,
                new Label("Image URL"), imgField
        );
        dialog.getDialogPane().setContent(content);

        Platform.runLater(nameField::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
            if (result == createType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Le nom est obligatoire.").showAndWait();
                    return;
                }

                Group newGroup = new Group();
                newGroup.setName(name);
                newGroup.setDescription(descArea.getText().trim());
                newGroup.setImageUrl(imgField.getText().trim().isEmpty() ? null : imgField.getText().trim());
                newGroup.setCreatedById(CURRENT_USER_ID);
                newGroup.setCreatedAt(LocalDateTime.now());

                groupService.createGroup(newGroup);
                loadUserGroups();
                loadVisibilityOptions();
            }
        });
    }

    private void handleCreateGroup() { showCreateGroupDialog(); }

    private void loadGroupFeed(int groupId) {
        publicationsList.getChildren().clear();

        if (!groupService.isMember(groupId, CURRENT_USER_ID)) {
            showAlert("❌ Vous n'êtes pas membre de ce groupe !");
            return;
        }

        List<Publication> groupPubs = publicationService.findByGroupId(groupId);
        for (Publication pub : groupPubs)
            publicationsList.getChildren().add(createPublicationCard(pub));
        feedScrollPane.setVvalue(0);
    }

    // ============================================================
    // SIDEBAR ACTIVE STATE
    // ============================================================
    private void setActiveSidebarBtn(Button btn) {
        String base = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: transparent;";
        for (Button b : sidebarNavButtons) {
            b.setStyle(base);
            b.getProperties().put("state", "inactive");
            if (b.getGraphic() instanceof HBox hb && hb.getChildren().size() >= 3) {
                if (hb.getChildren().get(0) instanceof Region bar)
                    bar.setStyle(bar.getStyle().replace("-fx-opacity: 1;", "-fx-opacity: 0;"));
                if (hb.getChildren().get(2) instanceof Label lbl)
                    lbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px; -fx-font-weight: normal;");
            }
        }
        activeSidebarBtn = btn;
        if (btn != null) {
            String color = (String) btn.getUserData();
            String accent = sbAccent(color);
            btn.setStyle("-fx-background-color: " + accent + "18; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: transparent;");
            btn.getProperties().put("state", "active");
            if (btn.getGraphic() instanceof HBox hb && hb.getChildren().size() >= 3) {
                if (hb.getChildren().get(0) instanceof Region bar)
                    bar.setStyle("-fx-background-color:" + accent + "; -fx-background-radius: 2; -fx-opacity: 1;");
                if (hb.getChildren().get(2) instanceof Label lbl)
                    lbl.setStyle("-fx-text-fill:" + accent + "; -fx-font-size: 12.5px; -fx-font-weight: bold;");
            }
        }
    }

    // ============================================================
    // CHAT FUNCTIONS
    // ============================================================
    public void navigateToChat() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/communication/ChatView.fxml"));
            HBox chatView = loader.load();
            chatController = loader.getController();
            chatController.setMainController(this);

            if (mainRoot != null) {
                mainRoot.setCenter(chatView);
            }
            hideRightSidebar();

        } catch (Exception e) {
            System.err.println("❌ Erreur navigation chat : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showChatNotification(Message msg) {
        showInAppToast("💬 " + msg.getSenderName() + " : " +
                (msg.getContent().length() > 40 ?
                        msg.getContent().substring(0, 40) + "..." :
                        msg.getContent()));
    }

    public void updateChatBadge() {
        if (chatBtn == null) return;
        int unread = new ChatService().countUnread(CURRENT_USER_ID);
        Platform.runLater(() -> {
            if (unread > 0) {
                setSbBtnText(chatBtn, "Messages 🔴" + unread);
            } else {
                setSbBtnText(chatBtn, "Messages");
            }
        });
    }

    private void setSbBtnText(Button btn, String newText) {
        if (btn == null) return;
        if (btn.getGraphic() instanceof HBox hbox) {
            hbox.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .map(n -> (Label) n)
                    .filter(lbl -> !lbl.getStyle().contains("font-size: 13px"))
                    .findFirst()
                    .ifPresent(lbl -> lbl.setText(newText));
        }
    }

    private void onBackgroundMessageReceived(Message msg) {
        Platform.runLater(() -> {
            showChatNotification(msg);
            updateChatBadge();
        });
    }

    // ============================================================
    // AVATAR HELPER
    // ============================================================
    public void loadAvatarIntoLabel(Label initialsLabel, int userId, int size) {
        if (userProfileService == null) return;
        try {
            UserProfile profile = userProfileService.findByUserId(userId);
            if (profile == null || profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) return;

            Image img = new Image(profile.getAvatarUrl(), size, size, true, true, true);

            img.progressProperty().addListener((obs, old, progress) -> {
                if (progress.doubleValue() >= 1.0 && !img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(size);
                    iv.setFitHeight(size);
                    iv.setPreserveRatio(false);

                    double r = size / 2.0;
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(r, r, r);
                    iv.setClip(clip);

                    Platform.runLater(() -> {
                        StackPane parent = (StackPane) initialsLabel.getParent();
                        if (parent != null) {
                            initialsLabel.setVisible(false);
                            parent.setStyle(
                                    "-fx-background-color: transparent;" +
                                            "-fx-border-color: #E5E7EB;" +
                                            "-fx-border-width: 1;" +
                                            "-fx-border-radius: 9999px;" +
                                            "-fx-background-radius: 9999px;"
                            );
                            parent.getChildren().removeIf(n -> n instanceof ImageView);
                            parent.getChildren().add(iv);
                            StackPane.setAlignment(iv, Pos.CENTER);
                        }
                    });
                }
            });
        } catch (Exception e) {
            // Silent fail
        }
    }

    private StackPane createAvatarCircle(User user, int size) {
        StackPane stack = new StackPane();
        stack.setMinSize(size, size);
        stack.setMaxSize(size, size);
        stack.getStyleClass().add("avatar");

        Label initial = new Label(user.getInitials());
        initial.getStyleClass().add("avatar-label");
        initial.setStyle("-fx-font-size: " + (size / 2.5) + "px;");

        stack.getChildren().add(initial);
        StackPane.setAlignment(initial, Pos.CENTER);
        return stack;
    }

    // ============================================================
    // NAVIGATION
    // ============================================================
    public void openUserProfile(int userId) {
        try {
            if (mainRoot == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/communication/UserProfile.fxml"));
            BorderPane profileView = loader.load();

            UserProfileController controller = loader.getController();
            controller.setMainController(this);
            controller.setProfileUserId(userId);

            mainRoot.setCenter(profileView);
            hideRightSidebar();

        } catch (Exception e) {
            System.err.println("❌ Error opening user profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void navigateToMyProfile() { openUserProfile(CURRENT_USER_ID); }

    public void goBackToFeed() {
        try {
            if (mainRoot != null) mainRoot.setCenter(feedPane);
            showRightSidebar();
            loadPublications();
            feedScrollPane.setVvalue(0);
            setActiveSidebarBtn(homeBtn);
        } catch (Exception e) {
            System.err.println("❌ Error going back: " + e.getMessage());
        }
    }

    private void hideRightSidebar() {
        if (mainRoot != null && mainRoot.getRight() != null) {
            savedRightNode = mainRoot.getRight();
            mainRoot.setRight(null);
        }
    }

    private void showRightSidebar() {
        if (mainRoot != null && savedRightNode != null) {
            mainRoot.setRight(savedRightNode);
            savedRightNode = null;
        }
        if (rightSidebar != null) {
            rightSidebar.setVisible(true);
            rightSidebar.setManaged(true);
        }
    }

    private void restoreFeedPane() {
        if (mainRoot != null) mainRoot.setCenter(feedPane);
        showRightSidebar();
    }

    // ============================================================
    // NOTIFICATIONS
    // ============================================================
    private void setupNotificationButton() {
        if (notifBellBtn != null) {
            notifBellBtn.setOnAction(e -> toggleNotificationPopup());
        }
    }

    public void updateNotificationBadge() {
        if (notificationService == null || notifBadgeLabel == null) return;
        int unreadCount = notificationService.countUnread(CURRENT_USER_ID);
        if (unreadCount > 0) {
            notifBadgeLabel.setText(String.valueOf(Math.min(unreadCount, 99)));
            notifBadgeLabel.setVisible(true);
            notifBadgeLabel.setManaged(true);
        } else {
            notifBadgeLabel.setVisible(false);
            notifBadgeLabel.setManaged(false);
        }
    }

    public void updateNetworkBadge() {
        if (followService == null || networkBadgeLabel == null) return;
        int pendingCount = followService.countPendingFollowers(CURRENT_USER_ID);
        if (pendingCount > 0) {
            networkBadgeLabel.setText(String.valueOf(Math.min(pendingCount, 99)));
            networkBadgeLabel.setVisible(true);
            networkBadgeLabel.setManaged(true);
        } else {
            networkBadgeLabel.setVisible(false);
            networkBadgeLabel.setManaged(false);
        }
    }

    public void toggleNotificationPopup() {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide();
            notificationPopup = null;
        } else {
            showNotificationPopup();
        }
    }

    private void showNotificationPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/communication/NotificationPopup.fxml"));
            VBox popupContent = loader.load();
            popupContent.setPrefWidth(360);
            popupContent.setPrefHeight(500);
            popupContent.setMinHeight(100);

            NotificationPopupController controller = loader.getController();
            controller.setMainController(this);
            controller.loadNotifications();

            notificationPopup = new Popup();
            notificationPopup.setAutoHide(true);
            notificationPopup.getContent().add(popupContent);

            if (notifBellBtn != null && notifBellBtn.getScene() != null) {
                javafx.geometry.Bounds bounds = notifBellBtn.localToScreen(notifBellBtn.getBoundsInLocal());
                notificationPopup.show(
                        notifBellBtn.getScene().getWindow(),
                        bounds.getMaxX() - 360,
                        bounds.getMinY() - 508
                );
            }
        } catch (Exception e) {
            System.err.println("❌ Error opening notification popup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void closeNotificationPopup() {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide();
            notificationPopup = null;
        }
    }

    public void navigateToPublication(int publicationId) {
        loadPublications();
        feedScrollPane.setVvalue(0);
    }

    // ============================================================
    // TOAST
    // ============================================================
    public void showInAppToast(String message) {
        if (mainRoot == null) return;

        Label toast = new Label("🔔 " + message);
        toast.setStyle(
                "-fx-background-color: #1F2937; -fx-text-fill: white; " +
                        "-fx-padding: 12 20; -fx-background-radius: 12px; " +
                        "-fx-font-size: 13px; -fx-font-weight: 600; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);"
        );
        toast.setMaxWidth(320);
        toast.setWrapText(true);

        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 24, 24, 0));

        if (mainRoot.getParent() instanceof StackPane) {
            StackPane wrapper = (StackPane) mainRoot.getParent();
            wrapper.getChildren().add(toast);

            javafx.animation.FadeTransition fadeIn =
                    new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();

            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
            pause.setOnFinished(e -> {
                javafx.animation.FadeTransition fadeOut =
                        new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
                fadeOut.setFromValue(1); fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> wrapper.getChildren().remove(toast));
                fadeOut.play();
            });
            pause.play();
        }
    }

    // ============================================================
    // MISC
    // ============================================================
    private void handlePollButton() { showAlert("📊 Fonctionnalité de sondage à venir !"); }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshFeed() { loadPublications(); }
    public void refreshSuggestions() { loadSuggestions(); }
    public UserService getUserService() { return userService; }
    public ReactionService getReactionService() { return reactionService; }
    public CommentaireService getCommentaireService() { return commentaireService; }
    public SavedPostService getSavedPostService() { return savedPostService; }
    public ServicesPublication getPublicationService() { return publicationService; }
    public GroupService getGroupService() { return groupService; }
    public NotificationService getNotificationService() { return notificationService; }
    public int getCurrentUserId() { return UserSession.getInstance().getUserId(); }
    public FollowService getFollowService() { return followService; }
    public void loadUserGroupsSidebar() { loadUserGroups(); }

    public void navigateToNetwork() {
        try {
            if (mainRoot == null) return;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/communication/NetworkView.fxml"));
            BorderPane networkView = loader.load();
            NetworkController controller = loader.getController();
            controller.setMainController(this);
            mainRoot.setCenter(networkView);
            hideRightSidebar();
        } catch (Exception e) {
            System.err.println("❌ Error opening network page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ████  SIDEBAR — matching MainFX light theme  █████████████████████████
    // ══════════════════════════════════════════════════════════════════════

    private VBox sbGroupsList;

    private void buildSidebar() {
        if (sidebarContainer == null) return;

        sidebarContainer.getChildren().clear();
        // ── Light background matching MainFX ─────────────────────────────
        sidebarContainer.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-border-color: transparent #e2e8f0 transparent transparent;" +
                        "-fx-border-width: 0 1 0 0;"
        );
        sidebarContainer.setEffect(new DropShadow(18, 4, 0, Color.rgb(0, 20, 80, 0.06)));

        // ── Logo area (identical to MainFX) ──────────────────────────────
        sidebarContainer.getChildren().add(sbGradientDivider());

        // ── Nav buttons ──────────────────────────────────────────────────
        homeBtn    = sbNavBtn("🏠", "Accueil",     "blue");
        savedBtn   = sbNavBtn("🔖", "Sauvegardés", "yellow");
        popularBtn = sbNavBtn("🔥", "Populaires",  "red");
        profileBtn = sbNavBtn("👤", "Mon Profil",  "blue");
        chatBtn    = sbNavBtn("💬", "Messages",    "blue");
        networkBtn = sbNavBtn("👥", "Réseau",       "green");
        sidebarNavButtons.addAll(java.util.List.of(homeBtn, savedBtn, popularBtn, profileBtn, chatBtn, networkBtn));

        homeBtn.setOnAction(e -> { setActiveSidebarBtn(homeBtn);    goBackToFeed(); });
        savedBtn.setOnAction(e -> { setActiveSidebarBtn(savedBtn);   restoreFeedPane(); loadSavedPublications(); });
        popularBtn.setOnAction(e -> { setActiveSidebarBtn(popularBtn); restoreFeedPane(); loadPopularPublications(); });
        profileBtn.setOnAction(e -> { setActiveSidebarBtn(profileBtn); navigateToMyProfile(); });
        chatBtn.setOnAction(e -> { setActiveSidebarBtn(chatBtn);    navigateToChat(); });
        networkBtn.setOnAction(e -> { setActiveSidebarBtn(networkBtn); navigateToNetwork(); });

        // Network badge overlay
        StackPane networkStack = new StackPane();
        networkStack.getChildren().add(networkBtn);
        networkBadgeLabel = new Label("0");
        networkBadgeLabel.setStyle(
                "-fx-background-color: #5db87a; -fx-text-fill: white; -fx-font-size: 9px;" +
                        "-fx-font-weight: 700; -fx-background-radius: 9999px; -fx-padding: 1 5; -fx-min-width: 16;");
        networkBadgeLabel.setVisible(false);
        networkBadgeLabel.setManaged(false);
        StackPane.setAlignment(networkBadgeLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(networkBadgeLabel, new Insets(4, 10, 0, 0));
        networkStack.getChildren().add(networkBadgeLabel);

        // ── Menu items ────────────────────────────────────────────────────
        VBox menuItems = new VBox(2);
        menuItems.setStyle("-fx-padding: 8 10 18 10; -fx-background-color: #f8fafc;");

        menuItems.getChildren().add(sbSectionLabel("NAVIGATION"));
        menuItems.getChildren().addAll(homeBtn, savedBtn, popularBtn);
        menuItems.getChildren().add(sbGap(4));

        menuItems.getChildren().add(sbSectionLabel("MON ESPACE"));
        menuItems.getChildren().add(profileBtn);
        menuItems.getChildren().add(chatBtn);
        menuItems.getChildren().add(networkStack);
        menuItems.getChildren().add(sbGap(4));

        menuItems.getChildren().add(sbSectionLabel("MES GROUPES"));

        // Groups inner list
        VBox groupsInner = new VBox(3);
        groupsInner.setStyle("-fx-padding: 0 0 0 2;");
        ScrollPane groupsScroll = new ScrollPane(groupsInner);
        groupsScroll.setFitToWidth(true);
        groupsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        groupsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        groupsScroll.setStyle(
                "-fx-background-color: #f8fafc; -fx-background: #f8fafc;" +
                        "-fx-border-color: transparent; -fx-padding: 0;"
        );
        groupsScroll.setPrefHeight(110);
        groupsScroll.setMaxHeight(160);
        VBox.setVgrow(groupsScroll, Priority.ALWAYS);
        menuItems.getChildren().add(groupsScroll);
        this.sbGroupsList = groupsInner;

        ScrollPane menuScroll = new ScrollPane(menuItems);
        menuScroll.setFitToWidth(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menuScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        menuScroll.setStyle(
                "-fx-background-color: #f8fafc; -fx-background: #f8fafc;" +
                        "-fx-border-color: transparent; -fx-padding: 0;"
        );
        VBox.setVgrow(menuScroll, Priority.ALWAYS);

        sidebarContainer.getChildren().add(menuScroll);
        sidebarContainer.getChildren().add(sbGradientDivider());
        sidebarContainer.getChildren().add(buildSidebarFooter());

        setActiveSidebarBtn(homeBtn);
    }

    // ── Footer user card (mirrors MainFX buildUserCard exactly) ───────────
    private VBox buildSidebarFooter() {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        // 4-color accent bar (2px, same as MainFX)
        HBox accentBar = new HBox();
        accentBar.setMinHeight(2); accentBar.setPrefHeight(2);
        for (String col : new String[]{"#e57373", "#5b8dee", "#f5c842", "#5db87a"}) {
            Region seg = new Region(); seg.setPrefHeight(2); HBox.setHgrow(seg, Priority.ALWAYS);
            seg.setStyle("-fx-background-color:" + col + ";");
            accentBar.getChildren().add(seg);
        }

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-padding: 12 14 13 16; -fx-cursor: hand;");

        // Avatar — gradient blue→purple, rounded square, matching MainFX
        String displayName = UserSession.getInstance().getUser();
        if (displayName == null || displayName.isBlank()) displayName = "User";
        String initials = displayName.length() >= 2
                ? displayName.substring(0, 2).toUpperCase()
                : displayName.toUpperCase();

        StackPane avatar = new StackPane();
        avatar.setPrefSize(42, 42); avatar.setMinSize(42, 42); avatar.setMaxSize(42, 42);
        Region avatarBg = new Region();
        avatarBg.setPrefSize(42, 42);
        avatarBg.setStyle(
                "-fx-background-color: linear-gradient(135deg, #3b82f6, #8b5cf6);" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 8, 0, 0, 2);"
        );
        sidebarUserInitials = new Label(initials);
        sidebarUserInitials.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Georgia';"
        );
        avatar.getChildren().addAll(avatarBg, sidebarUserInitials);

        // Info
        VBox info = new VBox(2); info.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(info, Priority.ALWAYS);
        sidebarUserName = new Label(displayName);
        sidebarUserName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String email = UserSession.getInstance().getEmail();
        if (email == null || email.isBlank()) email = displayName.toLowerCase() + "@humania.com";
        sidebarUserEmail = new Label(email);
        sidebarUserEmail.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b;");
        sidebarUserEmail.setMaxWidth(130);

        // Role badge — light-friendly colors matching MainFX
        String role = UserSession.getInstance().getRole() != null ? UserSession.getInstance().getRole() : "USER";
        String[] roleColors = switch (role) {
            case "ADMIN"     -> new String[]{"#dc2626", "#fee2e2"};
            case "MANAGER"   -> new String[]{"#2563eb", "#dbeafe"};
            case "RH"        -> new String[]{"#7c3aed", "#ede9fe"};
            case "FORMATEUR" -> new String[]{"#059669", "#d1fae5"};
            default          -> new String[]{"#64748b", "#f1f5f9"};
        };
        Label roleLbl = new Label(role);
        roleLbl.setStyle(
                "-fx-font-size: 8px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + roleColors[0] + ";" +
                        "-fx-background-color: " + roleColors[1] + ";" +
                        "-fx-padding: 2 6 2 6; -fx-background-radius: 6;" +
                        "-fx-border-color: " + roleColors[0] + "55; -fx-border-radius: 6; -fx-border-width: 1;"
        );
        info.getChildren().addAll(sidebarUserName, sidebarUserEmail, roleLbl);

        // Notification bell with badge
        StackPane notifStack = new StackPane();
        notifBellBtn = new Button("🔔");
        notifBellBtn.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 4;");
        notifBellBtn.setOnMouseEntered(e -> notifBellBtn.setStyle("-fx-background-color: #e2e8f0; -fx-border-width: 0; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 8;"));
        notifBellBtn.setOnMouseExited(e  -> notifBellBtn.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 4;"));
        notifBellBtn.setOnAction(e -> toggleNotificationPopup());
        notifBadgeLabel = new Label("0");
        notifBadgeLabel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 9px;" +
                        "-fx-font-weight: 700; -fx-background-radius: 9999px; -fx-padding: 1 4; -fx-min-width: 16;");
        notifBadgeLabel.setVisible(false);
        notifBadgeLabel.setManaged(false);
        StackPane.setAlignment(notifBadgeLabel, Pos.TOP_RIGHT);
        notifStack.getChildren().addAll(notifBellBtn, notifBadgeLabel);

        // Logout button — 🚪 matching MainFX
        logoutBtn = new Button("🚪");
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 15px; -fx-cursor: hand; -fx-text-fill: #64748b; -fx-border-color: transparent;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle("-fx-background-color: #fee2e2; -fx-font-size: 15px; -fx-cursor: hand; -fx-text-fill: #ef4444; -fx-background-radius: 8; -fx-border-color: transparent;"));
        logoutBtn.setOnMouseExited(e  -> logoutBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 15px; -fx-cursor: hand; -fx-text-fill: #64748b; -fx-border-color: transparent;"));
        logoutBtn.setTooltip(new Tooltip("Déconnexion"));
        logoutBtn.setOnAction(e -> {
            test.MainFX mfx = test.MainFX.getInstance();
            if (mfx != null) mfx.handleLogout();
        });

        // Hover on entire card → profile
        content.setOnMouseClicked(e -> { if (e.getTarget() != logoutBtn && e.getTarget() != notifBellBtn) navigateToMyProfile(); });
        content.setOnMouseEntered(e -> content.setStyle("-fx-padding: 12 14 13 16; -fx-cursor: hand; -fx-background-color: #e2e8f0;"));
        content.setOnMouseExited(e  -> content.setStyle("-fx-padding: 12 14 13 16; -fx-cursor: hand;"));

        content.getChildren().addAll(avatar, info, notifStack, logoutBtn);
        card.getChildren().addAll(accentBar, content);
        return card;
    }

    // ── Nav button (identical to MainFX makeNavButton) ─────────────────────
    private Button sbNavBtn(String icon, String text, String color) {
        // Light tinted badge — soft color background, black emoji
        StackPane iconBadge = new StackPane();
        iconBadge.setPrefSize(28, 28); iconBadge.setMinSize(28, 28); iconBadge.setMaxSize(28, 28);
        Region badgeBg = new Region();
        badgeBg.setPrefSize(28, 28);
        badgeBg.setStyle("-fx-background-color: " + sbAccent(color) + "28; -fx-background-radius: 8;");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");
        iconBadge.getChildren().addAll(badgeBg, iconLbl);

        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px;");

        Region colorBar = new Region();
        colorBar.setPrefSize(3, 16); colorBar.setMinSize(3, 16); colorBar.setMaxSize(3, 16);
        colorBar.setStyle("-fx-background-color: " + sbAccent(color) + "; -fx-background-radius: 2; -fx-opacity: 0;");

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
        return btn;
    }

    // ── Section label ──────────────────────────────────────────────────────
    private Label sbSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 14 8 4 8; -fx-letter-spacing: 1.8;");
        return lbl;
    }

    // ── Gradient divider (light version matching MainFX) ──────────────────
    private Region sbGradientDivider() {
        Region div = new Region(); div.setPrefHeight(1); div.setMaxHeight(1); div.setMinHeight(1);
        div.setStyle("-fx-background-color: linear-gradient(to right, transparent, #e2e8f0 20%, #cbd5e1 50%, #e2e8f0 80%, transparent);");
        return div;
    }

    private Region sbGap(double h) { Region r = new Region(); r.setPrefHeight(h); return r; }

    // ── Color helpers (identical to MainFX) ───────────────────────────────
    private String sbAccent(String c) { return switch(c){ case "red"->"#e57373"; case "blue"->"#5b8dee"; case "yellow"->"#c9a227"; case "green"->"#5db87a"; default->"#5b8dee"; }; }
}