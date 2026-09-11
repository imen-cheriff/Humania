package competence.services;

import competence.interfaces.Service;
import competence.models.CategorieCompetence;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesCategorieCompetence implements Service<CategorieCompetence> {

    private Connection cnx;

    public ServicesCategorieCompetence() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(CategorieCompetence categorie) {
        String req = "INSERT INTO `categorieCompetence`(`libelle`, `couleur`) VALUES (?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, categorie.getLibelle());
            pstm.setString(2, categorie.getCouleur());

            pstm.executeUpdate();
            System.out.println("Catégorie de compétence ajoutée: " + categorie.getLibelle());

        } catch (SQLException e) {
            System.out.println("Erreur ajout catégorie de compétence: " + e.getMessage());
        }
    }

    @Override
    public List<CategorieCompetence> getAll() {
        List<CategorieCompetence> categories = new ArrayList<>();
        String req = "SELECT * FROM `categorieCompetence`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                CategorieCompetence categorie = new CategorieCompetence();
                categorie.setId(rs.getInt("id"));
                categorie.setLibelle(rs.getString("libelle"));
                categorie.setCouleur(rs.getString("couleur"));

                categories.add(categorie);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération catégories de compétence: " + e.getMessage());
        }

        return categories;
    }

    @Override
    public void update(CategorieCompetence categorie) {
        String req = "UPDATE `categorieCompetence` SET `libelle`=?, `couleur`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, categorie.getLibelle());
            pstm.setString(2, categorie.getCouleur());
            pstm.setInt(3, categorie.getId());

            pstm.executeUpdate();
            System.out.println("Catégorie de compétence modifiée: " + categorie.getLibelle());

        } catch (SQLException e) {
            System.out.println("Erreur modification catégorie de compétence: " + e.getMessage());
        }
    }

    @Override
    public void delete(CategorieCompetence categorie) {
        String req = "DELETE FROM `categorieCompetence` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, categorie.getId());

            pstm.executeUpdate();
            System.out.println("Catégorie de compétence supprimée: " + categorie.getLibelle());

        } catch (SQLException e) {
            System.out.println("Erreur suppression catégorie de compétence: " + e.getMessage());
        }
    }

    public CategorieCompetence getById(int id) {
        String req = "SELECT * FROM `categorieCompetence` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                CategorieCompetence categorie = new CategorieCompetence();
                categorie.setId(rs.getInt("id"));
                categorie.setLibelle(rs.getString("libelle"));
                categorie.setCouleur(rs.getString("couleur"));

                return categorie;
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return null;
    }
}