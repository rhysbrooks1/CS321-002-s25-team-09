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

    private final Scanner scanner;
    private final String type;
    private String nextKey;

    public SSHFileReader(String filePath, String type) throws FileNotFoundException {
        this.scanner = new Scanner(new File(filePath));
        this.type = type;
        advance(); // Load the first key
    }

    private void advance() {
        nextKey = null;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(" ");
            if (parts.length < 3) continue;

            String timestamp = parts[1];
            String action = parts[2];

            switch (type) {
                case "accepted-ip":
                    if (action.equals("Accepted") && parts.length >= 5) {
                        nextKey = "Accepted-" + parts[4];
                    }
                    break;

                case "accepted-time":
                    if (action.equals("Accepted")) {
                        nextKey = "Accepted-" + timestamp;
                    }
                    break;

                case "failed-ip":
                    if (action.equals("Failed") && parts.length >= 5) {
                        nextKey = "Failed-" + parts[4];
                    }
                    break;

                case "failed-time":
                    if (action.equals("Failed")) {
                        nextKey = "Failed-" + timestamp;
                    }
                    break;

                case "invalid-ip":
                    if (action.equals("Invalid") && parts.length >= 5) {
                        nextKey = "Invalid-" + parts[4];
                    }
                    break;

                case "invalid-time":
                    if (action.equals("Invalid")) {
                        nextKey = "Invalid-" + timestamp;
                    }
                    break;

                case "reverseaddress-ip":
                    if ((action.equals("reverse") || action.equals("Address")) && parts.length >= 4) {
                        nextKey = action + "-" + parts[3];
                    }
                    break;

                case "reverseaddress-time":
                    if (action.equals("reverse") || action.equals("Address")) {
                        nextKey = action + "-" + timestamp;
                    }
                    break;

                case "user-ip":
                    if (!action.equals("reverse") && !action.equals("Address") && parts.length >= 5) {
                        nextKey = parts[3] + "-" + parts[4];
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown BTree type: " + type);
            }

            if (nextKey != null) return; // Found a valid key, stop scanning
        }
    }

    public boolean hasNextKey() {
        return nextKey != null;
    }

    public String nextKey() {
        if (nextKey == null) {
            throw new NoSuchElementException("No more keys in file.");
        }
        String current = nextKey;
        advance(); // prepare the next one
        return current;
    }

    public void close() {
        scanner.close();
    }
}
