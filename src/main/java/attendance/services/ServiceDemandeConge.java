package attendance.services;

import attendance.interfaces.service;
import attendance.models.DemandeConge;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceDemandeConge implements service<DemandeConge> {

    private Connection cnx;

    public ServiceDemandeConge() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(DemandeConge entity) {
        String req = "INSERT INTO `demande_conge`(`date_demande`, `motif`, `statut`, `conge_id`, `utilisateur_id`) VALUES (?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, Date.valueOf(entity.getDateDemande()));
            pstm.setString(2, entity.getMotif());
            pstm.setString(3, entity.getStatut());
            pstm.setInt(4, entity.getCongeId());
            pstm.setInt(5, entity.getUtilisateurId());
            pstm.executeUpdate();
            System.out.println("✅ Demande de congé ajoutée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    @Override
    public List<DemandeConge> getAll() {
        List<DemandeConge> list = new ArrayList<>();
        String req = "SELECT * FROM `demande_conge`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                DemandeConge dc = new DemandeConge();
                dc.setId(rs.getInt("id"));
                dc.setDateDemande(rs.getDate("date_demande").toLocalDate());
                dc.setMotif(rs.getString("motif"));
                dc.setStatut(rs.getString("statut"));
                dc.setCongeId(rs.getInt("conge_id"));
                dc.setUtilisateurId(rs.getInt("utilisateur_id"));
                list.add(dc);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(DemandeConge entity) {
        String req = "UPDATE `demande_conge` SET `date_demande`=?, `motif`=?, `statut`=?, `conge_id`=?, `utilisateur_id`=? WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, Date.valueOf(entity.getDateDemande()));
            pstm.setString(2, entity.getMotif());
            pstm.setString(3, entity.getStatut());
            pstm.setInt(4, entity.getCongeId());
            pstm.setInt(5, entity.getUtilisateurId());
            pstm.setInt(6, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Demande de congé modifiée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    @Override
    public void delete(DemandeConge entity) {
        String req = "DELETE FROM `demande_conge` WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Demande de congé supprimée !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    // ✅ MÉTHODE AJOUTÉE - Récupérer une demande de congé par ID
    @Override
    public DemandeConge getById(int id) {
        String req = "SELECT * FROM `demande_conge` WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                DemandeConge dc = new DemandeConge();
                dc.setId(rs.getInt("id"));
                dc.setDateDemande(rs.getDate("date_demande").toLocalDate());
                dc.setMotif(rs.getString("motif"));
                dc.setStatut(rs.getString("statut"));
                dc.setCongeId(rs.getInt("conge_id"));
                dc.setUtilisateurId(rs.getInt("utilisateur_id"));
                return dc;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
        }
        return null;
    }
}