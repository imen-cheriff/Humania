package planification.services;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ResendService {

    // ── Configuration ────────────────────────────────────────────────────
    private static final String RESEND_API_KEY = "re_UJ3ipi9i_2gdahf8TcJRkbohwSTSuZFui";
    private static final String FROM_EMAIL     = "onboarding@resend.dev";
    private static final String FROM_NAME      = "Calendrier des Réunions";

    private static final String API_URL = "https://api.resend.com/emails";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter
            .ofPattern("HH:mm");

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Sends a Zoom meeting invitation to every email in the recipients list.
     *
     * @param recipients   List of participant email addresses
     * @param meetingTitle Title of the meeting
     * @param startDate    Meeting start date/time
     * @param endDate      Meeting end date/time
     * @param zoom         The Zoom meeting details (URLs, password, ID)
     * @param organizer    Name of the meeting organizer
     */
    public void sendMeetingInvitation(
            List<String> recipients,
            String meetingTitle,
            Date startDate,
            Date endDate,
            ZoomService.ZoomMeetingResult zoom,
            String organizer) throws Exception {

        if (recipients == null || recipients.isEmpty()) return;

        String subject  = "Invitation : " + meetingTitle;
        String htmlBody = buildHtmlEmail(meetingTitle, startDate, endDate, zoom, organizer);
        String textBody = buildPlainTextEmail(meetingTitle, startDate, endDate, zoom, organizer);

        for (String email : recipients) {
            email = email.trim();
            if (email.isEmpty() || !email.contains("@")) continue;
            sendEmail(email, subject, htmlBody, textBody);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String html, String text) throws Exception {
        // Escape quotes and newlines for JSON
        String safeSubject = jsonEscape(subject);
        String safeHtml    = jsonEscape(html);
        String safeText    = jsonEscape(text);
        String safeTo      = jsonEscape(to);

        String body = "{"
                + "\"from\":\""    + FROM_NAME + " <" + FROM_EMAIL + ">\","
                + "\"to\":[\""     + safeTo    + "\"],"
                + "\"subject\":\"" + safeSubject + "\","
                + "\"html\":\""    + safeHtml    + "\","
                + "\"text\":\""    + safeText    + "\""
                + "}";

        HttpClient  client  = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + RESEND_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new Exception("Resend API error " + response.statusCode() + ": " + response.body());
        }
    }

    // ── Email templates ──────────────────────────────────────────────────

    private String buildHtmlEmail(
            String title, Date start, Date end,
            ZoomService.ZoomMeetingResult zoom, String organizer) {

        String dateStr  = formatDate(start);
        String timeStr  = formatTime(start) + " – " + formatTime(end);
        String joinUrl  = zoom.joinUrl  != null ? zoom.joinUrl  : "";
        String startUrl = zoom.startUrl != null ? zoom.startUrl : "";
        String password = zoom.password != null ? zoom.password : "";
        String meetId   = String.valueOf(zoom.meetingId);

        return "<!DOCTYPE html>"
                + "<html lang='fr'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>Invitation réunion</title></head>"
                + "<body style='margin:0;padding:0;background:#f1f5f9;font-family:Arial,sans-serif;'>"

                // Outer wrapper
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f1f5f9;padding:32px 0;'><tr><td align='center'>"
                + "<table width='560' cellpadding='0' cellspacing='0' style='background:white;border-radius:12px;"
                +        "box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;'>"

                // Header band
                + "<tr><td style='background:linear-gradient(135deg,#667eea,#764ba2);padding:32px 40px;text-align:center;'>"
                + "<div style='font-size:36px;margin-bottom:8px;'>📅</div>"
                + "<h1 style='color:white;margin:0;font-size:22px;font-weight:700;'>" + htmlEscape(title) + "</h1>"
                + "<p style='color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;'>Invitation à une réunion en ligne</p>"
                + "</td></tr>"

                // Date/time info card
                + "<tr><td style='padding:28px 40px 0;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f8fafc;border-radius:8px;"
                +        "border:1px solid #e2e8f0;'>"
                + "<tr>"
                + "  <td style='padding:16px 20px;border-right:1px solid #e2e8f0;'>"
                + "    <div style='font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:0.5px;'>DATE</div>"
                + "    <div style='font-size:14px;color:#1e293b;font-weight:600;margin-top:4px;'>" + htmlEscape(dateStr) + "</div>"
                + "  </td>"
                + "  <td style='padding:16px 20px;border-right:1px solid #e2e8f0;'>"
                + "    <div style='font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:0.5px;'>HORAIRE</div>"
                + "    <div style='font-size:14px;color:#1e293b;font-weight:600;margin-top:4px;'>" + htmlEscape(timeStr) + "</div>"
                + "  </td>"
                + "  <td style='padding:16px 20px;'>"
                + "    <div style='font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:0.5px;'>ORGANISATEUR</div>"
                + "    <div style='font-size:14px;color:#1e293b;font-weight:600;margin-top:4px;'>" + htmlEscape(organizer) + "</div>"
                + "  </td>"
                + "</tr></table>"
                + "</td></tr>"

                // Join button (main CTA)
                + "<tr><td style='padding:28px 40px 0;text-align:center;'>"
                + "<a href='" + joinUrl + "' style='display:inline-block;background:linear-gradient(135deg,#667eea,#764ba2);"
                +    "color:white;text-decoration:none;padding:14px 36px;border-radius:8px;"
                +    "font-size:15px;font-weight:700;letter-spacing:0.3px;'>🔗  Rejoindre la réunion</a>"
                + "</td></tr>"

                // Details section
                + "<tr><td style='padding:24px 40px 0;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"

                // Meeting ID
                + "<tr><td style='padding:8px 0;border-bottom:1px solid #f1f5f9;'>"
                + "<span style='font-size:12px;color:#64748b;'>🆔  ID de réunion</span>"
                + "<span style='float:right;font-size:12px;font-weight:600;color:#1e293b;font-family:monospace;'>" + meetId + "</span>"
                + "</td></tr>"

                // Password
                + (password.isEmpty() ? "" :
                "<tr><td style='padding:8px 0;border-bottom:1px solid #f1f5f9;'>"
                        + "<span style='font-size:12px;color:#64748b;'>🔒  Mot de passe</span>"
                        + "<span style='float:right;font-size:12px;font-weight:600;color:#1e293b;font-family:monospace;'>" + htmlEscape(password) + "</span>"
                        + "</td></tr>")

                // Join URL text
                + "<tr><td style='padding:8px 0;'>"
                + "<span style='font-size:12px;color:#64748b;'>🔗  Lien de connexion</span><br>"
                + "<a href='" + joinUrl + "' style='font-size:11px;color:#667eea;word-break:break-all;'>" + joinUrl + "</a>"
                + "</td></tr>"

                + "</table></td></tr>"

                // Info note
                + "<tr><td style='padding:20px 40px;'>"
                + "<div style='background:#eff6ff;border-radius:8px;border-left:3px solid #3b82f6;padding:12px 16px;'>"
                + "<p style='margin:0;font-size:12px;color:#1e40af;'>"
                + "ℹ️  Cliquez sur <strong>Rejoindre la réunion</strong> ou copiez le lien dans votre navigateur. "
                + "Assurez-vous d'avoir Zoom installé avant la réunion."
                + "</p></div>"
                + "</td></tr>"

                // Footer
                + "<tr><td style='background:#f8fafc;padding:20px 40px;text-align:center;"
                +        "border-top:1px solid #e2e8f0;'>"
                + "<p style='margin:0;font-size:11px;color:#94a3b8;'>Cet email a été envoyé automatiquement par le Calendrier des Réunions.</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String buildPlainTextEmail(
            String title, Date start, Date end,
            ZoomService.ZoomMeetingResult zoom, String organizer) {

        String dateStr  = formatDate(start);
        String timeStr  = formatTime(start) + " – " + formatTime(end);

        return "INVITATION : " + title + "\n"
                + "==========================================\n\n"
                + "Date       : " + dateStr + "\n"
                + "Horaire    : " + timeStr + "\n"
                + "Organisateur: " + organizer + "\n\n"
                + "── Rejoindre la réunion ──────────────────\n"
                + "Lien       : " + (zoom.joinUrl  != null ? zoom.joinUrl  : "") + "\n"
                + "ID réunion : " + zoom.meetingId + "\n"
                + (zoom.password != null && !zoom.password.isEmpty()
                ? "Mot de passe: " + zoom.password + "\n" : "")
                + "\n── Lien hôte (organisateur uniquement) ──\n"
                + (zoom.startUrl != null ? zoom.startUrl : "") + "\n\n"
                + "--\nCet email a été envoyé automatiquement par le Calendrier des Réunions.";
    }

    // ── String utilities ─────────────────────────────────────────────────

    /** Escapes a string for safe embedding inside a JSON string value. */
    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Escapes a string for safe embedding inside HTML. */
    private String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String formatDate(Date d) {
        if (d == null) return "";
        return d.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate().format(DATE_FMT);
    }

    private String formatTime(Date d) {
        if (d == null) return "--:--";
        return d.toInstant().atZone(ZoneId.systemDefault())
                .toLocalTime().format(TIME_FMT);
    }
}