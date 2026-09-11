package planification.models;

import java.util.Date;

public class ParticipEven {

    private int id;
    private int idEvenement;
    private int idEmploye;
    private Date dateParticipation;
    private boolean statut;
    private Date creeLe;

    public ParticipEven() {
    }


    public ParticipEven(int id, int idEvenement, int idEmploye, Date dateParticipation, boolean statut, Date creeLe) {
        this.id = id;
        this.idEvenement = idEvenement;
        this.idEmploye = idEmploye;
        this.dateParticipation = dateParticipation;
        this.statut = statut;
        this.creeLe = creeLe;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    public int getIdEmploye() {
        return idEmploye;
    }

    public void setIdEmploye(int idEmploye) {
        this.idEmploye = idEmploye;
    }

    public Date getDateParticipation() {
        return dateParticipation;
    }

    public void setDateParticipation(Date dateParticipation) {
        this.dateParticipation = dateParticipation;
    }

    public boolean isStatut() {
        return statut;
    }

    public void setStatut(boolean statut) {
        this.statut = statut;
    }

    public Date getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(Date creeLe) {
        this.creeLe = creeLe;
    }

    @Override
    public String toString() {
        return "ParticipEven{" +
                "id=" + id +
                ", idEvenement=" + idEvenement +
                ", idEmploye=" + idEmploye +
                ", dateParticipation=" + dateParticipation +
                ", statut=" + statut +
                ", creeLe=" + creeLe +
                '}';
    }

    public void valider() {
        if (idEvenement <= 0) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire et doit être positif.");
        }
        if (idEmploye <= 0) {
            throw new IllegalArgumentException("L'identifiant de l'employé est obligatoire et doit être positif.");
        }
        if (dateParticipation == null) {
            throw new IllegalArgumentException("La date de participation est obligatoire.");
        }
    }
}
