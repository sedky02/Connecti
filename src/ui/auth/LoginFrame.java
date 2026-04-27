package ui.auth;

import service.AuthService;
import ui.chat.ChatFrame;
import ui.components.BaseFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends BaseFrame {
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JTextField hostField = new JTextField("127.0.0.1", 16);
    private final JTextField portField = new JTextField("5000", 16);
    private final JLabel statusLabel = new JLabel(" ");

    private final AuthService authService = new AuthService();

    public LoginFrame() {
        super("Messaging App - Login", 440, 320);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        panel.add(usernameField, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        panel.add(passwordField, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Server Host:"), c);
        c.gridx = 1;
        panel.add(hostField, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JLabel("Server Port:"), c);
        c.gridx = 1;
        panel.add(portField, c);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        loginButton.setBackground(new Color(56, 132, 255));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        registerButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.add(loginButton);
        buttons.add(registerButton);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        panel.add(buttons, c);

        c.gridy = 5;
        statusLabel.setForeground(Color.DARK_GRAY);
        panel.add(statusLabel, c);

        panel.setBackground(new Color(248, 250, 253));
        getContentPane().setBackground(new Color(245, 247, 250));
        add(panel);
        applyFont(this);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String host = hostField.getText().trim();
        int port = parsePort(portField.getText().trim());

        if (port <= 0) {
            setStatus("Invalid port");
            return;
        }

        boolean success = authService.loginUser(username, password);
        if (!success) {
            setStatus("Invalid username or password");
            return;
        }

        dispose();
        ChatFrame chatFrame = new ChatFrame(username, host, port);
        chatFrame.setVisible(true);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        boolean success = authService.registerUser(username, password);
        setStatus(success ? "Registration successful" : "Registration failed");
    }

    private int parsePort(String portValue) {
        try {
            int port = Integer.parseInt(portValue);
            return port > 0 && port <= 65535 ? port : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}
