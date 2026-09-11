package communication.models;

import java.time.LocalDateTime;

public class Share {
    private Integer id;
    private Integer userId;         // FK vers User (celui qui partage)
    private Integer publicationId;  // FK vers Publication
    private LocalDateTime sharedAt;
    private String sharedMessage;   // Message personnel en partageant (optionnel)
    private Integer shareCount;     // Compteur de partages

    // Constructeurs
    public Share() {}

    public Share(Integer userId, Integer publicationId) {
        this.userId = userId;
        this.publicationId = publicationId;
        this.sharedAt = LocalDateTime.now();
        this.shareCount = 0;
    }

    public Share(Integer userId, Integer publicationId, String sharedMessage) {
        this.userId = userId;
        this.publicationId = publicationId;
        this.sharedMessage = sharedMessage;
        this.sharedAt = LocalDateTime.now();
        this.shareCount = 0;
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

    public LocalDateTime getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(LocalDateTime sharedAt) {
        this.sharedAt = sharedAt;
    }

    public String getSharedMessage() {
        return sharedMessage;
    }

    public void setSharedMessage(String sharedMessage) {
        this.sharedMessage = sharedMessage;
    }

    public Integer getShareCount() {
        return shareCount;
    }

    public void setShareCount(Integer shareCount) {
        this.shareCount = shareCount;
    }

    @Override
    public String toString() {
        return "Share{" +
                "id=" + id +
                ", userId=" + userId +
                ", publicationId=" + publicationId +
                '}';
    }
}