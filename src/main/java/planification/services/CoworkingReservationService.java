package planification.services;

import planification.models.ReservationModel;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service layer handling seat-level coworking reservations with
 * basic transaction handling to prevent double booking.
 */
public class CoworkingReservationService {

    private final Connection cnx;

    public CoworkingReservationService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /**
     * Returns all reservations for a given espace and date.
     */
    public List<ReservationModel> getReservationsForDate(int espaceId, Date day) {
        List<ReservationModel> list = new ArrayList<>();
        String sql = "SELECT id, espace_id, chair_number, user_id, reservation_date, created_at " +
                "FROM coworking_reservations WHERE espace_id = ? AND reservation_date = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, espaceId);
            ps.setDate(2, new java.sql.Date(day.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationModel r = new ReservationModel();
                    r.setId(rs.getInt("id"));
                    r.setEspaceId(rs.getInt("espace_id"));
                    r.setChairNumber(rs.getInt("chair_number"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setReservationDate(rs.getDate("reservation_date"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    r.setCreatedAt(ts != null ? new Date(ts.getTime()) : null);
                    list.add(r);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Erreur lors du chargement des réservations de coworking", ex);
        }
        return list;
    }

    /**
     * Attempts to reserve a given chair for a user and date.
     * Uses a transaction and row-level locking to prevent double booking.
     */
    public void reserveChair(int espaceId, int chairNumber, int userId, Date day) {
        String lockSql = "SELECT id, user_id FROM coworking_reservations " +
                "WHERE espace_id = ? AND chair_number = ? AND reservation_date = ? FOR UPDATE";
        String insertSql = "INSERT INTO coworking_reservations " +
                "(espace_id, chair_number, user_id, reservation_date, created_at) " +
                "VALUES (?,?,?,?,?)";
        boolean oldAutoCommit;
        try {
            oldAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            try (PreparedStatement lockStmt = cnx.prepareStatement(lockSql)) {
                lockStmt.setInt(1, espaceId);
                lockStmt.setInt(2, chairNumber);
                lockStmt.setDate(3, new java.sql.Date(day.getTime()));
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (rs.next()) {
                        int existingUserId = rs.getInt("user_id");
                        throw new IllegalStateException("Cette chaise est déjà réservée (userId=" + existingUserId + ").");
                    }
                }
            }

            try (PreparedStatement insertStmt = cnx.prepareStatement(insertSql)) {
                insertStmt.setInt(1, espaceId);
                insertStmt.setInt(2, chairNumber);
                insertStmt.setInt(3, userId);
                insertStmt.setDate(4, new java.sql.Date(day.getTime()));
                insertStmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                insertStmt.executeUpdate();
            }

            cnx.commit();
            cnx.setAutoCommit(oldAutoCommit);
        } catch (Exception ex) {
            try {
                cnx.rollback();
            } catch (SQLException ignored) {
            }
            throw new RuntimeException("Impossible de réserver la chaise " + chairNumber, ex);
        }
    }

    /**
     * Cancels a reservation for the given user / seat / date.
     */
    public void cancelChair(int espaceId, int chairNumber, int userId, Date day) {
        String sql = "DELETE FROM coworking_reservations " +
                "WHERE espace_id = ? AND chair_number = ? AND user_id = ? AND reservation_date = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, espaceId);
            ps.setInt(2, chairNumber);
            ps.setInt(3, userId);
            ps.setDate(4, new java.sql.Date(day.getTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Impossible d'annuler la réservation de la chaise " + chairNumber, ex);
        }
    }

    /**
     * Cancels all reservations of a given user for an espace and date.
     */
    public void cancelAllForUser(int espaceId, int userId, Date day) {
        String sql = "DELETE FROM coworking_reservations " +
                "WHERE espace_id = ? AND user_id = ? AND reservation_date = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, espaceId);
            ps.setInt(2, userId);
            ps.setDate(3, new java.sql.Date(day.getTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Impossible d'annuler les réservations de l'utilisateur", ex);
        }
    }
}

