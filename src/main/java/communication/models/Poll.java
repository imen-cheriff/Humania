package communication.models;

import java.time.LocalDateTime;

public class Poll {
    private Integer id;
    private Integer publicationId;
    private String question;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;    // ✅ expiration
    private boolean isAnonymous;        // ✅ anonyme
    private boolean allowMultiple;      // ✅ choix multiples
    private Integer createdById;        // ✅ qui a créé
    private Integer totalVotes;

    public Poll() {}

    public Poll(Integer publicationId, String question, Integer createdById) {
        this.publicationId = publicationId;
        this.question = question;
        this.createdById = createdById;
        this.createdAt = LocalDateTime.now();
        this.totalVotes = 0;
        this.isAnonymous = false;
        this.allowMultiple = false;
    }

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPublicationId() { return publicationId; }
    public void setPublicationId(Integer publicationId) { this.publicationId = publicationId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
    public boolean isAllowMultiple() { return allowMultiple; }
    public void setAllowMultiple(boolean allowMultiple) { this.allowMultiple = allowMultiple; }
    public Integer getCreatedById() { return createdById; }
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
    public Integer getTotalVotes() { return totalVotes; }
    public void setTotalVotes(Integer totalVotes) { this.totalVotes = totalVotes; }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isOpen() {
        return !isExpired();
    }
}