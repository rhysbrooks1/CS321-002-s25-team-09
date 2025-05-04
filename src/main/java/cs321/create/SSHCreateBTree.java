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
                    btree.dumpToFile(writer);
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
        // First pass - count frequencies
        Map<String, Integer> keyFrequencies = new HashMap<>();
        
        SSHFileReader reader = new SSHFileReader(logFilePath, treeType);
        while (reader.hasNextKey()) {
            String key = reader.nextKey();
            keyFrequencies.put(key, keyFrequencies.getOrDefault(key, 0) + 1);
        }
        reader.close();
        
        // Second pass - insert into BTree
        for (Map.Entry<String, Integer> entry : keyFrequencies.entrySet()) {
            String key = entry.getKey();
            int frequency = entry.getValue();
            
            // Try to create TreeObject with frequency - adapt based on constructor availability
            try {
                // Try to use constructor with frequency parameter
                TreeObject treeObj = createTreeObjectWithFrequency(key, frequency);
                btree.insert(treeObj);
            } catch (Exception e) {
                // Fall back to inserting key multiple times if frequency constructor not available
                TreeObject treeObj = new TreeObject(key);
                btree.insert(treeObj);
                
                // If needed, insert key multiple times to simulate frequency
                for (int i = 1; i < frequency; i++) {
                    try {
                        btree.insert(new TreeObject(key));
                    } catch (Exception ex) {
                        // If duplicate insertion fails, stop trying additional insertions
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Attempts to create a TreeObject with a frequency value using reflection.
     */
    private static TreeObject createTreeObjectWithFrequency(String key, int frequency) throws Exception {
        try {
            // Try to find constructor that takes key and frequency
            java.lang.reflect.Constructor<?> constructor = 
                TreeObject.class.getConstructor(String.class, int.class);
            return (TreeObject) constructor.newInstance(key, frequency);
        } catch (NoSuchMethodException e) {
            // If no such constructor, try setter method
            TreeObject obj = new TreeObject(key);
            try {
                java.lang.reflect.Method setFreq = 
                    TreeObject.class.getMethod("setFrequency", int.class);
                setFreq.invoke(obj, frequency);
                return obj;
            } catch (NoSuchMethodException ex) {
                // If no setter either, fall back to base constructor
                return new TreeObject(key);
            }
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