package server;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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

    public String getUsername() {
        return username;
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

            boolean authenticated;

            if (choice.equalsIgnoreCase("REGISTER")) {

                authenticated = AuthService.register(username, password);

                if (authenticated) {
                    sendMessage("Registration successful!");
                } else {
                    sendMessage("Registration failed. Username may already exist.");
                    return;
                }

            } else if (choice.equalsIgnoreCase("LOGIN")) {

                authenticated = AuthService.login(username, password);

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

            while ((message = input.readLine()) != null) {

                if (message.equalsIgnoreCase("/logout")) {
                    break;
                }

                // Retrieve all existing groups
                if (message.equalsIgnoreCase("/groups")) {
                    sendGroupList();
                    continue;
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
                                privateText);

                        MessageHistory.save(formatted);

                        receiver.sendMessage("[Private] " + formatted);

                        sendMessage("[Private confirmation] " + formatted);

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

                        sendMessage(
                                "Group created: " + groupName
                                        + ". You are the group admin.");

                        sendGroupList();
                        ChatServer.broadcast("[GROUPS_CHANGED]");

                    } else {
                        sendMessage("Group already exists.");
                    }

                    continue;
                }

                // REQUEST TO JOIN GROUP
                // Base64 encoding supports group names containing spaces.
                if (message.startsWith("/request64 ")) {

                    String encodedName = message.substring(11).trim();

                    try {
                        String groupName = decode(encodedName);

                        sendMessage(
                                ChatServer.requestToJoin(groupName, this));

                    } catch (IllegalArgumentException e) {
                        sendMessage("Invalid group name.");
                    }

                    continue;
                }

                // REQUEST TO JOIN GROUP - OLD COMMAND
                if (message.startsWith("/request ")) {

                    String groupName = message.substring(9).trim();

                    if (groupName.isEmpty()) {
                        sendMessage("Usage: /request groupName");
                    } else {
                        sendMessage(
                                ChatServer.requestToJoin(groupName, this));
                    }

                    continue;
                }

                // VIEW PENDING REQUESTS
                if (message.startsWith("/requests ")) {

                    String groupName = message.substring(10).trim();

                    if (groupName.isEmpty()) {
                        sendMessage("Usage: /requests groupName");
                        continue;
                    }

                    if (!ChatServer.groupExists(groupName)) {
                        sendMessage("Group not found.");
                        continue;
                    }

                    List<String> requests =
                            ChatServer.getPendingRequests(groupName, this);

                    if (requests == null) {
                        sendMessage("Only the group admin can view requests.");

                    } else if (requests.isEmpty()) {
                        sendMessage("No pending requests for " + groupName);

                    } else {
                        sendMessage("Pending requests for " + groupName + ":");

                        for (String requestedUser : requests) {
                            sendMessage("- " + requestedUser);
                        }
                    }

                    continue;
                }

                // APPROVE JOIN REQUEST
                if (message.startsWith("/approve ")) {

                    String[] parts = message.split("\\s+", 3);

                    if (parts.length != 3) {
                        sendMessage("Usage: /approve groupName username");
                    } else if (!ChatServer.approveJoin(
                            parts[1], parts[2], this)) {

                        sendMessage(
                                "Could not approve request. Check group, "
                                        + "username and admin permission.");
                    } else {
                        ChatServer.broadcast("[GROUPS_CHANGED]");
                    }

                    continue;
                }

                // REJECT JOIN REQUEST
                if (message.startsWith("/reject ")) {

                    String[] parts = message.split("\\s+", 3);

                    if (parts.length != 3) {
                        sendMessage("Usage: /reject groupName username");

                    } else if (!ChatServer.rejectJoin(
                            parts[1], parts[2], this)) {

                        sendMessage(
                                "Could not reject request. Check group, "
                                        + "username and admin permission.");
                    }

                    continue;
                }

                // DIRECT JOINING IS NOT ALLOWED
                if (message.startsWith("/join ")) {

                    sendMessage(
                            "Direct joining is disabled. Use a join request.");

                    continue;
                }

                // GROUP MESSAGE WITH ENCODED GROUP NAME
                if (message.startsWith("/group64 ")) {

                    String[] parts = message.split("\\s+", 3);

                    if (parts.length < 3) {
                        sendMessage("Invalid group message.");
                        continue;
                    }

                    try {
                        String groupName = decode(parts[1]);
                        String groupText = decode(parts[2]);

                        if (!ChatServer.groupExists(groupName)) {
                            sendMessage("Group not found.");

                        } else if (!ChatServer.isMember(groupName, this)) {
                            sendMessage(
                                    "You are not an approved member of this group.");

                        } else {

                            String formatted = MessageHistory.format(
                                    username,
                                    "Group: " + groupName,
                                    groupText);

                            MessageHistory.save(formatted);

                            ChatServer.sendGroupMessage(
                                    groupName, formatted);

                            ServerLogger.log(
                                    username + " sent a message to group "
                                            + groupName);
                        }

                    } catch (IllegalArgumentException e) {
                        sendMessage("Invalid group message.");
                    }

                    continue;
                }

                // OLD GROUP MESSAGE COMMAND
                if (message.startsWith("/group ")) {

                    String[] parts = message.split("\\s+", 3);

                    if (parts.length < 3) {
                        sendMessage("Usage: /group groupName message");
                        continue;
                    }

                    String groupName = parts[1];
                    String groupText = parts[2];

                    if (!ChatServer.groupExists(groupName)) {
                        sendMessage("Group not found.");

                    } else if (!ChatServer.isMember(groupName, this)) {
                        sendMessage(
                                "You are not an approved member of this group.");

                    } else {

                        String formatted = MessageHistory.format(
                                username,
                                "Group: " + groupName,
                                groupText);

                        MessageHistory.save(formatted);

                        ChatServer.sendGroupMessage(
                                groupName, formatted);
                    }

                    continue;
                }

                // PUBLIC MESSAGE
                String formatted = MessageHistory.format(
                        username,
                        "Public",
                        message);

                MessageHistory.save(formatted);

                ChatServer.broadcast(formatted);

                ServerLogger.log("Public message from " + username);
            }

        } catch (IOException e) {

            System.out.println("Client disconnected: " + username);

        } finally {

            if (registeredOnline) {

                ChatServer.onlineUsers.remove(username, this);

                ChatServer.removeUserFromGroups(this);

                ChatServer.broadcast(username + " left the chat.");

                ChatServer.broadcast("[GROUPS_CHANGED]");

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

    // Send all existing groups to the connected client
    private void sendGroupList() {

        sendMessage("[[GROUP_LIST_BEGIN]]");

        List<String> names =
                new ArrayList<>(ChatServer.groups.keySet());

        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);

        for (String groupName : names) {

            String admin = ChatServer.groupAdmins.getOrDefault(
                    groupName, "Unknown");

            boolean member = ChatServer.isMember(groupName, this);

            sendMessage(
                    "[[GROUP_ITEM]]"
                            + encode(groupName) + "|"
                            + encode(admin) + "|"
                            + member);
        }

        sendMessage("[[GROUP_LIST_END]]");
    }

    // Encode text safely
    private String encode(String value) {

        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    // Decode text safely
    private String decode(String value) {

        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8);
    }

    // Send messages safely
    public synchronized void sendMessage(String message) {

        if (output != null) {
            output.println(message);
        }
    }
}