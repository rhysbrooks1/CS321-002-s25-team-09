package cs321.create;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * SSHCreateBTreeArguments parses command line arguments for SSHCreateBTree and
 * validates them according to the project specification.
 */
public class SSHCreateBTreeArguments {

    private final boolean useCache;
    private final boolean createDatabase;
    private final int     degree;
    private final String  sshFilename;
    private final String  treeType;
    private final int     cacheSize;
    private final int     debugLevel;

    // Allowed tree types per spec
    private static final String[] VALID_TYPES = {
        "accepted-ip","accepted-time","invalid-ip","invalid-time",
        "failed-ip","failed-time","reverseaddress-ip","reverseaddress-time","user-ip"
    };

    private static final String USAGE =
        "Usage: SSHCreateBTree --cache=<0|1> --degree=<btree-degree> " +
        "--sshFile=<wrangled-ssh-file> --type=<tree-type> " +
        "[--cache-size=<100-10000>] --database=<yes|no> [--debug=<0|1>]";

    public SSHCreateBTreeArguments(String[] args) {
        Boolean _useCache   = null;
        Boolean _createDB   = null;
        Integer _degree     = null;
        String  _sshFile    = null;
        String  _treeType   = null;
        Integer _cacheSize  = null;
        Integer _debugLevel = 0;

        for (String arg : args) {
            if (arg.startsWith("--cache=")) {
                int v = parseInt(arg, 8, "cache");
                if (v!=0 && v!=1) fail("--cache must be 0 or 1");
                _useCache = v==1;

            } else if (arg.startsWith("--degree=")) {
                _degree = parseInt(arg, 9, "degree");
                if (_degree < 0) fail("--degree cannot be negative");

            } else if (arg.startsWith("--sshFile=")) {
                _sshFile = arg.substring(10);

            } else if (arg.startsWith("--type=")) {
                _treeType = arg.substring(7);

            } else if (arg.startsWith("--cache-size=")) {
                _cacheSize = parseInt(arg, 13, "cache-size");

            } else if (arg.startsWith("--database=")) {
                String val = arg.substring(11);
                if (!val.equals("yes") && !val.equals("no")) {
                    fail("--database must be 'yes' or 'no'");
                }
                _createDB = val.equals("yes");

            } else if (arg.startsWith("--debug=")) {
                _debugLevel = parseInt(arg, 8, "debug");
                if (_debugLevel<0 || _debugLevel>1) fail("--debug must be 0 or 1");

            } else {
                fail("Unknown argument: " + arg);
            }
        }

        // Check required
        if (_useCache==null || _createDB==null || _degree==null ||
            _sshFile==null || _treeType==null) {
            fail("Missing required arguments");
        }

        // Validate SSH log file
        if (!Files.exists(Paths.get(_sshFile)))  fail("SSH log file not found: " + _sshFile);
        if (!Files.isReadable(Paths.get(_sshFile))) fail("SSH log file not readable: " + _sshFile);

        // Validate tree type
        if (!Arrays.asList(VALID_TYPES).contains(_treeType)) {
            fail("Invalid tree type: " + _treeType);
        }

        // Cache size constraints
        if (_useCache) {
            if (_cacheSize==null) fail("--cache-size required when --cache=1");
            if (_cacheSize < 100 || _cacheSize > 10000) {
                fail("--cache-size must be between 100 and 10000");
            }
        } else {
            _cacheSize = 0; // ignored
        }

        // Assign finals
        this.useCache      = _useCache;
        this.createDatabase= _createDB;
        this.degree        = _degree;
        this.sshFilename   = _sshFile;
        this.treeType      = _treeType;
        this.cacheSize     = _cacheSize;
        this.debugLevel    = _debugLevel;
    }

    private int parseInt(String arg, int prefixLen, String name) {
        try {
            return Integer.parseInt(arg.substring(prefixLen));
        } catch (NumberFormatException e) {
            fail("Invalid integer for " + name + ": " + arg);
            return -1; // unreachable
        }
    }

    private void fail(String msg) {
        throw new IllegalArgumentException("Error: " + msg + "\n\n" + USAGE);
    }

    // Accessors
    public boolean getUseCache()      { return useCache; }
    public boolean getCreateDatabase(){ return createDatabase; }
    public int     getDegree()        { return degree; }
    public String  getSSHFilename()   { return sshFilename; }
    public String  getTreeType()      { return treeType; }
    public int     getCacheSize()     { return cacheSize; }
    public int     getDebugLevel()    { return debugLevel; }

    @Override
    public String toString() {
        return "SSHCreateBTreeArguments[cache="+useCache+", degree="+degree+
             ", sshFile="+sshFilename+", type="+treeType+
             ", cacheSize="+cacheSize+", db="+createDatabase+
             ", debug="+debugLevel+"]";
    }
}
