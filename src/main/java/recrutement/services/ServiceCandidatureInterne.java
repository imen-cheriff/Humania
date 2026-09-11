package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.CandidatureInterne;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCandidatureInterne implements Iservice<CandidatureInterne> {

    private Connection cnx;

    public ServiceCandidatureInterne() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(CandidatureInterne candidature) {
        String req = "INSERT INTO `candidature_interne`(`id`, `poste_actuel`, `nouveau_poste`, `nouveau_salaire`, `date_demande`, `motif`, `derniere_modification`) VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, candidature.getId());
            pstm.setString(2, candidature.getPosteActuel());
            pstm.setString(3, candidature.getNouveauPoste());
            pstm.setDouble(4, candidature.getNouveauSalaire());
            if (candidature.getDateDemande() != null) {
                pstm.setDate(5, candidature.getDateDemande());
            } else {
                pstm.setDate(5, null);
            }
            pstm.setString(6, candidature.getMotif());
            if (candidature.getDerniereModification() != null) {
                pstm.setTimestamp(7, candidature.getDerniereModification());
            } else {
                pstm.setTimestamp(7, null);
            }

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de l'ajout: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CandidatureInterne> getAll() {
        List<CandidatureInterne> candidatures = new ArrayList<>();
        String req = "SELECT * FROM `candidature_interne`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                CandidatureInterne candidature = new CandidatureInterne();
                candidature.setId(safeGetInt(rs, "id"));
                candidature.setPosteActuel(rs.getString("poste_actuel"));
                candidature.setNouveauPoste(rs.getString("nouveau_poste"));
                candidature.setNouveauSalaire(safeGetDouble(rs, "nouveau_salaire"));
                candidature.setDateDemande(rs.getDate("date_demande"));
                candidature.setMotif(rs.getString("motif"));
                candidature.setDerniereModification(rs.getTimestamp("derniere_modification"));

                candidatures.add(candidature);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur base de données: " + e.getMessage(), e);
        }

        return candidatures;
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
    public void update(CandidatureInterne candidature) {
        String req = "UPDATE `candidature_interne` SET `poste_actuel`=?, `nouveau_poste`=?, `nouveau_salaire`=?, `date_demande`=?, `motif`=?, `derniere_modification`=? WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, candidature.getPosteActuel());
            pstm.setString(2, candidature.getNouveauPoste());
            pstm.setDouble(3, candidature.getNouveauSalaire());
            if (candidature.getDateDemande() != null) {
                pstm.setDate(4, candidature.getDateDemande());
            } else {
                pstm.setDate(4, null);
            }
            pstm.setString(5, candidature.getMotif());
            if (candidature.getDerniereModification() != null) {
                pstm.setTimestamp(6, candidature.getDerniereModification());
            } else {
                pstm.setTimestamp(6, null);
            }
            pstm.setInt(7, candidature.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    /** Returns the next available ID for a new candidature (max id + 1). */
    public int getNextId() {
        String req = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM candidature_interne";
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
    public void delete(CandidatureInterne candidature) {
        String req = "DELETE FROM `candidature_interne` WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, candidature.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage(), e);
        }
    }
}
