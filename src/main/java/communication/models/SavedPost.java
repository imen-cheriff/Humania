package communication.models;

import java.time.LocalDateTime;

public class SavedPost {
    private Integer id;
    private Integer userId;      // FK vers User
    private Integer publicationId; // FK vers Publication
    private LocalDateTime savedAt;

    // Constructeurs
    public SavedPost() {}

    public SavedPost(Integer userId, Integer publicationId) {
        this.userId = userId;
        this.publicationId = publicationId;
        this.savedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(Integer publicationId) {
        this.publicationId = publicationId;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    @Override
    public String toString() {
        return "SavedPost{" +
                "id=" + id +
                ", userId=" + userId +
                ", publicationId=" + publicationId +
                '}';
    }
}