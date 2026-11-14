
package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFrame extends JFrame {
    private static final Color BLACK = new Color(7, 7, 10);
    private static final Color PANEL = new Color(17, 17, 23);
    private static final Color PANEL2 = new Color(25, 24, 32);
    private static final Color WHITE = new Color(248, 246, 252);
    private static final Color MUTED = new Color(164, 158, 180);
    private static final Color PURPLE = new Color(131, 58, 180);
    private static final Color PINK = new Color(225, 48, 108);
    private static final Color ORANGE = new Color(247, 119, 55);

    private final Socket socket;
    private final BufferedReader input;
    private final PrintWriter output;
    private final String username;

    private boolean loggedOut;
    private String selectedUser = null;

    private JPanel chatPanel;
    private JPanel historyPanel;
    private JPanel conversationsPanel;

    private JScrollPane chatScroll;
    private JScrollPane historyScroll;

    private JTextField messageField;
    private JLabel statusLabel;
    private JLabel conversationTitle;
    private JLabel conversationSubtitle;

    private final Set<String> discoveredUsers = new LinkedHashSet<>();

    public ChatFrame(Socket socket, BufferedReader input,
                     PrintWriter output, String username) {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.username = username;

        setTitle("Java Chat App");
        setSize(1280, 780);
        setMinimumSize(new Dimension(980, 620));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BLACK);

        JPanel root = new JPanel(new BorderLayout(1, 0));
        root.setBackground(BLACK);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildConversationList(), BorderLayout.CENTER);
        root.add(buildChatArea(), BorderLayout.EAST);

        add(root);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });

        addSystemMessage("Welcome, " + username + "!");
        addSystemMessage(
                "You are connected. Public messages are sent to everyone.");

        setVisible(true);
        receiveMessages();
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(205, 0));
        side.setBackground(PANEL);
        side.setBorder(new EmptyBorder(24, 17, 18, 17));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("✦  CHATSPACE");
        brand.setFont(new Font("Arial", Font.BOLD, 19));
        brand.setForeground(WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel avatar = new JLabel(initials(username), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(PURPLE);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Arial", Font.BOLD, 30));
        avatar.setPreferredSize(new Dimension(82, 82));
        avatar.setMaximumSize(new Dimension(82, 82));
        avatar.setBorder(BorderFactory.createLineBorder(PINK, 3));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(username);
        name.setFont(new Font("Arial", Font.BOLD, 17));
        name.setForeground(WHITE);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel("●  Connected");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(55, 220, 125));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        side.add(brand);
        side.add(Box.createVerticalStrut(48));
        side.add(avatar);
        side.add(Box.createVerticalStrut(13));
        side.add(name);
        side.add(Box.createVerticalStrut(6));
        side.add(statusLabel);
        side.add(Box.createVerticalStrut(48));

        side.add(sideButton("◉   Messages", true,
                () -> selectPublic()));

        side.add(Box.createVerticalStrut(10));

        side.add(sideButton("▣   Public Chat", false,
                () -> selectPublic()));

        side.add(Box.createVerticalStrut(10));

        side.add(sideButton("◈   Group Commands", false, () ->
                JOptionPane.showMessageDialog(
                        this,
                        "/create groupName\n" +
                        "/request groupName\n" +
                        "/requests groupName\n" +
                        "/approve groupName username\n" +
                        "/reject groupName username\n" +
                        "/group groupName message",
                        "Group chat commands",
                        JOptionPane.INFORMATION_MESSAGE)));

        side.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("JAVA CHAT APP");
        footer.setForeground(new Color(117, 108, 133));
        footer.setFont(new Font("Arial", Font.BOLD, 11));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        side.add(footer);
        side.add(Box.createVerticalStrut(12));

        side.add(sideButton("↪   Logout", false, this::logout));

        return side;
    }

    private JPanel buildConversationList() {
        JPanel middle = new JPanel(new BorderLayout(0, 15));
        middle.setPreferredSize(new Dimension(285, 0));
        middle.setBackground(BLACK);
        middle.setBorder(new EmptyBorder(25, 18, 20, 18));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel heading = new JLabel("Messages");
        heading.setForeground(WHITE);
        heading.setFont(new Font("Arial", Font.BOLD, 25));

        JLabel compose = new JLabel("＋");
        compose.setForeground(PINK);
        compose.setFont(new Font("Arial", Font.BOLD, 28));

        top.add(heading, BorderLayout.WEST);
        top.add(compose, BorderLayout.EAST);

        JTextField search = new JTextField();
        search.setText("  Search conversations...");
        search.setForeground(MUTED);
        search.setCaretColor(WHITE);
        search.setBackground(PANEL2);
        search.setFont(new Font("Arial", Font.PLAIN, 13));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(64, 55, 76)),
                new EmptyBorder(11, 10, 11, 10)));

        conversationsPanel = new JPanel();
        conversationsPanel.setBackground(BLACK);
        conversationsPanel.setLayout(
                new BoxLayout(conversationsPanel, BoxLayout.Y_AXIS));

        JScrollPane listScroll = new JScrollPane(conversationsPanel);
        styleScroll(listScroll, BLACK);

        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setOpaque(false);
        content.add(search, BorderLayout.NORTH);
        content.add(listScroll, BorderLayout.CENTER);

        middle.add(top, BorderLayout.NORTH);
        middle.add(content, BorderLayout.CENTER);

        addConversationItem(
                "Public Chat",
                "Messages for everyone",
                "◎",
                true,
                this::selectPublic);

        return middle;
    }

    private JPanel buildChatArea() {
        JPanel area = new JPanel(new BorderLayout(0, 0));
        area.setBackground(BLACK);
        area.setBorder(BorderFactory.createMatteBorder(
                0, 1, 0, 0, new Color(48, 42, 58)));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(PANEL);
        header.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel avatar = new JLabel("◎", SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(76, 35, 104));
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Arial", Font.BOLD, 25));
        avatar.setPreferredSize(new Dimension(48, 48));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        conversationTitle = new JLabel("Public Chat");
        conversationTitle.setForeground(WHITE);
        conversationTitle.setFont(new Font("Arial", Font.BOLD, 17));

        conversationSubtitle = new JLabel(
                "Chat with everyone on the server");
        conversationSubtitle.setForeground(MUTED);
        conversationSubtitle.setFont(new Font("Arial", Font.PLAIN, 12));

        titles.add(conversationTitle);
        titles.add(Box.createVerticalStrut(5));
        titles.add(conversationSubtitle);

        JPanel identity = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 12, 0));
        identity.setOpaque(false);
        identity.add(avatar);
        identity.add(titles);

        JLabel tools = new JLabel("  ⋯  ");
        tools.setForeground(WHITE);
        tools.setFont(new Font("Arial", Font.BOLD, 25));

        header.add(identity, BorderLayout.WEST);
        header.add(tools, BorderLayout.EAST);

        chatPanel = messagePanel();
        historyPanel = messagePanel();

        chatScroll = new JScrollPane(chatPanel);
        historyScroll = new JScrollPane(historyPanel);

        styleScroll(chatScroll, BLACK);
        styleScroll(historyScroll, BLACK);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 12));
        tabs.setBackground(PANEL);
        tabs.setForeground(WHITE);

        tabs.addTab("  Conversation  ", chatScroll);
        tabs.addTab("  Chat History  ", historyScroll);

        JPanel composer = new JPanel(new BorderLayout(12, 0));
        composer.setBackground(PANEL);
        composer.setBorder(new EmptyBorder(15, 20, 18, 20));

        messageField = new JTextField();
        messageField.setForeground(WHITE);
        messageField.setCaretColor(WHITE);
        messageField.setBackground(PANEL2);
        messageField.setFont(new Font("Arial", Font.PLAIN, 15));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PURPLE, 2),
                new EmptyBorder(13, 15, 13, 15)));

        messageField.setToolTipText(
                "Type a message. Use /msg username message for a private message.");

        GradientButton send = new GradientButton("➤  Send");
        send.setPreferredSize(new Dimension(112, 48));
        send.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        composer.add(messageField, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);

        area.add(header, BorderLayout.NORTH);
        area.add(tabs, BorderLayout.CENTER);
        area.add(composer, BorderLayout.SOUTH);

        return area;
    }

    private JPanel messagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BLACK);
        panel.setBorder(new EmptyBorder(20, 22, 20, 22));
        return panel;
    }

    private void styleScroll(JScrollPane scroll, Color background) {
        scroll.setBorder(null);
        scroll.getViewport().setBackground(background);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setBackground(PANEL);
    }

    private JButton sideButton(
            String text, boolean active, Runnable action) {

        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(WHITE);
        button.setBackground(active ? PURPLE : PANEL);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(13, 12, 13, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 47));

        button.addActionListener(e -> action.run());

        return button;
    }

    private void addConversationItem(
            String title,
            String preview,
            String symbol,
            boolean active,
            Runnable action) {

        JPanel item = new JPanel(new BorderLayout(11, 0));
        item.setBackground(active ? new Color(55, 28, 75) : PANEL);
        item.setBorder(new EmptyBorder(13, 10, 13, 10));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        JLabel icon = new JLabel(symbol, SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(new Color(75, 34, 100));
        icon.setForeground(Color.WHITE);
        icon.setFont(new Font("Arial", Font.BOLD, 18));
        icon.setPreferredSize(new Dimension(42, 42));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 13));

        JLabel previewLabel = new JLabel(preview);
        previewLabel.setForeground(MUTED);
        previewLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        labels.add(titleLabel);
        labels.add(Box.createVerticalStrut(6));
        labels.add(previewLabel);

        item.add(icon, BorderLayout.WEST);
        item.add(labels, BorderLayout.CENTER);

        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });

        conversationsPanel.add(item);
        conversationsPanel.add(Box.createVerticalStrut(8));
        conversationsPanel.revalidate();
        conversationsPanel.repaint();
    }

    private void selectPublic() {
        selectedUser = null;
        conversationTitle.setText("Public Chat");
        conversationSubtitle.setText(
                "Chat with everyone on the server");

        messageField.setToolTipText("Type a public message...");
        messageField.requestFocusInWindow();
    }

    private void selectUser(String user) {
        selectedUser = user;
        conversationTitle.setText(user);
        conversationSubtitle.setText(
                "Private conversation · use /msg automatically");

        messageField.setToolTipText("Private message to " + user);
        messageField.requestFocusInWindow();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();

        if (message.isEmpty() || loggedOut) {
            return;
        }

        if (selectedUser != null && !message.startsWith("/")) {
            output.println("/msg " + selectedUser + " " + message);
        } else {
            output.println(message);
        }

        messageField.setText("");
    }

    private void receiveMessages() {
        Thread receiver = new Thread(() -> {
            try {
                String message;

                while ((message = input.readLine()) != null) {
                    final String received = message;

                    SwingUtilities.invokeLater(
                            () -> displayMessage(received));
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
        }, "chat-ui-receiver");

        receiver.setDaemon(true);
        receiver.start();
    }

    private void displayMessage(String original) {
        String message = original;

        if (message.startsWith("[Private confirmation] ")) {
            message = message.substring(
                    "[Private confirmation] ".length());

        } else if (message.startsWith("[Private] ")) {
            message = message.substring("[Private] ".length());
        }

        Pattern pattern = Pattern.compile(
                "^\\[(\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2})\\] " +
                "\\[([^\\]]+)\\] ([^:]+): (.*)$");

        Matcher matcher = pattern.matcher(message);

        if (matcher.matches()) {
            String time = matcher.group(1).substring(11);
            String type = matcher.group(2);
            String sender = matcher.group(3);
            String text = matcher.group(4);

            boolean sent = sender.equals(username);

            if (!sent) {
                addDiscoveredUser(sender);
            }

            addChatBubble(
                    sent ? "You" : sender,
                    text,
                    time,
                    sent,
                    type);

        } else {
            addSystemMessage(original);
        }
    }

    private void addDiscoveredUser(String user) {
        if (discoveredUsers.add(user)) {
            addConversationItem(
                    user,
                    "Tap to start a private chat",
                    "●",
                    false,
                    () -> selectUser(user));
        }
    }

    private void addChatBubble(
            String sender,
            String message,
            String time,
            boolean sent,
            String type) {

        JPanel row = new JPanel(new FlowLayout(
                sent ? FlowLayout.RIGHT : FlowLayout.LEFT,
                5, 3));

        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(
                sent ? new Color(88, 35, 139) : PANEL2);

        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel meta = new JLabel(sender + "  ·  " + type);
        meta.setForeground(
                sent ? new Color(232, 205, 255) : MUTED);
        meta.setFont(new Font("Arial", Font.BOLD, 10));

        JLabel body = new JLabel(
                "<html><div style='width:390px;'>" +
                escapeHtml(message) +
                "</div></html>");

        body.setForeground(WHITE);
        body.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel clock = new JLabel(time);
        clock.setForeground(new Color(190, 178, 204));
        clock.setFont(new Font("Arial", Font.PLAIN, 10));
        clock.setAlignmentX(Component.RIGHT_ALIGNMENT);

        bubble.add(meta);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(body);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(clock);

        row.add(bubble);

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(9));

        addHistoryBubble(sender, message, time, sent, type);
        refreshPanels();
    }

    private void addHistoryBubble(
            String sender,
            String message,
            String time,
            boolean sent,
            String type) {

        JPanel row = new JPanel(new FlowLayout(
                sent ? FlowLayout.RIGHT : FlowLayout.LEFT));

        row.setOpaque(false);

        JLabel label = new JLabel(
                "[" + time + "] " + sender +
                " (" + type + "): " + message);

        label.setForeground(WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 12));

        row.add(label);

        historyPanel.add(row);
        historyPanel.add(Box.createVerticalStrut(7));
    }

    private void addSystemMessage(String message) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);

        JLabel label = new JLabel(message);
        label.setForeground(new Color(177, 151, 199));
        label.setFont(new Font("Arial", Font.ITALIC, 12));

        row.add(label);

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(8));

        JPanel historyRow =
                new JPanel(new FlowLayout(FlowLayout.CENTER));

        historyRow.setOpaque(false);

        JLabel historyLabel = new JLabel(message);
        historyLabel.setForeground(MUTED);
        historyLabel.setFont(
                new Font("Arial", Font.ITALIC, 12));

        historyRow.add(historyLabel);

        historyPanel.add(historyRow);

        refreshPanels();
    }

    private void refreshPanels() {
        chatPanel.revalidate();
        chatPanel.repaint();

        historyPanel.revalidate();
        historyPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            chatScroll.getVerticalScrollBar().setValue(
                    chatScroll.getVerticalScrollBar().getMaximum());

            historyScroll.getVerticalScrollBar().setValue(
                    historyScroll.getVerticalScrollBar().getMaximum());
        });
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String initials(String value) {
        return value.isEmpty()
                ? "?"
                : value.substring(0, 1).toUpperCase();
    }

    private void logout() {
        if (loggedOut) {
            return;
        }

        loggedOut = true;
        output.println("/logout");

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println(
                    "Error closing connection: " + e.getMessage());
        }

        dispose();
    }

    private static class GradientButton extends JButton {

        GradientButton(String text) {
            super(text);

            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(new GradientPaint(
                    0, 0, PURPLE,
                    getWidth(), getHeight(), ORANGE));

            g2.fillRoundRect(
                    0, 0, getWidth(), getHeight(), 24, 24);

            g2.dispose();

            super.paintComponent(g);
        }
    }
}

