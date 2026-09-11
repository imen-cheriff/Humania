package communication.models.enums;
public enum TypeReaction {
    LIKE("👍", "/emojis/like.png"),
    LOVE("❤️", "/emojis/love.png"),
    LAUGH("😂", "/emojis/laugh.png"),
    WOW("😮",  "/emojis/wow.png"),
    SAD("😢",  "/emojis/sad.png"),
    ANGRY("😠","/emojis/angry.png");

    private final String emoji;
    private final String imagePath;

    TypeReaction(String emoji, String imagePath) {
        this.emoji = emoji;
        this.imagePath = imagePath;
    }

    public String getEmoji() { return emoji; }
    public String getImagePath() { return imagePath; }
}