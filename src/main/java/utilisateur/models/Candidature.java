package utilisateur.models;

import java.time.LocalDateTime;

/**
 * Model for the database table <code>candidature</code>.
 */
public class Candidature {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private Integer candidatId;
    private Integer employeId;
    private String typeCandidat;
    private String statut;
    private String etapePipeline;
    private Double scoringIa;
    private String cvUrl;
    private String lettreMotivationUrl;
    private Double salairePretendu;
    private String commentairesRh;
    private LocalDateTime dateDepot;

    /**
     * Transient flag set by the service layer after a DB-level JOIN check.
     * Do NOT derive this from employeId alone — dummy/legacy employeId values
     * in the DB would cause false positives.
     */
    private boolean alreadyConverted = false;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getCandidatId() { return candidatId; }
    public void setCandidatId(Integer candidatId) { this.candidatId = candidatId; }

    public Integer getEmployeId() { return employeId; }
    public void setEmployeId(Integer employeId) { this.employeId = employeId; }

    public String getTypeCandidat() { return typeCandidat; }
    public void setTypeCandidat(String typeCandidat) { this.typeCandidat = typeCandidat; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getEtapePipeline() { return etapePipeline; }
    public void setEtapePipeline(String etapePipeline) { this.etapePipeline = etapePipeline; }

    public Double getScoringIa() { return scoringIa; }
    public void setScoringIa(Double scoringIa) { this.scoringIa = scoringIa; }

    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }

    public String getLettreMotivationUrl() { return lettreMotivationUrl; }
    public void setLettreMotivationUrl(String lettreMotivationUrl) { this.lettreMotivationUrl = lettreMotivationUrl; }

    public Double getSalairePretendu() { return salairePretendu; }
    public void setSalairePretendu(Double salairePretendu) { this.salairePretendu = salairePretendu; }

    public String getCommentairesRh() { return commentairesRh; }
    public void setCommentairesRh(String commentairesRh) { this.commentairesRh = commentairesRh; }

    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }

    /**
     * Set by CandidatureService after verifying via JOIN that employe_id
     * references a real utilisateur row. Never set this based on employeId alone.
     */
    public void setAlreadyConverted(boolean alreadyConverted) {
        this.alreadyConverted = alreadyConverted;
    }

    /**
     * Returns true only if the service layer confirmed this candidature
     * is linked to an existing utilisateur record.
     */
    public boolean isAlreadyConverted() {
        return alreadyConverted;
    }
}