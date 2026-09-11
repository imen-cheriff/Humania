package communication.services;

import communication.interfaces.Service;
import communication.models.Share;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShareService implements Service<Share> {

    private Connection cnx;

    public ShareService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Share share) {
        String req = "INSERT INTO SHARE (userId, publicationId, sharedAt, sharedMessage, shareCount) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setInt(1, share.getUserId());
            pstm.setInt(2, share.getPublicationId());
            pstm.setTimestamp(3, Timestamp.valueOf(share.getSharedAt()));
            pstm.setString(4, share.getSharedMessage());
            pstm.setInt(5, share.getShareCount());

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                share.setId(rs.getInt(1));
            }

            System.out.println("✅ Publication partagée avec succès : ID = " + share.getId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du partage : " + e.getMessage());
        }
    }

    @Override
    public List<Share> getAll() {
        List<Share> shares = new ArrayList<>();
        String req = "SELECT * FROM SHARE ORDER BY sharedAt DESC";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Share share = mapResultSetToShare(rs);
                shares.add(share);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération : " + e.getMessage());
        }

        return shares;
    }

    @Override
    public void update(Share share) {
        String req = "UPDATE SHARE SET sharedMessage = ?, shareCount = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, share.getSharedMessage());
            pstm.setInt(2, share.getShareCount());
            pstm.setInt(3, share.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Partage mis à jour : ID = " + share.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    @Override
    public void delete(Share share) {
        String req = "DELETE FROM SHARE WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, share.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Partage supprimé : ID = " + share.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    // ========== MÉTHODES SPÉCIALISÉES ==========

    public Share findById(int id) {
        String req = "SELECT * FROM SHARE WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToShare(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
        }

        return null;
    }

    public List<Share> findByUserId(int userId) {
        List<Share> shares = new ArrayList<>();
        String req = "SELECT * FROM SHARE WHERE userId = ? ORDER BY sharedAt DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Share share = mapResultSetToShare(rs);
                shares.add(share);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par user : " + e.getMessage());
        }

        return shares;
    }

    public List<Share> findByPublicationId(int publicationId) {
        List<Share> shares = new ArrayList<>();
        String req = "SELECT * FROM SHARE WHERE publicationId = ? ORDER BY sharedAt DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Share share = mapResultSetToShare(rs);
                shares.add(share);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par publication : " + e.getMessage());
        }

        return shares;
    }

    public int countSharesByPublication(int publicationId) {
        String req = "SELECT COUNT(*) as count FROM SHARE WHERE publicationId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage : " + e.getMessage());
        }

        return 0;
    }

    public void incrementShareCount(int shareId) {
        String req = "UPDATE SHARE SET shareCount = shareCount + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, shareId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'incrémentation : " + e.getMessage());
        }
    }

    // ========== MÉTHODES PRIVÉES ==========

    private Share mapResultSetToShare(ResultSet rs) throws SQLException {
        Share share = new Share();
        share.setId(rs.getInt("id"));
        share.setUserId(rs.getInt("userId"));
        share.setPublicationId(rs.getInt("publicationId"));
        share.setSharedAt(rs.getTimestamp("sharedAt").toLocalDateTime());
        share.setSharedMessage(rs.getString("sharedMessage"));
        share.setShareCount(rs.getInt("shareCount"));
        return share;
    }
}