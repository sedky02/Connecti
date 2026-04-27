package ui.chat;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ChatPanel extends JPanel {
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private final String type;
    private final String target;
    private final String username;
    private final Consumer<String> sendAction;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    public ChatPanel(String type, String target, String username, Consumer<String> sendAction) {
        this.type = type;
        this.target = target;
        this.username = username;
        this.sendAction = sendAction;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(6, 6));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    public void appendMessage(String sender, String content) {
        String line = String.format("[%s] %s: %s",
                LocalTime.now().format(formatter),
                sender,
                content);
        chatArea.append(line + "\n");
    }

    private void sendMessage() {
        String content = sanitize(inputField.getText());
        if (content.isBlank()) {
            return;
        }
        String line;
        switch (type) {
            case "PRIVATE":
                line = "PRIVATE|" + username + "|" + target + "|" + content;
                break;
            case "ROOM":
                line = "ROOM|" + username + "|" + target + "|" + content;
                break;
            default:
                line = "PUBLIC|" + username + "||" + content;
                break;
        }
        sendAction.accept(line);
        inputField.setText("");
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("|", "/");
    }
}
