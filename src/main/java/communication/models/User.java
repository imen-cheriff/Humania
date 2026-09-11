package communication.models;

import utilisateur.models.Utilisateur;

/**
 * communication.models.User
 *
 * Thin adapter over utilisateur.models.Utilisateur.
 * ALL data comes from the `utilisateur` table — there is no separate `users` table.
 *
 * This class exists only so that communication controllers keep their
 * "User user = userService.findById(...)" declarations unchanged.
 *
 * Extra methods added for the communication layer:
 *   getBio()        → null stub  (bio lives in UserProfile, not utilisateur)
 *   getRoleString() → alias for getRoleAsString()
 */
public class User extends Utilisateur {

    public User() {
        super();
    }

    // ── setRole(String) bridge ────────────────────────────────────────────────
    // UserService reads role as a String from DB and calls this.
    public void setRole(String roleStr) {
        if (roleStr == null) return;
        try {
            super.setRole(utilisateur.enums.Role.valueOf(roleStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            super.setRole(utilisateur.enums.Role.EMPLOYE);
        }
    }

    // ── Bio stub ──────────────────────────────────────────────────────────────
    // Bio lives in UserProfile. Returns null so controllers that do
    //   (profile.getBio() != null ? profile.getBio() : profileUser.getBio())
    // fall through to the UserProfile value gracefully.
    public String getBio() {
        return null;
    }

    // ── getRoleString() alias ─────────────────────────────────────────────────
    // UserProfileController calls getRoleString(); Utilisateur has getRoleAsString().
    public String getRoleString() {
        return getRoleAsString();
    }

    // ── Legacy English aliases (kept for backward compat) ────────────────────
    public String getFirstName()        { return getPrenom(); }
    public void   setFirstName(String v){ setPrenom(v); }
    public String getLastName()         { return getNom(); }
    public void   setLastName(String v) { setNom(v); }
}