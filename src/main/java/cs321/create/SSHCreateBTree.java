package cs321.create;

import cs321.btree.BTree;
import cs321.btree.TreeObject;
import cs321.common.ParseArgumentException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

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

            // Compute optimal degree if degree=0
            int degree = parsed.getDegree();
            if (degree == 0) {
                degree = computeOptimalDegree(DEFAULT_DISK_BLOCK_SIZE);
            }

            // Initialize the BTree with parameters
            BTree btree;
            if (parsed.isCacheEnabled()) {
                // Build BTree with cache
                btree = new BTree(degree, parsed.getTreeType(), true, parsed.getCacheSize());
            } else {
                // Build BTree without cache
                btree = new BTree(degree, parsed.getTreeType(), false, 0);
            }

            // Read the SSH log and insert each key
            SSHFileReader reader = new SSHFileReader(parsed.getSSHFileName(), parsed.getTreeType());
            while (reader.hasNextKey()) {
                String key = reader.nextKey();
                btree.insert(new TreeObject(key));
            }
            reader.close();

            // Dump to text if in debug mode
            if (parsed.getDebugLevel() == 1) {
                String dumpFileName = "dump-" + parsed.getTreeType() + ".0.txt";
                try (PrintWriter writer = new PrintWriter(new File(dumpFileName))) {
                    btree.dumpToFile(writer);
                }
            }

            // Dump to SQLite database if requested
            if (parsed.useDatabase()) {
                String tableName = parsed.getTreeType().replace("-", "");
                btree.dumpToDatabase("SSHLogDB.db", tableName);
            }

            // Flush and close the BTree file
            btree.finishUp();

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
        // 32-byte fixed key (for <= 32 char strings), 8-byte long frequency, 8-byte child pointers
        int keySize = 32 + 8;           // TreeObject = key + frequency = 40 bytes
        int pointerSize = 8;            // Each child pointer is 8 bytes
        int metadataSize = 16;          // e.g., numKeys, isLeaf

        // Solve: blockSize >= metadataSize + (2t - 1) * keySize + 2t * pointerSize
        // Simplified estimate:
        return (blockSize - metadataSize) / (keySize + pointerSize);
    }
}
