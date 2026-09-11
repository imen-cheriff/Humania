package communication.controllers;

import communication.models.Message;
import communication.models.User;
import communication.models.Group;
import communication.services.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatController {

    @FXML private VBox conversationsList;
    @FXML private VBox messagesList;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageInput;
    @FXML private Button sendBtn;
    @FXML private Button newChatBtn;
    @FXML private Button privateTabBtn;
    @FXML private Button groupTabBtn;
    @FXML private TextField searchField;
    @FXML private Label chatNameLabel;
    @FXML private Label chatStatusLabel;
    @FXML private Label chatAvatarLabel;
    @FXML private StackPane chatAvatarPane;
    @FXML private HBox inputBox;

    private FeedController feedController;
    private ChatService chatService;
    private UserService userService;
    private GroupService groupService;

    private int currentUserId;
    private User currentUser;

    private Integer activeChatUserId = null;
    private Integer activeChatGroupId = null;
    private boolean showingGroups = false;

    // ✅ Scheduler comme champ de classe
    private ScheduledExecutorService scheduler;

    private static final String[] COLORS = {"#3D7EE8", "#2DAA63", "#F5A623", "#E8392A"};

    public void setMainController(FeedController controller) {
        this.feedController = controller;
        this.userService = controller.getUserService();
        this.groupService = controller.getGroupService();
        this.currentUserId = controller.getCurrentUserId();
        this.currentUser = userService.findById(currentUserId);
        this.chatService = new ChatService();
        initialize();
    }

    private void initialize() {
        privateTabBtn.setOnAction(e -> {
            showingGroups = false;
            setActiveTab(privateTabBtn, groupTabBtn);
            loadConversations();
        });

        groupTabBtn.setOnAction(e -> {
            showingGroups = true;
            setActiveTab(groupTabBtn, privateTabBtn);
            loadGroupConversations();
        });

        sendBtn.setOnAction(e -> handleSend());
        messageInput.setOnAction(e -> handleSend());
        newChatBtn.setOnAction(e -> openNewChatDialog());
        searchField.textProperty().addListener((obs, old, newVal) -> filterConversations(newVal));

        inputBox.setDisable(true);
        loadConversations();

        // ✅ WebSocket + Polling
        chatService.subscribeToMessages(currentUserId, this::onNewMessageReceived);
        chatService.startPolling(currentUserId, this::onNewMessageReceived);

        // ✅ Scheduler comme champ — peut être arrêté dans cleanup()
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() ->
                        Platform.runLater(this::loadConversations),
                30, 30, TimeUnit.SECONDS
        );
    }

    // ✅ cleanup() comme méthode de classe — pas dans initialize()
    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        chatService.disconnect();
    }

    // ── Charger conversations privées ─────────────────────────
    private void loadConversations() {
        conversationsList.getChildren().clear();
        List<User> users = userService.getAll();

        for (User user : users) {
            if (user.getId() == currentUserId) continue;
            addConversationItem(user);
        }
    }

    // ── Charger conversations de groupe ───────────────────────
    private void loadGroupConversations() {
        conversationsList.getChildren().clear();
        List<Group> groups = groupService.findByUserId(currentUserId);
        for (Group group : groups) {
            addGroupConversationItem(group);
        }
    }

    // ── Item conversation privée ───────────────────────────────
    private void addConversationItem(User user) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 14, 10, 14));
        item.setStyle("-fx-background-radius: 14px; -fx-cursor: hand;");
        item.setOnMouseEntered(e -> {
            if (!"selected".equals(item.getProperties().get("state")))
                item.setStyle("-fx-background-color: #F4F5FB; -fx-background-radius: 14px; -fx-cursor: hand;");
        });
        item.setOnMouseExited(e -> {
            if (!"selected".equals(item.getProperties().get("state")))
                item.setStyle("-fx-background-radius: 14px; -fx-cursor: hand;");
        });

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(42, 42);
        avatar.setMaxSize(42, 42);
        String color = COLORS[user.getId() % COLORS.length];
        avatar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 9999px;" +
                " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 1);");
        Label initials = new Label(user.getInitials());
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 14px;");
        initials.setMinSize(42, 42);
        initials.setMaxSize(42, 42);
        initials.setAlignment(Pos.CENTER);
        avatar.getChildren().add(initials);

        // Photo de profil
        new Thread(() -> {
            communication.models.UserProfile profile =
                    new UserProfileService().findByUserId(user.getId());
            if (profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
                Platform.runLater(() -> {
                    try {
                        javafx.scene.image.Image img = new javafx.scene.image.Image(
                                profile.getAvatarUrl(), 40, 40, true, true, true);
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(40); iv.setFitHeight(40);
                        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
                        iv.setClip(clip);
                        avatar.getChildren().clear();
                        avatar.getChildren().add(iv);
                    } catch (Exception ignored) {}
                });
            }
        }).start();

        // Badge online
        new Thread(() -> {
            boolean online = userService.isOnline(user.getId());
            if (online) {
                Platform.runLater(() -> {
                    StackPane badge = new StackPane();
                    badge.setMinSize(11, 11);
                    badge.setMaxSize(11, 11);
                    badge.setStyle(
                            "-fx-background-color: #2DAA63; -fx-background-radius: 9999px; " +
                                    "-fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 9999px;"
                    );
                    StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
                    avatar.getChildren().add(badge);
                });
            }
        }).start();

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(user.getFullName());
        name.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #1A1D2E;");
        Label username = new Label("@" + user.getUsername());
        username.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3C4;");
        info.getChildren().addAll(name, username);
        // ✅ Vérifie messages non lus pour cet user
        new Thread(() -> {
            int unread = chatService.countUnreadFromSender(user.getId(), currentUserId);
            if (unread > 0) {
                Platform.runLater(() -> {
                    // Badge rouge avec nombre
                    Label unreadBadge = new Label(String.valueOf(unread));
                    unreadBadge.setStyle(
                            "-fx-background-color: #6C5CE7; -fx-text-fill: white; " +
                                    "-fx-font-size: 10px; -fx-font-weight: 700; " +
                                    "-fx-background-radius: 9999px; -fx-padding: 2 7; " +
                                    "-fx-min-width: 20; -fx-alignment: center;"
                    );
                    name.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #1A1D2E;");
                    item.getChildren().add(unreadBadge);
                });
            }
        }).start();

        // ✅ Bouton supprimer — à droite, caché par défaut
        Button deleteConvBtn = new Button("🗑");
        deleteConvBtn.setStyle(
                "-fx-background-color: #FEE2E2; -fx-text-fill: #E8392A; " +
                        "-fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 0; " +
                        "-fx-background-radius: 6px; -fx-padding: 4 8;"
        );
        deleteConvBtn.setVisible(false);
        deleteConvBtn.setManaged(false);
        deleteConvBtn.setOnAction(e -> {
            e.consume();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer la conversation avec " + user.getFullName() + " ?");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    new Thread(() -> {
                        chatService.deleteConversation(currentUserId, user.getId());
                        Platform.runLater(() -> {
                            if (activeChatUserId != null && activeChatUserId == user.getId()) {
                                messagesList.getChildren().clear();
                                activeChatUserId = null;
                                chatNameLabel.setText("Sélectionnez une conversation");
                                chatStatusLabel.setText("");
                                inputBox.setDisable(true);
                            }
                            loadConversations();
                        });
                    }).start();
                }
            });
        });

        // ✅ Ordre correct : avatar, info, deleteBtn
        item.getChildren().addAll(avatar, info, deleteConvBtn);

        // ✅ Un seul hover listener
        item.setOnMouseEntered(e -> {
            deleteConvBtn.setVisible(true);
            deleteConvBtn.setManaged(true);
            if (activeChatUserId == null || activeChatUserId != user.getId())
                item.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 10px; -fx-cursor: hand;");
        });
        item.setOnMouseExited(e -> {
            deleteConvBtn.setVisible(false);
            deleteConvBtn.setManaged(false);
            if (activeChatUserId == null || activeChatUserId != user.getId())
                item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand;");
        });

        item.setOnMouseClicked(e -> {
            activeChatUserId = user.getId();
            activeChatGroupId = null;
            openPrivateChat(user, item);
        });

        conversationsList.getChildren().add(item);
    }

    // ── Item conversation groupe ───────────────────────────────
    private void addGroupConversationItem(Group group) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand;");

        StackPane avatar = new StackPane();
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        String color = COLORS[group.getId() % COLORS.length];
        avatar.setStyle("-fx-background-color: " + color +
                "; -fx-background-radius: 9999px;");
        Label initials = new Label(String.valueOf(group.getName().charAt(0)).toUpperCase());
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 14px;");
        avatar.getChildren().add(initials);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(group.getName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #111827;");
        Label members = new Label(group.getMemberCount() + " membres");
        members.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        info.getChildren().addAll(name, members);

        item.getChildren().addAll(avatar, info);

        item.setOnMouseEntered(e -> {
            if (activeChatGroupId == null || activeChatGroupId != group.getId())
                item.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 10px; -fx-cursor: hand;");
        });
        item.setOnMouseExited(e -> {
            if (activeChatGroupId == null || activeChatGroupId != group.getId())
                item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand;");
        });

        item.setOnMouseClicked(e -> {
            activeChatGroupId = group.getId();
            activeChatUserId = null;
            openGroupChat(group, item);
        });

        conversationsList.getChildren().add(item);
    }

    // ── Ouvrir chat privé ─────────────────────────────────────
    private void openPrivateChat(User user, HBox selectedItem) {
        // Highlight item sélectionné
        highlightSelected(selectedItem);

        // Header
        String color = COLORS[user.getId() % COLORS.length];
        chatAvatarPane.setStyle("-fx-background-color: " + color +
                "; -fx-background-radius: 9999px;");
        chatAvatarLabel.setText(user.getInitials());
        // Après chatAvatarLabel.setText(user.getInitials())
        new Thread(() -> {
            communication.models.UserProfile profile =
                    new UserProfileService().findByUserId(user.getId());
            if (profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
                Platform.runLater(() -> {
                    try {
                        javafx.scene.image.Image img = new javafx.scene.image.Image(
                                profile.getAvatarUrl(), 42, 42, true, true, true);
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(42); iv.setFitHeight(42);
                        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(21, 21, 21);
                        iv.setClip(clip);
                        chatAvatarPane.getChildren().clear();
                        chatAvatarPane.getChildren().add(iv);
                    } catch (Exception ignored) {}
                });
            }
        }).start();
        chatNameLabel.setText(user.getFullName());
        // ✅ Status dynamique
        new Thread(() -> {
            boolean online = userService.isOnline(user.getId());
            java.time.LocalDateTime lastSeen = userService.getLastSeen(user.getId());

            Platform.runLater(() -> {
                if (online) {
                    chatStatusLabel.setText("🟢 En ligne");
                    chatStatusLabel.setStyle(
                            "-fx-font-size: 11px; -fx-text-fill: #2DAA63; -fx-font-weight: 600;"
                    );
                } else if (lastSeen != null) {
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                            lastSeen, java.time.LocalDateTime.now());

                    String lastSeenText;
                    if (minutes < 1) lastSeenText = "Vu à l'instant";
                    else if (minutes < 60) lastSeenText = "Vu il y a " + minutes + "min";
                    else if (minutes < 1440) lastSeenText = "Vu il y a " + (minutes/60) + "h";
                    else lastSeenText = "Vu il y a " + (minutes/1440) + "j";

                    chatStatusLabel.setText("⚫ " + lastSeenText);
                    chatStatusLabel.setStyle(
                            "-fx-font-size: 11px; -fx-text-fill: #9CA3AF;"
                    );
                } else {
                    chatStatusLabel.setText("⚫ Hors ligne");
                    chatStatusLabel.setStyle(
                            "-fx-font-size: 11px; -fx-text-fill: #9CA3AF;"
                    );
                }
            });
        }).start();

        inputBox.setDisable(false);
        messageInput.requestFocus();

        // Charger messages
        loadMessages(chatService.getPrivateMessages(currentUserId, user.getId()));


        // Marquer comme lu + refresh liste
        new Thread(() -> {
            chatService.markAsRead(user.getId(), currentUserId);
            Platform.runLater(this::loadConversations);
        }).start();
    }

    // ── Ouvrir chat groupe ────────────────────────────────────
    private void openGroupChat(Group group, HBox selectedItem) {
        highlightSelected(selectedItem);

        String color = COLORS[group.getId() % COLORS.length];
        chatAvatarPane.setStyle("-fx-background-color: " + color +
                "; -fx-background-radius: 9999px;");
        chatAvatarLabel.setText(String.valueOf(group.getName().charAt(0)).toUpperCase());
        chatNameLabel.setText(group.getName());
        chatStatusLabel.setText("👥 " + group.getMemberCount() + " membres");

        inputBox.setDisable(false);
        messageInput.requestFocus();

        loadMessages(chatService.getGroupMessages(group.getId()));
    }

    // ── Charger messages dans l'UI ────────────────────────────
    private void loadMessages(List<Message> messages) {
        messagesList.getChildren().clear();

        if (messages.isEmpty()) {
            Label empty = new Label("Aucun message. Commencez la conversation ! 👋");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
            empty.setWrapText(true);
            VBox centered = new VBox(empty);
            centered.setAlignment(Pos.CENTER);
            centered.setPadding(new Insets(40));
            messagesList.getChildren().add(centered);
            return;
        }

        for (Message msg : messages) {
            messagesList.getChildren().add(buildMessageBubble(msg));
        }

        // Scroll en bas
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    // ── Bulle de message ──────────────────────────────────────
    private HBox buildMessageBubble(Message msg) {
        boolean isMe = msg.getSenderId() == currentUserId;

        // ── Outer row ─────────────────────────────────────────────
        HBox row = new HBox(10);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 16, 3, 16));

        // ── Avatar (other side only) ───────────────────────────────
        if (!isMe) {
            StackPane avatar = new StackPane();
            avatar.setMinSize(34, 34);
            avatar.setMaxSize(34, 34);
            String color = COLORS[msg.getSenderId() % COLORS.length];
            avatar.setStyle("-fx-background-color: " + color + ";" +
                    " -fx-background-radius: 9999px;" +
                    " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 1);");
            String initialsText = msg.getSenderAvatar() != null ? msg.getSenderAvatar() : "?";
            Label avatarLabel = new Label(initialsText);
            avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 11.5px;");
            avatarLabel.setMinSize(34, 34);
            avatarLabel.setMaxSize(34, 34);
            avatarLabel.setAlignment(Pos.CENTER);
            avatar.getChildren().add(avatarLabel);

            // Load real profile photo
            new Thread(() -> {
                communication.models.UserProfile profile =
                        new UserProfileService().findByUserId(msg.getSenderId());
                if (profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
                    Platform.runLater(() -> {
                        try {
                            javafx.scene.image.Image img = new javafx.scene.image.Image(
                                    profile.getAvatarUrl(), 34, 34, true, true, true);
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                            iv.setFitWidth(34); iv.setFitHeight(34);
                            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(17, 17, 17);
                            iv.setClip(clip);
                            avatar.getChildren().clear();
                            avatar.getChildren().add(iv);
                        } catch (Exception ignored) {}
                    });
                }
            }).start();

            row.getChildren().add(avatar);
        }

        // ── Bubble container ──────────────────────────────────────
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(340);

        // Sender name (for group / other side)
        if (!isMe) {
            Label senderName = new Label(msg.getSenderName());
            senderName.setStyle(
                    "-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #6C5CE7; -fx-padding: 0 4 0 4;");
            bubble.getChildren().add(senderName);
        }

        // ── Message content label ─────────────────────────────────
        Label content = new Label(msg.getContent());
        content.setWrapText(true);
        content.setMaxWidth(320);

        if (isMe) {
            content.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #6C5CE7, #A29BFE);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 13.5px;" +
                            "-fx-padding: 11 16;" +
                            "-fx-background-radius: 18 18 4 18;" +
                            "-fx-effect: dropshadow(gaussian, rgba(108,92,231,0.35), 10, 0, 0, 3);"
            );
        } else {
            content.setStyle(
                    "-fx-background-color: #FFFFFF;" +
                            "-fx-text-fill: #1A1D2E;" +
                            "-fx-font-size: 13.5px;" +
                            "-fx-padding: 11 16;" +
                            "-fx-background-radius: 18 18 18 4;" +
                            "-fx-border-color: #EAECF5;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 18 18 18 4;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);"
            );
        }

        // ── Timestamp ─────────────────────────────────────────────
        Label time = new Label(msg.getCreatedAt() != null ?
                msg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3C4; -fx-padding: 0 4;");
        time.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        bubble.getChildren().addAll(content, time);
        row.getChildren().add(bubble);

        // ── Right-click delete (own messages) ─────────────────────
        if (isMe && msg.getId() != null) {
            content.setOnMousePressed(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    ContextMenu menu = new ContextMenu();
                    MenuItem deleteItem = new MenuItem("🗑   Supprimer le message");
                    deleteItem.setStyle("-fx-text-fill: #E17055; -fx-font-weight: 600;");
                    deleteItem.setOnAction(ev -> new Thread(() -> {
                        boolean success = chatService.deleteMessage(msg.getId());
                        if (success) Platform.runLater(() ->
                                messagesList.getChildren().remove(row));
                    }).start());
                    menu.getItems().add(deleteItem);
                    menu.show(content, e.getScreenX(), e.getScreenY());
                    e.consume();
                }
            });
        }

        return row;
    }

    // ── Envoyer message ───────────────────────────────────────
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        if (activeChatUserId == null && activeChatGroupId == null) return;

        messageInput.clear();
        String avatar = currentUser != null ? currentUser.getInitials() : "?";
        String name = currentUser != null ? currentUser.getFullName() : "Moi";

        new Thread(() -> {
            boolean success;
            if (activeChatUserId != null) {
                success = chatService.sendPrivateMessage(text, currentUserId, name, avatar, activeChatUserId);
            } else {
                success = chatService.sendGroupMessage(text, currentUserId, name, avatar, activeChatGroupId);
            }
            if (success) {
                // ✅ Recharge depuis Supabase pour avoir les vrais IDs
                List<Message> messages = activeChatUserId != null ?
                        chatService.getPrivateMessages(currentUserId, activeChatUserId) :
                        chatService.getGroupMessages(activeChatGroupId);
                Platform.runLater(() -> {
                    loadMessages(messages);
                    Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
                });
            }
        }).start();
    }

    // ── Nouveau chat dialog ───────────────────────────────────
    private void openNewChatDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("💬 Nouvelle conversation");
        dialog.setHeaderText("Choisir un utilisateur");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(340);

        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        TextField search = new TextField();
        search.setPromptText("Rechercher un utilisateur...");
        search.setStyle("-fx-padding: 8 12; -fx-background-radius: 8px;");

        ListView<User> listView = new ListView<>();
        listView.setPrefHeight(250);
        List<User> allUsers = userService.getAll();
        allUsers.removeIf(u -> u.getId() == currentUserId);
        listView.getItems().addAll(allUsers);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) { setText(null); }
                else setText(user.getFullName() + " (@" + user.getUsername() + ")"); }
        });

        search.textProperty().addListener((obs, old, nv) -> {
            listView.getItems().clear();
            allUsers.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(nv.toLowerCase())
                            || u.getUsername().toLowerCase().contains(nv.toLowerCase()))
                    .forEach(listView.getItems()::add);
        });

        content.getChildren().addAll(search, listView);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(btn ->
                btn == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(user -> {
            if (user != null) {
                showingGroups = false;
                setActiveTab(privateTabBtn, groupTabBtn);
                loadConversations();
                activeChatUserId = user.getId();
                // Trouve et clique l'item
                conversationsList.getChildren().forEach(node -> {
                    if (node instanceof HBox) {
                        HBox item = (HBox) node;
                        item.getChildren().stream()
                                .filter(n -> n instanceof VBox)
                                .findFirst()
                                .ifPresent(vb -> {
                                    VBox info = (VBox) vb;
                                    info.getChildren().stream()
                                            .filter(n -> n instanceof Label)
                                            .map(n -> (Label) n)
                                            .filter(l -> l.getText().equals(user.getFullName()))
                                            .findFirst()
                                            .ifPresent(l -> openPrivateChat(user, item));
                                });
                    }
                });
            }
        });
    }

    // ── WebSocket — nouveau message reçu ──────────────────────
    private void onNewMessageReceived(Message msg) {
        System.out.println("📨 onNewMessageReceived: " + msg.getSenderName() +
                " | activeChatUserId=" + activeChatUserId);

        boolean isCurrentChat =
                (activeChatUserId != null && msg.getSenderId() == activeChatUserId) ||
                        (activeChatGroupId != null && activeChatGroupId.equals(msg.getGroupId()));

        System.out.println("isCurrentChat=" + isCurrentChat);

        if (isCurrentChat && msg.getSenderId() != currentUserId) {
            messagesList.getChildren().add(buildMessageBubble(msg));
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
        } else if (msg.getSenderId() != currentUserId) {
            System.out.println("🔔 Envoi notification toast...");
            Platform.runLater(() -> {
                feedController.showChatNotification(msg);
                feedController.updateChatBadge();
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private void highlightSelected(HBox selected) {
        conversationsList.getChildren().forEach(n -> {
            if (n instanceof HBox) {
                ((HBox) n).setStyle("-fx-background-radius: 14px; -fx-cursor: hand;");
                ((HBox) n).getProperties().put("state", "inactive");
            }
        });
        selected.setStyle(
                "-fx-background-color: #F0EEFF; -fx-background-radius: 14px; -fx-cursor: hand;" +
                        "-fx-border-color: #D4CAFF; -fx-border-width: 1; -fx-border-radius: 14px;"
        );
        selected.getProperties().put("state", "selected");
    }

    private void setActiveTab(Button active, Button inactive) {
        active.setStyle(
                "-fx-background-color: #6C5CE7; -fx-text-fill: white; " +
                        "-fx-font-weight: 700; -fx-font-size: 12.5px; " +
                        "-fx-background-radius: 12px 0 0 12px; " +
                        "-fx-border-width: 0; -fx-cursor: hand; -fx-padding: 9 0;"
        );
        inactive.setStyle(
                "-fx-background-color: #F4F5FB; -fx-text-fill: #5A5F7D; " +
                        "-fx-font-weight: 600; -fx-font-size: 12.5px; " +
                        "-fx-background-radius: 0 12px 12px 0; " +
                        "-fx-border-color: #EAECF5; -fx-border-width: 1; " +
                        "-fx-cursor: hand; -fx-padding: 9 0;"
        );
    }

    private void filterConversations(String query) {
        if (showingGroups) loadGroupConversations();
        else loadConversations();

        if (query == null || query.isBlank()) return;
        conversationsList.getChildren().removeIf(node -> {
            if (node instanceof HBox) {
                HBox item = (HBox) node;
                return item.getChildren().stream()
                        .filter(n -> n instanceof VBox)
                        .flatMap(n -> ((VBox) n).getChildren().stream())
                        .filter(n -> n instanceof Label)
                        .map(n -> ((Label) n).getText().toLowerCase())
                        .noneMatch(t -> t.contains(query.toLowerCase()));
            }
            return false;
        });
    }


}