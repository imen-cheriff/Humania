package communication.models;

import communication.models.enums.Statut;

import java.time.LocalDateTime;

public class Commentaire {
    private Integer id;
    private String contenu;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Integer publicationId;  // FK vers Publication
    private Integer authorId;  // FK vers User (celui qui commente)
    private Integer nombreReactions;
    private Statut statut;  // ACTIF, SUPPRIME, ARCHIVÉ
    private String gifUrl;
    public String getGifUrl() { return gifUrl; }

    // Constructeurs
    public Commentaire() {}

    public Commentaire(String contenu, Integer publicationId, Integer authorId) {
        this.contenu = contenu;
        this.publicationId = publicationId;
        this.authorId = authorId;
        this.dateCreation = LocalDateTime.now();
        this.nombreReactions = 0;
        this.statut = Statut.ACTIF;
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setGifUrl(String gifUrl) { this.gifUrl = gifUrl; }

    public void setContenu(String contenu) {
        this.contenu = contenu;
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

    public Integer getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(Integer publicationId) {
        this.publicationId = publicationId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getNombreReactions() {
        return nombreReactions;
    }

    public void setNombreReactions(Integer nombreReactions) {
        this.nombreReactions = nombreReactions;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + id +
                ", publicationId=" + publicationId +
                ", authorId=" + authorId +
                '}';
    }
}