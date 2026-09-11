package planification.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import planification.interfaces.IService;
import planification.models.Espace;
import utils.MyDataBase;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEspace implements IService<Espace> {

    private final Connection cnx;

    public ServiceEspace() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // ─────────────────────────────────────────────────────
    //  ADD  — inclut typeEspace dans l'INSERT
    // ─────────────────────────────────────────────────────
    @Override
    public void add(Espace espace) {
        espace.valider();
        Gson gson = new Gson();
        String equipmentJson = gson.toJson(espace.getListeEquipements());

        // ← typeEspace ajouté ici
        String req = "INSERT INTO `espaces`(`nom`, `capacite`, `etage`, `listeEquipements`, " +
                "`urlImage`, `disponible`, `typeEspace`) VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1,  espace.getNom());
            pstm.setInt(2,     espace.getCapacite());
            pstm.setInt(3,     espace.getEtage());
            pstm.setString(4,  equipmentJson);
            pstm.setString(5,  espace.getUrlImage());
            pstm.setBoolean(6, espace.isDisponible());
            pstm.setString(7,  espace.getTypeEspace() != null ? espace.getTypeEspace() : "reunion"); // ← param 7
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServiceEspace.add() : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    //  GET ALL  — lit typeEspace depuis la BDD
    // ─────────────────────────────────────────────────────
    @Override
    public List<Espace> getAll() {
        List<Espace> espaces = new ArrayList<>();
        String req = "SELECT * FROM `espaces`";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs  = stm.executeQuery(req);
            Gson gson     = new Gson();
            Type listType = new TypeToken<List<String>>(){}.getType();

            while (rs.next()) {

                // ── Équipements (JSON → List<String>) ────
                String equipementsJson = rs.getString("listeEquipements");
                List<String> equipements = new ArrayList<>();
                if (equipementsJson != null && !equipementsJson.trim().isEmpty()) {
                    try {
                        equipements = gson.fromJson(equipementsJson, listType);
                    } catch (Exception e) {
                        equipements.add(equipementsJson);
                    }
                }

                // ── typeEspace ← depuis la BDD ───────────
                // Supporte "typeEspace" (camelCase) et "type_espace" (snake_case)
                String typeEspace = readTypeEspace(rs);

                Espace espace = new Espace(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("capacite"),
                        rs.getInt("etage"),
                        equipements,
                        rs.getString("urlImage"),
                        rs.getBoolean("disponible"),
                        typeEspace  // ← maintenant passé au constructeur
                );
                espaces.add(espace);
            }
        } catch (SQLException e) {
            System.out.println("ServiceEspace.getAll() : " + e.getMessage());
        }
        return espaces;
    }

    /** Lit typeEspace depuis le ResultSet (typeEspace ou type_espace). */
    private String readTypeEspace(ResultSet rs) {
        String val = null;
        try {
            val = rs.getString("typeEspace");
        } catch (SQLException e) {
            try {
                val = rs.getString("type_espace");
            } catch (SQLException ignored) {}
        }
        if (val != null && !val.isBlank()) {
            return val.trim().toLowerCase();
        }
        return "reunion";
    }

    // ─────────────────────────────────────────────────────
    //  UPDATE  — inclut typeEspace dans le SET
    // ─────────────────────────────────────────────────────
    @Override
    public void update(Espace espace) {
        espace.valider();
        Gson gson = new Gson();
        String equipmentJson = gson.toJson(espace.getListeEquipements());

        // ← typeEspace ajouté ici
        String req = "UPDATE `espaces` SET `nom` = ?, `capacite` = ?, `etage` = ?, " +
                "`listeEquipements` = ?, `urlImage` = ?, `disponible` = ?, " +
                "`typeEspace` = ? WHERE `id` = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1,  espace.getNom());
            pstm.setInt(2,     espace.getCapacite());
            pstm.setInt(3,     espace.getEtage());
            pstm.setString(4,  equipmentJson);
            pstm.setString(5,  espace.getUrlImage());
            pstm.setBoolean(6, espace.isDisponible());
            pstm.setString(7,  espace.getTypeEspace() != null ? espace.getTypeEspace() : "reunion"); // ← param 7
            pstm.setInt(8,     espace.getId()); // ← décalé à 8
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServiceEspace.update() : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    //  DELETE  — inchangé
    // ─────────────────────────────────────────────────────
    @Override
    public void delete(Espace espace) {
        String req = "DELETE FROM `espaces` WHERE `id` = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, espace.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServiceEspace.delete() : " + e.getMessage());
        }
    }
}