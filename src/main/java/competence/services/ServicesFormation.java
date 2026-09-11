package competence.services;

import competence.interfaces.Service;
import competence.models.Formation;
import competence.enums.StatutFormation;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesFormation implements Service<Formation> {

    private Connection cnx;

    public ServicesFormation() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Formation formation) {
        String req = "INSERT INTO `formation`(`titre`, `duree`, `cout`, `statutFormation`, `categorie_id`) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, formation.getTitre());
            pstm.setInt(2, formation.getDuree());
            pstm.setDouble(3, formation.getCout());
            pstm.setString(4, formation.getStatutFormation().name());
            pstm.setInt(5, formation.getCategorie() != null ? formation.getCategorie().getId() : 0);

            pstm.executeUpdate();
            System.out.println("Formation ajoutée: " + formation.getTitre());

        } catch (SQLException e) {
            System.out.println("Erreur ajout formation: " + e.getMessage());
        }
    }

    @Override
    public List<Formation> getAll() {
        List<Formation> formations = new ArrayList<>();
        String req = "SELECT * FROM `formation`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Formation formation = new Formation();
                formation.setId(rs.getInt("id"));
                formation.setTitre(rs.getString("titre"));
                formation.setDuree(rs.getInt("duree"));
                formation.setCout(rs.getDouble("cout"));
                formation.setStatutFormation(StatutFormation.valueOf(rs.getString("statutFormation")));

                formations.add(formation);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération formations: " + e.getMessage());
        }

        return formations;
    }

    @Override
    public void update(Formation formation) {
        String req = "UPDATE `formation` SET `titre`=?, `duree`=?, `cout`=?, `statutFormation`=?, " +
                "`categorie_id`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, formation.getTitre());
            pstm.setInt(2, formation.getDuree());
            pstm.setDouble(3, formation.getCout());
            pstm.setString(4, formation.getStatutFormation().name());
            pstm.setInt(5, formation.getCategorie() != null ? formation.getCategorie().getId() : 0);
            pstm.setInt(6, formation.getId());

            pstm.executeUpdate();
            System.out.println("Formation modifiée: " + formation.getTitre());

        } catch (SQLException e) {
            System.out.println("Erreur modification formation: " + e.getMessage());
        }
    }

    @Override
    public void delete(Formation formation) {
        String req = "DELETE FROM `formation` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, formation.getId());

            pstm.executeUpdate();
            System.out.println("Formation supprimée: " + formation.getTitre());

        } catch (SQLException e) {
            System.out.println("Erreur suppression formation: " + e.getMessage());
        }
    }

    public Formation getById(int id) {
        String req = "SELECT * FROM `formation` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                Formation formation = new Formation();
                formation.setId(rs.getInt("id"));
                formation.setTitre(rs.getString("titre"));
                formation.setDuree(rs.getInt("duree"));
                formation.setCout(rs.getDouble("cout"));
                formation.setStatutFormation(StatutFormation.valueOf(rs.getString("statutFormation")));

                return formation;
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return null;
    }

    public List<Formation> rechercher(String critere) {
        List<Formation> formations = new ArrayList<>();
        String req = "SELECT * FROM `formation` WHERE `titre` LIKE ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, "%" + critere + "%");
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Formation formation = new Formation();
                formation.setId(rs.getInt("id"));
                formation.setTitre(rs.getString("titre"));
                formation.setDuree(rs.getInt("duree"));
                formation.setCout(rs.getDouble("cout"));
                formation.setStatutFormation(StatutFormation.valueOf(rs.getString("statutFormation")));

                formations.add(formation);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return formations;
    }

    public List<Formation> getByStatut(StatutFormation statut) {
        List<Formation> formations = new ArrayList<>();
        String req = "SELECT * FROM `formation` WHERE `statutFormation`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, statut.name());
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Formation formation = new Formation();
                formation.setId(rs.getInt("id"));
                formation.setTitre(rs.getString("titre"));
                formation.setDuree(rs.getInt("duree"));
                formation.setCout(rs.getDouble("cout"));
                formation.setStatutFormation(StatutFormation.valueOf(rs.getString("statutFormation")));

                formations.add(formation);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return formations;
    }
}