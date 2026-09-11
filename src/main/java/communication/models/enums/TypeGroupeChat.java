package communication.models.enums;

public enum TypeGroupeChat {
    EQUIPE("Équipe"),
    PROJET("Projet"),
    GENERAL("Général");

    private final String displayName;

    TypeGroupeChat(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}