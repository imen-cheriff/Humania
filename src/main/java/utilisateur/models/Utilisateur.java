// ═══════════════════════════════════════════════════════════════════════
// FUSION COMPLÈTE: version propre + version ami
// AJOUTS vs version propre   : champs employé (posteActuel, managerId,
//   matricule, dateEmbauche, departement) + leurs getters/setters
// AJOUTS vs version ami       : isOnline, lastSeen + helpers communication
//   (getFullName, getInitials, isManager, getRoleAsString,
//    getCreatedAt/setCreatedAt, getAvatarUrl/setAvatarUrl)
// ═══════════════════════════════════════════════════════════════════════
package utilisateur.models;

import utilisateur.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Utilisateur {

    // ── Champs de base ────────────────────────────────────
    protected int           id;
    protected String        nom;
    protected String        prenom;
    protected String        email;
    protected String        username;
    protected String        numtel;
    protected String        pdp;
    protected String        motDePasse; // HASHED
    protected Role          role;
    protected String        statut;
    protected LocalDateTime dateCreation;
    protected byte[]        donneesFaciales;

    // ── MFA ───────────────────────────────────────────────
    private boolean mfaEnabled = false;
    private String  mfaSecret  = null;

    // ── Champs employé (stockés directement dans utilisateur) ──
    private String    posteActuel;
    private Integer   managerId;
    private String    matricule;
    private LocalDate dateEmbauche;
    private String    departement;

    // ── Champs présence (module communication) ────────────
    private boolean       isOnline;
    private LocalDateTime lastSeen;

    // ─────────────────────────────────────────────────────
    public Utilisateur() {}

    public Utilisateur(int id, String nom, String prenom, String email, String username,
                       String numtel, String pdp, String motDePasse, Role role, String statut,
                       LocalDateTime dateCreation, byte[] donneesFaciales) {
        this.id             = id;
        this.nom            = nom;
        this.prenom         = prenom;
        this.email          = email;
        this.username       = username;
        this.numtel         = numtel;
        this.pdp            = pdp;
        this.motDePasse     = motDePasse;
        this.role           = role;
        this.statut         = statut;
        this.dateCreation   = dateCreation;
        this.donneesFaciales = donneesFaciales;
    }

    // ── Getters/Setters champs de base ────────────────────

    public int     getId()                              { return id; }
    public void    setId(int id)                        { this.id = id; }

    public String  getNom()                             { return nom; }
    public void    setNom(String nom)                   { this.nom = nom; }

    public String  getPrenom()                          { return prenom; }
    public void    setPrenom(String prenom)             { this.prenom = prenom; }

    public String  getEmail()                           { return email; }
    public void    setEmail(String email)               { this.email = email; }

    public String  getUsername()                        { return username; }
    public void    setUsername(String username)         { this.username = username; }

    public String  getNumtel()                          { return numtel; }
    public void    setNumtel(String numtel)             { this.numtel = numtel; }

    public String  getPdp()                             { return pdp; }
    public void    setPdp(String pdp)                   { this.pdp = pdp; }

    public String  getMotDePasse()                      { return motDePasse; }
    public void    setMotDePasse(String motDePasse)     { this.motDePasse = motDePasse; }

    public Role    getRole()                            { return role; }
    public void    setRole(Role role)                   { this.role = role; }

    public String  getStatut()                          { return statut; }
    public void    setStatut(String statut)             { this.statut = statut; }

    public LocalDateTime getDateCreation()              { return dateCreation; }
    public void    setDateCreation(LocalDateTime v)     { this.dateCreation = v; }

    public byte[]  getDonneesFaciales()                 { return donneesFaciales; }
    public void    setDonneesFaciales(byte[] v)         { this.donneesFaciales = v; }

    // ── Getters/Setters MFA ───────────────────────────────

    public boolean isMfaEnabled()                       { return mfaEnabled; }
    public void    setMfaEnabled(boolean v)             { this.mfaEnabled = v; }

    public String  getMfaSecret()                       { return mfaSecret; }
    public void    setMfaSecret(String s)               { this.mfaSecret = s; }

    // ── Getters/Setters champs employé ────────────────────

    public String    getPosteActuel()                   { return posteActuel; }
    public void      setPosteActuel(String v)           { this.posteActuel = v; }

    public Integer   getManagerId()                     { return managerId; }
    public void      setManagerId(Integer v)            { this.managerId = v; }

    public String    getMatricule()                     { return matricule; }
    public void      setMatricule(String v)             { this.matricule = v; }

    public LocalDate getDateEmbauche()                  { return dateEmbauche; }
    public void      setDateEmbauche(LocalDate v)       { this.dateEmbauche = v; }

    public String    getDepartement()                   { return departement; }
    public void      setDepartement(String v)           { this.departement = v; }

    // ── Getters/Setters présence (module communication) ───

    public boolean       isOnline()                     { return isOnline; }
    public void          setOnline(boolean online)      { this.isOnline = online; }

    public LocalDateTime getLastSeen()                  { return lastSeen; }
    public void          setLastSeen(LocalDateTime v)   { this.lastSeen = v; }

    // ── Méthodes helper (module communication) ────────────

    /** Prénom + Nom concaténés, utilisé dans PostCardController, ChatController, etc. */
    public String getFullName() {
        String p = (prenom != null) ? prenom : "";
        String n = (nom    != null) ? nom    : "";
        return (p + " " + n).trim();
    }

    /** Initiales pour les cercles d'avatar dans Chat, Feed, PostCard. */
    public String getInitials() {
        StringBuilder sb = new StringBuilder();
        if (prenom != null && !prenom.isEmpty()) sb.append(prenom.charAt(0));
        if (nom    != null && !nom.isEmpty())    sb.append(nom.charAt(0));
        return sb.toString().toUpperCase();
    }

    /** Utilisé par le module communication pour l'interface basée sur les rôles. */
    public boolean isManager() {
        return this.role == Role.MANAGER || this.role == Role.RH || this.role == Role.ADMIN;
    }

    /** Le rôle sous forme de String (utilisé par le code communication). */
    public String getRoleAsString() {
        return this.role != null ? this.role.name() : "EMPLOYE";
    }

    /** Alias getCreatedAt() → dateCreation (compatibilité module communication). */
    public LocalDateTime getCreatedAt()                 { return dateCreation; }
    public void          setCreatedAt(LocalDateTime v)  { this.dateCreation = v; }

    /** Alias getAvatarUrl() → pdp (photo de profil, compatibilité module communication). */
    public String getAvatarUrl()                        { return pdp; }
    public void   setAvatarUrl(String avatarUrl)        { this.pdp = avatarUrl; }

    // ── Object overrides ──────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Utilisateur) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Utilisateur{id=" + id
                + ", nom='" + nom + "', prenom='" + prenom
                + "', email='" + email + "', role=" + role
                + ", statut='" + statut + "', mfaEnabled=" + mfaEnabled
                + ", isOnline=" + isOnline + '}';
    }
}