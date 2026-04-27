package model;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private int postId;
    private String username;
    private String content;
    private LocalDateTime createdAt;

    public Comment() {
    }

    public Comment(int id, int postId, String username, String content, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.username = username;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Comment(int postId, String username, String content, LocalDateTime createdAt) {
        this.postId = postId;
        this.username = username;
        this.content = content;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
