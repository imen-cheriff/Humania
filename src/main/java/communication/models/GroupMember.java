package communication.models;

import java.time.LocalDateTime;

public class GroupMember {
    private int id;
    private int groupId;
    private int userId;
    private String role;
    private LocalDateTime joinedAt;

    public GroupMember() {
        this.joinedAt = LocalDateTime.now();
        this.role = "MEMBER";
    }

    public GroupMember(int groupId, int userId, String role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", role='" + role + '\'' +
                ", joinedAt=" + joinedAt +
                '}';
    }
}