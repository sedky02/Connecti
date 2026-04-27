package network.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();

    public boolean addClient(String username, ClientHandler handler, String room) {
        if (username == null || username.isBlank() || clients.containsKey(username)) {
            return false;
        }
        clients.put(username, handler);
        rooms.computeIfAbsent(room, key -> ConcurrentHashMap.newKeySet()).add(username);
        return true;
    }

    public void removeClient(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        clients.remove(username);
        for (Set<String> members : rooms.values()) {
            members.remove(username);
        }
    }

    public void moveUserToRoom(String username, String oldRoom, String newRoom) {
        if (username == null || username.isBlank() || newRoom == null || newRoom.isBlank()) {
            return;
        }
        if (oldRoom != null && !oldRoom.isBlank()) {
            rooms.computeIfAbsent(oldRoom, key -> ConcurrentHashMap.newKeySet()).remove(username);
        }
        rooms.computeIfAbsent(newRoom, key -> ConcurrentHashMap.newKeySet()).add(username);
    }

    public void sendToUser(String username, String message) {
        ClientHandler handler = clients.get(username);
        if (handler != null) {
            handler.send(message);
        }
    }

    public void broadcastToRoom(String room, String message) {
        Set<String> members = rooms.getOrDefault(room, ConcurrentHashMap.newKeySet());
        for (String username : members) {
            ClientHandler handler = clients.get(username);
            if (handler != null) {
                handler.send(message);
            }
        }
    }

    public void broadcastUsers() {
        List<String> users = new ArrayList<>(clients.keySet());
        users.sort(String::compareToIgnoreCase);
        String line = "USERS|SERVER||" + String.join(",", users);
        for (ClientHandler client : clients.values()) {
            client.send(line);
        }
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(clients.keySet());
    }
}
