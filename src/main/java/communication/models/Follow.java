package communication.models;

import java.time.LocalDateTime;

public class Follow {
    public static final String ACTIVE = "ACTIVE";
    public static final String PENDING = "PENDING";

    private Integer id;
    private Integer followerId;   // FK vers User (celui qui suit)
    private Integer followedId;   // FK vers User (celui suivi)
    private LocalDateTime followedAt;
    private String status;

    // Constructeurs
    public Follow() {}

    public Follow(Integer followerId, Integer followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
        this.followedAt = LocalDateTime.now();
        this.status = ACTIVE;
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Integer followerId) {
        this.followerId = followerId;
    }

    public Integer getFollowedId() {
        return followedId;
    }

    public void setFollowedId(Integer followedId) {
        this.followedId = followedId;
    }

    public LocalDateTime getFollowedAt() {
        return followedAt;
    }

    public void setFollowedAt(LocalDateTime followedAt) {
        this.followedAt = followedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Follow{" +
                "id=" + id +
                ", followerId=" + followerId +
                ", followedId=" + followedId +
                ", status='" + status + '\'' +
                '}';
    }
}