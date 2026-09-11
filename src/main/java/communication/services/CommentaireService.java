package communication.services;

import communication.interfaces.Service;
import communication.models.Commentaire;
import communication.models.enums.Statut;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService implements Service<Commentaire> {

    private Connection cnx;

    public CommentaireService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /**
     * Ajouter un commentaire
     */
    @Override
    public void add(Commentaire commentaire) {
        String req = "INSERT INTO COMMENTAIRE (contenu, dateCreation, publicationId, authorId, nombreReactions, statut, gifUrl) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, commentaire.getContenu());
            pstm.setTimestamp(2, Timestamp.valueOf(commentaire.getDateCreation()));
            pstm.setInt(3, commentaire.getPublicationId());
            pstm.setInt(4, commentaire.getAuthorId());
            pstm.setInt(5, commentaire.getNombreReactions());
            pstm.setString(6, commentaire.getStatut().name());
            pstm.setString(7, commentaire.getGifUrl()); //

            pstm.executeUpdate();

            // Récupérer l'ID généré
            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                commentaire.setId(rs.getInt(1));
            }

            System.out.println("✅ Commentaire ajouté avec succès : ID = " + commentaire.getId());

            // Incrémenter le compteur de commentaires dans la publication
            incrementPublicationComments(commentaire.getPublicationId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout du commentaire : " + e.getMessage());
        }
    }

    /**
     * Récupérer tous les commentaires (non supprimés)
     */
    @Override
    public List<Commentaire> getAll() {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM COMMENTAIRE WHERE statut != 'SUPPRIME' ORDER BY dateCreation DESC";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Commentaire com = mapResultSetToCommentaire(rs);
                commentaires.add(com);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des commentaires : " + e.getMessage());
        }

        return commentaires;
    }

    /**
     * Mettre à jour un commentaire
     */
    @Override
    public void update(Commentaire commentaire) {
        String req = "UPDATE COMMENTAIRE SET contenu = ?, dateModification = ?, nombreReactions = ?, statut = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, commentaire.getContenu());
            pstm.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstm.setInt(3, commentaire.getNombreReactions());
            pstm.setString(4, commentaire.getStatut().name());
            pstm.setInt(5, commentaire.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Commentaire mis à jour : ID = " + commentaire.getId());
            } else {
                System.out.println("⚠️ Aucun commentaire trouvé avec l'ID = " + commentaire.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour du commentaire : " + e.getMessage());
        }
    }

    /**
     * Supprimer un commentaire (soft delete)
     */
    @Override
    public void delete(Commentaire commentaire) {
        String req = "UPDATE COMMENTAIRE SET statut = 'SUPPRIME' WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, commentaire.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Commentaire supprimé (soft delete) : ID = " + commentaire.getId());

                // Décrémenter le compteur de commentaires dans la publication
                decrementPublicationComments(commentaire.getPublicationId());
            } else {
                System.out.println("⚠️ Aucun commentaire trouvé avec l'ID = " + commentaire.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression du commentaire : " + e.getMessage());
        }
    }

    // ========== MÉTHODES SPÉCIALISÉES ==========

    /**
     * Trouver tous les commentaires d'une publication
     */
    public List<Commentaire> findByPublicationId(int publicationId) {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM COMMENTAIRE WHERE publicationId = ? AND statut != 'SUPPRIME' ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Commentaire com = mapResultSetToCommentaire(rs);
                commentaires.add(com);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par publication : " + e.getMessage());
        }

        return commentaires;
    }

    /**
     * Trouver tous les commentaires d'un auteur
     */
    public List<Commentaire> findByAuthorId(int authorId) {
        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM COMMENTAIRE WHERE authorId = ? AND statut != 'SUPPRIME' ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, authorId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Commentaire com = mapResultSetToCommentaire(rs);
                commentaires.add(com);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par auteur : " + e.getMessage());
        }

        return commentaires;
    }

    /**
     * Trouver un commentaire par ID
     */
    public Commentaire findById(int id) {
        String req = "SELECT * FROM COMMENTAIRE WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToCommentaire(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par ID : " + e.getMessage());
        }

        return null;
    }

    /**
     * Incrémenter le nombre de réactions d'un commentaire
     */
    public void incrementReactions(int commentaireId) {
        String req = "UPDATE COMMENTAIRE SET nombreReactions = nombreReactions + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, commentaireId);
            pstm.executeUpdate();

            System.out.println("✅ Réactions du commentaire incrémentées : ID = " + commentaireId);

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'incrémentation des réactions : " + e.getMessage());
        }
    }



    /**
     * Mapper un ResultSet vers un objet Commentaire
     */
    private Commentaire mapResultSetToCommentaire(ResultSet rs) throws SQLException {
        Commentaire com = new Commentaire();
        com.setId(rs.getInt("id"));
        com.setContenu(rs.getString("contenu"));
        com.setDateCreation(rs.getTimestamp("dateCreation").toLocalDateTime());

        // Récupérer dateModification
        Timestamp dateModification = rs.getTimestamp("dateModification");
        if (dateModification != null) {
            com.setDateModification(dateModification.toLocalDateTime());
        }

        com.setPublicationId(rs.getInt("publicationId"));
        com.setAuthorId(rs.getInt("authorId"));
        com.setNombreReactions(rs.getInt("nombreReactions"));
        com.setStatut(Statut.valueOf(rs.getString("statut")));
        try { com.setGifUrl(rs.getString("gifUrl")); } catch (Exception ignored) {}
        return com;

    }

    /**
     * Incrémenter le compteur de commentaires dans la publication
     */
    private void incrementPublicationComments(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreCommentaires = nombreCommentaires + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'incrémentation des commentaires : " + e.getMessage());
        }
    }

    /**
     * Décrémenter le compteur de commentaires dans la publication
     */
    private void decrementPublicationComments(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreCommentaires = nombreCommentaires - 1 WHERE id = ? AND nombreCommentaires > 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la décrémentation des commentaires : " + e.getMessage());
        }
    }
}