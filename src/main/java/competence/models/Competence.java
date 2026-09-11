package competence.models;

import competence.enums.TypeCompetence;
import utilisateur.models.Employe;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import java.util.ArrayList;
import java.util.List;

public class Competence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String libelle;
    private int niveauMax;
    private TypeCompetence typeCompetence;
    private CategorieCompetence categorie;
    private List<CompetenceEmploye> competencesEmployes;
    private List<EvaluationFormation> evaluationsFormations;

    public Competence() {
        this.competencesEmployes = new ArrayList<>();
        this.evaluationsFormations = new ArrayList<>();
    }

    public Competence(String libelle, int niveauMax, TypeCompetence typeCompetence) {
        this.libelle = libelle;
        this.niveauMax = niveauMax;
        this.typeCompetence = typeCompetence;
        this.competencesEmployes = new ArrayList<>();
        this.evaluationsFormations = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() {return id; }

    public void setId(int id) {this.id = id; }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public int getNiveauMax() {
        return niveauMax;
    }

    public void setNiveauMax(int niveauMax) {
        this.niveauMax = niveauMax;
    }

    public TypeCompetence getTypeCompetence() {
        return typeCompetence;
    }

    public void setTypeCompetence(TypeCompetence typeCompetence) {
        this.typeCompetence = typeCompetence;
    }

    public CategorieCompetence getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieCompetence categorie) {
        this.categorie = categorie;
    }

    public List<CompetenceEmploye> getCompetencesEmployes() {
        return competencesEmployes;
    }

    public void setCompetencesEmployes(List<CompetenceEmploye> competencesEmployes) {
        this.competencesEmployes = competencesEmployes;
    }

    public List<EvaluationFormation> getEvaluationsFormations() {
        return evaluationsFormations;
    }

    public void setEvaluationsFormations(List<EvaluationFormation> evaluationsFormations) {
        this.evaluationsFormations = evaluationsFormations;
    }

    // Méthodes métier
    public int evaluerNiveau(Employe employe) {
        for (CompetenceEmploye ce : competencesEmployes) {
            if (ce.getEmploye().equals(employe)) {
                return ce.getNiveauActuel();
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Competence{" +
                "id='" + id + '\'' +
                ", libelle='" + libelle + '\'' +
                ", niveauMax=" + niveauMax +
                ", typeCompetence=" + typeCompetence +
                ", categorie=" + (categorie != null ? categorie.getLibelle() : "N/A") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Competence that = (Competence) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}