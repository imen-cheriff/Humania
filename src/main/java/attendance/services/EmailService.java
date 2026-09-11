package attendance.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import attendance.models.Absence;
import attendance.models.Conge;

/**
 * Service d'envoi d'emails via Resend API
 * https://resend.com
 */
public class EmailService {

    // ✅ CONFIGUREZ CES VALEURS
    private static final String API_KEY      = "re_bzZLTAw2_Dh1tH5jLCkx3sfwXSKTfzfUp";
    private static final String FROM_EMAIL   = "onboarding@resend.dev";        // domaine Resend pour les tests
    private static final String RH_EMAIL     = "insafweslati0@gmail.com";        // email du RH / chef d'équipe
    private static final String RH_NOM       = "Responsable RH";

    private static final String API_URL = "https://api.resend.com/emails";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // =====================================================================
    //  ENVOI EMAIL - NOUVELLE DEMANDE D'ABSENCE
    // =====================================================================
    public void envoyerNotificationAbsence(Absence absence, String nomEmploye) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String dateDebut = absence.getDateDebut() != null ? absence.getDateDebut().format(fmt) : "?";
            String heureDebut = absence.getHeureDebut() != null ? absence.getHeureDebut() : "?";
            String heureFin   = absence.getHeureFin()   != null ? absence.getHeureFin()   : "?";
            String motif      = absence.getMotif()      != null ? absence.getMotif()       : "Non précisé";
            String duree      = absence.getDureeMinutes() > 0
                    ? formatDuree(absence.getDureeMinutes()) : "?";

            String sujet = "🔔 Nouvelle demande d'autorisation - " + nomEmploye;

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <style>
                            body { font-family: 'Segoe UI', Arial, sans-serif; background: #f8fafc; margin: 0; padding: 20px; }
                            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                            .header { background: linear-gradient(135deg, #667eea, #764ba2); padding: 28px 32px; }
                            .header h1 { color: white; margin: 0; font-size: 22px; }
                            .header p  { color: rgba(255,255,255,0.8); margin: 6px 0 0; font-size: 14px; }
                            .body { padding: 28px 32px; }
                            .badge { display: inline-block; background: #fef3c7; color: #d97706; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 16px; }
                            .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 20px 0; }
                            .info-box { background: #f8fafc; border-radius: 8px; padding: 14px 16px; border-left: 3px solid #667eea; }
                            .info-box .label { color: #94a3b8; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                            .info-box .value { color: #1e293b; font-weight: bold; font-size: 14px; }
                            .motif-box { background: #f0f4ff; border-radius: 8px; padding: 14px 16px; margin: 16px 0; border-left: 3px solid #667eea; }
                            .btn { display: inline-block; background: linear-gradient(135deg, #667eea, #764ba2); color: white; text-decoration: none; padding: 12px 28px; border-radius: 8px; font-weight: bold; font-size: 14px; margin-top: 20px; }
                            .footer { background: #f8fafc; padding: 16px 32px; text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>🗓️ Nouvelle demande d'autorisation</h1>
                                <p>Une demande vient d'être soumise et attend votre validation</p>
                            </div>
                            <div class="body">
                                <div class="badge">⏳ En attente de validation</div>
                                <p style="color:#1e293b; font-size:15px; margin:0 0 16px;">
                                    <strong>%s</strong> a soumis une demande d'autorisation d'absence.
                                </p>
                                <div class="info-grid">
                                    <div class="info-box">
                                        <div class="label">📅 Date</div>
                                        <div class="value">%s</div>
                                    </div>
                                    <div class="info-box">
                                        <div class="label">⌛ Durée</div>
                                        <div class="value">%s</div>
                                    </div>
                                    <div class="info-box">
                                        <div class="label">🕐 Heure début</div>
                                        <div class="value">%s</div>
                                    </div>
                                    <div class="info-box">
                                        <div class="label">🕔 Heure fin</div>
                                        <div class="value">%s</div>
                                    </div>
                                </div>
                                <div class="motif-box">
                                    <div class="label" style="color:#667eea; font-size:11px; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:6px;">📝 Motif</div>
                                    <div style="color:#1e293b; font-size:14px;">%s</div>
                                </div>
                                <p style="color:#64748b; font-size:13px; margin-top:20px;">
                                    Connectez-vous à l'application <strong>Humania</strong> pour approuver ou refuser cette demande.
                                </p>
                            </div>
                            <div class="footer">
                                Humania RH Management • Email automatique, merci de ne pas répondre
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(nomEmploye, dateDebut, duree, heureDebut, heureFin, motif);

            envoyerEmail(RH_EMAIL, RH_NOM, sujet, html);
            System.out.println("✅ Email absence envoyé à " + RH_EMAIL);

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email absence : " + e.getMessage());
        }
    }

    // =====================================================================
    //  ENVOI EMAIL - NOUVELLE DEMANDE DE CONGÉ
    // =====================================================================
    public void envoyerNotificationConge(Conge conge, String nomEmploye) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String dateDebut = conge.getDateDebut() != null ? conge.getDateDebut().format(fmt) : "?";
            String dateFin   = conge.getDateFin()   != null ? conge.getDateFin().format(fmt)   : "?";
            int    nbrJours  = conge.getNbrJours();

            String sujet = "🏖️ Nouvelle demande de congé - " + nomEmploye;

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <style>
                            body { font-family: 'Segoe UI', Arial, sans-serif; background: #f8fafc; margin: 0; padding: 20px; }
                            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                            .header { background: linear-gradient(135deg, #10b981, #059669); padding: 28px 32px; }
                            .header h1 { color: white; margin: 0; font-size: 22px; }
                            .header p  { color: rgba(255,255,255,0.8); margin: 6px 0 0; font-size: 14px; }
                            .body { padding: 28px 32px; }
                            .badge { display: inline-block; background: #d1fae5; color: #059669; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 16px; }
                            .info-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin: 20px 0; }
                            .info-box { background: #f0fdf4; border-radius: 8px; padding: 14px 16px; border-left: 3px solid #10b981; }
                            .info-box .label { color: #94a3b8; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                            .info-box .value { color: #1e293b; font-weight: bold; font-size: 14px; }
                            .footer { background: #f8fafc; padding: 16px 32px; text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>🏖️ Nouvelle demande de congé</h1>
                                <p>Une demande vient d'être soumise et attend votre validation</p>
                            </div>
                            <div class="body">
                                <div class="badge">⏳ En attente de validation</div>
                                <p style="color:#1e293b; font-size:15px; margin:0 0 16px;">
                                    <strong>%s</strong> a soumis une demande de congé.
                                </p>
                                <div class="info-grid">
                                    <div class="info-box">
                                        <div class="label">📅 Début</div>
                                        <div class="value">%s</div>
                                    </div>
                                    <div class="info-box">
                                        <div class="label">📅 Fin</div>
                                        <div class="value">%s</div>
                                    </div>
                                    <div class="info-box">
                                        <div class="label">⌛ Durée</div>
                                        <div class="value">%d jour(s)</div>
                                    </div>
                                </div>
                                <p style="color:#64748b; font-size:13px; margin-top:20px;">
                                    Connectez-vous à l'application <strong>Humania</strong> pour approuver ou refuser cette demande.
                                </p>
                            </div>
                            <div class="footer">
                                Humania RH Management • Email automatique, merci de ne pas répondre
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(nomEmploye, dateDebut, dateFin, nbrJours);

            envoyerEmail(RH_EMAIL, RH_NOM, sujet, html);
            System.out.println("✅ Email congé envoyé à " + RH_EMAIL);

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email congé : " + e.getMessage());
        }
    }

    // =====================================================================
    //  MÉTHODE COMMUNE D'ENVOI
    // =====================================================================
    private void envoyerEmail(String toEmail, String toNom, String sujet, String html) throws Exception {
        String json = """
                {
                    "from": "%s",
                    "to": ["%s"],
                    "subject": "%s",
                    "html": %s
                }
                """.formatted(
                FROM_EMAIL,
                toEmail,
                sujet,
                toJson(html)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            System.out.println("✅ Resend OK : " + response.body());
        } else {
            System.err.println("❌ Resend erreur " + response.statusCode() + " : " + response.body());
        }
    }

    // Convertit une string HTML en JSON string (échappe les guillemets et sauts de ligne)
    private String toJson(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "  ");
        return "\"" + escaped + "\"";
    }

    private String formatDuree(int minutes) {
        if (minutes >= 60) {
            int h = minutes / 60;
            int m = minutes % 60;
            return m > 0 ? h + "h" + m + "min" : h + "h";
        }
        return minutes + " min";
    }
}