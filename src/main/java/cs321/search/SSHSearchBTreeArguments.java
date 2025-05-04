package cs321.search;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * SSHSearchBTreeArguments parses command line arguments for SSHSearchBTree.
 * Only accepts the hyphenated flags used by the integration scripts.
 */
public class SSHSearchBTreeArguments {

    private boolean useCache;
    private int     degree;
    private String  btreeFilename;
    private String  queryFilename;
    private Integer topFrequency;
    private int     cacheSize;
    private int     debugLevel = 0;

    private static final Integer[] VALID_TOP_FREQUENCIES = {10, 25, 50};

    private static final String USAGE =
        "Usage: SSHSearchBTree --cache=<0|1> --degree=<btree-degree> " +
        "--btree-file=<btree-filename> --query-file=<query-filename> " +
        "[--top-frequency=<10|25|50>] [--cache-size=<100-10000>] [--debug=<0|1>]";

    public SSHSearchBTreeArguments(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--cache=")) {
                int v = Integer.parseInt(arg.substring("--cache=".length()));
                if (v != 0 && v != 1) {
                    throw new IllegalArgumentException("Error: cache must be 0 or 1\n\n" + USAGE);
                }
                useCache = (v == 1);

            } else if (arg.startsWith("--degree=")) {
                degree = Integer.parseInt(arg.substring("--degree=".length()));
                if (degree < 0) {
                    throw new IllegalArgumentException("Error: degree cannot be negative\n\n" + USAGE);
                }

            } else if (arg.startsWith("--btree-file=")) {
                btreeFilename = arg.substring("--btree-file=".length());

            } else if (arg.startsWith("--query-file=")) {
                queryFilename = arg.substring("--query-file=".length());

            } else if (arg.startsWith("--top-frequency=")) {
                topFrequency = Integer.parseInt(arg.substring("--top-frequency=".length()));

            } else if (arg.startsWith("--cache-size=")) {
                cacheSize = Integer.parseInt(arg.substring("--cache-size=".length()));

            } else if (arg.startsWith("--debug=")) {
                debugLevel = Integer.parseInt(arg.substring("--debug=".length()));

            } else {
                throw new IllegalArgumentException("Error: Unknown argument: " + arg + "\n\n" + USAGE);
            }
        }

        // Validate required arguments
        if (btreeFilename == null || queryFilename == null) {
            throw new IllegalArgumentException("Error: Missing required arguments\n\n" + USAGE);
        }
        // Validate files exist
        if (!Files.exists(Paths.get(btreeFilename))) {
            throw new IllegalArgumentException(
                "Error: BTree file does not exist: " + btreeFilename + "\n\n" + USAGE
            );
        }
        if (!Files.exists(Paths.get(queryFilename))) {
            throw new IllegalArgumentException(
                "Error: Query file does not exist: " + queryFilename + "\n\n" + USAGE
            );
        }

        // Cache constraints
        if (useCache) {
            if (cacheSize < 100 || cacheSize > 10000) {
                throw new IllegalArgumentException(
                    "Error: cache-size must be between 100 and 10000\n\n" + USAGE
                );
            }
        } else {
            cacheSize = 0;
        }

        // top-frequency constraints
        if (topFrequency != null && !Arrays.asList(VALID_TOP_FREQUENCIES).contains(topFrequency)) {
            throw new IllegalArgumentException(
                "Error: top-frequency must be 10, 25, or 50\n\n" + USAGE
            );
        }
        // debug constraints
        if (debugLevel < 0 || debugLevel > 1) {
            throw new IllegalArgumentException(
                "Error: debug must be 0 or 1\n\n" + USAGE
            );
        }
    }

    public boolean getUseCache()      { return useCache; }
    public int     getDegree()        { return degree; }
    public String  getBtreeFilename() { return btreeFilename; }
    public String  getQueryFilename() { return queryFilename; }
    public int     getTopFrequency()  { return topFrequency == null ? 0 : topFrequency; }
    public int     getCacheSize()     { return cacheSize; }
    public int     getDebugLevel()    { return debugLevel; }

    @Override
    public String toString() {
        return "SSHSearchBTreeArguments[cache="+useCache+
               ", degree="+degree+", btree-file="+btreeFilename+
               ", query-file="+queryFilename+", top-frequency="+topFrequency+
               ", cache-size="+cacheSize+", debug="+debugLevel+"]";
    }
}
