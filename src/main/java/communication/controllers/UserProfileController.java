package communication.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import communication.models.*;
import communication.services.*;
import utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class UserProfileController {

    // ===== FXML =====
    @FXML
    private StackPane coverImageContainer;
    @FXML
    private StackPane avatarContainer;
    @FXML
    private Label userFullName;
    @FXML
    private Label userTitle;
    @FXML
    private Label userCompany;
    @FXML
    private Button followBtn;
    @FXML
    private Button messageBtn;
    @FXML
    private Button editProfileBtn;
    @FXML
    private Button settingsBtn;
    @FXML
    private Button backBtn;
    @FXML
    private Label aboutContent;
    @FXML
    private VBox contactInfoBox;
    @FXML
    private VBox statsBox;
    @FXML
    private VBox recentActivityBox;
    @FXML
    private ScrollPane contentScrollPane;

    // ===== SERVICES =====
    private UserService userService;
    private UserProfileService userProfileService;
    private ServicesPublication publicationService;
    private ReactionService reactionService;
    private CommentaireService commentaireService;
    private FollowService followService;

    private FeedController feedController;

    // ===== STATE =====
    private User profileUser;
    private UserProfile cachedProfile; // loaded ONCE — avoids repeated DB calls
    private int currentUserId;
    private boolean isInitializing = false; // BUG-3 guard: blocks handleFollowClick during init
    // Dynamic user ID from login session (was hardcoded = 1)
    public static int CURRENT_USER_ID = UserSession.getInstance().getUserId();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ======================================================================
    // INIT
    // ======================================================================
    public void setMainController(FeedController controller) {
        this.feedController = controller;
        if (controller != null) {
            this.publicationService = controller.getPublicationService();
            this.reactionService = controller.getReactionService();
            this.commentaireService = controller.getCommentaireService();
            this.userService = controller.getUserService();
            this.followService = controller.getFollowService();
            this.userProfileService = new UserProfileService();
        }
    }

    public void setProfileUserId(int userId) {
        // Refresh dynamic user ID from session
        CURRENT_USER_ID = UserSession.getInstance().getUserId();
        this.currentUserId = CURRENT_USER_ID;
        this.profileUser = userService.findById(userId);
        // Load the UserProfile row ONCE — all setup methods reuse cachedProfile
        this.cachedProfile = (userProfileService != null) ? userProfileService.findByUserId(userId) : null;

        // ── DEBUG ──────────────────────────────────────────────────────────
        System.out.println("🔍 [setProfileUserId]"
                + " currentUserId=" + CURRENT_USER_ID
                + " | profileUserId=" + userId
                + " | isOwnProfile=" + (userId == CURRENT_USER_ID)
                + " | profileUser=" + (profileUser != null ? profileUser.getFullName() : "NULL")
                + " | cachedProfile=" + (cachedProfile != null ? "loaded" : "NULL"));
        // ───────────────────────────────────────────────────────────────────

        isInitializing = true; // Block follow-click during init (Bug 3)
        initialize();
        isInitializing = false; // Unlock after full init
    }

    @FXML
    public void initialize() {
        if (profileUser == null) {
            // Show error state instead of leaving the page blank
            if (userFullName != null) userFullName.setText("Utilisateur introuvable");
            if (userTitle != null) userTitle.setText("");
            if (userCompany != null) userCompany.setText("");
            if (aboutContent != null) aboutContent.setText("Ce profil n'a pas pu être chargé.");
            System.err.println("⚠️ [UserProfileController] profileUser is null — cannot load profile.");
            return;
        }

        try {
            setupProfileHeader();
            setupAboutSection();
            setupContactInfo();
            setupStats();
            setupRecentActivity();
            setupActionButtons();

            System.out.println("✅ UserProfileController initialized for: " + profileUser.getFullName());
        } catch (Exception e) {
            System.err.println("❌ [UserProfileController] Error during initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ======================================================================
    // PROFILE HEADER (cover + avatar)
    // ======================================================================
    private void setupProfileHeader() {
        // Reload from DB only if we navigated back (cachedProfile already set in
        // setProfileUserId for first load)
        UserProfile profile = cachedProfile;

        // ===== COVER =====
        coverImageContainer.getChildren().clear();

        String coverUrl = (profile != null) ? profile.getCoverUrl() : null;
        if (coverUrl != null && !coverUrl.isBlank()) {
            try {
                ImageView coverImg = new ImageView(new Image(coverUrl, true));
                coverImg.setPreserveRatio(false);
                coverImg.fitWidthProperty().bind(coverImageContainer.widthProperty());
                coverImg.setFitHeight(220);
                coverImageContainer.getChildren().add(coverImg);
            } catch (Exception ignored) {
            }
        }
        // Gradient overlay / fallback is already set in FXML style

        // ===== AVATAR =====
        avatarContainer.getChildren().clear();
        String avatarUrl = (profile != null) ? profile.getAvatarUrl() : null;

        StackPane avatarRing = new StackPane();
        avatarRing.setPrefSize(110, 110);
        avatarRing.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 55; " +
                        "-fx-padding: 4;");

        if (avatarUrl != null && !avatarUrl.isBlank()) {
            try {
                ImageView avImg = new ImageView(new Image(avatarUrl, true));
                avImg.setFitWidth(100);
                avImg.setFitHeight(100);
                avImg.setPreserveRatio(false);
                Circle clip = new Circle(50, 50, 50);
                avImg.setClip(clip);
                avatarRing.getChildren().add(avImg);
            } catch (Exception ignored) {
                avatarRing.getChildren().add(buildInitialsCircle(profileUser, 100));
            }
        } else {
            avatarRing.getChildren().add(buildInitialsCircle(profileUser, 100));
        }
        avatarContainer.getChildren().add(avatarRing);

        // ===== TEXT INFO =====
        userFullName.setText(profileUser.getFullName());
        String jobTitle = (profile != null && profile.getJobTitle() != null)
                ? profile.getJobTitle()
                : profileUser.getRoleString(); // MERGE FIX: was getRole(), renamed to avoid return-type conflict
        String company = (profile != null && profile.getCompany() != null)
                ? profile.getCompany()
                : "Humania RH Management";

        userTitle.setText(jobTitle != null ? jobTitle : "Team Member");
        userCompany.setText(company);
    }

    private StackPane buildInitialsCircle(User user, int size) {
        StackPane stack = new StackPane();
        stack.setPrefSize(size, size);
        stack.setMaxSize(size, size);
        stack.setMinSize(size, size);
        // Use a Region for gradient background instead of Circle (JavaFX gradient on shapes is limited)
        Region bg = new Region();
        bg.setPrefSize(size, size); bg.setMinSize(size, size); bg.setMaxSize(size, size);
        bg.setStyle("-fx-background-color: linear-gradient(to bottom right, #A29BFE, #6C5CE7);" +
                " -fx-background-radius: " + (size/2) + "px;");
        Label label = new Label(user != null ? user.getInitials() : "?");
        label.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: " + (size / 2.2) + "px;");
        stack.getChildren().addAll(bg, label);
        stack.setAlignment(Pos.CENTER);
        return stack;
    }

    // ======================================================================
    // ABOUT SECTION
    // ======================================================================
    private void setupAboutSection() {
        // Reuse already-loaded profile — NO extra DB trip
        UserProfile profile = cachedProfile;
        String bio = (profile != null && profile.getBio() != null && !profile.getBio().isBlank())
                ? profile.getBio()
                : profileUser.getBio();

        if (bio != null && !bio.isBlank()) {
            aboutContent.setText(bio);
            aboutContent.setStyle("-fx-text-fill: #1A1D2E; -fx-font-size: 13.5px; -fx-line-spacing: 2;");
        } else {
            aboutContent.setText("Aucune description pour le moment.");
            aboutContent.setStyle("-fx-text-fill: #A0A8CC; -fx-font-size: 13px; -fx-font-style: italic;");
        }
    }

    // ======================================================================
    // CONTACT INFO
    // ======================================================================
    private void setupContactInfo() {
        contactInfoBox.getChildren().clear();
        contactInfoBox.setSpacing(10);

        contactInfoBox.getChildren().addAll(
                createContactItem("📧", "Email", profileUser.getEmail(), "Work Email"),
                createContactItem("👤", "Username", "@" + profileUser.getUsername(), ""),
                createContactItem("📅", "Inscrit",
                        profileUser.getCreatedAt() != null
                                ? profileUser.getCreatedAt().format(DATE_FMT)
                                : "N/A",
                        "Membre depuis"));
    }

    private HBox createContactItem(String icon, String label, String value, String subtitle) {
        HBox box = new HBox(14);
        box.setStyle(
                "-fx-padding: 12 14; -fx-background-color: #F4F5FB; -fx-background-radius: 14px; " +
                        "-fx-border-color: #E8EAF6; -fx-border-width: 1.5; -fx-border-radius: 14px;");
        box.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px; -fx-min-width: 28; -fx-alignment: center;");

        VBox content = new VBox(2);
        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-weight: 700; -fx-font-size: 10.5px; -fx-text-fill: #A0A8CC;");
        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 13px; -fx-text-fill: #1A1D2E; -fx-font-weight: 600;");
        valueText.setWrapText(true);

        content.getChildren().addAll(labelText, valueText);
        if (!subtitle.isEmpty()) {
            Label sub = new Label(subtitle);
            sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
            content.getChildren().add(sub);
        }

        box.getChildren().addAll(iconLabel, content);
        HBox.setHgrow(content, Priority.ALWAYS);
        return box;
    }

    // ======================================================================
    // STATS — modern card layout
    // ======================================================================
    private void setupStats() {
        statsBox.getChildren().clear();
        statsBox.setSpacing(0);
        statsBox.setStyle("-fx-padding: 0;");

        int postCount = publicationService.countByAuthorId(profileUser.getId());
        int followerCount = followService.countFollowers(profileUser.getId());
        int followingCount = followService.countFollowing(profileUser.getId());
        int totalLikes = calcTotalLikes(profileUser.getId());

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER);
        row.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #EAECF5; -fx-border-width: 1.5; " +
                        "-fx-border-radius: 18; -fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.08), 16, 0, 0, 4);");

        row.getChildren().addAll(
                createStatCard(String.valueOf(postCount), "Publications", "#6C5CE7", true),
                createStatCard(String.valueOf(followerCount), "Followers", "#00CEC9", true),
                createStatCard(String.valueOf(followingCount), "Following", "#00B894", true),
                createStatCard(String.valueOf(totalLikes), "Likes", "#E17055", false));
        statsBox.getChildren().add(row);
    }

    private VBox createStatCard(String count, String label, String color, boolean rightBorder) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-padding: 20 10; " +
                        "-fx-min-width: 80; " +
                        (rightBorder
                                ? "-fx-border-color: transparent #EAECF5 transparent transparent; -fx-border-width: 0 1 0 0;"
                                : ""));
        HBox.setHgrow(box, Priority.ALWAYS);

        Label countLabel = new Label(count);
        countLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 24px; -fx-text-fill: " + color + ";");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0A8CC; -fx-font-weight: 600;");

        box.getChildren().addAll(countLabel, labelText);
        return box;
    }

    // ======================================================================
    // RECENT ACTIVITY
    // ======================================================================
    private void setupRecentActivity() {
        recentActivityBox.getChildren().clear();
        recentActivityBox.setSpacing(10);

        List<Publication> userPosts = publicationService.findByAuthorId(profileUser.getId());

        if (userPosts.isEmpty()) {
            Label noPostsLabel = new Label("Aucune publication pour le moment.");
            noPostsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF; -fx-font-style: italic;");
            recentActivityBox.getChildren().add(noPostsLabel);
            return;
        }

        int limit = Math.min(5, userPosts.size());
        for (int i = 0; i < limit; i++) {
            recentActivityBox.getChildren().add(createActivityItem(userPosts.get(i)));
        }

        if (userPosts.size() > 5) {
            Button viewAllBtn = new Button("Voir toutes les publications (" + userPosts.size() + ")");
            viewAllBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #7C3AED; " +
                            "-fx-border-width: 0; -fx-font-weight: 600; -fx-font-size: 13px; " +
                            "-fx-cursor: hand; -fx-padding: 8;");
            viewAllBtn.setOnAction(e -> loadAllPosts(userPosts));
            recentActivityBox.getChildren().add(viewAllBtn);
        }
    }

    private HBox createActivityItem(Publication post) {
        HBox item = new HBox(12);
        item.setStyle(
                "-fx-padding: 12; -fx-background-color: #FFFFFF; " +
                        "-fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
        item.setAlignment(Pos.TOP_LEFT);

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label contentText = new Label(post.getContenu() != null && post.getContenu().length() > 120
                ? post.getContenu().substring(0, 120) + "…"
                : post.getContenu());
        contentText.setWrapText(true);
        contentText.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

        Label statsText = new Label("👍 " + post.getNombreReactions() + "  💬 " + post.getNombreCommentaires());
        statsText.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        Label timeLabel = new Label(getTimeAgo(post.getDateCreation()));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        content.getChildren().addAll(contentText, statsText, timeLabel);
        item.getChildren().add(content);
        return item;
    }

    private void loadAllPosts(List<Publication> allPosts) {
        recentActivityBox.getChildren().clear();
        for (Publication post : allPosts)
            recentActivityBox.getChildren().add(createActivityItem(post));
    }

    // ======================================================================
    // ACTION BUTTONS (Follow / Edit Profile / Back)
    // ======================================================================
    private void setupActionButtons() {
        // ── BUG-1 FIX: use CURRENT_USER_ID (static final, never 0)
        // NOT currentUserId (instance field, could be 0 before setProfileUserId runs)
        boolean isOwnProfile = (profileUser.getId() == CURRENT_USER_ID);

        // ── DEBUG ──────────────────────────────────────────────────────────
        String dbFollowStatus = followService.getFollowStatus(CURRENT_USER_ID, profileUser.getId());
        System.out.println("🔍 [setupActionButtons]"
                + " CURRENT_USER_ID=" + CURRENT_USER_ID
                + " | profileUser.getId()=" + profileUser.getId()
                + " | isOwnProfile=" + isOwnProfile
                + " | followStatus=" + dbFollowStatus);
        // ───────────────────────────────────────────────────────────────────

        if (isOwnProfile) {
            editProfileBtn.setVisible(true);
            editProfileBtn.setManaged(true);
            editProfileBtn.setText("✏  Modifier le profil");
            editProfileBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #6C5CE7, #A29BFE); -fx-text-fill: white; -fx-padding: 11 28; " +
                            "-fx-background-radius: 9999px; -fx-font-weight: 700; -fx-font-size: 13px; " +
                            "-fx-cursor: hand; -fx-border-width: 0; " +
                            "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.45), 14, 0, 0, 4);");
            editProfileBtn.setOnAction(e -> showEditProfileDialog());

            // ── BUG-3 FIX: neutralise followBtn handler before hiding it
            followBtn.setOnAction(null);
            followBtn.setVisible(false);
            followBtn.setManaged(false);
            messageBtn.setOnAction(null);
            messageBtn.setVisible(false);
            messageBtn.setManaged(false);

            if (settingsBtn != null) {
                settingsBtn.setVisible(false);
                settingsBtn.setManaged(false);
            }

        } else {
            editProfileBtn.setOnAction(null);
            editProfileBtn.setVisible(false);
            editProfileBtn.setManaged(false);

            // ── BUG-3 FIX: detach handler FIRST, update visuals, then re-attach
            followBtn.setOnAction(null); // 1. detach (safety)
            String followStatus = followService.getFollowStatus(CURRENT_USER_ID, profileUser.getId());
            updateFollowButton3State(followStatus); // 2. set text/style only (3 states)
            followBtn.setVisible(true);
            followBtn.setManaged(true);
            followBtn.setOnAction(e -> handleFollowClick()); // 3. attach LAST

            messageBtn.setOnAction(null);
            messageBtn.setVisible(true);
            messageBtn.setManaged(true);
            messageBtn.setText("💬  Message");
            messageBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #6C5CE7; " +
                            "-fx-border-color: #6C5CE7; -fx-border-width: 2; -fx-padding: 10 22; " +
                            "-fx-background-radius: 9999px; -fx-border-radius: 9999px; " +
                            "-fx-font-weight: 700; -fx-font-size: 13px; -fx-cursor: hand;");
            messageBtn.setOnAction(e -> showAlert("Messagerie à venir !"));

            if (settingsBtn != null) {
                settingsBtn.setVisible(false);
                settingsBtn.setManaged(false);
            }
        }

        // Back button
        backBtn.setOnAction(null);
        backBtn.setText("← Retour");
        backBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-width: 0; " +
                        "-fx-font-size: 13px; -fx-text-fill: #6B7280; -fx-padding: 8 12; " +
                        "-fx-cursor: hand; -fx-font-weight: 500;");
        backBtn.setOnAction(e -> goBack());
    }

    /**
     * 3-state follow button: null → "Suivre", "PENDING" → "⏳ En attente", "ACTIVE" → "✓ Abonné(e)"
     */
    private void updateFollowButton3State(String status) {
        if ("ACTIVE".equals(status)) {
            followBtn.setText("✓ Abonné(e)");
            followBtn.setStyle(
                    "-fx-background-color: #EDE9FE; -fx-text-fill: #6C5CE7; " +
                            "-fx-padding: 11 24; -fx-background-radius: 9999px; " +
                            "-fx-font-weight: 700; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 0;");
        } else if ("PENDING".equals(status)) {
            followBtn.setText("⏳ En attente");
            followBtn.setStyle(
                    "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; " +
                            "-fx-padding: 11 24; -fx-background-radius: 9999px; " +
                            "-fx-font-weight: 700; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 0;");
        } else {
            followBtn.setText("+ Suivre");
            followBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #6C5CE7, #A29BFE); -fx-text-fill: white; " +
                            "-fx-padding: 11 24; -fx-background-radius: 9999px; " +
                            "-fx-font-weight: 700; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 0; " +
                            "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.4), 12, 0, 0, 3);");
        }
    }

    private void handleFollowClick() {
        // BUG-3 guard: prevent any follow/unfollow action during initialization
        if (isInitializing) {
            System.out.println("⚠️ [handleFollowClick] Blocked during initialization — ignoring.");
            return;
        }
        if (followService == null)
            return;

        String currentStatus = followService.getFollowStatus(currentUserId, profileUser.getId());

        if ("ACTIVE".equals(currentStatus)) {
            // Unfollow
            followService.unfollow(currentUserId, profileUser.getId());
            FollowStateManager.getInstance().markUnfollowing(profileUser.getId());
            updateFollowButton3State(null);
            System.out.println("✅ Unfollowed " + profileUser.getFullName());
        } else if ("PENDING".equals(currentStatus)) {
            // Cancel pending request
            followService.unfollow(currentUserId, profileUser.getId());
            FollowStateManager.getInstance().markUnfollowing(profileUser.getId());
            updateFollowButton3State(null);
            System.out.println("✅ Cancelled pending follow to " + profileUser.getFullName());
        } else {
            // Send follow request (PENDING)
            Follow follow = new Follow();
            follow.setFollowerId(currentUserId);
            follow.setFollowedId(profileUser.getId());
            follow.setFollowedAt(LocalDateTime.now());
            followService.add(follow);
            FollowStateManager.getInstance().markFollowing(profileUser.getId());
            updateFollowButton3State("PENDING");

            // 🔔 Notify the target user with FOLLOW_REQUEST
            if (feedController != null) {
                NotificationService ns =
                        feedController.getNotificationService();
                if (ns != null) {
                    User currentUserObj = userService.findById(currentUserId);
                    Notification notif = new Notification();
                    notif.setUserId(profileUser.getId());
                    notif.setType("FOLLOW_REQUEST");
                    notif.setTitre("Demande de suivi");
                    notif.setMessage((currentUserObj != null ? currentUserObj.getFullName() : "Quelqu'un")
                            + " souhaite vous suivre.");
                    notif.setRelatedUserId(currentUserId);
                    notif.setSeen(false);
                    notif.setDateCreation(LocalDateTime.now());
                    ns.add(notif);
                    feedController.updateNotificationBadge();
                }
                feedController.updateNetworkBadge();
            }
            System.out.println("✅ Follow request sent to " + profileUser.getFullName());
        }

        // Refresh stats and sync sidebar
        setupStats();
        if (feedController != null) {
            feedController.refreshSuggestions();
        }
    }

    // ======================================================================
    // EDIT PROFILE DIALOG (with real file upload)
    // ======================================================================
    private void showEditProfileDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le profil");
        dialog.setHeaderText(null);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setPrefWidth(460);

        // ===== SECTION 1: AVATAR =====
        VBox avatarSection = new VBox(10);
        Label photoLabel = new Label("📷 Photo de profil");
        photoLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #111827;");

        HBox photoRow = new HBox(16);
        photoRow.setAlignment(Pos.CENTER_LEFT);

        // Preview circle
        StackPane preview = buildInitialsCircle(profileUser, 64);
        // Reuse cachedProfile — no extra DB trip for the dialog
        UserProfile up = cachedProfile;
        final String[] newAvatarPath = { up != null ? up.getAvatarUrl() : null };
        final String[] newCoverPath = { up != null ? up.getCoverUrl() : null };

        if (newAvatarPath[0] != null && !newAvatarPath[0].isBlank()) {
            try {
                ImageView avImg = new ImageView(new Image(newAvatarPath[0], true));
                avImg.setFitWidth(64);
                avImg.setFitHeight(64);
                avImg.setPreserveRatio(false);
                Circle clip = new Circle(32, 32, 32);
                avImg.setClip(clip);
                preview.getChildren().setAll(avImg);
            } catch (Exception ignored) {
            }
        }

        VBox photoBtns = new VBox(8);
        Button uploadPhotoBtn = new Button("📁 Choisir une image");
        uploadPhotoBtn.setStyle(
                "-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-padding: 8 16; " +
                        "-fx-background-radius: 8; -fx-font-weight: 600; -fx-cursor: hand; -fx-border-width: 0;");
        uploadPhotoBtn.setOnAction(e -> {
            File file = openImageFileChooser(dialog);
            if (file != null) {
                String saved = saveFileToAppData(file, "avatars");
                if (saved != null) {
                    newAvatarPath[0] = saved;
                    try {
                        ImageView avImg = new ImageView(new Image(file.toURI().toString(), true));
                        avImg.setFitWidth(64);
                        avImg.setFitHeight(64);
                        avImg.setPreserveRatio(false);
                        Circle clip = new Circle(32, 32, 32);
                        avImg.setClip(clip);
                        preview.getChildren().setAll(avImg);
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        Button uploadCoverBtn = new Button("🖼️ Choisir une couverture");
        uploadCoverBtn.setStyle(
                "-fx-background-color: #2563EB; -fx-text-fill: white; -fx-padding: 8 16; " +
                        "-fx-background-radius: 8; -fx-font-weight: 600; -fx-cursor: hand; -fx-border-width: 0;");
        uploadCoverBtn.setOnAction(e -> {
            File file = openImageFileChooser(dialog);
            if (file != null) {
                String saved = saveFileToAppData(file, "covers");
                if (saved != null)
                    newCoverPath[0] = saved;
            }
        });

        photoBtns.getChildren().addAll(uploadPhotoBtn, uploadCoverBtn);
        photoRow.getChildren().addAll(preview, photoBtns);
        avatarSection.getChildren().addAll(photoLabel, photoRow);

        // ===== SECTION 2: BIO =====
        VBox bioSection = new VBox(8);
        Label bioLabel = new Label("📝 Bio");
        bioLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #111827;");

        String existingBio = (up != null && up.getBio() != null) ? up.getBio()
                : (profileUser.getBio() != null ? profileUser.getBio() : "");
        TextArea bioArea = new TextArea(existingBio);
        bioArea.setWrapText(true);
        bioArea.setPrefRowCount(4);
        bioArea.setPromptText("Parlez de vous à vos collègues…");
        bioArea.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 13px; " +
                        "-fx-control-inner-background: #FFFFFF;");

        Label bioHint = new Label("Max 500 caractères");
        bioHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        bioSection.getChildren().addAll(bioLabel, bioArea, bioHint);

        content.getChildren().addAll(avatarSection, new Separator(), bioSection);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        sp.setPrefHeight(420);

        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newBio = bioArea.getText().trim();
            if (newBio.length() > 500) {
                showAlert("La bio doit faire au maximum 500 caractères.");
                showEditProfileDialog();
                return;
            }

            if (userProfileService != null) {
                // ✅ FIX: si la row UserProfile n'existe pas encore en DB,
                // un simple UPDATE ne fait rien (0 rows affected).
                // On crée d'abord la row, puis on update.
                if (cachedProfile == null) {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUserId(profileUser.getId());
                    newProfile.setBio(newBio);
                    newProfile.setAvatarUrl(newAvatarPath[0]);
                    newProfile.setCoverUrl(newCoverPath[0]);
                    userProfileService.add(newProfile);  // ✅ add() et non create()
                } else {
                    userProfileService.updateBio(profileUser.getId(), newBio);
                    if (newAvatarPath[0] != null) {
                        userProfileService.updateAvatarUrl(profileUser.getId(), newAvatarPath[0]);
                    }
                    if (newCoverPath[0] != null) {
                        userProfileService.updateCoverUrl(profileUser.getId(), newCoverPath[0]);
                    }
                }
            }

            // Refresh cached profile then update UI
            cachedProfile = (userProfileService != null)
                    ? userProfileService.findByUserId(profileUser.getId())
                    : null;
            setupProfileHeader();
            setupAboutSection();
            showAlert("✅ Profil mis à jour avec succès !");
        }
    }

    /**
     * Opens a FileChooser filtered to images (PNG, JPG, GIF). Returns null if
     * cancelled.
     */
    private File openImageFileChooser(Dialog<?> parentDialog) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images (PNG, JPG, GIF)", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File file = chooser.showOpenDialog(
                parentDialog.getDialogPane().getScene() != null
                        ? parentDialog.getDialogPane().getScene().getWindow()
                        : null);

        if (file != null) {
            long maxBytes = 5L * 1024 * 1024; // 5 MB
            if (file.length() > maxBytes) {
                showAlert("❌ Fichier trop volumineux. Taille max : 5 Mo.");
                return null;
            }
        }
        return file;
    }

    /**
     * Copies the file into {user.home}/.humania/{folder}/ and returns the URI
     * string.
     */
    private String saveFileToAppData(File source, String folder) {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".humania", folder);
            Files.createDirectories(dir);
            Path dest = dir.resolve(source.getName());
            Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            String uriStr = dest.toUri().toString();
            System.out.println("✅ Image saved: " + uriStr);
            return uriStr;
        } catch (IOException e) {
            showAlert("❌ Impossible de sauvegarder l'image : " + e.getMessage());
            return null;
        }
    }

    // updateCoverUrl removed — now delegated to UserProfileService.updateCoverUrl()
    // The old try-with-resources incorrectly closed the shared JDBC connection.

    // ======================================================================
    // HELPERS
    // ======================================================================
    private String getTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60)
            return "À l'instant";
        else if (seconds < 3600)
            return (seconds / 60) + " min";
        else if (seconds < 86400)
            return (seconds / 3600) + " h";
        else if (seconds < 604800)
            return (seconds / 86400) + " j";
        else
            return dateTime.format(DATE_FMT);
    }

    private int calcTotalLikes(int userId) {
        List<Publication> posts = publicationService.findByAuthorId(userId);
        int total = 0;
        for (Publication p : posts)
            total += p.getNombreReactions();
        return total;
    }

    private void goBack() {
        if (feedController != null)
            feedController.goBackToFeed();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}