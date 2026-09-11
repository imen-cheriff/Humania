package utilisateur.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import utils.PropertiesUtil;

import java.util.Properties;

/**
 * Mail service using Gmail SMTP.
 * Works immediately — sends to ANY email address.
 *
 * Setup (one-time):
 *  1. Go to your Google Account → Security → 2-Step Verification → App passwords
 *  2. Generate an App Password for "Mail"
 *  3. Put your Gmail and that app password in application.properties
 */
public class MailService {

    private static final String FROM_NAME = "Humania RH";

    // ── Send credentials after candidate conversion ───────────────────────────

    public void envoyerCredentials(String toEmail, String proEmail, String motDePasse)
            throws Exception {
        String html =
                "<html><body style='font-family:Arial,sans-serif;color:#333;'>" +
                        "<div style='max-width:600px;margin:auto;border:1px solid #e5e7eb;" +
                        "border-radius:8px;padding:32px;'>" +
                        "<h2 style='color:#7c3aed;'>Bienvenue chez Humania !</h2>" +
                        "<p>Votre compte professionnel a ete cree avec succes.</p>" +
                        "<table style='border-collapse:collapse;width:100%;margin:16px 0;'>" +
                        "<tr><td style='padding:8px 12px;background:#f3f4f6;font-weight:bold;width:40%;'>" +
                        "Email de connexion</td>" +
                        "<td style='padding:8px 12px;background:#fafafa;'>" + proEmail + "</td></tr>" +
                        "<tr><td style='padding:8px 12px;background:#f3f4f6;font-weight:bold;'>" +
                        "Mot de passe temporaire</td>" +
                        "<td style='padding:8px 12px;background:#fafafa;font-family:monospace;font-size:15px;'>" +
                        motDePasse + "</td></tr></table>" +
                        "<p style='color:#ef4444;font-size:13px;'>Changez votre mot de passe des la premiere connexion.</p>" +
                        "<p>Cordialement,<br/><strong>L'equipe Humania RH</strong></p>" +
                        "</div></body></html>";

        sendEmail(toEmail, "Vos acces Humania - Bienvenue !", html);
    }

    // ── Send OTP for password reset ───────────────────────────────────────────

    public void envoyerOtp(String toEmail, String otp) throws Exception {
        String html =
                "<html><body style='font-family:Arial,sans-serif;color:#333;'>" +
                        "<div style='max-width:600px;margin:auto;border:1px solid #e5e7eb;" +
                        "border-radius:8px;padding:32px;'>" +
                        "<h2 style='color:#7c3aed;'>Reinitialisation du mot de passe</h2>" +
                        "<p>Vous avez demande la reinitialisation de votre mot de passe Humania.</p>" +
                        "<p>Votre code de verification est :</p>" +
                        "<div style='text-align:center;margin:24px 0;'>" +
                        "<span style='font-size:36px;font-weight:bold;letter-spacing:10px;color:#7c3aed;" +
                        "background:#f3f4f6;padding:16px 24px;border-radius:8px;'>" + otp + "</span></div>" +
                        "<p style='color:#e53e3e;font-size:13px;'>Ce code expire dans 10 minutes." +
                        " Ne le partagez avec personne.</p>" +
                        "<p>Si vous n'avez pas fait cette demande, ignorez cet email.</p>" +
                        "<p>Cordialement,<br/><strong>L'equipe Humania RH</strong></p>" +
                        "</div></body></html>";

        sendEmail(toEmail, "Votre code de reinitialisation Humania", html);
    }

    // ── Shared Gmail SMTP send ────────────────────────────────────────────────

    private void sendEmail(String toEmail, String subject, String htmlContent) throws Exception {
        String gmailUser = PropertiesUtil.getProperty("gmail.username");
        String gmailPass = PropertiesUtil.getProperty("gmail.app.password");

        if (gmailUser == null || gmailUser.isBlank()) {
            throw new Exception("Configuration manquante: 'gmail.username' dans application.properties");
        }
        if (gmailPass == null || gmailPass.isBlank()) {
            throw new Exception("Configuration manquante: 'gmail.app.password' dans application.properties");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(gmailUser, gmailPass);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(gmailUser, FROM_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html; charset=UTF-8");

        Transport.send(message);
    }
}