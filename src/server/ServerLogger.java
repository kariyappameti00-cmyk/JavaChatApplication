
package server;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static final String FILE = "logs/server.log";

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static synchronized void log(String message) {

        String time = LocalDateTime.now().format(FORMAT);

        String entry = "[" + time + "] " + message;

        try {
            Files.createDirectories(Paths.get("logs"));

            Files.writeString(
                    Paths.get(FILE),
                    entry + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println(entry);

        } catch (IOException e) {
            System.out.println("Logging error: " + e.getMessage());
        }
    }
}
