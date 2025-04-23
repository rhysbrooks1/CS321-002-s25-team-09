package cs321.search;


public class SSHSearchBTreeArguments
{
    private final boolean cache;
    private final int degree;
    private final String btreeFilename;
    private final String queryFilename;
    private final int topFrequency;
    private final int cacheSize;
    private final int debugLevel;

    public SSHSearchBTreeArguments(String[] args)
    {
        boolean cache = false;
        int degree = -1;
        String btreeFilename = null;
        String queryFilename = null;
        int topFrequency = 0;
        int cacheSize = 0;
        int debugLevel = 0;

        for (String arg : args) {
            if (arg.startsWith("--cache=")) {
                cache = arg.split("=")[1].equals("1");
            } else if (arg.startsWith("--degree=")) {
                degree = Integer.parseInt(arg.split("=")[1]);
            } else if (arg.startsWith("--btree-file=")) {
                btreeFilename = arg.split("=")[1];
            } else if (arg.startsWith("--query-file=")) {
                queryFilename = arg.split("=")[1];
            } else if (arg.startsWith("--top-frequency=")) {
                topFrequency = Integer.parseInt(arg.split("=")[1]);
            } else if (arg.startsWith("--cache-size=")) {
                cacheSize = Integer.parseInt(arg.split("=")[1]);
            } else if (arg.startsWith("--debug=")) {
                debugLevel = Integer.parseInt(arg.split("=")[1]);
            }
        }

        if (degree < 0 || btreeFilename == null || queryFilename == null) {
            throw new IllegalArgumentException("Missing required arguments");
        }

        this.cache = cache;
        this.degree = degree;
        this.btreeFilename = btreeFilename;
        this.queryFilename = queryFilename;
        this.topFrequency = topFrequency;
        this.cacheSize = cacheSize;
        this.debugLevel = debugLevel;
    }

    public boolean isCacheEnabled() {
        return cache;
    }

    public int getDegree() {
        return degree;
    }

    public String getBtreeFile() {
        return btreeFilename;
    }

    public String getQueryFile() {
        return queryFilename;
    }

    public int getTopFrequency() {
        return topFrequency;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getDebugLevel() {
        return debugLevel;
    }
}
