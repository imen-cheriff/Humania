package recrutement.models;

import java.util.Date;

public class EvaluationCandidat {
    private int Id;
    private int CandidatureExterneId;
    private int EntretienId;
    private int NoteTechnique;
    private int NoteSavoirEtre;
    private int NoteMotivation;
    private int NoteCultureFit;
    private String Commentaire;
    private String Recommandation;
    private String PointFort;
    private String AxeAmelioration;
    private Date DateEvaluation;

    public EvaluationCandidat() {
    }

    public EvaluationCandidat(int id, int candidatureExterneId, int entretienId, int noteTechnique, int noteSavoirEtre, int noteMotivation, int noteCultureFit, String commentaire, String recommandation, String pointFort, String axeAmelioration, Date dateEvaluation) {
        Id = id;
        CandidatureExterneId = candidatureExterneId;
        EntretienId = entretienId;
        NoteTechnique = noteTechnique;
        NoteSavoirEtre = noteSavoirEtre;
        NoteMotivation = noteMotivation;
        NoteCultureFit = noteCultureFit;
        Commentaire = commentaire;
        Recommandation = recommandation;
        PointFort = pointFort;
        AxeAmelioration = axeAmelioration;
        DateEvaluation = dateEvaluation;
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

    public int getEntretienId() {
        return EntretienId;
    }

    public void setEntretienId(int entretienId) {
        EntretienId = entretienId;
    }

    public int getNoteTechnique() {
        return NoteTechnique;
    }

    public void setNoteTechnique(int noteTechnique) {
        NoteTechnique = noteTechnique;
    }

    public int getNoteSavoirEtre() {
        return NoteSavoirEtre;
    }

    public void setNoteSavoirEtre(int noteSavoirEtre) {
        NoteSavoirEtre = noteSavoirEtre;
    }

    public int getNoteMotivation() {
        return NoteMotivation;
    }

    public void setNoteMotivation(int noteMotivation) {
        NoteMotivation = noteMotivation;
    }

    public int getNoteCultureFit() {
        return NoteCultureFit;
    }

    public void setNoteCultureFit(int noteCultureFit) {
        NoteCultureFit = noteCultureFit;
    }

    public String getCommentaire() {
        return Commentaire;
    }

    public void setCommentaire(String commentaire) {
        Commentaire = commentaire;
    }

    public String getRecommandation() {
        return Recommandation;
    }

    public void setRecommandation(String recommandation) {
        Recommandation = recommandation;
    }

    public String getPointFort() {
        return PointFort;
    }

    public void setPointFort(String pointFort) {
        PointFort = pointFort;
    }

    public String getAxeAmelioration() {
        return AxeAmelioration;
    }

    public void setAxeAmelioration(String axeAmelioration) {
        AxeAmelioration = axeAmelioration;
    }

    public Date getDateEvaluation() {
        return DateEvaluation;
    }

    public void setDateEvaluation(Date dateEvaluation) {
        DateEvaluation = dateEvaluation;
    }

    @Override
    public String toString() {
        return "EvaluationCandidat{" +
                "Id=" + Id +
                ", CandidatureId=" + CandidatureExterneId +
                ", EntretienId=" + EntretienId +
                ", NoteTechnique=" + NoteTechnique +
                ", NoteSavoirEtre=" + NoteSavoirEtre +
                ", NoteMotivation=" + NoteMotivation +
                ", NoteCultureFit=" + NoteCultureFit +
                ", Commentaire='" + Commentaire + '\'' +
                ", Recommandation='" + Recommandation + '\'' +
                ", PointFort='" + PointFort + '\'' +
                ", AxeAmelioration='" + AxeAmelioration + '\'' +
                ", DateEvaluation=" + DateEvaluation +
                '}';
    }
}
