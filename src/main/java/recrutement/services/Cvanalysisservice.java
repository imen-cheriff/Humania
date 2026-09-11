package recrutement.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service réutilisable pour l'analyse de CV via le webhook n8n.
 * Extrait de CvAnalyzerApp et adapté pour l'intégration dans Humania.
 */
public class Cvanalysisservice {

    private static final String WEBHOOK_URL =
            "https://bouaaa.app.n8n.cloud/webhook-test/cv-upload";

    // ── Résultat d'une analyse ─────────────────────────────────────────────
    public static class AnalysisResult {
        public final double vote;
        public final String consideration;
        public final double[] axisScores; // [Technical, Experience, Match, Location, Growth]

        public AnalysisResult(double vote, String consideration, double[] axisScores) {
            this.vote          = vote;
            this.consideration = consideration;
            this.axisScores    = axisScores;
        }
    }

    /**
     * Envoie le fichier CV au webhook n8n et retourne le résultat parsé.
     * Appeler dans un thread background (CompletableFuture).
     */
    public AnalysisResult analyze(File cvFile) throws Exception {
        String json = postFormData(cvFile);
        double vote           = parseVote(json);
        String consideration  = parseConsideration(json);
        double[] axisScores   = deriveAxisScores(consideration, vote);
        return new AnalysisResult(vote, consideration, axisScores);
    }

    // ── HTTP multipart POST ────────────────────────────────────────────────
    private String postFormData(File file) throws Exception {
        String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");
        String CRLF = "\r\n";

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + CRLF).getBytes());
        body.write(("Content-Disposition: form-data; name=\"cv\"; filename=\""
                + file.getName() + "\"" + CRLF).getBytes());
        body.write(("Content-Type: application/pdf" + CRLF + CRLF).getBytes());
        body.write(Files.readAllBytes(file.toPath()));
        body.write((CRLF + "--" + boundary + "--" + CRLF).getBytes());
        byte[] bodyBytes = body.toByteArray();

        HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(120_000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream()) { os.write(bodyBytes); }

        int status = conn.getResponseCode();
        InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        if (status >= 400)
            throw new IOException("HTTP " + status + ": " + sb.toString().trim());
        return sb.toString().trim();
    }

    // ── Parseurs JSON ──────────────────────────────────────────────────────
    private double parseVote(String json) {
        Matcher m = Pattern.compile("\"vote\"\\s*:\\s*\"?(\\d+(?:\\.\\d+)?)\"?").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 5;
    }

    private String parseConsideration(String json) {
        Matcher m = Pattern.compile("\"consideration\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\n", "\n").replace("\\\"", "\"")
                : "Aucune évaluation disponible.";
    }

    // ── Dérivation des scores par axe ──────────────────────────────────────
    public double[] deriveAxisScores(String text, double vote) {
        String lo = text.toLowerCase();
        double[] s = new double[5];

        // Technical Skills
        s[0] = contains(lo, "python","java","javascript","php","technical","stack","framework")
                ? (contains(lo,"doesn't","lacks","missing","not mentioned") ? vote*0.7 : vote*1.1)
                : vote;

        // Experience
        s[1] = contains(lo, "experience","years","junior","senior","background")
                ? (contains(lo,"lacking","insufficient","limited","doesn't fully") ? vote*0.75 : vote*1.05)
                : vote;

        // Requirements Match
        s[2] = contains(lo, "requirement","profile","match","meet","fit")
                ? (contains(lo,"doesn't fully","not fully","partially","some") ? vote*0.8 : vote*1.1)
                : vote;

        // Location Fit
        s[3] = contains(lo, "location","tunis","remote","local","relocation","plus")
                ? (contains(lo,"plus","advantage","great","perfect") ? Math.min(10, vote*1.3) : vote*0.8)
                : vote;

        // Growth Potential
        s[4] = contains(lo, "potential","learn","grow","promising","eager","motivated")
                ? (contains(lo,"potential") ? vote*1.1 : vote*0.9)
                : vote;

        for (int i = 0; i < s.length; i++)
            s[i] = Math.max(1, Math.min(10, s[i]));
        return s;
    }

    private boolean contains(String text, String... keywords) {
        for (String k : keywords) if (text.contains(k)) return true;
        return false;
    }
}