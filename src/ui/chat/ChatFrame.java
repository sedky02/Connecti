package ui.chat;

import service.ChatService;
import service.UserService;
import ui.components.BaseFrame;
import ui.users.UserListPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatFrame extends BaseFrame {
    private final UserListPanel userListPanel = new UserListPanel();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final Map<String, ChatPanel> chats = new ConcurrentHashMap<>();
    private final JTextField roomField = new JTextField("general");
    private final JButton joinRoomButton = new JButton("Join Room");

    private final ChatService chatService;
    private final UserService userService = new UserService();
    private final String username;
    private List<String> allUsers = new ArrayList<>();

    public ChatFrame(String username, String host, int port) {
        super("Messaging App - " + username, 1000, 650);
        this.username = username;
        this.chatService = new ChatService(username, host, port);
        this.allUsers = userService.getAllUsernames();
        initUI();
        wireService();
        connect();
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        userListPanel.setCurrentUsername(username);
        userListPanel.setUserClickListener(this::openPrivateTab);

        JPanel topPanel = new JPanel(new BorderLayout(6, 0));
        topPanel.add(new JLabel("Room:"), BorderLayout.WEST);
        topPanel.add(roomField, BorderLayout.CENTER);
        topPanel.add(joinRoomButton, BorderLayout.EAST);
        joinRoomButton.addActionListener(e -> joinRoom(roomField.getText()));

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(userListPanel, BorderLayout.EAST);

        getOrCreatePublicTab();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                chatService.disconnect();
            }
        });
    }

    private void wireService() {
        chatService.setMessageListener(this::routeEvent);
        chatService.setUserListListener(this::updateUsers);
        chatService.setErrorListener(this::appendSystem);
    }

    private void connect() {
        chatService.connect();
    }

    private ChatPanel getOrCreatePublicTab() {
        return chats.computeIfAbsent("PUBLIC", key -> addTab("PUBLIC", "Public", ""));
    }

    private void openPrivateTab(String otherUser) {
        if (otherUser == null || otherUser.isBlank() || otherUser.equals(username)) {
            return;
        }
        String key = "PRIVATE_" + otherUser;
        chats.computeIfAbsent(key, k -> addTab("PRIVATE", "PM: " + otherUser, otherUser));
        selectTab(key);
    }

    private void joinRoom(String roomName) {
        final String room = (roomName == null || roomName.isBlank()) ? "general" : roomName;

        String key = "ROOM_" + room;
        chats.computeIfAbsent(key, k -> addTab("ROOM", "Room: " + room, room));
        selectTab(key);
        chatService.sendProtocolLine("JOIN|" + username + "|" + room + "|");
    }

    private ChatPanel addTab(String type, String title, String target) {
        ChatPanel panel = new ChatPanel(type, target, username, chatService::sendProtocolLine);
        tabbedPane.addTab(title, panel);
        return panel;
    }

    private void selectTab(String key) {
        ChatPanel panel = chats.get(key);
        if (panel != null) {
            tabbedPane.setSelectedComponent(panel);
        }
    }

    private void routeEvent(ChatService.ChatEvent event) {
        SwingUtilities.invokeLater(() -> {
            String key;
            switch (event.getType()) {
                case "PRIVATE":
                    key = "PRIVATE_" + (username.equals(event.getSender()) ? event.getTarget() : event.getSender());
                    chats.computeIfAbsent(key, k -> addTab("PRIVATE", "PM: " + extractPrivatePeer(event), extractPrivatePeer(event)));
                    break;
                case "ROOM":
                    String room = event.getTarget().isBlank() ? "general" : event.getTarget();
                    key = "ROOM_" + room;
                    chats.computeIfAbsent(key, k -> addTab("ROOM", "Room: " + room, room));
                    break;
                case "PUBLIC":
                default:
                    key = "PUBLIC";
                    getOrCreatePublicTab();
                    break;
            }
            ChatPanel panel = chats.get(key);
            if (panel != null) {
                panel.appendMessage(event.getSender(), event.getContent());
            }
        });
    }

    private String extractPrivatePeer(ChatService.ChatEvent event) {
        return username.equals(event.getSender()) ? event.getTarget() : event.getSender();
    }

    private void updateUsers(List<String> onlineUsers) {
        SwingUtilities.invokeLater(() -> {
            allUsers = mergeKnownUsers(allUsers, onlineUsers);
            userListPanel.setUsers(allUsers, onlineUsers);
        });
    }

    private List<String> mergeKnownUsers(List<String> baseUsers, List<String> onlineUsers) {
        List<String> merged = new ArrayList<>(baseUsers);
        for (String online : onlineUsers) {
            if (!merged.contains(online)) {
                merged.add(online);
            }
        }
        return merged;
    }

    private void appendSystem(String text) {
        SwingUtilities.invokeLater(() -> getOrCreatePublicTab().appendMessage("SYSTEM", text));
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("|", "/");
    }
}
