package planification.services;


import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.time.format.*;
import java.util.Date;



    public class ZoomService {

        // ── Configuration ────────────────────────────────────────────────────
        // Replace these with your real credentials (or load from a config file / env vars)
        private static final String ACCOUNT_ID    = "zflLiAYvSs6P5Qe7Xev6Rg";
        private static final String CLIENT_ID     = "KW6HkW0QRDyBbjJ2nxjs4w";
        private static final String CLIENT_SECRET = "gL9JyxxWGULdnuQ6H19XNV6cRa3sTjpl";
        private static final String ZOOM_USER_ID  = "me"; // or host email address

        private static final String TOKEN_URL     = "https://zoom.us/oauth/token";
        private static final String API_BASE      = "https://api.zoom.us/v2";

        // ── Result DTO ───────────────────────────────────────────────────────
        public static class ZoomMeetingResult {
            public final long   meetingId;
            public final String joinUrl;
            public final String startUrl;
            public final String password;

            public ZoomMeetingResult(long meetingId, String joinUrl, String startUrl, String password) {
                this.meetingId = meetingId;
                this.joinUrl   = joinUrl;
                this.startUrl  = startUrl;
                this.password  = password;
            }
        }

        // ── Public API ───────────────────────────────────────────────────────

        /**
         * Creates a Zoom meeting and returns the result containing join/start URLs.
         *
         * @param topic     Meeting title
         * @param startTime Meeting start time
         * @param endTime   Meeting end time (used to compute duration in minutes)
         * @param agenda    Optional description / agenda text
         * @return ZoomMeetingResult with URLs and meeting ID
         * @throws Exception on any network or API error
         */
        public ZoomMeetingResult createMeeting(String topic, Date startTime, Date endTime, String agenda)
                throws Exception {

            String token = fetchAccessToken();

            // Compute duration in minutes (min 1, max 1440)
            long durationMinutes = 60;
            if (startTime != null && endTime != null) {
                durationMinutes = (endTime.getTime() - startTime.getTime()) / 60_000L;
                durationMinutes = Math.max(1, Math.min(1440, durationMinutes));
            }

            // Format start time as ISO 8601 UTC
            String startIso = "";
            if (startTime != null) {
                startIso = Instant.ofEpochMilli(startTime.getTime())
                        .atZone(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            }

            // Build JSON body
            String safeAgenda = (agenda != null ? agenda.replace("\"", "'") : "");
            if (safeAgenda.length() > 2000) safeAgenda = safeAgenda.substring(0, 2000);

            String safeTopic = (topic != null ? topic.replace("\"", "'") : "Réunion");
            if (safeTopic.length() > 200) safeTopic = safeTopic.substring(0, 200);

            String body = "{"
                    + "\"topic\":\""    + safeTopic      + "\","
                    + "\"type\":2,"                              // 2 = scheduled meeting
                    + "\"start_time\":\"" + startIso     + "\","
                    + "\"duration\":"  + durationMinutes  + ","
                    + "\"timezone\":\"Africa/Tunis\","
                    + "\"agenda\":\""  + safeAgenda       + "\","
                    + "\"settings\":{"
                    +   "\"host_video\":true,"
                    +   "\"participant_video\":true,"
                    +   "\"join_before_host\":false,"
                    +   "\"mute_upon_entry\":true,"
                    +   "\"waiting_room\":true,"
                    +   "\"auto_recording\":\"none\""
                    + "}"
                    + "}";

            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/users/" + ZOOM_USER_ID + "/meetings"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new Exception("Zoom API error " + response.statusCode() + ": " + response.body());
            }

            return parseCreateResponse(response.body());
        }

        /**
         * Deletes a Zoom meeting by its numeric meeting ID.
         *
         * @param zoomMeetingId The numeric meeting ID returned when the meeting was created
         * @throws Exception on any network or API error
         */
        public void deleteMeeting(long zoomMeetingId) throws Exception {
            String token = fetchAccessToken();

            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/meetings/" + zoomMeetingId
                            + "?schedule_for_reminder=false&cancel_meeting_reminder=false"))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 204 = success, 404 = already gone — both are acceptable
            if (response.statusCode() != 204 && response.statusCode() != 404) {
                throw new Exception("Zoom delete error " + response.statusCode() + ": " + response.body());
            }
        }

        // ── Private helpers ──────────────────────────────────────────────────

        /** Fetches a short-lived Server-to-Server OAuth access token. */
        private String fetchAccessToken() throws Exception {
            // Basic auth = Base64(clientId:clientSecret)
            String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
            String encoded = java.util.Base64.getEncoder()
                    .encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            String formBody = "grant_type=account_credentials&account_id=" + ACCOUNT_ID;

            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Authorization", "Basic " + encoded)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new Exception("Zoom OAuth error " + response.statusCode() + ": " + response.body());
            }

            return extractJsonString(response.body(), "access_token");
        }

        /** Parses the create-meeting JSON response without a third-party library. */
        private ZoomMeetingResult parseCreateResponse(String json) throws Exception {
            long   meetingId = Long.parseLong(extractJsonString(json, "id").replace(".0", "").trim());
            String joinUrl   = extractJsonString(json, "join_url");
            String startUrl  = extractJsonString(json, "start_url");
            String password  = extractJsonString(json, "password");
            return new ZoomMeetingResult(meetingId, joinUrl, startUrl, password);
        }

        /**
         * Minimal JSON string extractor — avoids pulling in Gson/Jackson.
         * Works for flat string and number values at any depth.
         */
        private String extractJsonString(String json, String key) {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex < 0) return "";

            int colon = json.indexOf(':', keyIndex + searchKey.length());
            if (colon < 0) return "";

            // Skip whitespace after colon
            int valueStart = colon + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            if (valueStart >= json.length()) return "";

            char first = json.charAt(valueStart);
            if (first == '"') {
                // String value
                int end = valueStart + 1;
                while (end < json.length()) {
                    if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                    end++;
                }
                return json.substring(valueStart + 1, end);
            } else {
                // Number / boolean / null
                int end = valueStart;
                while (end < json.length() && ",}\n\r ".indexOf(json.charAt(end)) < 0) {
                    end++;
                }
                return json.substring(valueStart, end).trim();
            }
        }
    }
