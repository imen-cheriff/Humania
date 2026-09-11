package communication.services;

import communication.models.Message;
import com.google.gson.*;
import okhttp3.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatService {

    private final OkHttpClient client = new OkHttpClient();
    private WebSocket webSocket;
    private Consumer<Message> onNewMessage;
    private final java.util.Set<String> processedIds =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    // ✅ Scheduler sauvegardé pour pouvoir l'arrêter
    private java.util.concurrent.ScheduledExecutorService pollingScheduler;

    // ── Envoyer un message privé ──────────────────────────────
    public boolean sendPrivateMessage(String content, int senderId, String senderName,
                                      String senderAvatar, int receiverId) {
        JsonObject body = new JsonObject();
        body.addProperty("content", content);
        body.addProperty("sender_id", senderId);
        body.addProperty("sender_name", senderName);
        body.addProperty("sender_avatar", senderAvatar);
        body.addProperty("receiver_id", receiverId);
        body.addProperty("is_read", false);
        return postMessage(body);
    }

    // ── Envoyer un message de groupe ─────────────────────────
    public boolean sendGroupMessage(String content, int senderId, String senderName,
                                    String senderAvatar, int groupId) {
        JsonObject body = new JsonObject();
        body.addProperty("content", content);
        body.addProperty("sender_id", senderId);
        body.addProperty("sender_name", senderName);
        body.addProperty("sender_avatar", senderAvatar);
        body.addProperty("group_id", groupId);
        body.addProperty("is_read", false);
        return postMessage(body);
    }

    private boolean postMessage(JsonObject body) {
        try {
            Request request = new Request.Builder()
                    .url(SupabaseConfig.MESSAGES_URL)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(RequestBody.create(body.toString(),
                            MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            System.err.println("❌ Send message error: " + e.getMessage());
            return false;
        }
    }

    // ── Récupérer messages privés ─────────────────────────────
    public List<Message> getPrivateMessages(int userId, int otherId) {
        String url = SupabaseConfig.MESSAGES_URL +
                "?or=(and(sender_id.eq." + userId + ",receiver_id.eq." + otherId + ")," +
                "and(sender_id.eq." + otherId + ",receiver_id.eq." + userId + "))" +
                "&order=created_at.asc&limit=100";
        return fetchMessages(url);
    }

    // ── Récupérer messages de groupe ──────────────────────────
    public List<Message> getGroupMessages(int groupId) {
        String url = SupabaseConfig.MESSAGES_URL +
                "?group_id=eq." + groupId +
                "&order=created_at.asc&limit=100";
        return fetchMessages(url);
    }

    // ── Compter messages non lus ──────────────────────────────
    public int countUnread(int receiverId) {
        try {
            String url = SupabaseConfig.MESSAGES_URL +
                    "?receiver_id=eq." + receiverId +
                    "&is_read=is.false";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                    .addHeader("Prefer", "count=exact")
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String countHeader = response.header("Content-Range");
                if (countHeader != null && countHeader.contains("/")) {
                    return Integer.parseInt(countHeader.split("/")[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ countUnread error: " + e.getMessage());
        }
        return 0;
    }

    // ── Compter non lus d'un expéditeur spécifique ────────────
    public int countUnreadFromSender(int senderId, int receiverId) {
        try {
            String url = SupabaseConfig.MESSAGES_URL +
                    "?sender_id=eq." + senderId +
                    "&receiver_id=eq." + receiverId +
                    "&is_read=is.false";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                    .addHeader("Prefer", "count=exact")
                    .head()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String contentRange = response.header("Content-Range");
                if (contentRange != null && contentRange.contains("/")) {
                    return Integer.parseInt(contentRange.split("/")[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ countUnreadFromSender error: " + e.getMessage());
        }
        return 0;
    }

    // ── Marquer comme lu ──────────────────────────────────────
    public void markAsRead(int senderId, int receiverId) {
        try {
            String url = SupabaseConfig.MESSAGES_URL +
                    "?sender_id=eq." + senderId +
                    "&receiver_id=eq." + receiverId +
                    "&is_read=is.false";
            JsonObject body = new JsonObject();
            body.addProperty("is_read", true);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .patch(RequestBody.create(body.toString(),
                            MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                System.out.println("✅ Messages marqués comme lus");
            }
        } catch (Exception e) {
            System.err.println("❌ markAsRead error: " + e.getMessage());
        }
    }

    // ── Supprimer message ─────────────────────────────────────
    public boolean deleteMessage(String messageId) {
        try {
            String url = SupabaseConfig.MESSAGES_URL + "?id=eq." + messageId;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            System.err.println("❌ deleteMessage error: " + e.getMessage());
            return false;
        }
    }

    // ── Supprimer conversation ────────────────────────────────
    public boolean deleteConversation(int userId, int otherId) {
        try {
            String url = SupabaseConfig.MESSAGES_URL +
                    "?or=(and(sender_id.eq." + userId + ",receiver_id.eq." + otherId + ")," +
                    "and(sender_id.eq." + otherId + ",receiver_id.eq." + userId + "))";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            System.err.println("❌ deleteConversation error: " + e.getMessage());
            return false;
        }
    }

    // ── WebSocket Realtime ────────────────────────────────────
    public void subscribeToMessages(int currentUserId, Consumer<Message> onMessage) {
        this.onNewMessage = onMessage;
        String url = SupabaseConfig.REALTIME_URL + "?apikey=" + SupabaseConfig.ANON_KEY;
        Request request = new Request.Builder().url(url).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                String joinMsg = "{"
                        + "\"topic\":\"realtime:public:messages\","
                        + "\"event\":\"phx_join\","
                        + "\"payload\":{"
                        +   "\"config\":{"
                        +     "\"broadcast\":{\"self\":false},"
                        +     "\"presence\":{\"key\":\"\"},"
                        +     "\"postgres_changes\":[{"
                        +       "\"event\":\"INSERT\","
                        +       "\"schema\":\"public\","
                        +       "\"table\":\"messages\""
                        +     "}]"
                        +   "}"
                        + "},"
                        + "\"ref\":\"1\""
                        + "}";
                ws.send(joinMsg);
                System.out.println("✅ WebSocket Supabase connecté");
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                    String event = json.has("event") ? json.get("event").getAsString() : "";

                    if ("heartbeat".equals(event) || "phx_reply".equals(event)) {
                        ws.send("{\"topic\":\"phoenix\",\"event\":\"heartbeat\",\"payload\":{},\"ref\":\"hb\"}");
                        return;
                    }

                    if ("postgres_changes".equals(event)) {
                        JsonObject payload = json.getAsJsonObject("payload");
                        if (payload != null && payload.has("data")) {
                            JsonObject data = payload.getAsJsonObject("data");
                            if (data.has("record")) {
                                Message msg = parseMessage(data.getAsJsonObject("record"));
                                boolean isForMe = msg.getReceiverId() != null &&
                                        msg.getReceiverId() == currentUserId;
                                boolean isFromGroup = msg.getGroupId() != null;
                                boolean isNotMine = msg.getSenderId() != currentUserId;
                                if ((isForMe || isFromGroup) && isNotMine) {
                                    javafx.application.Platform.runLater(() ->
                                            onNewMessage.accept(msg));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ WebSocket parse error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                System.err.println("❌ WebSocket failure: " + t.getMessage());
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        subscribeToMessages(currentUserId, onMessage);
                    } catch (InterruptedException ignored) {}
                }).start();
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                System.out.println("🔌 WebSocket fermé: " + reason);
            }
        });
    }

    // ── Polling ───────────────────────────────────────────────
    public void startPolling(int currentUserId, Consumer<Message> onMessage) {
        // ✅ Stop any existing poller before creating a new one (prevents accumulation on re-login)
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            pollingScheduler.shutdownNow();
        }
        // ✅ Scheduler sauvegardé comme champ
        pollingScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        pollingScheduler.scheduleAtFixedRate(() -> {
            try {
                String url = SupabaseConfig.MESSAGES_URL +
                        "?receiver_id=eq." + currentUserId +
                        "&is_read=is.false" +
                        "&order=created_at.desc&limit=10";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SupabaseConfig.ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.body() == null) return;
                    String body = response.body().string();
                    JsonArray array = JsonParser.parseString(body).getAsJsonArray();

                    // Only log when there are actual unread messages
                    if (array.size() > 0) {
                        System.out.println("📨 Messages non lus trouvés: " + array.size());
                    }

                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        String id = obj.get("id").getAsString();
                        // ✅ CORRIGÉ : processedIds.contains() pas equals()
                        if (!processedIds.contains(id)) {
                            processedIds.add(id);
                            Message msg = parseMessage(obj);
                            System.out.println("🆕 Nouveau message de: " + msg.getSenderName());
                            javafx.application.Platform.runLater(() -> onMessage.accept(msg));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Polling error: " + e.getMessage());
            }
        }, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    // ── Disconnect — arrête TOUT ──────────────────────────────
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Bye");
            webSocket = null;
        }
        // ✅ Arrête le polling
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            pollingScheduler.shutdownNow();
            System.out.println("✅ Polling arrêté");
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private List<Message> fetchMessages(String url) {
        List<Message> messages = new ArrayList<>();
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.body() == null) return messages;
                String body = response.body().string();
                JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                for (JsonElement el : array) {
                    messages.add(parseMessage(el.getAsJsonObject()));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ fetchMessages error: " + e.getMessage());
        }
        return messages;
    }

    private Message parseMessage(JsonObject obj) {
        Message msg = new Message();
        if (obj.has("id") && !obj.get("id").isJsonNull())
            msg.setId(obj.get("id").getAsString());
        if (obj.has("content") && !obj.get("content").isJsonNull())
            msg.setContent(obj.get("content").getAsString());
        if (obj.has("sender_id") && !obj.get("sender_id").isJsonNull())
            msg.setSenderId(obj.get("sender_id").getAsInt());
        if (obj.has("sender_name") && !obj.get("sender_name").isJsonNull())
            msg.setSenderName(obj.get("sender_name").getAsString());
        if (obj.has("sender_avatar") && !obj.get("sender_avatar").isJsonNull())
            msg.setSenderAvatar(obj.get("sender_avatar").getAsString());
        if (obj.has("receiver_id") && !obj.get("receiver_id").isJsonNull())
            msg.setReceiverId(obj.get("receiver_id").getAsInt());
        if (obj.has("group_id") && !obj.get("group_id").isJsonNull())
            msg.setGroupId(obj.get("group_id").getAsInt());
        if (obj.has("is_read") && !obj.get("is_read").isJsonNull())
            msg.setRead(obj.get("is_read").getAsBoolean());
        try {
            if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
                String dateStr = obj.get("created_at").getAsString().substring(0, 19);
                msg.setCreatedAt(LocalDateTime.parse(dateStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            }
        } catch (Exception ignored) {}
        return msg;
    }
}