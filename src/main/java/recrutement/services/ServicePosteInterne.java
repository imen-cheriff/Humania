package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.PosteInterne;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePosteInterne implements Iservice<PosteInterne> {

    private Connection cnx;

    public ServicePosteInterne() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(PosteInterne poste) {
        String req = "INSERT INTO `poste_interne`(`id`, `type_poste`, `remuneration`, `date_debut`, `date_fin`) VALUES (?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, poste.getId());
            pstm.setString(2, poste.getTypePoste());
            pstm.setDouble(3, poste.getRemuneration());
            if (poste.getDateDebut() != null) {
                pstm.setDate(4, poste.getDateDebut());
            } else {
                pstm.setDate(4, null);
            }
            if (poste.getDateFin() != null) {
                pstm.setDate(5, poste.getDateFin());
            } else {
                pstm.setDate(5, null);
            }

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de l'ajout: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PosteInterne> getAll() {
        List<PosteInterne> postes = new ArrayList<>();
        String req = "SELECT * FROM `poste_interne`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                int id = safeGetInt(rs, "id");
                String typePoste = rs.getString("type_poste");
                double remuneration = safeGetDouble(rs, "remuneration");
                Date dateDebut = rs.getDate("date_debut");
                Date dateFin = rs.getDate("date_fin");
                PosteInterne poste = new PosteInterne(id, typePoste, remuneration, dateDebut, dateFin);
                postes.add(poste);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur base de données: " + e.getMessage(), e);
        }

        return postes;
    }

    private static int safeGetInt(ResultSet rs, String column) {
        try {
            String s = rs.getString(column);
            if (s == null || s.trim().isEmpty()) return 0;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double safeGetDouble(ResultSet rs, String column) {
        try {
            String s = rs.getString(column);
            if (s == null || s.trim().isEmpty()) return 0.0;
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public void update(PosteInterne poste) {
        String req = "UPDATE `poste_interne` SET `type_poste`=?, `remuneration`=?, `date_debut`=?, `date_fin`=? WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, poste.getTypePoste());
            pstm.setDouble(2, poste.getRemuneration());
            if (poste.getDateDebut() != null) {
                pstm.setDate(3, poste.getDateDebut());
            } else {
                pstm.setDate(3, null);
            }
            if (poste.getDateFin() != null) {
                pstm.setDate(4, poste.getDateFin());
            } else {
                pstm.setDate(4, null);
            }
            pstm.setInt(5, poste.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    public int getNextId() {
        String req = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM poste_interne";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            if (rs.next()) {
                return rs.getInt("next_id");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 1;
    }

    @Override
    public void delete(PosteInterne poste) {
        String req = "DELETE FROM `poste_interne` WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, poste.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage(), e);
        }
    }
}
