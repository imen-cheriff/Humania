package attendance.models;

import java.time.LocalDate;

public class Absence {

    private int id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int nbrJours;
    private String statut;
    private int typeAbsenceId;
    private int utilisateurId;
    // Nouveaux champs pour les autorisations
    private String heureDebut;
    private String heureFin;
    private int dureeMinutes;
    private String motif;

    public Absence() {}

    public Absence(int id, LocalDate dateDebut, LocalDate dateFin, int nbrJours,
                   String statut, int typeAbsenceId, int utilisateurId) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbrJours = nbrJours;
        this.statut = statut;
        this.typeAbsenceId = typeAbsenceId;
        this.utilisateurId = utilisateurId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getNbrJours() { return nbrJours; }
    public void setNbrJours(int nbrJours) { this.nbrJours = nbrJours; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getTypeAbsenceId() { return typeAbsenceId; }
    public void setTypeAbsenceId(int typeAbsenceId) { this.typeAbsenceId = typeAbsenceId; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }

    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }

    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    @Override
    public String toString() {
        return "Absence{id=" + id +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", nbrJours=" + nbrJours +
                ", statut='" + statut + '\'' +
                ", typeAbsenceId=" + typeAbsenceId +
                ", utilisateurId=" + utilisateurId +
                ", heureDebut='" + heureDebut + '\'' +
                ", heureFin='" + heureFin + '\'' +
                ", dureeMinutes=" + dureeMinutes +
                ", motif='" + motif + '\'' +
                "}\n";
    }
}