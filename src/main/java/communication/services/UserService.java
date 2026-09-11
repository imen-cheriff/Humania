package communication.services;

import communication.models.User;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * communication.services.UserService
 *
 * Queries the `utilisateur` table directly — no `users` table.
 * Returns communication.models.User objects (which extend Utilisateur),
 * so all controllers that declare "User user = ..." continue to compile.
 *
 * Requires: ALTER TABLE utilisateur
 *               ADD COLUMN is_online  BOOLEAN   DEFAULT FALSE,
 *               ADD COLUMN last_seen  TIMESTAMP NULL;
 */
public class UserService {

    private Connection cnx;

    public UserService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    public User findById(int userId) {
        String req = "SELECT * FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        } catch (SQLException e) {
            System.err.println("❌ UserService.findById: " + e.getMessage());
        }
        return null;
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String req = "SELECT * FROM utilisateur " +
                "WHERE (statut IS NULL OR UPPER(TRIM(statut)) != 'ARCHIVE')";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) users.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.err.println("❌ UserService.getAll: " + e.getMessage());
        }
        return users;
    }

    // ── findByUsername ────────────────────────────────────────────────────────

    public User findByUsername(String username) {
        String req = "SELECT * FROM utilisateur WHERE username = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1, username);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        } catch (SQLException e) {
            System.err.println("❌ UserService.findByUsername: " + e.getMessage());
        }
        return null;
    }

    // ── findByIds ─────────────────────────────────────────────────────────────

    public List<User> findByIds(List<Integer> userIds) {
        List<User> users = new ArrayList<>();
        if (userIds == null || userIds.isEmpty()) return users;

        String placeholders = String.join(",",
                userIds.stream().map(id -> "?").toArray(String[]::new));
        String req = "SELECT * FROM utilisateur WHERE id IN (" + placeholders + ")";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            for (int i = 0; i < userIds.size(); i++) pstm.setInt(i + 1, userIds.get(i));
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) users.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.err.println("❌ UserService.findByIds: " + e.getMessage());
        }
        return users;
    }

    // ── Presence ──────────────────────────────────────────────────────────────

    public void setOnline(int userId, boolean online) {
        String req = "UPDATE utilisateur SET is_online = ?, last_seen = NOW() WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setBoolean(1, online);
            pstm.setInt(2, userId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ UserService.setOnline: " + e.getMessage());
        }
    }

    public boolean isOnline(int userId) {
        String req = "SELECT is_online FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return rs.getBoolean("is_online");
        } catch (SQLException e) {
            System.err.println("❌ UserService.isOnline: " + e.getMessage());
        }
        return false;
    }

    public java.time.LocalDateTime getLastSeen(int userId) {
        String req = "SELECT last_seen FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_seen");
                return ts != null ? ts.toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            System.err.println("❌ UserService.getLastSeen: " + e.getMessage());
        }
        return null;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    /**
     * Maps one row from `utilisateur` to a communication.models.User.
     *
     * utilisateur column  →  User/Utilisateur field
     * ──────────────────────────────────────────────
     * nom                 →  setNom()
     * prenom              →  setPrenom()
     * pdp                 →  setPdp()   (== setAvatarUrl())
     * date_creation       →  setCreatedAt()
     * is_online           →  setOnline()    (graceful if column missing)
     * last_seen           →  setLastSeen()  (graceful if column missing)
     * role                →  setRole(String) — bridges String → Role enum
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setNumtel(rs.getString("numtel"));
        user.setPdp(rs.getString("pdp"));          // avatar / photo de profil
        user.setStatut(rs.getString("statut"));

        // Role: String from DB → Role enum (via User.setRole(String) bridge)
        user.setRole(rs.getString("role"));

        // date_creation → createdAt alias (getCreatedAt() defined in Utilisateur)
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) user.setCreatedAt(ts.toLocalDateTime());

        // Presence fields — graceful if migration not yet applied
        try {
            user.setOnline(rs.getBoolean("is_online"));
        } catch (SQLException ignored) {}

        try {
            Timestamp ls = rs.getTimestamp("last_seen");
            if (ls != null) user.setLastSeen(ls.toLocalDateTime());
        } catch (SQLException ignored) {}

        return user;
    }
}