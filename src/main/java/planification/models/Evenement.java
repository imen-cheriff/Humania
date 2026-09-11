package planification.models;

import java.util.Date;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private Date dateEvenement;
    private Date dateHeureDebut;
    private Date dateHeureFin;
    private String lieu;
    private int nbParticipantsMax;
    private String participantsInscrits;
    private String creePar;
    private Date creeLe;

    public Evenement(){}

    public Evenement(int id, String titre, String description,
                     Date dateEvenement, Date dateHeureDebut, Date dateHeureFin,
                     String lieu, int nbParticipantsMax, String participantsInscrits,
                     String creePar, Date creeLe) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateEvenement = dateEvenement;
        this.dateHeureDebut = dateHeureDebut;
        this.dateHeureFin = dateHeureFin;
        this.lieu = lieu;
        this.nbParticipantsMax = nbParticipantsMax;
        this.participantsInscrits = participantsInscrits;
        this.creePar = creePar;
        this.creeLe = creeLe;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateEvenement() {
        return dateEvenement;
    }

    public void setDateEvenement(Date dateEvenement) {
        this.dateEvenement = dateEvenement;
    }

    public Date getDateHeureDebut() {
        return dateHeureDebut;
    }

    public void setDateHeureDebut(Date dateHeureDebut) {
        this.dateHeureDebut = dateHeureDebut;
    }

    public Date getDateHeureFin() {
        return dateHeureFin;
    }

    public void setDateHeureFin(Date dateHeureFin) {
        this.dateHeureFin = dateHeureFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public int getNbParticipantsMax() {
        return nbParticipantsMax;
    }

    public void setNbParticipantsMax(int nbParticipantsMax) {
        this.nbParticipantsMax = nbParticipantsMax;
    }

    public String getParticipantsInscrits() {
        return participantsInscrits;
    }

    public void setParticipantsInscrits(String participantsInscrits) {
        this.participantsInscrits = participantsInscrits;
    }

    public String getCreePar() {
        return creePar;
    }

    public void setCreePar(String creePar) {
        this.creePar = creePar;
    }

    public Date getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(Date creeLe) {
        this.creeLe = creeLe;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", dateEvenement=" + dateEvenement +
                ", dateHeureDebut=" + dateHeureDebut +
                ", dateHeureFin=" + dateHeureFin +
                ", lieu='" + lieu + '\'' +
                ", nbParticipantsMax=" + nbParticipantsMax +
                ", participantsInscrits='" + participantsInscrits + '\'' +
                ", creePar='" + creePar + '\'' +
                ", creeLe=" + creeLe +
                '}';
    }

    public void valider() {
        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de l'événement ne doit pas être vide.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("La description de l'événement ne doit pas être vide.");
        }
        if (lieu == null || lieu.trim().isEmpty()) {
            throw new IllegalArgumentException("Le lieu de l'événement ne doit pas être vide.");
        }
        if (dateEvenement == null) {
            throw new IllegalArgumentException("La date de l'événement est obligatoire.");
        }
        if (dateHeureDebut == null || dateHeureFin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (dateHeureFin.before(dateHeureDebut)) {
            throw new IllegalArgumentException("La date/heure de fin doit être postérieure ou égale à la date/heure de début.");
        }
        if (nbParticipantsMax <= 0) {
            throw new IllegalArgumentException("Le nombre maximum de participants doit être positif.");
        }
        // Validation : creePar doit contenir uniquement des lettres (et espaces)
        if (creePar != null && !creePar.trim().isEmpty()) {
            if (!creePar.matches("^[a-zA-ZÀ-ÿ\\s]+$")) {
                throw new IllegalArgumentException("Le nom du créateur doit contenir uniquement des lettres (pas de chiffres ni de caractères spéciaux).");
            }
        }
        // Validation : creeLe doit être une date valide
        if (creeLe == null) {
            throw new IllegalArgumentException("La date de création est obligatoire.");
        }
    }
}

