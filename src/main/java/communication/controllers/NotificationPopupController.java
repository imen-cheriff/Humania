package communication.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import communication.services.*;
import communication.models.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationPopupController {

    @FXML private VBox notificationsList;
    @FXML private VBox emptyState;
    @FXML private Button markAllReadBtn;

    private NotificationService notificationService;
    private UserService userService;
    private FeedController feedController;
    private int currentUserId;

    public void setMainController(FeedController controller) {
        this.feedController = controller;
        this.notificationService = controller.getNotificationService();
        this.userService = controller.getUserService();
        this.currentUserId = controller.getCurrentUserId();
    }

    @FXML
    public void initialize() {
        if (markAllReadBtn != null) {
            markAllReadBtn.setOnAction(e -> markAllAsRead());
        }
    }

    public void loadNotifications() {
        if (notificationService == null) {
            System.err.println("❌ [NotificationPopup] notificationService is null");
            return;
        }

        notificationsList.getChildren().clear();
        List<Notification> notifications = notificationService.findByUserId(currentUserId);
        System.out.println("🔔 [NotificationPopup] userId=" + currentUserId + " | notifications found: " + notifications.size());

        if (notifications.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            notificationsList.setVisible(false);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        notificationsList.setVisible(true);

        notifications.sort((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()));

        int limit = Math.min(20, notifications.size());
        for (int i = 0; i < limit; i++) {
            HBox notifItem = createNotificationItem(notifications.get(i));
            notificationsList.getChildren().add(notifItem);
        }
    }

    private HBox createNotificationItem(Notification notif) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));

        // ✅ FIX: getSeen() au lieu de getIsRead()
        boolean seen = notif.getSeen() != null && notif.getSeen();

        item.setStyle(
                "-fx-cursor: hand; " +
                        "-fx-background-color: " + (seen ? "white" : "#F3F4F6") + ";"
        );

        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-cursor: hand; -fx-background-color: #F9FAFB;"
        ));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-cursor: hand; " +
                        "-fx-background-color: " + (seen ? "white" : "#F3F4F6") + ";"
        ));

        // Icon
        Label iconLabel = new Label(getNotificationIcon(notif.getType()));
        iconLabel.setStyle("-fx-font-size: 24px; -fx-min-width: 32; -fx-alignment: center;");

        // Content
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        // ✅ FIX: getMessage() au lieu de getContenu()
        Label messageLabel = new Label(notif.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #111827; " +
                        "-fx-font-weight: " + (seen ? "400" : "600") + ";"
        );

        Label timeLabel = new Label(getTimeAgo(notif.getDateCreation()));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        content.getChildren().addAll(messageLabel, timeLabel);

        // Unread indicator
        Region indicator = new Region();
        if (!seen) {
            indicator.setMinSize(8, 8);
            indicator.setMaxSize(8, 8);
            indicator.setStyle(
                    "-fx-background-color: #7C3AED; -fx-background-radius: 50%;"
            );
        }

        item.getChildren().addAll(iconLabel, content, indicator);
        item.setOnMouseClicked(e -> handleNotificationClick(notif));

        return item;
    }

    private String getNotificationIcon(String type) {
        if (type == null) return "🔔";
        switch (type) {
            case "REACTION":
            case "LIKE":           return "👍";
            case "COMMENT":        return "💬";
            case "SHARE":          return "📤";
            case "FOLLOW":         return "👤";
            case "FOLLOW_REQUEST": return "👤";
            case "FOLLOW_ACCEPTED":return "✅";
            case "MENTION":        return "📢";
            case "GROUP": return "👥";
            default:               return "🔔";
        }
    }

    private void handleNotificationClick(Notification notif) {
        // ✅ FIX: getSeen() au lieu de getIsRead()
        boolean seen = notif.getSeen() != null && notif.getSeen();

        if (!seen) {
            notificationService.markAsRead(notif.getId());
            if (feedController != null) {
                feedController.updateNotificationBadge();
            }
        }

        // Handle FOLLOW_REQUEST specially - redirect to Network page
        if ("FOLLOW_REQUEST".equals(notif.getType())) {
            if (feedController != null) {
                feedController.closeNotificationPopup();
                feedController.navigateToNetwork();
            }
            return;
        }

        // ✅ FIX: getRelatedPublicationId() au lieu de getPublicationId()
        if (notif.getRelatedPublicationId() != null && notif.getRelatedPublicationId() > 0) {
            if (feedController != null) {
                feedController.closeNotificationPopup();
                feedController.navigateToPublication(notif.getRelatedPublicationId());
            }
            // ✅ FIX: getRelatedUserId() au lieu de getFromUserId()
        } else if (notif.getRelatedUserId() != null && notif.getRelatedUserId() > 0) {
            if (feedController != null) {
                feedController.closeNotificationPopup();
                feedController.openUserProfile(notif.getRelatedUserId());
            }
        }
    }

    private void markAllAsRead() {
        if (notificationService == null) return;
        notificationService.markAllAsRead(currentUserId);
        loadNotifications();
        if (feedController != null) {
            feedController.updateNotificationBadge();
        }
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60)        return "À l'instant";
        else if (seconds < 3600) return (seconds / 60) + "m";
        else if (seconds < 86400) return (seconds / 3600) + "h";
        else if (seconds < 604800) return (seconds / 86400) + "j";
        else return dateTime.format(DateTimeFormatter.ofPattern("dd MMM"));
    }
}