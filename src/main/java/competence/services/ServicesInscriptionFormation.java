package competence.services;

import competence.interfaces.Service;
import competence.models.InscriptionFormation;
import competence.enums.Statut;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesInscriptionFormation implements Service<InscriptionFormation> {

    private Connection cnx;

    public ServicesInscriptionFormation() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(InscriptionFormation inscription) {
        String req = "INSERT INTO `inscriptionFormation`(`statut`, `progression`, `noteFinale`, " +
                "`dateInscription`, `session_id`, `action_pdi_id`) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, inscription.getStatut().name());
            pstm.setInt(2, inscription.getProgression());
            if (inscription.getNoteFinale() != null) {
                pstm.setDouble(3, inscription.getNoteFinale());
            } else {
                pstm.setNull(3, Types.DOUBLE);
            }
            pstm.setDate(4, inscription.getDateInscription());
            pstm.setInt(5, inscription.getSession() != null ? inscription.getSession().getId() : 0);
            pstm.setInt(6, inscription.getActionPDI() != null ? inscription.getActionPDI().getId() : 0);

            pstm.executeUpdate();
            System.out.println("Inscription formation ajoutée");

        } catch (SQLException e) {
            System.out.println("Erreur ajout inscription formation: " + e.getMessage());
        }
    }

    @Override
    public List<InscriptionFormation> getAll() {
        List<InscriptionFormation> inscriptions = new ArrayList<>();
        String req = "SELECT * FROM `inscriptionFormation`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                InscriptionFormation inscription = new InscriptionFormation();
                inscription.setId(rs.getInt("id"));
                inscription.setStatut(Statut.valueOf(rs.getString("statut")));
                inscription.setProgression(rs.getInt("progression"));
                Double noteFinale = rs.getDouble("noteFinale");
                inscription.setNoteFinale(rs.wasNull() ? null : noteFinale);
                inscription.setDateInscription(rs.getDate("dateInscription"));

                inscriptions.add(inscription);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération inscriptions formation: " + e.getMessage());
        }

        return inscriptions;
    }

    @Override
    public void update(InscriptionFormation inscription) {
        String req = "UPDATE `inscriptionFormation` SET `statut`=?, `progression`=?, `noteFinale`=?, " +
                "`dateInscription`=?, `session_id`=?, `action_pdi_id`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, inscription.getStatut().name());
            pstm.setInt(2, inscription.getProgression());
            if (inscription.getNoteFinale() != null) {
                pstm.setDouble(3, inscription.getNoteFinale());
            } else {
                pstm.setNull(3, Types.DOUBLE);
            }
            pstm.setDate(4, inscription.getDateInscription());
            pstm.setInt(5, inscription.getSession() != null ? inscription.getSession().getId() : 0);
            pstm.setInt(6, inscription.getActionPDI() != null ? inscription.getActionPDI().getId() : 0);
            pstm.setInt(7, inscription.getId());

            pstm.executeUpdate();
            System.out.println("Inscription formation modifiée");

        } catch (SQLException e) {
            System.out.println("Erreur modification inscription formation: " + e.getMessage());
        }
    }

    @Override
    public void delete(InscriptionFormation inscription) {
        String req = "DELETE FROM `inscriptionFormation` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, inscription.getId());

            pstm.executeUpdate();
            System.out.println("Inscription formation supprimée");

        } catch (SQLException e) {
            System.out.println("Erreur suppression inscription formation: " + e.getMessage());
        }
    }

    public InscriptionFormation getById(int id) {
        String req = "SELECT * FROM `inscriptionFormation` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                InscriptionFormation inscription = new InscriptionFormation();
                inscription.setId(rs.getInt("id"));
                inscription.setStatut(Statut.valueOf(rs.getString("statut")));
                inscription.setProgression(rs.getInt("progression"));
                Double noteFinale = rs.getDouble("noteFinale");
                inscription.setNoteFinale(rs.wasNull() ? null : noteFinale);
                inscription.setDateInscription(rs.getDate("dateInscription"));

                return inscription;
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return null;
    }

    public List<InscriptionFormation> getBySessionId(int sessionId) {
        List<InscriptionFormation> inscriptions = new ArrayList<>();
        String req = "SELECT * FROM `inscriptionFormation` WHERE `session_id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, sessionId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                InscriptionFormation inscription = new InscriptionFormation();
                inscription.setId(rs.getInt("id"));
                inscription.setStatut(Statut.valueOf(rs.getString("statut")));
                inscription.setProgression(rs.getInt("progression"));
                Double noteFinale = rs.getDouble("noteFinale");
                inscription.setNoteFinale(rs.wasNull() ? null : noteFinale);
                inscription.setDateInscription(rs.getDate("dateInscription"));

                inscriptions.add(inscription);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return inscriptions;
    }

    public List<InscriptionFormation> getByStatut(Statut statut) {
        List<InscriptionFormation> inscriptions = new ArrayList<>();
        String req = "SELECT * FROM `inscriptionFormation` WHERE `statut`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, statut.name());
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                InscriptionFormation inscription = new InscriptionFormation();
                inscription.setId(rs.getInt("id"));
                inscription.setStatut(Statut.valueOf(rs.getString("statut")));
                inscription.setProgression(rs.getInt("progression"));
                Double noteFinale = rs.getDouble("noteFinale");
                inscription.setNoteFinale(rs.wasNull() ? null : noteFinale);
                inscription.setDateInscription(rs.getDate("dateInscription"));

                inscriptions.add(inscription);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return inscriptions;
    }
}