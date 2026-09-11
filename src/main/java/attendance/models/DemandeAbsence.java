package attendance.models;

import java.time.LocalDate;

public class DemandeAbsence {

    private int id;
    private LocalDate dateDemande;
    private String motif;
    private String statut;
    private int absenceId;
    private int utilisateurId;

    public DemandeAbsence() {
    }

    public DemandeAbsence(int id, LocalDate dateDemande, String motif, String statut,
                          int absenceId, int utilisateurId) {
        this.id = id;
        this.dateDemande = dateDemande;
        this.motif = motif;
        this.statut = statut;
        this.absenceId = absenceId;
        this.utilisateurId = utilisateurId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDate dateDemande) {
        this.dateDemande = dateDemande;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getAbsenceId() {
        return absenceId;
    }

    public void setAbsenceId(int absenceId) {
        this.absenceId = absenceId;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    @Override
    public String toString() {
        return "DemandeAbsence{" +
                "id=" + id +
                ", dateDemande=" + dateDemande +
                ", motif='" + motif + '\'' +
                ", statut='" + statut + '\'' +
                ", absenceId=" + absenceId +
                ", utilisateurId=" + utilisateurId +
                "}\n";
    }
}