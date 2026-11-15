package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

    private boolean loggedOut = false;

    private String selectedUser = null;
    private String selectedGroup = null;
    private boolean selectedGroupMember = false;

    private JPanel chatPanel;
    private JPanel historyPanel;
    private JPanel conversationsPanel;

    private JScrollPane chatScroll;
    private JScrollPane historyScroll;

    private JTextField messageField;

    private JLabel statusLabel;
    private JLabel conversationTitle;
    private JLabel conversationSubtitle;

    private JButton joinGroupButton;

    private final Set<String> discoveredUsers =
            new LinkedHashSet<>();

    private final Map<String, GroupInfo> groups =
            new LinkedHashMap<>();

    private boolean receivingGroupList = false;

    public ChatFrame(
            Socket socket,
            BufferedReader input,
            PrintWriter output,
            String username) {

        this.socket = socket;
        this.input = input;
        this.output = output;
        this.username = username;

        setTitle("ChatSpace - " + username);
        setSize(1350, 820);
        setMinimumSize(new Dimension(1050, 650));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BLACK);

        // Three-column layout
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BLACK);

        GridBagConstraints c = new GridBagConstraints();

        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;

        // LEFT SIDEBAR
        c.gridx = 0;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 1);

        root.add(buildSidebar(), c);

        // MIDDLE CONVERSATION LIST
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 1);

        root.add(buildConversationList(), c);

        // RIGHT CHAT AREA
        c.gridx = 2;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 0);

        root.add(buildChatArea(), c);

        setContentPane(root);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });

        addSystemMessage("Welcome, " + username + "!");

        addSystemMessage(
                "You are connected. Select a conversation or group from the left.");

        setVisible(true);

        // Start receiving messages
        receiveMessages();

        // Retrieve existing groups from server
        refreshGroups();
    }

    // ==========================================
    // LEFT SIDEBAR
    // ==========================================

    private JPanel buildSidebar() {

        JPanel side = new JPanel();

        side.setPreferredSize(new Dimension(190, 0));
        side.setBackground(PANEL);

        side.setBorder(
                new EmptyBorder(24, 16, 18, 16));

        side.setLayout(
                new BoxLayout(side, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("✦ CHATSPACE");

        brand.setFont(
                new Font("Arial", Font.BOLD, 18));

        brand.setForeground(WHITE);

        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel avatar = new JLabel(
                initials(username),
                SwingConstants.CENTER);

        avatar.setOpaque(true);
        avatar.setBackground(PURPLE);
        avatar.setForeground(Color.WHITE);

        avatar.setFont(
                new Font("Arial", Font.BOLD, 29));

        avatar.setPreferredSize(
                new Dimension(76, 76));

        avatar.setMaximumSize(
                new Dimension(76, 76));

        avatar.setBorder(
                BorderFactory.createLineBorder(PINK, 3));

        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(username);

        name.setFont(
                new Font("Arial", Font.BOLD, 15));

        name.setForeground(WHITE);

        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel("● Connected");

        statusLabel.setFont(
                new Font("Arial", Font.PLAIN, 11));

        statusLabel.setForeground(
                new Color(55, 220, 125));

        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        side.add(brand);
        side.add(Box.createVerticalStrut(45));
        side.add(avatar);
        side.add(Box.createVerticalStrut(12));
        side.add(name);
        side.add(Box.createVerticalStrut(6));
        side.add(statusLabel);

        side.add(Box.createVerticalStrut(42));

        side.add(sideButton(
                "◎  Public Chat",
                this::selectPublic));

        side.add(Box.createVerticalStrut(10));

        side.add(sideButton(
                "＋  Create Group",
                this::createGroup));

        side.add(Box.createVerticalStrut(10));

        side.add(sideButton(
                "↻  Refresh Groups",
                this::refreshGroups));

        side.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("JAVA CHAT APP");

        footer.setForeground(
                new Color(117, 108, 133));

        footer.setFont(
                new Font("Arial", Font.BOLD, 10));

        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        side.add(footer);
        side.add(Box.createVerticalStrut(12));

        side.add(sideButton(
                "↪  Logout",
                this::logout));

        return side;
    }

    // ==========================================
    // MIDDLE CONVERSATION PANEL
    // ==========================================

    private JPanel buildConversationList() {

        JPanel middle = new JPanel(
                new BorderLayout(0, 12));

        middle.setPreferredSize(
                new Dimension(300, 0));

        middle.setBackground(BLACK);

        middle.setBorder(
                new EmptyBorder(23, 16, 18, 16));

        JLabel heading = new JLabel("Messages");

        heading.setForeground(WHITE);

        heading.setFont(
                new Font("Arial", Font.BOLD, 24));

        JTextField search = new JTextField();

        search.setForeground(MUTED);
        search.setCaretColor(WHITE);
        search.setBackground(PANEL2);

        search.setFont(
                new Font("Arial", Font.PLAIN, 12));

        search.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(64, 55, 76)),
                        new EmptyBorder(10, 10, 10, 10)));

        search.setToolTipText(
                "Search conversations");

        conversationsPanel = new JPanel();

        conversationsPanel.setBackground(BLACK);

        conversationsPanel.setLayout(
                new BoxLayout(
                        conversationsPanel,
                        BoxLayout.Y_AXIS));

        JScrollPane listScroll =
                new JScrollPane(conversationsPanel);

        styleScroll(listScroll, BLACK);

        JPanel top = new JPanel(
                new BorderLayout(0, 12));

        top.setOpaque(false);

        top.add(heading, BorderLayout.NORTH);
        top.add(search, BorderLayout.SOUTH);

        middle.add(top, BorderLayout.NORTH);
        middle.add(listScroll, BorderLayout.CENTER);

        rebuildConversationList();

        return middle;
    }

    // ==========================================
    // RIGHT CHAT PANEL
    // ==========================================

    private JPanel buildChatArea() {

        JPanel area = new JPanel(
                new BorderLayout());

        area.setBackground(BLACK);

        area.setBorder(
                BorderFactory.createMatteBorder(
                        0, 1, 0, 0,
                        new Color(48, 42, 58)));

        JPanel header = new JPanel(
                new BorderLayout(12, 0));

        header.setBackground(PANEL);

        header.setBorder(
                new EmptyBorder(18, 22, 18, 22));

        JLabel avatar = new JLabel(
                "◎",
                SwingConstants.CENTER);

        avatar.setOpaque(true);
        avatar.setBackground(
                new Color(76, 35, 104));

        avatar.setForeground(Color.WHITE);

        avatar.setFont(
                new Font("Arial", Font.BOLD, 24));

        avatar.setPreferredSize(
                new Dimension(46, 46));

        JPanel titles = new JPanel();

        titles.setOpaque(false);

        titles.setLayout(
                new BoxLayout(
                        titles,
                        BoxLayout.Y_AXIS));

        conversationTitle = new JLabel("Public Chat");

        conversationTitle.setForeground(WHITE);

        conversationTitle.setFont(
                new Font("Arial", Font.BOLD, 17));

        conversationSubtitle = new JLabel(
                "Chat with everyone on the server");

        conversationSubtitle.setForeground(MUTED);

        conversationSubtitle.setFont(
                new Font("Arial", Font.PLAIN, 11));

        titles.add(conversationTitle);
        titles.add(Box.createVerticalStrut(5));
        titles.add(conversationSubtitle);

        JPanel identity = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT, 12, 0));

        identity.setOpaque(false);

        identity.add(avatar);
        identity.add(titles);

        JPanel headerActions = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT, 8, 4));

        headerActions.setOpaque(false);

        joinGroupButton = smallButton(
                "Request to Join",
                this::requestToJoinSelectedGroup);

        joinGroupButton.setVisible(false);

        JButton refresh = smallButton(
                "Refresh Groups",
                this::refreshGroups);

        headerActions.add(joinGroupButton);
        headerActions.add(refresh);

        header.add(identity, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);

        chatPanel = messagePanel();
        historyPanel = messagePanel();

        chatScroll = new JScrollPane(chatPanel);
        historyScroll = new JScrollPane(historyPanel);

        styleScroll(chatScroll, BLACK);
        styleScroll(historyScroll, BLACK);

        JTabbedPane tabs = new JTabbedPane();

        tabs.setFont(
                new Font("Arial", Font.BOLD, 12));

        tabs.setBackground(PANEL);
        tabs.setForeground(WHITE);

        tabs.addTab("Conversation", chatScroll);
        tabs.addTab("Chat History", historyScroll);

        JPanel composer = new JPanel(
                new BorderLayout(12, 0));

        composer.setBackground(PANEL);

        composer.setBorder(
                new EmptyBorder(14, 18, 17, 18));

        messageField = new JTextField();

        messageField.setForeground(WHITE);
        messageField.setCaretColor(WHITE);
        messageField.setBackground(PANEL2);

        messageField.setFont(
                new Font("Arial", Font.PLAIN, 14));

        messageField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                PURPLE, 2),
                        new EmptyBorder(12, 14, 12, 14)));

        JButton send = new GradientButton("➤ Send");

        send.setPreferredSize(
                new Dimension(112, 46));

        send.addActionListener(e -> sendMessage());

        messageField.addActionListener(
                e -> sendMessage());

        composer.add(messageField, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);

        area.add(header, BorderLayout.NORTH);
        area.add(tabs, BorderLayout.CENTER);
        area.add(composer, BorderLayout.SOUTH);

        return area;
    }

    // ==========================================
    // COMMON UI COMPONENTS
    // ==========================================

    private JPanel messagePanel() {

        JPanel panel = new JPanel();

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS));

        panel.setBackground(BLACK);

        panel.setBorder(
                new EmptyBorder(18, 20, 18, 20));

        return panel;
    }

    private void styleScroll(
            JScrollPane scroll,
            Color background) {

        scroll.setBorder(null);

        scroll.getViewport().setBackground(background);

        scroll.getVerticalScrollBar().setUnitIncrement(18);

        scroll.getVerticalScrollBar().setBackground(PANEL);
    }

    private JButton sideButton(
            String text,
            Runnable action) {

        JButton button = new JButton(text);

        button.setHorizontalAlignment(
                SwingConstants.LEFT);

        button.setFont(
                new Font("Arial", Font.BOLD, 12));

        button.setForeground(WHITE);
        button.setBackground(PANEL);
        button.setOpaque(true);

        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(12, 10, 12, 10));

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR));

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45));

        button.addActionListener(e -> action.run());

        return button;
    }

    private JButton smallButton(
            String text,
            Runnable action) {

        JButton button = new JButton(text);

        button.setForeground(WHITE);

        button.setBackground(
                new Color(65, 35, 85));

        button.setFont(
                new Font("Arial", Font.BOLD, 11));

        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(8, 11, 8, 11));

        button.addActionListener(e -> action.run());

        return button;
    }

    // ==========================================
    // CONVERSATION LIST AND GROUP LIST
    // ==========================================

    private void rebuildConversationList() {

        if (conversationsPanel == null) {
            return;
        }

        conversationsPanel.removeAll();

        addSectionHeading("DIRECT MESSAGES");

        addConversationItem(
                "Public Chat",
                "Messages for everyone",
                "◎",
                selectedUser == null && selectedGroup == null,
                this::selectPublic);

        for (String user : discoveredUsers) {

            addConversationItem(
                    user,
                    "Private conversation",
                    "●",
                    user.equals(selectedUser),
                    () -> selectUser(user));
        }

        addSectionHeading("GROUPS");

        if (groups.isEmpty()) {

            JLabel empty = new JLabel("  No groups found");

            empty.setForeground(MUTED);

            empty.setFont(
                    new Font("Arial", Font.ITALIC, 11));

            empty.setBorder(
                    new EmptyBorder(8, 8, 12, 4));

            empty.setAlignmentX(Component.LEFT_ALIGNMENT);

            conversationsPanel.add(empty);

        } else {

            for (GroupInfo info : groups.values()) {

                String detail = "Admin: " + info.admin;

                if (info.member) {
                    detail += "  ·  Member";
                } else {
                    detail += "  ·  Public group";
                }

                addConversationItem(
                        info.name,
                        detail,
                        "▣",
                        info.name.equals(selectedGroup),
                        () -> selectGroup(info));
            }
        }

        conversationsPanel.revalidate();
        conversationsPanel.repaint();
    }

    private void addSectionHeading(String text) {

        JLabel label = new JLabel(text);

        label.setForeground(PINK);

        label.setFont(
                new Font("Arial", Font.BOLD, 10));

        label.setBorder(
                new EmptyBorder(16, 5, 8, 5));

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        conversationsPanel.add(label);
    }

    private void addConversationItem(
            String title,
            String preview,
            String symbol,
            boolean active,
            Runnable action) {

        JPanel item = new JPanel(
                new BorderLayout(10, 0));

        item.setBackground(
                active
                        ? new Color(55, 28, 75)
                        : PANEL);

        item.setBorder(
                new EmptyBorder(10, 8, 10, 8));

        item.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 64));

        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(
                symbol,
                SwingConstants.CENTER);

        icon.setOpaque(true);

        icon.setBackground(
                new Color(75, 34, 100));

        icon.setForeground(Color.WHITE);

        icon.setFont(
                new Font("Arial", Font.BOLD, 17));

        icon.setPreferredSize(
                new Dimension(38, 38));

        JPanel labels = new JPanel();

        labels.setOpaque(false);

        labels.setLayout(
                new BoxLayout(
                        labels,
                        BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);

        titleLabel.setForeground(WHITE);

        titleLabel.setFont(
                new Font("Arial", Font.BOLD, 12));

        JLabel previewLabel = new JLabel(preview);

        previewLabel.setForeground(MUTED);

        previewLabel.setFont(
                new Font("Arial", Font.PLAIN, 10));

        labels.add(titleLabel);
        labels.add(Box.createVerticalStrut(5));
        labels.add(previewLabel);

        item.add(icon, BorderLayout.WEST);
        item.add(labels, BorderLayout.CENTER);

        item.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR));

        item.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });

        conversationsPanel.add(item);

        conversationsPanel.add(
                Box.createVerticalStrut(6));
    }

    // ==========================================
    // GROUP MANAGEMENT
    // ==========================================

    private void createGroup() {

        String name = JOptionPane.showInputDialog(
                this,
                "Enter a name for the new group:",
                "Create Group",
                JOptionPane.PLAIN_MESSAGE);

        if (name == null || name.trim().isEmpty()) {
            return;
        }

        output.println("/create " + name.trim());
    }

    // Ask server to return all existing groups
    private void refreshGroups() {

        if (!loggedOut) {
            output.println("/groups");
        }
    }

    private void requestToJoinSelectedGroup() {

        if (selectedGroup == null) {
            return;
        }

        GroupInfo info = groups.get(selectedGroup);

        if (info == null) {
            refreshGroups();
            return;
        }

        output.println(
                "/request64 " + encode(selectedGroup));

        addSystemMessage(
                "Join request sent for group: " + selectedGroup);
    }

    private void selectGroup(GroupInfo info) {

        selectedUser = null;

        selectedGroup = info.name;

        selectedGroupMember = info.member;

        conversationTitle.setText(info.name);

        conversationSubtitle.setText(
                "Group · Admin: " + info.admin
                        + (info.member
                        ? " · You are a member"
                        : " · Membership approval required"));

        joinGroupButton.setVisible(!info.member);

        messageField.setToolTipText(
                info.member
                        ? "Message " + info.name
                        : "Request permission to join this group first");

        rebuildConversationList();

        if (!info.member) {

            addSystemMessage(
                    "You are not a member of this group. Use Request to Join.");
        }

        messageField.requestFocusInWindow();
    }

    // ==========================================
    // CONVERSATION SELECTION
    // ==========================================

    private void selectPublic() {

        selectedUser = null;
        selectedGroup = null;
        selectedGroupMember = false;

        conversationTitle.setText("Public Chat");

        conversationSubtitle.setText(
                "Chat with everyone on the server");

        messageField.setToolTipText(
                "Type a public message...");

        joinGroupButton.setVisible(false);

        rebuildConversationList();

        messageField.requestFocusInWindow();
    }

    private void selectUser(String user) {

        selectedUser = user;

        selectedGroup = null;
        selectedGroupMember = false;

        conversationTitle.setText(user);

        conversationSubtitle.setText(
                "Private conversation");

        messageField.setToolTipText(
                "Private message to " + user);

        joinGroupButton.setVisible(false);

        rebuildConversationList();

        messageField.requestFocusInWindow();
    }

    // ==========================================
    // SEND MESSAGES
    // ==========================================

    private void sendMessage() {

        String message = messageField.getText().trim();

        if (message.isEmpty() || loggedOut) {
            return;
        }

        if (selectedGroup != null) {

            if (!selectedGroupMember) {

                JOptionPane.showMessageDialog(
                        this,
                        "You must be an approved member before sending messages.",
                        "Group membership required",
                        JOptionPane.INFORMATION_MESSAGE);

                return;
            }

            output.println(
                    "/group64 "
                            + encode(selectedGroup)
                            + " "
                            + encode(message));

        } else if (selectedUser != null) {

            output.println(
                    "/msg " + selectedUser + " " + message);

        } else {

            output.println(message);
        }

        messageField.setText("");
    }

    // ==========================================
    // RECEIVE SERVER MESSAGES
    // ==========================================

    private void receiveMessages() {

        Thread receiver = new Thread(() -> {

            try {

                String message;

                while ((message = input.readLine()) != null) {

                    final String received = message;

                    SwingUtilities.invokeLater(
                            () -> handleServerMessage(received));
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

    // Process group list protocol and normal messages
    private void handleServerMessage(String message) {

        if ("[[GROUP_LIST_BEGIN]]".equals(message)) {

            receivingGroupList = true;

            groups.clear();

            return;
        }

        if (message.startsWith("[[GROUP_ITEM]]")) {

            if (receivingGroupList) {

                String data = message.substring(
                        "[[GROUP_ITEM]]".length());

                String[] parts = data.split("\\|", -1);

                if (parts.length == 3) {

                    try {

                        String name = decode(parts[0]);

                        String admin = decode(parts[1]);

                        boolean member =
                                Boolean.parseBoolean(parts[2]);

                        groups.put(
                                name,
                                new GroupInfo(name, admin, member));

                    } catch (IllegalArgumentException ignored) {
                        // Ignore malformed group records
                    }
                }
            }

            return;
        }

        if ("[[GROUP_LIST_END]]".equals(message)) {

            receivingGroupList = false;

            if (selectedGroup != null) {

                GroupInfo selected = groups.get(selectedGroup);

                if (selected == null) {

                    selectedGroup = null;

                    selectedGroupMember = false;

                    selectPublic();

                } else {

                    selectedGroupMember = selected.member;

                    conversationSubtitle.setText(
                            "Group · Admin: " + selected.admin
                                    + (selected.member
                                    ? " · You are a member"
                                    : " · Membership approval required"));

                    joinGroupButton.setVisible(!selected.member);
                }
            }

            rebuildConversationList();

            return;
        }

        if ("[GROUPS_CHANGED]".equals(message)) {

            refreshGroups();

            return;
        }

        displayMessage(message);

        if (message.startsWith("Group created:")) {
            refreshGroups();

        } else if (message.contains("approved!")
                || message.contains("has joined")) {

            refreshGroups();
        }
    }

    // ==========================================
    // DISPLAY MESSAGES
    // ==========================================

    private void displayMessage(String original) {

        String message = original;

        if (message.startsWith("[Private confirmation] ")) {

            message = message.substring(
                    "[Private confirmation] ".length());

        } else if (message.startsWith("[Private] ")) {

            message = message.substring(
                    "[Private] ".length());
        }

        Pattern pattern = Pattern.compile(
                "^\\[(\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2})\\] \\[([^\\]]+)\\] ([^:]+): (.*)$");

        Matcher matcher = pattern.matcher(message);

        if (matcher.matches()) {

            String time = matcher.group(1).substring(11);

            String type = matcher.group(2);

            String sender = matcher.group(3);

            String text = matcher.group(4);

            boolean sent = sender.equals(username);

            if (!sent && type.startsWith("Private to ")) {

                String otherUser =
                        type.substring("Private to ".length());

                addDiscoveredUser(otherUser);

            } else if (!sent && type.equals("Public")) {

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

        if (user.equals(username)
                || !discoveredUsers.add(user)) {

            return;
        }

        rebuildConversationList();
    }

    // ==========================================
    // CHAT BUBBLES
    // ==========================================

    private void addChatBubble(
            String sender,
            String message,
            String time,
            boolean sent,
            String type) {

        JPanel row = new JPanel(
                new FlowLayout(
                        sent
                                ? FlowLayout.RIGHT
                                : FlowLayout.LEFT,
                        5,
                        3));

        row.setOpaque(false);

        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel();

        bubble.setLayout(
                new BoxLayout(
                        bubble,
                        BoxLayout.Y_AXIS));

        bubble.setBackground(
                sent
                        ? new Color(88, 35, 139)
                        : PANEL2);

        bubble.setBorder(
                new EmptyBorder(10, 14, 10, 14));

        JLabel meta = new JLabel(
                sender + "  ·  " + type);

        meta.setForeground(
                sent
                        ? new Color(232, 205, 255)
                        : MUTED);

        meta.setFont(
                new Font("Arial", Font.BOLD, 10));

        JLabel body = new JLabel(
                "<html><div style='width:420px;'>"
                        + escapeHtml(message)
                        + "</div></html>");

        body.setForeground(WHITE);

        body.setFont(
                new Font("Arial", Font.PLAIN, 14));

        JLabel clock = new JLabel(time);

        clock.setForeground(
                new Color(190, 178, 204));

        clock.setFont(
                new Font("Arial", Font.PLAIN, 10));

        clock.setAlignmentX(Component.RIGHT_ALIGNMENT);

        bubble.add(meta);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(body);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(clock);

        row.add(bubble);

        chatPanel.add(row);

        chatPanel.add(Box.createVerticalStrut(9));

        addHistoryBubble(
                sender, message, time, sent, type);

        refreshPanels();
    }

    private void addHistoryBubble(
            String sender,
            String message,
            String time,
            boolean sent,
            String type) {

        JPanel row = new JPanel(
                new FlowLayout(
                        sent
                                ? FlowLayout.RIGHT
                                : FlowLayout.LEFT));

        row.setOpaque(false);

        JLabel label = new JLabel(
                "[" + time + "] "
                        + sender
                        + " (" + type + "): "
                        + message);

        label.setForeground(WHITE);

        label.setFont(
                new Font("Arial", Font.PLAIN, 12));

        row.add(label);

        historyPanel.add(row);

        historyPanel.add(
                Box.createVerticalStrut(7));
    }

    private void addSystemMessage(String message) {

        if (chatPanel == null || historyPanel == null) {
            return;
        }

        JPanel row = new JPanel(
                new FlowLayout(FlowLayout.CENTER));

        row.setOpaque(false);

        JLabel label = new JLabel(
                escapeHtml(message));

        label.setForeground(
                new Color(177, 151, 199));

        label.setFont(
                new Font("Arial", Font.ITALIC, 12));

        row.add(label);

        chatPanel.add(row);

        chatPanel.add(
                Box.createVerticalStrut(8));

        JPanel historyRow = new JPanel(
                new FlowLayout(FlowLayout.CENTER));

        historyRow.setOpaque(false);

        JLabel historyLabel = new JLabel(
                escapeHtml(message));

        historyLabel.setForeground(MUTED);

        historyLabel.setFont(
                new Font("Arial", Font.ITALIC, 12));

        historyRow.add(historyLabel);

        historyPanel.add(historyRow);

        refreshPanels();
    }

    private void refreshPanels() {

        if (chatPanel == null || historyPanel == null) {
            return;
        }

        chatPanel.revalidate();
        chatPanel.repaint();

        historyPanel.revalidate();
        historyPanel.repaint();

        SwingUtilities.invokeLater(() -> {

            if (chatScroll != null) {

                chatScroll.getVerticalScrollBar().setValue(
                        chatScroll.getVerticalScrollBar().getMaximum());
            }

            if (historyScroll != null) {

                historyScroll.getVerticalScrollBar().setValue(
                        historyScroll.getVerticalScrollBar().getMaximum());
            }
        });
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private String escapeHtml(String text) {

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String initials(String value) {

        return value == null || value.isEmpty()
                ? "?"
                : value.substring(0, 1).toUpperCase();
    }

    private String encode(String value) {

        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {

        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8);
    }

    // ==========================================
    // LOGOUT
    // ==========================================

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
                    "Error closing connection: "
                            + e.getMessage());
        }

        dispose();
    }

    // ==========================================
    // GROUP INFORMATION CLASS
    // ==========================================

    private static class GroupInfo {

        final String name;
        final String admin;
        final boolean member;

        GroupInfo(
                String name,
                String admin,
                boolean member) {

            this.name = name;
            this.admin = admin;
            this.member = member;
        }
    }

    // ==========================================
    // GRADIENT BUTTON
    // ==========================================

    private static class GradientButton extends JButton {

        GradientButton(String text) {

            super(text);

            setForeground(Color.WHITE);

            setFont(
                    new Font("Arial", Font.BOLD, 13));

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);

            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(
                    new GradientPaint(
                            0, 0, PURPLE,
                            getWidth(), getHeight(), ORANGE));

            g2.fillRoundRect(
                    0, 0,
                    getWidth(), getHeight(),
                    22, 22);

            g2.dispose();

            super.paintComponent(g);
        }
    }
}