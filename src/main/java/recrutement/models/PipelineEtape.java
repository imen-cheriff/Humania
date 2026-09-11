package recrutement.models;

public class PipelineEtape {
    private int Id;
    private int PosteExterneId;
    private int Ordre;
    private String Libelle;
    private String DescriptionEtape;
    private int DureeMoyenne;
    private String ActionAutomatique;
    private Boolean Obligatoire;

    public PipelineEtape() {
    }

    public PipelineEtape(int id, int posteExterneId, int ordre, String libelle, String descriptionEtape, int dureeMoyenne, String actionAutomatique, Boolean obligatoire) {
        Id = id;
        PosteExterneId = posteExterneId;
        Ordre = ordre;
        Libelle = libelle;
        DescriptionEtape = descriptionEtape;
        DureeMoyenne = dureeMoyenne;
        ActionAutomatique = actionAutomatique;
        Obligatoire = obligatoire;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getPosteInterneId() {
        return PosteExterneId;
    }

    public void setPosteInterneId(int posteInterneId) {
        PosteExterneId = posteInterneId;
    }

    public int getOrdre() {
        return Ordre;
    }

    public void setOrdre(int ordre) {
        Ordre = ordre;
    }

    public String getLibelle() {
        return Libelle;
    }

    public void setLibelle(String libelle) {
        Libelle = libelle;
    }

    public String getDescriptionEtape() {
        return DescriptionEtape;
    }

    public void setDescriptionEtape(String descriptionEtape) {
        DescriptionEtape = descriptionEtape;
    }

    public int getDureeMoyenne() {
        return DureeMoyenne;
    }

    public void setDureeMoyenne(int dureeMoyenne) {
        DureeMoyenne = dureeMoyenne;
    }

    public String getActionAutomatique() {
        return ActionAutomatique;
    }

    public void setActionAutomatique(String actionAutomatique) {
        ActionAutomatique = actionAutomatique;
    }

    public Boolean getObligatoire() {
        return Obligatoire;
    }

    public void setObligatoire(Boolean obligatoire) {
        Obligatoire = obligatoire;
    }

    @Override
    public String toString() {
        return "PipelineEtape{" +
                "Id=" + Id +
                ", PosteInterneId=" + PosteExterneId +
                ", Ordre=" + Ordre +
                ", Libelle='" + Libelle + '\'' +
                ", DescriptionEtape='" + DescriptionEtape + '\'' +
                ", DureeMoyenne=" + DureeMoyenne +
                ", ActionAutomatique='" + ActionAutomatique + '\'' +
                ", Obligatoire=" + Obligatoire +
                '}';
    }
}
