package cs321.create;

import cs321.btree.BTree;
import cs321.btree.TreeObject;
import cs321.common.ParseArgumentException;

import java.io.FileNotFoundException;

/**
 * The driver class for building a BTree representation of an SSH Log file.
 */
public class SSHCreateBTree {

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

            // Initialize BTree
            BTree btree = new BTree(parsed.getDegree(), parsed.getTreeType(), parsed.isCacheEnabled(), parsed.getCacheSize());

            // Read from wrangled file and insert into BTree
            SSHFileReader reader = new SSHFileReader(parsed.getSSHFileName(), parsed.getTreeType());
            while (reader.hasNextKey()) {
                String key = reader.nextKey();
                btree.insert(new TreeObject(key));
            }
            reader.close();

            // Save the BTree to disk
            btree.saveToFile();

            // Optional: write inorder traversal to dump file
            if (parsed.getDebugLevel() == 1) {
                btree.dumpToTextFile();
            }

            // Optional: insert BTree contents into a database
            if (parsed.useDatabase()) {
                btree.writeToDatabase("SSHLogDB.db");
            }

        } catch (ParseArgumentException e) {
            printUsageAndExit("Argument error: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Print usage message and exit.
     * @param errorMessage explanation of failure
     */
    private static void printUsageAndExit(String errorMessage) {
        System.err.println("Error: " + errorMessage);
        System.err.println("Usage:");
        System.err.println("  java -jar SSHCreateBTree.jar "
                + "--cache=<0|1> --degree=<btree-degree> --sshFile=<file> "
                + "--type=<tree-type> --database=<yes|no> "
                + "[--cache-size=<n>] [--debug=<0|1>]");
        System.exit(1);
    }
}
