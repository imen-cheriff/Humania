package communication.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;

import communication.services.*;
import communication.models.*;
import communication.models.enums.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PostCardController {

    @FXML
    private Label authorAvatar;
    @FXML
    private Label authorName;
    @FXML
    private Label postMeta;
    @FXML
    private Button menuBtn;
    @FXML
    private Label postContent;
    @FXML
    private VBox imageContainer;
    @FXML
    private ImageView postImage;
    @FXML
    private HBox shareHeader;
    @FXML
    private Label sharerAvatar;
    @FXML
    private Label sharerName;
    @FXML
    private Label shareTime;
    @FXML
    private VBox shareMessageBox;
    @FXML
    private Label shareMessage;
    @FXML
    private VBox originalPostContainer;
    @FXML
    private VBox originalPostBox;
    @FXML
    private Label originalAuthorAvatar;
    @FXML
    private Label originalAuthorName;
    @FXML
    private Label originalPostMeta;
    @FXML
    private Label originalPostContent;
    @FXML
    private ImageView originalPostImage;
    @FXML
    private Label likesLabel;
    @FXML
    private Label commentsLabel;
    @FXML
    private Label sharesLabel;
    @FXML
    private Button likeBtn;
    @FXML
    private Button commentBtn;
    @FXML
    private Button shareBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private VBox commentsSection;
    @FXML
    private TextField commentInput;
    @FXML
    private Button commentSendBtn;
    @FXML
    private VBox commentsList;
    @FXML
    private Button viewMoreCommentsBtn;
    @FXML
    private Label currentUserAvatar;
    @FXML
    private HBox normalHeader;
    @FXML
    private Button shareMenuBtn;

    private FeedController feedController;
    private Publication publication;
    private int currentUserId;
    private UserService userService;
    private UserProfileService userProfileService;
    private NotificationService notificationService;
    private ReactionService reactionService;
    private CommentaireService commentaireService;
    private SavedPostService savedPostService;
    private ServicesPublication publicationService;
    private final GiphyService giphyService = new GiphyService();
    private String pendingGifUrl = null;
    private final GroqService groqService = new GroqService();
    private final PollService pollService = new PollService();

    public void setMainController(FeedController feedController) {
        this.feedController = feedController;
        this.userService = feedController.getUserService();
        this.reactionService = feedController.getReactionService();
        this.commentaireService = feedController.getCommentaireService();
        this.savedPostService = feedController.getSavedPostService();
        this.publicationService = feedController.getPublicationService();
        this.notificationService = feedController.getNotificationService();
        this.userProfileService = new UserProfileService();
    }

    public void setPublication(Publication pub) {
        this.publication = pub;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    /**
     * Sanitize username: if it looks like a file path/URL, derive a clean display name.
     */
    private String sanitizeUsername(User user) {
        String u = user.getUsername();
        if (u != null && !u.contains("://") && !u.contains("file:") && !u.contains("/") && !u.contains("\\")) {
            return u;
        }
        // Fallback: email prefix or name-based handle
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf("@"));
        }
        return user.getFullName().toLowerCase().replace(" ", ".");
    }

    public void initialize() {
        if (publication == null)
            return;

        User author = userService.findById(publication.getAuthorId());
        if (author == null)
            return;

        // Check if it's a shared post
        if (publication.isShared()) {
            displaySharedPost(publication, author);
        } else {
            displayNormalPost(publication, author);

        }

        // Stats
        updateStats();

        // Actions
        setupActions();
        //  Charge le poll si la publication en a un
        loadPollIfExists();

        // Comments
        loadComments();
        setupCommentInput();
    }
    //==============================================================
    // load if poll exist
    //===============================================================
    private void loadPollIfExists() {
        Poll poll = pollService.findByPublicationId(publication.getId());
        if (poll == null) return;

        List<PollOption> options = pollService.getOptions(poll.getId());
        if (options.isEmpty()) return;

        // Cache le contenu texte normal
        if (postContent != null) {
            postContent.setVisible(false);
            postContent.setManaged(false);
        }

        VBox pollBox = buildPollUI(poll, options);

        // Insère le pollBox dans le parent de postContent
        if (postContent != null && postContent.getParent() instanceof VBox) {
            VBox parent = (VBox) postContent.getParent();
            int idx = parent.getChildren().indexOf(postContent);
            parent.getChildren().add(idx + 1, pollBox);
        }
    }

    private VBox buildPollUI(Poll poll, List<PollOption> options) {
        VBox pollBox = new VBox(10);
        pollBox.setStyle(
                "-fx-background-color: #F8FAFF; -fx-background-radius: 12px; " +
                        "-fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 12px; " +
                        "-fx-padding: 16;"
        );

        // ── Header ──
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label pollIcon = new Label("📊");
        pollIcon.setStyle("-fx-font-size: 16px;");
        Label pollQuestion = new Label(poll.getQuestion());
        pollQuestion.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #111827;");
        pollQuestion.setWrapText(true);
        HBox.setHgrow(pollQuestion, Priority.ALWAYS);
        header.getChildren().addAll(pollIcon, pollQuestion);

        // Badges
        HBox badges = new HBox(6);
        if (poll.isAnonymous()) {
            Label anonBadge = new Label("🔒 Anonyme");
            anonBadge.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280; " +
                    "-fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 9999px;");
            badges.getChildren().add(anonBadge);
        }
        if (poll.isAllowMultiple()) {
            Label multiBadge = new Label("☑️ Choix multiples");
            multiBadge.setStyle("-fx-background-color: #EEF4FF; -fx-text-fill: #3D7EE8; " +
                    "-fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 9999px;");
            badges.getChildren().add(multiBadge);
        }
        if (poll.isExpired()) {
            Label expBadge = new Label("⏱️ Expiré");
            expBadge.setStyle("-fx-background-color: #FEF2F1; -fx-text-fill: #E8392A; " +
                    "-fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 9999px;");
            badges.getChildren().add(expBadge);
        } else if (poll.getExpiresAt() != null) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDateTime.now(), poll.getExpiresAt());
            Label expBadge = new Label("📅 " + (daysLeft == 0 ? "Expire aujourd'hui" : daysLeft + "j restants"));
            expBadge.setStyle("-fx-background-color: #FFFBF0; -fx-text-fill: #F5A623; " +
                    "-fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 9999px;");
            badges.getChildren().add(expBadge);
        }

        // ── Options ──
        int totalVotes = poll.getTotalVotes() != null ? poll.getTotalVotes() : 0;
        List<Integer> votedIds = pollService.getVotedOptionIds(poll.getId(), currentUserId);
        boolean hasVoted = !votedIds.isEmpty();
        boolean canVote = poll.isOpen();

        VBox optionsBox = new VBox(8);
        for (PollOption option : options) {
            optionsBox.getChildren().add(
                    buildOptionRow(poll, option, totalVotes, votedIds, canVote, optionsBox, options)
            );
        }

        // ── Footer ──
        Label totalLabel = new Label(totalVotes + " vote" + (totalVotes > 1 ? "s" : ""));
        totalLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");

        // Bouton voir les votants (admin seulement)
        boolean isAdmin = poll.getCreatedById() != null && poll.getCreatedById() == currentUserId;
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().add(totalLabel);

        if (isAdmin && !poll.isAnonymous()) {
            Button votersBtn = new Button("👁 Voir les votants");
            votersBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #3D7EE8; " +
                            "-fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 0; -fx-font-weight: 600;"
            );
            votersBtn.setOnAction(e -> showVotersDialog(poll, options));
            footer.getChildren().add(votersBtn);
        }

        pollBox.getChildren().addAll(header);
        if (!badges.getChildren().isEmpty()) pollBox.getChildren().add(badges);
        pollBox.getChildren().addAll(optionsBox, footer);

        return pollBox;
    }

    private HBox buildOptionRow(Poll poll, PollOption option, int totalVotes,
                                List<Integer> votedIds, boolean canVote,
                                VBox optionsBox, List<PollOption> allOptions) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        boolean isVoted = votedIds.contains(option.getId());
        int votes = option.getVoteCount() != null ? option.getVoteCount() : 0;
        double percent = totalVotes > 0 ? (votes * 100.0 / totalVotes) : 0;

        // Bouton vote
        ToggleButton voteBtn = new ToggleButton(
                option.getOptionText());
        voteBtn.setSelected(isVoted);
        voteBtn.setDisable(!canVote);
        voteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(voteBtn, Priority.ALWAYS);
        voteBtn.setStyle(
                "-fx-background-color: " + (isVoted ? "#3D7EE8" : "white") + "; " +
                        "-fx-text-fill: " + (isVoted ? "white" : "#374151") + "; " +
                        "-fx-font-size: 13px; -fx-font-weight: " + (isVoted ? "700" : "400") + "; " +
                        "-fx-background-radius: 8px; -fx-cursor: hand; " +
                        "-fx-border-color: " + (isVoted ? "#3D7EE8" : "#E5E7EB") + "; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 8px; -fx-padding: 8 12;"
        );

        // Barre de progression
        ProgressBar bar = new ProgressBar(percent / 100);
        bar.setPrefWidth(80);
        bar.setPrefHeight(6);
        bar.setStyle(isVoted ?
                "-fx-accent: #3D7EE8;" : "-fx-accent: #E5E7EB;");

        Label percentLabel = new Label(String.format("%.0f%%", percent));
        percentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-min-width: 32;");

        // Action vote
        voteBtn.setOnAction(e -> {
            boolean added = pollService.vote(poll.getId(), option.getId(), currentUserId);
            // Refresh le poll UI
            Poll refreshed = pollService.findById(poll.getId());
            List<PollOption> refreshedOptions = pollService.getOptions(poll.getId());
            if (refreshed != null) {
                // Rebuild
                VBox newPollBox = buildPollUI(refreshed, refreshedOptions);
                javafx.scene.Node current = optionsBox.getParent();
                if (current instanceof VBox) {
                    VBox grandParent = (VBox) current;
                    int idx = grandParent.getChildren().indexOf(current);
                    // Trouve le pollBox dans le parent de postContent
                    if (postContent != null && postContent.getParent() instanceof VBox) {
                        VBox pp = (VBox) postContent.getParent();
                        pp.getChildren().removeIf(n -> n instanceof VBox &&
                                n.getStyle().contains("F8FAFF"));
                        int insertIdx = pp.getChildren().indexOf(postContent) + 1;
                        pp.getChildren().add(insertIdx, newPollBox);
                    }
                }
            }
        });

        row.getChildren().addAll(voteBtn, bar, percentLabel);
        return row;
    }

    private void showVotersDialog(Poll poll, List<PollOption> options) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("👁 Votants");
        dialog.setHeaderText("Qui a voté pour quoi ?");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(380);

        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(16));

        for (PollOption option : options) {
            Label optLabel = new Label(option.getOptionText() + " — " +
                    option.getVoteCount() + " vote(s)");
            optLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #3D7EE8;");

            VBox votersList = new VBox(4);
            List<PollVote> votes = pollService.getVoters(poll.getId(), option.getId());

            if (votes.isEmpty()) {
                Label none = new Label("  Aucun vote");
                none.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
                votersList.getChildren().add(none);
            } else {
                for (PollVote vote : votes) {
                    User voter = userService.findById(vote.getUserId());
                    Label voterLabel = new Label("  👤 " +
                            (voter != null ? voter.getFullName() : "Utilisateur #" + vote.getUserId()));
                    voterLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
                    votersList.getChildren().add(voterLabel);
                }
            }

            content.getChildren().addAll(optLabel, votersList, new Separator());
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(350);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private void displayNormalPost(Publication pub, User author) {
        // Hide share elements
        if (shareHeader != null) {
            shareHeader.setVisible(false);
            shareHeader.setManaged(false);
        }
        if (shareMessageBox != null) {
            shareMessageBox.setVisible(false);
            shareMessageBox.setManaged(false);
        }
        if (originalPostContainer != null) {
            originalPostContainer.setVisible(false);
            originalPostContainer.setManaged(false);
        }

        // Author initials (shown while avatar loads or if no avatar)
        authorAvatar.setText(author.getInitials());
        authorName.setText(author.getFullName());

        postMeta.setText("@" + sanitizeUsername(author) + " \u2022 " + getTimeAgo(pub.getDateCreation()));
        // ✅ Badge visibilité en coin de card
        addVisibilityBadge(pub);

        // 🖼️ Try to load avatar photo from UserProfile
        loadAvatarIntoLabel(authorAvatar, author.getId());

        // 🆕 Make username clickable to open profile
        authorName.setOnMouseClicked(e -> {
            if (feedController != null && author != null) {
                feedController.openUserProfile(author.getId());
            }
        });
        authorName.setStyle(
                "-fx-font-weight: 600; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #111827; " +
                        "-fx-cursor: hand;");

        // Content
        if (pub.getContenu() != null && !pub.getContenu().trim().isEmpty()) {
            postContent.setText(pub.getContenu());
        } else {
            postContent.setVisible(false);
            postContent.setManaged(false);
        }

        // Image
        if (pub.getImageUrl() != null && !pub.getImageUrl().trim().isEmpty()) {
            try {
                Image img = new Image(pub.getImageUrl(), true);
                postImage.setImage(img);
                imageContainer.setVisible(true);
                imageContainer.setManaged(true);
            } catch (Exception e) {
                imageContainer.setVisible(false);
                imageContainer.setManaged(false);
            }
        } else {
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
        }

        // Menu button (only for author)
        if (pub.getAuthorId() == currentUserId) {
            menuBtn.setOnAction(e -> showPostMenu());
        } else {
            menuBtn.setVisible(false);
            menuBtn.setManaged(false);
        }
    }

    private void displaySharedPost(Publication pub, User sharer) {
        // ✅ Hide normal header completely
        if (normalHeader != null) {
            normalHeader.setVisible(false);
            normalHeader.setManaged(false);
        }

        // ✅ Show share header
        if (shareHeader != null) {
            shareHeader.setVisible(true);
            shareHeader.setManaged(true);

            if (sharerAvatar != null) {
                sharerAvatar.setText(sharer.getInitials());
                // ✅ FIX: load real avatar photo for sharer in shared post header (32px = avatar-sm)
                loadAvatarIntoLabel(sharerAvatar, sharer.getId(), 32);
            }
            if (sharerName != null) {
                sharerName.setText(sharer.getFullName());
                // ✅ Badge visibilité en coin de card
                addVisibilityBadge(pub);
            }
            if (shareTime != null) {
                shareTime.setText(getTimeAgo(pub.getDateCreation()));
            }
        }

        // Show share message if exists
        if (pub.getShareMessage() != null && !pub.getShareMessage().trim().isEmpty()) {
            if (shareMessageBox != null) {
                shareMessageBox.setVisible(true);
                shareMessageBox.setManaged(true);
            }
            if (shareMessage != null) {
                shareMessage.setText(pub.getShareMessage());
            }
        } else {
            if (shareMessageBox != null) {
                shareMessageBox.setVisible(false);
                shareMessageBox.setManaged(false);
            }
        }

        // Hide normal content elements
        if (postContent != null) {
            postContent.setVisible(false);
            postContent.setManaged(false);
        }
        if (imageContainer != null) {
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
        }

        // Get original post
        Publication originalPub = publicationService.findById(pub.getSharedFromId());
        if (originalPub == null) {
            // Original post deleted
            if (originalPostContainer != null) {
                originalPostContainer.setVisible(false);
                originalPostContainer.setManaged(false);
            }

            if (postContent != null) {
                postContent.setText("[Publication originale supprimée]");
                postContent.setVisible(true);
                postContent.setManaged(true);
                postContent.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic;");
            }

            // Menu button for sharer
            if (pub.getAuthorId() == currentUserId && shareMenuBtn != null) {
                shareMenuBtn.setVisible(true);
                shareMenuBtn.setManaged(true);
                shareMenuBtn.setOnAction(e -> showPostMenu());
            }

            return;
        }

        User originalAuthor = userService.findById(originalPub.getAuthorId());
        if (originalAuthor == null) {
            if (postContent != null) {
                postContent.setText("[Auteur introuvable]");
                postContent.setVisible(true);
                postContent.setManaged(true);
                postContent.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic;");
            }
            return;
        }

        // ✅ Show original post in grey box (ALWAYS)
        if (originalPostContainer != null) {
            originalPostContainer.setVisible(true);
            originalPostContainer.setManaged(true);

            if (originalAuthorAvatar != null) {
                originalAuthorAvatar.setText(originalAuthor.getInitials());
                // ✅ FIX: load real avatar photo for original author in shared posts (32px = avatar-sm)
                loadAvatarIntoLabel(originalAuthorAvatar, originalAuthor.getId(), 32);
            }
            if (originalAuthorName != null) {
                originalAuthorName.setText(originalAuthor.getFullName());
            }
            if (originalPostMeta != null) {
                originalPostMeta.setText(
                        "@" + sanitizeUsername(originalAuthor) + " • " + getTimeAgo(originalPub.getDateCreation()));
            }
            if (originalPostContent != null) {
                originalPostContent.setText(originalPub.getContenu());
            }

            // Original image
            if (originalPub.getImageUrl() != null && !originalPub.getImageUrl().trim().isEmpty()) {
                if (originalPostImage != null) {
                    try {
                        Image img = new Image(originalPub.getImageUrl(), true);

                        img.errorProperty().addListener((obs, old, newVal) -> {
                            if (newVal) {
                                originalPostImage.setVisible(false);
                                originalPostImage.setManaged(false);
                            }
                        });

                        img.progressProperty().addListener((obs, old, newVal) -> {
                            if (newVal.doubleValue() == 1.0 && !img.isError()) {
                                originalPostImage.setImage(img);
                                originalPostImage.setVisible(true);
                                originalPostImage.setManaged(true);
                            }
                        });

                    } catch (Exception e) {
                        originalPostImage.setVisible(false);
                        originalPostImage.setManaged(false);
                    }
                }
            } else {
                if (originalPostImage != null) {
                    originalPostImage.setVisible(false);
                    originalPostImage.setManaged(false);
                }
            }
        }

        // ✅ Menu button visible in share header for sharer
        if (pub.getAuthorId() == currentUserId && shareMenuBtn != null) {
            shareMenuBtn.setVisible(true);
            shareMenuBtn.setManaged(true);
            shareMenuBtn.setOnAction(e -> showPostMenu());
        }
    }

    private void updateStats() {
        // Top réactions (max 3 types)
        StringBuilder reactionsDisplay = new StringBuilder();
        int totalReactions = 0;

        for (TypeReaction type : TypeReaction.values()) {
            int count = reactionService.countReactionsByType(publication.getId(), type);
            if (count > 0) {
                reactionsDisplay.append(type.getEmoji());
                totalReactions += count;
            }
        }

        if (totalReactions > 0) {
            reactionsDisplay.append(" ").append(totalReactions);
            likesLabel.setText(reactionsDisplay.toString());
        } else {
            likesLabel.setText("👍 0");
        }

        commentsLabel.setText("💬 " + publication.getNombreCommentaires() + " ");
        int shareCount = publicationService.countShares(publication.getId());
        sharesLabel.setText("📤 " + shareCount + " ");
    }

    private void setupActions() {
        // Like button
        setupLikeButton();
        likeBtn.setOnAction(e -> handleLike());

        // Comment button
        commentBtn.setOnAction(e -> commentInput.requestFocus());

        // Share button
        shareBtn.setOnAction(e -> handleShare());

        // Save button
        boolean isSaved = savedPostService.isPostSavedByUser(currentUserId, publication.getId());
        if (isSaved) {
            saveBtn.setText("🔖 Saved");
            saveBtn.getStyleClass().add("post-action-btn-active");
        }
        saveBtn.setOnAction(e -> handleSave());
    }

    private void handleLike() {
        boolean hasLiked = reactionService.userHasReacted(currentUserId, publication.getId());

        if (hasLiked) {
            Reaction reaction = reactionService.findByUserAndPublication(currentUserId, publication.getId());
            if (reaction != null) {
                reactionService.delete(reaction);
                likeBtn.setText("👍 Like");
                likeBtn.getStyleClass().remove("post-action-btn-active");
            }
        } else {
            Reaction reaction = new Reaction();
            reaction.setType(TypeReaction.LIKE);
            reaction.setUserId(currentUserId);
            reaction.setPublicationId(publication.getId());
            reaction.setDateCreation(LocalDateTime.now());
            reactionService.add(reaction);

            likeBtn.setText("👍 Liked");
            if (!likeBtn.getStyleClass().contains("post-action-btn-active")) {
                likeBtn.getStyleClass().add("post-action-btn-active");
            }

            // 🔔 Notify post author (skip if liking own post)
            if (notificationService != null
                    && publication.getAuthorId() != currentUserId) {
                User liker = userService.findById(currentUserId);
                Notification notif = new Notification();
                notif.setUserId(publication.getAuthorId());
                notif.setType("LIKE");
                notif.setTitre("Nouveau like");
                notif.setMessage((liker != null ? liker.getFullName() : "Quelqu’un")
                        + " a aimé votre publication.");
                notif.setRelatedUserId(currentUserId);
                notif.setRelatedPublicationId(publication.getId());
                notificationService.add(notif);
                if (feedController != null) feedController.updateNotificationBadge();
            }
        }

        publication = publicationService.findById(publication.getId());
        updateStats();
    }

    private void handleShare() {
        Dialog<ShareResult> dialog = new Dialog<>();
        dialog.setTitle("📤 Partager");
        dialog.setHeaderText("Partager cette publication");

        ButtonType shareButtonType = new ButtonType("Partager", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(shareButtonType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(10));

        // Preview
        VBox previewBox = new VBox(8);
        previewBox.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8; -fx-padding: 12;");
        User author = userService.findById(publication.getAuthorId());
        Label authorLabel = new Label("📝 " + (author != null ? author.getFullName() : "Unknown"));
        authorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        Label contentPreview = new Label(publication.getContenu());
        contentPreview.setWrapText(true);
        contentPreview.setMaxWidth(350);
        contentPreview.setStyle("-fx-font-size: 12; -fx-text-fill: #6B7280;");
        previewBox.getChildren().addAll(authorLabel, contentPreview);

        // Visibility selector
        Label visLabel = new Label("Partager dans :");
        ComboBox<FeedController.VisibilityOption> visSelector = new ComboBox<>();
        visSelector.getItems().add(new FeedController.VisibilityOption("🌍 Public", "PUBLIC", null));
        List<Group> userGroups = feedController.getGroupService().findByUserId(currentUserId);
        for (Group group : userGroups) {
            visSelector.getItems().add(
                    new FeedController.VisibilityOption("👥 " + group.getName(), "GROUP", group.getId()));
        }
        visSelector.getSelectionModel().selectFirst();

        // Message
        Label messageLabel = new Label("Ajouter un commentaire (optionnel) :");
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Qu'en pensez-vous ?");
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(3);

        content.getChildren().addAll(previewBox, visLabel, visSelector, messageLabel, messageArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == shareButtonType) {
                return new ShareResult(messageArea.getText(), visSelector.getSelectionModel().getSelectedItem());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            int originalId = publication.isShared() ? publication.getSharedFromId() : publication.getId();

            Publication sharedPub = new Publication();
            sharedPub.setContenu(publication.getContenu());
            sharedPub.setAuthorId(currentUserId);
            sharedPub.setDateCreation(LocalDateTime.now());
            sharedPub.setStatut(Statut.ACTIF);
            sharedPub.setSharedFromId(originalId);
            sharedPub.setShareMessage(result.message != null ? result.message.trim() : "");
            sharedPub.setVisibility(result.visibility.getVisibility());
            sharedPub.setGroupId(result.visibility.getGroupId());

            publicationService.add(sharedPub);
            if (notificationService != null && publication.getAuthorId() != currentUserId) {
                User sharer = userService.findById(currentUserId);
                Notification notif = new Notification();
                notif.setUserId(publication.getAuthorId());
                notif.setType("SHARE");
                notif.setTitre("Nouveau partage");
                notif.setMessage((sharer != null ? sharer.getFullName() : "Quelqu'un")
                        + " a partagé votre publication.");
                notif.setRelatedUserId(currentUserId);
                notif.setRelatedPublicationId(publication.getId());
                notif.setSeen(false);
                notif.setDateCreation(LocalDateTime.now());
                notificationService.add(notif);
                if (feedController != null) feedController.updateNotificationBadge();
            }


            feedController.refreshFeed();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("✅ Publication partagée !");
            alert.showAndWait();
        });
    }

    private static class ShareResult {
        String message;
        FeedController.VisibilityOption visibility;

        ShareResult(String message, FeedController.VisibilityOption visibility) {
            this.message = message;
            this.visibility = visibility;
        }
    }

    private void handleSave() {
        boolean isSaved = savedPostService.isPostSavedByUser(currentUserId, publication.getId());

        if (isSaved) {
            savedPostService.deleteByUserAndPublication(currentUserId, publication.getId());
            saveBtn.setText("🔖 Save");
            saveBtn.getStyleClass().remove("post-action-btn-active");
        } else {
            SavedPost savedPost = new SavedPost();
            savedPost.setUserId(currentUserId);
            savedPost.setPublicationId(publication.getId());
            savedPost.setSavedAt(LocalDateTime.now());
            savedPostService.add(savedPost);

            saveBtn.setText("🔖 Saved");
            if (!saveBtn.getStyleClass().contains("post-action-btn-active")) {
                saveBtn.getStyleClass().add("post-action-btn-active");
            }
        }
    }

    private void loadComments() {
        commentsList.getChildren().clear();
        List<Commentaire> comments = commentaireService.findByPublicationId(publication.getId());

        int displayCount = Math.min(2, comments.size());
        for (int i = 0; i < displayCount; i++) {
            Commentaire comment = comments.get(i);
            HBox commentBox = createCommentBox(comment);
            commentsList.getChildren().add(commentBox);
        }

        if (comments.size() > 2) {
            viewMoreCommentsBtn
                    .setText("View " + (comments.size() - 2) + " more comment" + (comments.size() - 2 > 1 ? "s" : ""));
            viewMoreCommentsBtn.setVisible(true);
            viewMoreCommentsBtn.setManaged(true);
            viewMoreCommentsBtn.setOnAction(e -> showAllComments());
        } else {
            viewMoreCommentsBtn.setVisible(false);
            viewMoreCommentsBtn.setManaged(false);
        }
    }

    private void showAllComments() {
        commentsList.getChildren().clear();
        List<Commentaire> comments = commentaireService.findByPublicationId(publication.getId());

        for (Commentaire comment : comments) {
            HBox commentBox = createCommentBox(comment);
            commentsList.getChildren().add(commentBox);
        }

        viewMoreCommentsBtn.setVisible(false);
        viewMoreCommentsBtn.setManaged(false);
    }

    private HBox createCommentBox(Commentaire comment) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.TOP_LEFT);
        box.getStyleClass().add("comment-item");

        User author = userService.findById(comment.getAuthorId());
        if (author == null)
            return box;

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(24, 24);
        avatar.setMaxSize(24, 24);
        avatar.getStyleClass().addAll("avatar", "avatar-sm");
        Label avatarLabel = new Label(author.getInitials());
        avatarLabel.getStyleClass().add("avatar-label");
        avatarLabel.setStyle("-fx-font-size: 11px;");
        avatar.getChildren().add(avatarLabel);
        // 🖼️ Load real photo (24px)
        loadAvatarIntoLabel(avatarLabel, author.getId(), 24);

        // Content
        VBox contentBox = new VBox(4);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Bubble
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("comment-bubble");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(author.getFullName());
        nameLabel.getStyleClass().add("comment-author");
        Label timeLabel = new Label("• " + getTimeAgo(comment.getDateCreation()));
        timeLabel.getStyleClass().add("comment-meta");
        topRow.getChildren().addAll(nameLabel, timeLabel);

        Label textLabel = new Label(comment.getContenu());
        textLabel.getStyleClass().add("comment-text");
        textLabel.setWrapText(true);

        bubble.getChildren().addAll(topRow, textLabel);
        // ✅ Affiche le GIF si présent
        if (comment.getGifUrl() != null && !comment.getGifUrl().isBlank()) {
            ImageView gifView = new ImageView();
            gifView.setFitWidth(200);
            gifView.setFitHeight(150);
            gifView.setPreserveRatio(true);
            new Thread(() -> {
                try {
                    Image img = new Image(
                            comment.getGifUrl(), 200, 150, true, true, true);
                    javafx.application.Platform.runLater(() -> gifView.setImage(img));
                } catch (Exception ignored) {}
            }).start();
            bubble.getChildren().add(gifView);
        }

        // Actions
        HBox actions = new HBox(8);
        actions.getStyleClass().add("comment-actions");

        Reaction userReaction = reactionService.findByUserAndComment(currentUserId, comment.getId());
        boolean hasLiked = userReaction != null;

        Button likeBtn = new Button(hasLiked ? "👍 Liked" : "Like");
        likeBtn.getStyleClass().add(hasLiked ? "comment-action-btn-active" : "comment-action-btn");
        likeBtn.setOnAction(e -> handleCommentLike(comment, likeBtn));

        if (comment.getNombreReactions() > 0) {
            Label reactionsLabel = new Label("👍 " + comment.getNombreReactions());
            reactionsLabel.getStyleClass().add("comment-meta");
            reactionsLabel.setStyle("-fx-text-fill: #7C3AED;");
            actions.getChildren().add(reactionsLabel);
        }

        actions.getChildren().add(likeBtn);

        if (comment.getAuthorId() == currentUserId) {
            Button menuBtn = new Button("⋮");
            menuBtn.getStyleClass().add("comment-action-btn");
            menuBtn.setOnAction(e -> showCommentMenu(comment));
            actions.getChildren().add(menuBtn);
        }

        contentBox.getChildren().addAll(bubble, actions);
        box.getChildren().addAll(avatar, contentBox);

        return box;
    }

    private void handleCommentLike(Commentaire comment, Button likeBtn) {
        Reaction existingReaction = reactionService.findByUserAndComment(currentUserId, comment.getId());

        if (existingReaction != null) {
            reactionService.delete(existingReaction);
            likeBtn.setText("Like");
            likeBtn.getStyleClass().remove("comment-action-btn-active");
            likeBtn.getStyleClass().add("comment-action-btn");
        } else {
            Reaction reaction = new Reaction();
            reaction.setType(TypeReaction.LIKE);
            reaction.setUserId(currentUserId);
            reaction.setCommentaireId(comment.getId());
            reaction.setDateCreation(LocalDateTime.now());
            reactionService.add(reaction);

            likeBtn.setText("👍 Liked");
            likeBtn.getStyleClass().remove("comment-action-btn");
            likeBtn.getStyleClass().add("comment-action-btn-active");
        }

        loadComments();
    }

    private void setupCommentInput() {
        User currentUser = userService.findById(currentUserId);
        if (currentUser != null && currentUserAvatar != null) {
            currentUserAvatar.setText(currentUser.getInitials());
            loadAvatarIntoLabel(currentUserAvatar, currentUserId, 24);
        }

        // ✅ Bouton GIF — EN DEHORS du listener, créé une seule fois
        if (commentInput.getParent() instanceof HBox) {
            HBox inputRow = (HBox) commentInput.getParent();
            boolean gifAlreadyAdded = inputRow.getChildren().stream()
                    .anyMatch(n -> n instanceof Button && "GIF".equals(((Button) n).getText()));

            if (!gifAlreadyAdded) {
                Button gifBtn = new Button("GIF");
                gifBtn.setStyle(
                        "-fx-background-color: #F3F4F6; -fx-text-fill: #374151; " +
                                "-fx-font-weight: 700; -fx-font-size: 11px; " +
                                "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 4 8;"
                );
                gifBtn.setOnAction(e -> openGifPicker());
                inputRow.getChildren().add(gifBtn);
            }
        }

        // Listener — seulement pour le send button
        commentInput.textProperty().addListener((obs, old, newVal) -> {
            boolean hasText = newVal != null && !newVal.trim().isEmpty();
            commentSendBtn.setVisible(hasText);
            commentSendBtn.setManaged(hasText);
        });

        commentSendBtn.setOnAction(e -> handleAddComment());
        commentInput.setOnAction(e -> handleAddComment());
    }

    private void handleAddComment() {
        String content = commentInput.getText().trim();
        if (content.isEmpty())
            return;

        Commentaire newComment = new Commentaire();
        newComment.setContenu(content);
        newComment.setGifUrl(pendingGifUrl); // ✅ null si pas de GIF
        newComment.setPublicationId(publication.getId());
        newComment.setAuthorId(currentUserId);
        newComment.setDateCreation(LocalDateTime.now());
        newComment.setStatut(Statut.ACTIF);
        newComment.setNombreReactions(0);

        commentaireService.add(newComment);

        // 🔔 Notify post author (skip if commenting on own post)
        if (notificationService != null
                && publication.getAuthorId() != currentUserId) {
            User commenter = userService.findById(currentUserId);
            Notification notif = new Notification();
            notif.setUserId(publication.getAuthorId());
            notif.setType("COMMENT");
            notif.setTitre("Nouveau commentaire");
            notif.setMessage((commenter != null ? commenter.getFullName() : "Quelqu’un")
                    + " a commenté votre publication.");
            notif.setRelatedUserId(currentUserId);
            notif.setRelatedPublicationId(publication.getId());
            notificationService.add(notif);
            if (feedController != null) feedController.updateNotificationBadge();
        }

        commentInput.clear();
        // Reset GIF
        pendingGifUrl = null;
// Retire le preview
        if (commentInput.getParent() instanceof HBox) {
            HBox inputRow = (HBox) commentInput.getParent();
            if (inputRow.getParent() instanceof VBox) {
                VBox commentBox = (VBox) inputRow.getParent();
                commentBox.getChildren().removeIf(n ->
                        n instanceof HBox && n.getId() != null && n.getId().equals("gifPreview")
                );
            }
        }
        commentSendBtn.setVisible(false);
        commentSendBtn.setManaged(false);

        publication = publicationService.findById(publication.getId());
        updateStats();
        loadComments();
    }

    private void showPostMenu() {
        ContextMenu menu = new ContextMenu();

        // ✅ Résumer — visible pour tout le monde
        MenuItem summarizeItem = new MenuItem("📝  Résumer avec IA");
        summarizeItem.setOnAction(e -> handleSummarize());
        menu.getItems().add(summarizeItem);
        Poll existingPoll = pollService.findByPublicationId(publication.getId());
        if (existingPoll != null && publication.getAuthorId() == currentUserId) {
            MenuItem editPollItem = new MenuItem("📊  Modifier le sondage");
            editPollItem.setOnAction(e -> handleEditPoll(existingPoll));
            menu.getItems().add(editPollItem);
        }

        // ✅ Edit/Delete — seulement pour l'auteur
        if (publication.getAuthorId() == currentUserId) {
            MenuItem editItem = new MenuItem("✏️  Modifier");
            editItem.setOnAction(e -> handleEditPost());

            MenuItem deleteItem = new MenuItem("🗑️  Supprimer");
            deleteItem.setStyle("-fx-text-fill: #EF4444;");
            deleteItem.setOnAction(e -> handleDeletePost());

            menu.getItems().addAll(editItem, deleteItem);
        }

        menu.show(menuBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void handleSummarize() {
        String content = publication.getContenu();
        if (content == null || content.trim().length() < 30) {
            new Alert(Alert.AlertType.INFORMATION,
                    "La publication est trop courte pour être résumée.").showAndWait();
            return;
        }

        // ✅ Loading stage non bloquant
        javafx.stage.Stage loadingStage = new javafx.stage.Stage();
        loadingStage.initModality(javafx.stage.Modality.NONE);
        loadingStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        loadingStage.setAlwaysOnTop(true);
        Label loadingLabel = new Label("📝 Résumé en cours...");
        loadingLabel.setStyle("-fx-padding: 20 32; -fx-font-size: 13px; -fx-font-weight: 600;");
        VBox loadingBox = new VBox(loadingLabel);
        loadingBox.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);"
        );
        loadingStage.setScene(new javafx.scene.Scene(loadingBox));
        loadingStage.show();

        new Thread(() -> {
            String summary = groqService.summarize(content);
            javafx.application.Platform.runLater(() -> {
                loadingStage.close();
                if (summary == null || summary.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "❌ Erreur IA.").showAndWait();
                    return;
                }

                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("📝 Résumé IA");
                dialog.setHeaderText(null);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                dialog.getDialogPane().setPrefWidth(440);

                VBox c = new VBox(12);
                c.setPadding(new javafx.geometry.Insets(16));

                Label originalLabel = new Label("Publication originale :");
                originalLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #9CA3AF;");

                Label originalText = new Label(content);
                originalText.setWrapText(true);
                originalText.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
                originalText.setMaxWidth(400);

                Label summaryLabel = new Label("📝 Résumé :");
                summaryLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #3D7EE8;");

                Label summaryText = new Label(summary);
                summaryText.setWrapText(true);
                summaryText.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #111827;");
                summaryText.setMaxWidth(400);

                c.getChildren().addAll(
                        originalLabel, originalText,
                        new Separator(),
                        summaryLabel, summaryText
                );
                dialog.getDialogPane().setContent(c);
                dialog.showAndWait();
            });
        }).start();
    }

    private void showCommentMenu(Commentaire comment) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏️  Modifier");
        editItem.setOnAction(e -> handleEditComment(comment));

        MenuItem deleteItem = new MenuItem("🗑️  Supprimer");
        deleteItem.setStyle("-fx-text-fill: #EF4444;");
        deleteItem.setOnAction(e -> handleDeleteComment(comment));

        menu.getItems().addAll(editItem, deleteItem);
        menu.show(commentsList, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void handleEditPost() {
        TextInputDialog dialog = new TextInputDialog(
                publication.isShared() ? publication.getShareMessage() : publication.getContenu());
        dialog.setTitle("Modifier la publication");
        dialog.setHeaderText(null);
        dialog.setContentText("Nouveau contenu :");

        dialog.showAndWait().ifPresent(newContent -> {
            if (!newContent.trim().isEmpty()) {
                if (publication.isShared()) {
                    publication.setShareMessage(newContent.trim());
                } else {
                    publication.setContenu(newContent.trim());
                }
                publicationService.update(publication);
                feedController.refreshFeed();
            }
        });
    }

    private void handleEditComment(Commentaire comment) {
        TextInputDialog dialog = new TextInputDialog(comment.getContenu());
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText(null);
        dialog.setContentText("Nouveau contenu :");

        dialog.showAndWait().ifPresent(newContent -> {
            if (!newContent.trim().isEmpty()) {
                comment.setContenu(newContent.trim());
                commentaireService.update(comment);
                loadComments();
            }
        });
    }

    private void handleDeletePost() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Supprimer");
        confirmAlert.setHeaderText("Êtes-vous sûr ?");
        confirmAlert.setContentText("Cette action est irréversible.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                publicationService.delete(publication);
                if (onDeleteCallback != null) {
                    onDeleteCallback.run(); // ✅ refresh le feed groupe si on est dans un groupe
                } else {
                    feedController.loadPublications(); // comportement normal dans le feed
                }
                feedController.refreshFeed();
            }
        });
    }

    private void handleDeleteComment(Commentaire comment) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Supprimer");
        confirmAlert.setHeaderText("Êtes-vous sûr ?");
        confirmAlert.setContentText("Cette action est irréversible.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                commentaireService.delete(comment);
                publication = publicationService.findById(publication.getId());
                updateStats();
                loadComments();
            }
        });
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60)
            return "Just now";
        else if (seconds < 3600)
            return (seconds / 60) + "m";
        else if (seconds < 86400)
            return (seconds / 3600) + "h";
        else if (seconds < 604800)
            return (seconds / 86400) + "d";
        else
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    /**
     * Loads the profile avatar into the StackPane containing the Label.
     * @param initialsLabel the Label showing initials (fallback)
     * @param userId        the user to load avatar for
     * @param size          size in pixels (e.g. 40 for post header, 24 for comments)
     */
    private void loadAvatarIntoLabel(Label initialsLabel, int userId, int size) {
        if (userProfileService == null) return;
        try {
            UserProfile profile = userProfileService.findByUserId(userId);
            if (profile == null || profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) return;

            Image img = new Image(
                    profile.getAvatarUrl(), size, size, true, true, true);

            img.errorProperty().addListener((obs, old, err) -> { });

            img.progressProperty().addListener((obs, old, progress) -> {
                if (progress.doubleValue() >= 1.0 && !img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(size);
                    iv.setFitHeight(size);
                    iv.setPreserveRatio(false); // ✅ false pour remplir exactement le cercle

                    // ✅ FIX: clip centré au milieu de l'ImageView (coordonnées locales)
                    double r = size / 2.0;
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(r, r, r);
                    iv.setClip(clip);

                    javafx.application.Platform.runLater(() -> {
                        StackPane parent =
                                (StackPane) initialsLabel.getParent();
                        if (parent != null) {
                            initialsLabel.setVisible(false);
                            parent.setStyle(
                                    "-fx-background-color: transparent;" +
                                            "-fx-border-color: #E5E7EB;" +
                                            "-fx-border-width: 1;" +
                                            "-fx-border-radius: 9999px;" +
                                            "-fx-background-radius: 9999px;"
                            );
                            // ✅ Retire l'ancien ImageView s'il existe déjà (évite les doublons)
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
    // ============================================================
// GIF
// ============================================================
    private void openGifPicker() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🎞️ Choisir un GIF");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(520, 480);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(12));

        // ── Barre de recherche ──
        HBox searchRow = new HBox(8);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher un GIF...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button searchBtn = new Button("🔍");
        searchBtn.setStyle(
                "-fx-background-color: #3D7EE8; -fx-text-fill: white; " +
                        "-fx-background-radius: 8px; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 6 12;"
        );
        searchRow.getChildren().addAll(searchField, searchBtn);

        // ── Grille de GIFs ──
        FlowPane gifGrid = new FlowPane();
        gifGrid.setHgap(6);
        gifGrid.setVgap(6);
        gifGrid.setPrefWrapLength(480);

        ScrollPane scrollPane = new ScrollPane(gifGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(360);
        scrollPane.setStyle("-fx-background-color: transparent;");

        content.getChildren().addAll(searchRow, scrollPane);
        dialog.getDialogPane().setContent(content);

        // ── Charge les trending au départ ──
        javafx.application.Platform.runLater(() ->
                loadGifsIntoGrid(gifGrid, giphyService.trending(20), dialog)
        );

        // ── Recherche ──
        Runnable doSearch = () -> {
            String q = searchField.getText().trim();
            if (!q.isEmpty()) {
                gifGrid.getChildren().clear();
                Label loading = new Label("Chargement...");
                loading.setStyle("-fx-text-fill: #9CA3AF;");
                gifGrid.getChildren().add(loading);

                new Thread(() -> {
                    List<GiphyService.GifResult> results = giphyService.search(q, 20);
                    javafx.application.Platform.runLater(() ->
                            loadGifsIntoGrid(gifGrid, results, dialog)
                    );
                }).start();
            }
        };

        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run());

        dialog.showAndWait();
    }

    private void loadGifsIntoGrid(FlowPane grid,
                                  List<GiphyService.GifResult> gifs,
                                  Dialog<?> dialog) {
        grid.getChildren().clear();

        if (gifs.isEmpty()) {
            Label empty = new Label("Aucun GIF trouvé");
            empty.setStyle("-fx-text-fill: #9CA3AF;");
            grid.getChildren().add(empty);
            return;
        }

        for (GiphyService.GifResult gif : gifs) {
            ImageView iv = new ImageView();
            iv.setFitWidth(110);
            iv.setFitHeight(80);
            iv.setPreserveRatio(false);
            iv.setStyle("-fx-cursor: hand;");

            // Charge l'image en background
            new Thread(() -> {
                try {
                    Image img = new Image(
                            gif.previewUrl, 110, 80, false, true, true);
                    javafx.application.Platform.runLater(() -> iv.setImage(img));
                } catch (Exception ignored) {}
            }).start();

            // Wrapper avec hover
            StackPane wrapper = new StackPane(iv);
            wrapper.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8px;");
            wrapper.setMinSize(110, 80);
            wrapper.setMaxSize(110, 80);

            wrapper.setOnMouseEntered(e ->
                    wrapper.setStyle("-fx-background-color: #DBEAFE; -fx-background-radius: 8px;")
            );
            wrapper.setOnMouseExited(e ->
                    wrapper.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8px;")
            );

            // ✅ Clic → sélectionne le GIF et ferme
            wrapper.setOnMouseClicked(e -> {
                pendingGifUrl = gif.originalUrl;
                dialog.close();
                // Affiche preview sous le commentInput
                showGifPreviewInComment(gif.originalUrl);
            });

            grid.getChildren().add(wrapper);
        }
    }

    private void showGifPreviewInComment(String gifUrl) {
        // Affiche un petit aperçu du GIF sélectionné sous la zone de texte
        if (commentInput.getParent() instanceof HBox) {
            HBox inputRow = (HBox) commentInput.getParent();
            if (inputRow.getParent() instanceof VBox) {
                VBox commentBox = (VBox) inputRow.getParent();

                // Retire l'ancien preview si existe
                commentBox.getChildren().removeIf(n ->
                        n instanceof HBox && n.getId() != null && n.getId().equals("gifPreview")
                );

                HBox preview = new HBox(8);
                preview.setId("gifPreview");
                preview.setAlignment(Pos.CENTER_LEFT);
                preview.setStyle("-fx-padding: 4 0;");

                ImageView iv = new ImageView(
                        new Image(gifUrl, 80, 60, false, true, true));
                iv.setFitWidth(80);
                iv.setFitHeight(60);

                Button removeBtn = new Button("✕");
                removeBtn.setStyle(
                        "-fx-background-color: #FEF2F1; -fx-text-fill: #E8392A; " +
                                "-fx-background-radius: 50%; -fx-cursor: hand; -fx-border-width: 0;"
                );
                removeBtn.setOnAction(e -> {
                    pendingGifUrl = null;
                    commentBox.getChildren().remove(preview);
                });

                preview.getChildren().addAll(iv, removeBtn);

                // Insère après inputRow
                int idx = commentBox.getChildren().indexOf(inputRow);
                commentBox.getChildren().add(idx + 1, preview);
            }
        }
    }
    // ============================================================
// REACTION SYSTEM
// ============================================================
    private void setupLikeButton() {
        Reaction existing = reactionService.findByUserAndPublication(currentUserId, publication.getId());
        if (existing != null) {
            Image activeImg = new Image(
                    getClass().getResourceAsStream(existing.getType().getImagePath()), 18, 18, true, true
            );
            likeBtn.setGraphic(new ImageView(activeImg));
            likeBtn.setText(" " + existing.getType().name());
            likeBtn.getStyleClass().add("post-action-btn-active");
        } else {
            Image defaultImg = new Image(
                    getClass().getResourceAsStream(TypeReaction.LIKE.getImagePath()), 18, 18, true, true
            );
            likeBtn.setGraphic(new ImageView(defaultImg));
            likeBtn.setText(" Like");
        }

        // Clic rapide = LIKE par défaut
        likeBtn.setOnAction(e -> handleReaction(TypeReaction.LIKE));

        // Hover → affiche le popup de réactions après 600ms
        likeBtn.setOnMouseEntered(e -> scheduleReactionPopup());
        likeBtn.setOnMouseExited(e -> cancelReactionPopup());
    }

    private javafx.animation.PauseTransition hoverPause;
    private javafx.stage.Popup reactionPopup;

    private void scheduleReactionPopup() {
        hoverPause = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(600)
        );
        hoverPause.setOnFinished(e -> showReactionPopup());
        hoverPause.play();
    }

    private void cancelReactionPopup() {
        if (hoverPause != null) hoverPause.stop();
        // Ne cache pas le popup immédiatement — le mouse peut aller sur le popup
    }

    private void showReactionPopup() {
        if (reactionPopup != null && reactionPopup.isShowing()) return;

        HBox emojiBar = new HBox(6);
        emojiBar.setAlignment(Pos.CENTER);
        emojiBar.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 9999px;" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 9999px;" +
                        "-fx-padding: 8 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);"
        );

        for (TypeReaction type : TypeReaction.values()) {
            // Image PNG colorée
            Image emojiImg = new Image(
                    getClass().getResourceAsStream(type.getImagePath()), 28, 28, true, true
            );
            ImageView imgView = new ImageView(emojiImg);
            imgView.setFitWidth(28);
            imgView.setFitHeight(28);

            Button emojiBtn = new Button();
            emojiBtn.setGraphic(imgView);
            emojiBtn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-width: 0;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 4 6;"
            );

// Hover — agrandit l'image
            emojiBtn.setOnMouseEntered(e -> {
                imgView.setFitWidth(36);
                imgView.setFitHeight(36);
                emojiBtn.setStyle(
                        "-fx-background-color: #F3F4F6;" +
                                "-fx-border-width: 0;" +
                                "-fx-cursor: hand;" +
                                "-fx-background-radius: 9999px;" +
                                "-fx-padding: 2 4;"
                );
            });
            emojiBtn.setOnMouseExited(e -> {
                imgView.setFitWidth(28);
                imgView.setFitHeight(28);
                emojiBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-border-width: 0;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 4 6;"
                );
            });

            Tooltip tooltip = new Tooltip(type.name());
            tooltip.setStyle("-fx-font-size: 11px;");
            emojiBtn.setTooltip(tooltip);



            emojiBtn.setOnAction(e -> {
                handleReaction(type);
                if (reactionPopup != null) reactionPopup.hide();
            });

            emojiBar.getChildren().add(emojiBtn);
        }

        // Cache le popup si la souris quitte la barre
        emojiBar.setOnMouseExited(e -> {
            if (reactionPopup != null) reactionPopup.hide();
        });

        reactionPopup = new javafx.stage.Popup();
        reactionPopup.setAutoHide(true);
        reactionPopup.getContent().add(emojiBar);

        // Position au-dessus du bouton Like
        javafx.geometry.Bounds bounds = likeBtn.localToScreen(likeBtn.getBoundsInLocal());
        reactionPopup.show(
                likeBtn.getScene().getWindow(),
                bounds.getMinX() - 20,
                bounds.getMinY() - 70
        );
    }

    private void handleReaction(TypeReaction type) {
        Reaction existing = reactionService.findByUserAndPublication(currentUserId, publication.getId());

        if (existing != null) {
            if (existing.getType() == type) {
                // ✅ Toggle off → reset au Like par défaut
                reactionService.delete(existing);
                Image defaultImg = new Image(
                        getClass().getResourceAsStream(TypeReaction.LIKE.getImagePath()), 18, 18, true, true
                );
                likeBtn.setGraphic(new ImageView(defaultImg));
                likeBtn.setText(" Like");
                likeBtn.getStyleClass().remove("post-action-btn-active");

            } else {
                // ✅ Réaction différente → changer le type
                reactionService.changeType(existing.getId(), type);
                Image reactionImg = new Image(
                        getClass().getResourceAsStream(type.getImagePath()), 18, 18, true, true
                );
                likeBtn.setGraphic(new ImageView(reactionImg));
                likeBtn.setText(" " + type.name());
                if (!likeBtn.getStyleClass().contains("post-action-btn-active")) {
                    likeBtn.getStyleClass().add("post-action-btn-active");
                }
            }

        } else {
            // ✅ Nouvelle réaction
            Reaction reaction = new Reaction();
            reaction.setType(type);
            reaction.setUserId(currentUserId);
            reaction.setPublicationId(publication.getId());
            reaction.setDateCreation(LocalDateTime.now());
            reactionService.add(reaction);

            Image reactionImg = new Image(
                    getClass().getResourceAsStream(type.getImagePath()), 18, 18, true, true
            );
            likeBtn.setGraphic(new ImageView(reactionImg));
            likeBtn.setText(" " + type.name());
            if (!likeBtn.getStyleClass().contains("post-action-btn-active")) {
                likeBtn.getStyleClass().add("post-action-btn-active");
            }

            // Notification
            if (notificationService != null && publication.getAuthorId() != currentUserId) {
                User liker = userService.findById(currentUserId);
                Notification notif = new Notification();
                notif.setUserId(publication.getAuthorId());
                notif.setType("REACTION");
                notif.setTitre("Nouvelle réaction");
                notif.setMessage((liker != null ? liker.getFullName() : "Quelqu'un")
                        + " a réagi à votre publication.");
                notif.setRelatedUserId(currentUserId);
                notif.setRelatedPublicationId(publication.getId());
                notif.setSeen(false);
                notif.setDateCreation(LocalDateTime.now());
                notificationService.add(notif);
                if (feedController != null) feedController.updateNotificationBadge();
            }
        }


        publication = publicationService.findById(publication.getId());
        updateStats();
    }

    private void addVisibilityBadge(Publication pub) {
        // ✅ Choisit le bon header selon le type de post
        HBox targetHeader = pub.isShared() ? shareHeader : normalHeader;
        if (targetHeader == null || targetHeader.getChildren().contains(new Label())) return;

        // Vérifie qu'on n'a pas déjà ajouté un badge
        boolean alreadyHasBadge = targetHeader.getChildren().stream()
                .anyMatch(n -> n instanceof Label && ((Label) n).getStyle().contains("background-radius: 9999px"));
        if (alreadyHasBadge) return;

        // Crée le badge
        Label badge = new Label();
        badge.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8;" +
                        "-fx-background-radius: 9999px;"
        );

        if (pub.isGroupPost()) {
            String groupName = "Groupe";
            // ✅ Même logique de couleur que les avatars sidebar/membres
            String[] colors = {"#3D7EE8", "#2DAA63", "#F5A623", "#E8392A"};
            String[] tints  = {"#EEF4FF", "#EDFBF4", "#FFFBF0", "#FEF2F1"};
            int idx = pub.getGroupId() % colors.length;
            String color = colors[idx];
            String tint  = tints[idx];

            if (feedController != null) {
                Group g = feedController.getGroupService().findById(pub.getGroupId());
                if (g != null) groupName = g.getName();
            }
            badge.setText("👥 " + groupName);
            badge.setStyle(badge.getStyle() +
                    "-fx-background-color: " + tint + "; -fx-text-fill: " + color + ";");

        } else if ("PUBLIC".equals(pub.getVisibility())) {
            badge.setText("🌍 Public");
            badge.setStyle(badge.getStyle() +
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;");
        } else {
            return;
        }

        // ✅ Insère spacer + badge avant le dernier élément (menuBtn / shareMenuBtn)
        if (!targetHeader.getChildren().contains(badge)) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            int insertIndex = Math.max(0, targetHeader.getChildren().size() - 1);
            targetHeader.getChildren().add(insertIndex, spacer);
            targetHeader.getChildren().add(insertIndex + 1, badge);
        }
    }


    private Runnable onDeleteCallback;

    public void setOnDeleteCallback(Runnable callback) {
        this.onDeleteCallback = callback;
    }
    // Legacy overload for post-header (40px)
    private void loadAvatarIntoLabel(Label initialsLabel, int userId) {
        loadAvatarIntoLabel(initialsLabel, userId, 40);
    }
    //==================================
    //modif poll
    //===============
    private void handleEditPoll(Poll poll) {
        List<PollOption> options = pollService.getOptions(poll.getId());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✏️ Modifier le sondage");
        ButtonType saveType = new ButtonType("✅ Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(440);

        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(16));

        // Question
        Label qLabel = new Label("Question");
        qLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
        TextField questionField = new TextField(poll.getQuestion());
        questionField.setStyle("-fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 8px;");

        // Options existantes
        Label optLabel = new Label("Options");
        optLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
        VBox optionsBox = new VBox(8);
        List<TextField> optFields = new ArrayList<>();

        for (int i = 0; i < options.size(); i++) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label((i + 1) + ".");
            num.setStyle("-fx-font-weight: 700; -fx-text-fill: #3D7EE8; -fx-min-width: 20;");
            TextField f = new TextField(options.get(i).getOptionText());
            f.setStyle("-fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 8px;");
            HBox.setHgrow(f, Priority.ALWAYS);
            optFields.add(f);
            row.getChildren().addAll(num, f);
            optionsBox.getChildren().add(row);
        }

        content.getChildren().addAll(qLabel, questionField, optLabel, optionsBox);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result != saveType) return;

            // Update question dans la publication
            String newQuestion = questionField.getText().trim();
            if (!newQuestion.isEmpty()) {
                publication.setContenu("📊 " + newQuestion);
                publicationService.update(publication);
            }

            // Update chaque option
            for (int i = 0; i < options.size(); i++) {
                String newText = optFields.get(i).getText().trim();
                if (!newText.isEmpty()) {
                    options.get(i).setOptionText(newText);
                    pollService.updateOption(options.get(i));
                }
            }

            // Refresh UI
            if (onDeleteCallback != null) {
                onDeleteCallback.run();
            } else {
                feedController.refreshFeed();
            }
        });
    }
}
