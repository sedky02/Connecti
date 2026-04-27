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
        String sql = "INSERT INTO messages (sender, content, `timestamp`) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message.getSender());
            ps.setString(2, message.getContent());
            ps.setTimestamp(3, Timestamp.valueOf(message.getTimestamp()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<Message> findRecentMessages(int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, sender, content, `timestamp` FROM messages ORDER BY `timestamp` DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(limit, 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("content"),
                            rs.getTimestamp("timestamp").toLocalDateTime()
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