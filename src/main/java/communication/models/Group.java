package communication.models;

import java.time.LocalDateTime;

public class Group {
    private int id;
    private String name;
    private String description;
    private int createdById;
    private LocalDateTime createdAt;
    private String imageUrl;
    private int memberCount;

    public Group() {
        this.createdAt = LocalDateTime.now();
        this.memberCount = 0;
    }

    public Group(int id, String name, String description, int createdById) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdById = createdById;
        this.createdAt = LocalDateTime.now();
        this.memberCount = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCreatedById() {
        return createdById;
    }

    public void setCreatedById(int createdById) {
        this.createdById = createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && createdById > 0;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdById=" + createdById +
                ", createdAt=" + createdAt +
                ", memberCount=" + memberCount +
                '}';
    }
}