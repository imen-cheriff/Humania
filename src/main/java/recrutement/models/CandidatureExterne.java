package recrutement.models;

import java.sql.Date;
import java.sql.Timestamp;

public class CandidatureExterne {

    private int id;
    private int posteExterneId;
    private String nom;
    private String prenom;
    private Date dateDepot;
    private String statut;
    private String etapePipeline;
    private double scoringIa;
    private String cvUrl;
    private String lettreMotivationUrl;
    private Timestamp derniereModification;

    public CandidatureExterne() {
    }

    public CandidatureExterne(int id, int posteExterneId, String nom, String prenom, Date dateDepot, String statut, String etapePipeline, double scoringIa, String cvUrl, String lettreMotivationUrl, Timestamp derniereModification) {
        this.id = id;
        this.posteExterneId = posteExterneId;
        this.nom = nom;
        this.prenom = prenom;
        this.dateDepot = dateDepot;
        this.statut = statut;
        this.etapePipeline = etapePipeline;
        this.scoringIa = scoringIa;
        this.cvUrl = cvUrl;
        this.lettreMotivationUrl = lettreMotivationUrl;
        this.derniereModification = derniereModification;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPosteExterneId() {
        return posteExterneId;
    }

    public void setPosteExterneId(int posteExterneId) {
        this.posteExterneId = posteExterneId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Date getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(Date dateDepot) {
        this.dateDepot = dateDepot;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getEtapePipeline() {
        return etapePipeline;
    }

    public void setEtapePipeline(String etapePipeline) {
        this.etapePipeline = etapePipeline;
    }

    public double getScoringIa() {
        return scoringIa;
    }

    public void setScoringIa(double scoringIa) {
        this.scoringIa = scoringIa;
    }

    public String getCvUrl() {
        return cvUrl;
    }

    public void setCvUrl(String cvUrl) {
        this.cvUrl = cvUrl;
    }

    public String getLettreMotivationUrl() {
        return lettreMotivationUrl;
    }

    public void setLettreMotivationUrl(String lettreMotivationUrl) {
        this.lettreMotivationUrl = lettreMotivationUrl;
    }

    public Timestamp getDerniereModification() {
        return derniereModification;
    }

    public void setDerniereModification(Timestamp derniereModification) {
        this.derniereModification = derniereModification;
    }

    @Override
    public String toString() {
        return "CandidatureExterne{" +
                "id=" + id +
                ", posteExterneId=" + posteExterneId +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", dateDepot=" + dateDepot +
                ", statut='" + statut + '\'' +
                ", etapePipeline='" + etapePipeline + '\'' +
                ", scoringIa=" + scoringIa +
                ", cvUrl='" + cvUrl + '\'' +
                ", lettreMotivationUrl='" + lettreMotivationUrl + '\'' +
                ", derniereModification=" + derniereModification +
                '}';
    }
}
