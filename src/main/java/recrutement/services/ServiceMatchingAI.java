package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.MatchingAI;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMatchingAI implements Iservice<MatchingAI> {

    private Connection cnx ;

    public ServiceMatchingAI(){
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(MatchingAI matching) {
        String req ="INSERT INTO `matching_ai`(`id`, `candidatures_id`, `poste_interne_id`, `score_global`, `score_competences`, `score_experience`, `score_formation`, `score_localisation`, `raisonnement`, `competences_manquantes`, `date_calcul`, `version_modele`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,matching.getId());
            pstm.setInt(2,matching.getCandidaturesId());
            pstm.setInt(3,matching.getPosteInterneId());
            pstm.setDouble(4,matching.getScoreGlobal());
            pstm.setDouble(5,matching.getScoreCompetences());
            pstm.setDouble(6,matching.getScoreExperience());
            pstm.setDouble(7,matching.getScoreFormation());
            pstm.setDouble(8,matching.getScoreLocalisation());
            pstm.setString(9,matching.getRaisonnement());
            pstm.setString(10,matching.getCompetencesManquantes());
            if(matching.getDateCalcul() != null) {
                pstm.setDate(11, new Date(matching.getDateCalcul().getTime()));
            } else {
                pstm.setDate(11, null);
            }
            pstm.setString(12,matching.getVersionModele());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<MatchingAI> getAll() {
        List<MatchingAI> matchings = new ArrayList<>();
        String req ="SELECT * FROM `matching_ai` ";
        try {
            Statement stm =this.cnx.createStatement() ;
           ResultSet rs =  stm.executeQuery(req);
           while (rs.next()){
               MatchingAI matching = new MatchingAI();
               matching.setId(rs.getInt("id"));
               matching.setCandidaturesId(rs.getInt("candidatures_id"));
               matching.setPosteInterneId(rs.getInt("poste_interne_id"));
               matching.setScoreGlobal(rs.getDouble("score_global"));
               matching.setScoreCompetences(rs.getDouble("score_competences"));
               matching.setScoreExperience(rs.getDouble("score_experience"));
               matching.setScoreFormation(rs.getDouble("score_formation"));
               matching.setScoreLocalisation(rs.getDouble("score_localisation"));
               matching.setRaisonnement(rs.getString("raisonnement"));
               matching.setCompetencesManquantes(rs.getString("competences_manquantes"));
               matching.setDateCalcul(rs.getDate("date_calcul"));
               matching.setVersionModele(rs.getString("version_modele"));

               matchings.add(matching);
           }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return matchings;
    }

    @Override
    public void update(MatchingAI matching) {
        String req ="UPDATE `matching_ai` SET `candidatures_id`=?, `poste_interne_id`=?, `score_global`=?, `score_competences`=?, `score_experience`=?, `score_formation`=?, `score_localisation`=?, `raisonnement`=?, `competences_manquantes`=?, `date_calcul`=?, `version_modele`=? WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,matching.getCandidaturesId());
            pstm.setInt(2,matching.getPosteInterneId());
            pstm.setDouble(3,matching.getScoreGlobal());
            pstm.setDouble(4,matching.getScoreCompetences());
            pstm.setDouble(5,matching.getScoreExperience());
            pstm.setDouble(6,matching.getScoreFormation());
            pstm.setDouble(7,matching.getScoreLocalisation());
            pstm.setString(8,matching.getRaisonnement());
            pstm.setString(9,matching.getCompetencesManquantes());
            if(matching.getDateCalcul() != null) {
                pstm.setDate(10, new Date(matching.getDateCalcul().getTime()));
            } else {
                pstm.setDate(10, null);
            }
            pstm.setString(11,matching.getVersionModele());
            pstm.setInt(12,matching.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(MatchingAI matching) {
        String req ="DELETE FROM `matching_ai` WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,matching.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
