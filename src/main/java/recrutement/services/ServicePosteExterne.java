package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.PosteExterne;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePosteExterne implements Iservice<PosteExterne> {

    private Connection cnx;

    public ServicePosteExterne() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(PosteExterne poste) {
        String req = "INSERT INTO `poste_externe`(`id`, `titre`, `description`, `type_contrat`, `salaire`, `competences_requises`, `experience_requise`, `niveau_etude_requis`, `statut`, `date_publication`, `date_cloture`, `nombre_employe`, `priorite`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, poste.getId());
            pstm.setString(2, poste.getTitre());
            pstm.setString(3, poste.getDescription());
            pstm.setString(4, poste.getTypeContrat());
            pstm.setDouble(5, poste.getSalaire() != null ? poste.getSalaire() : 0.0);
            pstm.setString(6, poste.getCompetences_Requises());
            pstm.setInt(7, poste.getExperience_Requise());
            pstm.setString(8, poste.getNiveau_Etude_Requis());
            pstm.setString(9, poste.getStatut());
            if (poste.getDatePublication() != null) {
                pstm.setDate(10, new Date(poste.getDatePublication().getTime()));
            } else {
                pstm.setDate(10, null);
            }
            if (poste.getDateCloture() != null) {
                pstm.setDate(11, new Date(poste.getDateCloture().getTime()));
            } else {
                pstm.setDate(11, null);
            }
            pstm.setInt(12, poste.getNombreEmploye());
            pstm.setInt(13, poste.getPriorite());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de l'ajout: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PosteExterne> getAll() {
        List<PosteExterne> postes = new ArrayList<>();
        String req = "SELECT * FROM `poste_externe`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                PosteExterne poste = new PosteExterne();
                poste.setId(safeGetInt(rs, "id"));
                poste.setTitre(rs.getString("titre"));
                poste.setDescription(rs.getString("description"));
                poste.setTypeContrat(rs.getString("type_contrat"));
                poste.setSalaire(safeGetDouble(rs, "salaire"));
                poste.setCompetences_Requises(rs.getString("competences_requises"));
                poste.setExperience_Requise(safeGetInt(rs, "experience_requise"));
                poste.setNiveau_Etude_Requis(rs.getString("niveau_etude_requis"));
                poste.setStatut(rs.getString("statut"));
                poste.setDatePublication(rs.getDate("date_publication"));
                poste.setDateCloture(rs.getDate("date_cloture"));
                try {
                    poste.setPublicExterne(safeGetBoolean(rs, "public_externe"));
                } catch (Exception ignored) {
                    poste.setPublicExterne(true); // défaut pour poste_externe
                }
                poste.setResponsableRHid(safeGetInt(rs, "responsable_rh_id")); // 0 si colonne absente
                poste.setNombreEmploye(safeGetInt(rs, "nombre_employe"));
                poste.setPriorite(safeGetInt(rs, "priorite"));

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

    private static boolean safeGetBoolean(ResultSet rs, String column) {
        try {
            Object o = rs.getObject(column);
            if (o == null) return false;
            if (o instanceof Boolean) return (Boolean) o;
            String s = String.valueOf(o).trim().toLowerCase();
            return "1".equals(s) || "true".equals(s) || "oui".equals(s) || "yes".equals(s);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void update(PosteExterne poste) {
        String req = "UPDATE `poste_externe` SET `titre`=?, `description`=?, `type_contrat`=?, `salaire`=?, `competences_requises`=?, `experience_requise`=?, `niveau_etude_requis`=?, `statut`=?, `date_publication`=?, `date_cloture`=?, `nombre_employe`=?, `priorite`=? WHERE `id`=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, poste.getTitre());
            pstm.setString(2, poste.getDescription());
            pstm.setString(3, poste.getTypeContrat());
            pstm.setDouble(4, poste.getSalaire() != null ? poste.getSalaire() : 0.0);
            pstm.setString(5, poste.getCompetences_Requises());
            pstm.setInt(6, poste.getExperience_Requise());
            pstm.setString(7, poste.getNiveau_Etude_Requis());
            pstm.setString(8, poste.getStatut());
            if (poste.getDatePublication() != null) {
                pstm.setDate(9, new Date(poste.getDatePublication().getTime()));
            } else {
                pstm.setDate(9, null);
            }
            if (poste.getDateCloture() != null) {
                pstm.setDate(10, new Date(poste.getDateCloture().getTime()));
            } else {
                pstm.setDate(10, null);
            }
            pstm.setInt(11, poste.getNombreEmploye());
            pstm.setInt(12, poste.getPriorite());
            pstm.setInt(13, poste.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    /** Returns the next available ID for a new poste externe (max id + 1). */
    public int getNextId() {
        String req = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM poste_externe";
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
    public void delete(PosteExterne poste) {
        String req = "DELETE FROM `poste_externe` WHERE `id`=?";
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
