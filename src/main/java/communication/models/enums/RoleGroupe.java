package communication.models.enums;
public enum RoleGroupe {
    ADMIN("Admin"),
    MODERATEUR("Modérateur"),
    MEMBRE("Membre");

    private final String displayName;

    RoleGroupe(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}