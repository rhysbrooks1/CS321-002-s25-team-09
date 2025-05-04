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
    private static final Pattern USER_PATTERN = Pattern.compile("user\\s+([^\\s]+)");
    private static final Pattern PORT_PATTERN = Pattern.compile("port\\s+(\\d+)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\w{3}\\s+\\d+\\s+\\d+:\\d+:\\d+)");
    
    private BufferedReader reader;
    private String nextKey;
    private String treeType;
    
    /**
     * Constructor opens the file and prepares for reading.
     * 
     * @param filename SSH log file path
     * @param treeType type of key to extract
     * @throws IOException if file cannot be read
     */
    public SSHFileReader(String filename, String treeType) throws IOException {
        this.reader = new BufferedReader(new FileReader(filename));
        this.treeType = treeType;
        readNextKey();
    }
    
    /**
     * Checks if there are more keys to read.
     * 
     * @return true if more keys are available
     */
    public boolean hasNextKey() {
        return nextKey != null;
    }
    
    /**
     * Returns the next key and advances to the next one.
     * 
     * @return the next key
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
     * 
     * @throws IOException if an error occurs while closing
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
    
    /**
     * Reads the next valid key from the file based on the tree type.
     * 
     * @throws IOException if an error occurs while reading
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
     * 
     * @param logLine line from SSH log
     * @param type key type to extract
     * @return extracted key or null if no match
     */
    private String extractKey(String logLine, String type) {
        switch (type) {
            case "accepted-ip":
                // Extract IPs from accepted connections
                if (logLine.contains("Accepted")) {
                    return extractIP(logLine);
                }
                return null;
                
            case "accepted-time":
                // Extract timestamps from accepted connections
                if (logLine.contains("Accepted")) {
                    return extractTime(logLine);
                }
                return null;
                
            case "invalid-ip":
                // Extract IPs from invalid user lines
                if (logLine.contains("Invalid user")) {
                    return extractIP(logLine);
                }
                return null;
                
            case "invalid-time":
                // Extract timestamps from invalid user lines
                if (logLine.contains("Invalid user")) {
                    return extractTime(logLine);
                }
                return null;
                
            case "failed-ip":
                // Extract IPs from failed password lines
                if (logLine.contains("Failed password")) {
                    return extractIP(logLine);
                }
                return null;
                
            case "failed-time":
                // Extract timestamps from failed password lines
                if (logLine.contains("Failed password")) {
                    return extractTime(logLine);
                }
                return null;
                
            case "reverseaddress-ip":
                // Extract reversed IP addresses
                String ip = extractIP(logLine);
                if (ip != null) {
                    // Reverse the IP address (e.g., 192.168.1.1 -> 1.1.168.192)
                    String[] ipParts = ip.split("\\.");
                    if (ipParts.length == 4) {
                        return ipParts[3] + "." + ipParts[2] + "." + 
                               ipParts[1] + "." + ipParts[0];
                    }
                }
                return null;
                
            case "reverseaddress-time":
                // Extract reverse address (reversed IP) and time
                String ip2 = extractIP(logLine);
                String time = extractTime(logLine);
                
                if (ip2 != null && time != null) {
                    // Reverse the IP address
                    String[] ipParts = ip2.split("\\.");
                    if (ipParts.length == 4) {
                        String reversedIP = ipParts[3] + "." + ipParts[2] + "." + 
                                          ipParts[1] + "." + ipParts[0];
                        return reversedIP + "-" + time;
                    }
                }
                return null;
                
            case "user-ip":
                // Extract username and IP pairs
                String user = extractUser(logLine);
                String ip3 = extractIP(logLine);
                
                if (user != null && ip3 != null) {
                    return user + "-" + ip3;
                }
                return null;
                
            default:
                // Unknown type, return null
                System.err.println("Warning: Unknown tree type: " + type);
                return null;
        }
    }
    
    /**
     * Extracts IP address from log line.
     */
    private String extractIP(String logLine) {
        Matcher matcher = IP_PATTERN.matcher(logLine);
        return matcher.find() ? matcher.group() : null;
    }
    
    /**
     * Extracts username from log line.
     */
    private String extractUser(String logLine) {
        Matcher matcher = USER_PATTERN.matcher(logLine);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    /**
     * Extracts port number from log line.
     */
    private String extractPort(String logLine) {
        Matcher matcher = PORT_PATTERN.matcher(logLine);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    /**
     * Extracts timestamp from log line.
     */
    private String extractTime(String logLine) {
        Matcher matcher = TIME_PATTERN.matcher(logLine);
        return matcher.find() ? matcher.group(1) : null;
    }
}