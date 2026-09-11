package recrutement.services;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║            DocuSignService — Signature électronique              ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  DÉPENDANCE MAVEN — ajoute dans pom.xml :                       ║
 * ║  <dependency>                                                    ║
 * ║    <groupId>com.docusign</groupId>                               ║
 * ║    <artifactId>docusign-esign-java</artifactId>                  ║
 * ║    <version>4.4.0</version>                                      ║
 * ║  </dependency>                                                   ║
 * ║                                                                  ║
 * ║  FICHIER CLÉ RSA — place ta clé privée ici :                    ║
 * ║  src/main/resources/docusign_private.pem                        ║
 * ║  (générée dans DocuSign Admin > Apps & Keys > RSA Keypairs)     ║
 * ║                                                                  ║
 * ║  CONSENTEMENT (1 seule fois) — ouvre dans un navigateur :       ║
 * ║  https://account-d.docusign.com/oauth/auth                      ║
 * ║    ?response_type=code                                           ║
 * ║    &scope=signature%20impersonation                              ║
 * ║    &client_id=UVEF6eSMTmUqAznyQUSj0K2wrk7HQ7M4                  ║
 * ║    &redirect_uri=https://httpbin.org/get                         ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class DocuSignService {

    // ── ✅ Credentials — tous remplis ────────────────────────────────────
    private static final String INTEGRATION_KEY  = "UVEF6eSMTmUqAznyQUSj0K2wrk7HQ7M4";
    private static final String USER_GUID        = "9f427280-4888-4b61-9091-0295a0c43608";
    private static final String ACCOUNT_ID       = "f16f7dfc-0954-4e54-9091-46d238548929";
    private static final String BASE_PATH        = "https://na4.docusign.net/restapi";
    private static final String OAUTH_BASE_PATH  = "account.docusign.com"; // production OAuth
    private static final String PRIVATE_KEY_PATH = "src/main/resources/docusign_private.pem";
    // ─────────────────────────────────────────────────────────────────────

    private static final long   TOKEN_EXPIRY_SEC = 3600L;
    private static final String RETURN_URL       = "https://httpbin.org/get";

    private static final List<String> SCOPES = Arrays.asList(
            OAuth.Scope_SIGNATURE,
            OAuth.Scope_IMPERSONATION
    );

    // ── Interne : client auth ─────────────────────────────────────────────
    private static class AuthResult {
        final ApiClient apiClient;
        final String    accountId;
        AuthResult(ApiClient c, String id) { this.apiClient = c; this.accountId = id; }
    }

    /**
     * Construit un ApiClient authentifié via JWT Grant.
     */
    private AuthResult buildAuthenticatedClient() throws Exception {

        // 1. Créer le client avec OAuth base path
        ApiClient apiClient = new ApiClient();
        apiClient.setOAuthBasePath(OAUTH_BASE_PATH);

        // 2. Lire la clé RSA privée
        byte[] privateKeyBytes;
        try {
            privateKeyBytes = Files.readAllBytes(new File(PRIVATE_KEY_PATH).toPath());
        } catch (IOException e) {
            throw new Exception(
                    "❌ Clé RSA introuvable : " + PRIVATE_KEY_PATH + "\n" +
                            "→ Génère une paire RSA dans DocuSign Admin > Apps & Keys > RSA Keypairs\n" +
                            "→ Sauvegarde la clé privée dans : " + PRIVATE_KEY_PATH, e
            );
        }

        // 3. Obtenir le token JWT
        OAuth.OAuthToken oAuthToken;
        try {
            oAuthToken = apiClient.requestJWTUserToken(
                    INTEGRATION_KEY,
                    USER_GUID,
                    SCOPES,
                    privateKeyBytes,
                    TOKEN_EXPIRY_SEC
            );
        } catch (ApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("consent_required")) {
                throw new Exception(
                        "❌ Consentement DocuSign requis.\n\n" +
                                "Ouvre ce lien dans ton navigateur et clique Accepter :\n\n" +
                                "https://account.docusign.com/oauth/auth" +
                                "?response_type=code" +
                                "&scope=signature%20impersonation" +
                                "&client_id=" + INTEGRATION_KEY +
                                "&redirect_uri=https://httpbin.org/get", e
                );
            }
            throw new Exception("❌ Erreur JWT DocuSign : " + e.getMessage(), e);
        }

        // 4. Appliquer le token au client
        apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());

        // 5. Utiliser le basePath connu (na4.docusign.net)
        apiClient.setBasePath(BASE_PATH);

        return new AuthResult(apiClient, ACCOUNT_ID);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Signature Embarquée — URL de signature à ouvrir dans le navigateur
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Envoie un document PDF pour signature embarquée.
     * Retourne une URL à ouvrir directement dans le navigateur.
     *
     * @param pdfFile     Fichier PDF à signer
     * @param signerName  Nom complet du signataire
     * @param signerEmail Email du signataire
     * @return URL DocuSign de signature
     */
    public String sendForEmbeddedSigning(File pdfFile, String signerName, String signerEmail)
            throws Exception {

        if (!pdfFile.exists())
            throw new Exception("❌ Fichier introuvable : " + pdfFile.getAbsolutePath());

        AuthResult   auth         = buildAuthenticatedClient();
        EnvelopesApi envelopesApi = new EnvelopesApi(auth.apiClient);

        // Document PDF encodé en base64
        String pdfBase64 = Base64.getEncoder().encodeToString(
                Files.readAllBytes(pdfFile.toPath()));

        Document document = new Document();
        document.setDocumentBase64(pdfBase64);
        document.setName(pdfFile.getName());
        document.setFileExtension("pdf");
        document.setDocumentId("1");

        // Zone de signature (bas de page 1)
        SignHere signHere = new SignHere();
        signHere.setDocumentId("1");
        signHere.setPageNumber("1");
        signHere.setRecipientId("1");
        signHere.setTabLabel("SignHereTab");
        signHere.setXPosition("100");
        signHere.setYPosition("680");

        Tabs tabs = new Tabs();
        tabs.setSignHereTabs(Arrays.asList(signHere));

        // Signataire — clientUserId obligatoire pour embedded signing
        Signer signer = new Signer();
        signer.setEmail(signerEmail);
        signer.setName(signerName);
        signer.setRecipientId("1");
        signer.setRoutingOrder("1");
        signer.setClientUserId("1001");
        signer.setTabs(tabs);

        Recipients recipients = new Recipients();
        recipients.setSigners(Arrays.asList(signer));

        EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
        envelopeDefinition.setEmailSubject("Document à signer : " + pdfFile.getName());
        envelopeDefinition.setDocuments(Arrays.asList(document));
        envelopeDefinition.setRecipients(recipients);
        envelopeDefinition.setStatus("sent");

        // Créer l'enveloppe
        EnvelopeSummary summary = envelopesApi.createEnvelope(auth.accountId, envelopeDefinition);
        String envelopeId = summary.getEnvelopeId();

        // Obtenir l'URL de signature embarquée
        RecipientViewRequest viewRequest = new RecipientViewRequest();
        viewRequest.setReturnUrl(RETURN_URL);
        viewRequest.setAuthenticationMethod("none");
        viewRequest.setEmail(signerEmail);
        viewRequest.setUserName(signerName);
        viewRequest.setClientUserId("1001");
        viewRequest.setRecipientId("1");

        ViewUrl viewUrl = envelopesApi.createRecipientView(
                auth.accountId, envelopeId, viewRequest);

        return viewUrl.getUrl();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Signature Distante — email envoyé au signataire
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Envoie un email DocuSign au signataire pour qu'il signe à distance.
     *
     * @param pdfFile     Fichier PDF à signer
     * @param signerName  Nom du signataire
     * @param signerEmail Email du signataire
     * @return ID de l'enveloppe créée
     */
    public String sendForRemoteSigning(File pdfFile, String signerName, String signerEmail)
            throws Exception {

        if (!pdfFile.exists())
            throw new Exception("❌ Fichier introuvable : " + pdfFile.getAbsolutePath());

        AuthResult   auth         = buildAuthenticatedClient();
        EnvelopesApi envelopesApi = new EnvelopesApi(auth.apiClient);

        String pdfBase64 = Base64.getEncoder().encodeToString(
                Files.readAllBytes(pdfFile.toPath()));

        Document document = new Document();
        document.setDocumentBase64(pdfBase64);
        document.setName(pdfFile.getName());
        document.setFileExtension("pdf");
        document.setDocumentId("1");

        SignHere signHere = new SignHere();
        signHere.setDocumentId("1");
        signHere.setPageNumber("1");
        signHere.setRecipientId("1");
        signHere.setTabLabel("SignHereTab");
        signHere.setXPosition("100");
        signHere.setYPosition("680");

        Tabs tabs = new Tabs();
        tabs.setSignHereTabs(Arrays.asList(signHere));

        // Pas de clientUserId = signature distante par email
        Signer signer = new Signer();
        signer.setEmail(signerEmail);
        signer.setName(signerName);
        signer.setRecipientId("1");
        signer.setRoutingOrder("1");
        signer.setTabs(tabs);

        Recipients recipients = new Recipients();
        recipients.setSigners(Arrays.asList(signer));

        EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
        envelopeDefinition.setEmailSubject("Document à signer : " + pdfFile.getName());
        envelopeDefinition.setDocuments(Arrays.asList(document));
        envelopeDefinition.setRecipients(recipients);
        envelopeDefinition.setStatus("sent");

        EnvelopeSummary summary = envelopesApi.createEnvelope(auth.accountId, envelopeDefinition);
        return summary.getEnvelopeId();
    }
}