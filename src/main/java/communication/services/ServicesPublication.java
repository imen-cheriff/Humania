package communication.services;

import communication.interfaces.Service;
import communication.models.Publication;
import communication.models.enums.Statut;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServicesPublication implements Service<Publication> {

    private Connection cnx;

    public ServicesPublication() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Publication publication) {
        String req = "INSERT INTO PUBLICATION (contenu, authorId, dateCreation, statut, imageUrl, sharedFromId, shareMessage, visibility, groupId, nombreCommentaires, nombreReactions) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, publication.getContenu());
            pstm.setInt(2, publication.getAuthorId());
            pstm.setTimestamp(3, Timestamp.valueOf(publication.getDateCreation()));
            pstm.setString(4, publication.getStatut().name());
            pstm.setString(5, publication.getImageUrl());

            if (publication.getSharedFromId() != null) {
                pstm.setInt(6, publication.getSharedFromId());
            } else {
                pstm.setNull(6, Types.INTEGER);
            }

            pstm.setString(7, publication.getShareMessage());
            pstm.setString(8, publication.getVisibility());

            if (publication.getGroupId() != null) {
                pstm.setInt(9, publication.getGroupId());
            } else {
                pstm.setNull(9, Types.INTEGER);
            }

            pstm.setInt(10, publication.getNombreCommentaires());
            pstm.setInt(11, publication.getNombreReactions());

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                publication.setId(rs.getInt(1));
            }

            System.out.println("✅ Publication ajoutée : ID = " + publication.getId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout publication : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Publication> getAll() {
        List<Publication> publications = new ArrayList<>();
        String req = "SELECT * FROM PUBLICATION WHERE statut != 'SUPPRIME' ORDER BY dateCreation DESC";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Publication pub = mapResultSetToPublication(rs);
                publications.add(pub);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération publications : " + e.getMessage());
        }

        return publications;
    }

    @Override
    public void update(Publication publication) {
        String req = "UPDATE PUBLICATION SET contenu = ?, imageUrl = ?, dateModification = ?, statut = ?, shareMessage = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, publication.getContenu());
            pstm.setString(2, publication.getImageUrl());
            pstm.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstm.setString(4, publication.getStatut().name());
            pstm.setString(5, publication.getShareMessage());
            pstm.setInt(6, publication.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Publication mise à jour : ID = " + publication.getId());
            } else {
                System.out.println("⚠️ Aucune publication trouvée avec l'ID = " + publication.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour publication : " + e.getMessage());
        }
    }

    @Override
    public void delete(Publication publication) {
        // 1. Soft-delete all shared copies that reference this publication
        String reqShares = "UPDATE PUBLICATION SET statut = 'SUPPRIME' WHERE sharedFromId = ?";
        try {
            PreparedStatement pstmShares = this.cnx.prepareStatement(reqShares);
            pstmShares.setInt(1, publication.getId());
            int sharedDeleted = pstmShares.executeUpdate();
            if (sharedDeleted > 0) {
                System.out.println("✅ " + sharedDeleted + " publications partagées supprimées pour l'original ID = " + publication.getId());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression publications partagées : " + e.getMessage());
        }

        // 2. Delete all saved-post entries referencing this publication
        String reqSaved = "DELETE FROM SAVEDPOST WHERE publicationId = ?";
        try {
            PreparedStatement pstmSaved = this.cnx.prepareStatement(reqSaved);
            pstmSaved.setInt(1, publication.getId());
            int savedDeleted = pstmSaved.executeUpdate();
            if (savedDeleted > 0) {
                System.out.println("✅ " + savedDeleted + " sauvegardes supprimées pour la publication ID = " + publication.getId());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression sauvegardes : " + e.getMessage());
        }

        // 3. Also delete saved-post entries for shared copies of this publication
        String reqSavedShared = "DELETE FROM SAVEDPOST WHERE publicationId IN (SELECT id FROM PUBLICATION WHERE sharedFromId = ?)";
        try {
            PreparedStatement pstmSavedShared = this.cnx.prepareStatement(reqSavedShared);
            pstmSavedShared.setInt(1, publication.getId());
            int savedSharedDeleted = pstmSavedShared.executeUpdate();
            if (savedSharedDeleted > 0) {
                System.out.println("✅ " + savedSharedDeleted + " sauvegardes de publications partagées supprimées pour l'original ID = " + publication.getId());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression sauvegardes partagées : " + e.getMessage());
        }

        // 4. Soft-delete the original publication
        String req = "UPDATE PUBLICATION SET statut = 'SUPPRIME' WHERE id = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publication.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Publication supprimée (soft delete) : ID = " + publication.getId());
            } else {
                System.out.println("⚠️ Aucune publication trouvée avec l'ID = " + publication.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression publication : " + e.getMessage());
        }
    }

    // ========== MÉTHODES SPÉCIALISÉES ==========

    public Publication findById(int id) {
        String req = "SELECT * FROM PUBLICATION WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToPublication(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findById : " + e.getMessage());
        }

        return null;
    }

    public List<Publication> findByAuthorId(int authorId) {
        List<Publication> publications = new ArrayList<>();
        String req = "SELECT * FROM PUBLICATION WHERE authorId = ? AND statut != 'SUPPRIME' ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, authorId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Publication pub = mapResultSetToPublication(rs);
                publications.add(pub);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche par auteur : " + e.getMessage());
        }

        return publications;
    }

    public List<Publication> findByGroupId(int groupId) {
        List<Publication> publications = new ArrayList<>();
        String req = "SELECT * FROM PUBLICATION WHERE groupId = ? AND visibility = 'GROUP' AND statut != 'SUPPRIME' ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                publications.add(mapResultSetToPublication(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche publications groupe : " + e.getMessage());
        }

        return publications;
    }

    public int countShares(int publicationId) {
        String req = "SELECT COUNT(*) as count FROM PUBLICATION WHERE sharedFromId = ? AND statut != 'SUPPRIME'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur countShares : " + e.getMessage());
        }

        return 0;
    }

    public void incrementComments(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreCommentaires = nombreCommentaires + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur incrémentation commentaires : " + e.getMessage());
        }
    }

    public void decrementComments(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreCommentaires = nombreCommentaires - 1 WHERE id = ? AND nombreCommentaires > 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur décrémentation commentaires : " + e.getMessage());
        }
    }

    public void incrementReactions(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreReactions = nombreReactions + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur incrémentation réactions : " + e.getMessage());
        }
    }

    public void decrementReactions(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreReactions = nombreReactions - 1 WHERE id = ? AND nombreReactions > 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur décrémentation réactions : " + e.getMessage());
        }
    }
    public int countByAuthorId(int authorId) {
        String req = "SELECT COUNT(*) as count FROM PUBLICATION WHERE authorId = ? AND statut != 'SUPPRIME'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, authorId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur countByAuthorId : " + e.getMessage());
        }

        return 0;
    }
    // ========== MÉTHODE PRIVÉE ==========

    private Publication mapResultSetToPublication(ResultSet rs) throws SQLException {
        Publication pub = new Publication();
        pub.setId(rs.getInt("id"));
        pub.setContenu(rs.getString("contenu"));
        pub.setImageUrl(rs.getString("imageUrl"));
        pub.setAuthorId(rs.getInt("authorId"));
        pub.setDateCreation(rs.getTimestamp("dateCreation").toLocalDateTime());

        Timestamp dateModification = rs.getTimestamp("dateModification");
        if (dateModification != null) {
            pub.setDateModification(dateModification.toLocalDateTime());
        }

        pub.setNombreCommentaires(rs.getInt("nombreCommentaires"));
        pub.setNombreReactions(rs.getInt("nombreReactions"));
        pub.setStatut(Statut.valueOf(rs.getString("statut")));
        pub.setSharedFromId(rs.getObject("sharedFromId", Integer.class));
        pub.setShareMessage(rs.getString("shareMessage"));
        pub.setVisibility(rs.getString("visibility"));
        pub.setGroupId(rs.getObject("groupId", Integer.class));

        return pub;
    }
}