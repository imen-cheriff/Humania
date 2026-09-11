package recrutement.models;

import java.sql.Date;
import java.sql.Timestamp;

public class CandidatureInterne {

    private int id;
    private String posteActuel;
    private String nouveauPoste;
    private double nouveauSalaire;
    private Date dateDemande;
    private String motif;
    private Timestamp derniereModification;

    public CandidatureInterne() {}

    public CandidatureInterne(int id, String posteActuel, String nouveauPoste, double nouveauSalaire, Date dateDemande, String motif, Timestamp derniereModification) {
        this.id = id;
        this.posteActuel = posteActuel;
        this.nouveauPoste = nouveauPoste;
        this.nouveauSalaire = nouveauSalaire;
        this.dateDemande = dateDemande;
        this.motif = motif;
        this.derniereModification = derniereModification;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPosteActuel() {
        return posteActuel;
    }

    public void setPosteActuel(String posteActuel) {
        this.posteActuel = posteActuel;
    }

    public String getNouveauPoste() {
        return nouveauPoste;
    }

    public void setNouveauPoste(String nouveauPoste) {
        this.nouveauPoste = nouveauPoste;
    }

    public double getNouveauSalaire() {
        return nouveauSalaire;
    }

    public void setNouveauSalaire(double nouveauSalaire) {
        this.nouveauSalaire = nouveauSalaire;
    }

    public Date getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(Date dateDemande) {
        this.dateDemande = dateDemande;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Timestamp getDerniereModification() {
        return derniereModification;
    }

    public void setDerniereModification(Timestamp derniereModification) {
        this.derniereModification = derniereModification;
    }

    @Override
    public String toString() {
        return "CandidatureInterne{" +
                "id=" + id +
                ", posteActuel='" + posteActuel + '\'' +
                ", nouveauPoste='" + nouveauPoste + '\'' +
                ", nouveauSalaire=" + nouveauSalaire +
                ", dateDemande=" + dateDemande +
                ", motif='" + motif + '\'' +
                ", derniereModification=" + derniereModification +
                '}';
    }
}



