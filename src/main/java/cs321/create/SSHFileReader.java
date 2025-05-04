package cs321.create;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for reading SSH log files and extracting keys based on specified tree type.
 */
public class SSHFileReader {
    
    // Regular expression patterns for extracting information
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+/\\d+\\s+(\\d+:\\d+:\\d+))");
    
    private BufferedReader reader;
    private String nextKey;
    private String treeType;
    
    /**
     * Constructor opens the file and prepares for reading.
     */
    public SSHFileReader(String filename, String treeType) throws IOException {
        this.reader = new BufferedReader(new FileReader(filename));
        this.treeType = treeType;
        readNextKey();
    }
    
    /**
     * Checks if there are more keys to read.
     */
    public boolean hasNextKey() {
        return nextKey != null;
    }
    
    /**
     * Returns the next key and advances to the next one.
     */
    public String nextKey() {
        String current = nextKey;
        try {
            readNextKey();
        } catch (IOException e) {
            nextKey = null;
        }
        return current;
    }
    
    /**
     * Closes the reader.
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
    
    /**
     * Reads the next valid key from the file based on the tree type.
     */
    private void readNextKey() throws IOException {
        String line;
        nextKey = null;
        
        while (nextKey == null && (line = reader.readLine()) != null) {
            nextKey = extractKey(line, treeType);
        }
    }
    
    /**
     * Extracts the appropriate key from a log line based on the tree type.
     */
    private String extractKey(String logLine, String type) {
        String[] parts = logLine.split("\\s+");
        if (parts.length < 4) return null;
        
        // Extract time in HH:MM format
        Matcher timeMatcher = TIME_PATTERN.matcher(logLine);
        String timeOnly = null;
        if (timeMatcher.find() && timeMatcher.group(2) != null) {
            String fullTime = timeMatcher.group(2);
            if (fullTime.length() >= 5) {
                timeOnly = fullTime.substring(0, 5); // Extract HH:MM part
            }
        }
        
        String ip = extractIP(logLine);
        String event = parts[2];
        String username = parts.length > 3 ? parts[3] : "";
        
        switch (type) {
            case "accepted-ip":
                if (event.equals("Accepted") && ip != null) {
                    return ip; // Plain IP for accepted-ip
                }
                break;
                
            case "accepted-time":
                if (event.equals("Accepted") && timeOnly != null) {
                    return "Accepted-" + timeOnly; // Format as "Accepted-HH:MM"
                }
                break;
                
            case "invalid-ip":
                if ((event.equals("Invalid") || logLine.contains("Invalid user")) && ip != null) {
                    return "Invalid-" + ip; // Format as "Invalid-IP"
                }
                break;
                
            case "invalid-time":
                if ((event.equals("Invalid") || logLine.contains("Invalid user")) && timeOnly != null) {
                    return "Invalid-" + timeOnly; // Format as "Invalid-HH:MM"
                }
                break;
                
            case "failed-ip":
                if ((event.equals("Failed") || logLine.contains("Failed password")) && ip != null) {
                    return "*****-" + ip; // Format as "*****-IP" as seen in examples
                }
                break;
                
            case "failed-time":
                if ((event.equals("Failed") || logLine.contains("Failed password")) && timeOnly != null) {
                    return "Failed-" + timeOnly; // Format as "Failed-HH:MM"
                }
                break;
                
            case "reverseaddress-ip":
                if (ip != null) {
                    String[] ipParts = ip.split("\\.");
                    if (ipParts.length == 4) {
                        // Format as ".243.22-IP" as seen in examples
                        return "." + ipParts[2] + "." + ipParts[3] + "-" + ip;
                    }
                }
                break;
                
            case "reverseaddress-time":
                if (ip != null && timeOnly != null) {
                    return "Address-" + timeOnly; // Format as "Address-HH:MM"
                }
                break;
                
            case "user-ip":
                if (event.equals("Accepted") && ip != null && !username.isEmpty()) {
                    return username + "-" + ip; // Format as "username-IP"
                }
                break;
        }
        
        return null;
    }
    
    /**
     * Extracts IP address from log line.
     */
    private String extractIP(String logLine) {
        Matcher matcher = IP_PATTERN.matcher(logLine);
        return matcher.find() ? matcher.group() : null;
    }
}