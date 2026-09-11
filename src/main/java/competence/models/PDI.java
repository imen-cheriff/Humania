package competence.models;

import competence.enums.Statut;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class PDI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int annee;
    private int progressionGlobale;
    private Date dateCreation;
    private Statut statut;
    private List<ActionPDI> actions;

    public PDI() {
        this.actions = new ArrayList<>();
    }

    public PDI(int annee, Date dateCreation, Statut statut) {
        this.annee = annee;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.progressionGlobale = 0;
        this.actions = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAnnee() {
        return annee;
    }

    public void setAnnee(int annee) {
        this.annee = annee;
    }

    public int getProgressionGlobale() {
        return progressionGlobale;
    }

    public void setProgressionGlobale(int progressionGlobale) {
        this.progressionGlobale = progressionGlobale;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public List<ActionPDI> getActions() {
        return actions;
    }

    public void setActions(List<ActionPDI> actions) {
        this.actions = actions;
    }

    // Méthodes métier
    public void calculerProgression() {
        if (actions.isEmpty()) {
            this.progressionGlobale = 0;
            return;
        }

        int totalProgression = 0;
        for (ActionPDI action : actions) {
            if (action.getStatut() == Statut.TERMINE) {
                totalProgression += 100;
            } else if (action.getStatut() == Statut.EN_COURS) {
                totalProgression += 50;
            }
        }
        this.progressionGlobale = totalProgression / actions.size();
    }

    @Override
    public String toString() {
        return "PDI{" +
                "id=" + id +
                ", annee=" + annee +
                ", progressionGlobale=" + progressionGlobale +
                ", dateCreation=" + dateCreation +
                ", statut=" + statut +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PDI pdi = (PDI) o;
        return id == pdi.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}