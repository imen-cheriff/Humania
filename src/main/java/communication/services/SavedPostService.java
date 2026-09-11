package communication.services;

import communication.interfaces.Service;
import communication.models.SavedPost;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavedPostService implements Service<SavedPost> {

    private Connection cnx;

    public SavedPostService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(SavedPost savedPost) {
        String req = "INSERT INTO SAVEDPOST (userId, publicationId, savedAt) VALUES (?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setInt(1, savedPost.getUserId());
            pstm.setInt(2, savedPost.getPublicationId());
            pstm.setTimestamp(3, Timestamp.valueOf(savedPost.getSavedAt()));

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                savedPost.setId(rs.getInt(1));
            }

            System.out.println("✅ Publication sauvegardée avec succès : ID = " + savedPost.getId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    @Override
    public List<SavedPost> getAll() {
        List<SavedPost> savedPosts = new ArrayList<>();
        String req = "SELECT * FROM SAVEDPOST ORDER BY savedAt DESC";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                SavedPost savedPost = mapResultSetToSavedPost(rs);
                savedPosts.add(savedPost);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération : " + e.getMessage());
        }

        return savedPosts;
    }

    @Override
    public void update(SavedPost savedPost) {
        System.out.println("⚠️ Update non applicable pour SavedPost");
    }

    @Override
    public void delete(SavedPost savedPost) {
        String req = "DELETE FROM SAVEDPOST WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, savedPost.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Sauvegarde supprimée : ID = " + savedPost.getId());
            } else {
                System.out.println("⚠️ Aucune sauvegarde trouvée avec l'ID = " + savedPost.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    // ========== MÉTHODES SPÉCIALISÉES ==========

    public SavedPost findById(int id) {
        String req = "SELECT * FROM SAVEDPOST WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToSavedPost(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
        }

        return null;
    }

    public List<SavedPost> findByUserId(int userId) {
        List<SavedPost> savedPosts = new ArrayList<>();
        String req = "SELECT * FROM SAVEDPOST WHERE userId = ? ORDER BY savedAt DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                SavedPost savedPost = mapResultSetToSavedPost(rs);
                savedPosts.add(savedPost);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par user : " + e.getMessage());
        }

        return savedPosts;
    }

    public boolean isPostSavedByUser(int userId, int publicationId) {
        String req = "SELECT COUNT(*) as count FROM SAVEDPOST WHERE userId = ? AND publicationId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            pstm.setInt(2, publicationId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification : " + e.getMessage());
        }

        return false;
    }

    public void deleteByUserAndPublication(int userId, int publicationId) {
        String req = "DELETE FROM SAVEDPOST WHERE userId = ? AND publicationId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            pstm.setInt(2, publicationId);

            pstm.executeUpdate();
            System.out.println("✅ Sauvegarde supprimée");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    // ========== MÉTHODES PRIVÉES ==========

    private SavedPost mapResultSetToSavedPost(ResultSet rs) throws SQLException {
        SavedPost savedPost = new SavedPost();
        savedPost.setId(rs.getInt("id"));
        savedPost.setUserId(rs.getInt("userId"));
        savedPost.setPublicationId(rs.getInt("publicationId"));
        savedPost.setSavedAt(rs.getTimestamp("savedAt").toLocalDateTime());
        return savedPost;
    }
}