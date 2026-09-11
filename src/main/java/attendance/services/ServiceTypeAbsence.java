package attendance.services;

import attendance.models.TypeAbsence;
import attendance.interfaces.service;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTypeAbsence implements service<TypeAbsence> {

    // Connexion fraîche à chaque appel — évite les connexions expirées
    private Connection getCnx() {
        return MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(TypeAbsence entity) {
        String req = "INSERT INTO `type_absence`(`libelle`, `description`) VALUES (?,?)";
        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setString(1, entity.getLibelle());
            pstm.setString(2, entity.getDescription());
            pstm.executeUpdate();
            System.out.println("✅ Type d'absence ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<TypeAbsence> getAll() {
        List<TypeAbsence> typeAbsences = new ArrayList<>();
        String req = "SELECT * FROM `type_absence`";
        try {
            Statement stm = getCnx().createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                TypeAbsence ta = new TypeAbsence();
                ta.setId(rs.getInt("id"));
                ta.setLibelle(rs.getString("libelle"));
                ta.setDescription(rs.getString("description"));
                typeAbsences.add(ta);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la récupération : " + e.getMessage());
            e.printStackTrace();
        }
        return typeAbsences;
    }

    @Override
    public void update(TypeAbsence entity) {
        String req = "UPDATE `type_absence` SET `libelle`=?, `description`=? WHERE `id`=?";
        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setString(1, entity.getLibelle());
            pstm.setString(2, entity.getDescription());
            pstm.setInt(3, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Type d'absence modifié avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la modification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void delete(TypeAbsence entity) {
        String req = "DELETE FROM `type_absence` WHERE `id`=?";
        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setInt(1, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Type d'absence supprimé avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public TypeAbsence getById(int id) {
        String req = "SELECT * FROM `type_absence` WHERE `id` = ?";
        try {
            PreparedStatement pstm = getCnx().prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                TypeAbsence ta = new TypeAbsence();
                ta.setId(rs.getInt("id"));
                ta.setLibelle(rs.getString("libelle"));
                ta.setDescription(rs.getString("description"));
                return ta;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}