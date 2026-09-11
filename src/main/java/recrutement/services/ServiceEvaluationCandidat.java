package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.EvaluationCandidat;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvaluationCandidat implements Iservice<EvaluationCandidat> {

    private Connection cnx ;

    public ServiceEvaluationCandidat(){
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(EvaluationCandidat evaluation) {
        String req ="INSERT INTO `evaluation_candidat`(`id`, `candidature_id`, `entretien_id`, `note_technique`, `note_savoir_etre`, `note_motivation`, `note_culture_fit`, `commentaire`, `recommandation`, `point_fort`, `axe_amelioration`, `date_evaluation`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,evaluation.getId());
            pstm.setInt(2,evaluation.getCandidatureId());
            pstm.setInt(3,evaluation.getEntretienId());
            pstm.setInt(4,evaluation.getNoteTechnique());
            pstm.setInt(5,evaluation.getNoteSavoirEtre());
            pstm.setInt(6,evaluation.getNoteMotivation());
            pstm.setInt(7,evaluation.getNoteCultureFit());
            pstm.setString(8,evaluation.getCommentaire());
            pstm.setString(9,evaluation.getRecommandation());
            pstm.setString(10,evaluation.getPointFort());
            pstm.setString(11,evaluation.getAxeAmelioration());
            if(evaluation.getDateEvaluation() != null) {
                pstm.setDate(12, new Date(evaluation.getDateEvaluation().getTime()));
            } else {
                pstm.setDate(12, null);
            }

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<EvaluationCandidat> getAll() {
        List<EvaluationCandidat> evaluations = new ArrayList<>();
        String req ="SELECT * FROM `evaluation_candidat` ";
        try {
            Statement stm =this.cnx.createStatement() ;
           ResultSet rs =  stm.executeQuery(req);
           while (rs.next()){
               EvaluationCandidat evaluation = new EvaluationCandidat();
               evaluation.setId(rs.getInt("id"));
               evaluation.setCandidatureId(rs.getInt("candidature_id"));
               evaluation.setEntretienId(rs.getInt("entretien_id"));
               evaluation.setNoteTechnique(rs.getInt("note_technique"));
               evaluation.setNoteSavoirEtre(rs.getInt("note_savoir_etre"));
               evaluation.setNoteMotivation(rs.getInt("note_motivation"));
               evaluation.setNoteCultureFit(rs.getInt("note_culture_fit"));
               evaluation.setCommentaire(rs.getString("commentaire"));
               evaluation.setRecommandation(rs.getString("recommandation"));
               evaluation.setPointFort(rs.getString("point_fort"));
               evaluation.setAxeAmelioration(rs.getString("axe_amelioration"));
               evaluation.setDateEvaluation(rs.getDate("date_evaluation"));

               evaluations.add(evaluation);
           }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return evaluations;
    }

    @Override
    public void update(EvaluationCandidat evaluation) {
        String req ="UPDATE `evaluation_candidat` SET `candidature_id`=?, `entretien_id`=?, `note_technique`=?, `note_savoir_etre`=?, `note_motivation`=?, `note_culture_fit`=?, `commentaire`=?, `recommandation`=?, `point_fort`=?, `axe_amelioration`=?, `date_evaluation`=? WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,evaluation.getCandidatureId());
            pstm.setInt(2,evaluation.getEntretienId());
            pstm.setInt(3,evaluation.getNoteTechnique());
            pstm.setInt(4,evaluation.getNoteSavoirEtre());
            pstm.setInt(5,evaluation.getNoteMotivation());
            pstm.setInt(6,evaluation.getNoteCultureFit());
            pstm.setString(7,evaluation.getCommentaire());
            pstm.setString(8,evaluation.getRecommandation());
            pstm.setString(9,evaluation.getPointFort());
            pstm.setString(10,evaluation.getAxeAmelioration());
            if(evaluation.getDateEvaluation() != null) {
                pstm.setDate(11, new Date(evaluation.getDateEvaluation().getTime()));
            } else {
                pstm.setDate(11, null);
            }
            pstm.setInt(12,evaluation.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(EvaluationCandidat evaluation) {
        String req ="DELETE FROM `evaluation_candidat` WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,evaluation.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
