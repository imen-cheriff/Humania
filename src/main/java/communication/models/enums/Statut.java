package communication.models.enums;

public enum Statut {
    ACTIF("Actif"),
    SUPPRIME("Supprimé"),
    ARCHIVE("Archivé");

    private final String displayName;

    Statut(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
