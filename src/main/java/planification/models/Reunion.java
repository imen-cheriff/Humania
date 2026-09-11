package planification.models;

import java.util.Date;

public class Reunion {

    private int id;
    private String titre;
    private String description;
    private Date dateHeureDebut;
    private Date dateHeureFin;
    private int idSalle;
    private String nomOrganisateur;
    private String emailOrganisateur;
    private String participants;
    private boolean statut;
    private boolean enLigne;
    private Date creeLe;
    private long   zoomMeetingId;   // numeric Zoom meeting ID (0 = none)
    private String zoomJoinUrl;     // participant join link
    private String zoomStartUrl;    // host start link
    private String zoomPassword;

    public Reunion() {
    }

    public Reunion(int id, String titre, String description, Date dateHeureDebut, Date dateHeureFin,
            int idSalle, String nomOrganisateur, String emailOrganisateur, String participants,
            boolean statut, boolean enLigne, Date creeLe) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateHeureDebut = dateHeureDebut;
        this.dateHeureFin = dateHeureFin;
        this.idSalle = idSalle;
        this.nomOrganisateur = nomOrganisateur;
        this.emailOrganisateur = emailOrganisateur;
        this.participants = participants;
        this.statut = statut;
        this.enLigne = enLigne;
        this.creeLe = creeLe;
    }
    public long getZoomMeetingId()              { return zoomMeetingId; }
    public void setZoomMeetingId(long id)       { this.zoomMeetingId = id; }

    public String getZoomJoinUrl()              { return zoomJoinUrl; }
    public void setZoomJoinUrl(String url)      { this.zoomJoinUrl = url; }

    public String getZoomStartUrl()             { return zoomStartUrl; }
    public void setZoomStartUrl(String url)     { this.zoomStartUrl = url; }

    public String getZoomPassword()             { return zoomPassword; }
    public void setZoomPassword(String pwd)     { this.zoomPassword = pwd; }

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
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateHeureDebut() {
        return dateHeureDebut;
    }

    public void setDateHeureDebut(Date dateHeureDebut) {
        this.dateHeureDebut = dateHeureDebut;
    }

    public Date getDateHeureFin() {
        return dateHeureFin;
    }

    public void setDateHeureFin(Date dateHeureFin) {
        this.dateHeureFin = dateHeureFin;
    }

    public int getIdSalle() {
        return idSalle;
    }

    public void setIdSalle(int idSalle) {
        this.idSalle = idSalle;
    }

    public String getNomOrganisateur() {
        return nomOrganisateur;
    }

    public void setNomOrganisateur(String nomOrganisateur) {
        this.nomOrganisateur = nomOrganisateur;
    }

    public String getEmailOrganisateur() {
        return emailOrganisateur;
    }

    public void setEmailOrganisateur(String emailOrganisateur) {
        this.emailOrganisateur = emailOrganisateur;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public boolean isStatut() {
        return statut;
    }

    public void setStatut(boolean statut) {
        this.statut = statut;
    }

    public boolean isEnLigne() {
        return enLigne;
    }

    public void setEnLigne(boolean enLigne) {
        this.enLigne = enLigne;
    }

    public Date getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(Date creeLe) {
        this.creeLe = creeLe;
    }

    @Override
    public String toString() {
        return "Reunion{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", dateHeureDebut=" + dateHeureDebut +
                ", dateHeureFin=" + dateHeureFin +
                ", idSalle=" + idSalle +
                ", nomOrganisateur='" + nomOrganisateur + '\'' +
                ", emailOrganisateur='" + emailOrganisateur + '\'' +
                ", participants='" + participants + '\'' +
                ", statut=" + statut +
                ", enLigne=" + enLigne +
                ", creeLe=" + creeLe +
                '}';
    }

    public void valider() {
        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de la réunion ne doit pas être vide.");
        }
        if (description == null) {
            description = "";
        }
        if (!enLigne && idSalle <= 0) {
            throw new IllegalArgumentException("L'identifiant de la salle est obligatoire et doit être positif.");
        }
        if (nomOrganisateur == null || nomOrganisateur.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'organisateur ne doit pas être vide.");
        }
        if (emailOrganisateur == null || emailOrganisateur.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email de l'organisateur ne doit pas être vide.");
        }
        if (dateHeureDebut == null || dateHeureFin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (dateHeureFin.before(dateHeureDebut)) {
            throw new IllegalArgumentException(
                    "La date/heure de fin doit être postérieure ou égale à la date/heure de début.");
        }
    }
}
