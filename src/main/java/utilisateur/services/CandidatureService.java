package utilisateur.services;

import utilisateur.models.Candidature;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {

    private final Connection cnx;

    public CandidatureService() {
        cnx = MyDataBase.getInstance().getCnx();
    }

    /**
     * Fetches candidatures where statut = 'Accepté'.
     * A candidature is considered "already converted" ONLY if its employe_id
     * references a real row in the utilisateur table (i.e. the conversion was
     * done through the app and the utilisateur record actually exists).
     * This prevents test/dummy employe_id values (e.g. 1, 3 …) from being
     * treated as converted.
     */
    public List<Candidature> getAcceptedCandidatures() throws SQLException {
        List<Candidature> list = new ArrayList<>();

        String sql = """
                SELECT c.*,
                       CASE WHEN u.id IS NOT NULL THEN 1 ELSE 0 END AS deja_converti
                FROM candidature c
                LEFT JOIN utilisateur u
                       ON c.employe_id = u.id
                      AND c.employe_id IS NOT NULL
                      AND c.employe_id > 0
                WHERE c.statut = ?
                ORDER BY c.date_depot DESC
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, "Accepté");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Candidature c = mapRow(rs);
                c.setAlreadyConverted(rs.getInt("deja_converti") == 1);
                list.add(c);
            }
        }
        return list;
    }

    /** Updates candidature.employe_id with the new user id after conversion. */
    public void setEmployeId(int candidatureId, int employeId) throws SQLException {
        String sql = "UPDATE candidature SET employe_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, employeId);
            ps.setInt(2, candidatureId);
            ps.executeUpdate();
        }
    }

    /**
     * Checks whether a candidature has already been converted by verifying
     * that employe_id points to an existing utilisateur row.
     * Used before conversion to prevent double-conversion.
     */
    public boolean isDejaConverti(int candidatureId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM candidature c
                JOIN utilisateur u ON c.employe_id = u.id
                WHERE c.id = ?
                  AND c.employe_id IS NOT NULL
                  AND c.employe_id > 0
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, candidatureId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private Candidature mapRow(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setId(rs.getInt("id"));
        c.setNom(rs.getString("nom"));
        c.setPrenom(rs.getString("prenom"));
        try { c.setEmail(rs.getString("email")); } catch (SQLException ignored) { c.setEmail(null); }
        int cid = rs.getInt("candidat_id");
        c.setCandidatId(rs.wasNull() ? null : cid);
        int eid = rs.getInt("employe_id");
        c.setEmployeId((rs.wasNull() || eid <= 0) ? null : eid);
        c.setTypeCandidat(rs.getString("type_candidat"));
        c.setStatut(rs.getString("statut"));
        c.setEtapePipeline(rs.getString("etape_pipeline"));
        double s = rs.getDouble("scoring_ia");
        c.setScoringIa(rs.wasNull() ? null : s);
        c.setCvUrl(rs.getString("cv_url"));
        c.setLettreMotivationUrl(rs.getString("lettre_motivation_url"));
        double sal = rs.getDouble("salaire_pretendu");
        c.setSalairePretendu(rs.wasNull() ? null : sal);
        c.setCommentairesRh(rs.getString("commentaires_rh"));
        Date d = rs.getDate("date_depot");
        c.setDateDepot(d != null ? d.toLocalDate().atStartOfDay() : null);
        return c;
    }
}