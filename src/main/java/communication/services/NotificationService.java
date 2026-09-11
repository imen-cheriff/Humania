package communication.services;

import communication.interfaces.Service;
import communication.models.Notification;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService implements Service<Notification> {

    private Connection cnx;

    public NotificationService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Notification notification) {
        String req = "INSERT INTO NOTIFICATION (titre, message, type, userId, seen, dateCreation, relatedUserId, relatedPublicationId, relatedCommentaireId) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, notification.getTitre());
            pstm.setString(2, notification.getMessage());
            pstm.setString(3, notification.getType());
            pstm.setInt(4, notification.getUserId());
            pstm.setBoolean(5, notification.getSeen() != null ? notification.getSeen() : false);
            pstm.setTimestamp(6, Timestamp.valueOf(notification.getDateCreation()));

            // Relations optionnelles
            if (notification.getRelatedUserId() != null) {
                pstm.setInt(7, notification.getRelatedUserId());
            } else {
                pstm.setNull(7, Types.INTEGER);
            }

            if (notification.getRelatedPublicationId() != null) {
                pstm.setInt(8, notification.getRelatedPublicationId());
            } else {
                pstm.setNull(8, Types.INTEGER);
            }

            if (notification.getRelatedCommentaireId() != null) {
                pstm.setInt(9, notification.getRelatedCommentaireId());
            } else {
                pstm.setNull(9, Types.INTEGER);
            }

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                notification.setId(rs.getInt(1));
            }

            System.out.println("✅ Notification créée : " + notification.getTitre());

        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout notification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Notification> getAll() {
        List<Notification> notifications = new ArrayList<>();
        String req = "SELECT * FROM NOTIFICATION ORDER BY dateCreation DESC";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération notifications : " + e.getMessage());
        }

        return notifications;
    }

    @Override
    public void update(Notification notification) {
        String req = "UPDATE NOTIFICATION SET titre = ?, message = ?, type = ?, seen = ?, dateViewAt = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, notification.getTitre());
            pstm.setString(2, notification.getMessage());
            pstm.setString(3, notification.getType());
            pstm.setBoolean(4, notification.getSeen());

            if (notification.getDateViewAt() != null) {
                pstm.setTimestamp(5, Timestamp.valueOf(notification.getDateViewAt()));
            } else {
                pstm.setNull(5, Types.TIMESTAMP);
            }

            pstm.setInt(6, notification.getId());

            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour notification : " + e.getMessage());
        }
    }

    @Override
    public void delete(Notification notification) {
        String req = "DELETE FROM NOTIFICATION WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, notification.getId());
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression notification : " + e.getMessage());
        }
    }

    // ===== MÉTHODES SPÉCIALISÉES =====

    public List<Notification> findByUserId(int userId) {
        List<Notification> notifications = new ArrayList<>();
        String req = "SELECT * FROM NOTIFICATION WHERE userId = ? ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findByUserId notifications : " + e.getMessage());
        }

        return notifications;
    }

    public List<Notification> findUnreadByUserId(int userId) {
        List<Notification> notifications = new ArrayList<>();
        String req = "SELECT * FROM NOTIFICATION WHERE userId = ? AND seen = 0 ORDER BY dateCreation DESC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findUnreadByUserId : " + e.getMessage());
        }

        return notifications;
    }

    public int countUnread(int userId) {
        String req = "SELECT COUNT(*) as count FROM NOTIFICATION WHERE userId = ? AND seen = 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur countUnread : " + e.getMessage());
        }

        return 0;
    }

    public void markAsRead(int notificationId) {
        String req = "UPDATE NOTIFICATION SET seen = 1, dateViewAt = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstm.setInt(2, notificationId);
            pstm.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur markAsRead : " + e.getMessage());
        }
    }

    public void markAllAsRead(int userId) {
        String req = "UPDATE NOTIFICATION SET seen = 1, dateViewAt = ? WHERE userId = ? AND seen = 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstm.setInt(2, userId);

            int rowsAffected = pstm.executeUpdate();
            System.out.println("✅ " + rowsAffected + " notifications marquées lues");

        } catch (SQLException e) {
            System.err.println("❌ Erreur markAllAsRead : " + e.getMessage());
        }
    }

    // ===== MAPPER =====
    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notif = new Notification();
        notif.setId(rs.getInt("id"));
        notif.setTitre(rs.getString("titre"));
        notif.setMessage(rs.getString("message"));
        notif.setType(rs.getString("type"));
        notif.setUserId(rs.getInt("userId"));
        notif.setSeen(rs.getBoolean("seen"));

        Timestamp dateCreation = rs.getTimestamp("dateCreation");
        if (dateCreation != null) {
            notif.setDateCreation(dateCreation.toLocalDateTime());
        }

        Timestamp dateViewAt = rs.getTimestamp("dateViewAt");
        if (dateViewAt != null) {
            notif.setDateViewAt(dateViewAt.toLocalDateTime());
        }

        // Relations optionnelles
        notif.setRelatedUserId(rs.getObject("relatedUserId", Integer.class));
        notif.setRelatedPublicationId(rs.getObject("relatedPublicationId", Integer.class));
        notif.setRelatedCommentaireId(rs.getObject("relatedCommentaireId", Integer.class));

        return notif;
    }
}