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
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        JPanel createPostPanel = new JPanel(new BorderLayout(8, 8));
        createPostPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230)),
                new EmptyBorder(8, 8, 8, 8)
        ));
        createPostPanel.setBackground(Color.WHITE);

        postInput.setLineWrap(true);
        postInput.setWrapStyleWord(true);
        postInput.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane inputScroll = new JScrollPane(postInput);
        inputScroll.setPreferredSize(new Dimension(200, 90));
        inputScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));

        postButton.setFocusable(false);
        postButton.addActionListener(e -> createPost());

        createPostPanel.add(new JLabel("Create Post"), BorderLayout.NORTH);
        createPostPanel.add(inputScroll, BorderLayout.CENTER);
        createPostPanel.add(postButton, BorderLayout.EAST);
        statusLabel.setForeground(new Color(130, 54, 54));
        createPostPanel.add(statusLabel, BorderLayout.SOUTH);

        feedListPanel.setLayout(new BoxLayout(feedListPanel, BoxLayout.Y_AXIS));
        feedListPanel.setBackground(new Color(245, 247, 250));
        feedScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));
        feedScroll.getVerticalScrollBar().setUnitIncrement(16);

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
            setLayout(new BorderLayout(6, 6));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 224, 230)),
                    new EmptyBorder(10, 10, 10, 10)
            ));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            JLabel userLabel = new JLabel(post.getUsername());
            userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD));
            JLabel timeLabel = new JLabel(post.getCreatedAt().format(postTimeFmt));
            timeLabel.setForeground(new Color(120, 129, 140));
            header.add(userLabel, BorderLayout.WEST);
            header.add(timeLabel, BorderLayout.EAST);

            JTextArea contentArea = new JTextArea(post.getContent());
            contentArea.setEditable(false);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setOpaque(false);
            contentArea.setBorder(new EmptyBorder(6, 0, 6, 0));

            JButton likeButton = new JButton(post.isLikedByCurrentUser() ? "Unlike" : "Like");
            JLabel likeCountLabel = new JLabel(post.getLikeCount() + " likes");
            JButton commentsButton = new JButton("Comments");

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
                row.setOpaque(false);
                JLabel meta = new JLabel(comment.getUsername() + " • " + comment.getCreatedAt().format(commentTimeFmt));
                meta.setForeground(new Color(120, 129, 140));
                JTextArea text = new JTextArea(comment.getContent());
                text.setEditable(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                text.setOpaque(false);
                text.setBorder(new EmptyBorder(2, 0, 6, 0));
                row.add(meta, BorderLayout.NORTH);
                row.add(text, BorderLayout.CENTER);
                commentsPanel.add(row);
            }

            JPanel addCommentPanel = new JPanel(new BorderLayout(6, 0));
            addCommentPanel.setOpaque(false);
            JTextField commentField = new JTextField();
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
}
