package communication.services;

import communication.models.Poll;
import communication.models.PollOption;
import communication.models.PollVote;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PollService {

    private Connection cnx;

    public PollService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // ── Créer un poll ──────────────────────────────────────────
    public Poll createPoll(Poll poll, List<String> options) {
        String req = "INSERT INTO poll (publicationId, question, createdAt, expiresAt, isAnonymous, allowMultiple, createdById, totalVotes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setObject(1, poll.getPublicationId());
            pstm.setString(2, poll.getQuestion());
            pstm.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstm.setTimestamp(4, poll.getExpiresAt() != null ? Timestamp.valueOf(poll.getExpiresAt()) : null);
            pstm.setBoolean(5, poll.isAnonymous());
            pstm.setBoolean(6, poll.isAllowMultiple());
            pstm.setInt(7, poll.getCreatedById());
            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                poll.setId(rs.getInt(1));
            }

            // Insérer les options
            for (int i = 0; i < options.size(); i++) {
                addOption(new PollOption(poll.getId(), options.get(i), i + 1));
            }

            System.out.println("✅ Poll créé : ID = " + poll.getId());
        } catch (SQLException e) {
            System.err.println("❌ Erreur création poll : " + e.getMessage());
        }
        return poll;
    }

    // ── Ajouter une option ─────────────────────────────────────
    public void addOption(PollOption option) {
        String req = "INSERT INTO poll_option (pollId, optionText, voteCount, optionOrder) VALUES (?, ?, 0, ?)";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setInt(1, option.getPollId());
            pstm.setString(2, option.getOptionText());
            pstm.setInt(3, option.getOptionOrder());
            pstm.executeUpdate();
            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) option.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout option : " + e.getMessage());
        }
    }

    // ── Voter ──────────────────────────────────────────────────
    public boolean vote(int pollId, int optionId, int userId) {
        // Vérifie si déjà voté pour cette option
        if (hasVotedForOption(pollId, optionId, userId)) {
            // Retire le vote (toggle)
            removeVote(pollId, optionId, userId);
            return false; // vote retiré
        }

        // Si pas allowMultiple, retire les autres votes d'abord
        Poll poll = findById(pollId);
        if (poll != null && !poll.isAllowMultiple()) {
            removeAllVotes(pollId, userId);
        }

        // Ajoute le vote
        String req = "INSERT INTO poll_vote (pollId, optionId, userId, votedAt) VALUES (?, ?, ?, NOW())";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId);
            pstm.setInt(2, optionId);
            pstm.setInt(3, userId);
            pstm.executeUpdate();

            // Incrémente le compteur
            updateVoteCount(optionId, 1);
            updateTotalVotes(pollId, 1);
            return true; // vote ajouté
        } catch (SQLException e) {
            System.err.println("❌ Erreur vote : " + e.getMessage());
            return false;
        }
    }

    // ── Retirer un vote ────────────────────────────────────────
    private void removeVote(int pollId, int optionId, int userId) {
        String req = "DELETE FROM poll_vote WHERE pollId=? AND optionId=? AND userId=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId); pstm.setInt(2, optionId); pstm.setInt(3, userId);
            pstm.executeUpdate();
            updateVoteCount(optionId, -1);
            updateTotalVotes(pollId, -1);
        } catch (SQLException e) {
            System.err.println("❌ Erreur removeVote : " + e.getMessage());
        }
    }

    private void removeAllVotes(int pollId, int userId) {
        // Récupère les options votées
        List<Integer> votedOptions = getVotedOptionIds(pollId, userId);
        String req = "DELETE FROM poll_vote WHERE pollId=? AND userId=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId); pstm.setInt(2, userId);
            pstm.executeUpdate();
            for (int optId : votedOptions) {
                updateVoteCount(optId, -1);
                updateTotalVotes(pollId, -1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur removeAllVotes : " + e.getMessage());
        }
    }

    // ── Getters ────────────────────────────────────────────────
    public Poll findById(int pollId) {
        String req = "SELECT * FROM poll WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapPoll(rs);
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById poll : " + e.getMessage());
        }
        return null;
    }

    public Poll findByPublicationId(int publicationId) {
        String req = "SELECT * FROM poll WHERE publicationId = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, publicationId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapPoll(rs);
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByPublicationId : " + e.getMessage());
        }
        return null;
    }

    public List<PollOption> getOptions(int pollId) {
        List<PollOption> options = new ArrayList<>();
        String req = "SELECT * FROM poll_option WHERE pollId = ? ORDER BY optionOrder";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                PollOption opt = new PollOption();
                opt.setId(rs.getInt("id"));
                opt.setPollId(rs.getInt("pollId"));
                opt.setOptionText(rs.getString("optionText"));
                opt.setVoteCount(rs.getInt("voteCount"));
                opt.setOptionOrder(rs.getInt("optionOrder"));
                options.add(opt);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getOptions : " + e.getMessage());
        }
        return options;
    }

    public List<PollVote> getVoters(int pollId, int optionId) {
        List<PollVote> votes = new ArrayList<>();
        String req = "SELECT * FROM poll_vote WHERE pollId=? AND optionId=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId); pstm.setInt(2, optionId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                PollVote v = new PollVote();
                v.setId(rs.getInt("id"));
                v.setPollId(rs.getInt("pollId"));
                v.setOptionId(rs.getInt("optionId"));
                v.setUserId(rs.getInt("userId"));
                votes.add(v);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getVoters : " + e.getMessage());
        }
        return votes;
    }

    public boolean hasVotedForOption(int pollId, int optionId, int userId) {
        String req = "SELECT COUNT(*) FROM poll_vote WHERE pollId=? AND optionId=? AND userId=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId); pstm.setInt(2, optionId); pstm.setInt(3, userId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { }
        return false;
    }

    public List<Integer> getVotedOptionIds(int pollId, int userId) {
        List<Integer> ids = new ArrayList<>();
        String req = "SELECT optionId FROM poll_vote WHERE pollId=? AND userId=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId); pstm.setInt(2, userId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) ids.add(rs.getInt("optionId"));
        } catch (SQLException e) { }
        return ids;
    }

    // ── Helpers ────────────────────────────────────────────────
    private void updateVoteCount(int optionId, int delta) {
        // ✅ Recalcule depuis les vrais votes plutôt qu'incrémenter
        String req = "UPDATE poll_option SET voteCount = " +
                "(SELECT COUNT(*) FROM poll_vote WHERE optionId = ?) WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, optionId);
            pstm.setInt(2, optionId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateVoteCount : " + e.getMessage());
        }
    }

    private void updateTotalVotes(int pollId, int delta) {
        // ✅ Recalcule depuis les vrais votes
        String req = "UPDATE poll SET totalVotes = " +
                "(SELECT COUNT(*) FROM poll_vote WHERE pollId = ?) WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, pollId);
            pstm.setInt(2, pollId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateTotalVotes : " + e.getMessage());
        }
    }
    public void updateOption(PollOption option) {
        String req = "UPDATE poll_option SET optionText = ? WHERE id = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1, option.getOptionText());
            pstm.setInt(2, option.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateOption : " + e.getMessage());
        }
    }

    private Poll mapPoll(ResultSet rs) throws SQLException {
        Poll poll = new Poll();
        poll.setId(rs.getInt("id"));
        poll.setPublicationId(rs.getObject("publicationId", Integer.class));
        poll.setQuestion(rs.getString("question"));
        poll.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        Timestamp exp = rs.getTimestamp("expiresAt");
        if (exp != null) poll.setExpiresAt(exp.toLocalDateTime());
        poll.setAnonymous(rs.getBoolean("isAnonymous"));
        poll.setAllowMultiple(rs.getBoolean("allowMultiple"));
        poll.setCreatedById(rs.getInt("createdById"));
        poll.setTotalVotes(rs.getInt("totalVotes"));
        return poll;
    }
}