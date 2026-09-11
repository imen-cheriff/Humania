package communication.services;



import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GiphyService {

    private static final String API_KEY = "ssxr2LTh5SjyJL4AEtnqTvpYRycFcU9q";
    private static final String BASE_URL = "https://api.giphy.com/v1/gifs";
    private final HttpClient client = HttpClient.newHttpClient();

    // Résultat d'un GIF
    public static class GifResult {
        public final String id;
        public final String previewUrl;  // petit pour affichage grille
        public final String originalUrl; // pour sauvegarder
        public final String title;

        public GifResult(String id, String previewUrl, String originalUrl, String title) {
            this.id = id;
            this.previewUrl = previewUrl;
            this.originalUrl = originalUrl;
            this.title = title;
        }
    }

    // ── Recherche ──────────────────────────────────────────────
    public List<GifResult> search(String query, int limit) {
        try {
            String url = BASE_URL + "/search?api_key=" + API_KEY
                    + "&q=" + java.net.URLEncoder.encode(query, "UTF-8")
                    + "&limit=" + limit
                    + "&rating=g";

            return fetchGifs(url);
        } catch (Exception e) {
            System.err.println("❌ Giphy search error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Trending (page d'accueil du picker) ───────────────────
    public List<GifResult> trending(int limit) {
        try {
            String url = BASE_URL + "/trending?api_key=" + API_KEY
                    + "&limit=" + limit
                    + "&rating=g";
            return fetchGifs(url);
        } catch (Exception e) {
            System.err.println("❌ Giphy trending error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Parser la réponse JSON ─────────────────────────────────
    private List<GifResult> fetchGifs(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        return parseGiphyResponse(response.body());
    }

    private List<GifResult> parseGiphyResponse(String json) {
        List<GifResult> results = new ArrayList<>();
        try {
            // Parse manuel sans librairie externe
            String[] items = json.split("\"type\":\"gif\"");

            for (int i = 1; i < items.length; i++) {
                String item = items[i];

                String id = extractValue(item, "\"id\":\"", "\"");
                String title = extractValue(item, "\"title\":\"", "\"");

                // Preview (fixed_width_small) pour la grille
                String previewUrl = extractValue(item,
                        "\"fixed_width_small\":{\"height\":\"", null);
                // On cherche le url dans fixed_width_small
                int fixedIdx = item.indexOf("\"fixed_width\":{");
                String fixedSection = fixedIdx >= 0 ? item.substring(fixedIdx, fixedIdx + 300) : "";
                String displayUrl = extractValue(fixedSection, "\"url\":\"", "\"");
                displayUrl = displayUrl.replace("\\u0026", "&");

                // Original URL
                int origIdx = item.indexOf("\"original\":{");
                String origSection = origIdx >= 0 ? item.substring(origIdx, origIdx + 300) : "";
                String originalUrl = extractValue(origSection, "\"url\":\"", "\"");
                originalUrl = originalUrl.replace("\\u0026", "&");

                if (id != null && displayUrl != null && !displayUrl.isEmpty()) {
                    results.add(new GifResult(id, displayUrl, originalUrl, title != null ? title : ""));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Parse error: " + e.getMessage());
        }
        return results;
    }

    private String extractValue(String text, String startKey, String endKey) {
        if (text == null) return null;
        int start = text.indexOf(startKey);
        if (start < 0) return null;
        start += startKey.length();
        if (endKey == null) return text.substring(start, Math.min(start + 200, text.length()));
        int end = text.indexOf(endKey, start);
        if (end < 0) return null;
        return text.substring(start, end);
    }
}
