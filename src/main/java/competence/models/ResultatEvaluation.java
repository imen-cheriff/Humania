package competence.models;

import competence.enums.Statut;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.sql.Date;

public class ResultatEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Double note;
    private Date datePassage;
    private Statut statut;
    private String commentaire;
    private EvaluationFormation evaluation;
    private InscriptionFormation inscription;

    public ResultatEvaluation() {
    }

    public ResultatEvaluation(Double note, Date datePassage, Statut statut) {
        this.note = note;
        this.datePassage = datePassage;
        this.statut = statut;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Double getNote() {
        return note;
    }

    public void setNote(Double note) {
        this.note = note;
    }

    public Date getDatePassage() {
        return datePassage;
    }

    public void setDatePassage(Date datePassage) {
        this.datePassage = datePassage;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public EvaluationFormation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(EvaluationFormation evaluation) {
        this.evaluation = evaluation;
    }

    public InscriptionFormation getInscription() {
        return inscription;
    }

    public void setInscription(InscriptionFormation inscription) {
        this.inscription = inscription;
    }

    @Override
    public String toString() {
        return "ResultatEvaluation{" +
                "id=" + id +
                ", note=" + note +
                ", datePassage=" + datePassage +
                ", statut=" + statut +
                ", commentaire='" + commentaire + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultatEvaluation that = (ResultatEvaluation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}