package planification.services;

import planification.interfaces.IService;
import planification.models.ResevEspace;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReservEsp implements IService<ResevEspace> {

    private final Connection cnx;

    public ServiceReservEsp() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // ── INSERT ────────────────────────────────────────────
    @Override
    public void add(ResevEspace r) {
        r.valider();
        // Columns match the actual DB table (no numeroChaise column)
        String req = "INSERT INTO `reservation_espaces`" +
                "(`idEspace`, `idEmploye`, `dateReservation`, `dateHeureDebut`, " +
                "`dateHeureFin`, `objectif`, `statut`, `creeLe`) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1,       r.getIdEspace());
            pstm.setInt(2,       r.getIdEmploye());
            pstm.setDate(3,      new Date(r.getDateReservation().getTime()));
            pstm.setTimestamp(4, new Timestamp(r.getDateHeureDebut().getTime()));
            pstm.setTimestamp(5, new Timestamp(r.getDateHeureFin().getTime()));
            pstm.setString(6,    r.getObjectif());
            pstm.setBoolean(7,   r.isStatut());
            pstm.setDate(8,      new Date(r.getCreeLe().getTime()));
            pstm.executeUpdate();
        } catch (SQLException e) {
            // Rethrow so the UI can display the real error message
            throw new RuntimeException("Erreur lors de l'insertion : " + e.getMessage(), e);
        }
    }

    // ── SELECT ALL ────────────────────────────────────────
    @Override
    public List<ResevEspace> getAll() {
        List<ResevEspace> reservations = new ArrayList<>();
        String req = "SELECT * FROM `reservation_espaces`";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs  = stm.executeQuery(req);
            while (rs.next()) {
                ResevEspace r = new ResevEspace();
                r.setId(rs.getInt("id"));
                r.setIdEspace(rs.getInt("idEspace"));
                r.setIdEmploye(rs.getInt("idEmploye"));
                r.setDateReservation(rs.getDate("dateReservation"));

                Timestamp tsDebut = rs.getTimestamp("dateHeureDebut");
                r.setDateHeureDebut(tsDebut != null ? new java.util.Date(tsDebut.getTime()) : null);

                Timestamp tsFin = rs.getTimestamp("dateHeureFin");
                r.setDateHeureFin(tsFin != null ? new java.util.Date(tsFin.getTime()) : null);

                r.setObjectif(rs.getString("objectif"));
                r.setStatut(rs.getBoolean("statut"));
                r.setCreeLe(rs.getDate("creeLe"));

                // numeroChaise column does not exist in this DB

                reservations.add(r);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return reservations;
    }

    // ── UPDATE ────────────────────────────────────────────
    @Override
    public void update(ResevEspace r) {
        r.valider();
        String req = "UPDATE `reservation_espaces` SET " +
                "`idEspace` = ?, `idEmploye` = ?, `dateReservation` = ?, " +
                "`dateHeureDebut` = ?, `dateHeureFin` = ?, `objectif` = ?, " +
                "`statut` = ?, `creeLe` = ? WHERE `id` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1,       r.getIdEspace());
            pstm.setInt(2,       r.getIdEmploye());
            pstm.setDate(3,      new Date(r.getDateReservation().getTime()));
            pstm.setTimestamp(4, new Timestamp(r.getDateHeureDebut().getTime()));
            pstm.setTimestamp(5, new Timestamp(r.getDateHeureFin().getTime()));
            pstm.setString(6,    r.getObjectif());
            pstm.setBoolean(7,   r.isStatut());
            pstm.setDate(8,      new Date(r.getCreeLe().getTime()));
            pstm.setInt(9,       r.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour : " + e.getMessage(), e);
        }
    }

    // ── CONFLICT CHECK (salle entière uniquement) ─────────
    /**
     * Returns true if the espace already has a FULL-ROOM reservation (numeroChaise IS NULL)
     * whose time window overlaps [debut, fin[.
     * Overlap: existing.start < requested.fin AND existing.fin > requested.debut
     */
    public boolean hasConflict(int idEspace, java.util.Date debut, java.util.Date fin) {
        String sql =
                "SELECT COUNT(*) FROM `reservation_espaces` " +
                        "WHERE `idEspace` = ? " +
                        "  AND `statut` = TRUE " +
                        "  AND `dateHeureDebut` < ? " +
                        "  AND `dateHeureFin`   > ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEspace);
            ps.setTimestamp(2, new Timestamp(fin.getTime()));
            ps.setTimestamp(3, new Timestamp(debut.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("hasConflict error: " + e.getMessage());
        }
        return false;
    }

    // ── GET RESERVATIONS FOR A GIVEN ESPACE ON A GIVEN DATE ──
    /**
     * Returns all full-room reservations for the given espace on the given date.
     * Used to highlight the current user's own bookings in the slot grid.
     */
    public List<ResevEspace> getByEspaceAndDate(int idEspace, Date date) {
        List<ResevEspace> list = new ArrayList<>();
        String sql = "SELECT * FROM `reservation_espaces` " +
                "WHERE `idEspace` = ? " +
                "  AND `statut` = TRUE " +
                "  AND DATE(`dateHeureDebut`) = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEspace);
            ps.setDate(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ResevEspace r = new ResevEspace();
                    r.setId(rs.getInt("id"));
                    r.setIdEspace(rs.getInt("idEspace"));
                    r.setIdEmploye(rs.getInt("idEmploye"));
                    r.setDateReservation(rs.getDate("dateReservation"));
                    Timestamp tsD = rs.getTimestamp("dateHeureDebut");
                    r.setDateHeureDebut(tsD != null ? new java.util.Date(tsD.getTime()) : null);
                    Timestamp tsF = rs.getTimestamp("dateHeureFin");
                    r.setDateHeureFin(tsF != null ? new java.util.Date(tsF.getTime()) : null);
                    r.setObjectif(rs.getString("objectif"));
                    r.setStatut(rs.getBoolean("statut"));
                    r.setCreeLe(rs.getDate("creeLe"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.out.println("getByEspaceAndDate error: " + e.getMessage());
        }
        return list;
    }

    // ── DELETE ────────────────────────────────────────────
    @Override
    public void delete(ResevEspace r) {
        String req = "DELETE FROM `reservation_espaces` WHERE `id` = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, r.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}