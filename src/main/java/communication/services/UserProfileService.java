// ═══════════════════════════════════════════════════════════════════════
// MERGE: communication.services.UserProfileService
// WHAT CHANGED: Added UserService dependency to inject Utilisateur into
//   UserProfile objects. mapResultSetToUserProfile() now loads the real
//   Utilisateur and calls profile.setUtilisateur(). updateAvatarUrl()
//   now also syncs to utilisateur.pdp.
// WHY: UserProfile now delegates identity to Utilisateur (single source
//   of truth). Profiles need the real user injected to work correctly.
// IMPACT: All controllers that load UserProfile via this service.
// ═══════════════════════════════════════════════════════════════════════
package communication.services;

import communication.models.User; // MERGE: needed for UserService.findById()
import communication.models.UserProfile;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserProfileService {

    private Connection cnx;
    // ADDED FOR MERGE: used to load Utilisateur and inject into UserProfile
    private UserService userService;

    public UserProfileService() {
        this.cnx = MyDataBase.getInstance().getCnx();
        this.userService = new UserService(); // MERGE: inject UserService
    }

    // ===== CREATE =====
    public void add(UserProfile profile) {
        String req = "INSERT INTO userprofile (userId, bio, avatarUrl, coverUrl, location, website, company, jobTitle, followersCount, followingCount, postsCount) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setInt(1, profile.getUserId());
            pstm.setString(2, profile.getBio());
            pstm.setString(3, profile.getAvatarUrl());
            pstm.setString(4, profile.getCoverUrl());
            pstm.setString(5, profile.getLocation());
            pstm.setString(6, profile.getWebsite());
            pstm.setString(7, profile.getCompany());
            pstm.setString(8, profile.getJobTitle());
            pstm.setInt(9, profile.getFollowersCount());
            pstm.setInt(10, profile.getFollowingCount());
            pstm.setInt(11, profile.getPostsCount());

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                profile.setId(rs.getInt(1));
            }

            System.out.println("✅ UserProfile créé : " + profile.getUserId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création du profil : " + e.getMessage());
        }
    }

    // ===== READ =====
    public List<UserProfile> getAll() {
        List<UserProfile> profiles = new ArrayList<>();
        String req = "SELECT * FROM userprofile";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                profiles.add(mapResultSetToUserProfile(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des profils : " + e.getMessage());
        }

        return profiles;
    }

    public UserProfile findById(int id) {
        String req = "SELECT * FROM userprofile WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToUserProfile(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche du profil : " + e.getMessage());
        }

        return null;
    }

    public UserProfile findByUserId(int userId) {
        String req = "SELECT * FROM userprofile WHERE userId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToUserProfile(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche du profil par userId : " + e.getMessage());
        }

        return null;
    }

    // ===== UPDATE =====
    public void update(UserProfile profile) {
        String req = "UPDATE userprofile SET bio = ?, avatarUrl = ?, coverUrl = ?, location = ?, website = ?, company = ?, jobTitle = ?, followersCount = ?, followingCount = ?, postsCount = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, profile.getBio());
            pstm.setString(2, profile.getAvatarUrl());
            pstm.setString(3, profile.getCoverUrl());
            pstm.setString(4, profile.getLocation());
            pstm.setString(5, profile.getWebsite());
            pstm.setString(6, profile.getCompany());
            pstm.setString(7, profile.getJobTitle());
            pstm.setInt(8, profile.getFollowersCount());
            pstm.setInt(9, profile.getFollowingCount());
            pstm.setInt(10, profile.getPostsCount());
            pstm.setInt(11, profile.getId());

            pstm.executeUpdate();
            System.out.println("✅ UserProfile mis à jour : " + profile.getId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour du profil : " + e.getMessage());
        }
    }

    // ===== DELETE =====
    public void delete(UserProfile profile) {
        String req = "DELETE FROM userprofile WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, profile.getId());
            pstm.executeUpdate();
            System.out.println("✅ UserProfile supprimé : " + profile.getId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression du profil : " + e.getMessage());
        }
    }

    // ===== SPECIALIZED =====
    public void incrementFollowerCount(int userId) {
        executeUpdate("UPDATE userprofile SET followersCount = followersCount + 1 WHERE userId = ?", userId);
    }

    public void decrementFollowerCount(int userId) {
        executeUpdate("UPDATE userprofile SET followersCount = GREATEST(followersCount - 1, 0) WHERE userId = ?",
                userId);
    }

    public void incrementFollowingCount(int userId) {
        executeUpdate("UPDATE userprofile SET followingCount = followingCount + 1 WHERE userId = ?", userId);
    }

    public void decrementFollowingCount(int userId) {
        executeUpdate("UPDATE userprofile SET followingCount = GREATEST(followingCount - 1, 0) WHERE userId = ?",
                userId);
    }

    public void incrementPostsCount(int userId) {
        executeUpdate("UPDATE userprofile SET postsCount = postsCount + 1 WHERE userId = ?", userId);
    }

    public void decrementPostsCount(int userId) {
        executeUpdate("UPDATE userprofile SET postsCount = GREATEST(postsCount - 1, 0) WHERE userId = ?", userId);
    }

    public void updateBio(int userId, String bio) {
        String req = "UPDATE userprofile SET bio = ? WHERE userId = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, bio);
            pstm.setInt(2, userId);
            pstm.executeUpdate();
            System.out.println("✅ Bio mise à jour pour userId : " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur bio : " + e.getMessage());
        }
    }

    public void updateAvatarUrl(int userId, String avatarUrl) {
        // MERGE: update both userprofile.avatarUrl AND utilisateur.pdp
        String req = "UPDATE userprofile SET avatarUrl = ? WHERE userId = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, avatarUrl);
            pstm.setInt(2, userId);
            pstm.executeUpdate();

            // MERGE: sync avatar to utilisateur.pdp (single source of truth)
            String reqSync = "UPDATE utilisateur SET pdp = ? WHERE id = ?";
            PreparedStatement pstmSync = this.cnx.prepareStatement(reqSync);
            pstmSync.setString(1, avatarUrl);
            pstmSync.setInt(2, userId);
            pstmSync.executeUpdate();

            System.out.println("✅ Avatar mis à jour pour userId : " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur avatar : " + e.getMessage());
        }
    }

    public void updateCoverUrl(int userId, String coverUrl) {
        String req = "UPDATE userprofile SET coverUrl = ? WHERE userId = ?";
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, coverUrl);
            pstm.setInt(2, userId);
            pstm.executeUpdate();
            System.out.println("✅ CoverUrl mise à jour pour userId : " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur coverUrl : " + e.getMessage());
        }
    }

    // ===== PRIVATE HELPERS =====
    private void executeUpdate(String req, int userId) {
        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur update userprofile : " + e.getMessage());
        }
    }

    private UserProfile mapResultSetToUserProfile(ResultSet rs) throws SQLException {
        UserProfile profile = new UserProfile();
        profile.setId(rs.getInt("id"));
        int userId = rs.getInt("userId");
        profile.setUserId(userId);
        profile.setBio(rs.getString("bio"));
        profile.setAvatarUrl(rs.getString("avatarUrl"));
        profile.setCoverUrl(rs.getString("coverUrl"));
        // [DEPRECATED - kept for reference] these columns will be dropped later
        profile.setLocation(rs.getString("location"));
        // [DEPRECATED - kept for reference] no real data source
        profile.setWebsite(rs.getString("website"));
        // [DEPRECATED - kept for reference] now delegates to Employe.departement
        profile.setCompany(rs.getString("company"));
        // [DEPRECATED - kept for reference] now delegates to Employe.posteActuel
        profile.setJobTitle(rs.getString("jobTitle"));
        profile.setFollowersCount(rs.getInt("followersCount"));
        profile.setFollowingCount(rs.getInt("followingCount"));
        profile.setPostsCount(rs.getInt("postsCount"));

        // ADDED FOR MERGE: inject the real Utilisateur into the profile
        // This enables delegation: getFullName(), getInitials(), getAvatarUrl(), etc.
        User user = userService.findById(userId);
        if (user != null) {
            profile.setUtilisateur(user); // MERGE: single source of truth
        }

        return profile;
    }
}