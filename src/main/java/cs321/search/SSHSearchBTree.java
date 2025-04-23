package cs321.search;

import cs321.btree.BTree;
import cs321.btree.TreeObject;

import java.io.*;
import java.util.*;

/**
 * Driver class to search a serialized BTree using SSHSearchBTreeArguments.
 */
public class SSHSearchBTree {

    /**
     * Main method to parse CLI args and construct the program.
     *
     * @param argv Command-line arguments
     */
    public static void main(String[] argv) {
        try {
            SSHSearchBTreeArguments parsedArgs = new SSHSearchBTreeArguments(argv);
            new SSHSearchBTree(parsedArgs); 
        } catch (IllegalArgumentException e) {
            System.err.println("Argument error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Constructs and runs the BTree search using validated arguments.
     *
     * @param args Parsed command-line arguments
     */
    public SSHSearchBTree(SSHSearchBTreeArguments args) {
        try {
            // Initialize BTree from file
            BTree tree = new BTree(args.getDegree(), extractTreeType(args.getBtreeFile()),
                                   args.isCacheEnabled(), 
                                   args.getCacheSize() != null ? args.getCacheSize() : 0);

            // Read queries from file
            List<String> queryKeys = new ArrayList<>();
            try (Scanner scanner = new Scanner(new File(args.getQueryFile()))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) queryKeys.add(line);
                }
            }

            // Search each query key
            List<TreeObject> found = new ArrayList<>();
            for (String key : queryKeys) {
                TreeObject obj = tree.search(key);
                if (obj != null) found.add(obj);
            }

            // Output results
            if (args.getTopFrequency() != null) {
                found.sort(Comparator
                        .comparingLong(TreeObject::getCount).reversed()
                        .thenComparing(TreeObject::getKey));
                int limit = Math.min(args.getTopFrequency(), found.size());
                for (int i = 0; i < limit; i++) {
                    System.out.println(found.get(i));
                }
            } else {
                for (TreeObject obj : found) {
                    System.out.println(obj);
                }
            }

        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Helper method to guess the BTree type from the filename.
     * Assumes filename format: something.btree.<type>.<degree>
     */
    private String extractTreeType(String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length < 3) return "unknown";
        return parts[parts.length - 2]; // second to last part is type
    }
}
