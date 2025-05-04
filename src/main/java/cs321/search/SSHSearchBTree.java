package cs321.search;

import cs321.btree.BTree;
import cs321.btree.TreeObject;
import java.io.*;
import java.util.*;

/**
 * Search keys in an existing B-Tree file and optionally output the top K by frequency.
 *
 * Usage:
 *   java -jar SSHSearchBTree.jar \
 *     --cache=<0|1> --degree=<btree-degree> \
 *     --btreeFile=<btree-filename> \
 *     --queryFile=<query-filename> \
 *     [--topFrequency=<10|25|50>] [--cacheSize=<100-10000>] [--debug=<0|1>]
 */
public class SSHSearchBTree {

    public static void main(String[] args) {
        try {
            SSHSearchBTreeArguments params = new SSHSearchBTreeArguments(args);

            // Load the existing B-Tree
            BTree tree = new BTree(
                params.getDegree(),
                params.getBtreeFilename(),
                params.getUseCache(),
                params.getCacheSize()
            );

            // Read queries
            List<String> queries = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(params.getQueryFilename()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        queries.add(line);
                    }
                }
            }

            // Search and collect results
            List<TreeObject> hits = new ArrayList<>();
            for (String key : queries) {
                TreeObject obj = tree.search(key);
                if (obj != null) {
                    hits.add(obj);
                } else if (params.getDebugLevel() == 1) {
                    System.err.println("Query not found: " + key);
                }
            }

            // Sort by frequency desc, then key asc
            hits.sort(Comparator
                .comparingLong(TreeObject::getCount).reversed()
                .thenComparing(TreeObject::getKey)
            );

            // Limit to topFrequency if specified
            int limit = params.getTopFrequency();
            if (limit > 0 && hits.size() > limit) {
                hits = hits.subList(0, limit);
            }

            // Print results
            for (TreeObject obj : hits) {
                System.out.println(obj.getKey() + " " + obj.getCount());
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
