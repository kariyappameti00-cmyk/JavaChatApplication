
package server;

import java.net.*;
import java.io.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    public static final int PORT = 6000;

    // Store online users
    public static ConcurrentHashMap<String, ClientHandler> onlineUsers =
            new ConcurrentHashMap<>();

    // Store groups and group members
    public static ConcurrentHashMap<String, Set<ClientHandler>> groups =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        System.out.println("Starting Chat Server...");
        ServerLogger.log("Chat server is starting.");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Server started on port " + PORT);
            ServerLogger.log("Server started on port " + PORT);

            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("New client connected!");
                ServerLogger.log("New client connected.");

                ClientHandler handler = new ClientHandler(socket);

                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (IOException e) {

            System.out.println("Server error: " + e.getMessage());
            ServerLogger.log("Server error: " + e.getMessage());
        }
    }

    // Broadcast message to all online users
    public static void broadcast(String message) {

        for (ClientHandler user : onlineUsers.values()) {
            user.sendMessage(message);
        }
    }

    // Create group
    public static boolean createGroup(
            String groupName, ClientHandler user) {

        Set<ClientHandler> members =
                ConcurrentHashMap.newKeySet();

        members.add(user);

        return groups.putIfAbsent(groupName, members) == null;
    }

    // Join group
    public static boolean joinGroup(
            String groupName, ClientHandler user) {

        Set<ClientHandler> members = groups.get(groupName);

        if (members == null) {
            return false;
        }

        members.add(user);
        return true;
    }

    // Send message to group members
    public static void sendGroupMessage(
            String groupName, String message) {

        Set<ClientHandler> members = groups.get(groupName);

        if (members != null) {

            for (ClientHandler member : members) {
                member.sendMessage(message);
            }
        }
    }

    // Remove disconnected user from all groups
    public static void removeUserFromGroups(ClientHandler user) {

        for (Set<ClientHandler> members : groups.values()) {
            members.remove(user);
        }
    }
}
