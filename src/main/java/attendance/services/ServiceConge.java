package attendance.services;

import attendance.models.Conge;
import attendance.interfaces.service;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceConge implements service<Conge> {

    private Connection cnx;

    public ServiceConge() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Conge entity) {
        String req = "INSERT INTO `conge`(`date_debut`, `date_fin`, `nbr_jours`, `statut`, `type_conge_id`, `utilisateur_id`) VALUES (?,?,?,?,?,?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, Date.valueOf(entity.getDateDebut()));
            pstm.setDate(2, Date.valueOf(entity.getDateFin()));
            pstm.setInt(3, entity.getNbrJours());
            pstm.setString(4, entity.getStatut());
            pstm.setInt(5, entity.getTypeCongeId());
            pstm.setInt(6, entity.getUtilisateurId());

            pstm.executeUpdate();
            System.out.println("✅ Congé ajouté avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @Override
    public List<Conge> getAll() {
        List<Conge> conges = new ArrayList<>();
        String req = "SELECT * FROM `conge`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Conge c = new Conge();
                c.setId(rs.getInt("id"));
                c.setDateDebut(rs.getDate("date_debut").toLocalDate());
                c.setDateFin(rs.getDate("date_fin").toLocalDate());
                c.setNbrJours(rs.getInt("nbr_jours"));
                c.setStatut(rs.getString("statut"));
                c.setTypeCongeId(rs.getInt("type_conge_id"));
                c.setUtilisateurId(rs.getInt("utilisateur_id"));

                conges.add(c);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la récupération : " + e.getMessage());
        }

        return conges;
    }

    @Override
    public void update(Conge entity) {
        String req = "UPDATE `conge` SET `date_debut`=?, `date_fin`=?, `nbr_jours`=?, `statut`=?, `type_conge_id`=?, `utilisateur_id`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setDate(1, Date.valueOf(entity.getDateDebut()));
            pstm.setDate(2, Date.valueOf(entity.getDateFin()));
            pstm.setInt(3, entity.getNbrJours());
            pstm.setString(4, entity.getStatut());
            pstm.setInt(5, entity.getTypeCongeId());
            pstm.setInt(6, entity.getUtilisateurId());
            pstm.setInt(7, entity.getId());

            pstm.executeUpdate();
            System.out.println("✅ Congé modifié avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la modification : " + e.getMessage());
        }
    }

    @Override
    public void delete(Conge entity) {
        String req = "DELETE FROM `conge` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, entity.getId());

            pstm.executeUpdate();
            System.out.println("✅ Congé supprimé avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    // ✅ MÉTHODE AJOUTÉE - Récupérer un congé par ID
    @Override
    public Conge getById(int id) {
        String req = "SELECT * FROM `conge` WHERE `id` = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                Conge c = new Conge();
                c.setId(rs.getInt("id"));
                c.setDateDebut(rs.getDate("date_debut").toLocalDate());
                c.setDateFin(rs.getDate("date_fin").toLocalDate());
                c.setNbrJours(rs.getInt("nbr_jours"));
                c.setStatut(rs.getString("statut"));
                c.setTypeCongeId(rs.getInt("type_conge_id"));
                c.setUtilisateurId(rs.getInt("utilisateur_id"));
                return c;
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
        }

        return null;
    }
}