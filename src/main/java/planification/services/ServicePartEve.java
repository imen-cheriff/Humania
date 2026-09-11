package planification.services;

import planification.interfaces.IService;
import planification.models.ParticipEven;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePartEve implements IService<ParticipEven> {

    private Connection cnx;

    public ServicePartEve() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(ParticipEven participEven) {
        participEven.valider();

        String req ="INSERT INTO `participation_evenement`(`idEvenement`, `idEmploye`, `dateParticipation`, `statut`, `creeLe`) VALUES (?,?,?,?,?)";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, participEven.getIdEvenement());
            pstm.setInt(2, participEven.getIdEmploye());
            pstm.setDate(3, new Date(participEven.getDateParticipation().getTime()));
            pstm.setBoolean(4, participEven.isStatut());
            pstm.setDate(5, new Date(participEven.getCreeLe().getTime()));

            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<ParticipEven> getAll() {
        List<ParticipEven> participations = new ArrayList<>();
        String req = "SELECT * FROM `participation_evenement`";
        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                ParticipEven participEven = new ParticipEven();
                participEven.setId(rs.getInt("id"));
                participEven.setIdEvenement(rs.getInt("idEvenement"));
                participEven.setIdEmploye(rs.getInt("idEmploye"));
                participEven.setDateParticipation(rs.getDate("dateParticipation"));
                participEven.setStatut(rs.getBoolean("statut"));
                participEven.setCreeLe(rs.getDate("creeLe"));
                participations.add(participEven);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return participations;
    }

    @Override
    public void update(ParticipEven participEven) {
        participEven.valider();
        String req = "UPDATE `participation_evenement` SET `idEvenement` = ?, `idEmploye` = ?, `dateParticipation` = ?, `statut` = ?, `creeLe` = ? WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, participEven.getIdEvenement());
            pstm.setInt(2, participEven.getIdEmploye());
            pstm.setDate(3, new Date(participEven.getDateParticipation().getTime()));
            pstm.setBoolean(4, participEven.isStatut());
            pstm.setDate(5, new Date(participEven.getCreeLe().getTime()));
            pstm.setInt(6, participEven.getId());

            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(ParticipEven participEven) {
        String req = "DELETE FROM `participation_evenement` WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, participEven.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Returns true if the employee is already registered for this event.
     */
    public boolean isAlreadyRegistered(int idEvenement, int idEmploye) {
        String req = "SELECT COUNT(*) FROM `participation_evenement` WHERE `idEvenement` = ? AND `idEmploye` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, idEvenement);
            pstm.setInt(2, idEmploye);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    /**
     * Cancels (deletes) a participation by event + employee IDs.
     * Returns true if a row was deleted.
     */
    public boolean cancelParticipation(int idEvenement, int idEmploye) {
        String req = "DELETE FROM `participation_evenement` WHERE `idEvenement` = ? AND `idEmploye` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, idEvenement);
            pstm.setInt(2, idEmploye);
            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}