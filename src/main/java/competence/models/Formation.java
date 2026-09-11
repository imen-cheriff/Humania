package competence.models;

import competence.enums.StatutFormation;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.util.ArrayList;
import java.util.List;

public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String titre;
    private String description;
    private int duree;
    private double cout;
    private StatutFormation statutFormation;
    private CategorieFormation categorie;
    private List<SessionFormation> sessions;

    public Formation() {
        this.sessions = new ArrayList<>();
    }

    public Formation(String titre, String description, int duree, double cout, StatutFormation statutFormation) {
        this.titre = titre;
        this.description = description;
        this.duree = duree;
        this.cout = cout;
        this.statutFormation = statutFormation;
        this.sessions = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public double getCout() {
        return cout;
    }

    public void setCout(double cout) {
        this.cout = cout;
    }

    public StatutFormation getStatutFormation() {
        return statutFormation;
    }

    public void setStatutFormation(StatutFormation statutFormation) {
        this.statutFormation = statutFormation;
    }

    public CategorieFormation getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieFormation categorie) {
        this.categorie = categorie;
    }

    public List<SessionFormation> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionFormation> sessions) {
        this.sessions = sessions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Formation{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", duree=" + duree +
                ", cout=" + cout +
                ", statutFormation=" + statutFormation +
                ", categorie=" + (categorie != null ? categorie.getLibelle() : "N/A") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Formation that = (Formation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}