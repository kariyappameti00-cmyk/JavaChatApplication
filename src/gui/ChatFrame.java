
package gui;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class ChatFrame extends JFrame {

    private JTextArea chatArea;
    private JTextField messageField;
    private JLabel statusLabel;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private String username;
    private boolean loggedOut = false;

    public ChatFrame(
            Socket socket,
            BufferedReader input,
            PrintWriter output,
            String username) {

        this.socket = socket;
        this.input = input;
        this.output = output;
        this.username = username;

        setTitle("Java Chat Application - " + username);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Chat display area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Message input
        messageField = new JTextField();

        JButton sendButton = new JButton("Send");
        JButton clearButton = new JButton("Clear Chat");
        JButton logoutButton = new JButton("Logout");

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());

        statusLabel = new JLabel("  Connected as: " + username);
        statusLabel.setForeground(new Color(0, 128, 0));

        topPanel.add(statusLabel, BorderLayout.WEST);

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topButtons.add(clearButton);
        topButtons.add(logoutButton);

        topPanel.add(topButtons, BorderLayout.EAST);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Button actions
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        clearButton.addActionListener(e -> chatArea.setText(""));

        logoutButton.addActionListener(e -> logout());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                logout();
            }
        });

        setVisible(true);

        chatArea.append("Welcome, " + username + "!\n");
        chatArea.append("You are connected to the chat server.\n\n");

        receiveMessages();
    }

    // Send message to server
    private void sendMessage() {

        String message = messageField.getText().trim();

        if (!message.isEmpty() && !loggedOut) {

            output.println(message);

            messageField.setText("");
        }
    }

    // Receive messages from server
    private void receiveMessages() {

        Thread receiver = new Thread(() -> {

            try {

                String message;

                while ((message = input.readLine()) != null) {

                    String receivedMessage = message;

                    SwingUtilities.invokeLater(() -> {

                        chatArea.append(receivedMessage + "\n");

                        chatArea.setCaretPosition(
                                chatArea.getDocument().getLength());
                    });
                }

            } catch (IOException e) {

                SwingUtilities.invokeLater(() -> {

                    if (!loggedOut) {
                        statusLabel.setText("Disconnected");
                        statusLabel.setForeground(Color.RED);
                        chatArea.append("Connection closed.\n");
                    }
                });
            }

        });

        receiver.setDaemon(true);
        receiver.start();
    }

    // Logout
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
