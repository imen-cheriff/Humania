// ═══════════════════════════════════════════════════════════════════════
// MERGE: communication.models.UserProfile ← utilisateur.models.Utilisateur
// WHAT CHANGED: Added Utilisateur reference as single source of truth.
//   avatarUrl now delegates to Utilisateur.pdp.
//   Added delegation helpers: getFullName, getInitials, getUsername, etc.
//   jobTitle/company now delegate to Employe.posteActuel/departement.
//   location/website marked [DEPRECATED] — no real data source.
// WHY: UserProfile was provisional, duplicating user identity data.
// IMPACT: UserProfileService, UserProfileController, PostCardController,
//   ChatController, FeedController, NetworkController
// ═══════════════════════════════════════════════════════════════════════
package communication.models;

import utilisateur.models.Employe;
import utilisateur.models.Utilisateur;

public class UserProfile {
    private int id;

    // [DEPRECATED - kept for reference] was: private int userId;
    // MERGE: now delegates to utilisateur.getId() when available
    private int userId;

    // MERGE: single source of truth for user identity
    private Utilisateur utilisateur;

    private String bio;
    private String avatarUrl; // MERGE: delegates to utilisateur.pdp when available
    private String coverUrl;

    // [DEPRECATED - kept for reference] no real data source in the system
    private String location;
    // [DEPRECATED - kept for reference] no real data source in the system
    private String website;
    // [DEPRECATED - kept for reference] now delegates to Employe.departement
    private String company;
    // [DEPRECATED - kept for reference] now delegates to Employe.posteActuel
    private String jobTitle;

    private int followersCount;
    private int followingCount;
    private int postsCount;

    public UserProfile() {
        this.followersCount = 0;
        this.followingCount = 0;
        this.postsCount = 0;
    }

    // ADDED FOR MERGE: constructor that injects the real Utilisateur
    public UserProfile(Utilisateur utilisateur) {
        this();
        this.utilisateur = utilisateur;
        if (utilisateur != null) {
            this.userId = utilisateur.getId();
        }
    }

    // ── MERGE: Utilisateur reference ────────────────────────────────

    // ADDED FOR MERGE: inject Utilisateur for identity delegation
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        if (utilisateur != null) {
            this.userId = utilisateur.getId();
        }
    }

    // ── MERGE: Delegated getters from Utilisateur (convenience) ─────

    // ADDED FOR MERGE: used by PostCardController, FeedController
    public String getFullName() {
        return utilisateur != null ? utilisateur.getFullName() : "";
    }

    // ADDED FOR MERGE: used for avatar initials
    public String getInitials() {
        return utilisateur != null ? utilisateur.getInitials() : "";
    }

    // ADDED FOR MERGE: convenience delegate
    public String getUsername() {
        return utilisateur != null ? utilisateur.getUsername() : "";
    }

    // ADDED FOR MERGE: convenience delegate
    public String getEmail() {
        return utilisateur != null ? utilisateur.getEmail() : "";
    }

    // ADDED FOR MERGE: convenience delegate
    public String getNom() {
        return utilisateur != null ? utilisateur.getNom() : "";
    }

    // ADDED FOR MERGE: convenience delegate
    public String getPrenom() {
        return utilisateur != null ? utilisateur.getPrenom() : "";
    }

    // ADDED FOR MERGE: convenience delegate
    public String getRoleAsString() {
        return utilisateur != null ? utilisateur.getRoleAsString() : "EMPLOYE";
    }

    // ADDED FOR MERGE: convenience delegate
    public boolean isManager() {
        return utilisateur != null && utilisateur.isManager();
    }

    // ADDED FOR MERGE: convenience delegate
    public boolean isOnline() {
        return utilisateur != null && utilisateur.isOnline();
    }

    // ── MERGE: Delegated from Employe (job info) ────────────────────

    // ADDED FOR MERGE: delegates to Employe.posteActuel when available
    // Falls back to local jobTitle field for backward compat
    public String getJobTitle() {
        if (utilisateur instanceof Employe) {
            String poste = ((Employe) utilisateur).getPosteActuel();
            if (poste != null) return poste;
        }
        return jobTitle; // MERGE: fallback to local field
    }

    // ADDED FOR MERGE: delegates to Employe.departement when available
    // Falls back to local company field for backward compat
    public String getCompany() {
        if (utilisateur instanceof Employe) {
            String dept = ((Employe) utilisateur).getDepartement();
            if (dept != null) return dept;
        }
        return company; // MERGE: fallback to local field
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        // MERGE: prefer utilisateur.getId() when available
        if (utilisateur != null) return utilisateur.getId();
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        // MERGE: prefer utilisateur.pdp when available
        if (utilisateur != null && utilisateur.getPdp() != null) {
            return utilisateur.getPdp();
        }
        return avatarUrl; // MERGE: fallback to local field
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        // MERGE: sync to utilisateur.pdp
        if (utilisateur != null) {
            utilisateur.setPdp(avatarUrl);
        }
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    // [DEPRECATED - kept for reference] no real data source
    public String getLocation() {
        return location;
    }

    // [DEPRECATED - kept for reference] no real data source
    public void setLocation(String location) {
        this.location = location;
    }

    // [DEPRECATED - kept for reference] no real data source
    public String getWebsite() {
        return website;
    }

    // [DEPRECATED - kept for reference] no real data source
    public void setWebsite(String website) {
        this.website = website;
    }

    // [DEPRECATED - kept for reference] use getCompany() delegation instead
    public void setCompany(String company) {
        this.company = company;
    }

    // [DEPRECATED - kept for reference] use getJobTitle() delegation instead
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public int getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(int postsCount) {
        this.postsCount = postsCount;
    }

    @Override
    public String toString() {
        return "UserProfile{id=" + id + ", userId=" + getUserId() +
                ", name=" + getFullName() + // MERGE: added
                ", followersCount=" + followersCount +
                ", followingCount=" + followingCount +
                ", postsCount=" + postsCount + '}';
    }
}