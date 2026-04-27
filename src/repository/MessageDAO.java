package repository;

import model.Message;

import java.util.List;

public interface MessageDAO {
    boolean saveMessage(Message message);
    List<Message> findRecentMessages(int limit);
}