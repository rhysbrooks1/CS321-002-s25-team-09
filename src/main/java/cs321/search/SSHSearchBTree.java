package cs321.search;

import cs321.btree.BTree;
import cs321.btree.TreeObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles searching within a BTree for specific key queries
 * from a given file and outputs results based on frequency.
 *
 * Usage: see SSHSearchBTreeArguments.
 *
 * @author Alec Conn, Paul Bokelman
 */
public class SSHSearchBTree {

    static final String DATABASE_URL = "jdbc:sqlite:SSHLogDB.db";

    /**
     * Constructor; loads the BTree, executes queries, and prints results.
     */
    public SSHSearchBTree(SSHSearchBTreeArguments params) throws IOException, SQLException {
        Integer cacheSize = params.getUseCache() ? params.getCacheSize() : null;
        // use provided BTree constructor signature
        BTree btree = new BTree(
            params.getDegree(),
            params.getBtreeFilename(),
            params.getUseCache(),
            cacheSize == null ? -1 : cacheSize
        );

        List<String> queries = readQueryFile(params.getQueryFilename());
        Map<String, Long> results = searchBTree(btree, queries, params.getDebugLevel());

        if (params.getTopFrequency() != null) {
            results = results.entrySet().stream()
                .sorted((e1, e2) -> {
                    int cmp = Long.compare(e2.getValue(), e1.getValue());
                    return (cmp != 0) ? cmp : e1.getKey().compareTo(e2.getKey());
                })
                .limit(params.getTopFrequency())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a,b) -> a,
                    HashMap::new
                ));
        }

        for (Map.Entry<String, Long> entry : results.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }

        if (params.getDebugLevel() == 1) {
            System.err.println("Debug: Search completed successfully.");
        }
    }

    public static void main(String[] args) {
        try {
            new SSHSearchBTree(new SSHSearchBTreeArguments(args));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> readQueryFile(String queryFile) throws IOException {
        List<String> queries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(queryFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                queries.add(line.trim());
            }
        }
        return queries;
    }

    private static Map<String, Long> searchBTree(
        BTree btree,
        List<String> queries,
        int debugLevel
    ) throws IOException {
        Map<String, Long> results = new HashMap<>();
        for (String key : queries) {
            TreeObject obj = btree.search(key);
            if (obj != null) {
                results.put(obj.getKey(), obj.getCount());
            } else if (debugLevel == 1) {
                System.err.println("Error: Query not found in BTree: " + key);
            }
        }
        return results;
    }
}
