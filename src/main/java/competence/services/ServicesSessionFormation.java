package competence.services;

import competence.interfaces.Service;
import competence.models.SessionFormation;
import competence.enums.Statut;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesSessionFormation implements Service<SessionFormation> {

    private Connection cnx;

    public ServicesSessionFormation() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(SessionFormation session) {
        String req = "INSERT INTO `sessionFormation`(`dateDebut`, `dateFin`, `lieu`, `statut`, `formation_id`) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, session.getDateDebut());
            pstm.setDate(2, session.getDateFin());
            pstm.setString(3, session.getLieu());
            pstm.setString(4, session.getStatut().name());
            pstm.setInt(5, session.getFormation() != null ? session.getFormation().getId() : 0);

            pstm.executeUpdate();
            System.out.println("Session de formation ajoutée");

        } catch (SQLException e) {
            System.out.println("Erreur ajout session de formation: " + e.getMessage());
        }
    }

    @Override
    public List<SessionFormation> getAll() {
        List<SessionFormation> sessions = new ArrayList<>();
        String req = "SELECT * FROM `sessionFormation`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                SessionFormation session = new SessionFormation();
                session.setId(rs.getInt("id"));
                session.setDateDebut(rs.getDate("dateDebut"));
                session.setDateFin(rs.getDate("dateFin"));
                session.setLieu(rs.getString("lieu"));
                session.setStatut(Statut.valueOf(rs.getString("statut")));

                sessions.add(session);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération sessions de formation: " + e.getMessage());
        }

        return sessions;
    }

    @Override
    public void update(SessionFormation session) {
        String req = "UPDATE `sessionFormation` SET `dateDebut`=?, `dateFin`=?, `lieu`=?, `statut`=?, " +
                "`formation_id`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, session.getDateDebut());
            pstm.setDate(2, session.getDateFin());
            pstm.setString(3, session.getLieu());
            pstm.setString(4, session.getStatut().name());
            pstm.setInt(5, session.getFormation() != null ? session.getFormation().getId() : 0);
            pstm.setInt(6, session.getId());

            pstm.executeUpdate();
            System.out.println("Session de formation modifiée");

        } catch (SQLException e) {
            System.out.println("Erreur modification session de formation: " + e.getMessage());
        }
    }

    @Override
    public void delete(SessionFormation session) {
        String req = "DELETE FROM `sessionFormation` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, session.getId());

            pstm.executeUpdate();
            System.out.println("Session de formation supprimée");

        } catch (SQLException e) {
            System.out.println("Erreur suppression session de formation: " + e.getMessage());
        }
    }

    public SessionFormation getById(int id) {
        String req = "SELECT * FROM `sessionFormation` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                SessionFormation session = new SessionFormation();
                session.setId(rs.getInt("id"));
                session.setDateDebut(rs.getDate("dateDebut"));
                session.setDateFin(rs.getDate("dateFin"));
                session.setLieu(rs.getString("lieu"));
                session.setStatut(Statut.valueOf(rs.getString("statut")));

                return session;
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return null;
    }

    public List<SessionFormation> getByFormationId(int formationId) {
        List<SessionFormation> sessions = new ArrayList<>();
        String req = "SELECT * FROM `sessionFormation` WHERE `formation_id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, formationId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                SessionFormation session = new SessionFormation();
                session.setId(rs.getInt("id"));
                session.setDateDebut(rs.getDate("dateDebut"));
                session.setDateFin(rs.getDate("dateFin"));
                session.setLieu(rs.getString("lieu"));
                session.setStatut(Statut.valueOf(rs.getString("statut")));

                sessions.add(session);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return sessions;
    }

    public List<SessionFormation> getByStatut(Statut statut) {
        List<SessionFormation> sessions = new ArrayList<>();
        String req = "SELECT * FROM `sessionFormation` WHERE `statut`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, statut.name());
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                SessionFormation session = new SessionFormation();
                session.setId(rs.getInt("id"));
                session.setDateDebut(rs.getDate("dateDebut"));
                session.setDateFin(rs.getDate("dateFin"));
                session.setLieu(rs.getString("lieu"));
                session.setStatut(Statut.valueOf(rs.getString("statut")));

                sessions.add(session);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return sessions;
    }
}