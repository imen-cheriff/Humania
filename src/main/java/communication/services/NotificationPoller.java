package communication.services;

import javafx.application.Platform;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class NotificationPoller {

    private final NotificationService notificationService;
    private final int userId;
    private final Runnable onBadgeUpdate;           // → updateNotificationBadge()
    private final Consumer<String> onNewNotif;      // → message de la nouvelle notif
    private final javafx.stage.Stage stage;

    private ScheduledExecutorService executor;
    private int lastUnreadCount = -1;
    private TrayIcon trayIcon;

    public NotificationPoller(
            NotificationService notificationService,
            int userId,
            javafx.stage.Stage stage,
            Runnable onBadgeUpdate,
            Consumer<String> onNewNotif) {

        this.notificationService = notificationService;
        this.userId = userId;
        this.stage = stage;
        this.onBadgeUpdate = onBadgeUpdate;
        this.onNewNotif = onNewNotif;

        setupSystemTray();
    }

    // ── Setup System Tray ──────────────────────────────────────
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;
        try {
            // ✅ Icône par défaut si logo introuvable
            Image icon;
            var iconUrl = getClass().getResource("/images/logo.png");
            if (iconUrl != null) {
                icon = Toolkit.getDefaultToolkit().createImage(iconUrl);
            } else {
                // Crée une icône colorée simple 16x16
                java.awt.image.BufferedImage img =
                        new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setColor(new Color(61, 126, 232)); // bleu Humania
                g.fillOval(0, 0, 16, 16);
                g.dispose();
                icon = img;
            }

            trayIcon = new TrayIcon(icon, "HUMANIA");
            trayIcon.setImageAutoSize(true);

            // Clic sur la notif système → remet l'app au premier plan
            trayIcon.addActionListener(e ->
                    Platform.runLater(() -> {
                        stage.show();
                        stage.toFront();
                        stage.requestFocus();
                    })
            );

            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            System.err.println("⚠️ System tray non disponible : " + e.getMessage());
        }
    }

    // ── Start polling ──────────────────────────────────────────
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notif-poller");
            t.setDaemon(true); // ✅ S'arrête quand l'app se ferme
            return t;
        });

        executor.scheduleAtFixedRate(this::poll, 0, 5, TimeUnit.SECONDS);
        System.out.println("🔔 NotificationPoller démarré (toutes les 5s)");
    }

    // ── Stop polling ───────────────────────────────────────────
    public void stop() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            System.out.println("🔕 NotificationPoller arrêté");
        }
        if (trayIcon != null) {
            try { SystemTray.getSystemTray().remove(trayIcon); }
            catch (Exception ignored) {}
        }
    }

    // ── Poll logic ─────────────────────────────────────────────
    private void poll() {
        try {
            int currentUnread = notificationService.countUnread(userId);

            if (lastUnreadCount == -1) {
                // Premier poll — initialise sans notifier
                lastUnreadCount = currentUnread;
                return;
            }

            if (currentUnread > lastUnreadCount) {
                // ✅ Nouvelles notifs détectées !
                int newCount = currentUnread - lastUnreadCount;

                // Récupère le message de la dernière notif
                String message = getLatestNotifMessage();

                // Met à jour l'UI (thread-safe)
                Platform.runLater(() -> {
                    onBadgeUpdate.run();
                    if (onNewNotif != null) onNewNotif.accept(message);
                });

                // Notification système si app en arrière-plan
                if (!stage.isFocused() && trayIcon != null) {
                    trayIcon.displayMessage(
                            "HUMANIA — " + newCount + " nouvelle(s) notification(s)",
                            message,
                            MessageType.INFO
                    );
                }
            }

            lastUnreadCount = currentUnread;

        } catch (Exception e) {
            System.err.println("❌ Erreur polling notifs : " + e.getMessage());
        }
    }

    private String getLatestNotifMessage() {
        try {
            var notifs = notificationService.findByUserId(userId);
            if (notifs != null && !notifs.isEmpty()) {
                notifs.sort((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()));
                return notifs.get(0).getMessage();
            }
        } catch (Exception ignored) {}
        return "Vous avez une nouvelle notification";
    }
}