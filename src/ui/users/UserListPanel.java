package ui.users;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class UserListPanel extends JPanel {
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(listModel);
    private final Map<String, String> displayToUsername = new HashMap<>();
    private Consumer<String> userClickListener = username -> {};
    private String currentUsername;
    private final Set<String> onlineUsers = new HashSet<>();
    private int hoverIndex = -1;

    public UserListPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(230, 0));
        setBorder(new EmptyBorder(10, 10, 10, 6));

        JLabel title = new JLabel("Users");
        title.setBorder(new EmptyBorder(0, 4, 8, 4));
        add(title, BorderLayout.NORTH);

        userList.setFixedCellHeight(32);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new UserRenderer());
        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        add(scrollPane, BorderLayout.CENTER);

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

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                userList.repaint();
            }
        });
        userList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = userList.locationToIndex(e.getPoint());
                if (index != hoverIndex) {
                    hoverIndex = index;
                    userList.repaint();
                }
            }
        });
        applyTheme();
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
        this.onlineUsers.clear();
        this.onlineUsers.addAll(onlineUsers);

        List<String> sortedUsers = new ArrayList<>(allUsers);
        Collections.sort(sortedUsers);
        for (String user : sortedUsers) {
            if (user.equals(currentUsername)) {
                continue;
            }
            String display = user;
            listModel.addElement(display);
            displayToUsername.put(display, user);
        }
    }

    private void applyTheme() {
        Color background = new Color(248, 250, 253);
        Color listBackground = Color.WHITE;
        Color text = new Color(35, 41, 49);
        setBackground(background);
        for (Component component : getComponents()) {
            component.setBackground(background);
            component.setForeground(text);
        }
        userList.setBackground(listBackground);
        userList.setForeground(text);
        userList.setSelectionBackground(new Color(221, 234, 252));
        userList.setSelectionForeground(text);
    }

    private class UserRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel dotLabel = new JLabel("\u25CF");
        private final JLabel nameLabel = new JLabel();

        private UserRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(6, 10, 6, 10));
            add(dotLabel, BorderLayout.WEST);
            add(nameLabel, BorderLayout.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
                                                      String value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            String username = displayToUsername.getOrDefault(value, value);
            boolean online = onlineUsers.contains(username);
            nameLabel.setText(username);

            Color bg;
            if (isSelected) {
                bg = new Color(221, 234, 252);
            } else if (hoverIndex == index) {
                bg = new Color(242, 246, 251);
            } else {
                bg = Color.WHITE;
            }
            Color text = new Color(35, 41, 49);

            setBackground(bg);
            dotLabel.setForeground(online ? new Color(71, 180, 90) : new Color(160, 166, 176));
            nameLabel.setForeground(text);
            return this;
        }
    }
}
