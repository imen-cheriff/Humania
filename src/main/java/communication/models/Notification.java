package communication.models;

import java.time.LocalDateTime;

public class Notification {
    private Integer id;
    private String titre;
    private String message;
    private String type;  // MESSAGE, PUBLICATION, COMMENTAIRE, REACTION, DOCUMENT, FOLLOW
    private LocalDateTime dateCreation;
    private Integer userId;  // FK vers User (destinataire)
    private Boolean seen;  // true = lu, false = non-lu (colonne 'seen' dans DB)
    private LocalDateTime dateViewAt;  // quand a-t-il été vu

    // Relations
    private Integer relatedUserId;        // Qui a déclenché la notification
    private Integer relatedPublicationId; // Publication concernée
    private Integer relatedCommentaireId; // Commentaire concerné

    // Constructeurs
    public Notification() {
        this.dateCreation = LocalDateTime.now();
        this.seen = false;
    }

    public Notification(String titre, String message, String type, Integer userId) {
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.userId = userId;
        this.dateCreation = LocalDateTime.now();
        this.seen = false;
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    // Alias pour compatibilité avec le code
    public Boolean getIsRead() {
        return seen;
    }

    public void setIsRead(Boolean isRead) {
        this.seen = isRead;
    }

    public LocalDateTime getDateViewAt() {
        return dateViewAt;
    }

    public void setDateViewAt(LocalDateTime dateViewAt) {
        this.dateViewAt = dateViewAt;
    }

    public Integer getRelatedUserId() {
        return relatedUserId;
    }

    public void setRelatedUserId(Integer relatedUserId) {
        this.relatedUserId = relatedUserId;
    }

    public Integer getRelatedPublicationId() {
        return relatedPublicationId;
    }

    public void setRelatedPublicationId(Integer relatedPublicationId) {
        this.relatedPublicationId = relatedPublicationId;
    }

    public Integer getRelatedCommentaireId() {
        return relatedCommentaireId;
    }

    public void setRelatedCommentaireId(Integer relatedCommentaireId) {
        this.relatedCommentaireId = relatedCommentaireId;
    }

    // Alias pour compatibilité
    public Integer getPublicationId() {
        return relatedPublicationId;
    }

    public void setPublicationId(Integer publicationId) {
        this.relatedPublicationId = publicationId;
    }

    public Integer getFromUserId() {
        return relatedUserId;
    }

    public void setFromUserId(Integer fromUserId) {
        this.relatedUserId = fromUserId;
    }

    public String getContenu() {
        return message != null ? message : titre;
    }

    // Utilitaires
    public void markAsRead() {
        this.seen = true;
        this.dateViewAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", type='" + type + '\'' +
                ", seen=" + seen +
                '}';
    }
}