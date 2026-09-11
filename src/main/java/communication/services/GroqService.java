package communication.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GroqService {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    private final HttpClient client = HttpClient.newHttpClient();

    // ── Améliorer un texte ─────────────────────────────────────
    public String improveText(String text) {
        String prompt = "Tu es un assistant RH professionnel. " +
                "Améliore et reformule ce texte pour une publication sur un réseau social d'entreprise. " +
                "Garde le même sens, rends-le plus professionnel et engageant. " +
                "Réponds UNIQUEMENT avec le texte amélioré, sans explication.\n\nTexte : " + text;
        return callGroq(prompt);
    }

    // ── Résumer une publication ────────────────────────────────
    public String summarize(String text) {
        String prompt = "Résume ce texte en 2-3 phrases claires et concises en français. " +
                "Réponds UNIQUEMENT avec le résumé, sans introduction ni explication.\n\nTexte : " + text;
        return callGroq(prompt);
    }
    public String generatePost(String subject, String tone) {
        String prompt = "Tu es un expert en communication RH. " +
                "Rédige un post " + tone + " pour un réseau social d'entreprise sur le sujet suivant : " +
                subject + ". " +
                "Le post doit être engageant, entre 3 et 6 phrases, avec des emojis appropriés. " +
                "Réponds UNIQUEMENT avec le texte du post, sans titre ni explication.";
        return callGroq(prompt);
    }

    // ── Appel API ──────────────────────────────────────────────
    private String callGroq(String prompt) {
        try {
            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":"
                    + "\"" + escapeJson(prompt) + "\"}],"
                    + "\"max_tokens\":500,"
                    + "\"temperature\":0.7"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // ✅ LOG ici — dans le try
            System.out.println("🤖 Groq raw response: " + response.body());

            return parseGroqResponse(response.body());

        } catch (Exception e) {
            System.err.println("❌ Groq error: " + e.getMessage());
            return null;
        }
        // ❌ SUPPRIME les lignes dupliquées ici
    }

    // ── Parser la réponse ──────────────────────────────────────
    private String parseGroqResponse(String json) {
        try {
            // Cherche "content":"
            String key = "\"content\":\"";
            int idx = json.indexOf(key);
            if (idx < 0) return null;
            idx += key.length();

            // Lit jusqu'au prochain " non échappé
            StringBuilder sb = new StringBuilder();
            while (idx < json.length()) {
                char c = json.charAt(idx);
                char prev = idx > 0 ? json.charAt(idx - 1) : 0;
                if (c == '"' && prev != '\\') break;
                sb.append(c);
                idx++;
            }

            return sb.toString()
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")   // ✅ retire les guillemets échappés
                    .replace("\\'", "'")
                    .replace("\\u00e9", "é") // ✅ fix encoding français
                    .replace("\\u00e8", "è")
                    .replace("\\u00ea", "ê")
                    .replace("\\u00e0", "à")
                    .replace("\\u00f4", "ô")
                    .replace("\\u00fb", "û")
                    .replace("\\u00e7", "ç")
                    .replace("\\u00ee", "î")
                    .trim();

        } catch (Exception e) {
            System.err.println("❌ Parse error: " + e.getMessage());
            return null;
        }
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}