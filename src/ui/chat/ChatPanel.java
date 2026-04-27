package ui.chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ChatPanel extends JPanel {
    private final JTextPane chatPane = new JTextPane();
    private final JTextArea inputArea = new JTextArea(2, 20);
    private final JButton sendButton = new JButton("Send");
    private final JScrollPane chatScroll;
    private final JScrollPane inputScroll;

    private final String type;
    private final String target;
    private final String username;
    private final Consumer<String> sendAction;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    private boolean darkMode;

    private final SimpleAttributeSet ownStyle = new SimpleAttributeSet();
    private final SimpleAttributeSet otherStyle = new SimpleAttributeSet();
    private final SimpleAttributeSet systemStyle = new SimpleAttributeSet();
    private final SimpleAttributeSet timeStyle = new SimpleAttributeSet();

    public ChatPanel(String type, String target, String username, Consumer<String> sendAction) {
        this.type = type;
        this.target = target;
        this.username = username;
        this.sendAction = sendAction;
        this.chatScroll = new JScrollPane(chatPane);
        this.inputScroll = new JScrollPane(inputArea);
        initUI();
        applyStyles(false);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        chatPane.setEditable(false);
        chatPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        chatScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(chatScroll, BorderLayout.CENTER);

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        inputScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));
        inputScroll.setPreferredSize(new Dimension(200, 72));

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBorder(new EmptyBorder(2, 0, 0, 0));
        bottom.add(inputScroll, BorderLayout.CENTER);
        sendButton.setPreferredSize(new Dimension(90, 40));
        sendButton.setFocusable(false);
        JPanel sendWrap = new JPanel(new BorderLayout());
        sendWrap.add(sendButton, BorderLayout.NORTH);
        sendWrap.setOpaque(false);
        bottom.add(sendWrap, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "send-message");
        inputArea.getActionMap().put("send-message", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendMessage();
            }
        });
        inputArea.getInputMap().put(KeyStroke.getKeyStroke("shift ENTER"), "insert-break");
    }

    public void appendMessage(String sender, String content) {
        StyledDocument doc = chatPane.getStyledDocument();
        String time = "[" + LocalTime.now().format(formatter) + "] ";
        String cleanContent = content == null ? "" : content;
        SimpleAttributeSet bodyStyle = resolveStyle(sender);
        try {
            doc.insertString(doc.getLength(), time, timeStyle);
            doc.insertString(doc.getLength(), sender + ": ", bodyStyle);
            doc.insertString(doc.getLength(), cleanContent + "\n", bodyStyle);
        } catch (BadLocationException ignored) {
        }
        SwingUtilities.invokeLater(() -> chatPane.setCaretPosition(doc.getLength()));
    }

    private void sendMessage() {
        String content = sanitize(inputArea.getText());
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
        inputArea.setText("");
        inputArea.requestFocusInWindow();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("|", "/").replace("\r", "").replace("\n", " ");
    }

    private SimpleAttributeSet resolveStyle(String sender) {
        if ("SYSTEM".equalsIgnoreCase(sender) || "SERVER".equalsIgnoreCase(sender)) {
            return systemStyle;
        }
        if (username.equals(sender)) {
            return ownStyle;
        }
        return otherStyle;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        applyStyles(darkMode);
    }

    private void applyStyles(boolean dark) {
        Color background = dark ? new Color(28, 31, 36) : Color.WHITE;
        Color text = dark ? new Color(236, 239, 244) : new Color(42, 47, 55);
        Color subtle = dark ? new Color(140, 148, 158) : new Color(120, 129, 140);

        setBackground(background);
        chatPane.setBackground(background);
        chatPane.setForeground(text);
        chatPane.setCaretColor(text);
        inputArea.setBackground(dark ? new Color(35, 39, 45) : Color.WHITE);
        inputArea.setForeground(text);
        inputArea.setCaretColor(text);
        chatScroll.setBackground(background);
        inputScroll.setBackground(background);

        sendButton.setBackground(dark ? new Color(63, 114, 175) : new Color(56, 132, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        chatScroll.setBorder(BorderFactory.createLineBorder(dark ? new Color(62, 68, 76) : new Color(220, 224, 230)));
        inputScroll.setBorder(BorderFactory.createLineBorder(dark ? new Color(62, 68, 76) : new Color(220, 224, 230)));

        StyleConstants.setForeground(ownStyle, dark ? new Color(167, 218, 255) : new Color(10, 84, 170));
        StyleConstants.setBold(ownStyle, true);
        StyleConstants.setForeground(otherStyle, text);
        StyleConstants.setBold(otherStyle, false);
        StyleConstants.setForeground(systemStyle, dark ? new Color(244, 203, 117) : new Color(164, 104, 0));
        StyleConstants.setItalic(systemStyle, true);
        StyleConstants.setForeground(timeStyle, subtle);
        StyleConstants.setItalic(timeStyle, true);
    }
}
