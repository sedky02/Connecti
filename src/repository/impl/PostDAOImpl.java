package repository.impl;

import database.DatabaseConnection;
import model.Comment;
import model.Post;
import repository.PostDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostDAOImpl implements PostDAO {
    private static volatile boolean schemaReady;

    public PostDAOImpl() {
        ensureSchema();
    }

    @Override
    public boolean createPost(Post post) {
        String sql = "INSERT INTO posts (user_id, content, created_at) " +
                "SELECT id, ?, ? FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getContent());
            ps.setTimestamp(2, Timestamp.valueOf(post.getCreatedAt()));
            ps.setString(3, post.getUsername());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<Post> findGlobalFeed(String currentUsername, int limit) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.id, u.username, p.content, p.created_at, " +
                "COUNT(DISTINCT pl.user_id) AS like_count, " +
                "MAX(CASE WHEN pl.user_id = cu.id THEN 1 ELSE 0 END) AS liked_by_me " +
                "FROM posts p " +
                "JOIN users u ON u.id = p.user_id " +
                "LEFT JOIN users cu ON cu.username = ? " +
                "LEFT JOIN post_likes pl ON pl.post_id = p.id " +
                "GROUP BY p.id, u.username, p.content, p.created_at " +
                "ORDER BY p.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUsername);
            ps.setInt(2, Math.max(limit, 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(new Post(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getInt("like_count"),
                            rs.getInt("liked_by_me") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            return posts;
        }
        return posts;
    }

    @Override
    public boolean toggleLike(int postId, String username) {
        String existsSql = "SELECT 1 FROM post_likes pl " +
                "JOIN users u ON u.id = pl.user_id " +
                "WHERE pl.post_id = ? AND u.username = ?";
        String insertSql = "INSERT INTO post_likes (post_id, user_id) " +
                "SELECT ?, id FROM users WHERE username = ?";
        String deleteSql = "DELETE pl FROM post_likes pl " +
                "JOIN users u ON u.id = pl.user_id " +
                "WHERE pl.post_id = ? AND u.username = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean exists;
            try (PreparedStatement existsPs = conn.prepareStatement(existsSql)) {
                existsPs.setInt(1, postId);
                existsPs.setString(2, username);
                try (ResultSet rs = existsPs.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                    deletePs.setInt(1, postId);
                    deletePs.setString(2, username);
                    if (deletePs.executeUpdate() <= 0) {
                        conn.rollback();
                        return false;
                    }
                }
            } else {
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, postId);
                    insertPs.setString(2, username);
                    if (insertPs.executeUpdate() <= 0) {
                        conn.rollback();
                        return false;
                    }
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean addComment(Comment comment) {
        String sql = "INSERT INTO post_comments (post_id, user_id, content, created_at) " +
                "SELECT ?, id, ?, ? FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, comment.getPostId());
            ps.setString(2, comment.getContent());
            ps.setTimestamp(3, Timestamp.valueOf(comment.getCreatedAt()));
            ps.setString(4, comment.getUsername());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<Comment> findCommentsByPost(int postId) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT pc.id, pc.post_id, u.username, pc.content, pc.created_at FROM post_comments pc " +
                "JOIN users u ON u.id = pc.user_id " +
                "WHERE pc.post_id = ? ORDER BY pc.created_at ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(new Comment(
                            rs.getInt("id"),
                            rs.getInt("post_id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            return comments;
        }
        return comments;
    }

    private void ensureSchema() {
        if (schemaReady) {
            return;
        }
        synchronized (PostDAOImpl.class) {
            if (schemaReady) {
                return;
            }
            String createPosts = "CREATE TABLE IF NOT EXISTS posts (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "content TEXT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_posts_user (user_id), " +
                    "INDEX idx_posts_created (created_at)" +
                    ")";
            String createLikes = "CREATE TABLE IF NOT EXISTS post_likes (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "post_id BIGINT NOT NULL, " +
                    "user_id BIGINT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY unique_user_post_like (post_id, user_id), " +
                    "INDEX idx_likes_post (post_id), " +
                    "INDEX idx_likes_user (user_id)" +
                    ")";
            String createComments = "CREATE TABLE IF NOT EXISTS post_comments (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "post_id BIGINT NOT NULL, " +
                    "user_id BIGINT NOT NULL, " +
                    "content TEXT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_comments_post (post_id), " +
                    "INDEX idx_comments_user (user_id)" +
                    ")";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(createPosts);
                st.execute(createLikes);
                st.execute(createComments);
                schemaReady = true;
            } catch (SQLException e) {
                schemaReady = false;
            }
        }
    }
}
