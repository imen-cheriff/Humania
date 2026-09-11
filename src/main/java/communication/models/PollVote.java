package communication.models;

import java.time.LocalDateTime;

public class PollVote {
    private Integer id;
    private Integer pollId;
    private Integer optionId;
    private Integer userId;
    private LocalDateTime votedAt;

    public PollVote() {}

    public PollVote(Integer pollId, Integer optionId, Integer userId) {
        this.pollId = pollId;
        this.optionId = optionId;
        this.userId = userId;
        this.votedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPollId() { return pollId; }
    public void setPollId(Integer pollId) { this.pollId = pollId; }
    public Integer getOptionId() { return optionId; }
    public void setOptionId(Integer optionId) { this.optionId = optionId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
}