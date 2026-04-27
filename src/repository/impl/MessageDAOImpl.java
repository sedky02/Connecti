package repository.impl;

import database.DatabaseConnection;
import model.Message;
import repository.MessageDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageDAOImpl implements MessageDAO {
    @Override
    public boolean saveMessage(Message message) {
        String sql = "INSERT INTO messages (sender, type, room, receiver, content, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message.getSender());
            ps.setString(2, message.getType());
            ps.setString(3, message.getRoom());
            ps.setString(4, message.getReceiver());
            ps.setString(5, message.getContent());
            ps.setTimestamp(6, Timestamp.valueOf(message.getCreatedAt()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<Message> findByRoom(String room, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, sender, type, room, receiver, content, created_at " +
                "FROM messages WHERE room = ? AND (type = 'PUBLIC' OR type = 'ROOM') " +
                "ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room);
            ps.setInt(2, Math.max(limit, 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("type"),
                            rs.getString("room"),
                            rs.getString("receiver"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            return messages;
        }
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public List<Message> findPrivateConversation(String user1, String user2, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, sender, type, room, receiver, content, created_at " +
                "FROM messages WHERE type = 'PRIVATE' " +
                "AND ((sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)) " +
                "ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            ps.setInt(5, Math.max(limit, 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("type"),
                            rs.getString("room"),
                            rs.getString("receiver"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            return messages;
        }
        Collections.reverse(messages);
        return messages;
    }
}
