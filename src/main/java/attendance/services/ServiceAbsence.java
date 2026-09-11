package attendance.services;

import attendance.models.Absence;
import attendance.interfaces.service;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAbsence implements service<Absence> {

    // ⚠️ Ne pas cacher la connexion en champ : elle peut expirer entre 2 appels.
    // On récupère toujours une connexion fraîche à chaque opération.
    private Connection getCnx() {
        return MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Absence absence) {
        String req = "INSERT INTO `absence`(`date_debut`, `date_fin`, `nbr_jours`, `statut`, `type_absence_id`, `utilisateur_id`, `heure_debut`, `heure_fin`, `duree_minutes`, `motif`) VALUES (?,?,?,?,?,?,?,?,?,?)";

        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setDate(1, Date.valueOf(absence.getDateDebut()));
            pstm.setDate(2, Date.valueOf(absence.getDateFin()));
            pstm.setInt(3, absence.getNbrJours());
            pstm.setString(4, absence.getStatut());
            pstm.setInt(5, absence.getTypeAbsenceId());
            pstm.setInt(6, absence.getUtilisateurId());
            pstm.setString(7, absence.getHeureDebut());
            pstm.setString(8, absence.getHeureFin());
            pstm.setInt(9, absence.getDureeMinutes());
            pstm.setString(10, absence.getMotif());

            pstm.executeUpdate();
            System.out.println("✅ Absence ajoutée avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Absence> getAll() {
        List<Absence> absences = new ArrayList<>();
        String req = "SELECT * FROM `absence`";

        try {
            Statement stm = getCnx().createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Absence a = new Absence();
                a.setId(rs.getInt("id"));
                a.setDateDebut(rs.getDate("date_debut").toLocalDate());
                a.setDateFin(rs.getDate("date_fin").toLocalDate());
                a.setNbrJours(rs.getInt("nbr_jours"));
                a.setStatut(rs.getString("statut"));
                a.setTypeAbsenceId(rs.getInt("type_absence_id"));
                a.setUtilisateurId(rs.getInt("utilisateur_id"));
                a.setHeureDebut(rs.getString("heure_debut"));
                a.setHeureFin(rs.getString("heure_fin"));
                a.setDureeMinutes(rs.getInt("duree_minutes"));
                a.setMotif(rs.getString("motif"));
                absences.add(a);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la récupération : " + e.getMessage());
            e.printStackTrace();
        }

        return absences;
    }

    @Override
    public void update(Absence absence) {
        String req = "UPDATE `absence` SET `date_debut`=?, `date_fin`=?, `nbr_jours`=?, `statut`=?, `type_absence_id`=?, `utilisateur_id`=?, `heure_debut`=?, `heure_fin`=?, `duree_minutes`=?, `motif`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setDate(1, Date.valueOf(absence.getDateDebut()));
            pstm.setDate(2, Date.valueOf(absence.getDateFin()));
            pstm.setInt(3, absence.getNbrJours());
            pstm.setString(4, absence.getStatut());
            pstm.setInt(5, absence.getTypeAbsenceId());
            pstm.setInt(6, absence.getUtilisateurId());
            pstm.setString(7, absence.getHeureDebut());
            pstm.setString(8, absence.getHeureFin());
            pstm.setInt(9, absence.getDureeMinutes());
            pstm.setString(10, absence.getMotif());
            pstm.setInt(11, absence.getId());

            pstm.executeUpdate();
            System.out.println("✅ Absence modifiée avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la modification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Absence absence) {
        String req = "DELETE FROM `absence` WHERE `id`=?";

        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setInt(1, absence.getId());
            pstm.executeUpdate();
            System.out.println("✅ Absence supprimée avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Absence getById(int id) {
        String req = "SELECT * FROM `absence` WHERE `id` = ?";

        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                Absence a = new Absence();
                a.setId(rs.getInt("id"));
                a.setDateDebut(rs.getDate("date_debut").toLocalDate());
                a.setDateFin(rs.getDate("date_fin").toLocalDate());
                a.setNbrJours(rs.getInt("nbr_jours"));
                a.setStatut(rs.getString("statut"));
                a.setTypeAbsenceId(rs.getInt("type_absence_id"));
                a.setUtilisateurId(rs.getInt("utilisateur_id"));
                a.setHeureDebut(rs.getString("heure_debut"));
                a.setHeureFin(rs.getString("heure_fin"));
                a.setDureeMinutes(rs.getInt("duree_minutes"));
                a.setMotif(rs.getString("motif"));
                return a;
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}