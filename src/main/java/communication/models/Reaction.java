package communication.models;

import communication.models.enums.TypeReaction;

import java.time.LocalDateTime;

public class Reaction {
    private Integer id;
    private TypeReaction type;  // Emoji: LIKE, LOVE, LAUGH, WOW, SAD, ANGRY
    private LocalDateTime dateCreation;
    private Integer userId;  // (réagit)
    // une seule doit être non-null
    private Integer messageId;       // null si pas une réaction à message
    private Integer publicationId;   // null si pas une réaction à publication
    private Integer commentaireId;   // null si pas une réaction à commentaire


    public Reaction() {}

    public Reaction(TypeReaction type, Integer userId) {
        this.type = type;
        this.userId = userId;
        this.dateCreation = LocalDateTime.now();
    }

    // XOR (one target should be non-null)
    public boolean isValide() {
        int count = 0;
        if (messageId != null) count++;
        if (publicationId != null) count++;
        if (commentaireId != null) count++;
        return count == 1;  // one only doit être non-null
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TypeReaction getType() {
        return type;
    }

    public void setType(TypeReaction type) {
        this.type = type;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public Integer getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(Integer publicationId) {
        this.publicationId = publicationId;
    }

    public Integer getCommentaireId() {
        return commentaireId;
    }

    public void setCommentaireId(Integer commentaireId) {
        this.commentaireId = commentaireId;
    }

    @Override
    public String toString() {
        return "Reaction{" +
                "id=" + id +
                ", type=" + type +
                ", userId=" + userId +
                '}';
    }
}