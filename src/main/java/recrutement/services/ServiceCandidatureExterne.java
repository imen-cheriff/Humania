package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.CandidatureExterne;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCandidatureExterne implements Iservice<CandidatureExterne> {

    private Connection cnx;

    public ServiceCandidatureExterne() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(CandidatureExterne candidature) {
        String req = "INSERT INTO `candidature_externe`(`id`, `poste_externe_id`, `nom`, `prenom`, `date_depot`, `statut`, `etape_pipeline`, `scoring_ia`, `cv_url`, `lettre_motivation_url`, `derniere_modification`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, candidature.getId());
            pstm.setInt(2, candidature.getPosteExterneId());
            pstm.setString(3, candidature.getNom());
            pstm.setString(4, candidature.getPrenom());
            if (candidature.getDateDepot() != null) {
                pstm.setDate(5, candidature.getDateDepot());
            } else {
                pstm.setDate(5, null);
            }
            pstm.setString(6, candidature.getStatut());
            pstm.setString(7, candidature.getEtapePipeline());
            pstm.setDouble(8, candidature.getScoringIa());
            pstm.setString(9, candidature.getCvUrl());
            pstm.setString(10, candidature.getLettreMotivationUrl());
            if (candidature.getDerniereModification() != null) {
                pstm.setTimestamp(11, candidature.getDerniereModification());
            } else {
                pstm.setTimestamp(11, null);
            }

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de l'ajout: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CandidatureExterne> getAll() {
        List<CandidatureExterne> candidatures = new ArrayList<>();
        String req = "SELECT * FROM `candidature_externe`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                CandidatureExterne candidature = new CandidatureExterne();
                candidature.setId(safeGetInt(rs, "id"));
                candidature.setPosteExterneId(safeGetInt(rs, "poste_externe_id"));
                candidature.setNom(rs.getString("nom"));
                candidature.setPrenom(rs.getString("prenom"));
                candidature.setDateDepot(rs.getDate("date_depot"));
                candidature.setStatut(rs.getString("statut"));
                candidature.setEtapePipeline(rs.getString("etape_pipeline"));
                candidature.setScoringIa(safeGetDouble(rs, "scoring_ia"));
                candidature.setCvUrl(rs.getString("cv_url"));
                candidature.setLettreMotivationUrl(rs.getString("lettre_motivation_url"));
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
    public void update(CandidatureExterne candidature) {
        String req = "UPDATE `candidature_externe` SET `poste_externe_id`=?, `nom`=?, `prenom`=?, `date_depot`=?, `statut`=?, `etape_pipeline`=?, `scoring_ia`=?, `cv_url`=?, `lettre_motivation_url`=?, `derniere_modification`=? WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, candidature.getPosteExterneId());
            pstm.setString(2, candidature.getNom());
            pstm.setString(3, candidature.getPrenom());
            if (candidature.getDateDepot() != null) {
                pstm.setDate(4, candidature.getDateDepot());
            } else {
                pstm.setDate(4, null);
            }
            pstm.setString(5, candidature.getStatut());
            pstm.setString(6, candidature.getEtapePipeline());
            pstm.setDouble(7, candidature.getScoringIa());
            pstm.setString(8, candidature.getCvUrl());
            pstm.setString(9, candidature.getLettreMotivationUrl());
            if (candidature.getDerniereModification() != null) {
                pstm.setTimestamp(10, candidature.getDerniereModification());
            } else {
                pstm.setTimestamp(10, null);
            }
            pstm.setInt(11, candidature.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    /** Returns the next available ID for a new candidature (max id + 1). */
    public int getNextId() {
        String req = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM candidature_externe";
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
    public void delete(CandidatureExterne candidature) {
        String req = "DELETE FROM `candidature_externe` WHERE `id`=?";
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
