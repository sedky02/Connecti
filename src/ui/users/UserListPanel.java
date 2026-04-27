package ui.users;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class UserListPanel extends JPanel {
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(listModel);
    private final Map<String, String> displayToUsername = new HashMap<>();
    private Consumer<String> userClickListener = username -> {};
    private String currentUsername;

    public UserListPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220, 0));
        add(new JLabel("Online Users", SwingConstants.CENTER), BorderLayout.NORTH);
        add(new JScrollPane(userList), BorderLayout.CENTER);
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() < 1) {
                    return;
                }
                String selected = userList.getSelectedValue();
                if (selected == null) {
                    return;
                }
                String username = displayToUsername.get(selected);
                if (username != null && !username.equals(currentUsername)) {
                    userClickListener.accept(username);
                }
            }
        });
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public void setUserClickListener(Consumer<String> userClickListener) {
        this.userClickListener = userClickListener;
    }

    public void setUsers(List<String> allUsers, List<String> onlineUsers) {
        listModel.clear();
        displayToUsername.clear();

        List<String> sortedUsers = new ArrayList<>(allUsers);
        Collections.sort(sortedUsers);
        for (String user : sortedUsers) {
            if (user.equals(currentUsername)) {
                continue;
            }
            boolean online = onlineUsers.contains(user);
            String display = user + (online ? " (online)" : " (offline)");
            listModel.addElement(display);
            displayToUsername.put(display, user);
        }
    }
}
