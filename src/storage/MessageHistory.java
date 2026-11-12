
package storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageHistory {

    private static final Path FILE =
            Paths.get("data", "messages.txt");

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Add date and time to the message
    public static String format(
            String username, String type, String message) {

        String time = LocalDateTime.now().format(FORMAT);

        return "[" + time + "] [" + type + "] "
                + username + ": " + message;
    }

    // Save message in the history file
    public static synchronized void save(String message) {

        try {
            Files.createDirectories(FILE.getParent());

            Files.writeString(
                    FILE,
                    message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            System.out.println(
                    "Message history error: " + e.getMessage()
            );
        }
    }
}
