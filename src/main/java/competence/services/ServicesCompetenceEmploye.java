package competence.services;

import competence.interfaces.Service;
import competence.models.CompetenceEmploye;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesCompetenceEmploye implements Service<CompetenceEmploye> {

    private Connection cnx;

    public ServicesCompetenceEmploye() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(CompetenceEmploye competenceEmploye) {
        String req = "INSERT INTO `competenceEmploye`(`niveauActuel`, `niveauValide`, `preuveUrl`, " +
                "`dateEvaluation`, `employe_id`, `competence_id`) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, competenceEmploye.getNiveauActuel());
            pstm.setBoolean(2, competenceEmploye.isNiveauValide());
            pstm.setString(3, competenceEmploye.getPreuveUrl());
            pstm.setDate(4, competenceEmploye.getDateEvaluation());
            pstm.setInt(5, competenceEmploye.getEmploye() != null ? competenceEmploye.getEmploye().getId() : 0);
            pstm.setInt(6, competenceEmploye.getCompetence() != null ? competenceEmploye.getCompetence().getId() : 0);

            pstm.executeUpdate();
            System.out.println("Compétence employé ajoutée");

        } catch (SQLException e) {
            System.out.println("Erreur ajout compétence employé: " + e.getMessage());
        }
    }

    @Override
    public List<CompetenceEmploye> getAll() {
        List<CompetenceEmploye> competencesEmployes = new ArrayList<>();
        String req = "SELECT * FROM `competenceEmploye`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                CompetenceEmploye ce = new CompetenceEmploye();
                ce.setId(rs.getInt("id"));
                ce.setNiveauActuel(rs.getInt("niveauActuel"));
                ce.setNiveauValide(rs.getBoolean("niveauValide"));
                ce.setPreuveUrl(rs.getString("preuveUrl"));
                ce.setDateEvaluation(rs.getDate("dateEvaluation"));

                competencesEmployes.add(ce);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération compétences employé: " + e.getMessage());
        }

        return competencesEmployes;
    }

    @Override
    public void update(CompetenceEmploye competenceEmploye) {
        String req = "UPDATE `competenceEmploye` SET `niveauActuel`=?, `niveauValide`=?, `preuveUrl`=?, " +
                "`dateEvaluation`=?, `employe_id`=?, `competence_id`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, competenceEmploye.getNiveauActuel());
            pstm.setBoolean(2, competenceEmploye.isNiveauValide());
            pstm.setString(3, competenceEmploye.getPreuveUrl());
            pstm.setDate(4, competenceEmploye.getDateEvaluation());
            pstm.setInt(5, competenceEmploye.getEmploye() != null ? competenceEmploye.getEmploye().getId() : 0);
            pstm.setInt(6, competenceEmploye.getCompetence() != null ? competenceEmploye.getCompetence().getId() : 0);
            pstm.setInt(7, competenceEmploye.getId());

            pstm.executeUpdate();
            System.out.println("Compétence employé modifiée");

        } catch (SQLException e) {
            System.out.println("Erreur modification compétence employé: " + e.getMessage());
        }
    }

    @Override
    public void delete(CompetenceEmploye competenceEmploye) {
        String req = "DELETE FROM `competenceEmploye` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, competenceEmploye.getId());

            pstm.executeUpdate();
            System.out.println("Compétence employé supprimée");

        } catch (SQLException e) {
            System.out.println("Erreur suppression compétence employé: " + e.getMessage());
        }
    }

    public List<CompetenceEmploye> getByEmployeId(int employeId) {
        List<CompetenceEmploye> competencesEmployes = new ArrayList<>();
        String req = "SELECT * FROM `competenceEmploye` WHERE `employe_id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, employeId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                CompetenceEmploye ce = new CompetenceEmploye();
                ce.setId(rs.getInt("id"));
                ce.setNiveauActuel(rs.getInt("niveauActuel"));
                ce.setNiveauValide(rs.getBoolean("niveauValide"));
                ce.setPreuveUrl(rs.getString("preuveUrl"));
                ce.setDateEvaluation(rs.getDate("dateEvaluation"));

                competencesEmployes.add(ce);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return competencesEmployes;
    }

    public List<CompetenceEmploye> getByCompetenceId(int competenceId) {
        List<CompetenceEmploye> competencesEmployes = new ArrayList<>();
        String req = "SELECT * FROM `competenceEmploye` WHERE `competence_id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, competenceId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                CompetenceEmploye ce = new CompetenceEmploye();
                ce.setId(rs.getInt("id"));
                ce.setNiveauActuel(rs.getInt("niveauActuel"));
                ce.setNiveauValide(rs.getBoolean("niveauValide"));
                ce.setPreuveUrl(rs.getString("preuveUrl"));
                ce.setDateEvaluation(rs.getDate("dateEvaluation"));

                competencesEmployes.add(ce);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return competencesEmployes;
    }
}