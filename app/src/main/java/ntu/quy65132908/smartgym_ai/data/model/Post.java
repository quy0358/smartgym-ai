package ntu.quy65132908.smartgym_ai.data.model;

public class Post {
    private String id;
    private String authorId;
    private String authorName;
    private String content;
    private int likes;
    private long createdAt;

    public Post() {}

    public Post(String id, String authorName, String content, int likes, long createdAt) {
        this.id = id;
        this.authorName = authorName;
        this.content = content;
        this.likes = likes;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName != null ? authorName : ""; }
    public String getContent() { return content != null ? content : ""; }
    public int getLikes() { return likes; }
    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setContent(String content) { this.content = content; }
    public void setLikes(int likes) { this.likes = likes; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
