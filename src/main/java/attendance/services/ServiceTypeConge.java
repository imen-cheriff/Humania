package attendance.services;

import attendance.models.TypeConge;
import attendance.interfaces.service;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTypeConge implements service<TypeConge> {

    private Connection cnx;

    public ServiceTypeConge() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(TypeConge entity) {
        String req = "INSERT INTO `type_conge`(`libelle`, `description`) VALUES (?,?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, entity.getLibelle());
            pstm.setString(2, entity.getDescription());

            pstm.executeUpdate();
            System.out.println("✅ Type de congé ajouté avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @Override
    public List<TypeConge> getAll() {
        List<TypeConge> typeConges = new ArrayList<>();
        String req = "SELECT * FROM `type_conge`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                TypeConge tc = new TypeConge();
                tc.setId(rs.getInt("id"));
                tc.setLibelle(rs.getString("libelle"));
                tc.setDescription(rs.getString("description"));

                typeConges.add(tc);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la récupération : " + e.getMessage());
        }

        return typeConges;
    }

    @Override
    public void update(TypeConge entity) {
        String req = "UPDATE `type_conge` SET `libelle`=?, `description`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, entity.getLibelle());
            pstm.setString(2, entity.getDescription());
            pstm.setInt(3, entity.getId());

            pstm.executeUpdate();
            System.out.println("✅ Type de congé modifié avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la modification : " + e.getMessage());
        }
    }

    @Override
    public void delete(TypeConge entity) {
        String req = "DELETE FROM `type_conge` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, entity.getId());

            pstm.executeUpdate();
            System.out.println("✅ Type de congé supprimé avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    // ✅ MÉTHODE AJOUTÉE - Récupérer un type de congé par ID
    @Override
    public TypeConge getById(int id) {
        String req = "SELECT * FROM `type_conge` WHERE `id` = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                TypeConge tc = new TypeConge();
                tc.setId(rs.getInt("id"));
                tc.setLibelle(rs.getString("libelle"));
                tc.setDescription(rs.getString("description"));
                return tc;
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
        }

        return null;
    }
}