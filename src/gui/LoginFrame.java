
package gui;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> choiceBox;
    private JLabel statusLabel;

    public LoginFrame() {

        setTitle("Java Chat Application - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        choiceBox = new JComboBox<>(new String[]{"LOGIN", "REGISTER"});
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JButton loginButton = new JButton("Connect");
        statusLabel = new JLabel("Enter your details");

        panel.add(new JLabel("Choose:"));
        panel.add(choiceBox);

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        panel.add(new JLabel(""));
        panel.add(loginButton);

        panel.add(new JLabel("Status:"));
        panel.add(statusLabel);

        add(panel);

        loginButton.addActionListener(e -> connectToServer());

        setVisible(true);
    }

    private void connectToServer() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String choice = (String) choiceBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter username and password.");
            return;
        }

        statusLabel.setText("Connecting...");

        new Thread(() -> {

            try {

                Socket socket = new Socket("localhost", 6000);

                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                PrintWriter output = new PrintWriter(
                        socket.getOutputStream(), true);

                // Send authentication details
                output.println(choice);
                output.println(username);
                output.println(password);

                String response;
                String lastResponse = "";

                // Wait for successful login or registration
                while ((response = input.readLine()) != null) {

                    lastResponse = response;

                    if (response.startsWith("Welcome!")) {

                        SwingUtilities.invokeLater(() -> {

                            new ChatFrame(socket, input, output, username);
                            dispose();

                        });

                        return;
                    }
                }

                socket.close();

                String finalResponse = lastResponse;

                SwingUtilities.invokeLater(() ->
                        statusLabel.setText(finalResponse.isEmpty()
                                ? "Connection closed."
                                : finalResponse));

            } catch (IOException e) {

                SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Server connection failed."));
            }

        }).start();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
