package competence.models;

import competence.enums.Statut;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class InscriptionFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Statut statut;
    private int progression;
    private Double noteFinale;
    private Date dateInscription;
    private SessionFormation session;
    private ActionPDI actionPDI;
    private List<ResultatEvaluation> resultatsEvaluations;

    public InscriptionFormation() {
        this.resultatsEvaluations = new ArrayList<>();
    }

    public InscriptionFormation(Statut statut, Date dateInscription) {
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.progression = 0;
        this.resultatsEvaluations = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public int getProgression() {
        return progression;
    }

    public void setProgression(int progression) {
        this.progression = progression;
    }

    public Double getNoteFinale() {
        return noteFinale;
    }

    public void setNoteFinale(Double noteFinale) {
        this.noteFinale = noteFinale;
    }

    public Date getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(Date dateInscription) {
        this.dateInscription = dateInscription;
    }

    public SessionFormation getSession() {
        return session;
    }

    public void setSession(SessionFormation session) {
        this.session = session;
    }

    public ActionPDI getActionPDI() {
        return actionPDI;
    }

    public void setActionPDI(ActionPDI actionPDI) {
        this.actionPDI = actionPDI;
    }

    public List<ResultatEvaluation> getResultatsEvaluations() {
        return resultatsEvaluations;
    }

    public void setResultatsEvaluations(List<ResultatEvaluation> resultatsEvaluations) {
        this.resultatsEvaluations = resultatsEvaluations;
    }

    @Override
    public String toString() {
        return "InscriptionFormation{" +
                "id=" + id +
                ", statut=" + statut +
                ", progression=" + progression +
                ", noteFinale=" + noteFinale +
                ", dateInscription=" + dateInscription +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InscriptionFormation that = (InscriptionFormation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}