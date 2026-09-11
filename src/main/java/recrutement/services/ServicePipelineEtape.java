package recrutement.services;

import recrutement.interfaces.Iservice;
import recrutement.models.PipelineEtape;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePipelineEtape implements Iservice<PipelineEtape> {

    private Connection cnx ;

    public ServicePipelineEtape(){
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(PipelineEtape etape) {
        String req ="INSERT INTO `pipeline_etape`(`id`, `poste_interne_id`, `ordre`, `libelle`, `description_etape`, `duree_moyenne`, `action_automatique`, `obligatoire`) VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,etape.getId());
            pstm.setInt(2,etape.getPosteInterneId());
            pstm.setInt(3,etape.getOrdre());
            pstm.setString(4,etape.getLibelle());
            pstm.setString(5,etape.getDescriptionEtape());
            pstm.setInt(6,etape.getDureeMoyenne());
            pstm.setString(7,etape.getActionAutomatique());
            pstm.setBoolean(8,etape.getObligatoire());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<PipelineEtape> getAll() {
        List<PipelineEtape> etapes = new ArrayList<>();
        String req ="SELECT * FROM `pipeline_etape` ";
        try {
            Statement stm =this.cnx.createStatement() ;
           ResultSet rs =  stm.executeQuery(req);
           while (rs.next()){
               PipelineEtape etape = new PipelineEtape();
               etape.setId(rs.getInt("id"));
               etape.setPosteInterneId(rs.getInt("poste_interne_id"));
               etape.setOrdre(rs.getInt("ordre"));
               etape.setLibelle(rs.getString("libelle"));
               etape.setDescriptionEtape(rs.getString("description_etape"));
               etape.setDureeMoyenne(rs.getInt("duree_moyenne"));
               etape.setActionAutomatique(rs.getString("action_automatique"));
               etape.setObligatoire(rs.getBoolean("obligatoire"));

               etapes.add(etape);
           }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return etapes;
    }

    @Override
    public void update(PipelineEtape etape) {
        String req ="UPDATE `pipeline_etape` SET `poste_interne_id`=?, `ordre`=?, `libelle`=?, `description_etape`=?, `duree_moyenne`=?, `action_automatique`=?, `obligatoire`=? WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,etape.getPosteInterneId());
            pstm.setInt(2,etape.getOrdre());
            pstm.setString(3,etape.getLibelle());
            pstm.setString(4,etape.getDescriptionEtape());
            pstm.setInt(5,etape.getDureeMoyenne());
            pstm.setString(6,etape.getActionAutomatique());
            pstm.setBoolean(7,etape.getObligatoire());
            pstm.setInt(8,etape.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(PipelineEtape etape) {
        String req ="DELETE FROM `pipeline_etape` WHERE `id`=?";
        try {
            PreparedStatement pstm  =  this.cnx.prepareStatement(req);
            pstm.setInt(1,etape.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
