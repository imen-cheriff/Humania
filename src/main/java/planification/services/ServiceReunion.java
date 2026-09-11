package planification.services;

import planification.interfaces.IService;
import planification.models.Reunion;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReunion implements IService<Reunion> {

    private Connection cnx;

    public ServiceReunion() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Reunion reunion) {
        reunion.valider();

        String req = "INSERT INTO `reunion`(`titre`, `description`, `dateHeureDebut`, `dateHeureFin`, "
                + "`idSalle`, `nomOrganisateur`, `emailOrganisateur`, `participants`, `statut`, `enLigne`, "
                + "`creeLe`, `zoom_meeting_id`, `zoom_join_url`, `zoom_start_url`, `zoom_password`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1,    reunion.getTitre());
            pstm.setString(2,    reunion.getDescription() != null ? reunion.getDescription() : "");
            pstm.setTimestamp(3, new Timestamp(reunion.getDateHeureDebut().getTime()));
            pstm.setTimestamp(4, new Timestamp(reunion.getDateHeureFin().getTime()));
            pstm.setInt(5,       reunion.getIdSalle());
            pstm.setString(6,    reunion.getNomOrganisateur());
            pstm.setString(7,    reunion.getEmailOrganisateur());
            pstm.setString(8,    reunion.getParticipants());
            pstm.setBoolean(9,   reunion.isStatut());
            pstm.setBoolean(10,  reunion.isEnLigne());
            pstm.setTimestamp(11, reunion.getCreeLe() != null
                    ? new Timestamp(reunion.getCreeLe().getTime())
                    : new Timestamp(System.currentTimeMillis()));
            pstm.setLong(12,     reunion.getZoomMeetingId());
            pstm.setString(13,   reunion.getZoomJoinUrl());
            pstm.setString(14,   reunion.getZoomStartUrl());
            pstm.setString(15,   reunion.getZoomPassword());

            pstm.executeUpdate();
            try (ResultSet keys = pstm.getGeneratedKeys()) {
                if (keys.next()) {
                    reunion.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Reunion> getAll() {
        List<Reunion> reunions = new ArrayList<>();
        String req = "SELECT * FROM `reunion`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs  = stm.executeQuery(req);
            while (rs.next()) {
                Reunion reunion = new Reunion();
                reunion.setId(rs.getInt("id"));
                reunion.setTitre(rs.getString("titre"));
                reunion.setDescription(rs.getString("description"));
                Timestamp tsD = rs.getTimestamp("dateHeureDebut");
                Timestamp tsF = rs.getTimestamp("dateHeureFin");
                reunion.setDateHeureDebut(tsD != null ? new java.util.Date(tsD.getTime()) : null);
                reunion.setDateHeureFin(tsF  != null ? new java.util.Date(tsF.getTime())  : null);
                reunion.setIdSalle(rs.getInt("idSalle"));
                reunion.setNomOrganisateur(rs.getString("nomOrganisateur"));
                reunion.setEmailOrganisateur(rs.getString("emailOrganisateur"));
                reunion.setParticipants(rs.getString("participants"));
                reunion.setStatut(rs.getBoolean("statut"));
                reunion.setEnLigne(rs.getBoolean("enLigne"));
                reunion.setCreeLe(rs.getDate("creeLe"));
                // Zoom fields
                reunion.setZoomMeetingId(rs.getLong("zoom_meeting_id"));
                reunion.setZoomJoinUrl(rs.getString("zoom_join_url"));
                reunion.setZoomStartUrl(rs.getString("zoom_start_url"));
                reunion.setZoomPassword(rs.getString("zoom_password"));
                reunions.add(reunion);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return reunions;
    }

    @Override
    public void update(Reunion reunion) {
        reunion.valider();
        String req = "UPDATE `reunion` SET `titre` = ?, `description` = ?, `dateHeureDebut` = ?, "
                + "`dateHeureFin` = ?, `idSalle` = ?, `nomOrganisateur` = ?, `emailOrganisateur` = ?, "
                + "`participants` = ?, `statut` = ?, `enLigne` = ?, `creeLe` = ?, "
                + "`zoom_meeting_id` = ?, `zoom_join_url` = ?, `zoom_start_url` = ?, `zoom_password` = ? "
                + "WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1,    reunion.getTitre());
            pstm.setString(2,    reunion.getDescription() != null ? reunion.getDescription() : "");
            pstm.setTimestamp(3, new Timestamp(reunion.getDateHeureDebut().getTime()));
            pstm.setTimestamp(4, new Timestamp(reunion.getDateHeureFin().getTime()));
            pstm.setInt(5,       reunion.getIdSalle());
            pstm.setString(6,    reunion.getNomOrganisateur());
            pstm.setString(7,    reunion.getEmailOrganisateur());
            pstm.setString(8,    reunion.getParticipants());
            pstm.setBoolean(9,   reunion.isStatut());
            pstm.setBoolean(10,  reunion.isEnLigne());
            pstm.setTimestamp(11, reunion.getCreeLe() != null
                    ? new Timestamp(reunion.getCreeLe().getTime())
                    : new Timestamp(System.currentTimeMillis()));
            pstm.setLong(12,     reunion.getZoomMeetingId());
            pstm.setString(13,   reunion.getZoomJoinUrl());
            pstm.setString(14,   reunion.getZoomStartUrl());
            pstm.setString(15,   reunion.getZoomPassword());
            pstm.setInt(16,      reunion.getId());

            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Reunion reunion) {
        String req = "DELETE FROM `reunion` WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, reunion.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}