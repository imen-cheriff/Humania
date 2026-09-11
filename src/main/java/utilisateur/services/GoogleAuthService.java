package utilisateur.services;

import utils.PropertiesUtil;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Google OAuth2 login flow for a desktop JavaFX app.
 *
 * Flow:
 *  1. Opens system browser to Google's OAuth consent screen.
 *  2. Local HTTP server on port 8080 catches the redirect + auth code.
 *  3. Exchanges code for access token, fetches email from Google.
 *  4. Calls the provided emailValidator to check if the email exists in DB.
 *  5. Shows success or "access denied" page in the browser accordingly.
 *  6. Returns the GoogleUser — or throws if email not found or any error.
 */
public class GoogleAuthService {

    private static final String AUTH_URL     = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final int    PORT         = 8080;

    public record GoogleUser(String email, String nom, String prenom, String googleId) {}

    /**
     * Full OAuth2 flow.
     *
     * @param emailValidator called with the email from Google.
     *                       Return null if the email is valid (exists in DB).
     *                       Return an error message string if access should be denied.
     *                       This is called on the background thread — safe for DB access.
     */
    public GoogleUser authenticate(Function<String, String> emailValidator) throws Exception {
        String clientId     = PropertiesUtil.getProperty("google.client.id");
        String clientSecret = PropertiesUtil.getProperty("google.client.secret");

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new Exception(
                    "Configuration manquante: 'google.client.id' et 'google.client.secret' " +
                            "requis dans application.properties.");
        }

        String authUrl = AUTH_URL
                + "?client_id="    + URLEncoder.encode(clientId,     StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope="        + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=select_account";

        // Start callback server BEFORE opening browser
        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        startCallbackServer(codeFuture);

        // Open browser
        if (!Desktop.isDesktopSupported())
            throw new Exception("Impossible d'ouvrir le navigateur automatiquement.");
        Desktop.getDesktop().browse(new URI(authUrl));

        // Wait up to 3 minutes for the user to complete login
        String code;
        try {
            code = codeFuture.get(3, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            throw new Exception("Timeout: connexion Google non completee dans les 3 minutes.");
        }

        if (code == null || code.isBlank())
            throw new Exception("Connexion Google annulee.");

        // Exchange code → access token → user info
        String     accessToken = exchangeCodeForToken(code, clientId, clientSecret);
        GoogleUser googleUser  = fetchUserInfo(accessToken);

        // ── Validate email against DB (via the lambda passed by LoginController) ──
        String validationError = emailValidator.apply(googleUser.email());
        if (validationError != null) {
            // Show "access denied" page in the browser
            sendBrowserPage(buildDeniedPage(googleUser.email(), validationError));
            throw new Exception(validationError);
        }

        // Show success page in the browser
        sendBrowserPage(buildSuccessPage(googleUser.prenom()));
        return googleUser;
    }

    // ── Callback HTTP server ──────────────────────────────────────────────────

    // We keep a reference to send the response page after validation
    private PrintWriter  browserOut;
    private Socket       browserSocket;

    private void startCallbackServer(CompletableFuture<String> codeFuture) {
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT)) {
                server.setSoTimeout(200_000);
                Socket socket  = server.accept();
                browserSocket  = socket;
                BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                browserOut         = new PrintWriter(socket.getOutputStream(), true);

                String requestLine = in.readLine();
                String code = null;
                if (requestLine != null && requestLine.contains("code=")) {
                    String query = requestLine.split(" ")[1];
                    for (String param : query.substring(query.indexOf('?') + 1).split("&")) {
                        if (param.startsWith("code="))
                            code = URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
                    }
                }

                // Don't send the browser response yet — wait for DB validation
                // (sendBrowserPage will be called after authenticate() validates the email)
                codeFuture.complete(code != null ? code : "");

            } catch (Exception e) {
                codeFuture.completeExceptionally(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void sendBrowserPage(String html) {
        if (browserOut == null) return;
        try {
            browserOut.println("HTTP/1.1 200 OK");
            browserOut.println("Content-Type: text/html; charset=UTF-8");
            browserOut.println("Connection: close");
            browserOut.println();
            browserOut.println(html);
            browserOut.flush();
            if (browserSocket != null) browserSocket.close();
        } catch (Exception ignored) {}
    }

    // ── Browser HTML pages ────────────────────────────────────────────────────

    private String buildSuccessPage(String prenom) {
        return "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family:sans-serif;text-align:center;padding:80px;background:#f0fdf4;'>" +
                "<div style='max-width:480px;margin:auto;background:white;padding:48px;border-radius:16px;" +
                "box-shadow:0 4px 24px rgba(0,0,0,0.08);'>" +
                "<div style='font-size:56px;margin-bottom:16px;'>✅</div>" +
                "<h2 style='color:#16a34a;margin-bottom:8px;'>Connexion reussie !</h2>" +
                "<p style='color:#4b5563;'>Bienvenue <strong>" + escapeHtml(prenom) + "</strong>.</p>" +
                "<p style='color:#9ca3af;font-size:13px;margin-top:24px;'>Vous pouvez fermer cet onglet et retourner a Humania.</p>" +
                "</div></body></html>";
    }

    private String buildDeniedPage(String email, String reason) {
        return "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family:sans-serif;text-align:center;padding:80px;background:#fef2f2;'>" +
                "<div style='max-width:480px;margin:auto;background:white;padding:48px;border-radius:16px;" +
                "box-shadow:0 4px 24px rgba(0,0,0,0.08);'>" +
                "<div style='font-size:56px;margin-bottom:16px;'>⛔</div>" +
                "<h2 style='color:#dc2626;margin-bottom:8px;'>Acces refuse</h2>" +
                "<p style='color:#4b5563;'>L'adresse <strong>" + escapeHtml(email) + "</strong><br>" +
                "n'est pas autorisee a acceder a Humania.</p>" +
                "<p style='color:#6b7280;font-size:13px;margin-top:16px;'>" + escapeHtml(reason) + "</p>" +
                "<p style='color:#9ca3af;font-size:12px;margin-top:24px;'>Contactez votre administrateur pour obtenir un acces.</p>" +
                "</div></body></html>";
    }

    // ── Token exchange ────────────────────────────────────────────────────────

    private String exchangeCodeForToken(String code, String clientId, String clientSecret)
            throws Exception {
        String body = "code="           + URLEncoder.encode(code,         StandardCharsets.UTF_8)
                + "&client_id="     + URLEncoder.encode(clientId,     StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        HttpClient  client  = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400)
            throw new Exception("Echec echange code Google: " + response.body());

        return extractJsonValue(response.body(), "access_token");
    }

    // ── Fetch user info ───────────────────────────────────────────────────────

    private GoogleUser fetchUserInfo(String accessToken) throws Exception {
        HttpClient  client  = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400)
            throw new Exception("Impossible de recuperer les infos Google: " + response.body());

        String json       = response.body();
        String email      = extractJsonValue(json, "email");
        String givenName  = extractJsonValue(json, "given_name");
        String familyName = extractJsonValue(json, "family_name");
        String name       = extractJsonValue(json, "name");
        String googleId   = extractJsonValue(json, "sub");

        String prenom = (givenName  != null && !givenName.isBlank())  ? givenName  : name;
        String nom    = (familyName != null && !familyName.isBlank()) ? familyName : "";

        return new GoogleUser(email, nom, prenom, googleId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}