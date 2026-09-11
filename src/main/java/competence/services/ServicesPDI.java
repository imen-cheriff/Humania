package competence.services;

import competence.interfaces.Service;
import competence.models.PDI;
import competence.enums.Statut;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesPDI implements Service<PDI> {

    private Connection cnx;

    public ServicesPDI() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(PDI pdi) {
        String req = "INSERT INTO `pdi`(`annee`, `progressionGlobale`, `dateCreation`, `statut`) " +
                "VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, pdi.getAnnee());
            pstm.setInt(2, pdi.getProgressionGlobale());
            pstm.setDate(3, pdi.getDateCreation());
            pstm.setString(4, pdi.getStatut().name());

            pstm.executeUpdate();
            System.out.println("PDI ajouté");

        } catch (SQLException e) {
            System.out.println("Erreur ajout PDI: " + e.getMessage());
        }
    }

    @Override
    public List<PDI> getAll() {
        List<PDI> pdis = new ArrayList<>();
        String req = "SELECT * FROM `pdi`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                PDI pdi = new PDI();
                pdi.setId(rs.getInt("id"));
                pdi.setAnnee(rs.getInt("annee"));
                pdi.setProgressionGlobale(rs.getInt("progressionGlobale"));
                pdi.setDateCreation(rs.getDate("dateCreation"));
                pdi.setStatut(Statut.valueOf(rs.getString("statut")));

                pdis.add(pdi);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération PDI: " + e.getMessage());
        }

        return pdis;
    }

    @Override
    public void update(PDI pdi) {
        String req = "UPDATE `pdi` SET `annee`=?, `progressionGlobale`=?, `dateCreation`=?, `statut`=? " +
                "WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, pdi.getAnnee());
            pstm.setInt(2, pdi.getProgressionGlobale());
            pstm.setDate(3, pdi.getDateCreation());
            pstm.setString(4, pdi.getStatut().name());
            pstm.setInt(5, pdi.getId());

            pstm.executeUpdate();
            System.out.println("PDI modifié");

        } catch (SQLException e) {
            System.out.println("Erreur modification PDI: " + e.getMessage());
        }
    }

    @Override
    public void delete(PDI pdi) {
        String req = "DELETE FROM `pdi` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, pdi.getId());

            pstm.executeUpdate();
            System.out.println("PDI supprimé");

        } catch (SQLException e) {
            System.out.println("Erreur suppression PDI: " + e.getMessage());
        }
    }

    public PDI getById(int id) {
        String req = "SELECT * FROM `pdi` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                PDI pdi = new PDI();
                pdi.setId(rs.getInt("id"));
                pdi.setAnnee(rs.getInt("annee"));
                pdi.setProgressionGlobale(rs.getInt("progressionGlobale"));
                pdi.setDateCreation(rs.getDate("dateCreation"));
                pdi.setStatut(Statut.valueOf(rs.getString("statut")));

                return pdi;
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return null;
    }

    public List<PDI> getByAnnee(int annee) {
        List<PDI> pdis = new ArrayList<>();
        String req = "SELECT * FROM `pdi` WHERE `annee`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, annee);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                PDI pdi = new PDI();
                pdi.setId(rs.getInt("id"));
                pdi.setAnnee(rs.getInt("annee"));
                pdi.setProgressionGlobale(rs.getInt("progressionGlobale"));
                pdi.setDateCreation(rs.getDate("dateCreation"));
                pdi.setStatut(Statut.valueOf(rs.getString("statut")));

                pdis.add(pdi);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return pdis;
    }

    public List<PDI> getByStatut(Statut statut) {
        List<PDI> pdis = new ArrayList<>();
        String req = "SELECT * FROM `pdi` WHERE `statut`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, statut.name());
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                PDI pdi = new PDI();
                pdi.setId(rs.getInt("id"));
                pdi.setAnnee(rs.getInt("annee"));
                pdi.setProgressionGlobale(rs.getInt("progressionGlobale"));
                pdi.setDateCreation(rs.getDate("dateCreation"));
                pdi.setStatut(Statut.valueOf(rs.getString("statut")));

                pdis.add(pdi);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return pdis;
    }
}