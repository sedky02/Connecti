package service;

import model.Message;
import network.client.ChatClient;
import network.client.ReadThread;
import repository.MessageDAO;
import repository.impl.MessageDAOImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ChatService {
    private final String username;
    private final ChatClient chatClient;
    private final MessageDAO messageDAO;

    private Consumer<ChatEvent> messageListener = m -> {};
    private Consumer<List<String>> userListListener = u -> {};
    private Consumer<String> errorListener = e -> {};

    public ChatService(String username, String host, int port) {
        this.username = username;
        this.messageDAO = new MessageDAOImpl();
        this.chatClient = new ChatClient(host, port, this::handleIncomingPacket, this::handleClientError);
    }

    public ChatService() {
        this.username = null;
        this.messageDAO = new MessageDAOImpl();
        this.chatClient = null;
    }

    public ChatService(MessageDAO messageDAO) {
        this.username = null;
        this.messageDAO = messageDAO;
        this.chatClient = null;
    }

    public void setMessageListener(Consumer<ChatEvent> messageListener) {
        this.messageListener = messageListener;
    }

    public void setUserListListener(Consumer<List<String>> userListListener) {
        this.userListListener = userListListener;
    }

    public void setErrorListener(Consumer<String> errorListener) {
        this.errorListener = errorListener;
    }

    public void connect() {
        if (chatClient != null) {
            chatClient.start(username);
            sendProtocolLine("JOIN|" + username + "|general|");
        }
    }

    public boolean sendProtocolLine(String line) {
        if (chatClient == null) {
            return false;
        }
        if (line == null || line.isBlank()) {
            return false;
        }
        chatClient.sendRaw(line);
        return true;
    }

    public void disconnect() {
        if (chatClient != null) {
            chatClient.close();
        }
    }

    public boolean saveIncomingMessage(Message message) {
        if (messageDAO == null) {
            return false;
        }
        return messageDAO.saveMessage(message);
    }

    public List<Message> getRecentMessages(int limit) {
        if (messageDAO == null) {
            return Collections.emptyList();
        }
        return messageDAO.findRecentMessages(limit);
    }

    private void handleIncomingPacket(ReadThread.Packet packet) {
        if (packet == null) {
            return;
        }
        switch (packet.getType()) {
            case "PUBLIC":
            case "PRIVATE":
            case "ROOM":
                messageListener.accept(new ChatEvent(
                        packet.getType(),
                        packet.getSender(),
                        packet.getTarget(),
                        packet.getContent()
                ));
                break;
            case "USERS":
                List<String> users = packet.getContent().isBlank()
                        ? Collections.emptyList()
                        : List.of(packet.getContent().split(",", -1));
                userListListener.accept(users);
                break;
            case "JOIN":
                if ("SERVER".equalsIgnoreCase(packet.getSender())) {
                    messageListener.accept(new ChatEvent("ROOM", "SYSTEM", packet.getTarget(), packet.getContent()));
                }
                break;
            default:
                break;
        }
    }

    private void handleClientError(String error) {
        errorListener.accept(error);
    }

    public void persistMessage(String sender, String type, String target, String content) {
        if (messageDAO == null) {
            return;
        }
        String dbContent = ("PUBLIC".equals(type) || target == null || target.isBlank())
                ? content
                : "[" + type + ":" + target + "] " + content;
        messageDAO.saveMessage(new Message(sender, dbContent, LocalDateTime.now()));
    }

    public static class ChatEvent {
        private final String type;
        private final String sender;
        private final String target;
        private final String content;

        public ChatEvent(String type, String sender, String target, String content) {
            this.type = type;
            this.sender = sender;
            this.target = target;
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public String getSender() {
            return sender;
        }

        public String getTarget() {
            return target;
        }

        public String getContent() {
            return content;
        }
    }
}
