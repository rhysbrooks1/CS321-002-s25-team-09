package cs321.create;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.NoSuchElementException;

/**
 * Reads the stripped (wrangled) SSH log file line-by-line and provides keys for BTree insertion.
 * Each call to nextKey() returns the next line's key formatted as: type-field (e.g., Accepted-1.2.3.4).
 */
public class SSHFileReader {

    private Scanner scanner;
    private String type; // One of the 9 types: accepted-ip, accepted-time, etc.

    /**
     * Constructs the reader for a stripped SSH log file.
     * @param filePath path to stripped log file
     * @param type BTree key type (e.g., accepted-ip, failed-time)
     * @throws FileNotFoundException if file cannot be read
     */
    public SSHFileReader(String filePath, String type) throws FileNotFoundException {
        this.scanner = new Scanner(new File(filePath));
        this.type = type;
    }

    /**
     * Reads and returns the next key from the file, formatted by the BTree type.
     * @return formatted key (e.g., Accepted-192.168.1.1)
     * @throws NoSuchElementException if no more keys exist
     */
    public String nextKey() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(" ");

            if (parts.length < 3) continue;

            String timestamp = parts[1];
            String action = parts[2];

            switch (type) {
                case "accepted-ip":
                    if (action.equals("Accepted") && parts.length >= 5) return "Accepted-" + parts[4];
                    break;

                case "accepted-time":
                    if (action.equals("Accepted")) return "Accepted-" + timestamp;
                    break;

                case "failed-ip":
                    if (action.equals("Failed") && parts.length >= 5) return "Failed-" + parts[4];
                    break;

                case "failed-time":
                    if (action.equals("Failed")) return "Failed-" + timestamp;
                    break;

                case "invalid-ip":
                    if (action.equals("Invalid") && parts.length >= 5) return "Invalid-" + parts[4];
                    break;

                case "invalid-time":
                    if (action.equals("Invalid")) return "Invalid-" + timestamp;
                    break;

                case "reverseaddress-ip":
                    if (action.equals("reverse") || action.equals("Address")) return action + "-" + parts[3];
                    break;

                case "reverseaddress-time":
                    if (action.equals("reverse") || action.equals("Address")) return action + "-" + timestamp;
                    break;

                case "user-ip":
                    if (!action.equals("reverse") && !action.equals("Address") && parts.length >= 5) {
                        return parts[3] + "-" + parts[4]; // username-IP
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown BTree type: " + type);
            }
        }

        throw new NoSuchElementException("No more keys in file.");
    }

    /**
     * Returns true if there are more lines to read.
     */
    public boolean hasNextKey() {
        return scanner.hasNextLine();
    }

    /**
     * Closes the underlying scanner.
     */
    public void close() {
        scanner.close();
    }
}