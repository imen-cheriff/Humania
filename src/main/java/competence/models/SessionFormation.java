package competence.models;

import competence.enums.Statut;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class SessionFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Date dateDebut;
    private Date dateFin;
    private String lieu;
    private Statut statut;
    private Formation formation;
    private List<EvaluationFormation> evaluations;
    private List<InscriptionFormation> inscriptions;

    public SessionFormation() {
        this.evaluations = new ArrayList<>();
        this.inscriptions = new ArrayList<>();
    }

    public SessionFormation(Date dateDebut, Date dateFin, String lieu, Statut statut) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.statut = statut;
        this.evaluations = new ArrayList<>();
        this.inscriptions = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public Formation getFormation() {
        return formation;
    }

    public void setFormation(Formation formation) {
        this.formation = formation;
    }

    public List<EvaluationFormation> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(List<EvaluationFormation> evaluations) {
        this.evaluations = evaluations;
    }

    public List<InscriptionFormation> getInscriptions() {
        return inscriptions;
    }

    public void setInscriptions(List<InscriptionFormation> inscriptions) {
        this.inscriptions = inscriptions;
    }

    @Override
    public String toString() {
        return "SessionFormation{" +
                "id=" + id +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", lieu='" + lieu + '\'' +
                ", statut=" + statut +
                ", formation=" + (formation != null ? formation.getTitre() : "N/A") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionFormation that = (SessionFormation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}