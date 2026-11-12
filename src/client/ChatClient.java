
package client;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) {

        try {
            // Connect to the chat server
            Socket socket = new Socket("localhost", 6000);

            System.out.println("Connected to server!");

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true);

            // Thread to receive messages
            Thread receiver = new Thread(() -> {
                try {
                    String message;

                    while ((message = input.readLine()) != null) {
                        System.out.println(message);
                    }

                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            });

            receiver.start();

            // Read messages from keyboard
            Scanner scanner = new Scanner(System.in);

            while (scanner.hasNextLine()) {

                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("/logout")) {
                    break;
                }

                output.println(message);
            }

            socket.close();
            scanner.close();

        } catch (IOException e) {
            System.out.println(
                    "Unable to connect: " + e.getMessage());
        }
    }
}
