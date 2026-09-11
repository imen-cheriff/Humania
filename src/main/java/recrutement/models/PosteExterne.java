package recrutement.models;

import java.util.Date;

public class PosteExterne {
    private int id;
    private String titre;
    private String Description;
    private String TypeContrat;
    private Double Salaire;
    private String Competences_Requises;
    private int Experience_Requise;
    private String Niveau_Etude_Requis;
    private String Statut;
    private Date DatePublication;
    private Date DateCloture;
    private Boolean PublicExterne;
    private int ResponsableRHid;
    private int NombreEmploye;
    private int Priorite;

    public PosteExterne() {
    }

    public PosteExterne(int id, String titre, String description, String typeContrat, Double salaire, String competences_Requises, int experience_Requise, String niveau_Etude_Requis, String statut, Date datePublication, Date dateCloture, Boolean publicExterne, int responsableRHid, int nombreEmploye, int priorite) {
        this.id = id;
        this.titre = titre;
        this.Description = description;
        this.TypeContrat = typeContrat;
        this.Salaire = salaire;
        this.Competences_Requises = competences_Requises;
        this.Experience_Requise = experience_Requise;
        this.Niveau_Etude_Requis = niveau_Etude_Requis;
        this.Statut = statut;
        this.DatePublication = datePublication;
        this.DateCloture = dateCloture;
        this.PublicExterne = publicExterne;
        this.ResponsableRHid = responsableRHid;
        this.NombreEmploye = nombreEmploye;
        this.Priorite = priorite;
    }

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

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getTypeContrat() {
        return TypeContrat;
    }

    public void setTypeContrat(String typeContrat) {
        TypeContrat = typeContrat;
    }


    public Double getSalaire() {
        return Salaire;
    }

    public void setSalaire(Double salaire) {
        Salaire = salaire;
    }

    public String getCompetences_Requises() {
        return Competences_Requises;
    }

    public void setCompetences_Requises(String competences_Requises) {
        Competences_Requises = competences_Requises;
    }

    public int getExperience_Requise() {
        return Experience_Requise;
    }

    public void setExperience_Requise(int experience_Requise) {
        Experience_Requise = experience_Requise;
    }

    public String getNiveau_Etude_Requis() {
        return Niveau_Etude_Requis;
    }

    public void setNiveau_Etude_Requis(String niveau_Etude_Requis) {
        Niveau_Etude_Requis = niveau_Etude_Requis;
    }

    public String getStatut() {
        return Statut;
    }

    public void setStatut(String statut) {
        Statut = statut;
    }

    public Date getDatePublication() {
        return DatePublication;
    }

    public void setDatePublication(Date datePublication) {
        DatePublication = datePublication;
    }

    public Date getDateCloture() {
        return DateCloture;
    }

    public void setDateCloture(Date dateCloture) {
        DateCloture = dateCloture;
    }

    public Boolean getPublicExterne() {
        return PublicExterne;
    }

    public void setPublicExterne(Boolean publicExterne) {
        PublicExterne = publicExterne;
    }

    public int getResponsableRHid() {
        return ResponsableRHid;
    }

    public void setResponsableRHid(int responsableRHid) {
        ResponsableRHid = responsableRHid;
    }

    public int getNombreEmploye() {
        return NombreEmploye;
    }

    public void setNombreEmploye(int nombrePostes) {
        NombreEmploye = nombrePostes;
    }

    public int getPriorite() {
        return Priorite;
    }

    public void setPriorite(int priorite) {
        Priorite = priorite;
    }

    @Override
    public String toString() {
        return "PosteInterne{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", Description='" + Description + '\'' +
                ", TypeContrat='" + TypeContrat + '\'' +
                ", Salaire=" + Salaire +
                ", Competences_Requises='" + Competences_Requises + '\'' +
                ", Experience_Requise=" + Experience_Requise +
                ", Niveau_Etude_Requis='" + Niveau_Etude_Requis + '\'' +
                ", Statut='" + Statut + '\'' +
                ", DatePublication=" + DatePublication +
                ", DateCloture=" + DateCloture +
                ", PublicExterne=" + PublicExterne +
                ", ResponsableRHid=" + ResponsableRHid +
                ", NombreEmploye=" + NombreEmploye +
                ", Priorite=" + Priorite +
                '}';
    }
}
