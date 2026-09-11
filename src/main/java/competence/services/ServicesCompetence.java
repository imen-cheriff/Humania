package competence.services;

import competence.interfaces.Service;
import competence.models.Competence;
import competence.enums.TypeCompetence;
import java.util.List;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;

public class ServicesCompetence implements Service<Competence> {

    private Connection cnx;

    public ServicesCompetence() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Competence competence) {
        // 1- Req SQL : insert into ... (sans l'id car auto-increment)
        String req = "INSERT INTO `competence`(`libelle`, `niveauMax`, `typeCompetence`, `categorie_id`) " +
                "VALUES (?, ?, ?, ?)";

        // 2- Executer la req
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, competence.getLibelle());
            pstm.setInt(2, competence.getNiveauMax());
            pstm.setString(3, competence.getTypeCompetence().name());
            pstm.setInt(4, competence.getCategorie() != null ? competence.getCategorie().getId() : 0);

            pstm.executeUpdate();
            System.out.println(" Compétence ajoutée: " + competence.getLibelle());

        } catch (SQLException e) {
            System.out.println(" Erreur ajout compétence: " + e.getMessage());
        }
    }

    @Override
    public List<Competence> getAll() {
        List<Competence> competences = new ArrayList<>();

        // 1- Req SQL : select ...
        String req = "SELECT * FROM `competence`";

        try {
            // 2- Executer
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            // 3- Matching resultat
            while (rs.next()) {
                Competence competence = new Competence();
                competence.setId(rs.getInt("id"));
                competence.setLibelle(rs.getString("libelle"));
                competence.setNiveauMax(rs.getInt("niveauMax"));
                competence.setTypeCompetence(TypeCompetence.valueOf(rs.getString("typeCompetence")));

                competences.add(competence);
            }

        } catch (SQLException e) {
            System.out.println(" Erreur récupération compétences: " + e.getMessage());
        }

        return competences;
    }

    @Override
    public void update(Competence competence) {
        // 1- Req SQL : update ...
        String req = "UPDATE `competence` SET `libelle`=?, `niveauMax`=?, `typeCompetence`=?, " +
                "`categorie_id`=? WHERE `id`=?";

        try {
            // 2- Executer
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, competence.getLibelle());
            pstm.setInt(2, competence.getNiveauMax());
            pstm.setString(3, competence.getTypeCompetence().name());
            pstm.setInt(4, competence.getCategorie() != null ? competence.getCategorie().getId() : 0);
            pstm.setInt(5, competence.getId());

            pstm.executeUpdate();
            System.out.println(" Compétence modifiée: " + competence.getLibelle());

        } catch (SQLException e) {
            System.out.println(" Erreur modification compétence: " + e.getMessage());
        }
    }

    @Override
    public void delete(Competence competence) {
        // 1- Req SQL : delete ...
        String req = "DELETE FROM `competence` WHERE `id`=?";

        try {
            // 2- Executer
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, competence.getId());

            pstm.executeUpdate();
            System.out.println("Compétence supprimée: " + competence.getLibelle());

        } catch (SQLException e) {
            System.out.println("Erreur suppression compétence: " + e.getMessage());
        }
    }

    public Competence getById(int id) {
        String req = "SELECT * FROM `competence` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                Competence competence = new Competence();
                competence.setId(rs.getInt("id"));
                competence.setLibelle(rs.getString("libelle"));
                competence.setNiveauMax(rs.getInt("niveauMax"));
                competence.setTypeCompetence(TypeCompetence.valueOf(rs.getString("typeCompetence")));

                return competence;
            }

        } catch (SQLException e) {
            System.out.println(" Erreur: " + e.getMessage());
        }

        return null;
    }

    public List<Competence> rechercher(String critere) {
        List<Competence> competences = new ArrayList<>();
        String req = "SELECT * FROM `competence` WHERE `libelle` LIKE ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, "%" + critere + "%");
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Competence competence = new Competence();
                competence.setId(rs.getInt("id"));
                competence.setLibelle(rs.getString("libelle"));
                competence.setNiveauMax(rs.getInt("niveauMax"));
                competence.setTypeCompetence(TypeCompetence.valueOf(rs.getString("typeCompetence")));

                competences.add(competence);
            }

        } catch (SQLException e) {
            System.out.println(" Erreur: " + e.getMessage());
        }

        return competences;
    }

    public List<Competence> getByType(TypeCompetence type) {
        List<Competence> competences = new ArrayList<>();
        String req = "SELECT * FROM `competence` WHERE `typeCompetence`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, type.name());
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                Competence competence = new Competence();
                competence.setId(rs.getInt("id"));
                competence.setLibelle(rs.getString("libelle"));
                competence.setNiveauMax(rs.getInt("niveauMax"));
                competence.setTypeCompetence(TypeCompetence.valueOf(rs.getString("typeCompetence")));

                competences.add(competence);
            }

        } catch (SQLException e) {
            System.out.println(" Erreur: " + e.getMessage());
        }

        return competences;
    }
}