package cs321.create;

import cs321.common.ParseArgumentException;
import cs321.common.ParseArgumentUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * SSHCreateBTreeArguments parses command line arguments for SSHCreateBTree.
 * Expected flags:
 *   --cache=<0|1>
 *   --degree=<btree-degree>
 *   --sshFile=<input filename>
 *   --type=<btree-type>
 *   [--cache-size=<n>] if cache=1
 *   --database=<yes|no>
 *   [--debug=<0|1>]
 */
public class SSHCreateBTreeArguments {

    private final boolean useCache;
    private final int degree;
    private final String SSHFileName;
    private final String treeType;
    private final int cacheSize;
    private final int debugLevel;
    private final boolean useDatabase;

    /**
     * Constructor parses and validates CLI arguments.
     * @param args command-line arguments
     * @throws ParseArgumentException if invalid arguments are provided
     */
    public SSHCreateBTreeArguments(String[] args) throws ParseArgumentException {
        Map<String, String> map = new HashMap<>();

        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] split = arg.substring(2).split("=", 2);
                map.put(split[0], split[1]);
            }
        }

        // Validate required arguments
        if (!map.containsKey("cache") || !map.containsKey("degree") ||
            !map.containsKey("sshFile") || !map.containsKey("type") ||
            !map.containsKey("database")) {
            throw new ParseArgumentException("Missing required arguments: --cache, --degree, --sshFile, --type, --database");
        }

        int cacheInt = ParseArgumentUtils.convertStringToInt(map.get("cache"));
        ParseArgumentUtils.verifyRanges(cacheInt, 0, 1);
        this.useCache = (cacheInt == 1);

        this.degree = ParseArgumentUtils.convertStringToInt(map.get("degree"));
        this.SSHFileName = map.get("sshFile");
        this.treeType = map.get("type");

        String dbFlag = map.get("database");
        if (!dbFlag.equals("yes") && !dbFlag.equals("no")) {
            throw new ParseArgumentException("--database must be yes or no");
        }
        this.useDatabase = dbFlag.equals("yes");

        if (useCache) {
            if (!map.containsKey("cache-size")) {
                throw new ParseArgumentException("--cache-size is required when cache=1");
            }
            int size = ParseArgumentUtils.convertStringToInt(map.get("cache-size"));
            ParseArgumentUtils.verifyRanges(size, 100, 10000);
            this.cacheSize = size;
        } else {
            this.cacheSize = -1;
        }

        if (map.containsKey("debug")) {
            int debug = ParseArgumentUtils.convertStringToInt(map.get("debug"));
            ParseArgumentUtils.verifyRanges(debug, 0, 1);
            this.debugLevel = debug;
        } else {
            this.debugLevel = 0;
        }
    }

    public boolean isCacheEnabled() {
        return useCache;
    }

    public int getDegree() {
        return degree;
    }

    public String getSSHFileName() {
        return SSHFileName;
    }

    public String getTreeType() {
        return treeType;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getDebugLevel() {
        return debugLevel;
    }

    public boolean useDatabase() {
        return useDatabase;
    }

    @Override
    public String toString() {
        return "SSHCreateBTreeArguments{" +
                "useCache=" + useCache +
                ", degree=" + degree +
                ", SSHFileName='" + SSHFileName + '\'' +
                ", treeType='" + treeType + '\'' +
                ", cacheSize=" + cacheSize +
                ", debugLevel=" + debugLevel +
                ", useDatabase=" + useDatabase +
                '}';
    }
}