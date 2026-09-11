package planification.models;

import java.util.Date;

public class ResevEspace {

    // ── Champs ────────────────────────────────────────────
    private int     id;
    private int     idEspace;
    private int     idEmploye;
    private Date    dateReservation;
    private Date    dateHeureDebut;
    private Date    dateHeureFin;
    private String  objectif;
    private boolean statut;
    private Date    creeLe;

    /**
     * Numéro de chaise réservée.
     * Null pour une salle de réunion (réservation complète).
     * Entier pour un espace coworking (réservation d'une chaise).
     */
    private Integer numeroChaise;

    // ── Constructeurs ─────────────────────────────────────

    public ResevEspace() {}

    /** Réservation salle de réunion (toute la salle — pas de chaise) */
    public ResevEspace(int idEspace, int idEmploye, Date dateReservation,
                       Date dateHeureDebut, Date dateHeureFin,
                       String objectif, boolean statut, Date creeLe) {
        this.idEspace        = idEspace;
        this.idEmploye       = idEmploye;
        this.dateReservation = dateReservation;
        this.dateHeureDebut  = dateHeureDebut;
        this.dateHeureFin    = dateHeureFin;
        this.objectif        = objectif;
        this.statut          = statut;
        this.creeLe          = creeLe;
        this.numeroChaise    = null;
    }

    /** Réservation coworking (chaise spécifique) */
    public ResevEspace(int idEspace, int idEmploye, Date dateReservation,
                       Date dateHeureDebut, Date dateHeureFin,
                       String objectif, boolean statut, Date creeLe, int numeroChaise) {
        this(idEspace, idEmploye, dateReservation, dateHeureDebut, dateHeureFin,
                objectif, statut, creeLe);
        this.numeroChaise = numeroChaise;
    }

    // ── Validation ────────────────────────────────────────
    public void valider() {
        if (idEspace <= 0)
            throw new IllegalArgumentException("L'ID de l'espace est invalide.");
        if (idEmploye <= 0)
            throw new IllegalArgumentException("L'ID de l'employé est invalide.");
        if (dateReservation == null)
            throw new IllegalArgumentException("La date de réservation est obligatoire.");
        if (dateHeureDebut == null || dateHeureFin == null)
            throw new IllegalArgumentException("Les heures de début et de fin sont obligatoires.");
        if (dateHeureFin.before(dateHeureDebut))
            throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début.");
    }

    // ── Getters / Setters ─────────────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                  { this.id = id; }

    public int getIdEspace()                    { return idEspace; }
    public void setIdEspace(int idEspace)      { this.idEspace = idEspace; }

    public int getIdEmploye()                   { return idEmploye; }
    public void setIdEmploye(int idEmploye)    { this.idEmploye = idEmploye; }

    public Date getDateReservation()            { return dateReservation; }
    public void setDateReservation(Date d)     { this.dateReservation = d; }

    public Date getDateHeureDebut()             { return dateHeureDebut; }
    public void setDateHeureDebut(Date d)      { this.dateHeureDebut = d; }

    public Date getDateHeureFin()               { return dateHeureFin; }
    public void setDateHeureFin(Date d)        { this.dateHeureFin = d; }

    public String getObjectif()                 { return objectif; }
    public void setObjectif(String objectif)   { this.objectif = objectif; }

    public boolean isStatut()                   { return statut; }
    public void setStatut(boolean statut)      { this.statut = statut; }

    public Date getCreeLe()                     { return creeLe; }
    public void setCreeLe(Date creeLe)         { this.creeLe = creeLe; }

    /** Null = réservation salle entière ; non-null = numéro de chaise coworking */
    public Integer getNumeroChaise()                    { return numeroChaise; }
    public void setNumeroChaise(Integer numeroChaise)  { this.numeroChaise = numeroChaise; }

    public boolean isCoworkingReservation()     { return numeroChaise != null; }
}