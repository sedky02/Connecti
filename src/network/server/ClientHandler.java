package network.server;

import model.Message;
import service.ChatService;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ClientManager clientManager;
    private final ChatService chatService;

    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private String currentRoom = "general";

    public ClientHandler(Socket socket, ClientManager clientManager, ChatService chatService) {
        this.socket = socket;
        this.clientManager = clientManager;
        this.chatService = chatService;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            if (!registerClient()) {
                close();
                return;
            }

            clientManager.broadcastUsers();
            listenForMessages();
        } catch (IOException ignored) {
        } finally {
            if (username != null) {
                clientManager.removeClient(username);
                clientManager.broadcastUsers();
            }
            close();
        }
    }

    private boolean registerClient() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return false;
        }
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4 || !"JOIN".equals(parts[0])) {
            return false;
        }

        String candidate = parts[1].trim();
        String room = parts[2] == null || parts[2].isBlank() ? "general" : parts[2].trim();
        if (candidate.isBlank()) {
            return false;
        }

        boolean added = clientManager.addClient(candidate, this, room);
        if (!added) {
            return false;
        }

        username = candidate;
        currentRoom = room;
        return true;
    }

    private void listenForMessages() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\|", 4);
            if (parts.length < 4) {
                continue;
            }

            String type = parts[0];
            String sender = parts[1];
            String target = parts[2];
            String content = parts[3];

            if (!username.equals(sender)) {
                continue;
            }

            switch (type) {
                case "PUBLIC":
                    handlePublic(content);
                    break;
                case "PRIVATE":
                    handlePrivate(target, content);
                    break;
                case "ROOM":
                    handleRoom(target, content);
                    break;
                case "JOIN":
                    handleJoin(target);
                    break;
                default:
                    break;
            }
        }
    }

    private void sendRecentMessages(int limit, String room) {
        List<Message> messages = chatService.getRecentMessages(limit);
        for (Message message : messages) {
            String line = "ROOM|" + message.getSender() + "|" + room + "|" + message.getContent();
            send(line);
        }
    }

    private void handlePublic(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        chatService.persistMessage(username, "PUBLIC", currentRoom, content);
        clientManager.broadcastToRoom(currentRoom, "PUBLIC|" + username + "|" + currentRoom + "|" + content);
    }

    private void handlePrivate(String target, String content) {
        if (target == null || target.isBlank() || content == null || content.isBlank()) {
            return;
        }
        chatService.persistMessage(username, "PRIVATE", target, content);
        String message = "PRIVATE|" + username + "|" + target + "|" + content;
        clientManager.sendToUser(target, message);
        clientManager.sendToUser(username, message);
    }

    private void handleRoom(String room, String content) {
        String effectiveRoom = (room == null || room.isBlank()) ? currentRoom : room;
        if (content == null || content.isBlank()) {
            return;
        }
        chatService.persistMessage(username, "ROOM", effectiveRoom, content);
        clientManager.broadcastToRoom(effectiveRoom, "ROOM|" + username + "|" + effectiveRoom + "|" + content);
    }

    private void handleJoin(String room) {
        String nextRoom = (room == null || room.isBlank()) ? "general" : room.trim();
        if (nextRoom.equals(currentRoom)) {
            return;
        }
        clientManager.moveUserToRoom(username, currentRoom, nextRoom);
        currentRoom = nextRoom;
        sendRecentMessages(20, currentRoom);
        clientManager.broadcastUsers();
    }

    public synchronized void send(String line) {
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
