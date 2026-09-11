package recrutement.models;

import java.util.Date;

public class MatchingAI {
    private int Id;
    private int CandidaturesId;
    private int PosteInterneId;
    private Double ScoreGlobal;
    private Double ScoreCompetences;
    private Double ScoreExperience;
    private Double ScoreFormation;
    private Double ScoreLocalisation;
    private String Raisonnement;
    private String CompetencesManquantes;
    private Date DateCalcul;
    private String VersionModele;

    public MatchingAI() {
    }

    public MatchingAI(int id, int candidaturesId, int posteInterneId, Double scoreGlobal, Double scoreCompetences, Double scoreExperience, Double scoreFormation, Double scoreLocalisation, String raisonnement, String competencesManquantes, Date dateCalcul, String versionModele) {
        Id = id;
        CandidaturesId = candidaturesId;
        PosteInterneId = posteInterneId;
        ScoreGlobal = scoreGlobal;
        ScoreCompetences = scoreCompetences;
        ScoreExperience = scoreExperience;
        ScoreFormation = scoreFormation;
        ScoreLocalisation = scoreLocalisation;
        Raisonnement = raisonnement;
        CompetencesManquantes = competencesManquantes;
        DateCalcul = dateCalcul;
        VersionModele = versionModele;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getCandidaturesId() {
        return CandidaturesId;
    }

    public void setCandidaturesId(int candidaturesId) {
        CandidaturesId = candidaturesId;
    }

    public int getPosteInterneId() {
        return PosteInterneId;
    }

    public void setPosteInterneId(int posteInterneId) {
        PosteInterneId = posteInterneId;
    }

    public Double getScoreGlobal() {
        return ScoreGlobal;
    }

    public void setScoreGlobal(Double scoreGlobal) {
        ScoreGlobal = scoreGlobal;
    }

    public Double getScoreCompetences() {
        return ScoreCompetences;
    }

    public void setScoreCompetences(Double scoreCompetences) {
        ScoreCompetences = scoreCompetences;
    }

    public Double getScoreExperience() {
        return ScoreExperience;
    }

    public void setScoreExperience(Double scoreExperience) {
        ScoreExperience = scoreExperience;
    }

    public Double getScoreFormation() {
        return ScoreFormation;
    }

    public void setScoreFormation(Double scoreFormation) {
        ScoreFormation = scoreFormation;
    }

    public Double getScoreLocalisation() {
        return ScoreLocalisation;
    }

    public void setScoreLocalisation(Double scoreLocalisation) {
        ScoreLocalisation = scoreLocalisation;
    }

    public String getRaisonnement() {
        return Raisonnement;
    }

    public void setRaisonnement(String raisonnement) {
        Raisonnement = raisonnement;
    }

    public String getCompetencesManquantes() {
        return CompetencesManquantes;
    }

    public void setCompetencesManquantes(String competencesManquantes) {
        CompetencesManquantes = competencesManquantes;
    }

    public Date getDateCalcul() {
        return DateCalcul;
    }

    public void setDateCalcul(Date dateCalcul) {
        DateCalcul = dateCalcul;
    }

    public String getVersionModele() {
        return VersionModele;
    }

    public void setVersionModele(String versionModele) {
        VersionModele = versionModele;
    }

    @Override
    public String toString() {
        return "MatchingAI{" +
                "Id=" + Id +
                ", CandidaturesId=" + CandidaturesId +
                ", PosteInterneId=" + PosteInterneId +
                ", ScoreGlobal=" + ScoreGlobal +
                ", ScoreCompetences=" + ScoreCompetences +
                ", ScoreExperience=" + ScoreExperience +
                ", ScoreFormation=" + ScoreFormation +
                ", ScoreLocalisation=" + ScoreLocalisation +
                ", Raisonnement='" + Raisonnement + '\'' +
                ", CompetencesManquantes='" + CompetencesManquantes + '\'' +
                ", DateCalcul=" + DateCalcul +
                ", VersionModele='" + VersionModele + '\'' +
                '}';
    }
}
