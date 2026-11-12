
package server;

import java.net.*;
import java.io.*;

import auth.AuthService;
import storage.MessageHistory;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        boolean registeredOnline = false;

        try {

            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            output = new PrintWriter(
                    socket.getOutputStream(), true);

            // Authentication
            output.println("Choose: REGISTER or LOGIN");

            String choice = input.readLine();

            output.println("Enter username:");
            username = input.readLine();

            output.println("Enter password:");
            String password = input.readLine();

            if (choice == null || username == null || password == null) {
                return;
            }

            boolean authenticated = false;

            if (choice.equalsIgnoreCase("REGISTER")) {

                authenticated =
                        AuthService.register(username, password);

                if (authenticated) {
                    sendMessage("Registration successful!");
                } else {
                    sendMessage(
                            "Registration failed. Username may already exist.");
                    return;
                }

            } else if (choice.equalsIgnoreCase("LOGIN")) {

                authenticated =
                        AuthService.login(username, password);

                if (authenticated) {
                    sendMessage("Login successful!");
                } else {
                    sendMessage("Invalid username or password.");
                    return;
                }

            } else {

                sendMessage("Invalid choice.");
                return;
            }

            // Prevent duplicate online sessions
            if (ChatServer.onlineUsers.putIfAbsent(username, this) != null) {

                sendMessage("This user is already online.");
                return;
            }

            registeredOnline = true;

            System.out.println(username + " joined the chat.");
            ServerLogger.log(username + " logged in.");

            ChatServer.broadcast(username + " joined the chat!");

            sendMessage("Welcome! Type /logout to exit.");

            String message;

            // Main message processing loop
            while ((message = input.readLine()) != null) {

                if (message.equalsIgnoreCase("/logout")) {
                    break;
                }

                // PRIVATE MESSAGE
                if (message.startsWith("/msg ")) {

                    String[] parts = message.split("\\s+", 3);

                    if (parts.length < 3) {
                        sendMessage("Usage: /msg username message");
                        continue;
                    }

                    String receiverName = parts[1];
                    String privateText = parts[2];

                    ClientHandler receiver =
                            ChatServer.onlineUsers.get(receiverName);

                    if (receiver == null) {

                        sendMessage("User is not online.");

                    } else {

                        String formatted = MessageHistory.format(
                                username,
                                "Private to " + receiverName,
                                privateText
                        );

                        MessageHistory.save(formatted);

                        receiver.sendMessage(
                                "[Private] " + formatted
                        );

                        sendMessage(
                                "[Private confirmation] " + formatted
                        );

                        System.out.println(formatted);

                        ServerLogger.log(
                                username + " sent a private message to "
                                        + receiverName);
                    }

                    continue;
                }

                // CREATE GROUP
                if (message.startsWith("/create ")) {

                    String groupName = message.substring(8).trim();

                    if (groupName.isEmpty()) {

                        sendMessage("Enter a group name.");

                    } else if (ChatServer.createGroup(groupName, this)) {

                        sendMessage("Group created: " + groupName);

                    } else {

                        sendMessage("Group already exists.");
                    }

                    continue;
                }

                // JOIN GROUP
                if (message.startsWith("/join ")) {

                    String groupName = message.substring(6).trim();

                    if (ChatServer.joinGroup(groupName, this)) {

                        sendMessage("Joined group: " + groupName);

                    } else {

                        sendMessage("Group not found.");
                    }

                    continue;
                }

                // GROUP MESSAGE
                if (message.startsWith("/group ")) {

                    String[] parts = message.split("\\s+", 3);

                    if (parts.length < 3) {

                        sendMessage("Usage: /group groupName message");
                        continue;
                    }

                    String groupName = parts[1];
                    String groupText = parts[2];

                    if (!ChatServer.groups.containsKey(groupName)) {

                        sendMessage("Group not found.");

                    } else if (!ChatServer.groups.get(groupName).contains(this)) {

                        sendMessage("Join the group first.");

                    } else {

                        String formatted = MessageHistory.format(
                                username,
                                "Group: " + groupName,
                                groupText
                        );

                        MessageHistory.save(formatted);

                        ChatServer.sendGroupMessage(
                                groupName,
                                formatted
                        );

                        ServerLogger.log(
                                username + " sent a message to group "
                                        + groupName);
                    }

                    continue;
                }

                // PUBLIC MESSAGE
                String formatted = MessageHistory.format(
                        username,
                        "Public",
                        message
                );

                MessageHistory.save(formatted);

                System.out.println(formatted);

                ChatServer.broadcast(formatted);

                ServerLogger.log(
                        "Public message from " + username);
            }

        } catch (IOException e) {

            System.out.println("Client disconnected: " + username);

        } finally {

            if (registeredOnline) {

                ChatServer.onlineUsers.remove(username, this);

                ChatServer.removeUserFromGroups(this);

                ChatServer.broadcast(username + " left the chat.");

                System.out.println(username + " disconnected.");

                ServerLogger.log(username + " logged out.");
            }

            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Socket closing error.");
            }
        }
    }

    // Send message safely to client
    public synchronized void sendMessage(String message) {

        if (output != null) {
            output.println(message);
        }
    }
}
