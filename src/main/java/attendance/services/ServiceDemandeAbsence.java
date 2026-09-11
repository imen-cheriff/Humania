package attendance.services;

import attendance.interfaces.service;
import attendance.models.DemandeAbsence;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceDemandeAbsence implements service<DemandeAbsence> {

    private Connection cnx;

    public ServiceDemandeAbsence() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(DemandeAbsence entity) {
        String req = "INSERT INTO `demande_absence`(`date_demande`, `motif`, `statut`, `absence_id`, `utilisateur_id`) VALUES (?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, Date.valueOf(entity.getDateDemande()));
            pstm.setString(2, entity.getMotif());
            pstm.setString(3, entity.getStatut());
            pstm.setInt(4, entity.getAbsenceId());
            pstm.setInt(5, entity.getUtilisateurId());
            pstm.executeUpdate();
            System.out.println("✅ Demande d'absence ajoutée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    @Override
    public List<DemandeAbsence> getAll() {
        List<DemandeAbsence> list = new ArrayList<>();
        String req = "SELECT * FROM `demande_absence`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                DemandeAbsence da = new DemandeAbsence();
                da.setId(rs.getInt("id"));
                da.setDateDemande(rs.getDate("date_demande").toLocalDate());
                da.setMotif(rs.getString("motif"));
                da.setStatut(rs.getString("statut"));
                da.setAbsenceId(rs.getInt("absence_id"));
                da.setUtilisateurId(rs.getInt("utilisateur_id"));
                list.add(da);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(DemandeAbsence entity) {
        String req = "UPDATE `demande_absence` SET `date_demande`=?, `motif`=?, `statut`=?, `absence_id`=?, `utilisateur_id`=? WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, Date.valueOf(entity.getDateDemande()));
            pstm.setString(2, entity.getMotif());
            pstm.setString(3, entity.getStatut());
            pstm.setInt(4, entity.getAbsenceId());
            pstm.setInt(5, entity.getUtilisateurId());
            pstm.setInt(6, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Demande d'absence modifiée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    @Override
    public void delete(DemandeAbsence entity) {
        String req = "DELETE FROM `demande_absence` WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Demande d'absence supprimée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    // ✅ MÉTHODE AJOUTÉE - Récupérer une demande d'absence par ID
    @Override
    public DemandeAbsence getById(int id) {
        String req = "SELECT * FROM `demande_absence` WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                DemandeAbsence da = new DemandeAbsence();
                da.setId(rs.getInt("id"));
                da.setDateDemande(rs.getDate("date_demande").toLocalDate());
                da.setMotif(rs.getString("motif"));
                da.setStatut(rs.getString("statut"));
                da.setAbsenceId(rs.getInt("absence_id"));
                da.setUtilisateurId(rs.getInt("utilisateur_id"));
                return da;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
        }
        return null;
    }
}