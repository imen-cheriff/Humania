package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.EntretienRecrutement;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEntretienRecrutement implements Iservice<EntretienRecrutement> {

    private Connection cnx ;

    public ServiceEntretienRecrutement(){
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(EntretienRecrutement entretien) {
        String req ="INSERT INTO `entretien_recrutement`(`id`, `candidature_id`, `type_entretien`, `date`, `heure_debut`, `heure_fin`, `intervieweurs_ids`, `salle`, `url_visio`, `statut_entretien`, `note_entretien`, `duree`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,entretien.getId());
            pstm.setInt(2,entretien.getCandidatureId());
            pstm.setString(3,entretien.getTypeEntretien());
            if(entretien.getDate() != null) {
                pstm.setDate(4, new Date(entretien.getDate().getTime()));
            } else {
                pstm.setDate(4, null);
            }
            pstm.setTime(5,entretien.getHeureDebut());
            pstm.setTime(6,entretien.getHeureFin());
            pstm.setString(7,entretien.getIntervieweursIds());
            pstm.setString(8,entretien.getSalle());
            pstm.setString(9,entretien.getURLvisio());
            pstm.setString(10,entretien.getStatutEntretien());
            pstm.setDouble(11,entretien.getNoteEntretien());
            pstm.setInt(12,entretien.getDuree());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<EntretienRecrutement> getAll() {
        List<EntretienRecrutement> entretiens = new ArrayList<>();
        String req ="SELECT * FROM `entretien_recrutement` ";
        try {
            Statement stm =this.cnx.createStatement() ;
           ResultSet rs =  stm.executeQuery(req);
           while (rs.next()){
               EntretienRecrutement entretien = new EntretienRecrutement();
               entretien.setId(rs.getInt("id"));
               entretien.setCandidatureId(rs.getInt("candidature_id"));
               entretien.setTypeEntretien(rs.getString("type_entretien"));
               entretien.setDate(rs.getDate("date"));
               entretien.setHeureDebut(rs.getTime("heure_debut"));
               entretien.setHeureFin(rs.getTime("heure_fin"));
               entretien.setIntervieweursIds(rs.getString("intervieweurs_ids"));
               entretien.setSalle(rs.getString("salle"));
               entretien.setURLvisio(rs.getString("url_visio"));
               entretien.setStatutEntretien(rs.getString("statut_entretien"));
               entretien.setNoteEntretien(rs.getDouble("note_entretien"));
               entretien.setDuree(rs.getInt("duree"));

               entretiens.add(entretien);
           }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return entretiens;
    }

    @Override
    public void update(EntretienRecrutement entretien) {
        String req ="UPDATE `entretien_recrutement` SET `candidature_id`=?, `type_entretien`=?, `date`=?, `heure_debut`=?, `heure_fin`=?, `intervieweurs_ids`=?, `salle`=?, `url_visio`=?, `statut_entretien`=?, `note_entretien`=?, `duree`=? WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,entretien.getCandidatureId());
            pstm.setString(2,entretien.getTypeEntretien());
            if(entretien.getDate() != null) {
                pstm.setDate(3, new Date(entretien.getDate().getTime()));
            } else {
                pstm.setDate(3, null);
            }
            pstm.setTime(4,entretien.getHeureDebut());
            pstm.setTime(5,entretien.getHeureFin());
            pstm.setString(6,entretien.getIntervieweursIds());
            pstm.setString(7,entretien.getSalle());
            pstm.setString(8,entretien.getURLvisio());
            pstm.setString(9,entretien.getStatutEntretien());
            pstm.setDouble(10,entretien.getNoteEntretien());
            pstm.setInt(11,entretien.getDuree());
            pstm.setInt(12,entretien.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(EntretienRecrutement entretien) {
        String req ="DELETE FROM `entretien_recrutement` WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,entretien.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
