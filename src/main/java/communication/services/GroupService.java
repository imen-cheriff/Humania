package communication.services;

import communication.models.Group;
import communication.models.GroupMember;
import communication.models.Notification;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupService {

    private Connection cnx;

    public GroupService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    public void createGroup(Group group) {
        String req = "INSERT INTO `GROUPS` (name, description, createdById, createdAt, imageUrl) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, group.getName());
            pstm.setString(2, group.getDescription());
            pstm.setInt(3, group.getCreatedById());
            pstm.setTimestamp(4, Timestamp.valueOf(group.getCreatedAt()));
            pstm.setString(5, group.getImageUrl());

            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                group.setId(rs.getInt(1));
            }

            addMember(group.getId(), group.getCreatedById(), "ADMIN");

            System.out.println("✅ Groupe créé : " + group.getName());

        } catch (SQLException e) {
            System.err.println("❌ Erreur création groupe : " + e.getMessage());
        }
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        String req = "SELECT * FROM `GROUPS` ORDER BY name ASC";

        try {
            Statement stm = this.cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération groupes : " + e.getMessage());
        }

        return groups;
    }

    public Group findById(int groupId) {
        String req = "SELECT * FROM `GROUPS` WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return mapResultSetToGroup(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche groupe : " + e.getMessage());
        }

        return null;
    }

    public List<Group> findByUserId(int userId) {
        List<Group> groups = new ArrayList<>();
        String req = "SELECT g.* FROM `GROUPS` g " +
                "INNER JOIN GROUPMEMBER gm ON g.id = gm.groupId " +
                "WHERE gm.userId = ? " +
                "ORDER BY g.name ASC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche groupes user : " + e.getMessage());
        }

        return groups;
    }

    public void updateGroup(Group group) {
        String req = "UPDATE `GROUPS` SET name = ?, description = ?, imageUrl = ? WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setString(1, group.getName());
            pstm.setString(2, group.getDescription());
            pstm.setString(3, group.getImageUrl());
            pstm.setInt(4, group.getId());

            pstm.executeUpdate();
            System.out.println("✅ Groupe mis à jour : " + group.getId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour groupe : " + e.getMessage());
        }
    }

    public void deleteGroup(int groupId) {
        String req = "DELETE FROM `GROUPS` WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            pstm.executeUpdate();

            System.out.println("✅ Groupe supprimé : " + groupId);

        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression groupe : " + e.getMessage());
        }
    }

    public void addMember(int groupId, int userId, String role) {
        String req = "INSERT INTO GROUPMEMBER (groupId, userId, role, joinedAt) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            pstm.setInt(2, userId);
            pstm.setString(3, role);
            pstm.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstm.executeUpdate();
            incrementMemberCount(groupId);

            // ✅ Notification au membre ajouté (pas pour le créateur/admin lui-même)
            if (!"ADMIN".equals(role)) {
                Group group = findById(groupId);
                String groupName = group != null ? group.getName() : "un groupe";

                NotificationService notificationService = new NotificationService();
                Notification notif = new Notification();
                notif.setUserId(userId);                    // destinataire = membre ajouté
                notif.setType("GROUP");
                notif.setTitre("Ajout à un groupe");
                notif.setMessage("Vous avez été ajouté au groupe \"" + groupName + "\".");
                notif.setRelatedUserId(groupId);            // on stocke groupId ici pour navigation future
                notif.setSeen(false);
                notif.setDateCreation(LocalDateTime.now());
                notificationService.add(notif);

                System.out.println("✅ Notification envoyée au membre : userId=" + userId);
            }

            System.out.println("✅ Membre ajouté au groupe " + groupId);

        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout membre : " + e.getMessage());
        }
    }
    public void removeMember(int groupId, int userId) {
        String req = "DELETE FROM GROUPMEMBER WHERE groupId = ? AND userId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            pstm.setInt(2, userId);

            int rowsAffected = pstm.executeUpdate();

            if (rowsAffected > 0) {
                decrementMemberCount(groupId);
                System.out.println("✅ Membre retiré du groupe " + groupId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur retrait membre : " + e.getMessage());
        }
    }

    public List<GroupMember> getMembers(int groupId) {
        List<GroupMember> members = new ArrayList<>();
        String req = "SELECT * FROM GROUPMEMBER WHERE groupId = ? ORDER BY joinedAt ASC";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                members.add(mapResultSetToGroupMember(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération membres : " + e.getMessage());
        }

        return members;
    }

    public boolean isMember(int groupId, int userId) {
        String req = "SELECT COUNT(*) as count FROM GROUPMEMBER WHERE groupId = ? AND userId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            pstm.setInt(2, userId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification membre : " + e.getMessage());
        }

        return false;
    }

    public boolean isAdmin(int groupId, int userId) {
        String req = "SELECT role FROM GROUPMEMBER WHERE groupId = ? AND userId = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            pstm.setInt(2, userId);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                return "ADMIN".equals(rs.getString("role"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification admin : " + e.getMessage());
        }

        return false;
    }

    public int getUnreadCount(int groupId, int userId) {
        return 0;
    }

    private void incrementMemberCount(int groupId) {
        String req = "UPDATE `GROUPS` SET memberCount = memberCount + 1 WHERE id = ?";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur incrémentation membres : " + e.getMessage());
        }
    }

    private void decrementMemberCount(int groupId) {
        String req = "UPDATE `GROUPS` SET memberCount = memberCount - 1 WHERE id = ? AND memberCount > 0";

        try {
            PreparedStatement pstm = this.cnx.prepareStatement(req);
            pstm.setInt(1, groupId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur décrémentation membres : " + e.getMessage());
        }
    }

    private Group mapResultSetToGroup(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setCreatedById(rs.getInt("createdById"));
        group.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        group.setImageUrl(rs.getString("imageUrl"));
        group.setMemberCount(rs.getInt("memberCount"));
        return group;
    }

    private GroupMember mapResultSetToGroupMember(ResultSet rs) throws SQLException {
        GroupMember member = new GroupMember();
        member.setId(rs.getInt("id"));
        member.setGroupId(rs.getInt("groupId"));
        member.setUserId(rs.getInt("userId"));
        member.setRole(rs.getString("role"));
        member.setJoinedAt(rs.getTimestamp("joinedAt").toLocalDateTime());
        return member;
    }
}