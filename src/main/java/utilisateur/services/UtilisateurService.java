// ═══════════════════════════════════════════════════════════════════════
// FUSION COMPLÈTE: version propre + version ami
//
// ARCHITECTURE: Toutes les colonnes (y compris employé) sont dans la
//   table utilisateur (approche version ami — plus de tables employe/formateur).
//
// AJOUTS vs version ami     : méthodes présence setOnline/isOnline/getLastSeen,
//   insererAvecEmployeEtFormateur (garde la compatibilité API), mapRow lit
//   aussi is_online / last_seen, méthode inserer() privée managerExisteEtEstManager.
// AJOUTS vs version propre  : modifierAvecMotDePasse, executeQuery helper,
//   mapRow lit les colonnes employé, INSERT/UPDATE incluent colonnes employé.
// ═══════════════════════════════════════════════════════════════════════
package utilisateur.services;

import utilisateur.enums.Role;
import utilisateur.models.Utilisateur;
import utils.MyDataBase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService implements Service<Utilisateur> {

    private final Connection cnx;

    public UtilisateurService() {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // ================= PASSWORD HASHING =================

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // ================= LOGIN =================

    public Utilisateur login(String email, String motDePasse) throws SQLException {
        String sql = "SELECT * FROM utilisateur WHERE email = ? AND mot_de_passe = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);
        ps.setString(2, hashPassword(motDePasse));
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String statut = rs.getString("statut");
            if (!"actif".equalsIgnoreCase(statut)) throw new SQLException("Compte " + statut);
            return mapRow(rs);
        }
        return null;
    }

    // ================= PRÉSENCE (module communication) =================

    /** Marque l'utilisateur en ligne/hors ligne et met à jour last_seen. */
    public void setOnline(int userId, boolean online) {
        String sql = "UPDATE utilisateur SET is_online = ?, last_seen = NOW() WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBoolean(1, online);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ UtilisateurService.setOnline: " + e.getMessage());
        }
    }

    public boolean isOnline(int userId) {
        String sql = "SELECT is_online FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("is_online");
        } catch (SQLException e) {
            System.err.println("❌ UtilisateurService.isOnline: " + e.getMessage());
        }
        return false;
    }

    public LocalDateTime getLastSeen(int userId) {
        String sql = "SELECT last_seen FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_seen");
                return ts != null ? ts.toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            System.err.println("❌ UtilisateurService.getLastSeen: " + e.getMessage());
        }
        return null;
    }

    // ================= VÉRIFICATIONS =================

    public boolean emailExiste(String email) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM utilisateur WHERE email = ?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean emailExistePourAutre(int excludeUserId, String email) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM utilisateur WHERE email = ? AND id != ?");
        ps.setString(1, email);
        ps.setInt(2, excludeUserId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean usernameExiste(String username) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM utilisateur WHERE username = ?");
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean usernameExistePourAutre(int excludeId, String username) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM utilisateur WHERE username = ? AND id != ?");
        ps.setString(1, username);
        ps.setInt(2, excludeId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    // ================= INSERT =================

    public void inserer(Utilisateur u) throws SQLException {
        insererEtRetournerId(u);
    }

    /**
     * Insère un utilisateur (tous les champs y compris les colonnes employé)
     * et retourne la clé générée.
     */
    public int insererEtRetournerId(Utilisateur u) throws SQLException {
        String sql = """
            INSERT INTO utilisateur
              (nom, prenom, email, username, numtel, pdp,
               mot_de_passe, role, statut, date_creation, donnees_faciales,
               posteActuel, manager_id, matricule, dateEmbauche, departement)
            VALUES (?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?)
        """;
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1,  u.getNom());
        ps.setString(2,  u.getPrenom());
        ps.setString(3,  u.getEmail());
        ps.setString(4,  u.getUsername());
        ps.setString(5,  u.getNumtel());
        ps.setString(6,  u.getPdp());
        ps.setString(7,  hashPassword(u.getMotDePasse()));
        ps.setString(8,  u.getRole() != null ? u.getRole().name() : null);
        ps.setString(9,  u.getStatut() != null ? u.getStatut() : "Actif");
        ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
        ps.setBytes(11,  u.getDonneesFaciales());
        ps.setString(12, u.getPosteActuel());
        if (u.getManagerId() != null) ps.setInt(13, u.getManagerId());
        else                          ps.setNull(13, Types.INTEGER);
        ps.setString(14, u.getMatricule());
        if (u.getDateEmbauche() != null) ps.setDate(15, Date.valueOf(u.getDateEmbauche()));
        else                             ps.setNull(15, Types.DATE);
        ps.setString(16, u.getDepartement());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) return keys.getInt(1);
        throw new SQLException("Insert utilisateur failed — no ID returned.");
    }

    /**
     * Compatibilité avec l'ancien code qui appelait insererAvecEmployeEtFormateur.
     * Désormais tout est dans la table utilisateur, mais l'API reste identique.
     */
    public int insererAvecEmployeEtFormateur(Utilisateur u, Integer managerId,
                                             String specialiteFormateur) throws SQLException {
        return insererAvecEmployeEtFormateur(u, managerId, specialiteFormateur, null, null, null);
    }

    public int insererAvecEmployeEtFormateur(Utilisateur u, Integer managerId,
                                             String specialiteFormateur,
                                             String matricule, String posteActuel,
                                             String departement) throws SQLException {
        u.setManagerId(managerId);
        if (matricule   != null) u.setMatricule(matricule);
        if (posteActuel != null) u.setPosteActuel(posteActuel);
        if (departement != null) u.setDepartement(departement);
        return insererEtRetournerId(u);
    }

    // ================= CONVERSION candidat → utilisateur =================

    public ConversionResult convertirCandidatEnUtilisateur(Utilisateur u, int candidatureId,
                                                           String candidatEmail,
                                                           Integer managerId, String specialite,
                                                           String matricule, String posteActuel,
                                                           String departement) throws SQLException {
        if (candidatEmail == null || candidatEmail.isBlank())
            throw new SQLException("L'email du candidat est requis pour créer le compte.");
        if (emailExiste(candidatEmail))
            throw new SQLException("Un compte existe déjà avec l'adresse : " + candidatEmail);

        String username = u.getUsername();
        if (username == null || username.isBlank() || usernameExiste(username))
            username = genererUsernameUnique(u.getNom(), u.getPrenom());
        u.setUsername(username);

        u.setEmail(candidatEmail);
        u.setManagerId(managerId);
        u.setMatricule(matricule   != null && !matricule.isBlank()   ? matricule   : null);
        u.setPosteActuel(posteActuel != null && !posteActuel.isBlank() ? posteActuel : null);
        u.setDepartement(departement != null && !departement.isBlank() ? departement : null);

        String rawPassword = genererMotDePasseSecurise();
        u.setMotDePasse(rawPassword);

        boolean orig = cnx.getAutoCommit();
        cnx.setAutoCommit(false);
        try {
            if (managerId != null && !managerExiste(managerId))
                throw new SQLException("Manager invalide: id=" + managerId);
            int userId = insererEtRetournerId(u);
            PreparedStatement psC = cnx.prepareStatement(
                    "UPDATE candidature SET employe_id = ? WHERE id = ?");
            psC.setInt(1, userId);
            psC.setInt(2, candidatureId);
            psC.executeUpdate();
            cnx.commit();
            return new ConversionResult(userId, rawPassword, candidatEmail);
        } catch (SQLException | RuntimeException ex) {
            cnx.rollback();
            throw ex;
        } finally {
            cnx.setAutoCommit(orig);
        }
    }

    public record ConversionResult(int userId, String rawPassword, String loginEmail) {}

    // ================= UPDATE =================

    /**
     * Mise à jour complète (tous les champs sauf le mot de passe).
     */
    public void modifier(Utilisateur u) throws SQLException {
        String sql = """
            UPDATE utilisateur SET
              nom=?, prenom=?, email=?, username=?, numtel=?, pdp=?,
              role=?, statut=?,
              posteActuel=?, manager_id=?, matricule=?, dateEmbauche=?, departement=?
            WHERE id=?
        """;
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1,  u.getNom());
        ps.setString(2,  u.getPrenom());
        ps.setString(3,  u.getEmail());
        ps.setString(4,  u.getUsername());
        ps.setString(5,  u.getNumtel());
        ps.setString(6,  u.getPdp());
        ps.setString(7,  u.getRole() != null ? u.getRole().name() : null);
        ps.setString(8,  u.getStatut());
        ps.setString(9,  u.getPosteActuel());
        if (u.getManagerId() != null) ps.setInt(10, u.getManagerId());
        else                          ps.setNull(10, Types.INTEGER);
        ps.setString(11, u.getMatricule());
        if (u.getDateEmbauche() != null) ps.setDate(12, Date.valueOf(u.getDateEmbauche()));
        else                             ps.setNull(12, Types.DATE);
        ps.setString(13, u.getDepartement());
        ps.setInt(14,    u.getId());
        ps.executeUpdate();
    }

    /** Mise à jour avec changement de mot de passe optionnel. */
    public void modifierAvecMotDePasse(Utilisateur u, String nouveauMotDePasse) throws SQLException {
        modifier(u);
        if (nouveauMotDePasse != null && !nouveauMotDePasse.trim().isEmpty()) {
            PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE utilisateur SET mot_de_passe=? WHERE id=?");
            ps.setString(1, hashPassword(nouveauMotDePasse));
            ps.setInt(2, u.getId());
            ps.executeUpdate();
        }
    }

    /** Page profil : mise à jour des champs personnels + mot de passe optionnel. */
    public void modifierProfil(Utilisateur u, String nouveauMotDePasse) throws SQLException {
        if (nouveauMotDePasse != null && !nouveauMotDePasse.trim().isEmpty()) {
            PreparedStatement ps = cnx.prepareStatement("""
                UPDATE utilisateur SET
                  nom=?, prenom=?, email=?, username=?, numtel=?, pdp=?, mot_de_passe=?
                WHERE id=?
            """);
            ps.setString(1, u.getNom());   ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail()); ps.setString(4, u.getUsername());
            ps.setString(5, u.getNumtel()); ps.setString(6, u.getPdp());
            ps.setString(7, hashPassword(nouveauMotDePasse)); ps.setInt(8, u.getId());
            ps.executeUpdate();
        } else {
            PreparedStatement ps = cnx.prepareStatement("""
                UPDATE utilisateur SET
                  nom=?, prenom=?, email=?, username=?, numtel=?, pdp=?
                WHERE id=?
            """);
            ps.setString(1, u.getNom());   ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail()); ps.setString(4, u.getUsername());
            ps.setString(5, u.getNumtel()); ps.setString(6, u.getPdp());
            ps.setInt(7, u.getId());
            ps.executeUpdate();
        }
    }

    public void mettreAJourEmail(int userId, String email) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET email=? WHERE id=?");
        ps.setString(1, email); ps.setInt(2, userId);
        ps.executeUpdate();
    }

    // ================= DELETE / ARCHIVE =================

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM utilisateur WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void archiverUtilisateur(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET statut='Archive' WHERE id=?");
        ps.setInt(1, id); ps.executeUpdate();
    }

    public void restaurerUtilisateur(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET statut='Actif' WHERE id=?");
        ps.setInt(1, id); ps.executeUpdate();
    }

    // ================= READ =================

    public List<Utilisateur> recupererTous() throws SQLException {
        return executeQuery("SELECT * FROM utilisateur");
    }

    public List<Utilisateur> recupererActifs() throws SQLException {
        return executeQuery(
                "SELECT * FROM utilisateur WHERE (statut IS NULL OR UPPER(TRIM(statut)) != 'ARCHIVE')");
    }

    public List<Utilisateur> recupererArchives() throws SQLException {
        return executeQuery("SELECT * FROM utilisateur WHERE UPPER(TRIM(statut)) = 'ARCHIVE'");
    }

    private List<Utilisateur> executeQuery(String sql) throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ================= RECONNAISSANCE FACIALE =================

    public List<Utilisateur> getUtilisateursAvecDonneesFaciales() throws SQLException {
        return executeQuery("SELECT * FROM utilisateur WHERE donnees_faciales IS NOT NULL");
    }

    public void mettreAJourDonneesFaciales(int id, byte[] donnees) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET donnees_faciales=? WHERE id=?");
        ps.setBytes(1, donnees); ps.setInt(2, id);
        ps.executeUpdate();
    }

    // ================= MAPPING =================

    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setUsername(rs.getString("username"));
        u.setNumtel(rs.getString("numtel"));
        u.setPdp(rs.getString("pdp"));
        u.setMotDePasse(rs.getString("mot_de_passe"));

        String dbRole = rs.getString("role");
        if (dbRole != null) {
            try { u.setRole(Role.valueOf(dbRole)); }
            catch (IllegalArgumentException e) { u.setRole(Role.EMPLOYE); }
        }

        u.setStatut(rs.getString("statut"));
        Timestamp ts = rs.getTimestamp("date_creation");
        u.setDateCreation(ts != null ? ts.toLocalDateTime() : null);
        u.setDonneesFaciales(rs.getBytes("donnees_faciales"));

        // MFA (gracieux si colonnes absentes)
        try {
            u.setMfaSecret(rs.getString("mfa_secret"));
            u.setMfaEnabled(rs.getInt("mfa_enabled") == 1);
        } catch (SQLException ignored) {}

        // Champs employé (gracieux si colonnes absentes)
        try { u.setPosteActuel(rs.getString("posteActuel")); }          catch (SQLException ignored) {}
        try {
            int mid = rs.getInt("manager_id");
            u.setManagerId(rs.wasNull() ? null : mid);
        } catch (SQLException ignored) {}
        try { u.setMatricule(rs.getString("matricule")); }               catch (SQLException ignored) {}
        try {
            Date de = rs.getDate("dateEmbauche");
            u.setDateEmbauche(de != null ? de.toLocalDate() : null);
        } catch (SQLException ignored) {}
        try { u.setDepartement(rs.getString("departement")); }           catch (SQLException ignored) {}

        // Présence (gracieux si colonnes absentes)
        try { u.setOnline(rs.getBoolean("is_online")); }                 catch (SQLException ignored) {}
        try {
            Timestamp ls = rs.getTimestamp("last_seen");
            if (ls != null) u.setLastSeen(ls.toLocalDateTime());
        } catch (SQLException ignored) {}

        return u;
    }

    // ================= MFA =================

    public void activerMfa(int userId, String secret) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET mfa_secret=?, mfa_enabled=1 WHERE id=?");
        ps.setString(1, secret); ps.setInt(2, userId);
        ps.executeUpdate();
    }

    public void desactiverMfa(int userId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET mfa_secret=NULL, mfa_enabled=0 WHERE id=?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    // ================= MOT DE PASSE OUBLIÉ =================

    public void reinitialiserMotDePasse(String email, String newPassword) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET mot_de_passe=? WHERE email=?");
        ps.setString(1, hashPassword(newPassword)); ps.setString(2, email);
        ps.executeUpdate();
    }

    // ================= GOOGLE LOGIN =================

    public Utilisateur connecterViaGoogle(String email) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM utilisateur WHERE email=?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (!rs.next())
            throw new SQLException(
                    "Aucun compte associé à l'adresse : " + email
                            + "\nContactez votre administrateur pour obtenir un accès.");
        String statut = rs.getString("statut");
        if ("Archive".equalsIgnoreCase(statut))  throw new SQLException("Ce compte est archivé. Contactez l'administrateur.");
        if ("bloque".equalsIgnoreCase(statut))   throw new SQLException("Ce compte est bloqué. Contactez l'administrateur.");
        if ("suspendu".equalsIgnoreCase(statut)) throw new SQLException("Ce compte est suspendu. Contactez l'administrateur.");
        return mapRow(rs);
    }

    // ================= MANAGERS =================

    public List<Utilisateur> getManagers() throws SQLException {
        return executeQuery(
                "SELECT * FROM utilisateur WHERE role='MANAGER' "
                        + "AND UPPER(TRIM(COALESCE(statut,''))) != 'ARCHIVE' ORDER BY nom, prenom");
    }

    public void mettreAJourManagerEmploye(int utilisateurId, Integer managerId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE utilisateur SET manager_id=? WHERE id=?");
        if (managerId != null) ps.setInt(1, managerId);
        else                   ps.setNull(1, Types.INTEGER);
        ps.setInt(2, utilisateurId);
        ps.executeUpdate();
    }

    public Integer getManagerIdPourUtilisateur(int utilisateurId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT manager_id FROM utilisateur WHERE id=?");
        ps.setInt(1, utilisateurId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int mid = rs.getInt("manager_id");
            return rs.wasNull() ? null : mid;
        }
        return null;
    }

    // ================= GÉNÉRATION DE CREDENTIALS =================

    public String genererUsernameUnique(String nom, String prenom) throws SQLException {
        String base = (prenom + nom).toLowerCase().replaceAll("[^a-z0-9]", "");
        if (base.isEmpty()) base = "user";
        String candidate = base;
        int suffix = 1;
        while (usernameExiste(candidate)) { candidate = base + suffix++; }
        return candidate;
    }

    public String genererMotDePasseSecurise() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String symbols = "!@#$%^&*()-_=+[]{}";
        String all     = upper + lower + digits + symbols;
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(symbols.charAt(rnd.nextInt(symbols.length())));
        for (int i = sb.length(); i < 12; i++) sb.append(all.charAt(rnd.nextInt(all.length())));
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char t = chars[i]; chars[i] = chars[j]; chars[j] = t;
        }
        return new String(chars);
    }

    // ================= HELPERS PRIVÉS =================

    private boolean managerExiste(int managerId) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT role FROM utilisateur WHERE id=?");
        ps.setInt(1, managerId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return false;
        return "MANAGER".equalsIgnoreCase(rs.getString("role"));
    }
}