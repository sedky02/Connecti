package ui.feed;

import model.Comment;
import model.Post;
import service.PostService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeedPanel extends JPanel {
    private static final Color BG = new Color(245, 247, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(220, 224, 230);
    private static final Color PRIMARY = new Color(56, 132, 255);
    private static final Color TEXT = new Color(35, 41, 49);
    private static final Color SUBTLE = new Color(120, 129, 140);

    private final String currentUsername;
    private final PostService postService;

    private final JTextArea postInput = new JTextArea(3, 20);
    private final JButton postButton = new JButton("Post");
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel feedListPanel = new JPanel();
    private final JScrollPane feedScroll;
    private final Timer refreshTimer;
    private final Set<Integer> expandedPostIds = new HashSet<>();
    private final Map<Integer, String> commentDrafts = new HashMap<>();

    private final DateTimeFormatter postTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter commentTimeFmt = DateTimeFormatter.ofPattern("HH:mm");

    public FeedPanel(String currentUsername) {
        this.currentUsername = currentUsername;
        this.postService = new PostService();
        this.feedScroll = new JScrollPane(feedListPanel);
        this.refreshTimer = new Timer(5000, e -> {
            if (shouldAutoRefresh()) {
                refreshFeed(false);
            }
        });
        initUI();
        refreshFeed(true);
        refreshTimer.start();
    }

    private void initUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(BG);

        JPanel createPostPanel = new JPanel(new BorderLayout(8, 8));
        createPostPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 12, 12, 12)
        ));
        createPostPanel.setBackground(CARD_BG);

        postInput.setLineWrap(true);
        postInput.setWrapStyleWord(true);
        postInput.setFont(new Font("SansSerif", Font.PLAIN, 14));
        postInput.setForeground(TEXT);
        postInput.setBackground(CARD_BG);
        postInput.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane inputScroll = new JScrollPane(postInput);
        inputScroll.setPreferredSize(new Dimension(200, 96));
        inputScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        inputScroll.getVerticalScrollBar().setUnitIncrement(14);

        postButton.setFocusable(false);
        postButton.setBackground(PRIMARY);
        postButton.setForeground(Color.WHITE);
        postButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        postButton.addActionListener(e -> createPost());

        JLabel title = new JLabel("Global Feed");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Share updates with everyone");
        subtitle.setForeground(SUBTLE);
        subtitle.setBorder(new EmptyBorder(2, 0, 0, 0));
        JPanel titleWrap = new JPanel(new BorderLayout());
        titleWrap.setOpaque(false);
        titleWrap.add(title, BorderLayout.NORTH);
        titleWrap.add(subtitle, BorderLayout.SOUTH);

        createPostPanel.add(titleWrap, BorderLayout.NORTH);
        createPostPanel.add(inputScroll, BorderLayout.CENTER);

        JPanel rightAction = new JPanel(new BorderLayout());
        rightAction.setOpaque(false);
        rightAction.setBorder(new EmptyBorder(0, 8, 0, 0));
        rightAction.add(postButton, BorderLayout.NORTH);
        createPostPanel.add(rightAction, BorderLayout.EAST);
        statusLabel.setForeground(new Color(130, 54, 54));
        statusLabel.setBorder(new EmptyBorder(2, 2, 0, 2));
        createPostPanel.add(statusLabel, BorderLayout.SOUTH);

        feedListPanel.setLayout(new BoxLayout(feedListPanel, BoxLayout.Y_AXIS));
        feedListPanel.setBackground(BG);
        feedScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        feedScroll.getVerticalScrollBar().setUnitIncrement(16);
        feedScroll.getViewport().setBackground(BG);

        add(createPostPanel, BorderLayout.NORTH);
        add(feedScroll, BorderLayout.CENTER);
    }

    private void createPost() {
        String content = sanitize(postInput.getText());
        if (content.isBlank()) {
            statusLabel.setText("Post cannot be empty.");
            return;
        }
        if (postService.createPost(currentUsername, content)) {
            postInput.setText("");
            statusLabel.setText(" ");
            refreshFeed(true);
        } else {
            statusLabel.setText("Unable to post. Check database schema/connection.");
        }
    }

    private void refreshFeed(boolean scrollToTop) {
        List<Post> feed = postService.getGlobalFeed(currentUsername);
        feedListPanel.removeAll();
        for (Post post : feed) {
            feedListPanel.add(new PostCard(post));
            feedListPanel.add(Box.createVerticalStrut(8));
        }
        feedListPanel.revalidate();
        feedListPanel.repaint();
        if (scrollToTop) {
            SwingUtilities.invokeLater(() -> feedScroll.getVerticalScrollBar().setValue(0));
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\r", "");
    }

    public void stopAutoRefresh() {
        refreshTimer.stop();
    }

    private boolean shouldAutoRefresh() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
            return true;
        }
        return !(focusOwner instanceof JTextComponent && SwingUtilities.isDescendingFrom(focusOwner, this));
    }

    private class PostCard extends JPanel {
        private final Post post;
        private final JPanel commentsPanel = new JPanel();
        private boolean commentsVisible;

        private PostCard(Post post) {
            this.post = post;
            initCard();
        }

        private void initCard() {
            setLayout(new BorderLayout(8, 8));
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(12, 12, 12, 12)
            ));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel header = new JPanel(new BorderLayout(8, 0));
            header.setOpaque(false);
            JLabel avatar = new JLabel(initial(post.getUsername()), SwingConstants.CENTER);
            avatar.setOpaque(true);
            avatar.setBackground(new Color(229, 238, 255));
            avatar.setForeground(new Color(25, 96, 196));
            avatar.setPreferredSize(new Dimension(30, 30));
            avatar.setFont(new Font("SansSerif", Font.BOLD, 13));
            avatar.setBorder(BorderFactory.createLineBorder(new Color(200, 216, 242)));

            JLabel userLabel = new JLabel(post.getUsername());
            userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD));
            JLabel timeLabel = new JLabel(post.getCreatedAt().format(postTimeFmt));
            timeLabel.setForeground(SUBTLE);

            JPanel userBox = new JPanel(new BorderLayout(8, 0));
            userBox.setOpaque(false);
            userBox.add(avatar, BorderLayout.WEST);
            userBox.add(userLabel, BorderLayout.CENTER);

            header.add(userBox, BorderLayout.WEST);
            header.add(timeLabel, BorderLayout.EAST);

            JTextArea contentArea = new JTextArea(post.getContent());
            contentArea.setEditable(false);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setOpaque(false);
            contentArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
            contentArea.setForeground(TEXT);
            contentArea.setBorder(new EmptyBorder(2, 0, 4, 0));

            JButton likeButton = new JButton(post.isLikedByCurrentUser() ? "Unlike" : "Like");
            JLabel likeCountLabel = new JLabel(post.getLikeCount() + " likes");
            JButton commentsButton = new JButton("Comments");
            styleFlatButton(likeButton, post.isLikedByCurrentUser());
            styleFlatButton(commentsButton, commentsVisible);
            likeCountLabel.setForeground(SUBTLE);

            likeButton.addActionListener(e -> {
                postService.likePost(post.getId(), currentUsername);
                refreshFeed(false);
            });
            commentsButton.addActionListener(e -> toggleComments());

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            actions.setOpaque(false);
            actions.add(likeButton);
            actions.add(likeCountLabel);
            actions.add(commentsButton);

            commentsPanel.setLayout(new BoxLayout(commentsPanel, BoxLayout.Y_AXIS));
            commentsPanel.setOpaque(false);
            commentsVisible = expandedPostIds.contains(post.getId());
            commentsPanel.setVisible(commentsVisible);
            renderComments();

            add(header, BorderLayout.NORTH);
            add(contentArea, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout(0, 6));
            bottom.setOpaque(false);
            bottom.add(actions, BorderLayout.NORTH);
            bottom.add(commentsPanel, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        private void toggleComments() {
            commentsVisible = !commentsVisible;
            commentsPanel.setVisible(commentsVisible);
            if (commentsVisible) {
                expandedPostIds.add(post.getId());
            } else {
                expandedPostIds.remove(post.getId());
            }
            revalidate();
            repaint();
        }

        private void renderComments() {
            commentsPanel.removeAll();
            for (Comment comment : post.getComments()) {
                JPanel row = new JPanel(new BorderLayout(6, 0));
                row.setOpaque(true);
                row.setBackground(new Color(250, 251, 253));
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(236, 239, 244)),
                        new EmptyBorder(6, 8, 6, 8)
                ));
                JLabel meta = new JLabel(comment.getUsername() + " • " + comment.getCreatedAt().format(commentTimeFmt));
                meta.setForeground(SUBTLE);
                JTextArea text = new JTextArea(comment.getContent());
                text.setEditable(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                text.setOpaque(false);
                text.setForeground(TEXT);
                text.setBorder(new EmptyBorder(2, 0, 2, 0));
                row.add(meta, BorderLayout.NORTH);
                row.add(text, BorderLayout.CENTER);
                commentsPanel.add(row);
                commentsPanel.add(Box.createVerticalStrut(6));
            }

            JPanel addCommentPanel = new JPanel(new BorderLayout(6, 0));
            addCommentPanel.setOpaque(false);
            JTextField commentField = new JTextField();
            commentField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(6, 8, 6, 8)
            ));
            commentField.setText(commentDrafts.getOrDefault(post.getId(), ""));
            commentField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    commentDrafts.put(post.getId(), commentField.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    commentDrafts.put(post.getId(), commentField.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    commentDrafts.put(post.getId(), commentField.getText());
                }
            });
            JButton addButton = new JButton("Add");
            addButton.setBackground(PRIMARY);
            addButton.setForeground(Color.WHITE);
            addButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            addButton.addActionListener(e -> {
                String content = sanitize(commentField.getText());
                if (content.isBlank()) {
                    return;
                }
                if (postService.addComment(post.getId(), currentUsername, content)) {
                    commentDrafts.remove(post.getId());
                    expandedPostIds.add(post.getId());
                    refreshFeed(false);
                }
            });
            addCommentPanel.add(commentField, BorderLayout.CENTER);
            addCommentPanel.add(addButton, BorderLayout.EAST);
            commentsPanel.add(addCommentPanel);
        }
    }

    private String initial(String username) {
        if (username == null || username.isBlank()) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(username.trim().charAt(0)));
    }

    private void styleFlatButton(JButton button, boolean active) {
        button.setFocusable(false);
        button.setBackground(active ? new Color(226, 239, 255) : new Color(242, 246, 251));
        button.setForeground(active ? new Color(22, 97, 199) : TEXT);
        button.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }
}
