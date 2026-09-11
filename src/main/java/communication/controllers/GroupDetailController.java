package communication.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

import communication.models.*;
import communication.models.enums.*;
import communication.services.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class GroupDetailController {

    @FXML private Label groupNameLabel;
    @FXML private Label groupDescLabel;
    @FXML private Label groupMembersLabel;
    @FXML private Label groupAvatarLabel;
    @FXML private StackPane groupAvatarPane;
    @FXML private Label adminBadge;
    @FXML private Button settingsBtn;
    @FXML private Button backBtn;
    @FXML private Button groupPublishBtn;
    @FXML private Button imageBtn;
    @FXML private TextField postContentField;
    @FXML private VBox groupPublicationsList;
    @FXML private VBox membersList;
    @FXML private ScrollPane groupFeedScrollPane;
    @FXML private Label currentUserAvatar;
    @FXML private VBox createPostBox;
    @FXML private HBox imagePreviewBox;
    @FXML private javafx.scene.image.ImageView imagePreview;
    @FXML private Label imageUrlLabel;
    @FXML private Button removeImageBtn;
    @FXML private Button pollBtn;
    private FeedController feedController;
    private GroupService groupService;
    private ServicesPublication publicationService;
    private UserService userService;
    private UserProfileService userProfileService;
    private NotificationService notificationService;
    private ReactionService reactionService;
    private CommentaireService commentaireService;
    private SavedPostService savedPostService;


    private Group currentGroup;
    private int currentUserId;
    private User currentUser;
    private final GroqService groqService = new GroqService();
    private final PollService pollService = new PollService();

    public void setMainController(FeedController controller) {
        this.feedController = controller;
        this.groupService = controller.getGroupService();
        this.publicationService = controller.getPublicationService();
        this.userService = controller.getUserService();
        this.notificationService = controller.getNotificationService();
        this.reactionService = controller.getReactionService();
        this.commentaireService = controller.getCommentaireService();
        this.savedPostService = controller.getSavedPostService();
        this.currentUserId = controller.getCurrentUserId();
        this.userProfileService = new UserProfileService();
        this.currentUser = userService.findById(currentUserId);
    }

    public void setGroup(Group group) {
        this.currentGroup = group;
        loadGroupInfo();
        loadMembers();
        loadGroupFeed();
        setupActions();
    }

    // ============================================================
    // HEADER
    // ============================================================
    private void loadGroupInfo() {
        groupNameLabel.setText(currentGroup.getName());
        groupDescLabel.setText(currentGroup.getDescription() != null ? currentGroup.getDescription() : "");
        groupMembersLabel.setText(currentGroup.getMemberCount() + " membres");

        // Avatar initiale du groupe
        groupAvatarLabel.setText(String.valueOf(currentGroup.getName().charAt(0)).toUpperCase());
        // ✅ Couleur de l'avatar du groupe selon son ID
        String[] colors = {"#3D7EE8", "#2DAA63", "#F5A623", "#E8392A"};
        String color = colors[currentGroup.getId() % colors.length];
        groupAvatarPane.setStyle(
                "-fx-background-color: " + color + "; -fx-background-radius: 9999px;"
        );

        // Badge + bouton settings si admin/créateur
        boolean isAdmin = groupService.isAdmin(currentGroup.getId(), currentUserId);
        boolean isCreator = currentGroup.getCreatedById() == currentUserId;

        if (isAdmin || isCreator) {
            adminBadge.setVisible(true);
            adminBadge.setManaged(true);
            adminBadge.setStyle(
                    "-fx-background-color: #EDFBF4; -fx-text-fill: #2DAA63; " +
                            "-fx-font-size: 11px; -fx-font-weight: 700; " +
                            "-fx-padding: 3 10; -fx-background-radius: 9999px;"
            );
            settingsBtn.setVisible(true);
            settingsBtn.setManaged(true);
        }

        // Avatar utilisateur courant dans create post
        if (currentUser != null) {
            currentUserAvatar.setText(currentUser.getInitials());
            if (userProfileService != null) {
                feedController.loadAvatarIntoLabel(currentUserAvatar, currentUserId, 44);
            }
        }
    }

    // ============================================================
    // ACTIONS
    private void setupActions() {
        backBtn.setOnAction(e -> feedController.goBackToFeed());
        groupPublishBtn.setOnAction(e -> handlePublish());

        // ✅ Image button — avec preview comme le feed principal
        imageBtn.setOnAction(e -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("🖼️ Ajouter une image");
            ButtonType addBtnType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);
            TextField urlField = new TextField();
            urlField.setPromptText("https://example.com/image.jpg");
            urlField.setPrefWidth(360);
            dialog.getDialogPane().setContent(urlField);
            javafx.application.Platform.runLater(urlField::requestFocus);
            dialog.setResultConverter(b -> b == addBtnType ? urlField.getText() : null);
            dialog.showAndWait().ifPresent(url -> {
                if (url != null && !url.isBlank()) {
                    String trimmed = url.trim();
                    postContentField.setUserData(trimmed);

                    // ✅ Affiche la preview
                    try {
                        javafx.scene.image.Image img = new javafx.scene.image.Image(trimmed, true);
                        imagePreview.setImage(img);
                        imageUrlLabel.setText(trimmed);
                        imagePreviewBox.setVisible(true);
                        imagePreviewBox.setManaged(true);
                    } catch (Exception ex) {
                        imageUrlLabel.setText(trimmed);
                        imagePreviewBox.setVisible(true);
                        imagePreviewBox.setManaged(true);
                    }
                }
            });
        });

        // ✅ Remove image
        removeImageBtn.setOnAction(e -> {
            imagePreviewBox.setVisible(false);
            imagePreviewBox.setManaged(false);
            imagePreview.setImage(null);
            imageUrlLabel.setText("");
            postContentField.setUserData(null);
        });

        // ✅ Sondage — non disponible dans les groupes
        pollBtn.setOnAction(e -> openPollCreator());
        // ✅ Bouton IA groupe
        Button aiBtn = new Button("✨ IA");
        aiBtn.setStyle(
                "-fx-background-color: #EEF4FF; -fx-text-fill: #3D7EE8; " +
                        "-fx-font-weight: 700; -fx-font-size: 12px; " +
                        "-fx-background-radius: 8px; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 6 12;"
        );
        aiBtn.setOnAction(e -> handleImproveTextGroupe());

        if (pollBtn.getParent() instanceof HBox) {
            HBox btnBar = (HBox) pollBtn.getParent();
            btnBar.getChildren().add(aiBtn);
        }

        settingsBtn.setOnAction(e -> openGroupSettings());
    }
    //==============================================================
    // POLL OPTIONS
    //==============================================================
    private void openPollCreator() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📊 Créer un sondage");
        dialog.setHeaderText(null);
        ButtonType createType = new ButtonType("📊 Publier le sondage", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFF;");

        VBox content = new VBox(16);
        content.setPadding(new javafx.geometry.Insets(20));

        // ── Question ──
        VBox questionBox = new VBox(6);
        Label qLabel = new Label("📝 Question du sondage *");
        qLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #111827;");
        TextField questionField = new TextField();
        questionField.setPromptText("Ex: Quel jour préférez-vous pour la réunion ?");
        questionField.setStyle(
                "-fx-font-size: 13px; -fx-padding: 10 14; " +
                        "-fx-background-radius: 8px; -fx-border-color: #E5E7EB; " +
                        "-fx-border-radius: 8px; -fx-border-width: 1.5;"
        );
        questionBox.getChildren().addAll(qLabel, questionField);

        // ── Options ──
        VBox optionsSection = new VBox(8);
        Label optLabel = new Label("🗳️ Options de réponse (min. 2, max. 6) *");
        optLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #111827;");

        VBox optionsBox = new VBox(8);
        List<TextField> optionFields = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            optionsBox.getChildren().add(createOptionRow(optionFields, i + 1));
        }

        Button addOptionBtn = new Button("＋ Ajouter une option");
        addOptionBtn.setStyle(
                "-fx-background-color: #EEF4FF; -fx-text-fill: #3D7EE8; " +
                        "-fx-font-weight: 700; -fx-font-size: 12px; -fx-cursor: hand; " +
                        "-fx-border-width: 0; -fx-background-radius: 8px; -fx-padding: 6 14;"
        );
        addOptionBtn.setOnAction(e -> {
            if (optionFields.size() < 6) {
                optionsBox.getChildren().add(createOptionRow(optionFields, optionFields.size() + 1));
                if (optionFields.size() == 6) {
                    addOptionBtn.setDisable(true);
                    addOptionBtn.setText("Maximum 6 options atteint");
                }
            }
        });

        optionsSection.getChildren().addAll(optLabel, optionsBox, addOptionBtn);

        // ── Options avancées (admin seulement) ──
        boolean isAdmin = groupService.isAdmin(currentGroup.getId(), currentUserId)
                || currentGroup.getCreatedById() == currentUserId;

        CheckBox anonymeCheck = new CheckBox("🔒 Votes anonymes");
        anonymeCheck.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #1A5C38; -fx-font-weight: 700;"
        );

        CheckBox multipleCheck = new CheckBox("☑️ Permettre plusieurs réponses");
        multipleCheck.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #1A5C38; -fx-font-weight: 700;"
        );
        DatePicker expDatePicker = new DatePicker();
        expDatePicker.setPromptText("Aucune expiration");
        expDatePicker.setStyle("-fx-font-size: 12px;");

        // ✅ Bloque les dates passées
        expDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(java.time.LocalDate.now().plusDays(1))) {
                    setDisable(true);
                    setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #D1D5DB;");
                }
            }
        });

        content.getChildren().addAll(questionBox, optionsSection);

        if (isAdmin) {
            Separator sep = new Separator();
            sep.setStyle("-fx-opacity: 0.5;");

            VBox advancedBox = new VBox(10);
            advancedBox.setStyle(
                    "-fx-background-color: #D1FAE5; -fx-background-radius: 10px; " +
                            "-fx-border-color: #2DAA63; -fx-border-width: 1.5; " +
                            "-fx-border-radius: 10px; -fx-padding: 14;"
            );
            Label advLabel = new Label("⚙️ Options avancées — Admin");
            advLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #2DAA63;");

            Label expLabel = new Label("📅 Date d'expiration (optionnel)");
            expLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151; -fx-font-weight: 600;");

            advancedBox.getChildren().addAll(advLabel, anonymeCheck, multipleCheck, expLabel, expDatePicker);
            content.getChildren().addAll(sep, advancedBox);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(480);

        dialog.getDialogPane().setContent(scrollPane);
        javafx.application.Platform.runLater(questionField::requestFocus);

        // ✅ Style bouton Publier
        javafx.application.Platform.runLater(() -> {
            Button okBtn = (Button) dialog.getDialogPane().lookupButton(createType);
            if (okBtn != null) {
                okBtn.setStyle(
                        "-fx-background-color: #3D7EE8; -fx-text-fill: white; " +
                                "-fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 20;"
                );
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != createType) return;

            // ✅ Validations AVANT tout
            String question = questionField.getText().trim();
            if (question.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "⚠️ La question est obligatoire !").showAndWait();
                return;
            }

            List<String> options = new ArrayList<>();
            for (TextField f : optionFields) {
                if (!f.getText().trim().isEmpty()) options.add(f.getText().trim());
            }
            if (options.size() < 2) {
                new Alert(Alert.AlertType.WARNING, "⚠️ Minimum 2 options requises !").showAndWait();
                return;
            }

            java.time.LocalDate expDate = isAdmin ? expDatePicker.getValue() : null;
            if (expDate != null && expDate.isBefore(java.time.LocalDate.now().plusDays(1))) {
                new Alert(Alert.AlertType.WARNING,
                        "⚠️ La date d'expiration doit être au moins demain !").showAndWait();
                return;
            }

            // ✅ Créer la publication
            Publication pub = new Publication();
            pub.setContenu("📊 " + question);
            pub.setAuthorId(currentUserId);
            pub.setDateCreation(LocalDateTime.now());
            pub.setStatut(Statut.ACTIF);
            pub.setVisibility("GROUP");
            pub.setGroupId(currentGroup.getId());
            publicationService.add(pub);

            // ✅ Créer le poll
            Poll poll = new Poll(pub.getId(), question, currentUserId);
            if (isAdmin) {
                poll.setAnonymous(anonymeCheck.isSelected());
                poll.setAllowMultiple(multipleCheck.isSelected());
                if (expDate != null) poll.setExpiresAt(expDate.atTime(23, 59));
            }
            pollService.createPoll(poll, options);

            loadGroupFeed();
            groupFeedScrollPane.setVvalue(0);
        });
    }

    // ✅ Helper — crée une ligne d'option avec numéro
    private HBox createOptionRow(List<TextField> optionFields, int number) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label num = new Label(number + ".");
        num.setStyle("-fx-font-weight: 700; -fx-text-fill: #3D7EE8; -fx-font-size: 13px; -fx-min-width: 20;");

        TextField field = new TextField();
        field.setPromptText("Option " + number);
        field.setStyle(
                "-fx-font-size: 13px; -fx-padding: 8 12; " +
                        "-fx-background-radius: 8px; -fx-border-color: #E5E7EB; " +
                        "-fx-border-radius: 8px; -fx-border-width: 1.5;"
        );
        HBox.setHgrow(field, Priority.ALWAYS);
        optionFields.add(field);

        row.getChildren().addAll(num, field);
        return row;
    }

    // ============================================================
    // PUBLISH DANS LE GROUPE
    // ============================================================
    private void handlePublish() {
        String content = postContentField.getText().trim();
        String imageUrl = (String) postContentField.getUserData();

        if (content.isEmpty() && imageUrl == null) {
            new Alert(Alert.AlertType.WARNING, "Écrivez quelque chose !").showAndWait();
            return;
        }

        Publication pub = new Publication();
        pub.setContenu(content);
        pub.setAuthorId(currentUserId);
        pub.setDateCreation(LocalDateTime.now());
        pub.setStatut(Statut.ACTIF);
        pub.setVisibility("GROUP");
        pub.setGroupId(currentGroup.getId());
        pub.setImageUrl(imageUrl);

        publicationService.add(pub);

        postContentField.clear();
        postContentField.setUserData(null);
        loadGroupFeed();
        groupFeedScrollPane.setVvalue(0);
    }
    //============================================================
    //IA ASSISANTCE
    //=============================================================
    private void handleImproveTextGroupe() {
        String content = postContentField.getText().trim();
        if (content.isEmpty()) {
            openAIGenerateDialogGroupe();
            return;
        }

        // ✅ Loading stage non bloquant
        javafx.stage.Stage loadingStage = new javafx.stage.Stage();
        loadingStage.initModality(javafx.stage.Modality.NONE);
        loadingStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        loadingStage.setAlwaysOnTop(true);
        Label loadingLabel = new Label("✨ Amélioration en cours...");
        loadingLabel.setStyle(
                "-fx-padding: 20 32; -fx-font-size: 13px; -fx-font-weight: 600;"
        );
        VBox loadingBox = new VBox(loadingLabel);
        loadingBox.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );
        loadingStage.setScene(new javafx.scene.Scene(loadingBox));
        loadingStage.show();

        new Thread(() -> {
            String improved = groqService.improveText(content);
            javafx.application.Platform.runLater(() -> {
                loadingStage.close();
                if (improved == null || improved.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "❌ Erreur IA. Vérifie ta clé Groq.").showAndWait();
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
    private void openAIGenerateDialogGroupe() {
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
        promptField.setPromptText("Ex: Annoncer une formation sur Excel vendredi à 14h...");
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
        javafx.application.Platform.runLater(promptField::requestFocus);

        javafx.application.Platform.runLater(() -> {
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
                new Alert(Alert.AlertType.WARNING, "⚠️ Décrivez d'abord le sujet !").showAndWait();
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
                javafx.application.Platform.runLater(() -> {
                    loadingStage.close();
                    if (generated == null || generated.isEmpty()) {
                        new Alert(Alert.AlertType.ERROR, "❌ Erreur IA. Vérifie ta connexion.").showAndWait();
                        return;
                    }
                    // ✅ Insère dans le postContentField du GROUPE
                    postContentField.setText(generated);
                    postContentField.requestFocus();
                });
            }).start();
        });
    }

    // ============================================================
    // FEED DU GROUPE
    // ============================================================
    private void loadGroupFeed() {
        groupPublicationsList.getChildren().clear();
        List<Publication> publications = publicationService.findByGroupId(currentGroup.getId());

        if (publications.isEmpty()) {
            Label empty = new Label("Aucune publication dans ce groupe.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 24;");
            groupPublicationsList.getChildren().add(empty);
            return;
        }

        for (Publication pub : publications) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/communication/PostCard.fxml"));
                VBox card = loader.load();
                PostCardController controller = loader.getController();
                controller.setMainController(feedController);
                controller.setPublication(pub);
                controller.setCurrentUserId(currentUserId);
                controller.setOnDeleteCallback(() -> loadGroupFeed());
                controller.initialize();
                groupPublicationsList.getChildren().add(card);
            } catch (Exception e) {
                System.err.println("❌ Erreur PostCard groupe : " + e.getMessage());
            }
        }
    }


    // ============================================================
    // MEMBRES
    // ============================================================
    private void loadMembers() {
        membersList.getChildren().clear();
        List<GroupMember> members = groupService.getMembers(currentGroup.getId());

        for (GroupMember member : members) {
            User user = userService.findById(member.getUserId());
            if (user == null) continue;

            HBox card = new HBox(10);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-padding: 6 0;");

            // Avatar
            StackPane avatar = new StackPane();
            avatar.setMinSize(36, 36);
            avatar.setMaxSize(36, 36);
            avatar.setStyle("-fx-background-color: #7C3AED; -fx-background-radius: 9999px;");
            Label initials = new Label(user.getInitials());
            initials.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
            avatar.getChildren().add(initials);

            // Infos
            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(user.getFullName());
            name.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #111827;");
            Label role = new Label(member.getRole().equals("ADMIN") ? "👑 Admin" : "@" + user.getUsername());
            role.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
            info.getChildren().addAll(name, role);

            card.getChildren().addAll(avatar, info);

            // Bouton kick — visible seulement pour admin, pas sur soi-même
            boolean isAdmin = groupService.isAdmin(currentGroup.getId(), currentUserId);
            if (isAdmin && member.getUserId() != currentUserId) {
                Button kickBtn = new Button("✕");
                kickBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 0;");
                kickBtn.setTooltip(new Tooltip("Retirer du groupe"));
                kickBtn.setOnAction(e -> {
                    groupService.removeMember(currentGroup.getId(), member.getUserId());
                    loadMembers();
                    // Refresh member count
                    Group refreshed = groupService.findById(currentGroup.getId());
                    if (refreshed != null) {
                        currentGroup = refreshed;
                        groupMembersLabel.setText(currentGroup.getMemberCount() + " membres");
                    }
                });
                card.getChildren().add(kickBtn);
            }

            membersList.getChildren().add(card);
        }
    }

    // ============================================================
    // SETTINGS GROUPE (dialog)
    // ============================================================
    private void openGroupSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("⚙️ Gérer le groupe");
        dialog.setHeaderText(currentGroup.getName());

        ButtonType saveType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteType = new ButtonType("Supprimer le groupe", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, deleteType, ButtonType.CANCEL);

        Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteType);
        deleteBtn.setStyle("-fx-text-fill: #EF4444;");

        VBox content = new VBox(16);
        content.setPadding(new javafx.geometry.Insets(16));
        content.setPrefWidth(420);

        // Nom
        Label nameLabel = new Label("Nom du groupe");
        nameLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
        TextField nameField = new TextField(currentGroup.getName());

        // Description
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
        TextArea descArea = new TextArea(currentGroup.getDescription());
        descArea.setWrapText(true);
        descArea.setPrefRowCount(3);

        // Image URL
        Label imgLabel = new Label("Image URL (optionnel)");
        imgLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
        TextField imgField = new TextField(
                currentGroup.getImageUrl() != null ? currentGroup.getImageUrl() : "");

        // ✅ AJOUT MEMBRE
        Separator sep = new Separator();
        Label addMemberLabel = new Label("Ajouter un membre");
        addMemberLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");

        HBox addMemberBox = new HBox(8);
        addMemberBox.setAlignment(Pos.CENTER_LEFT);
        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur (@username)");
        HBox.setHgrow(usernameField, Priority.ALWAYS);

        Button addBtn = new Button("Ajouter");
        addBtn.setStyle(
                "-fx-background-color: #7C3AED; -fx-text-fill: white; " +
                        "-fx-font-weight: 600; -fx-padding: 8 16; " +
                        "-fx-background-radius: 8px; -fx-cursor: hand; -fx-border-width: 0;"
        );

        Label addFeedback = new Label();
        addFeedback.setStyle("-fx-font-size: 12px;");

        addBtn.setOnAction(e -> {
            String username = usernameField.getText().trim().replace("@", "");
            if (username.isEmpty()) {
                addFeedback.setText("⚠️ Entrez un nom d'utilisateur.");
                addFeedback.setStyle("-fx-text-fill: #D97706; -fx-font-size: 12px;");
                return;
            }

            User target = userService.findByUsername(username);
            if (target == null) {
                addFeedback.setText("❌ Utilisateur introuvable : @" + username);
                addFeedback.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
                return;
            }

            if (groupService.isMember(currentGroup.getId(), target.getId())) {
                addFeedback.setText("⚠️ @" + username + " est déjà membre.");
                addFeedback.setStyle("-fx-text-fill: #D97706; -fx-font-size: 12px;");
                return;
            }

            groupService.addMember(currentGroup.getId(), target.getId(), "MEMBER");
            addFeedback.setText("✅ @" + username + " ajouté au groupe !");
            addFeedback.setStyle("-fx-text-fill: #059669; -fx-font-size: 12px;");
            usernameField.clear();

            // Refresh membres + compteur
            Group refreshed = groupService.findById(currentGroup.getId());
            if (refreshed != null) {
                currentGroup = refreshed;
                groupMembersLabel.setText(currentGroup.getMemberCount() + " membres");
            }
            loadMembers();
            Publication welcomePost = new Publication();
            welcomePost.setContenu("👋 Bienvenue à " + target.getFullName() +
                    " qui vient de rejoindre le groupe ! Souhaitons-lui la bienvenue 🎉");
            welcomePost.setAuthorId(currentUserId);          // posté par l'admin
            welcomePost.setDateCreation(LocalDateTime.now());
            welcomePost.setStatut(Statut.ACTIF);
            welcomePost.setVisibility("GROUP");
            welcomePost.setGroupId(currentGroup.getId());
            publicationService.add(welcomePost);

// Refresh le feed du groupe
            loadGroupFeed();
        });

        addMemberBox.getChildren().addAll(usernameField, addBtn);
        content.getChildren().addAll(
                nameLabel, nameField,
                descLabel, descArea,
                imgLabel, imgField,
                sep, addMemberLabel, addMemberBox, addFeedback
        );
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result == saveType) {
                String newName = nameField.getText().trim();
                if (newName.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Le nom ne peut pas être vide.").showAndWait();
                    return;
                }
                currentGroup.setName(newName);
                currentGroup.setDescription(descArea.getText().trim());
                currentGroup.setImageUrl(imgField.getText().trim().isEmpty() ? null : imgField.getText().trim());
                groupService.updateGroup(currentGroup);
                groupNameLabel.setText(currentGroup.getName());
                groupDescLabel.setText(currentGroup.getDescription() != null ? currentGroup.getDescription() : "");
                groupAvatarLabel.setText(String.valueOf(currentGroup.getName().charAt(0)).toUpperCase());
                feedController.loadUserGroupsSidebar();
                System.out.println("✅ Groupe mis à jour : " + currentGroup.getName());

            } else if (result == deleteType) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer définitivement ce groupe et toutes ses publications ?");
                confirm.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        groupService.deleteGroup(currentGroup.getId());
                        feedController.loadUserGroupsSidebar();
                        feedController.goBackToFeed();
                    }
                });
            }
        });
    }
}