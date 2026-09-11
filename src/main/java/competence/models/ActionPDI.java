package competence.models;

import competence.enums.TypeAction;
import competence.enums.Statut;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class ActionPDI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private TypeAction typeAction;
    private Statut statut;
    private Date dateDebut;
    private Date dateFinPrevue;
    private int priorite;
    private PDI pdi;
    private List<InscriptionFormation> inscriptions;

    public ActionPDI() {
        this.inscriptions = new ArrayList<>();
    }

    public ActionPDI(TypeAction typeAction, Statut statut, Date dateDebut, Date dateFinPrevue, int priorite) {
        this.typeAction = typeAction;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFinPrevue = dateFinPrevue;
        this.priorite = priorite;
        this.inscriptions = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TypeAction getTypeAction() {
        return typeAction;
    }

    public void setTypeAction(TypeAction typeAction) {
        this.typeAction = typeAction;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFinPrevue() {
        return dateFinPrevue;
    }

    public void setDateFinPrevue(Date dateFinPrevue) {
        this.dateFinPrevue = dateFinPrevue;
    }

    public int getPriorite() {
        return priorite;
    }

    public void setPriorite(int priorite) {
        this.priorite = priorite;
    }

    public PDI getPdi() {
        return pdi;
    }

    public void setPdi(PDI pdi) {
        this.pdi = pdi;
    }

    public List<InscriptionFormation> getInscriptions() {
        return inscriptions;
    }

    public void setInscriptions(List<InscriptionFormation> inscriptions) {
        this.inscriptions = inscriptions;
    }

    @Override
    public String toString() {
        return "ActionPDI{" +
                "id=" + id +
                ", typeAction=" + typeAction +
                ", statut=" + statut +
                ", dateDebut=" + dateDebut +
                ", dateFinPrevue=" + dateFinPrevue +
                ", priorite=" + priorite +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionPDI actionPDI = (ActionPDI) o;
        return id == actionPDI.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}