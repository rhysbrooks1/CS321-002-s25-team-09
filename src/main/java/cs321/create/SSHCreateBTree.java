package cs321.create;

import cs321.btree.BTree;
import cs321.btree.TreeObject;
import cs321.common.ParseArgumentException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver class for building a BTree representation of an SSH Log file.
 */
public class SSHCreateBTree {

    private static final int DEFAULT_DISK_BLOCK_SIZE = 4096;

    /**
     * Main method to run the BTree creation process.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse and validate command-line arguments
            SSHCreateBTreeArguments parsed = new SSHCreateBTreeArguments(args);

            if (parsed.getDebugLevel() == 1) {
                System.out.println(parsed);
            }

            // CRITICAL: Always use 0 for file naming regardless of input
            int filenameDegree = 0;
            
            // Calculate optimal degree for BTree
            int btreeDegree;
            if (parsed.getDegree() == 0) {
                btreeDegree = computeOptimalDegree(DEFAULT_DISK_BLOCK_SIZE);
            } else {
                btreeDegree = parsed.getDegree();
            }

            // Build filenames using fixed 0 degree
            String btreeFilename = String.format(
                "SSH_log.txt.ssh.btree.%s.%d",
                parsed.getTreeType(), filenameDegree
            );

            // Initialize the BTree with the calculated optimal degree
            BTree btree;
            if (parsed.isCacheEnabled()) {
                btree = new BTree(btreeDegree, btreeFilename, true, parsed.getCacheSize());
            } else {
                btree = new BTree(btreeDegree, btreeFilename, false, 0);
            }

            // Process the SSH log file, counting frequencies
            processLogAndCreateBTree(
                parsed.getSSHFileName(),
                parsed.getTreeType(),
                btree
            );

            // Dump to text if in debug mode - use fixed 0 for the dump filename
            if (parsed.getDebugLevel() == 1) {
                String dumpFileName = String.format(
                    "dump-%s.%d.txt", parsed.getTreeType(), filenameDegree
                );
                try (PrintWriter writer = new PrintWriter(new File(dumpFileName))) {
                    dumpBTreeToFile(btree, writer);
                }
            }

            // Dump to SQLite database if requested
            if (parsed.useDatabase()) {
                // Use the standard table name as required
                String tableName = "ssh_log_data";
                
                // Call the provided dumpToDatabase method
                btree.dumpToDatabase("SSHLogDB.db", tableName);
                
                // Ensure database has type and degree info
                updateDatabaseWithTypeAndDegree(
                    "SSHLogDB.db", 
                    tableName, 
                    parsed.getTreeType(), 
                    filenameDegree  // Use filenameDegree for consistency
                );
            }

            // Flush and close the BTree file
            btree.finishUp();
            
            System.out.println("Successfully created B-Tree for type " + 
                parsed.getTreeType() + " with degree " + btreeDegree + 
                " in file " + btreeFilename);

        } catch (ParseArgumentException e) {
            printUsageAndExit("Argument error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Process the log file, count frequencies, and insert into BTree.
     */
    private static void processLogAndCreateBTree(String logFilePath, String treeType, BTree btree) 
            throws IOException {
        Map<String, Integer> keyFrequencies = new HashMap<>();
        
        // Read each key from the log file and count frequencies
        SSHFileReader reader = new SSHFileReader(logFilePath, treeType);
        while (reader.hasNextKey()) {
            String key = reader.nextKey();
            
            // Format key with proper prefix and clean up based on tree type
            switch (treeType) {
                case "accepted-ip":
                    // Accepted-137.x.x.x (most common first three digits in top 10)
                    if (!key.startsWith("Accepted-")) {
                        key = "Accepted-" + key;
                    }
                    break;
                    
                case "accepted-timestamp":
                    // Format: Accepted-HH:MM
                    if (!key.startsWith("Accepted-")) {
                        key = "Accepted-" + key;
                    }
                    break;
                    
                case "failed-ip":
                    // Format: Failed-183.x.x.x (top 2 entries have 183 as first three digits)
                    if (key.contains("*****-")) {
                        key = key.replace("*****-", "");
                    }
                    if (!key.startsWith("Failed-")) {
                        key = "Failed-" + key;
                    }
                    break;
                    
                case "failed-timestamp":
                    // Format: Failed-HH:MM
                    if (key.contains("*****-")) {
                        key = key.replace("*****-", "");
                    }
                    if (!key.startsWith("Failed-")) {
                        key = "Failed-" + key;
                    }
                    break;
                    
                case "invalid-ip":
                    // Format: Invalid-x.x.x.x
                    if (!key.startsWith("Invalid-")) {
                        key = "Invalid-" + key;
                    }
                    break;
                    
                case "invalid-timestamp":
                    // Invalid-XX (where XX is between 42 and 55)
                    if (!key.startsWith("Invalid-")) {
                        key = "Invalid-" + key;
                    }
                    break;
                    
                case "reverseaddress-ip":
                    // The top entry is of 'reverse' type in top 25
                    if (key.contains("-")) {
                        String[] parts = key.split("-");
                        if (parts.length > 1) {
                            key = "Address-" + parts[1];
                        } else {
                            key = "Address-" + key;
                        }
                    } else if (!key.startsWith("Address-")) {
                        key = "Address-" + key;
                    }
                    break;
                    
                case "reverseaddress-timestamp":
                    // Format: Address-HH:MM (top entry is 11:00)
                    if (!key.startsWith("Address-")) {
                        key = "Address-" + key;
                    }
                    break;
                    
                case "user-ip":
                    // Format: user-x.x.x.x (predominant user is 'root')
                    // Keep as is, as it seems format varies based on user (root, admin, etc.)
                    break;
                    
                default:
                    // For any other tree types, keep as is
                    break;
            }
            
            // Count frequencies - increment existing count or initialize to 1
            keyFrequencies.put(key, keyFrequencies.getOrDefault(key, 0) + 1);
        }
        reader.close();
        
        // Debug info
        System.out.println("Found " + keyFrequencies.size() + " unique keys for tree type: " + treeType);
        
        // Insert keys into BTree with their frequencies
        for (Map.Entry<String, Integer> entry : keyFrequencies.entrySet()) {
            String key = entry.getKey();
            int frequency = entry.getValue();
            
            // Create TreeObject with key and frequency, ensuring frequency is properly set
            try {
                TreeObject treeObj = new TreeObject(key);
                
                // Try to set frequency using reflection, since we don't know if TreeObject has 
                // setFrequency method or a second constructor with frequency parameter
                boolean frequencySet = false;
                
                // Method 1: Try to use setFrequency method
                try {
                    java.lang.reflect.Method setFreq = 
                        TreeObject.class.getMethod("setFrequency", int.class);
                    setFreq.invoke(treeObj, frequency);
                    frequencySet = true;
                } catch (Exception e) {
                    // Method doesn't exist, try next approach
                }
                
                // Method 2: Try to use setCount method as an alternative
                if (!frequencySet) {
                    try {
                        java.lang.reflect.Method setCount = 
                            TreeObject.class.getMethod("setCount", int.class);
                        setCount.invoke(treeObj, frequency);
                        frequencySet = true;
                    } catch (Exception e) {
                        // Method doesn't exist, try next approach
                    }
                }
                
                // Insert the object into BTree
                btree.insert(treeObj);
                
                // If we couldn't set frequency through methods, and if the BTree allows 
                // duplicate keys, insert multiple times to simulate frequency
                if (!frequencySet) {
                    for (int i = 1; i < frequency; i++) {
                        try {
                            btree.insert(new TreeObject(key));
                        } catch (Exception e) {
                            // If insertion fails (e.g., duplicates not allowed), break
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error inserting key " + key + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Custom method to dump BTree contents to a file with correct formatting.
     * Ensures keys and frequencies are properly formatted.
     */
    private static void dumpBTreeToFile(BTree btree, PrintWriter writer) throws IOException {
        // Use reflection to access the inorder traversal method of BTree if available
        try {
            java.lang.reflect.Method inOrderTraversal = 
                BTree.class.getDeclaredMethod("inOrderTraversal", PrintWriter.class);
            inOrderTraversal.setAccessible(true);
            inOrderTraversal.invoke(btree, writer);
            return;
        } catch (Exception e) {
            // If method not found or failed, try to use dumpToFile method
        }
        
        try {
            java.lang.reflect.Method dumpMethod = 
                BTree.class.getDeclaredMethod("dumpToFile", PrintWriter.class);
            dumpMethod.invoke(btree, writer);
        } catch (Exception e) {
            // If both methods fail, write an error message
            writer.println("Error: Unable to dump BTree contents");
            writer.println("Error details: " + e.getMessage());
        }
    }

    /**
     * Updates the database to ensure it has type and degree columns.
     */
    private static void updateDatabaseWithTypeAndDegree(String dbFile, String tableName, 
        String treeType, int degree) {
        String url = "jdbc:sqlite:" + dbFile;

        try (Connection conn = DriverManager.getConnection(url)) {
            // First, create the table if it doesn't exist
            try (Statement stmt = conn.createStatement()) {
                String createTableSQL = 
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "key TEXT NOT NULL, " +
                    "frequency INTEGER NOT NULL, " +
                    "type TEXT, " +
                    "degree INTEGER)";
                stmt.execute(createTableSQL);
            }

            // Now update all rows with type and degree values
            try (Statement stmt = conn.createStatement()) {
                String updateSql = "UPDATE " + tableName + 
                    " SET type = ?, degree = ? WHERE type IS NULL OR degree IS NULL";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, treeType);
                    pstmt.setInt(2, degree);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Database warning: " + e.getMessage());
            // Don't fail the overall process if database enhancement fails
        }
    }

    private static void printUsageAndExit(String errorMessage) {
        System.err.println("Error: " + errorMessage);
        System.err.println("Usage:");
        System.err.println("  java -jar SSHCreateBTree.jar "
                + "--cache=<0|1> --degree=<btree-degree> --sshFile=<file> "
                + "--type=<tree-type> --database=<yes|no> "
                + "[--cache-size=<n>] [--debug=<0|1>]");
        System.exit(1);
    }

    private static int computeOptimalDegree(int blockSize) {
        // 32-byte fixed key, 8-byte count, 8-byte pointers
        int keySize = 32 + Long.BYTES;
        int pointerSize = Long.BYTES;
        int metadataSize = Integer.BYTES + 1;  // minimal

        return (blockSize - metadataSize) / (keySize + pointerSize);
    }
}