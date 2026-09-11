package recrutement.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║         SignNowService — Signature électronique SignNow API v2       ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  Pas de SDK — utilise uniquement java.net.http (Java 11+)           ║
 * ║                                                                      ║
 * ║  DÉPENDANCE JSON — ajoute dans pom.xml :                            ║
 * ║  <dependency>                                                        ║
 * ║    <groupId>org.json</groupId>                                       ║
 * ║    <artifactId>json</artifactId>                                     ║
 * ║    <version>20240303</version>                                       ║
 * ║  </dependency>                                                       ║
 * ║                                                                      ║
 * ║  CONFIGURATION — remplis EMAIL + PASSWORD ci-dessous :              ║
 * ║  Ce sont les identifiants de ton compte SignNow (sandbox).          ║
 * ║  La clé API est déjà remplie.                                       ║
 * ║                                                                      ║
 * ║  FLOW SignNow :                                                      ║
 * ║  1. POST /oauth2/token (Basic auth avec API key)→ access_token      ║
 * ║  2. POST /document (multipart) → upload PDF → document_id           ║
 * ║  3. POST /document/{id}/invite → envoyer invite de signature        ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public class SignNowService {

    // ── ⚙ CONFIGURATION ──────────────────────────────────────────────────
    // Mode démonstration : simule la signature sans appeler l'API distante.
    // true = pas d'erreur, pas d'email réel. false = vraie API SignNow (nécessite identifiants valides).
    private static final boolean DEMO_MODE = true;

    // Clé API SignNow (Basic auth pour obtenir le token)
    private static final String API_KEY      = "14eae37608e316b1d0269e8f1ee8f5d8f6e9b99965eca60f8214cd7413663fa3";

    // Identifiants de ton compte SignNow (pour l'OAuth2 password grant)
    private static final String ACCOUNT_EMAIL    = "amine.bouassida003@gmail.com";   // ← à remplir
    private static final String ACCOUNT_PASSWORD = "bouassida003";        // ← à remplir
    // ─────────────────────────────────────────────────────────────────────

    // Sandbox SignNow. En production : "https://api.signnow.com"
    private static final String BASE_URL = "https://api-eval.signnow.com";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // ── Cache du token (valide 30 jours) ─────────────────────────────────
    private String cachedToken     = null;
    private long   tokenExpireTime = 0;

    // ═════════════════════════════════════════════════════════════════════
    //  ÉTAPE 1 — Obtenir le token OAuth2 (password grant)
    // ═════════════════════════════════════════════════════════════════════
    private String getAccessToken() throws Exception {

        // Retourner le token en cache s'il est encore valide
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedToken;
        }

        // Basic auth = Base64(API_KEY:API_KEY) — SignNow exige client_id:client_secret
        // où client_id == client_secret == ta clé API
        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString((API_KEY + ":" + API_KEY).getBytes(StandardCharsets.UTF_8));

        // Corps de la requête OAuth2 password grant
        String body = "grant_type=password"
                + "&username=" + urlEncode(ACCOUNT_EMAIL)
                + "&password=" + urlEncode(ACCOUNT_PASSWORD)
                + "&scope=*";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/oauth2/token"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String msg = parseError(response.body(), response.statusCode());
            if (response.statusCode() == 401) {
                throw new Exception(
                        "❌ Authentification SignNow échouée (401).\n\n" +
                                "Vérifie dans SignNowService.java :\n" +
                                "  → ACCOUNT_EMAIL    = \"" + ACCOUNT_EMAIL + "\"\n" +
                                "  → ACCOUNT_PASSWORD = ton mot de passe SignNow\n\n" +
                                "Détail : " + msg
                );
            }
            throw new Exception("❌ SignNow OAuth2 error: " + msg);
        }

        JSONObject json = new JSONObject(response.body());
        cachedToken     = json.getString("access_token");
        // expires_in est en secondes (généralement 2592000 = 30 jours)
        long expiresIn  = json.optLong("expires_in", 2592000L);
        tokenExpireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

        return cachedToken;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ÉTAPE 2 — Uploader le document PDF
    // ═════════════════════════════════════════════════════════════════════
    private String uploadDocument(String token, File pdfFile) throws Exception {
        String boundary = "----SignNowBoundary" + UUID.randomUUID().toString().replace("-", "");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Part : file (binary PDF)
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                + pdfFile.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: application/pdf\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(Files.readAllBytes(pdfFile.toPath()));
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/document"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new Exception("❌ Upload document échoué : "
                    + parseError(response.body(), response.statusCode()));
        }

        JSONObject json = new JSONObject(response.body());
        return json.getString("id");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ÉTAPE 3 — Envoyer l'invitation de signature (freeform invite)
    // ═════════════════════════════════════════════════════════════════════
    private String sendInvite(String token, String documentId,
                              String signerEmail, String signerName,
                              String senderEmail) throws Exception {

        // Freeform invite — le signataire peut signer n'importe où
        JSONObject to = new JSONObject();
        to.put("email",   signerEmail);
        to.put("role",    "Signer");
        to.put("order",   1);
        to.put("reassign", "0");
        to.put("decline_by_signature", "0");
        to.put("reminder",  0);
        to.put("expiration_days", 15);
        to.put("subject",  "Document à signer : veuillez apposer votre signature");
        to.put("message",  "Bonjour " + signerName + ",\n\nVeuillez signer le document joint.");

        JSONArray toArray = new JSONArray();
        toArray.put(to);

        JSONObject body = new JSONObject();
        body.put("to",   toArray);
        body.put("from", senderEmail);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/document/" + documentId + "/invite"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new Exception("❌ Envoi invitation échoué : "
                    + parseError(response.body(), response.statusCode()));
        }

        // Retourner l'URL de signature si disponible dans la réponse
        try {
            JSONObject json = new JSONObject(response.body());
            if (json.has("link")) return json.getString("link");
        } catch (Exception ignored) {}

        return ""; // L'email est envoyé directement par SignNow
    }

    // ═════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE — Envoyer un PDF pour signature
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Envoie un document PDF pour signature via SignNow.
     * Le signataire reçoit un email SignNow avec son lien de signature.
     *
     * @param pdfFile     Fichier PDF à signer
     * @param signerName  Nom complet du signataire
     * @param signerEmail Email du signataire
     * @return SignatureResult contenant l'ID du document et le statut
     */
    public SignatureResult sendForSignature(File pdfFile,
                                            String signerName,
                                            String signerEmail) throws Exception {
        if (!pdfFile.exists())
            throw new Exception("❌ Fichier introuvable : " + pdfFile.getAbsolutePath());

        if (DEMO_MODE) {
            // Simulation locale : faux id + lien vers le site SignNow (évite DNS_PROBE_FINISHED_NXDOMAIN)
            String fakeId   = "demo-" + UUID.randomUUID();
            String fakeLink = "https://www.signnow.com";
            return new SignatureResult(fakeId, fakeLink, signerEmail);
        }

        // Mode réel : utiliser l'API SignNow
        String token      = getAccessToken();
        String documentId = uploadDocument(token, pdfFile);
        String signingLink = sendInvite(token, documentId, signerEmail, signerName, ACCOUNT_EMAIL);
        return new SignatureResult(documentId, signingLink, signerEmail);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Vérifier le statut d'un document
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Vérifie si un document a été signé.
     *
     * @param documentId ID retourné par sendForSignature()
     * @return "completed" si signé, "pending" si en attente
     */
    public String getStatus(String documentId) throws Exception {
        // En mode démo, considérer comme signé (pas d'appel API)
        if (documentId != null && documentId.startsWith("demo-")) {
            return "completed";
        }

        String token = getAccessToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/document/" + documentId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("❌ Récupération statut échouée : "
                    + parseError(response.body(), response.statusCode()));
        }

        JSONObject json = new JSONObject(response.body());
        // SignNow indique le statut via les champs de signature
        if (json.has("signatures") && json.getJSONArray("signatures").length() > 0) {
            return "completed";
        }
        return "pending";
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Utilitaires
    // ═════════════════════════════════════════════════════════════════════

    private String parseError(String body, int status) {
        try {
            JSONObject json = new JSONObject(body);
            if (json.has("errors")) {
                JSONArray errors = json.getJSONArray("errors");
                if (errors.length() > 0) {
                    return "HTTP " + status + " — " + errors.getJSONObject(0).optString("message", body);
                }
            }
            if (json.has("error"))   return "HTTP " + status + " — " + json.getString("error");
            if (json.has("message")) return "HTTP " + status + " — " + json.getString("message");
        } catch (Exception ignored) {}
        return "HTTP " + status + " — " + body;
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Classe résultat
    // ═════════════════════════════════════════════════════════════════════
    public static class SignatureResult {
        public final String documentId;
        public final String signingLink;  // lien direct si disponible
        public final String signerEmail;

        public SignatureResult(String documentId, String signingLink, String signerEmail) {
            this.documentId  = documentId;
            this.signingLink = signingLink;
            this.signerEmail = signerEmail;
        }
    }
}