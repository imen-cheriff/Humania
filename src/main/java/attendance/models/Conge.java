package attendance.models;

import java.time.LocalDate;

public class Conge {

    private int id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int nbrJours;
    private String statut;
    private int typeCongeId;
    private int utilisateurId;

    public Conge() {
    }

    public Conge(int id, LocalDate dateDebut, LocalDate dateFin, int nbrJours,
                 String statut, int typeCongeId, int utilisateurId) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbrJours = nbrJours;
        this.statut = statut;
        this.typeCongeId = typeCongeId;
        this.utilisateurId = utilisateurId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public int getNbrJours() {
        return nbrJours;
    }

    public void setNbrJours(int nbrJours) {
        this.nbrJours = nbrJours;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getTypeCongeId() {
        return typeCongeId;
    }

    public void setTypeCongeId(int typeCongeId) {
        this.typeCongeId = typeCongeId;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    @Override
    public String toString() {
        return "Conge{" +
                "id=" + id +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", nbrJours=" + nbrJours +
                ", statut='" + statut + '\'' +
                ", typeCongeId=" + typeCongeId +
                ", utilisateurId=" + utilisateurId +
                "}\n";
    }
}