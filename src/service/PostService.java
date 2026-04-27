package service;

import model.Comment;
import model.Post;
import repository.PostDAO;
import repository.impl.PostDAOImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostService {
    private final PostDAO postDAO;

    public PostService() {
        this.postDAO = new PostDAOImpl();
    }

    public PostService(PostDAO postDAO) {
        this.postDAO = postDAO;
    }

    public boolean createPost(String username, String content) {
        if (username == null || username.isBlank() || content == null || content.isBlank()) {
            return false;
        }
        Post post = new Post(username.trim(), content.trim(), LocalDateTime.now());
        return postDAO.createPost(post);
    }

    public List<Post> getGlobalFeed(String currentUsername) {
        List<Post> posts = postDAO.findGlobalFeed(currentUsername, 100);
        for (Post post : posts) {
            post.setComments(postDAO.findCommentsByPost(post.getId()));
        }
        return posts;
    }

    public boolean likePost(int postId, String username) {
        if (postId <= 0 || username == null || username.isBlank()) {
            return false;
        }
        return postDAO.toggleLike(postId, username.trim());
    }

    public boolean addComment(int postId, String username, String content) {
        if (postId <= 0 || username == null || username.isBlank() || content == null || content.isBlank()) {
            return false;
        }
        Comment comment = new Comment(postId, username.trim(), content.trim(), LocalDateTime.now());
        return postDAO.addComment(comment);
    }

    public List<Comment> getComments(int postId) {
        if (postId <= 0) {
            return new ArrayList<>();
        }
        return postDAO.findCommentsByPost(postId);
    }
}
