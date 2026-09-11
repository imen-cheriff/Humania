package planification.services;

import planification.models.Evenement;
import planification.models.ParticipEven;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * AIService — wraps all Groq API calls for the planification module.
 * Features: onboarding checklist, offboarding checklist,
 *           event recommender, description generator, smart search, chatbot.
 */
public class AIService {

    private static final String GROQ_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    // ⚠️  Replace with your new key after regenerating at openrouter.ai/keys
    private static final String API_KEY      = "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";
    private static final String MODEL        = "meta-llama/llama-3.1-8b-instruct:free";

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // =========================================================================
    // PUBLIC DATA CLASS — shared by both controllers
    // =========================================================================

    /**
     * A single AI-generated task.
     * Fields match the shape of TaskItem in OnboardingController / OffboardingController.
     */
    public static class AiTask {
        public final String icon;
        public final String label;
        public final String category;
        public final int    dayOffset;

        public AiTask(String icon, String label, String category, int dayOffset) {
            this.icon      = icon;
            this.label     = label;
            this.category  = category;
            this.dayOffset = dayOffset;
        }
    }

    // =========================================================================
    // 1.  ONBOARDING — AI Checklist Generator
    // =========================================================================

    /**
     * Generates extra personalised onboarding tasks for the given employee.
     *
     * @param employeeName employee full name
     * @param role         employee role / poste
     * @param department   employee department
     * @return list of AiTask (may be empty on error, never null)
     */
    public List<AiTask> generateOnboardingTasks(String employeeName,
                                                String role,
                                                String department) {
        String systemPrompt =
                "You are an HR onboarding specialist. " +
                        "You ONLY reply with a JSON array of task objects, nothing else — no explanation, no markdown fences. " +
                        "Each object has exactly these fields: " +
                        "\"icon\" (single emoji), \"label\" (task name in French, max 60 chars), " +
                        "\"category\" (one of: Administratif, IT, Formation, Intégration, Accès, RH), " +
                        "\"dayOffset\" (integer >= 0, number of days after the arrival date). " +
                        "Example: [{\"icon\":\"🔑\",\"label\":\"Accès serveur de production\",\"category\":\"IT\",\"dayOffset\":5}]";

        String userPrompt =
                "Generate 4 to 6 specific and realistic onboarding tasks in French for:\n" +
                        "- Name: " + employeeName + "\n" +
                        "- Role: " + role + "\n" +
                        "- Department: " + department + "\n\n" +
                        "Focus ONLY on tasks specific to this role and department. " +
                        "Do NOT include generic tasks like badge creation, email setup, or contract signing. " +
                        "Reply ONLY with the raw JSON array, no other text.";

        String raw = callGroq(userPrompt, systemPrompt);
        return parseAiTasks(raw);
    }

    // =========================================================================
    // 2.  OFFBOARDING — AI Checklist Generator
    // =========================================================================

    /**
     * Generates extra personalised offboarding tasks for the given employee.
     *
     * @param employeeName    employee full name
     * @param role            employee role / poste
     * @param department      employee department
     * @param departureReason reason for leaving (Démission, Retraite, etc.)
     * @return list of AiTask (may be empty on error, never null)
     */
    public List<AiTask> generateOffboardingTasks(String employeeName,
                                                 String role,
                                                 String department,
                                                 String departureReason) {
        String systemPrompt =
                "You are an HR offboarding specialist. " +
                        "You ONLY reply with a JSON array of task objects, nothing else — no explanation, no markdown fences. " +
                        "Each object has exactly these fields: " +
                        "\"icon\" (single emoji), \"label\" (task name in French, max 60 chars), " +
                        "\"category\" (one of: Administratif, IT, Opérationnel, Équipement, Accès, RH, Finance), " +
                        "\"dayOffset\" (integer >= 0, number of days BEFORE the departure date). " +
                        "Example: [{\"icon\":\"📁\",\"label\":\"Transfert des projets en cours\",\"category\":\"Opérationnel\",\"dayOffset\":10}]";

        String userPrompt =
                "Generate 4 to 6 specific and realistic offboarding tasks in French for:\n" +
                        "- Name: " + employeeName + "\n" +
                        "- Role: " + role + "\n" +
                        "- Department: " + department + "\n" +
                        "- Departure reason: " + departureReason + "\n\n" +
                        "Focus ONLY on tasks specific to this role, department, and departure reason. " +
                        "Do NOT repeat generic tasks like badge return, email deactivation, or exit interview. " +
                        "Reply ONLY with the raw JSON array, no other text.";

        String raw = callGroq(userPrompt, systemPrompt);
        return parseAiTasks(raw);
    }

    // =========================================================================
    // 3.  EVENT RECOMMENDER
    // =========================================================================

    public List<Integer> getRecommendedEventIds(int employeeId,
                                                List<Evenement> allEvents,
                                                List<ParticipEven> allParticipations) {
        List<Integer> pastEventIds = allParticipations.stream()
                .filter(p -> p.getIdEmploye() == employeeId)
                .map(ParticipEven::getIdEvenement)
                .collect(Collectors.toList());

        String prompt = buildRecommenderPrompt(employeeId, allEvents, pastEventIds);
        String response = callGroq(prompt,
                "You are an event recommendation assistant. " +
                        "Reply ONLY with comma-separated event IDs. Example: 3,7,12");
        return parseIdList(response);
    }

    private String buildRecommenderPrompt(int employeeId,
                                          List<Evenement> allEvents,
                                          List<Integer> pastEventIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("Employee ID: ").append(employeeId).append("\n\n=== ALL EVENTS ===\n");
        for (Evenement ev : allEvents) {
            sb.append("ID: ").append(ev.getId())
                    .append(" | Title: ").append(ev.getTitre() != null ? ev.getTitre() : "N/A")
                    .append(" | Location: ").append(ev.getLieu() != null ? ev.getLieu() : "N/A")
                    .append(" | Date: ").append(ev.getDateEvenement() != null ? DATE_FMT.format(ev.getDateEvenement()) : "N/A")
                    .append("\n");
        }
        sb.append("\n=== ALREADY JOINED ===\n");
        if (pastEventIds.isEmpty()) {
            sb.append("None.\n");
        } else {
            for (int id : pastEventIds) {
                allEvents.stream().filter(e -> e.getId() == id).findFirst()
                        .ifPresent(e -> sb.append("ID: ").append(id).append(" | ").append(e.getTitre()).append("\n"));
            }
        }
        sb.append("\nRecommend up to 3 events not yet joined. Reply ONLY with IDs e.g. 4,9,2");
        return sb.toString();
    }

    // =========================================================================
    // 4.  DESCRIPTION GENERATOR
    // =========================================================================

    public String generateEventDescription(String title, String location, String date) {
        String prompt = "Write a short, professional event description in French (3-4 sentences) for:\n"
                + "Title: " + title + "\nLocation: " + location + "\nDate: " + date + "\n"
                + "Be engaging. Plain text only.";
        return callGroq(prompt,
                "You are a professional French event copywriter. Write concise, engaging descriptions.");
    }

    // =========================================================================
    // 5.  SMART SEARCH
    // =========================================================================

    public List<Integer> smartSearch(String query, List<Evenement> allEvents) {
        StringBuilder sb = new StringBuilder();
        sb.append("User query: \"").append(query).append("\"\n\n=== EVENTS ===\n");
        for (Evenement ev : allEvents) {
            sb.append("ID: ").append(ev.getId())
                    .append(" | Title: ").append(ev.getTitre() != null ? ev.getTitre() : "N/A")
                    .append(" | Location: ").append(ev.getLieu() != null ? ev.getLieu() : "N/A")
                    .append(" | Date: ").append(ev.getDateEvenement() != null ? DATE_FMT.format(ev.getDateEvenement()) : "N/A")
                    .append("\n");
        }
        sb.append("\nReturn ONLY matching IDs as comma-separated numbers, or: none");
        String response = callGroq(sb.toString(),
                "You are a smart event search engine. Reply ONLY with comma-separated IDs or 'none'.");
        return parseIdList(response);
    }

    // =========================================================================
    // 6.  CHATBOT
    // =========================================================================

    public String chat(String userMessage, List<Evenement> allEvents) {
        StringBuilder context = new StringBuilder("=== EVENTS ===\n");
        for (Evenement ev : allEvents) {
            context.append("- ").append(ev.getTitre() != null ? ev.getTitre() : "N/A")
                    .append(" | ").append(ev.getLieu() != null ? ev.getLieu() : "N/A")
                    .append(" | ").append(ev.getDateEvenement() != null ? DATE_FMT.format(ev.getDateEvenement()) : "N/A")
                    .append("\n");
        }
        context.append("\nUser question: ").append(userMessage);
        return callGroq(context.toString(),
                "You are a helpful event assistant. Answer in French, be concise and friendly.");
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    private String callGroq(String userMessage, String systemPrompt) {
        try {
            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":" + jsonString(systemPrompt) + "},"
                    + "{\"role\":\"user\",\"content\":" + jsonString(userMessage) + "}"
                    + "],"
                    + "\"max_tokens\":512,"
                    + "\"temperature\":0.5"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("HTTP-Referer", "https://humania.app")
                    .header("X-Title", "Humania HR")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return extractContent(response.body());
        } catch (Exception e) {
            System.err.println("[AIService] Error calling Groq: " + e.getMessage());
            return "";
        }
    }

    /** Extracts the first "content" string value from Groq's JSON response. */
    private String extractContent(String json) {
        try {
            String marker = "\"content\":\"";
            int start = json.indexOf(marker);
            if (start == -1) return "";
            start += marker.length();
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"')  { sb.append('"');  i++; continue; }
                    if (next == 'n')  { sb.append('\n'); i++; continue; }
                    if (next == 't')  { sb.append('\t'); i++; continue; }
                    if (next == '\\') { sb.append('\\'); i++; continue; }
                }
                if (c == '"') break;
                sb.append(c);
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Parses a JSON array of task objects.
     * Shape: [{"icon":"...","label":"...","category":"...","dayOffset":N}, ...]
     * Pure string parsing — no extra JSON library needed.
     */
    private List<AiTask> parseAiTasks(String json) {
        List<AiTask> tasks = new ArrayList<>();
        if (json == null || json.isBlank()) return tasks;

        // Strip markdown fences if the model wrapped the JSON
        json = json.replaceAll("(?s)```json|```", "").trim();

        // Split on object boundaries
        String[] objects = json.split("\\},\\s*\\{");
        for (String obj : objects) {
            try {
                String icon      = extractJsonString(obj, "icon");
                String label     = extractJsonString(obj, "label");
                String category  = extractJsonString(obj, "category");
                int    dayOffset = extractJsonInt(obj, "dayOffset");

                if (label != null && !label.isBlank()) {
                    tasks.add(new AiTask(
                            icon     != null && !icon.isBlank()     ? icon     : "🤖",
                            label,
                            category != null && !category.isBlank() ? category : "IA",
                            dayOffset
                    ));
                }
            } catch (Exception ignored) { }
        }
        return tasks;
    }

    private String extractJsonString(String obj, String key) {
        String marker = "\"" + key + "\":\"";
        int start = obj.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '\\' && i + 1 < obj.length()) { sb.append(obj.charAt(i + 1)); i++; continue; }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString();
    }

    private int extractJsonInt(String obj, String key) {
        String marker = "\"" + key + "\":";
        int start = obj.indexOf(marker);
        if (start == -1) return 0;
        start += marker.length();
        while (start < obj.length() && obj.charAt(start) == ' ') start++;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (Character.isDigit(c)) sb.append(c);
            else if (sb.length() > 0) break;
        }
        try { return Integer.parseInt(sb.toString()); } catch (Exception e) { return 0; }
    }

    private List<Integer> parseIdList(String response) {
        List<Integer> ids = new ArrayList<>();
        if (response == null || response.isBlank() || response.toLowerCase().contains("none")) return ids;
        for (String part : response.split("[,\\s]+")) {
            try { ids.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) { }
        }
        return ids;
    }

    private String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}