package utils;

import utilisateur.models.Utilisateur;

public class Session {

    // 🔐 Utilisateur actuellement connecté
    private static Utilisateur utilisateurConnecte;

    // ================= GETTERS / SETTERS =================

    public static Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public static void setUtilisateurConnecte(Utilisateur utilisateur) {
        utilisateurConnecte = utilisateur;
    }

    // ================= SESSION MANAGEMENT =================

    public static void clear() {
        utilisateurConnecte = null;
    }

    public static boolean isLoggedIn() {
        return utilisateurConnecte != null;
    }

    // ================= UTILS =================

    public static boolean isAdmin() {
        return isLoggedIn() && utilisateurConnecte.getRole() != null
                && utilisateurConnecte.getRole().name().equalsIgnoreCase("ADMIN");
    }
}