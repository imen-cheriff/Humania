package planification.services;

import planification.interfaces.IService;
import planification.models.Evenement;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvenement implements IService <Evenement> {

    private Connection cnx ;
    public ServiceEvenement(){
        this.cnx = MyDataBase.getInstance().getCnx();
    }


    @Override
    public void add(Evenement evenement) {

        evenement.valider();

        String req ="INSERT INTO `evenement`(`titre`, `description`, `dateEvenement`, `dateHeureDebut`," +
                " `dateHeureFin`, `lieu`, `nbParticipantsMax`, `participantsInscrits`, `creePar`, `creeLe`)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setString(1, evenement.getTitre());
            pstm.setString(2, evenement.getDescription());
            pstm.setDate(3, new Date(evenement.getDateEvenement().getTime()));
            pstm.setTimestamp(4, new Timestamp(evenement.getDateHeureDebut().getTime()));
            pstm.setTimestamp(5, new Timestamp(evenement.getDateHeureFin().getTime()));
            pstm.setString(6, evenement.getLieu());
            pstm.setInt(7, evenement.getNbParticipantsMax());
            pstm.setString(8, evenement.getParticipantsInscrits());
            pstm.setString(9, evenement.getCreePar());
            pstm.setDate(10, new Date(evenement.getCreeLe().getTime()));

            pstm.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<Evenement> getAll() {

        List<Evenement> evenements = new ArrayList<>();

        String req ="SELECT * FROM `evenement`";
        try {

            Statement stm  =  this.cnx.createStatement();

            ResultSet rs  =  stm.executeQuery(req);

            while (rs.next()) {
                Evenement evenement = new Evenement();
                evenement.setId(rs.getInt("id"));
                evenement.setTitre(rs.getString("titre"));
                evenement.setDescription(rs.getString("description"));
                evenement.setDateEvenement(rs.getDate("dateEvenement"));
                Timestamp tsDebut = rs.getTimestamp("dateHeureDebut");
                evenement.setDateHeureDebut(tsDebut != null ? new Date(tsDebut.getTime()) : null);
                Timestamp tsFin = rs.getTimestamp("dateHeureFin");
                evenement.setDateHeureFin(tsFin != null ? new Date(tsFin.getTime()) : null);
                evenement.setLieu(rs.getString("lieu"));
                evenement.setNbParticipantsMax(rs.getInt("nbParticipantsMax"));
                evenement.setParticipantsInscrits(rs.getString("participantsInscrits"));
                evenement.setCreePar(rs.getString("creePar"));
                evenement.setCreeLe(rs.getDate("creeLe"));
                evenements.add(evenement);
            }

        }catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return evenements;
    }

    @Override
    public void update(Evenement evenement) {
        evenement.valider();
        String req = "UPDATE `evenement` SET `titre` = ?, `description` = ?, `dateEvenement` = ?," +
                " `dateHeureDebut` = ?, `dateHeureFin` = ?, `lieu` = ?, `nbParticipantsMax` = ?, " +
                "`participantsInscrits` = ?, `creePar` = ?, `creeLe` = ? WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, evenement.getTitre());
            pstm.setString(2, evenement.getDescription());
            pstm.setDate(3, new Date(evenement.getDateEvenement().getTime()));
            pstm.setTimestamp(4, new Timestamp(evenement.getDateHeureDebut().getTime()));
            pstm.setTimestamp(5, new Timestamp(evenement.getDateHeureFin().getTime()));
            pstm.setString(6, evenement.getLieu());
            pstm.setInt(7, evenement.getNbParticipantsMax());
            pstm.setString(8, evenement.getParticipantsInscrits());
            pstm.setString(9, evenement.getCreePar());
            pstm.setDate(10, new Date(evenement.getCreeLe().getTime()));
            pstm.setInt(11, evenement.getId());

            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Evenement evenement) {
        String req = "DELETE FROM `evenement` WHERE `id` = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, evenement.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
