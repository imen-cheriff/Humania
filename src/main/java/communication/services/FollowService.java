package communication.services;

import communication.models.Follow;
import communication.models.User;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class FollowService {

    private Connection cnx;

    public FollowService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // ===== ADD FOLLOW =====
    // DB columns: followerId, followingId, followedAt, status
    // Always inserts as PENDING — target user must accept
    public void add(Follow follow) {
        // Check if already following (any status)
        if (isFollowing(follow.getFollowerId(), follow.getFollowedId())) {
            System.out.println("⚠️ Follow déjà existant : " + follow.getFollowerId() + " → " + follow.getFollowedId());
            return;
        }

        // ✅ FIX : status ACTIVE si le follower est manager
        User follower = new UserService().findById(follow.getFollowerId());
        String status = (follower != null && follower.isManager()) ? Follow.ACTIVE : Follow.PENDING;

        String req = "INSERT INTO follow (followerId, followingId, followedAt, status) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setInt(1, follow.getFollowerId());
            pstm.setInt(2, follow.getFollowedId()); // Java field: followedId → DB col: followingId
            pstm.setTimestamp(3, Timestamp.valueOf(
                    follow.getFollowedAt() != null ? follow.getFollowedAt() : LocalDateTime.now()));
            pstm.setString(4, status);

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                follow.setId(rs.getInt(1));
            }
            follow.setStatus(status);

            System.out.println("✅ Follow : " + follow.getFollowerId() + " → " + follow.getFollowedId() + " [" + status + "]");

        } catch (SQLException e) {
            System.err.println("❌ Erreur follow : " + e.getMessage());
        }
    }

    // ===== UNFOLLOW =====
    public void unfollow(int followerId, int followedId) {
        String req = "DELETE FROM follow WHERE followerId = ? AND followingId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, followerId);
            pstm.setInt(2, followedId);
            int rows = pstm.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Unfollow : " + followerId + " → " + followedId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur unfollow : " + e.getMessage());
        }
    }

    // ===== ACCEPT FOLLOW =====
    public void acceptFollow(int followerId, int followedId) {
        String req = "UPDATE follow SET status = 'ACTIVE' WHERE followerId = ? AND followingId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, followerId);
            pstm.setInt(2, followedId);
            int rows = pstm.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Follow accepté : " + followerId + " → " + followedId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur acceptFollow : " + e.getMessage());
        }
    }

    // ===== REJECT FOLLOW =====
    public void rejectFollow(int followerId, int followedId) {
        String req = "DELETE FROM follow WHERE followerId = ? AND followingId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, followerId);
            pstm.setInt(2, followedId);
            int rows = pstm.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Follow rejeté : " + followerId + " → " + followedId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur rejectFollow : " + e.getMessage());
        }
    }

    // ===== REMOVE FOLLOWER =====
    // Allows a user to remove someone who follows them
    public void removeFollower(int followerId, int followedId) {
        String req = "DELETE FROM follow WHERE followerId = ? AND followingId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, followerId);
            pstm.setInt(2, followedId);
            int rows = pstm.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Follower supprimé : " + followerId + " → " + followedId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur removeFollower : " + e.getMessage());
        }
    }

    // ===== IS FOLLOWING (any status: ACTIVE or PENDING) =====
    public boolean isFollowing(int followerId, int followedId) {
        String req = "SELECT COUNT(*) as cnt FROM follow WHERE followerId = ? AND followingId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, followerId);
            pstm.setInt(2, followedId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur isFollowing : " + e.getMessage());
        }
        return false;
    }

    // ===== IS ACTIVE FOLLOWING (only ACTIVE status) =====
    public boolean isActiveFollowing(int followerId, int followedId) {
        String req = "SELECT COUNT(*) as cnt FROM follow WHERE followerId = ? AND followingId = ? AND status = 'ACTIVE'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, followerId);
            pstm.setInt(2, followedId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur isActiveFollowing : " + e.getMessage());
        }
        return false;
    }

    // ===== GET FOLLOW STATUS =====
    // Returns "ACTIVE", "PENDING", or null if not following
    public String getFollowStatus(int followerId, int followedId) {
        String req = "SELECT status FROM follow WHERE followerId = ? AND followingId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, followerId);
            pstm.setInt(2, followedId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getFollowStatus : " + e.getMessage());
        }
        return null;
    }

    // ===== GET FOLLOWING LIST (ACTIVE only) =====
    public List<Follow> findFollowing(int userId) {
        List<Follow> follows = new ArrayList<>();
        String req = "SELECT * FROM follow WHERE followerId = ? AND status = 'ACTIVE'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                follows.add(mapResultSetToFollow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findFollowing : " + e.getMessage());
        }
        return follows;
    }

    // ===== GET FOLLOWERS LIST (ACTIVE only) =====
    public List<Follow> findFollowers(int userId) {
        List<Follow> follows = new ArrayList<>();
        String req = "SELECT * FROM follow WHERE followingId = ? AND status = 'ACTIVE'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                follows.add(mapResultSetToFollow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findFollowers : " + e.getMessage());
        }
        return follows;
    }

    // ===== GET PENDING FOLLOWERS =====
    public List<Follow> findPendingFollowers(int userId) {
        List<Follow> follows = new ArrayList<>();
        String req = "SELECT * FROM follow WHERE followingId = ? AND status = 'PENDING'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                follows.add(mapResultSetToFollow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findPendingFollowers : " + e.getMessage());
        }
        return follows;
    }

    // ===== COUNT FOLLOWERS (ACTIVE only) =====
    public int countFollowers(int userId) {
        String req = "SELECT COUNT(*) as cnt FROM follow WHERE followingId = ? AND status = 'ACTIVE'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur countFollowers : " + e.getMessage());
        }
        return 0;
    }

    // ===== COUNT FOLLOWING (ACTIVE only) =====
    public int countFollowing(int userId) {
        String req = "SELECT COUNT(*) as cnt FROM follow WHERE followerId = ? AND status = 'ACTIVE'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur countFollowing : " + e.getMessage());
        }
        return 0;
    }

    // ===== COUNT PENDING FOLLOWERS =====
    public int countPendingFollowers(int userId) {
        String req = "SELECT COUNT(*) as cnt FROM follow WHERE followingId = ? AND status = 'PENDING'";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur countPendingFollowers : " + e.getMessage());
        }
        return 0;
    }

    // ===== MAPPER =====
    // DB col 'followingId' → Java field 'followedId' (via setFollowedId)
    private Follow mapResultSetToFollow(ResultSet rs) throws SQLException {
        Follow follow = new Follow();
        follow.setId(rs.getInt("id"));
        follow.setFollowerId(rs.getInt("followerId"));
        follow.setFollowedId(rs.getInt("followingId")); // map DB col to Java field
        Timestamp ts = rs.getTimestamp("followedAt");
        if (ts != null) {
            follow.setFollowedAt(ts.toLocalDateTime());
        }
        try {
            follow.setStatus(rs.getString("status"));
        } catch (SQLException e) {
            follow.setStatus(Follow.ACTIVE); // fallback for old rows without status
        }
        return follow;
    }
}
