package ui.chat;

import service.ChatService;
import service.UserService;
import ui.components.BaseFrame;
import ui.feed.FeedPanel;
import ui.users.UserListPanel;

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatFrame extends BaseFrame {
    private final UserListPanel userListPanel = new UserListPanel();
    private final JTabbedPane appTabbedPane = new JTabbedPane();
    private final JTabbedPane chatTabbedPane = new JTabbedPane();
    private final Map<String, ChatPanel> chats = new ConcurrentHashMap<>();
    private final Map<String, String> tabTitles = new ConcurrentHashMap<>();
    private final Map<String, Integer> unreadCounts = new ConcurrentHashMap<>();
    private final JTextField roomField = new JTextField("general");
    private final JButton joinRoomButton = new JButton("Join Room");
    private final JLabel connectionLabel = new JLabel();
    private final String host;
    private final int port;

    private final ChatService chatService;
    private final UserService userService = new UserService();
    private final String username;
    private final FeedPanel feedPanel;
    private List<String> allUsers = new ArrayList<>();

    public ChatFrame(String username, String host, int port) {
        super("Messaging App - " + username, 1000, 650);
        this.username = username;
        this.host = host;
        this.port = port;
        this.chatService = new ChatService(username, host, port);
        this.feedPanel = new FeedPanel(username);
        this.allUsers = userService.getAllUsernames();
        initUI();
        wireService();
        connect();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));

        userListPanel.setCurrentUsername(username);
        userListPanel.setUserClickListener(this::openPrivateTab);
        userListPanel.setPreferredSize(new Dimension(240, 0));

        JPanel topPanel = buildTopToolbar();
        joinRoomButton.addActionListener(e -> joinRoom(roomField.getText()));

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        chatPanel.setOpaque(false);

        JPanel chatBodyPanel = new JPanel(new BorderLayout());
        chatBodyPanel.setOpaque(false);
        chatBodyPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setBorder(new EmptyBorder(0, 0, 0, 6));
        leftWrap.add(userListPanel, BorderLayout.CENTER);
        leftWrap.setOpaque(false);

        JPanel chatCenter = new JPanel(new BorderLayout());
        chatCenter.setOpaque(false);
        chatCenter.add(chatTabbedPane, BorderLayout.CENTER);

        chatBodyPanel.add(leftWrap, BorderLayout.WEST);
        chatBodyPanel.add(chatCenter, BorderLayout.CENTER);
        chatPanel.add(topPanel, BorderLayout.NORTH);
        chatPanel.add(chatBodyPanel, BorderLayout.CENTER);

        appTabbedPane.addTab("Chat", chatPanel);
        appTabbedPane.addTab("Feed", feedPanel);
        add(appTabbedPane, BorderLayout.CENTER);

        chatTabbedPane.addChangeListener(e -> clearUnreadForSelectedTab());
        appTabbedPane.addChangeListener(e -> {
            boolean chatSelected = appTabbedPane.getSelectedIndex() == 0;
            roomField.setEnabled(chatSelected);
            joinRoomButton.setEnabled(chatSelected);
        });
        getOrCreatePublicTab();
        applyLightTheme();
        applyFont(this);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                feedPanel.stopAutoRefresh();
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
        connectionLabel.setText("Connected: " + username + " @ " + host + ":" + port);
    }

    private ChatPanel getOrCreatePublicTab() {
        return chats.computeIfAbsent("PUBLIC", key -> addTab("PUBLIC", "Public", ""));
    }

    private void openPrivateTab(String otherUser) {
        if (otherUser == null || otherUser.isBlank() || otherUser.equals(username)) {
            return;
        }
        String key = "PRIVATE_" + otherUser;
        boolean existed = chats.containsKey(key);
        chats.computeIfAbsent(key, k -> addTab("PRIVATE", "PM: " + otherUser, otherUser));
        selectTab(key);
        if (!existed) {
            chatService.sendProtocolLine("PRIVATE|" + username + "|" + otherUser + "|/history");
        }
    }

    private void joinRoom(String roomName) {
        final String room = sanitize(roomName).isBlank() ? "general" : sanitize(roomName);

        String key = "ROOM_" + room;
        chats.computeIfAbsent(key, k -> addTab("ROOM", "Room: " + room, room));
        selectTab(key);
        chatService.sendProtocolLine("JOIN|" + username + "|" + room + "|");
    }

    private ChatPanel addTab(String type, String title, String target) {
        ChatPanel panel = new ChatPanel(type, target, username, chatService::sendProtocolLine);
        chatTabbedPane.addTab(title, panel);
        String key = "PUBLIC".equals(type) ? "PUBLIC" : ("PRIVATE".equals(type) ? "PRIVATE_" + target : "ROOM_" + target);
        tabTitles.put(key, title);
        unreadCounts.put(key, 0);
        return panel;
    }

    private void selectTab(String key) {
        ChatPanel panel = chats.get(key);
        if (panel != null) {
            appTabbedPane.setSelectedIndex(0);
            chatTabbedPane.setSelectedComponent(panel);
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
                if (chatTabbedPane.getSelectedComponent() != panel
                        && !username.equals(event.getSender())
                        && !"SYSTEM".equalsIgnoreCase(event.getSender())
                        && !"SERVER".equalsIgnoreCase(event.getSender())) {
                    incrementUnread(key);
                }
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

    private JPanel buildTopToolbar() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel roomLabel = new JLabel("Room");
        roomField.setPreferredSize(new Dimension(180, 32));
        joinRoomButton.setPreferredSize(new Dimension(100, 32));
        left.add(roomLabel);
        left.add(roomField);
        left.add(joinRoomButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        connectionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(connectionLabel);

        left.setOpaque(false);
        right.setOpaque(false);
        topPanel.add(left, BorderLayout.WEST);
        topPanel.add(right, BorderLayout.EAST);
        return topPanel;
    }

    private void incrementUnread(String key) {
        int next = unreadCounts.getOrDefault(key, 0) + 1;
        unreadCounts.put(key, next);
        refreshTabTitle(key);
    }

    private void clearUnreadForSelectedTab() {
        Component selected = chatTabbedPane.getSelectedComponent();
        if (selected == null) {
            return;
        }
        for (Map.Entry<String, ChatPanel> entry : chats.entrySet()) {
            if (entry.getValue() == selected) {
                unreadCounts.put(entry.getKey(), 0);
                refreshTabTitle(entry.getKey());
                break;
            }
        }
    }

    private void refreshTabTitle(String key) {
        ChatPanel panel = chats.get(key);
        if (panel == null) {
            return;
        }
        int index = chatTabbedPane.indexOfComponent(panel);
        if (index < 0) {
            return;
        }
        String base = tabTitles.getOrDefault(key, chatTabbedPane.getTitleAt(index));
        int unread = unreadCounts.getOrDefault(key, 0);
        chatTabbedPane.setTitleAt(index, unread > 0 ? base + " (" + unread + ")" : base);
    }

    private void applyLightTheme() {
        Color frameBg = new Color(245, 247, 250);
        Color panelBg = Color.WHITE;
        Color text = new Color(33, 39, 47);
        Color border = new Color(220, 224, 230);

        getContentPane().setBackground(frameBg);
        appTabbedPane.setBackground(panelBg);
        appTabbedPane.setForeground(text);
        chatTabbedPane.setBackground(panelBg);
        chatTabbedPane.setForeground(text);
        chatTabbedPane.setBorder(BorderFactory.createLineBorder(border));
        roomField.setBackground(Color.WHITE);
        roomField.setForeground(text);
        roomField.setCaretColor(text);
        roomField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(6, 8, 6, 8)
        ));

        joinRoomButton.setBackground(new Color(56, 132, 255));
        joinRoomButton.setForeground(Color.WHITE);
        joinRoomButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        connectionLabel.setForeground(text);
        repaint();
    }
}
