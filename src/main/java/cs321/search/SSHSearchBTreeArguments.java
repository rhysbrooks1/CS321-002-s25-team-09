package cs321.search;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses and validates command-line arguments for the SSHSearchBTree program.
 *
 * This class is responsible for interpreting arguments passed to:
 *
 *   java -jar build/libs/SSHSearchBTree.jar
 *
 * Required command-line arguments:
 *
 *   --cache=<0|1>
 *     Enables (1) or disables (0) the use of an in-memory cache for BTree nodes.
 *
 *   --degree=<btree-degree>
 *     Specifies the degree of the BTree.
 *     Use 0 to auto-calculate the optimal degree based on 4096-byte node size.
 *
 *   --btree-file=<btree-filename>
 *     The path to the binary file containing the serialized BTree.
 *
 *   --query-file=<query-filename>
 *     A file containing one query key per line to search within the BTree.
 *
 * Optional command-line arguments:
 *
 *   --top-frequency=<10|25|50>
 *     If provided, only the top N results will be returned based on frequency.
 *     Defaults to no frequency filtering if omitted.
 *
 *   --cache-size=<n>
 *     The maximum number of BTreeNode objects that can be stored in memory.
 *     Required if --cache=1. Must be between 100 and 10000.
 *
 *   --debug=<0|1>
 *     Enables verbose debug mode (1) or runs normally (0).
 *     Default is 0.
 *
 * Complete Format:
 * java -jar build/libs/SSHSearchBTree.jar --cache=<0/1> --degree=<btree-degree> \
          --btree-file=<btree-filename> --query-file=<query-fileaname> \
          [--top-frequency=<10/25/50>] [--cache-size=<n>]  [--debug=<0|1>]
 */

public class SSHSearchBTreeArguments {

    // Stores if cache should be used (true if --cache=1)
    private final boolean useCache;

    // Degree of the BTree (may be 0 to auto-calculate)
    private final int degree;

    // Path to the serialized BTree file
    private final String btreeFile;

    // Path to the query file (one key per line)
    private final String queryFile;

    // Optional: top N frequency values to return (10, 25, or 50)
    private final Integer topFrequency;

    // Optional: max number of BTreeNodes to keep in cache (100-10000), required if cache=1
    private final Integer cacheSize;

    // Optional: debug flag (0 = normal, 1 = verbose)
    private final int debug;

    /**
     * Constructs a new argument parser from the given CLI args.
     *
     * @param args command-line arguments
     */
    public SSHSearchBTreeArguments(String[] args) {
        Map<String, String> map = new HashMap<>();

        // Parse arguments in the form --key=value
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] split = arg.substring(2).split("=", 2); // remove '--' and split at '='
                map.put(split[0], split[1]); // store as key=value
            }
        }

        // Check that required arguments are present
        if (!map.containsKey("cache") || !map.containsKey("degree") ||
            !map.containsKey("btree-file") || !map.containsKey("query-file")) {
            throw new IllegalArgumentException("Missing required arguments: cache, degree, btree-file, query-file");
        }

        // Parse and validate cache flag
        int cache = Integer.parseInt(map.get("cache"));
        if (cache != 0 && cache != 1) {
            throw new IllegalArgumentException("--cache must be 0 or 1");
        }
        this.useCache = (cache == 1);

        // Parse BTree degree
        this.degree = Integer.parseInt(map.get("degree"));

        // Paths to BTree file and query file
        this.btreeFile = map.get("btree-file");
        this.queryFile = map.get("query-file");

        // Optional: parse top-frequency if present
        if (map.containsKey("top-frequency")) {
            int tf = Integer.parseInt(map.get("top-frequency"));
            if (tf != 10 && tf != 25 && tf != 50) {
                throw new IllegalArgumentException("--top-frequency must be 10, 25, or 50");
            }
            this.topFrequency = tf;
        } else {
            this.topFrequency = null;
        }

        // If cache is enabled, cache-size becomes required
        if (useCache) {
            if (!map.containsKey("cache-size")) {
                throw new IllegalArgumentException("--cache-size is required when cache=1");
            }
            int size = Integer.parseInt(map.get("cache-size"));
            if (size < 100 || size > 10000) {
                throw new IllegalArgumentException("--cache-size must be between 100 and 10000");
            }
            this.cacheSize = size;
        } else {
            this.cacheSize = null; // Not needed if cache is disabled
        }

        // Optional: parse debug flag
        if (map.containsKey("debug")) {
            int d = Integer.parseInt(map.get("debug"));
            if (d != 0 && d != 1) {
                throw new IllegalArgumentException("--debug must be 0 or 1");
            }
            this.debug = d;
        } else {
            this.debug = 0; // Default debug level
        }
    }

    // Accessor for --cache
    public boolean isCacheEnabled() {
        return useCache;
    }

    // Accessor for --degree
    public int getDegree() {
        return degree;
    }

    // Accessor for --btree-file
    public String getBtreeFile() {
        return btreeFile;
    }

    // Accessor for --query-file
    public String getQueryFile() {
        return queryFile;
    }

    // Accessor for --top-frequency
    public Integer getTopFrequency() {
        return topFrequency;
    }

    // Accessor for --cache-size
    public Integer getCacheSize() {
        return cacheSize;
    }

    // Accessor for --debug
    public int getDebug() {
        return debug;
    }
}
