package attendance.models;


import java.time.LocalDate;

public class DemandeConge {

    private int id;
    private LocalDate dateDemande;
    private String motif;
    private String statut;
    private int congeId;
    private int utilisateurId;

    public DemandeConge() {
    }

    public DemandeConge(int id, LocalDate dateDemande, String motif, String statut,
                        int congeId, int utilisateurId) {
        this.id = id;
        this.dateDemande = dateDemande;
        this.motif = motif;
        this.statut = statut;
        this.congeId = congeId;
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

    public int getCongeId() {
        return congeId;
    }

    public void setCongeId(int congeId) {
        this.congeId = congeId;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    @Override
    public String toString() {
        return "DemandeConge{" +
                "id=" + id +
                ", dateDemande=" + dateDemande +
                ", motif='" + motif + '\'' +
                ", statut='" + statut + '\'' +
                ", congeId=" + congeId +
                ", utilisateurId=" + utilisateurId +
                "}\n";
    }
}