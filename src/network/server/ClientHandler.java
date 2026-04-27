package network.server;

import model.Message;
import service.ChatService;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

            if (!register()) {
                close();
                return;
            }

            clientManager.broadcastUsers();

            listen();

        } catch (IOException ignored) {
        } finally {
            if (username != null) {
                clientManager.removeClient(username);
                clientManager.broadcastUsers();
            }
            close();
        }
    }

    private boolean register() throws IOException {
        String line = reader.readLine();
        if (line == null) return false;

        String[] p = line.split("\\|", 4);
        if (p.length < 3 || !"JOIN".equals(p[0])) return false;

        username = p[1].trim();
        currentRoom = (p[2] == null || p[2].isBlank()) ? "general" : p[2].trim();

        boolean added = clientManager.addClient(username, this, currentRoom);
        if (added) {
            sendRoomHistory(currentRoom, 30);
        }
        return added;
    }

    private void listen() throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            String[] p = line.split("\\|", 4);
            if (p.length < 4) continue;

            String type = p[0];
            String sender = p[1];
            String target = p[2];
            String content = p[3];

            if (!sender.equals(username)) continue;

            switch (type) {

                case "PUBLIC":
                    chatService.persistMessage(username, "PUBLIC", currentRoom, content);
                    clientManager.broadcastToRoom(currentRoom,
                            "PUBLIC|" + username + "|" + currentRoom + "|" + content);
                    break;

                case "PRIVATE":
                    handlePrivate(target, content);
                    break;

                case "ROOM":
                    String room = target.isBlank() ? currentRoom : target.trim();
                    if (!room.equals(currentRoom)) {
                        send("ROOM|SERVER|" + currentRoom + "|Join room before sending messages there");
                        break;
                    }
                    chatService.persistMessage(username, "ROOM", room, content);
                    clientManager.broadcastToRoom(room, "ROOM|" + username + "|" + room + "|" + content);
                    break;

                case "JOIN":
                    handleJoin(target);
                    break;
            }
        }
    }

    private void handleJoin(String room) {
        String newRoom = (room == null || room.isBlank()) ? "general" : room.trim();

        if (newRoom.equals(currentRoom)) {
            sendRoomHistory(currentRoom, 30);
            return;
        }

        clientManager.moveUserToRoom(username, currentRoom, newRoom);
        currentRoom = newRoom;

        sendRoomHistory(currentRoom, 30);
        send("ROOM|SERVER|" + currentRoom + "|Joined room: " + currentRoom);
        clientManager.broadcastUsers();
    }

    private void handlePrivate(String target, String content) {
        if (target == null || target.isBlank()) {
            return;
        }
        if ("/history".equals(content)) {
            sendPrivateHistory(target, 30);
            return;
        }
        chatService.persistMessage(username, "PRIVATE", target, content);
        String msg = "PRIVATE|" + username + "|" + target + "|" + content;
        clientManager.sendToUser(target, msg);
        clientManager.sendToUser(username, msg);
    }

    private void sendRoomHistory(String room, int limit) {
        List<Message> history = chatService.getRoomHistory(room, limit);
        for (Message message : history) {
            send(message.getType() + "|" + message.getSender() + "|" + room + "|" + message.getContent());
        }
    }

    private void sendPrivateHistory(String peer, int limit) {
        List<Message> history = chatService.getPrivateConversation(username, peer, limit);
        for (Message message : history) {
            send("PRIVATE|" + message.getSender() + "|" + message.getReceiver() + "|" + message.getContent());
        }
    }

    public synchronized void send(String msg) {
        try {
            writer.write(msg);
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {}
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
