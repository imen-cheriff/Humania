package recrutement.models;

import java.sql.Date;

public class PosteInterne {

    private int id;
    private String typePoste;
    private double remuneration;
    private Date dateDebut;
    private Date dateFin;

    public PosteInterne(int id, String typePoste, double remuneration, Date dateDebut, Date dateFin) {
        this.id = id;
        this.typePoste = typePoste;
        this.remuneration = remuneration;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTypePoste() {
        return typePoste;
    }

    public void setTypePoste(String typePoste) {
        this.typePoste = typePoste;
    }

    public double getRemuneration() {
        return remuneration;
    }

    public void setRemuneration(double remuneration) {
        this.remuneration = remuneration;
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

    @Override
    public String toString() {
        return "PosteInterne{" +
                "id=" + id +
                ", typePoste='" + typePoste + '\'' +
                ", remuneration=" + remuneration +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                '}';
    }
}

