package competence.models;

import utilisateur.models.Employe;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.sql.Date;

public class CompetenceEmploye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int niveauActuel;
    private boolean niveauValide;
    private String preuveUrl;
    private Date dateEvaluation;
    private Employe employe;
    private Competence competence;

    public CompetenceEmploye() {
    }

    public CompetenceEmploye(int niveauActuel, boolean niveauValide, Date dateEvaluation) {
        this.niveauActuel = niveauActuel;
        this.niveauValide = niveauValide;
        this.dateEvaluation = dateEvaluation;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNiveauActuel() {
        return niveauActuel;
    }

    public void setNiveauActuel(int niveauActuel) {
        this.niveauActuel = niveauActuel;
    }

    public boolean isNiveauValide() {
        return niveauValide;
    }

    public void setNiveauValide(boolean niveauValide) {
        this.niveauValide = niveauValide;
    }

    public String getPreuveUrl() {
        return preuveUrl;
    }

    public void setPreuveUrl(String preuveUrl) {
        this.preuveUrl = preuveUrl;
    }

    public Date getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(Date dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    public Employe getEmploye() {
        return employe;
    }

    public void setEmploye(Employe employe) {
        this.employe = employe;
    }

    public Competence getCompetence() {
        return competence;
    }

    public void setCompetence(Competence competence) {
        this.competence = competence;
    }

    // Méthodes métier
    public void validerNiveau() {
        this.niveauValide = true;
    }

    public void invaliderNiveau() {
        this.niveauValide = false;
    }

    @Override
    public String toString() {
        return "CompetenceEmploye{" +
                "id=" + id +
                ", niveauActuel=" + niveauActuel +
                ", niveauValide=" + niveauValide +
                ", dateEvaluation=" + dateEvaluation +
                ", employe=" + (employe != null ? employe.getMatricule() : "N/A") +
                ", competence=" + (competence != null ? competence.getLibelle() : "N/A") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompetenceEmploye that = (CompetenceEmploye) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}