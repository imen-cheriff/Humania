package utilisateur.services;

import utilisateur.enums.Role;
import utilisateur.models.Utilisateur;
import utils.MyDataBase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;

public class ServiceAdmin {

    private final Connection connection;

    public ServiceAdmin() {
        this.connection = MyDataBase.getInstance().getCnx();
    }

    /** Hash password using SHA-256 */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String hexStr = Integer.toHexString(0xff & b);
                if (hexStr.length() == 1) hex.append('0');
                hex.append(hexStr);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /** Initialize default admin if it doesn't exist */
    public void initializeAdmin() {
        String checkQuery = "SELECT COUNT(*) FROM utilisateur WHERE username = 'admin'";

        try (PreparedStatement ps = connection.prepareStatement(checkQuery);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next() && rs.getInt(1) == 0) {
                // admin doesn't exist → create
                createDefaultAdmin();
                System.out.println("✓ Default admin created successfully!");
                System.out.println("  Username: admin");
                System.out.println("  Password: admin123");
            } else {
                System.out.println("✓ Admin account already exists.");
            }

        } catch (SQLException e) {
            System.err.println("✗ Error checking/creating admin account!");
            e.printStackTrace();
        }
    }

    /** Create the default admin account */
    private void createDefaultAdmin() throws SQLException {
        String insertQuery = """
            INSERT INTO utilisateur
            (nom, prenom, email, username, numtel, pdp, mot_de_passe, role, statut, date_creation, donnees_faciales)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
            ps.setString(1, "Admin");
            ps.setString(2, "System");
            ps.setString(3, "admin@humania.tn");
            ps.setString(4, "admin");
            ps.setString(5, "00000000"); // default phone
            ps.setString(6, ""); // default profile pic
            ps.setString(7, hashPassword("admin123"));
            ps.setString(8, Role.ADMIN.name());
            ps.setString(9, "Actif");
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBytes(11, null); // no facial data

            ps.executeUpdate();
        }
    }

    /** Get admin by username */
    public Utilisateur getAdminByUsername(String username) throws SQLException {
        String query = "SELECT * FROM utilisateur WHERE username = ? AND role = 'ADMIN'";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Utilisateur u = new Utilisateur();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setUsername(rs.getString("username"));
                u.setNumtel(rs.getString("numtel"));
                u.setPdp(rs.getString("pdp"));
                u.setMotDePasse(rs.getString("mot_de_passe"));
                u.setRole(Role.valueOf(rs.getString("role")));
                u.setStatut(rs.getString("statut"));
                Timestamp ts = rs.getTimestamp("date_creation");
                u.setDateCreation(ts != null ? ts.toLocalDateTime() : null);
                u.setDonneesFaciales(rs.getBytes("donnees_faciales"));
                return u;
            }
        }
        return null;
    }

    /** Verify admin login */
    public boolean verifyLogin(String username, String password) throws SQLException {
        String query = "SELECT mot_de_passe FROM utilisateur WHERE username = ? AND role = 'ADMIN'";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("mot_de_passe");
                return storedHash.equals(hashPassword(password));
            }
        }
        return false;
    }
}