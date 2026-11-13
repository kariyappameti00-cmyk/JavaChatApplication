
package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

public class ChatFrame extends JFrame {

    private JPanel chatPanel;
    private JPanel historyPanel;

    private JScrollPane chatScroll;
    private JScrollPane historyScroll;

    private JTextField messageField;
    private JLabel statusLabel;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private String username;
    private boolean loggedOut = false;

    private final Color BACKGROUND = new Color(245, 247, 251);
    private final Color NAVY = new Color(24, 39, 75);
    private final Color BLUE = new Color(219, 234, 254);
    private final Color GREY = new Color(235, 238, 243);

    public ChatFrame(
            Socket socket,
            BufferedReader input,
            PrintWriter output,
            String username) {

        this.socket = socket;
        this.input = input;
        this.output = output;
        this.username = username;

        setTitle("Java Chat Application");
        setSize(900, 650);
        setMinimumSize(new Dimension(650, 500));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND);

        // ==============================
        // TOP HEADER
        // ==============================

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(15, 22, 15, 22));

        JLabel titleLabel = new JLabel("JAVA CHAT");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JPanel userPanel = new JPanel();
        userPanel.setOpaque(false);
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));

        JLabel userLabel = new JLabel("Logged in as: " + username);
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.BOLD, 13));

        statusLabel = new JLabel("● Connected");
        statusLabel.setForeground(new Color(110, 231, 150));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        userPanel.add(userLabel);
        userPanel.add(Box.createVerticalStrut(4));
        userPanel.add(statusLabel);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(userPanel, BorderLayout.EAST);

        // ==============================
        // CHAT PANEL
        // ==============================

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BACKGROUND);
        chatPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(BACKGROUND);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        // ==============================
        // HISTORY PANEL
        // ==============================

        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setBackground(BACKGROUND);
        historyPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        historyScroll = new JScrollPane(historyPanel);
        historyScroll.setBorder(null);
        historyScroll.getViewport().setBackground(BACKGROUND);
        historyScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Separate Chat and History tabs
        JTabbedPane tabs = new JTabbedPane();

        tabs.setFont(new Font("Arial", Font.BOLD, 13));
        tabs.addTab("  Chat  ", chatScroll);
        tabs.addTab("  Chat History  ", historyScroll);

        // ==============================
        // MESSAGE INPUT
        // ==============================

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(15, 18, 15, 18));

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 15));
        messageField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(210, 215, 225)),
                        new EmptyBorder(10, 12, 10, 12)
                )
        );

        JButton sendButton = createButton("Send", new Color(37, 99, 235));
        JButton clearButton = createButton("Clear Chat", new Color(100, 116, 139));
        JButton logoutButton = createButton("Logout", new Color(220, 53, 69));

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // ==============================
        // BOTTOM BUTTONS
        // ==============================

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);

        actionPanel.add(clearButton);
        actionPanel.add(logoutButton);

        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        // ==============================
        // ADD COMPONENTS
        // ==============================

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // ==============================
        // BUTTON ACTIONS
        // ==============================

        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        // Clear only live chat, not history
        clearButton.addActionListener(e -> {

            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Clear the current chat screen?",
                    "Clear Chat",
                    JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                chatPanel.removeAll();
                chatPanel.revalidate();
                chatPanel.repaint();

                addSystemMessage("Chat screen cleared.");
            }
        });

        logoutButton.addActionListener(e -> logout());

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });

        // Welcome message
        addSystemMessage("Welcome, " + username + "!");
        addSystemMessage("You are connected to the chat server.");

        setVisible(true);

        receiveMessages();
    }

    // ==============================
    // CREATE BUTTON
    // ==============================

    private JButton createButton(String text, Color color) {

        JButton button = new JButton(text);

        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setBorder(
                new EmptyBorder(10, 17, 10, 17)
        );

        return button;
    }

    // ==============================
    // SEND MESSAGE
    // ==============================

    private void sendMessage() {

        String message = messageField.getText().trim();

        if (!message.isEmpty() && !loggedOut) {

            output.println(message);

            messageField.setText("");
        }
    }

    // ==============================
    // RECEIVE MESSAGES
    // ==============================

    private void receiveMessages() {

        Thread receiver = new Thread(() -> {

            try {

                String message;

                while ((message = input.readLine()) != null) {

                    String receivedMessage = message;

                    SwingUtilities.invokeLater(() -> {
                        displayMessage(receivedMessage);
                    });
                }

            } catch (IOException e) {

                SwingUtilities.invokeLater(() -> {

                    if (!loggedOut) {

                        statusLabel.setText("● Disconnected");
                        statusLabel.setForeground(Color.RED);

                        addSystemMessage("Connection closed.");
                    }
                });
            }

        });

        receiver.setDaemon(true);
        receiver.start();
    }

    // ==============================
    // DISPLAY MESSAGE
    // ==============================

    private void displayMessage(String message) {

        String original = message;

        // Remove server prefixes before parsing
        if (message.startsWith("[Private confirmation] ")) {
            message = message.substring(
                    "[Private confirmation] ".length());
        } else if (message.startsWith("[Private] ")) {
            message = message.substring("[Private] ".length());
        }

        Pattern pattern = Pattern.compile(
                "^\\[(\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2})\\]"
                + " \\[([^\\]]+)\\] ([^:]+): (.*)$"
        );

        Matcher matcher = pattern.matcher(message);

        if (matcher.matches()) {

            String time = matcher.group(1);
            String type = matcher.group(2);
            String sender = matcher.group(3);
            String text = matcher.group(4);

            boolean sent = sender.equals(username);

            String displayName = sent ? "You" : sender;

            String shortTime = time.substring(11);

            addChatBubble(
                    displayName,
                    text,
                    shortTime,
                    sent,
                    type
            );

        } else {

            // Server notifications and group requests
            addSystemMessage(original);
        }
    }

    // ==============================
    // CHAT BUBBLE
    // ==============================

    private void addChatBubble(
            String sender,
            String message,
            String time,
            boolean sent,
            String type) {

        JPanel row = createBubbleRow(
                sender, message, time, sent, type
        );

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(12));

        // Keep the same messages in separate history tab
        JPanel historyRow = createBubbleRow(
                sender, message, time, sent, type
        );

        historyPanel.add(historyRow);
        historyPanel.add(Box.createVerticalStrut(12));

        refreshPanels();
    }

    private JPanel createBubbleRow(
            String sender,
            String message,
            String time,
            boolean sent,
            String type) {

        JPanel row = new JPanel(new FlowLayout(
                sent ? FlowLayout.LEFT : FlowLayout.RIGHT,
                8, 2
        ));

        row.setBackground(BACKGROUND);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));

        bubble.setBackground(sent ? BLUE : GREY);

        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel nameLabel = new JLabel(
                escapeHtml(sender) + "  •  " + escapeHtml(type)
        );

        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        nameLabel.setForeground(new Color(75, 85, 105));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel messageLabel = new JLabel(
                "<html><div style='width:340px;'>"
                        + escapeHtml(message)
                        + "</div></html>"
        );

        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setForeground(new Color(30, 41, 59));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timeLabel = new JLabel(time);

        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(100, 110, 125));
        timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        bubble.add(nameLabel);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(messageLabel);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(timeLabel);

        row.add(bubble);

        return row;
    }

    // ==============================
    // SYSTEM MESSAGE
    // ==============================

    private void addSystemMessage(String message) {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));

        row.setBackground(BACKGROUND);

        JLabel label = new JLabel(message);

        label.setFont(new Font("Arial", Font.ITALIC, 12));
        label.setForeground(new Color(110, 120, 135));

        row.add(label);

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(8));

        JPanel historyRow = new JPanel(
                new FlowLayout(FlowLayout.CENTER)
        );

        historyRow.setBackground(BACKGROUND);

        JLabel historyLabel = new JLabel(message);
        historyLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        historyLabel.setForeground(new Color(110, 120, 135));

        historyRow.add(historyLabel);

        historyPanel.add(historyRow);
        historyPanel.add(Box.createVerticalStrut(8));

        refreshPanels();
    }

    // ==============================
    // REFRESH AND AUTO SCROLL
    // ==============================

    private void refreshPanels() {

        chatPanel.revalidate();
        chatPanel.repaint();

        historyPanel.revalidate();
        historyPanel.repaint();

        SwingUtilities.invokeLater(() -> {

            chatScroll.getVerticalScrollBar().setValue(
                    chatScroll.getVerticalScrollBar().getMaximum()
            );

            historyScroll.getVerticalScrollBar().setValue(
                    historyScroll.getVerticalScrollBar().getMaximum()
            );
        });
    }

    // ==============================
    // HTML ESCAPING
    // ==============================

    private String escapeHtml(String text) {

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ==============================
    // LOGOUT
    // ==============================

    private void logout() {

        if (loggedOut) {
            return;
        }

        loggedOut = true;

        output.println("/logout");

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection.");
        }

        dispose();
    }
}
