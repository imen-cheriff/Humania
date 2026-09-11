package communication.services;

import communication.interfaces.Service;
import communication.models.Reaction;
import communication.models.enums.TypeReaction;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReactionService implements Service<Reaction> {

    private Connection cnx;

    public ReactionService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Reaction reaction) {
        // Validation: une seule cible doit être non-null
        if (!reaction.isValide()) {
            System.err.println("❌ Erreur: La réaction doit cibler une publication OU un commentaire!");
            return;
        }

        String req = "INSERT INTO REACTION (type, dateCreation, userId, publicationId, commentaireId) " +
                "VALUES (?, NOW(), ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, reaction.getType().name());
            pstm.setInt(2, reaction.getUserId());

            if (reaction.getPublicationId() != null) {
                pstm.setInt(3, reaction.getPublicationId());
            } else {
                pstm.setNull(3, Types.INTEGER);
            }

            if (reaction.getCommentaireId() != null) {
                pstm.setInt(4, reaction.getCommentaireId());
            } else {
                pstm.setNull(4, Types.INTEGER);
            }

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                reaction.setId(rs.getInt(1));
            }

            System.out.println("✅ Réaction ajoutée avec succès : ID = " + reaction.getId());

            // Incrémenter les compteurs
            if (reaction.getPublicationId() != null) {
                incrementPublicationReactions(reaction.getPublicationId());
            } else if (reaction.getCommentaireId() != null) {
                incrementCommentaireReactions(reaction.getCommentaireId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout de la réaction : " + e.getMessage());
        }
    }

    @Override
    public List<Reaction> getAll() {
        List<Reaction> reactions = new ArrayList<>();
        String req = "SELECT * FROM REACTION ORDER BY dateCreation DESC";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Reaction reaction = mapResultSetToReaction(rs);
                reactions.add(reaction);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des réactions : " + e.getMessage());
        }

        return reactions;
    }

    @Override
    public void update(Reaction reaction) {
        String req = "UPDATE REACTION SET type = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, reaction.getType().name());
            pstm.setInt(2, reaction.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Réaction mise à jour : ID = " + reaction.getId());
            } else {
                System.out.println("⚠️ Aucune réaction trouvée avec l'ID = " + reaction.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour de la réaction : " + e.getMessage());
        }
    }

    @Override
    public void delete(Reaction reaction) {
        String req = "DELETE FROM REACTION WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, reaction.getId());

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Réaction supprimée : ID = " + reaction.getId());

                // Décrémenter les compteurs
                if (reaction.getPublicationId() != null) {
                    decrementPublicationReactions(reaction.getPublicationId());
                } else if (reaction.getCommentaireId() != null) {
                    decrementCommentaireReactions(reaction.getCommentaireId());
                }
            } else {
                System.out.println("⚠️ Aucune réaction trouvée avec l'ID = " + reaction.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression de la réaction : " + e.getMessage());
        }
    }

    // ========== MÉTHODES SPÉCIALISÉES ==========

    /**
     * Récupérer toutes les réactions d'une publication
     */
    public List<Reaction> findByPublicationId(int publicationId) {
        List<Reaction> reactions = new ArrayList<>();
        String req = "SELECT * FROM REACTION WHERE publicationId = ? ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Reaction reaction = mapResultSetToReaction(rs);
                reactions.add(reaction);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par publication : " + e.getMessage());
        }

        return reactions;
    }

    /**
     * Récupérer toutes les réactions d'un commentaire
     */
    public List<Reaction> findByCommentaireId(int commentaireId) {
        List<Reaction> reactions = new ArrayList<>();
        String req = "SELECT * FROM REACTION WHERE commentaireId = ? ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, commentaireId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Reaction reaction = mapResultSetToReaction(rs);
                reactions.add(reaction);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par commentaire : " + e.getMessage());
        }

        return reactions;
    }

    /**
     * Récupérer toutes les réactions d'un utilisateur
     */
    public List<Reaction> findByUserId(int userId) {
        List<Reaction> reactions = new ArrayList<>();
        String req = "SELECT * FROM REACTION WHERE userId = ? ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Reaction reaction = mapResultSetToReaction(rs);
                reactions.add(reaction);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par user : " + e.getMessage());
        }

        return reactions;
    }

    /**
     * Trouver une réaction par son ID
     */
    public Reaction findById(int id) {
        String req = "SELECT * FROM REACTION WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToReaction(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par ID : " + e.getMessage());
        }

        return null;
    }

    /**
     * Compter le nombre de réactions d'un type spécifique sur une publication
     */
    public int countReactionsByType(int publicationId, TypeReaction type) {
        String req = "SELECT COUNT(*) as count FROM REACTION WHERE publicationId = ? AND type = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.setString(2, type.name());
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage des réactions : " + e.getMessage());
        }

        return 0;
    }

    /**
     * Vérifier si un utilisateur a déjà réagi à une publication
     */
    public boolean userHasReacted(int userId, int publicationId) {
        String req = "SELECT COUNT(*) as count FROM REACTION WHERE userId = ? AND publicationId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            pstm.setInt(2, publicationId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification de réaction : " + e.getMessage());
        }

        return false;
    }

    /**
     * Trouver une réaction spécifique d'un utilisateur sur une publication
     * (utilisé pour le unlike)
     */
    public Reaction findByUserAndPublication(int userId, int publicationId) {
        String req = "SELECT * FROM REACTION WHERE userId = ? AND publicationId = ? LIMIT 1";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            pstm.setInt(2, publicationId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToReaction(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche de réaction : " + e.getMessage());
        }

        return null;
    }
    /**
     * Trouver une réaction spécifique d'un utilisateur sur un commentaire
     * (utilisé pour le unlike sur commentaire)
     */
    public Reaction findByUserAndComment(int userId, int commentaireId) {
        String req = "SELECT * FROM REACTION WHERE userId = ? AND commentaireId = ? LIMIT 1";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            pstm.setInt(2, commentaireId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToReaction(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche de réaction : " + e.getMessage());
        }

        return null;
    }

    // ========== MÉTHODES PRIVÉES ==========

    /**
     * Mapper un ResultSet vers un objet Reaction
     */
    private Reaction mapResultSetToReaction(ResultSet rs) throws SQLException {
        Reaction reaction = new Reaction();
        reaction.setId(rs.getInt("id"));
        reaction.setType(TypeReaction.valueOf(rs.getString("type")));
        reaction.setDateCreation(rs.getTimestamp("dateCreation").toLocalDateTime());
        reaction.setUserId(rs.getInt("userId"));

        if (rs.getObject("publicationId") != null) {
            reaction.setPublicationId(rs.getInt("publicationId"));
        }

        if (rs.getObject("commentaireId") != null) {
            reaction.setCommentaireId(rs.getInt("commentaireId"));
        }

        return reaction;
    }

    /**
     * Incrémenter le compteur de réactions d'une publication
     */
    private void incrementPublicationReactions(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreReactions = nombreReactions + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'incrémentation : " + e.getMessage());
        }
    }

    /**
     * Décrémenter le compteur de réactions d'une publication
     */
    private void decrementPublicationReactions(int publicationId) {
        String req = "UPDATE PUBLICATION SET nombreReactions = nombreReactions - 1 WHERE id = ? AND nombreReactions > 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la décrémentation : " + e.getMessage());
        }
    }

    /**
     * Incrémenter le compteur de réactions d'un commentaire
     */
    private void incrementCommentaireReactions(int commentaireId) {
        String req = "UPDATE COMMENTAIRE SET nombreReactions = nombreReactions + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, commentaireId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'incrémentation : " + e.getMessage());
        }
    }

    /**
     * Décrémenter le compteur de réactions d'un commentaire
     */
    private void decrementCommentaireReactions(int commentaireId) {
        String req = "UPDATE COMMENTAIRE SET nombreReactions = nombreReactions - 1 WHERE id = ? AND nombreReactions > 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, commentaireId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la décrémentation : " + e.getMessage());
        }
    }
    // type de reaction
    public void changeType(int reactionId, TypeReaction newType) {
        String req = "UPDATE REACTION SET type = ? WHERE id = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, newType.name());
            pstm.setInt(2, reactionId);
            pstm.executeUpdate();
            System.out.println("✅ Réaction changée : ID=" + reactionId + " → " + newType.name());
        } catch (SQLException e) {
            System.err.println("❌ Erreur changeType : " + e.getMessage());
        }
    }
}