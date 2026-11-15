package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class LoginFrame extends JFrame {

    private static final Color BLACK = new Color(8, 8, 12);
    private static final Color PANEL = new Color(20, 19, 27);
    private static final Color FIELD = new Color(32, 30, 40);
    private static final Color WHITE = new Color(248, 246, 252);
    private static final Color MUTED = new Color(166, 158, 180);
    private static final Color PURPLE = new Color(131, 58, 180);
    private static final Color PINK = new Color(225, 48, 108);
    private static final Color ORANGE = new Color(247, 119, 55);

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> choiceBox;
    private JLabel statusLabel;
    private JButton connectButton;

    public LoginFrame() {

        setTitle("ChatSpace - Login");
        setSize(470, 570);
        setMinimumSize(new Dimension(430, 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(BLACK);

        JPanel card = new JPanel();
        card.setBackground(PANEL);
        card.setBorder(new EmptyBorder(35, 42, 35, 42));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(390, 475));

        JLabel logo = new JLabel("✦  CHATSPACE");
        logo.setForeground(WHITE);
        logo.setFont(new Font("Arial", Font.BOLD, 28));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Connect. Chat. Share.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Welcome back");
        heading.setForeground(WHITE);
        heading.setFont(new Font("Arial", Font.BOLD, 23));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        choiceBox = new JComboBox<>(
                new String[]{"LOGIN", "REGISTER"});

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        connectButton = new GradientButton("CONNECT");
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 48));

        statusLabel = new JLabel("Enter your account details");
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        styleCombo(choiceBox);
        styleField(usernameField);
        styleField(passwordField);

        card.add(logo);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(32));
        card.add(heading);
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("Account"));
        card.add(Box.createVerticalStrut(7));
        card.add(choiceBox);

        card.add(Box.createVerticalStrut(15));
        card.add(fieldLabel("Username"));
        card.add(Box.createVerticalStrut(7));
        card.add(usernameField);

        card.add(Box.createVerticalStrut(15));
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(7));
        card.add(passwordField);

        card.add(Box.createVerticalStrut(25));
        card.add(connectButton);
        card.add(Box.createVerticalStrut(17));
        card.add(statusLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 20, 20);

        background.add(card, gbc);

        setContentPane(background);

        connectButton.addActionListener(e -> connectToServer());

        passwordField.addActionListener(e -> connectToServer());

        setVisible(true);
    }

    private JLabel fieldLabel(String text) {

        JLabel label = new JLabel(text);
        label.setForeground(WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private void styleField(JTextField field) {

        field.setForeground(WHITE);
        field.setCaretColor(WHITE);
        field.setBackground(FIELD);
        field.setFont(new Font("Arial", Font.PLAIN, 14));

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(75, 62, 91)),
                new EmptyBorder(12, 13, 12, 13)));

        field.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45));

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleCombo(JComboBox<String> combo) {

        combo.setForeground(WHITE);
        combo.setBackground(FIELD);
        combo.setFont(new Font("Arial", Font.PLAIN, 13));

        combo.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40));

        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void connectToServer() {

        final String username = usernameField.getText().trim();

        final String password =
                new String(passwordField.getPassword());

        final String choice =
                (String) choiceBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {

            statusLabel.setText("Enter username and password.");
            statusLabel.setForeground(
                    new Color(255, 110, 130));

            return;
        }

        connectButton.setEnabled(false);

        statusLabel.setText("Connecting to server...");
        statusLabel.setForeground(MUTED);

        Thread connectionThread = new Thread(() -> {

            Socket socket = null;

            try {

                socket = new Socket("localhost", 6000);

                BufferedReader input = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));

                PrintWriter output = new PrintWriter(
                        socket.getOutputStream(), true);

                // Read server prompts and send credentials
                input.readLine();

                output.println(choice);

                input.readLine();

                output.println(username);

                input.readLine();

                output.println(password);

                String response;
                String lastResponse = "";

                while ((response = input.readLine()) != null) {

                    lastResponse = response;

                    if (response.startsWith("Welcome!")) {

                        final Socket connectedSocket = socket;

                        SwingUtilities.invokeLater(() -> {

                            new ChatFrame(
                                    connectedSocket,
                                    input,
                                    output,
                                    username);

                            dispose();
                        });

                        return;
                    }

                    if (response.startsWith("Invalid")
                            || response.startsWith("Registration failed")
                            || response.startsWith(
                                    "This user is already online")) {

                        break;
                    }
                }

                if (socket != null) {
                    socket.close();
                }

                final String result = lastResponse;

                SwingUtilities.invokeLater(() -> {

                    statusLabel.setText(
                            result.isEmpty()
                                    ? "Connection closed."
                                    : result);

                    statusLabel.setForeground(
                            new Color(255, 110, 130));

                    connectButton.setEnabled(true);
                });

            } catch (IOException ex) {

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }

                SwingUtilities.invokeLater(() -> {

                    statusLabel.setText(
                            "Server connection failed. Is ChatServer running?");

                    statusLabel.setForeground(
                            new Color(255, 110, 130));

                    connectButton.setEnabled(true);
                });
            }

        }, "login-connection-thread");

        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(LoginFrame::new);
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
                    0, 0,
                    getWidth(), getHeight(),
                    18, 18);

            g2.dispose();

            super.paintComponent(g);
        }
    }
}