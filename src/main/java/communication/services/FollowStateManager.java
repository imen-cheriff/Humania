package communication.services;

import communication.models.Follow;

import java.util.HashSet;
import java.util.Set;

/**
 * Singleton — centralized follow state cache with ACTIVE/PENDING support.
 * Both MainController (sidebar) and UserProfileController check
 * this so follow/unfollow is always in sync across the UI.
 */
public class FollowStateManager {

    private static FollowStateManager instance;

    /** Set of userId values that the current user ACTIVELY follows. */
    private final Set<Integer> followingIds = new HashSet<>();

    /** Set of userId values where follow request is PENDING. */
    private final Set<Integer> pendingIds = new HashSet<>();

    /**
     * BUG-2 guard: prevents loadFromDb from executing more than once.
     * Use forceReload() if an explicit refresh is needed.
     */
    private boolean isLoaded = false;

    private FollowStateManager() {
    }

    public static FollowStateManager getInstance() {
        if (instance == null) {
            instance = new FollowStateManager();
        }
        return instance;
    }

    // ===== STATE QUERIES =====

    /** Returns true if the current user actively follows targetUserId. */
    public boolean isFollowing(int targetUserId) {
        return followingIds.contains(targetUserId);
    }

    /** Returns true if the current user has a pending follow request to targetUserId. */
    public boolean isPending(int targetUserId) {
        return pendingIds.contains(targetUserId);
    }

    /** Returns true if any relationship exists (ACTIVE or PENDING). */
    public boolean isFollowingOrPending(int targetUserId) {
        return followingIds.contains(targetUserId) || pendingIds.contains(targetUserId);
    }

    // ===== STATE MUTATIONS =====

    /** Mark a new follow request as PENDING. */
    public void markFollowing(int targetUserId) {
        pendingIds.add(targetUserId);
    }

    /** Move from PENDING to ACTIVE (when accepted). */
    public void markActivated(int targetUserId) {
        pendingIds.remove(targetUserId);
        followingIds.add(targetUserId);
    }

    /** Remove from both sets (unfollow or reject). */
    public void markUnfollowing(int targetUserId) {
        followingIds.remove(targetUserId);
        pendingIds.remove(targetUserId);
    }

    /**
     * Initialise the cache from DB for a given current user.
     * Called ONCE at app start from MainController.initialize().
     * Subsequent calls are ignored (BUG-2 fix) — use forceReload() if needed.
     */
    public void loadFromDb(int currentUserId, FollowService followService) {
        if (isLoaded) {
            System.out.println("⚠️ [FollowStateManager] loadFromDb() called again — already loaded. Skipping."
                    + " (call forceReload() if an explicit refresh is required)");
            return;
        }
        followingIds.clear();
        pendingIds.clear();

        // Load ACTIVE follows
        followService.findFollowing(currentUserId)
                .forEach(f -> followingIds.add(f.getFollowedId()));

        // Load PENDING follows — use DB query via getFollowStatus for all users
        // We query all follow rows for this user (any status) and separate them
        for (Follow f : findAllFollowsForUser(currentUserId, followService)) {
            if (Follow.PENDING.equals(f.getStatus())) {
                pendingIds.add(f.getFollowedId());
            }
        }

        isLoaded = true;
        System.out.println("✅ FollowStateManager loaded " + followingIds.size()
                + " active + " + pendingIds.size() + " pending entries.");
    }

    /**
     * Helper: get ALL follow rows (any status) for a follower.
     */
    private java.util.List<Follow> findAllFollowsForUser(int userId, FollowService followService) {
        java.util.List<Follow> all = new java.util.ArrayList<>();
        try {
            java.sql.Connection cnx = utils.MyDataBase.getInstance().getCnx();
            java.sql.PreparedStatement pstm = cnx.prepareStatement(
                    "SELECT * FROM follow WHERE followerId = ?");
            pstm.setInt(1, userId);
            java.sql.ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                Follow f = new Follow();
                f.setFollowerId(rs.getInt("followerId"));
                f.setFollowedId(rs.getInt("followingId"));
                java.sql.Timestamp ts = rs.getTimestamp("followedAt");
                if (ts != null) f.setFollowedAt(ts.toLocalDateTime());
                try {
                    f.setStatus(rs.getString("status"));
                } catch (Exception e) {
                    f.setStatus(Follow.ACTIVE);
                }
                all.add(f);
            }
        } catch (Exception e) {
            System.err.println("❌ [FollowStateManager] findAllFollowsForUser error: " + e.getMessage());
        }
        return all;
    }

    /**
     * Forces a reload from DB — use only when follow state must be refreshed
     * (e.g., after a user session change).
     */
    public void forceReload(int currentUserId, FollowService followService) {
        isLoaded = false;
        loadFromDb(currentUserId, followService);
    }
}
