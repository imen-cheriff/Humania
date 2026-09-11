package communication.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import communication.models.*;
import communication.services.*;

import java.time.LocalDateTime;
import java.util.List;

public class NetworkController {

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private Label headerCountLabel;
    @FXML
    private Button backBtn;
    @FXML
    private Button followingTab;
    @FXML
    private Button followersTab;
    @FXML
    private Button pendingTab;

    private FeedController feedController;
    private FollowService followService;
    private UserService userService;
    private UserProfileService userProfileService;
    private NotificationService notificationService;
    private int currentUserId;
    private String currentTab = "FOLLOWING";

    public void setMainController(FeedController controller) {
        this.feedController = controller;
        if (controller != null) {
            this.followService = controller.getFollowService();
            this.userService = controller.getUserService();
            this.notificationService = controller.getNotificationService();
            this.currentUserId = controller.getCurrentUserId();
            this.userProfileService = new UserProfileService();
            initializeView();
        }
    }

    private void initializeView() {
        // Back button
        backBtn.setOnAction(null);
        backBtn.setOnAction(e -> feedController.goBackToFeed());

        // Tab buttons
        followingTab.setOnAction(null);
        followingTab.setOnAction(e -> setActiveTab("FOLLOWING"));

        followersTab.setOnAction(null);
        followersTab.setOnAction(e -> setActiveTab("FOLLOWERS"));

        pendingTab.setOnAction(null);
        pendingTab.setOnAction(e -> setActiveTab("PENDING"));

        // Load default tab
        setActiveTab("FOLLOWING");
        System.out.println("✅ NetworkController initialized");
    }

    private void setActiveTab(String tab) {
        // Reset all tabs to inactive style
        String inactiveStyle = "-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-border-width: 0 0 2.5 0; -fx-text-fill: #A0A8CC; -fx-font-weight: 500; " +
                "-fx-font-size: 13px; -fx-padding: 14 22; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: transparent; -fx-border-color: transparent transparent #6C5CE7 transparent; " +
                "-fx-border-width: 0 0 2.5 0; -fx-text-fill: #6C5CE7; -fx-font-weight: 700; " +
                "-fx-font-size: 13px; -fx-padding: 14 22; -fx-cursor: hand;";

        followingTab.setStyle(inactiveStyle);
        followersTab.setStyle(inactiveStyle);
        pendingTab.setStyle(inactiveStyle);

        switch (tab) {
            case "FOLLOWING":
                followingTab.setStyle(activeStyle);
                break;
            case "FOLLOWERS":
                followersTab.setStyle(activeStyle);
                break;
            case "PENDING":
                pendingTab.setStyle(activeStyle);
                break;
        }

        currentTab = tab;
        loadCurrentTab();
    }

    private void loadCurrentTab() {
        updateHeader();
        updateTabLabels();
        cardsContainer.getChildren().clear();

        switch (currentTab) {
            case "FOLLOWING":
                loadFollowingCards();
                break;
            case "FOLLOWERS":
                loadFollowersCards();
                break;
            case "PENDING":
                loadPendingCards();
                break;
        }
    }

    private void loadFollowingCards() {
        List<Follow> followingList = followService.findFollowing(currentUserId);

        if (followingList.isEmpty()) {
            showEmptyState("👤", "Vous ne suivez personne pour le moment.");
            return;
        }

        for (Follow follow : followingList) {
            User user = userService.findById(follow.getFollowedId());
            if (user != null) {
                boolean mutuel = followService.isActiveFollowing(follow.getFollowedId(), currentUserId);
                cardsContainer.getChildren().add(createUserCard(user, "FOLLOWING", mutuel, follow.getStatus()));
            }
        }
        System.out.println("✅ Loaded " + followingList.size() + " following cards");
    }

    private void loadFollowersCards() {
        List<Follow> followersList = followService.findFollowers(currentUserId);

        if (followersList.isEmpty()) {
            showEmptyState("👥", "Vous n'avez aucun suiveur pour le moment.");
            return;
        }

        for (Follow follow : followersList) {
            User user = userService.findById(follow.getFollowerId());
            if (user != null) {
                boolean mutuel = followService.isActiveFollowing(currentUserId, follow.getFollowerId());
                cardsContainer.getChildren().add(createUserCard(user, "FOLLOWER", mutuel, follow.getStatus()));
            }
        }
        System.out.println("✅ Loaded " + followersList.size() + " follower cards");
    }

    private void loadPendingCards() {
        List<Follow> pendingList = followService.findPendingFollowers(currentUserId);

        if (pendingList.isEmpty()) {
            showEmptyState("⏳", "Aucune demande en attente.");
            return;
        }

        for (Follow follow : pendingList) {
            User user = userService.findById(follow.getFollowerId());
            if (user != null) {
                cardsContainer.getChildren().add(createUserCard(user, "PENDING", false, Follow.PENDING));
            }
        }
        System.out.println("✅ Loaded " + pendingList.size() + " pending cards");
    }

    private VBox createUserCard(User user, String cardType, boolean mutuel, String status) {
        VBox card = new VBox(12);
        card.setPrefWidth(220);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E8EAF6; " +
                "-fx-border-width: 1.5; -fx-border-radius: 18; -fx-background-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.08), 16, 0, 0, 4);");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #D4CAFF; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 18; -fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.16), 20, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #E8EAF6; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 18; -fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.08), 16, 0, 0, 4);"));

        // ===== Avatar =====
        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(56, 56);
        avatarPane.setMaxSize(56, 56);
        avatarPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #A29BFE, #6C5CE7); -fx-background-radius: 9999;");

        Label initialsLabel = new Label(user.getInitials());
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 18px;");
        avatarPane.getChildren().add(initialsLabel);
        StackPane.setAlignment(initialsLabel, Pos.CENTER);

        // Try to load avatar photo
        loadAvatar(avatarPane, initialsLabel, user.getId(), 56);

        // ===== Name =====
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #1A1D2E;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        // ===== Username =====
        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0A8CC; -fx-font-weight: 500;");
        usernameLabel.setAlignment(Pos.CENTER);

        // ===== Badges =====
        HBox badgesBox = new HBox(6);
        badgesBox.setAlignment(Pos.CENTER);

        if (mutuel) {
            Label mutuelBadge = new Label("Mutuel");
            mutuelBadge.setStyle("-fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; " +
                    "-fx-background-radius: 9999; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: 600;");
            badgesBox.getChildren().add(mutuelBadge);
        }

        if (Follow.PENDING.equals(status)) {
            Label pendingBadge = new Label("En attente");
            pendingBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; " +
                    "-fx-background-radius: 9999; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: 600;");
            badgesBox.getChildren().add(pendingBadge);
        }

        // ===== Buttons =====
        HBox buttonsBox = new HBox(8);
        buttonsBox.setAlignment(Pos.CENTER);

        // "Voir profil" button (always present)
        Button viewProfileBtn = new Button("Voir profil");
        viewProfileBtn.setStyle("-fx-background-color: #F0EEFF; -fx-text-fill: #6C5CE7; " +
                "-fx-border-width: 0; -fx-border-radius: 9999px; " +
                "-fx-background-radius: 9999px; -fx-padding: 7 16; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
        viewProfileBtn.setOnAction(e -> {
            if (feedController != null) {
                feedController.openUserProfile(user.getId());
            }
        });

        buttonsBox.getChildren().add(viewProfileBtn);

        switch (cardType) {
            case "FOLLOWING":
                Button unfollowBtn = new Button("Se désabonner");
                unfollowBtn.setStyle("-fx-background-color: #FEF2F1; -fx-text-fill: #E8392A; " +
                        "-fx-border-width: 0; -fx-border-radius: 9999px; -fx-background-radius: 9999px; " +
                        "-fx-padding: 7 16; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
                unfollowBtn.setOnAction(e -> {
                    followService.unfollow(currentUserId, user.getId());
                    FollowStateManager.getInstance().markUnfollowing(user.getId());
                    if (feedController != null) {
                        feedController.refreshSuggestions();
                    }
                    loadCurrentTab();
                    System.out.println("✅ Désabonné de " + user.getFullName());
                });
                buttonsBox.getChildren().add(unfollowBtn);
                break;

            case "FOLLOWER":
                Button removeBtn = new Button("Supprimer");
                removeBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; " +
                        "-fx-border-width: 0; -fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    followService.removeFollower(user.getId(), currentUserId);
                    loadCurrentTab();
                    updateHeader();
                    System.out.println("✅ Follower supprimé : " + user.getFullName());
                });
                buttonsBox.getChildren().add(removeBtn);
                break;

            case "PENDING":
                Button acceptBtn = new Button("Accepter");
                acceptBtn.setStyle("-fx-background-color: linear-gradient(to right, #6C5CE7, #A29BFE); -fx-text-fill: white; " +
                        "-fx-border-width: 0; -fx-border-radius: 9999px; -fx-background-radius: 9999px; " +
                        "-fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: 700; " +
                        "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.35), 8, 0, 0, 2);");
                acceptBtn.setOnAction(e -> {
                    followService.acceptFollow(user.getId(), currentUserId);

                    // Send FOLLOW_ACCEPTED notification to the requester
                    if (notificationService != null) {
                        User currentUser = userService.findById(currentUserId);
                        Notification notif = new Notification();
                        notif.setUserId(user.getId());
                        notif.setType("FOLLOW_ACCEPTED");
                        notif.setTitre("Demande acceptée");
                        notif.setMessage((currentUser != null ? currentUser.getFullName() : "Quelqu'un")
                                + " a accepté votre demande de suivi.");
                        notif.setRelatedUserId(currentUserId);
                        notif.setSeen(false);
                        notif.setDateCreation(LocalDateTime.now());
                        notificationService.add(notif);
                    }

                    loadCurrentTab();
                    updateHeader();
                    if (feedController != null) feedController.updateNetworkBadge();
                    System.out.println("✅ Demande acceptée de " + user.getFullName());
                });

                Button rejectBtn = new Button("Refuser");
                rejectBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; " +
                        "-fx-border-width: 0; -fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
                rejectBtn.setOnAction(e -> {
                    followService.rejectFollow(user.getId(), currentUserId);
                    loadCurrentTab();
                    updateHeader();
                    if (feedController != null) feedController.updateNetworkBadge();
                    System.out.println("✅ Demande refusée de " + user.getFullName());
                });

                buttonsBox.getChildren().addAll(acceptBtn, rejectBtn);
                break;
        }

        card.getChildren().addAll(avatarPane, nameLabel, usernameLabel);
        if (!badgesBox.getChildren().isEmpty()) {
            card.getChildren().add(badgesBox);
        }
        card.getChildren().add(buttonsBox);

        return card;
    }

    private void loadAvatar(StackPane avatarPane, Label initialsLabel, int userId, int size) {
        if (userProfileService == null) return;
        try {
            UserProfile profile = userProfileService.findByUserId(userId);
            if (profile == null || profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) return;

            Image img = new Image(profile.getAvatarUrl(), size, size, true, true, true);

            img.progressProperty().addListener((obs, old, progress) -> {
                if (progress.doubleValue() >= 1.0 && !img.isError()) {
                    javafx.application.Platform.runLater(() -> {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(size);
                        iv.setFitHeight(size);
                        iv.setPreserveRatio(false);

                        double r = size / 2.0;
                        Circle clip = new Circle(r, r, r);
                        iv.setClip(clip);

                        initialsLabel.setVisible(false);
                        avatarPane.setStyle(
                                "-fx-background-color: transparent; " +
                                        "-fx-border-color: #E5E7EB; -fx-border-width: 1; " +
                                        "-fx-border-radius: 9999; -fx-background-radius: 9999;");
                        avatarPane.getChildren().removeIf(n -> n instanceof ImageView);
                        avatarPane.getChildren().add(iv);
                        StackPane.setAlignment(iv, Pos.CENTER);
                    });
                }
            });
        } catch (Exception e) {
            // Silent fail — keep initials
        }
    }

    private void updateHeader() {
        if (headerCountLabel != null && followService != null) {
            int following = followService.countFollowing(currentUserId);
            int followers = followService.countFollowers(currentUserId);
            int pending = followService.countPendingFollowers(currentUserId);
            headerCountLabel.setText(following + " abonnements · " + followers + " suiveurs · " + pending + " en attente");
        }
    }

    private void updateTabLabels() {
        if (followService != null) {
            int following = followService.countFollowing(currentUserId);
            int followers = followService.countFollowers(currentUserId);
            int pending = followService.countPendingFollowers(currentUserId);

            followingTab.setText("Abonnements (" + following + ")");
            followersTab.setText("Suiveurs (" + followers + ")");
            pendingTab.setText("En attente (" + pending + ")");
        }
    }

    private void showEmptyState(String emoji, String message) {
        VBox emptyBox = new VBox(12);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));
        emptyBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18px; " +
                "-fx-border-color: #E8EAF6; -fx-border-width: 1.5; -fx-border-radius: 18px; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.06), 12, 0, 0, 3);");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 42px;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0A8CC; -fx-font-weight: 500;");

        emptyBox.getChildren().addAll(emojiLabel, msgLabel);
        cardsContainer.getChildren().add(emptyBox);
    }
}
