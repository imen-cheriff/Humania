package communication.models;

public class PollOption {
    private Integer id;
    private Integer pollId;      // FK vers Poll
    private String optionText;
    private Integer voteCount;
    private Integer optionOrder; // Pour l'ordre des options

    // Constructeurs
    public PollOption() {}

    public PollOption(Integer pollId, String optionText, Integer optionOrder) {
        this.pollId = pollId;
        this.optionText = optionText;
        this.optionOrder = optionOrder;
        this.voteCount = 0;
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPollId() {
        return pollId;
    }

    public void setPollId(Integer pollId) {
        this.pollId = pollId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Integer voteCount) {
        this.voteCount = voteCount;
    }

    public Integer getOptionOrder() {
        return optionOrder;
    }

    public void setOptionOrder(Integer optionOrder) {
        this.optionOrder = optionOrder;
    }

    @Override
    public String toString() {
        return "PollOption{" +
                "id=" + id +
                ", optionText='" + optionText + '\'' +
                ", voteCount=" + voteCount +
                '}';
    }
}