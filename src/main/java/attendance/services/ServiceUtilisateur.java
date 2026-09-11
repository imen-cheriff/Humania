package attendance.services;

import attendance.interfaces.service;
import attendance.models.Utilisateur;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUtilisateur implements service<Utilisateur> {

    private Connection cnx;

    public ServiceUtilisateur() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Utilisateur entity) {
        String req = "INSERT INTO utilisateur (nom, prenom, email, username, mot_de_passe, role, poste_actuel, departement) VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, entity.getNom());
            pstm.setString(2, entity.getPrenom());
            pstm.setString(3, entity.getEmail());
            pstm.setString(4, entity.getEmail()); // username = email par défaut
            pstm.setString(5, entity.getMotDePasse());
            pstm.setString(6, entity.getRole());
            pstm.setString(7, entity.getPoste());
            pstm.setString(8, entity.getDepartement());
            pstm.executeUpdate();
            System.out.println("✅ Utilisateur ajouté !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur add : " + e.getMessage());
        }
    }

    @Override
    public List<Utilisateur> getAll() {
        List<Utilisateur> list = new ArrayList<>();
        String req = "SELECT * FROM utilisateur";  // ✅ pas de filtre actif
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getAll : " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(Utilisateur entity) {
        String req = "UPDATE utilisateur SET nom=?, prenom=?, email=?, role=?, poste_actuel=?, departement=? WHERE id=?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, entity.getNom());
            pstm.setString(2, entity.getPrenom());
            pstm.setString(3, entity.getEmail());
            pstm.setString(4, entity.getRole());
            pstm.setString(5, entity.getPoste());
            pstm.setString(6, entity.getDepartement());
            pstm.setInt(7, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Utilisateur modifié !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur update : " + e.getMessage());
        }
    }

    @Override
    public void delete(Utilisateur entity) {
        // Pas de colonne actif — suppression réelle
        String req = "DELETE FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, entity.getId());
            pstm.executeUpdate();
            System.out.println("✅ Utilisateur supprimé !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur delete : " + e.getMessage());
        }
    }

    @Override
    public Utilisateur getById(int id) {
        String req = "SELECT * FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
        }
        return null;
    }

    // Méthode utilitaire pour mapper un ResultSet → Utilisateur
    private Utilisateur mapResultSet(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setRole(rs.getString("role"));
        u.setPoste(rs.getString("posteActuel"));       // ✅ colonne réelle = poste_actuel
        u.setDepartement(rs.getString("departement"));
        return u;
    }
}