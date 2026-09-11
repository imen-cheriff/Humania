package communication.models;

/**
 * Représente une option de visibilité pour publier un post
 * Utilisée par MainController et PostCardController
 */
public class VisibilityOption {
    private String label;
    private String visibility;  // "PUBLIC" ou "GROUP"
    private Integer groupId;    // null si PUBLIC

    public VisibilityOption(String label, String visibility, Integer groupId) {
        this.label = label;
        this.visibility = visibility;
        this.groupId = groupId;
    }

    // Getters
    public String getLabel() {
        return label;
    }

    public String getVisibility() {
        return visibility;
    }

    public Integer getGroupId() {
        return groupId;
    }

    @Override
    public String toString() {
        return label;
    }
}