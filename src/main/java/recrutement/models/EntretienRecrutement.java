package recrutement.models;

import java.sql.Time;
import java.util.Date;

public class EntretienRecrutement {
    private int Id;
    private int CandidatureExterneId;
    private String TypeEntretien;
    private Date Date;
    private Time HeureDebut;
    private Time HeureFin;
    private String IntervieweursIds;
    private String Salle;
    private String URLvisio;
    private String StatutEntretien;
    private Double NoteEntretien;
    private int Duree;

    public EntretienRecrutement() {
    }

    public EntretienRecrutement(int id, int candidatureExterneId, String typeEntretien, Date date, Time heureDebut, Time heureFin, String intervieweursIds, String salle, String URLvisio, String statutEntretien, Double noteEntretien, int duree) {
        Id = id;
        CandidatureExterneId = candidatureExterneId;
        TypeEntretien = typeEntretien;
        Date = date;
        HeureDebut = heureDebut;
        HeureFin = heureFin;
        IntervieweursIds = intervieweursIds;
        Salle = salle;
        this.URLvisio = URLvisio;
        StatutEntretien = statutEntretien;
        NoteEntretien = noteEntretien;
        Duree = duree;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getCandidatureId() {
        return CandidatureExterneId;
    }

    public void setCandidatureId(int candidatureId) {
        CandidatureExterneId = candidatureId;
    }

    public String getTypeEntretien() {
        return TypeEntretien;
    }

    public void setTypeEntretien(String typeEntretien) {
        TypeEntretien = typeEntretien;
    }

    public Date getDate() {
        return Date;
    }

    public void setDate(Date date) {
        Date = date;
    }

    public Time getHeureDebut() {
        return HeureDebut;
    }

    public void setHeureDebut(Time heureDebut) {
        HeureDebut = heureDebut;
    }

    public Time getHeureFin() {
        return HeureFin;
    }

    public void setHeureFin(Time heureFin) {
        HeureFin = heureFin;
    }

    public String getIntervieweursIds() {
        return IntervieweursIds;
    }

    public void setIntervieweursIds(String intervieweursIds) {
        IntervieweursIds = intervieweursIds;
    }

    public String getSalle() {
        return Salle;
    }

    public void setSalle(String salle) {
        Salle = salle;
    }

    public String getURLvisio() {
        return URLvisio;
    }

    public void setURLvisio(String URLvisio) {
        this.URLvisio = URLvisio;
    }

    public String getStatutEntretien() {
        return StatutEntretien;
    }

    public void setStatutEntretien(String statutEntretien) {
        StatutEntretien = statutEntretien;
    }

    public Double getNoteEntretien() {
        return NoteEntretien;
    }

    public void setNoteEntretien(Double noteEntretien) {
        NoteEntretien = noteEntretien;
    }

    public int getDuree() {
        return Duree;
    }

    public void setDuree(int duree) {
        Duree = duree;
    }

    @Override
    public String toString() {
        return "EntretienRecrutement{" +
                "Id=" + Id +
                ", CandidatureId=" + CandidatureExterneId +
                ", TypeEntretien='" + TypeEntretien + '\'' +
                ", Date=" + Date +
                ", HeureDebut=" + HeureDebut +
                ", HeureFin=" + HeureFin +
                ", IntervieweursIds='" + IntervieweursIds + '\'' +
                ", Salle='" + Salle + '\'' +
                ", URLvisio='" + URLvisio + '\'' +
                ", StatutEntretien='" + StatutEntretien + '\'' +
                ", NoteEntretien=" + NoteEntretien +
                ", Duree=" + Duree +
                '}';
    }
}
