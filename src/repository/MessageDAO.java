package repository;

import model.Message;

import java.util.List;

public interface MessageDAO {
    boolean saveMessage(Message message);
    List<Message> findByRoom(String room, int limit);
    List<Message> findPrivateConversation(String user1, String user2, int limit);
}
