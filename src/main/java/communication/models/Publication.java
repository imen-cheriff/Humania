package communication.models;

import communication.models.enums.Statut;
import java.time.LocalDateTime;

public class Publication {
    private int id;
    private String contenu;
    private String imageUrl;
    private int authorId;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private int nombreCommentaires;
    private int nombreReactions;
    private Statut statut;
    private Integer sharedFromId;
    private String shareMessage;
    private String visibility;
    private Integer groupId;

    public Publication() {
        this.dateCreation = LocalDateTime.now();
        this.nombreCommentaires = 0;
        this.nombreReactions = 0;
        this.statut = Statut.ACTIF;
        this.visibility = "PUBLIC";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public int getNombreCommentaires() {
        return nombreCommentaires;
    }

    public void setNombreCommentaires(int nombreCommentaires) {
        this.nombreCommentaires = nombreCommentaires;
    }

    public int getNombreReactions() {
        return nombreReactions;
    }

    public void setNombreReactions(int nombreReactions) {
        this.nombreReactions = nombreReactions;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public Integer getSharedFromId() {
        return sharedFromId;
    }

    public void setSharedFromId(Integer sharedFromId) {
        this.sharedFromId = sharedFromId;
    }

    public String getShareMessage() {
        return shareMessage;
    }

    public void setShareMessage(String shareMessage) {
        this.shareMessage = shareMessage;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public boolean isShared() {
        return sharedFromId != null;
    }

    public boolean isPublic() {
        return "PUBLIC".equals(visibility);
    }

    public boolean isGroupPost() {
        return "GROUP".equals(visibility) && groupId != null;
    }

    @Override
    public String toString() {
        return "Publication{" +
                "id=" + id +
                ", contenu='" + contenu + '\'' +
                ", authorId=" + authorId +
                ", dateCreation=" + dateCreation +
                ", nombreCommentaires=" + nombreCommentaires +
                ", nombreReactions=" + nombreReactions +
                ", statut=" + statut +
                ", visibility='" + visibility + '\'' +
                ", groupId=" + groupId +
                '}';
    }
}