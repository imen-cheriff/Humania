package competence.models;

import competence.enums.TypeEvaluation;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.util.ArrayList;
import java.util.List;

public class EvaluationFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String titre;
    private TypeEvaluation type;
    private int duree;
    private SessionFormation session;
    private List<ResultatEvaluation> resultats;

    public EvaluationFormation() {
        this.resultats = new ArrayList<>();
    }

    public EvaluationFormation(String titre, TypeEvaluation type, int duree) {
        this.titre = titre;
        this.type = type;
        this.duree = duree;
        this.resultats = new ArrayList<>();
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

    public TypeEvaluation getType() {
        return type;
    }

    public void setType(TypeEvaluation type) {
        this.type = type;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public SessionFormation getSession() {
        return session;
    }

    public void setSession(SessionFormation session) {
        this.session = session;
    }

    public List<ResultatEvaluation> getResultats() {
        return resultats;
    }

    public void setResultats(List<ResultatEvaluation> resultats) {
        this.resultats = resultats;
    }

    @Override
    public String toString() {
        return "EvaluationFormation{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", type=" + type +
                ", duree=" + duree +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationFormation that = (EvaluationFormation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}