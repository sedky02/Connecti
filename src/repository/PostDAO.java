package repository;

import model.Comment;
import model.Post;

import java.util.List;

public interface PostDAO {
    boolean createPost(Post post);
    List<Post> findGlobalFeed(String currentUsername, int limit);
    boolean toggleLike(int postId, String username);
    boolean addComment(Comment comment);
    List<Comment> findCommentsByPost(int postId);
}
