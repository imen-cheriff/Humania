package competence.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exporteur PDF haute qualite pour la matrice des competences.
 * - Format paysage A4 pour maximiser les colonnes
 * - Pagination automatique par groupes de competences ET par groupes d'employes
 * - En-tetes de colonnes inclines a 45 deg pour gagner de la place
 * - Palette de couleurs par niveau (0-5)
 * - Aucun caractere Unicode hors WinAnsi
 */
public class SkillsMatrixPdfExporter {

    // -- Data classes ----------------------------------------------------------
    public static class SkillEntry {
        public final String name;
        public final int    level;
        public SkillEntry(String name, int level) { this.name = name; this.level = level; }
    }

    public static class EmployeeRow {
        public final String           name;
        public final String           poste;
        public final List<SkillEntry> skills;
        public EmployeeRow(String name, String poste, List<SkillEntry> skills) {
            this.name = name; this.poste = poste; this.skills = skills;
        }
    }

    // -- Palette niveaux -------------------------------------------------------
    private static final Color[] LVL_BG = {
            new Color(0xF3, 0xF4, 0xF6),
            new Color(0xFE, 0xE2, 0xE2),
            new Color(0xFF, 0xED, 0xD5),
            new Color(0xFE, 0xF9, 0xC3),
            new Color(0xD1, 0xFA, 0xE5),
            new Color(0xA7, 0xF3, 0xD0),
    };
    private static final Color[] LVL_FG = {
            new Color(0x9C, 0xA3, 0xAF),
            new Color(0x99, 0x1B, 0x1B),
            new Color(0x92, 0x40, 0x0E),
            new Color(0x71, 0x3F, 0x12),
            new Color(0x06, 0x5F, 0x46),
            new Color(0x06, 0x4E, 0x3B),
    };
    private static final String[] LVL_LABEL = { "-", "1", "2", "3", "4", "5" };
    private static final String[] LVL_NAME  = {
            "Non evalue", "Debutant", "Elementaire",
            "Intermediaire", "Avance", "Expert"
    };

    // -- Couleurs UI -----------------------------------------------------------
    private static final Color C_DARK      = new Color(0x1E, 0x1B, 0x4B);
    private static final Color C_ACCENT    = new Color(0x4F, 0x46, 0xE5);
    private static final Color C_ACCENT2   = new Color(0x7C, 0x3A, 0xED);
    private static final Color C_HDR_COL1  = new Color(0x31, 0x2E, 0x81);
    private static final Color C_HDR_COL2  = new Color(0x3D, 0x38, 0x99);
    private static final Color C_BORDER    = new Color(0xC7, 0xD2, 0xFE);
    private static final Color C_ROW_ODD   = new Color(0xF7, 0xF8, 0xFF);
    private static final Color C_ROW_EVEN  = Color.WHITE;
    private static final Color C_FOOTER_BG = new Color(0xEE, 0xF2, 0xFF);
    private static final Color C_WHITE     = Color.WHITE;
    private static final Color C_MUTED     = new Color(0x6B, 0x72, 0x80);
    private static final Color C_TEXT      = new Color(0x11, 0x18, 0x27);

    // -- Mise en page paysage A4 -----------------------------------------------
    private static final float PW       = PDRectangle.A4.getHeight(); // 841.89 pt
    private static final float PH       = PDRectangle.A4.getWidth();  // 595.28 pt
    private static final float MARGIN   = 22f;
    private static final float HDR_H    = 54f;
    private static final float LEG_H    = 20f;
    private static final float FOOT_H   = 18f;
    private static final float COL_HDR  = 50f;   // hauteur entetes inclines
    private static final float ROW_H    = 17f;
    private static final float COL_NAME = 108f;
    private static final float COL_POST = 80f;
    private static final float SKILL_W  = 32f;

    // -- API publique ----------------------------------------------------------
    public void exportToFile(List<EmployeeRow> rows,
                             List<String>      competences,
                             File              outFile) throws IOException {

        float usable  = PW - 2 * MARGIN - COL_NAME - COL_POST;
        int   maxCols = Math.max(1, (int)(usable / SKILL_W));

        float bodyH   = PH - MARGIN - HDR_H - LEG_H - COL_HDR - FOOT_H - MARGIN;
        int   maxRows = Math.max(1, (int)(bodyH / ROW_H));

        // Groupes de colonnes
        List<List<String>> colGroups = new ArrayList<>();
        for (int i = 0; i < competences.size(); i += maxCols)
            colGroups.add(competences.subList(i, Math.min(i + maxCols, competences.size())));
        if (colGroups.isEmpty()) colGroups.add(new ArrayList<>());

        // Groupes de lignes
        List<List<EmployeeRow>> rowGroups = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += maxRows)
            rowGroups.add(rows.subList(i, Math.min(i + maxRows, rows.size())));
        if (rowGroups.isEmpty()) rowGroups.add(new ArrayList<>());

        int totalPages = colGroups.size() * rowGroups.size();

        try (PDDocument doc = new PDDocument()) {
            doc.getDocumentInformation().setTitle("Matrice des competences");
            doc.getDocumentInformation().setCreator("Skills Manager");
            doc.getDocumentInformation().setCreationDate(java.util.Calendar.getInstance());

            PDFont bold    = new PDType1Font(FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(FontName.HELVETICA);
            PDFont italic  = new PDType1Font(FontName.HELVETICA_OBLIQUE);

            int pageNum = 1;
            for (int cg = 0; cg < colGroups.size(); cg++) {
                List<String> cols = colGroups.get(cg);
                String colInfo = colGroups.size() > 1
                        ? "Competences " + (cg * maxCols + 1) + " a " + (cg * maxCols + cols.size())
                        + " / " + competences.size()
                        : null;

                for (int rg = 0; rg < rowGroups.size(); rg++) {
                    List<EmployeeRow> rRows = rowGroups.get(rg);
                    String rowInfo = rowGroups.size() > 1
                            ? "Employes " + (rg * maxRows + 1) + " a " + (rg * maxRows + rRows.size())
                            + " / " + rows.size()
                            : null;

                    PDPage page = new PDPage(new PDRectangle(PW, PH));
                    doc.addPage(page);

                    try (PDPageContentStream cs = new PDPageContentStream(
                            doc, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {

                        float tableW = COL_NAME + COL_POST + cols.size() * SKILL_W;

                        drawHeader(cs, bold, regular,
                                rows.size(), competences.size(),
                                pageNum, totalPages, colInfo, rowInfo);
                        drawLegend(cs, bold, regular);

                        float tableTopY = PH - MARGIN - HDR_H - LEG_H;
                        drawColHeaders(cs, bold, italic, cols, MARGIN, tableTopY, tableW);

                        float dataTopY = tableTopY - COL_HDR;
                        drawRows(cs, bold, regular, rRows, cols, MARGIN, dataTopY);

                        float tableBottomY = dataTopY - ROW_H * rRows.size();
                        drawTableBorder(cs, MARGIN, tableBottomY, tableW,
                                COL_HDR + ROW_H * rRows.size());

                        drawFooter(cs, regular, italic, pageNum, totalPages);
                    }
                    pageNum++;
                }
            }

            doc.save(outFile);
        }
    }

    // -- Sections --------------------------------------------------------------

    private void drawHeader(PDPageContentStream cs, PDFont bold, PDFont regular,
                            int totalEmp, int totalSkill,
                            int pageNum, int totalPages,
                            String colInfo, String rowInfo) throws IOException {
        float x = MARGIN, y = PH - MARGIN - HDR_H;

        fillRect(cs, C_DARK,   x, y, PW - 2 * MARGIN, HDR_H);
        fillRect(cs, C_ACCENT, x, y + HDR_H - 4f, PW - 2 * MARGIN, 4f);
        fillRect(cs, C_ACCENT2, x, y, 5f, HDR_H);

        setFill(cs, C_WHITE);
        drawText(cs, bold, 13f, "Matrice des Competences", x + 14f, y + HDR_H - 20f);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        String sub  = totalEmp + " employes  |  " + totalSkill + " competences  |  " + date;
        if (colInfo != null) sub = "[" + colInfo + "]  " + sub;
        if (rowInfo != null) sub = sub + "  [" + rowInfo + "]";

        setFill(cs, new Color(0xC7, 0xD2, 0xFE));
        drawText(cs, regular, 7.5f, sub, x + 14f, y + 9f);

        String pg = "Page " + pageNum + " / " + totalPages;
        setFill(cs, new Color(0xC7, 0xD2, 0xFE));
        drawText(cs, bold, 8f, pg,
                PW - MARGIN - strW(bold, 8f, pg) - 10f, y + HDR_H - 20f);
    }

    private void drawLegend(PDPageContentStream cs, PDFont bold, PDFont regular) throws IOException {
        float y = PH - MARGIN - HDR_H - LEG_H + 3f;
        fillRect(cs, C_FOOTER_BG, MARGIN, y, PW - 2 * MARGIN, LEG_H - 2f);

        setFill(cs, C_MUTED);
        drawText(cs, bold, 6.5f, "NIVEAUX:", MARGIN + 8f, y + 5f);
        float lx = MARGIN + 54f;

        for (int i = 0; i < 6; i++) {
            fillRect(cs, LVL_BG[i], lx, y + 3f, 8f, 8f);
            strokeRect(cs, LVL_FG[i], lx, y + 3f, 8f, 8f, 0.4f);
            lx += 11f;
            setFill(cs, C_TEXT);
            String lbl = i + " = " + LVL_NAME[i];
            drawText(cs, regular, 6.5f, lbl, lx, y + 5f);
            lx += strW(regular, 6.5f, lbl) + 12f;
        }
    }

    private void drawColHeaders(PDPageContentStream cs, PDFont bold, PDFont italic,
                                List<String> cols, float tableX, float topY, float tableW) throws IOException {
        float y = topY - COL_HDR;

        fillRect(cs, C_HDR_COL1, tableX, y, tableW, COL_HDR);

        // Colonnes fixes
        setFill(cs, C_WHITE);
        drawText(cs, bold, 7f, "EMPLOYE",  tableX + 4f, y + COL_HDR / 2f - 3f);
        vline(cs, C_BORDER, tableX + COL_NAME, y, COL_HDR, 0.5f);
        drawText(cs, bold, 7f, "POSTE", tableX + COL_NAME + 4f, y + COL_HDR / 2f - 3f);
        vline(cs, C_BORDER, tableX + COL_NAME + COL_POST, y, COL_HDR, 0.5f);

        // En-tetes inclines
        float cx = tableX + COL_NAME + COL_POST;
        for (int i = 0; i < cols.size(); i++) {
            Color bg = (i % 2 == 0) ? C_HDR_COL1 : C_HDR_COL2;
            fillRect(cs, bg, cx, y, SKILL_W, COL_HDR);
            vline(cs, C_BORDER, cx + SKILL_W, y, COL_HDR, 0.3f);

            String label = sanitize(truncate(cols.get(i), COL_HDR - 4f, italic, 7f));
            // Rotation 45 deg, origine centree en bas de la cellule
            float tx = cx + SKILL_W / 2f - 2f;
            float ty = y + 5f;
            cs.beginText();
            cs.setFont(italic, 7f);
            setFill(cs, C_WHITE);
            float cos45 = (float)Math.cos(Math.PI / 4);
            float sin45 = (float)Math.sin(Math.PI / 4);
            cs.setTextMatrix(new Matrix(cos45, sin45, -sin45, cos45, tx, ty));
            cs.showText(label);
            cs.endText();

            cx += SKILL_W;
        }
    }

    private void drawRows(PDPageContentStream cs, PDFont bold, PDFont regular,
                          List<EmployeeRow> rRows, List<String> cols,
                          float tableX, float topY) throws IOException {
        float y = topY;
        for (int i = 0; i < rRows.size(); i++) {
            EmployeeRow row = rRows.get(i);
            y -= ROW_H;
            float rowW = COL_NAME + COL_POST + cols.size() * SKILL_W;

            fillRect(cs, i % 2 == 0 ? C_ROW_ODD : C_ROW_EVEN, tableX, y, rowW, ROW_H);
            hline(cs, C_BORDER, tableX, y, rowW, 0.25f);

            // Nom
            setFill(cs, C_TEXT);
            drawText(cs, bold, 7.5f,
                    sanitize(truncate(row.name != null ? row.name : "", COL_NAME - 7f, bold, 7.5f)),
                    tableX + 4f, y + (ROW_H - 7.5f) / 2f + 1f);
            vline(cs, C_BORDER, tableX + COL_NAME, y, ROW_H, 0.4f);

            // Poste
            setFill(cs, C_MUTED);
            drawText(cs, regular, 7f,
                    sanitize(truncate(row.poste != null ? row.poste : "", COL_POST - 5f, regular, 7f)),
                    tableX + COL_NAME + 3f, y + (ROW_H - 7f) / 2f + 1f);
            vline(cs, C_BORDER, tableX + COL_NAME + COL_POST, y, ROW_H, 0.4f);

            // Cellules competences
            float cx = tableX + COL_NAME + COL_POST;
            for (String comp : cols) {
                int lvl = 0;
                for (SkillEntry se : row.skills)
                    if (comp.equals(se.name)) { lvl = Math.min(5, Math.max(0, se.level)); break; }

                float pad = 2.5f;
                fillRect(cs, LVL_BG[lvl], cx + pad, y + pad, SKILL_W - 2*pad, ROW_H - 2*pad);

                setFill(cs, LVL_FG[lvl]);
                String lbl = LVL_LABEL[lvl];
                drawText(cs, bold, 7.5f, lbl,
                        cx + (SKILL_W - strW(bold, 7.5f, lbl)) / 2f,
                        y + (ROW_H - 7.5f) / 2f + 1f);

                vline(cs, C_BORDER, cx + SKILL_W, y, ROW_H, 0.25f);
                cx += SKILL_W;
            }
        }
    }

    private void drawTableBorder(PDPageContentStream cs,
                                 float x, float y, float w, float h) throws IOException {
        setStroke(cs, C_ACCENT);
        cs.setLineWidth(1f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private void drawFooter(PDPageContentStream cs, PDFont regular, PDFont italic,
                            int pageNum, int totalPages) throws IOException {
        float y = MARGIN - 2f;
        fillRect(cs, C_FOOTER_BG, MARGIN, y, PW - 2 * MARGIN, FOOT_H - 2f);
        setFill(cs, C_MUTED);
        drawText(cs, italic, 6.5f,
                "Skills Manager  |  Document confidentiel  |  "
                        + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                MARGIN + 8f, y + 5f);
        String pg = pageNum + " / " + totalPages;
        drawText(cs, regular, 6.5f, pg,
                PW - MARGIN - strW(regular, 6.5f, pg) - 8f, y + 5f);
    }

    // -- Lignes ----------------------------------------------------------------
    private void vline(PDPageContentStream cs, Color c, float x, float y, float h, float lw) throws IOException {
        setStroke(cs, c); cs.setLineWidth(lw);
        cs.moveTo(x, y); cs.lineTo(x, y + h); cs.stroke();
    }
    private void hline(PDPageContentStream cs, Color c, float x, float y, float w, float lw) throws IOException {
        setStroke(cs, c); cs.setLineWidth(lw);
        cs.moveTo(x, y); cs.lineTo(x + w, y); cs.stroke();
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
    /** Remplace les caracteres hors WinAnsi par des equivalents ASCII. */
    private String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if      (c == '\u2713' || c == '\u2714') sb.append('v');
            else if (c == '\u2717' || c == '\u2718') sb.append('x');
            else if (c == '\u2019' || c == '\u2018') sb.append('\'');
            else if (c == '\u201C' || c == '\u201D') sb.append('"');
            else if (c == '\u2014' || c == '\u2013') sb.append('-');
            else if (c < 256)                        sb.append(c);
            else                                     sb.append('?');
        }
        return sb.toString();
    }
}