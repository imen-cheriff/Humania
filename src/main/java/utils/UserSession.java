package utils;

/**
 * utils.UserSession
 *
 * CHANGE vs previous version:
 *   syncFromUtilisateur() now also copies the user's id → setUserId().
 *   Without this, UserSession.getInstance().getUserId() returned -1,
 *   causing all communication controllers (FeedController, ChatController,
 *   PostCardController, NetworkController…) to load no data.
 *
 *   LoginController already calls:
 *       UserSession.getInstance().syncFromUtilisateur(u);
 *   so NO change is needed in LoginController or anywhere else.
 */
public class UserSession {

    private static UserSession instance;

    private int    userId   = -1;  // DB primary key from utilisateur.id
    private String username;
    private String role;           // "ADMIN", "MANAGER", "EMPLOYE", …
    private String user     = null;
    private String email    = null;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getEmail()           { return email; }
    public void   setEmail(String e)   { this.email = e; }

    /** Returns display name (prenom + nom) or username as fallback. */
    public String getUser()            { return user != null ? user : username; }
    public void   setUser(String u)    { this.user = u; this.username = u; }

    public int    getUserId()          { return userId; }
    public void   setUserId(int id)    { this.userId = id; }

    public void   setRole(String r)    { this.role = r; }
    public String getUsername()        { return username; }
    public String getRole()            { return role != null ? role : "EMPLOYE"; }

    public boolean isAdmin()   { return "ADMIN".equals(role); }
    public boolean isManager() { return "MANAGER".equals(role); }

    public void clearSession() {
        username = null;
        user     = null;
        email    = null;
        role     = null;
        userId   = -1;
    }

    // ── syncFromUtilisateur ───────────────────────────────────────────────────

    /**
     * Populates UserSession from any Utilisateur object after login.
     * Uses reflection to stay decoupled from the model package.
     *
     * Called by LoginController (standard + Google + face login) — no change needed there.
     *
     * FIX: now also copies the user's id so that
     *      UserSession.getInstance().getUserId()
     *      returns the real DB id in all communication controllers.
     */
    public static void syncFromUtilisateur(Object utilisateur) {
        if (utilisateur == null) return;
        try {
            Class<?> cls = utilisateur.getClass();

            // ── id (FIX: was missing — caused getUserId() == -1) ─────────────
            try {
                int id = (int) cls.getMethod("getId").invoke(utilisateur);
                getInstance().setUserId(id);
            } catch (Exception ignored) {}

            // ── displayName = prénom + " " + nom ─────────────────────────────
            String prenom = "", nom = "";
            try { prenom = (String) cls.getMethod("getPrenom").invoke(utilisateur); }
            catch (Exception ignored) {}
            try { nom    = (String) cls.getMethod("getNom").invoke(utilisateur); }
            catch (Exception ignored) {}
            String displayName = (prenom + " " + nom).trim();
            if (displayName.isEmpty()) {
                try { displayName = (String) cls.getMethod("getEmail").invoke(utilisateur); }
                catch (Exception ignored) {}
            }
            if (displayName != null && !displayName.isBlank())
                getInstance().setUser(displayName);

            // ── username ──────────────────────────────────────────────────────
            try {
                String uname = (String) cls.getMethod("getUsername").invoke(utilisateur);
                if (uname != null && !uname.isBlank()) getInstance().username = uname;
            } catch (Exception ignored) {}

            // ── email ─────────────────────────────────────────────────────────
            try {
                String em = (String) cls.getMethod("getEmail").invoke(utilisateur);
                if (em != null && !em.isBlank()) getInstance().setEmail(em);
            } catch (Exception ignored) {}

            // ── role (Role enum → name string) ────────────────────────────────
            try {
                Object role = cls.getMethod("getRole").invoke(utilisateur);
                if (role != null) getInstance().setRole(role.toString());
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("UserSession.syncFromUtilisateur: " + e.getMessage());
        }
    }
}