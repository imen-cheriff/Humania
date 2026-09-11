package competence.services;

import competence.interfaces.Service;
import competence.models.ActionPDI;
import competence.enums.TypeAction;
import competence.enums.Statut;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesActionPDI implements Service<ActionPDI> {

    private Connection cnx;

    public ServicesActionPDI() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(ActionPDI action) {
        String req = "INSERT INTO `actionPDI`(`typeAction`, `statut`, `dateDebut`, `dateFinPrevue`, " +
                "`priorite`, `pdi_id`) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, action.getTypeAction().name());
            pstm.setString(2, action.getStatut().name());
            pstm.setDate(3, action.getDateDebut());
            pstm.setDate(4, action.getDateFinPrevue());
            pstm.setInt(5, action.getPriorite());
            pstm.setInt(6, action.getPdi() != null ? action.getPdi().getId() : 0);

            pstm.executeUpdate();
            System.out.println("Action PDI ajoutée");

        } catch (SQLException e) {
            System.out.println("Erreur ajout action PDI: " + e.getMessage());
        }
    }

    @Override
    public List<ActionPDI> getAll() {
        List<ActionPDI> actions = new ArrayList<>();
        String req = "SELECT * FROM `actionPDI`";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                ActionPDI action = new ActionPDI();
                action.setId(rs.getInt("id"));
                action.setTypeAction(TypeAction.valueOf(rs.getString("typeAction")));
                action.setStatut(Statut.valueOf(rs.getString("statut")));
                action.setDateDebut(rs.getDate("dateDebut"));
                action.setDateFinPrevue(rs.getDate("dateFinPrevue"));
                action.setPriorite(rs.getInt("priorite"));

                actions.add(action);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération actions PDI: " + e.getMessage());
        }

        return actions;
    }

    @Override
    public void update(ActionPDI action) {
        String req = "UPDATE `actionPDI` SET `typeAction`=?, `statut`=?, `dateDebut`=?, `dateFinPrevue`=?, " +
                "`priorite`=?, `pdi_id`=? WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, action.getTypeAction().name());
            pstm.setString(2, action.getStatut().name());
            pstm.setDate(3, action.getDateDebut());
            pstm.setDate(4, action.getDateFinPrevue());
            pstm.setInt(5, action.getPriorite());
            pstm.setInt(6, action.getPdi() != null ? action.getPdi().getId() : 0);
            pstm.setInt(7, action.getId());

            pstm.executeUpdate();
            System.out.println("Action PDI modifiée");

        } catch (SQLException e) {
            System.out.println("Erreur modification action PDI: " + e.getMessage());
        }
    }

    @Override
    public void delete(ActionPDI action) {
        String req = "DELETE FROM `actionPDI` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, action.getId());

            pstm.executeUpdate();
            System.out.println("Action PDI supprimée");

        } catch (SQLException e) {
            System.out.println("Erreur suppression action PDI: " + e.getMessage());
        }
    }

    public ActionPDI getById(int id) {
        String req = "SELECT * FROM `actionPDI` WHERE `id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                ActionPDI action = new ActionPDI();
                action.setId(rs.getInt("id"));
                action.setTypeAction(TypeAction.valueOf(rs.getString("typeAction")));
                action.setStatut(Statut.valueOf(rs.getString("statut")));
                action.setDateDebut(rs.getDate("dateDebut"));
                action.setDateFinPrevue(rs.getDate("dateFinPrevue"));
                action.setPriorite(rs.getInt("priorite"));

                return action;
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return null;
    }

    public List<ActionPDI> getByPdiId(int pdiId) {
        List<ActionPDI> actions = new ArrayList<>();
        String req = "SELECT * FROM `actionPDI` WHERE `pdi_id`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, pdiId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                ActionPDI action = new ActionPDI();
                action.setId(rs.getInt("id"));
                action.setTypeAction(TypeAction.valueOf(rs.getString("typeAction")));
                action.setStatut(Statut.valueOf(rs.getString("statut")));
                action.setDateDebut(rs.getDate("dateDebut"));
                action.setDateFinPrevue(rs.getDate("dateFinPrevue"));
                action.setPriorite(rs.getInt("priorite"));

                actions.add(action);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return actions;
    }

    public List<ActionPDI> getByStatut(Statut statut) {
        List<ActionPDI> actions = new ArrayList<>();
        String req = "SELECT * FROM `actionPDI` WHERE `statut`=?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, statut.name());
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                ActionPDI action = new ActionPDI();
                action.setId(rs.getInt("id"));
                action.setTypeAction(TypeAction.valueOf(rs.getString("typeAction")));
                action.setStatut(Statut.valueOf(rs.getString("statut")));
                action.setDateDebut(rs.getDate("dateDebut"));
                action.setDateFinPrevue(rs.getDate("dateFinPrevue"));
                action.setPriorite(rs.getInt("priorite"));

                actions.add(action);
            }

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return actions;
    }
}