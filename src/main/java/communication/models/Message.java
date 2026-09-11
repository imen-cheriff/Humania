package communication.models;

import java.time.LocalDateTime;

public class Message {
    private String id;
    private String content;
    private int senderId;
    private String senderName;
    private String senderAvatar;
    private Integer receiverId;   // null si groupe
    private Integer groupId;      // null si privé
    private LocalDateTime createdAt;
    private boolean isRead;

    public Message() {}

    public Message(String content, int senderId, String senderName,
                   String senderAvatar, Integer receiverId, Integer groupId) {
        this.content = content;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public Integer getReceiverId() { return receiverId; }
    public void setReceiverId(Integer receiverId) { this.receiverId = receiverId; }
    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}