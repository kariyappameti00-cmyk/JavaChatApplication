
package server;

import java.net.*;
import java.io.*;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    public static final int PORT = 6000;

    public static ConcurrentHashMap<String, ClientHandler> onlineUsers =
            new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Set<ClientHandler>> groups =
            new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, String> groupAdmins =
            new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Set<String>> pendingRequests =
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
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            ServerLogger.log("Server error: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler user : onlineUsers.values()) {
            user.sendMessage(message);
        }
    }

    // Create group and assign creator as admin
    public static boolean createGroup(
            String groupName, ClientHandler user) {

        if (groupName == null || groupName.trim().isEmpty()) {
            return false;
        }

        Set<ClientHandler> members = ConcurrentHashMap.newKeySet();
        members.add(user);

        if (groups.putIfAbsent(groupName, members) != null) {
            return false;
        }

        groupAdmins.put(groupName, user.getUsername());
        pendingRequests.put(groupName, ConcurrentHashMap.newKeySet());

        ServerLogger.log(user.getUsername() + " created group " + groupName);

        return true;
    }

    public static boolean groupExists(String groupName) {
        return groups.containsKey(groupName);
    }

    public static boolean isAdmin(
            String groupName, ClientHandler user) {

        String admin = groupAdmins.get(groupName);

        return admin != null && admin.equals(user.getUsername());
    }

    public static boolean isMember(
            String groupName, ClientHandler user) {

        Set<ClientHandler> members = groups.get(groupName);

        return members != null && members.contains(user);
    }

    // Request permission to join
    public static String requestToJoin(
            String groupName, ClientHandler user) {

        Set<ClientHandler> members = groups.get(groupName);

        if (members == null) {
            return "Group not found.";
        }

        if (members.contains(user)) {
            return "You are already a member of this group.";
        }

        Set<String> requests = pendingRequests.get(groupName);

        if (requests == null) {
            return "Request system unavailable.";
        }

        if (!requests.add(user.getUsername())) {
            return "You have already requested to join this group.";
        }

        String adminName = groupAdmins.get(groupName);
        ClientHandler admin = onlineUsers.get(adminName);

        if (admin != null) {
            admin.sendMessage(
                    "[Group Request] " + user.getUsername()
                    + " wants to join " + groupName
            );
        }

        ServerLogger.log(
                user.getUsername() + " requested to join " + groupName
        );

        return "Join request sent to group admin.";
    }

    // View pending requests
    public static List<String> getPendingRequests(
            String groupName, ClientHandler user) {

        if (!isAdmin(groupName, user)) {
            return null;
        }

        Set<String> requests = pendingRequests.get(groupName);

        if (requests == null) {
            return null;
        }

        List<String> result = new ArrayList<>(requests);
        Collections.sort(result);

        return result;
    }

    // Approve request
    public static boolean approveJoin(
            String groupName,
            String requestedUsername,
            ClientHandler admin) {

        if (!isAdmin(groupName, admin)) {
            return false;
        }

        Set<String> requests = pendingRequests.get(groupName);
        Set<ClientHandler> members = groups.get(groupName);

        if (requests == null || members == null ||
                !requests.contains(requestedUsername)) {
            return false;
        }

        ClientHandler requestedUser = onlineUsers.get(requestedUsername);

        if (requestedUser == null) {
            return false;
        }

        members.add(requestedUser);
        requests.remove(requestedUsername);

        requestedUser.sendMessage(
                "Your request to join " + groupName + " was approved!"
        );

        admin.sendMessage(
                requestedUsername + " has joined " + groupName + "."
        );

        ServerLogger.log(
                admin.getUsername() + " approved " + requestedUsername
                + " for group " + groupName
        );

        return true;
    }

    // Reject request
    public static boolean rejectJoin(
            String groupName,
            String requestedUsername,
            ClientHandler admin) {

        if (!isAdmin(groupName, admin)) {
            return false;
        }

        Set<String> requests = pendingRequests.get(groupName);

        if (requests == null || !requests.remove(requestedUsername)) {
            return false;
        }

        ClientHandler requestedUser = onlineUsers.get(requestedUsername);

        if (requestedUser != null) {
            requestedUser.sendMessage(
                    "Your request to join " + groupName + " was rejected."
            );
        }

        admin.sendMessage(
                "Rejected " + requestedUsername + "'s request."
        );

        ServerLogger.log(
                admin.getUsername() + " rejected " + requestedUsername
                + " for group " + groupName
        );

        return true;
    }

    public static void sendGroupMessage(
            String groupName, String message) {

        Set<ClientHandler> members = groups.get(groupName);

        if (members != null) {
            for (ClientHandler member : members) {
                member.sendMessage(message);
            }
        }
    }

    public static void removeUserFromGroups(ClientHandler user) {

        for (Set<ClientHandler> members : groups.values()) {
            members.remove(user);
        }

        for (Set<String> requests : pendingRequests.values()) {
            requests.remove(user.getUsername());
        }
    }
}
