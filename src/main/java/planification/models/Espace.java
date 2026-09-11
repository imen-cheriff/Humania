package planification.models;

import java.util.List;

public class Espace {

    // ── Champs ────────────────────────────────────────────
    private int          id;
    private String       nom;
    private int          capacite;
    private int          etage;
    private List<String> listeEquipements;
    private String       urlImage;
    private boolean      disponible;
    /** "reunion" ou "coworking" — correspond à l'ENUM en base */
    private String       typeEspace;

    // ── Constructeurs ─────────────────────────────────────

    /** Nouveau : sans id (INSERT) */
    public Espace(String nom, int capacite, int etage,
                  List<String> listeEquipements, String urlImage,
                  boolean disponible, String typeEspace) {
        this.nom              = nom;
        this.capacite         = capacite;
        this.etage            = etage;
        this.listeEquipements = listeEquipements;
        this.urlImage         = urlImage;
        this.disponible       = disponible;
        this.typeEspace       = typeEspace;
    }

    /** Avec id (UPDATE / lecture BDD) */
    public Espace(int id, String nom, int capacite, int etage,
                  List<String> listeEquipements, String urlImage,
                  boolean disponible, String typeEspace) {
        this.id               = id;
        this.nom              = nom;
        this.capacite         = capacite;
        this.etage            = etage;
        this.listeEquipements = listeEquipements;
        this.urlImage         = urlImage;
        this.disponible       = disponible;
        this.typeEspace       = typeEspace;
    }

    /** Constructeur legacy sans typeEspace (rétro-compatibilité) */
    public Espace(String nom, int capacite, int etage,
                  List<String> listeEquipements, String urlImage, boolean disponible) {
        this(nom, capacite, etage, listeEquipements, urlImage, disponible, "reunion");
    }

    /** Constructeur legacy avec id, sans typeEspace */
    public Espace(int id, String nom, int capacite, int etage,
                  List<String> listeEquipements, String urlImage, boolean disponible) {
        this(id, nom, capacite, etage, listeEquipements, urlImage, disponible, "reunion");
    }

    /** Constructeur vide */
    public Espace() {}

    // ── Validation ────────────────────────────────────────
    public void valider() {
        if (nom == null || nom.trim().isEmpty())
            throw new IllegalArgumentException("Le nom de l'espace ne peut pas être vide.");
        if (capacite <= 0)
            throw new IllegalArgumentException("La capacité doit être supérieure à 0.");
        if (typeEspace == null || (!typeEspace.equals("reunion") && !typeEspace.equals("coworking")))
            throw new IllegalArgumentException("Le type d'espace doit être 'reunion' ou 'coworking'.");
    }

    // ── Getters / Setters ─────────────────────────────────
    public int getId()                       { return id; }
    public void setId(int id)               { this.id = id; }

    public String getNom()                  { return nom; }
    public void setNom(String nom)          { this.nom = nom; }

    public int getCapacite()                { return capacite; }
    public void setCapacite(int capacite)   { this.capacite = capacite; }

    public int getEtage()                   { return etage; }
    public void setEtage(int etage)         { this.etage = etage; }

    public List<String> getListeEquipements()                        { return listeEquipements; }
    public void setListeEquipements(List<String> listeEquipements)   { this.listeEquipements = listeEquipements; }

    public String getUrlImage()             { return urlImage; }
    public void setUrlImage(String urlImage){ this.urlImage = urlImage; }

    public boolean isDisponible()               { return disponible; }
    public void setDisponible(boolean disponible){ this.disponible = disponible; }

    public String getTypeEspace()               { return typeEspace; }
    public void setTypeEspace(String typeEspace){ this.typeEspace = typeEspace; }

    /** Raccourcis booléens pratiques pour les controllers (insensible à la casse) */
    public boolean isReunion()    { return typeEspace != null && "reunion".equalsIgnoreCase(typeEspace.trim()); }
    public boolean isCoworking()  { return typeEspace != null && "coworking".equalsIgnoreCase(typeEspace.trim()); }

    // ── toString ──────────────────────────────────────────
    @Override
    public String toString() {
        return "Espace{id=" + id +
                ", nom='" + nom + '\'' +
                ", capacite=" + capacite +
                ", etage=" + etage +
                ", type=" + typeEspace +
                ", disponible=" + disponible +
                ", equipements=" + listeEquipements + '}';
    }
}