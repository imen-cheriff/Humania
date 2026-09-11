package competence.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exporteur PDF haute qualite pour les rapports de formation (modules).
 * - Page de couverture avec barre de progression
 * - Une carte par module avec couleurs selon statut
 * - Wrapping automatique du texte
 * - Aucun caractere Unicode hors WinAnsi
 */
public class ModulePdfExporter {

    // -- Data classes ----------------------------------------------------------
    public static class ModuleData {
        public final int    ordre;
        public final String titre;
        public final String typeContenu;
        public final int    dureeMinutes;
        public final String statut;
        public final int    scoreQuiz;
        public final String description;
        public final String contenuTexte;

        public ModuleData(int ordre, String titre, String typeContenu, int dureeMinutes,
                          String statut, int scoreQuiz, String description, String contenuTexte) {
            this.ordre = ordre; this.titre = titre; this.typeContenu = typeContenu;
            this.dureeMinutes = dureeMinutes; this.statut = statut; this.scoreQuiz = scoreQuiz;
            this.description = description; this.contenuTexte = contenuTexte;
        }
    }

    public static class ExportData {
        public final String           formationTitre;
        public final String           formateurNom;
        public final int              progressPct;
        public final boolean          includeContent;
        public final boolean          includeDesc;
        public final List<ModuleData> modules = new ArrayList<>();

        public ExportData(String formationTitre, String formateurNom, int progressPct,
                          boolean includeContent, boolean includeDesc) {
            this.formationTitre = formationTitre; this.formateurNom = formateurNom;
            this.progressPct = progressPct; this.includeContent = includeContent;
            this.includeDesc = includeDesc;
        }
    }

    // -- Palette ---------------------------------------------------------------
    private static final Color C_DARK       = new Color(0x1E, 0x1B, 0x4B);
    private static final Color C_ACCENT     = new Color(0x4F, 0x46, 0xE5);
    private static final Color C_ACCENT2    = new Color(0x7C, 0x3A, 0xED);
    private static final Color C_INDIGO_100 = new Color(0xE0, 0xE7, 0xFF);
    private static final Color C_INDIGO_50  = new Color(0xEE, 0xF2, 0xFF);
    private static final Color C_GREEN_BG   = new Color(0xF0, 0xFD, 0xF4);
    private static final Color C_GREEN_BD   = new Color(0x86, 0xEF, 0xAC);
    private static final Color C_GREEN_FG   = new Color(0x16, 0x65, 0x34);
    private static final Color C_BLUE_BG    = new Color(0xEF, 0xF6, 0xFF);
    private static final Color C_BLUE_BD    = new Color(0x93, 0xC5, 0xFD);
    private static final Color C_BLUE_FG    = new Color(0x1D, 0x4E, 0xD8);
    private static final Color C_GRAY_BG    = new Color(0xF9, 0xFA, 0xFB);
    private static final Color C_GRAY_BD    = new Color(0xE5, 0xE7, 0xEB);
    private static final Color C_GRAY_FG    = new Color(0x6B, 0x72, 0x80);
    private static final Color C_TEXT       = new Color(0x11, 0x18, 0x27);
    private static final Color C_WHITE      = Color.WHITE;

    // -- Mise en page portrait A4 ----------------------------------------------
    private static final float PW      = PDRectangle.A4.getWidth();   // 595
    private static final float PH      = PDRectangle.A4.getHeight();  // 842
    private static final float MARGIN  = 32f;
    private static final float CONTENT = PW - 2 * MARGIN;

    // -- API publique ----------------------------------------------------------
    public void exportToFile(ExportData data, File outFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.getDocumentInformation().setTitle(
                    sanitize(data.formationTitre != null ? data.formationTitre : "Formation"));
            doc.getDocumentInformation().setCreator("Skills Manager");
            doc.getDocumentInformation().setCreationDate(java.util.Calendar.getInstance());

            PDFont bold    = new PDType1Font(FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(FontName.HELVETICA);
            PDFont italic  = new PDType1Font(FontName.HELVETICA_OBLIQUE);

            // Page de couverture
            buildCoverPage(doc, bold, regular, italic, data);

            // Pages de modules
            final float BODY_TOP = PH - MARGIN - 50f;  // sous le mini-header
            final float BODY_BOT = MARGIN + 22f;        // au-dessus du footer

            PDPage currentPage = null;
            PDPageContentStream cs = null;
            float y = 0;
            int pageNum = 2;
            int totalPages = 1 + estimateModulePages(data, bold, regular, BODY_TOP, BODY_BOT);

            for (int i = 0; i < data.modules.size(); i++) {
                ModuleData m   = data.modules.get(i);
                float      cardH = calcCardH(m, data, regular, bold);

                if (currentPage == null || y - cardH < BODY_BOT) {
                    if (cs != null) {
                        drawModPageFooter(cs, regular, italic, pageNum - 1, totalPages);
                        cs.close();
                    }
                    currentPage = new PDPage(PDRectangle.A4);
                    doc.addPage(currentPage);
                    cs = new PDPageContentStream(doc, currentPage,
                            PDPageContentStream.AppendMode.OVERWRITE, true, true);
                    drawModPageHeader(cs, bold, regular, data.formationTitre, pageNum, totalPages);
                    y = BODY_TOP;
                    pageNum++;
                }

                drawCard(cs, bold, regular, italic, m, data, y);
                y -= cardH + 7f;
            }

            if (cs != null) {
                drawModPageFooter(cs, regular, italic, pageNum - 1, totalPages);
                cs.close();
            }

            doc.save(outFile);
        }
    }

    // -- Page de couverture ----------------------------------------------------

    private void buildCoverPage(PDDocument doc, PDFont bold, PDFont regular,
                                PDFont italic, ExportData data) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {

            // Bandeau superieur
            fillRect(cs, C_DARK,   0, PH - 240f, PW, 240f);
            fillRect(cs, C_ACCENT, 0, PH - 244f, PW, 4f);
            fillRect(cs, C_ACCENT2, 0, PH - 240f, 6f, 240f);

            // Titre
            String title = sanitize(data.formationTitre != null ? data.formationTitre : "Formation");
            float  tSize = strW(bold, 24f, title) > CONTENT - 20f ? 18f : 24f;
            setFill(cs, C_WHITE);
            drawText(cs, bold, tSize, title,
                    (PW - strW(bold, tSize, title)) / 2f, PH - 95f);

            // Sous-titre
            String sub = "Rapport de Formation";
            setFill(cs, C_INDIGO_100);
            drawText(cs, regular, 12f, sub,
                    (PW - strW(regular, 12f, sub)) / 2f, PH - 125f);

            // Ligne decorative
            setFill(cs, C_ACCENT2);
            fillRect(cs, C_ACCENT2,
                    (PW - 60f) / 2f, PH - 140f, 60f, 2f);

            // Carte metadonnees
            float cX = MARGIN + 30f, cW = CONTENT - 60f, cH = 160f;
            float cY = PH - 340f - cH;
            fillRect(cs, C_WHITE, cX, cY, cW, cH);
            strokeRect(cs, C_INDIGO_100, cX, cY, cW, cH, 1f);
            fillRect(cs, C_ACCENT, cX, cY + cH - 3f, cW, 3f); // top stripe

            float ry = cY + cH - 28f;
            metaRow(cs, bold, regular, "Formateur",
                    sanitize(data.formateurNom != null && !data.formateurNom.isEmpty()
                            ? data.formateurNom : "Non specifie"),
                    cX + 16f, ry);
            ry -= 26f;
            metaRow(cs, bold, regular, "Modules",
                    data.modules.size() + " module(s) selectionne(s)", cX + 16f, ry);
            ry -= 26f;
            int done = (int) data.modules.stream().filter(m -> "completed".equals(m.statut)).count();
            metaRow(cs, bold, regular, "Progression",
                    data.progressPct + "%  (" + done + " / " + data.modules.size() + " termine(s))",
                    cX + 16f, ry);
            ry -= 26f;
            metaRow(cs, bold, regular, "Date",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)),
                    cX + 16f, ry);

            // Barre de progression
            float bX = cX + 16f, bY = cY + 14f, bW = cW - 32f, bH = 10f;
            fillRect(cs, C_INDIGO_100, bX, bY, bW, bH);
            float fill = bW * Math.min(100, Math.max(0, data.progressPct)) / 100f;
            if (fill > 0) fillRect(cs, C_ACCENT, bX, bY, fill, bH);
            setFill(cs, C_ACCENT);
            String pctLabel = data.progressPct + "%";
            drawText(cs, bold, 7f, pctLabel, bX + bW + 6f, bY + 1f);

            // Pied de page couverture
            setFill(cs, C_GRAY_FG);
            String foot = "Document genere automatiquement - Skills Manager";
            drawText(cs, italic, 7.5f, foot,
                    (PW - strW(italic, 7.5f, foot)) / 2f, MARGIN + 8f);
        }
    }

    private void metaRow(PDPageContentStream cs, PDFont bold, PDFont regular,
                         String label, String value, float x, float y) throws IOException {
        setFill(cs, C_GRAY_FG);
        drawText(cs, bold, 8.5f, label + " :", x, y);
        float lx = x + strW(bold, 8.5f, label + " :") + 6f;
        setFill(cs, C_TEXT);
        drawText(cs, regular, 8.5f, value, lx, y);
    }

    // -- En-tete / pied de page des pages modules ------------------------------

    private void drawModPageHeader(PDPageContentStream cs, PDFont bold, PDFont regular,
                                   String titre, int pageNum, int totalPages) throws IOException {
        fillRect(cs, C_DARK, 0, PH - 42f, PW, 42f);
        fillRect(cs, C_ACCENT, 0, PH - 44f, PW, 2f);
        setFill(cs, C_WHITE);
        drawText(cs, bold, 10f, sanitize(titre != null ? titre : "Formation"),
                MARGIN, PH - 26f);
        String pg = "Page " + pageNum + " / " + totalPages;
        setFill(cs, C_INDIGO_100);
        drawText(cs, regular, 8f, pg,
                PW - MARGIN - strW(regular, 8f, pg), PH - 26f);
    }

    private void drawModPageFooter(PDPageContentStream cs, PDFont regular, PDFont italic,
                                   int pageNum, int totalPages) throws IOException {
        fillRect(cs, C_INDIGO_50, 0, 0, PW, MARGIN);
        setFill(cs, C_GRAY_FG);
        drawText(cs, italic, 6.5f, "Skills Manager  |  Document confidentiel",
                MARGIN, 8f);
        String pg = pageNum + " / " + totalPages;
        drawText(cs, regular, 6.5f, pg,
                PW - MARGIN - strW(regular, 6.5f, pg), 8f);
    }

    // -- Carte de module -------------------------------------------------------

    private void drawCard(PDPageContentStream cs, PDFont bold, PDFont regular,
                          PDFont italic, ModuleData m, ExportData data,
                          float topY) throws IOException {
        boolean done   = "completed".equals(m.statut);
        boolean active = "in_progress".equals(m.statut);

        Color bgColor = done ? C_GREEN_BG  : active ? C_BLUE_BG  : C_GRAY_BG;
        Color bdColor = done ? C_GREEN_BD  : active ? C_BLUE_BD  : C_GRAY_BD;
        Color fgColor = done ? C_GREEN_FG  : active ? C_BLUE_FG  : C_GRAY_FG;

        float cardH = calcCardH(m, data, regular, bold);
        float cardY = topY - cardH;
        float cardX = MARGIN;

        fillRect(cs, bgColor, cardX, cardY, CONTENT, cardH);
        strokeRect(cs, bdColor, cardX, cardY, CONTENT, cardH, 0.8f);
        fillRect(cs, fgColor, cardX, cardY, 4f, cardH);  // barre gauche

        float innerX = cardX + 13f;
        float y      = topY - 13f;

        // Badge numero
        float bR = 9f;
        drawCircle(cs, fgColor, innerX + bR, y - 1f, bR);
        setFill(cs, C_WHITE);
        String ordStr = String.valueOf(m.ordre);
        drawText(cs, bold, 7f, ordStr,
                innerX + bR - strW(bold, 7f, ordStr) / 2f, y - 4.5f);

        // Titre
        setFill(cs, done ? C_GREEN_FG : active ? C_BLUE_FG : C_TEXT);
        float titleMaxW = CONTENT - 160f;
        drawText(cs, bold, 10f,
                sanitize(truncate(m.titre, titleMaxW, bold, 10f)),
                innerX + bR * 2 + 6f, y - 2f);

        // Chip statut (droite)
        String chipTxt = done ? "Termine" : active ? "En cours" : "Non demarre";
        float  chipW   = strW(bold, 7.5f, chipTxt) + 14f;
        float  chipX   = cardX + CONTENT - chipW - 10f;
        fillRect(cs, bdColor, chipX, y - 10f, chipW, 14f);
        setFill(cs, fgColor);
        drawText(cs, bold, 7.5f, chipTxt, chipX + 7f, y - 6f);

        y -= 17f;

        // Ligne meta
        String typeLabel = contentTypeLabel(m.typeContenu);
        String meta      = typeLabel + "  |  " + m.dureeMinutes + " min";
        if (done && m.scoreQuiz >= 0) meta += "  |  Score: " + m.scoreQuiz + "%";
        setFill(cs, C_GRAY_FG);
        drawText(cs, italic, 8f, sanitize(meta), innerX + bR * 2 + 6f, y);
        y -= 13f;

        // Description
        if (data.includeDesc && m.description != null && !m.description.isBlank()) {
            List<String> lines = wrapText(m.description, CONTENT - 28f, regular, 8f);
            setFill(cs, C_TEXT);
            for (String line : lines) {
                drawText(cs, regular, 8f, sanitize(line), innerX + 4f, y);
                y -= 11f;
            }
            y -= 2f;
        }

        // Contenu texte
        if (data.includeContent && m.contenuTexte != null && !m.contenuTexte.isBlank()) {
            // separateur
            setStroke(cs, bdColor);
            cs.setLineWidth(0.5f);
            cs.moveTo(innerX, y + 6f);
            cs.lineTo(cardX + CONTENT - 12f, y + 6f);
            cs.stroke();
            y -= 5f;

            setFill(cs, C_GRAY_FG);
            drawText(cs, bold, 7.5f, "Contenu :", innerX + 4f, y);
            y -= 12f;

            List<String> lines = wrapText(m.contenuTexte, CONTENT - 28f, regular, 7.5f);
            setFill(cs, C_TEXT);
            for (String line : lines) {
                drawText(cs, regular, 7.5f, sanitize(line), innerX + 4f, y);
                y -= 10.5f;
            }
        }
    }

    private void drawCircle(PDPageContentStream cs, Color c, float cx, float cy, float r) throws IOException {
        float k = 0.5523f * r;
        setFill(cs, c);
        cs.moveTo(cx - r, cy);
        cs.curveTo(cx - r, cy + k, cx - k, cy + r, cx, cy + r);
        cs.curveTo(cx + k, cy + r, cx + r, cy + k, cx + r, cy);
        cs.curveTo(cx + r, cy - k, cx + k, cy - r, cx, cy - r);
        cs.curveTo(cx - k, cy - r, cx - r, cy - k, cx - r, cy);
        cs.fill();
    }

    // -- Calculs de hauteur ----------------------------------------------------

    private float calcCardH(ModuleData m, ExportData data, PDFont regular, PDFont bold) {
        float h = 13f + 17f + 8f; // titre + meta + padding
        if (data.includeDesc && m.description != null && !m.description.isBlank())
            h += wrapText(m.description, CONTENT - 28f, regular, 8f).size() * 11f + 2f;
        if (data.includeContent && m.contenuTexte != null && !m.contenuTexte.isBlank())
            h += 5f + 12f + wrapText(m.contenuTexte, CONTENT - 28f, regular, 7.5f).size() * 10.5f;
        return h + 10f;
    }

    private int estimateModulePages(ExportData data, PDFont bold, PDFont regular,
                                    float bodyTop, float bodyBot) {
        float usable = bodyTop - bodyBot;
        float used   = 0; int pages = 1;
        for (ModuleData m : data.modules) {
            float ch = calcCardH(m, data, regular, bold) + 7f;
            if (used + ch > usable) { pages++; used = 0; }
            used += ch;
        }
        return pages;
    }

    private List<String> wrapText(String text, float maxW, PDFont font, float size) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split("\\s+")) {
                if (word.isEmpty()) continue;
                String test = line.length() == 0 ? word : line + " " + word;
                if (strW(font, size, sanitize(test)) > maxW && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) lines.add(line.toString());
        }
        return lines;
    }

    // -- Primitives ------------------------------------------------------------

    private void fillRect(PDPageContentStream cs, Color c, float x, float y, float w, float h) throws IOException {
        setFill(cs, c); cs.addRect(x, y, w, h); cs.fill();
    }
    private void strokeRect(PDPageContentStream cs, Color c, float x, float y, float w, float h, float lw) throws IOException {
        setStroke(cs, c); cs.setLineWidth(lw); cs.addRect(x, y, w, h); cs.stroke();
    }
    private void setFill(PDPageContentStream cs, Color c) throws IOException {
        cs.setNonStrokingColor(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);
    }
    private void setStroke(PDPageContentStream cs, Color c) throws IOException {
        cs.setStrokingColor(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);
    }
    private void drawText(PDPageContentStream cs, PDFont font, float size,
                          String text, float x, float y) throws IOException {
        if (text == null || text.isEmpty()) return;
        cs.beginText(); cs.setFont(font, size);
        cs.newLineAtOffset(x, y); cs.showText(text); cs.endText();
    }
    private float strW(PDFont font, float size, String text) {
        try { return font.getStringWidth(sanitize(text)) / 1000f * size; }
        catch (IOException e) { return text.length() * size * 0.5f; }
    }
    private String truncate(String text, float maxW, PDFont font, float size) {
        if (text == null) return "";
        text = sanitize(text);
        if (strW(font, size, text) <= maxW) return text;
        while (text.length() > 1 && strW(font, size, text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
    private String contentTypeLabel(String type) {
        if (type == null) return "Document";
        switch (type.toLowerCase()) {
            case "video":                      return "Video";
            case "cours": case "lecture":      return "Cours";
            case "exercice": case "exercise":  return "Exercice";
            case "quiz":                       return "Quiz";
            default:                           return sanitize(type);
        }
    }
    /** Remplace tous les caracteres hors WinAnsi par des equivalents ASCII lisibles. */
    private String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if      (c == '\u2713' || c == '\u2714') sb.append('v');
            else if (c == '\u2717' || c == '\u2718') sb.append('x');
            else if (c == '\u2019' || c == '\u2018') sb.append('\'');
            else if (c == '\u201C' || c == '\u201D') sb.append('"');
            else if (c == '\u2014' || c == '\u2013') sb.append('-');
            else if (c == '\u00e9') sb.append('\u00e9'); // e accent - valide WinAnsi
            else if (c < 256)      sb.append(c);
            else                   sb.append('?');
        }
        return sb.toString();
    }
}