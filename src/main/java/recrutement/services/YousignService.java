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
 * ║           YousignService — Signature électronique Yousign API v3    ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  Pas de SDK Java — utilise uniquement java.net.http (Java 11+)      ║
 * ║                                                                      ║
 * ║  DÉPENDANCE JSON — ajoute dans pom.xml :                            ║
 * ║  <dependency>                                                        ║
 * ║    <groupId>org.json</groupId>                                       ║
 * ║    <artifactId>json</artifactId>                                     ║
 * ║    <version>20240303</version>                                       ║
 * ║  </dependency>                                                       ║
 * ║                                                                      ║
 * ║  CONFIGURATION :                                                     ║
 * ║  1. Connecte-toi sur https://sandbox.yousign.com                    ║
 * ║  2. Va dans Paramètres → API → Générer une clé API                  ║
 * ║  3. Colle la clé dans API_KEY ci-dessous                            ║
 * ║                                                                      ║
 * ║  FLOW YOUSIGN V3 :                                                   ║
 * ║  1. POST /signature_requests        → créer la demande (draft)      ║
 * ║  2. POST /signature_requests/{id}/documents → uploader le PDF       ║
 * ║  3. POST /signature_requests/{id}/signers   → ajouter le signataire ║
 * ║  4. POST /signature_requests/{id}/activate → envoyer                ║
 * ║     → Yousign envoie un email avec le lien de signature             ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public class YousignService {

    // ── ⚙ À REMPLIR ──────────────────────────────────────────────────────
    private static final String API_KEY = "UVEF6eSMTmUqAznyQUSj0K2wrk7HQ7M4";
    // ─────────────────────────────────────────────────────────────────────

    // Sandbox (tests gratuits). En production : "https://api.yousign.app/v3"
    private static final String BASE_URL = "https://api-sandbox.yousign.app/v3";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // ═════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE — Envoyer un PDF pour signature par email
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Envoie un document PDF pour signature via Yousign.
     * Le signataire reçoit un email avec son lien de signature unique.
     *
     * @param pdfFile     Fichier PDF à signer (doit exister sur le disque)
     * @param firstName   Prénom du signataire
     * @param lastName    Nom du signataire
     * @param email       Email du signataire
     * @return SignatureResult contenant l'ID et le lien de signature
     */
    public SignatureResult sendForSignature(File pdfFile, String firstName,
                                            String lastName, String email)
            throws Exception {

        if (!pdfFile.exists())
            throw new Exception("❌ Fichier introuvable : " + pdfFile.getAbsolutePath());

        // ── Étape 1 : Créer la Signature Request (statut draft) ───────────
        String signatureRequestId = createSignatureRequest(pdfFile.getName());

        // ── Étape 2 : Uploader le document PDF ────────────────────────────
        String documentId = uploadDocument(signatureRequestId, pdfFile);

        // ── Étape 3 : Ajouter le signataire + champ de signature ──────────
        String signerId = addSigner(signatureRequestId, documentId,
                firstName, lastName, email);

        // ── Étape 4 : Activer la Signature Request ────────────────────────
        //    → Yousign envoie l'email au signataire avec son lien unique
        String signingUrl = activateAndGetSigningLink(signatureRequestId, signerId);

        return new SignatureResult(signatureRequestId, documentId, signerId, signingUrl);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ÉTAPE 1 — Créer la Signature Request
    // ═════════════════════════════════════════════════════════════════════
    private String createSignatureRequest(String docName) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", "Signature - " + docName);
        body.put("delivery_mode", "email");      // Yousign envoie l'email
        body.put("ordered_signers", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signature_requests"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        JSONObject response = sendAndParse(request, "Créer Signature Request");
        return response.getString("id");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ÉTAPE 2 — Uploader le document (multipart/form-data)
    // ═════════════════════════════════════════════════════════════════════
    private String uploadDocument(String signatureRequestId, File pdfFile) throws Exception {
        String boundary = "----YousignBoundary" + UUID.randomUUID().toString().replace("-", "");

        // Construire le corps multipart manuellement
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // -- Part 1: nature
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Disposition: form-data; name=\"nature\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write("signable_document\r\n".getBytes(StandardCharsets.UTF_8));

        // -- Part 2: file (binary PDF)
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                + pdfFile.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: application/pdf\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(Files.readAllBytes(pdfFile.toPath()));
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));

        // -- Closing boundary
        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signature_requests/" + signatureRequestId + "/documents"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        JSONObject response = sendAndParse(request, "Upload document");
        return response.getString("id");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ÉTAPE 3 — Ajouter le signataire avec champ de signature
    // ═════════════════════════════════════════════════════════════════════
    private String addSigner(String signatureRequestId, String documentId,
                             String firstName, String lastName, String email)
            throws Exception {

        // Définir le champ de signature (bas de page 1, format A4)
        JSONObject signatureField = new JSONObject();
        signatureField.put("type", "signature");
        signatureField.put("document_id", documentId);
        signatureField.put("page", 1);
        signatureField.put("x", 200);   // coordonnée X (px depuis la gauche)
        signatureField.put("y", 680);   // coordonnée Y (px depuis le haut)
        signatureField.put("width", 200);
        signatureField.put("height", 50);

        JSONArray fields = new JSONArray();
        fields.put(signatureField);

        // Informations du signataire
        JSONObject info = new JSONObject();
        info.put("first_name", firstName);
        info.put("last_name", lastName);
        info.put("email", email);
        info.put("locale", "fr");

        JSONObject body = new JSONObject();
        body.put("info", info);
        body.put("signature_level", "electronic_signature");
        body.put("signature_authentication_mode", "no_otp"); // sans OTP
        body.put("fields", fields);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signature_requests/" + signatureRequestId + "/signers"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        JSONObject response = sendAndParse(request, "Ajouter signataire");
        return response.getString("id");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ÉTAPE 4 — Activer la Signature Request
    // ═════════════════════════════════════════════════════════════════════
    private String activateAndGetSigningLink(String signatureRequestId, String signerId)
            throws Exception {

        // Activer → Yousign envoie l'email au signataire
        HttpRequest activateRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signature_requests/" + signatureRequestId + "/activate"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        JSONObject activateResponse = sendAndParse(activateRequest, "Activer Signature Request");

        // Récupérer le lien de signature depuis la réponse d'activation
        // Les signers sont dans activateResponse.signers[]
        try {
            JSONArray signers = activateResponse.getJSONArray("signers");
            for (int i = 0; i < signers.length(); i++) {
                JSONObject s = signers.getJSONObject(i);
                if (s.getString("id").equals(signerId)) {
                    if (s.has("signature_link") && !s.isNull("signature_link")) {
                        return s.getString("signature_link");
                    }
                }
            }
        } catch (Exception ignored) {}

        // Si le lien n'est pas encore disponible, le récupérer séparément
        return getSigningLink(signatureRequestId, signerId);
    }

    /**
     * Récupère le lien de signature individuel du signataire.
     */
    private String getSigningLink(String signatureRequestId, String signerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signature_requests/" + signatureRequestId
                        + "/signers/" + signerId + "/signature_link"))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

        JSONObject response = sendAndParse(request, "Obtenir lien de signature");

        if (response.has("signature_link") && !response.isNull("signature_link")) {
            return response.getString("signature_link");
        }

        // Fallback : retourner le lien vers la plateforme Yousign directement
        return "https://sandbox.yousign.com/procedure/sign/" + signatureRequestId;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Vérifier le statut d'une Signature Request
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Vérifie si un document a été signé.
     * Statuts possibles : draft, ongoing, done, expired, canceled, rejected
     *
     * @param signatureRequestId ID retourné par sendForSignature()
     * @return "done" si signé, "ongoing" si en attente, etc.
     */
    public String getStatus(String signatureRequestId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/signature_requests/" + signatureRequestId))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

        JSONObject response = sendAndParse(request, "Vérifier statut");
        return response.getString("status");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Utilitaire HTTP
    // ═════════════════════════════════════════════════════════════════════
    private JSONObject sendAndParse(HttpRequest request, String stepName) throws Exception {
        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            String errorMsg = "❌ Yousign API — " + stepName + " failed\n"
                    + "HTTP " + status + "\n"
                    + body;

            // Décoder le message d'erreur Yousign si disponible
            try {
                JSONObject err = new JSONObject(body);
                if (err.has("message")) errorMsg += "\nMessage : " + err.getString("message");
                if (err.has("detail"))  errorMsg += "\nDétail : " + err.getString("detail");
            } catch (Exception ignored) {}

            throw new Exception(errorMsg);
        }

        return new JSONObject(body);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Classe résultat
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Résultat retourné après l'envoi d'une demande de signature.
     */
    public static class SignatureResult {
        public final String signatureRequestId;
        public final String documentId;
        public final String signerId;
        public final String signingUrl;   // URL directe de signature (ouvrir dans navigateur)

        public SignatureResult(String signatureRequestId, String documentId,
                               String signerId, String signingUrl) {
            this.signatureRequestId = signatureRequestId;
            this.documentId         = documentId;
            this.signerId           = signerId;
            this.signingUrl         = signingUrl;
        }

        @Override
        public String toString() {
            return "SignatureResult{" +
                    "signatureRequestId='" + signatureRequestId + '\'' +
                    ", signingUrl='" + signingUrl + '\'' +
                    '}';
        }
    }
}