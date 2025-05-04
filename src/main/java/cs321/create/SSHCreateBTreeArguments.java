package cs321.create;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * SSHCreateBTreeArguments parses command line arguments for SSHCreateBTree.
 */
public class SSHCreateBTreeArguments {

    private final boolean useCache;
    private final boolean createDatabase;
    private final int     degree;
    private final String  sshFilename;
    private final String  treeType;
    private final int     cacheSize;
    private final int     debugLevel;

    private static final String[] VALID_TREE_TYPES = {
        "accepted-ip","accepted-time","invalid-ip","invalid-time",
        "failed-ip","failed-time","reverseaddress-ip","reverseaddress-time","user-ip"
    };

    /** Usage message */
    private static final String USAGE =
      "Usage: SSHCreateBTree --cache=<0|1> --degree=<btree-degree> " +
      "--sshFile=<wrangled-ssh-file> --type=<tree-type> " +
      "[--cache-size=<n>] --database=<yes|no> [--debug=<0|1>]";

    public SSHCreateBTreeArguments(String[] args) {
        Boolean _useCache     = null;
        Boolean _createDB     = null;
        Integer _degree       = null;
        String  _sshFile      = null;
        String  _treeType     = null;
        Integer _cacheSize    = null;
        Integer _debugLevel   = 0;

        for (String arg : args) {
            if (arg.startsWith("--cache=")) {
                int v = Integer.parseInt(arg.substring(8));
                if (v!=0 && v!=1) throwIllegal("cache must be 0 or 1");
                _useCache = (v==1);
            }
            else if (arg.startsWith("--degree=")) {
                _degree = Integer.parseInt(arg.substring(9));
                if (_degree < 0) throwIllegal("degree cannot be negative");
            }
            else if (arg.startsWith("--sshFile=")) {
                _sshFile = arg.substring(10);
            }
            else if (arg.startsWith("--type=")) {
                _treeType = arg.substring(7);
            }
            else if (arg.startsWith("--cache-size=")) {
                _cacheSize = Integer.parseInt(arg.substring(13));
            }
            else if (arg.startsWith("--database=")) {
                String val = arg.substring(11);
                if (!val.equals("yes") && !val.equals("no")) 
                    throwIllegal("database must be 'yes' or 'no'");
                _createDB = val.equals("yes");
            }
            else if (arg.startsWith("--debug=")) {
                _debugLevel = Integer.parseInt(arg.substring(8));
                if (_debugLevel<0 || _debugLevel>1) 
                    throwIllegal("debug must be 0 or 1");
            }
            else {
                throwIllegal("Unknown argument: "+arg);
            }
        }

        // now check for missing args
        if (_useCache    == null ||
            _createDB    == null ||
            _degree      == null ||
            _sshFile     == null ||
            _treeType    == null) {
            throwIllegal("Missing required arguments");
        }

        // validate file existence/readability
        if (!Files.exists(Paths.get(_sshFile))) {
            throwIllegal("SSH log file does not exist: " + _sshFile);
        }
        if (!Files.isReadable(Paths.get(_sshFile))) {
            throwIllegal("SSH log file is not readable: " + _sshFile);
        }

        // validate treeType
        if (!Arrays.asList(VALID_TREE_TYPES).contains(_treeType)) {
            throwIllegal("Invalid tree type: " + _treeType);
        }

        // if using cache, ensure cacheSize is set and in range
        if (_useCache) {
            if (_cacheSize == null) 
                throwIllegal("Cache size must be specified when --cache=1");
            if (_cacheSize < 100 || _cacheSize > 10000) 
                throwIllegal("Cache size must be 100–10000");
        } else {
            _cacheSize = 0;  // won't be used
        }

        // finally assign to finals
        this.useCache      = _useCache;
        this.createDatabase= _createDB;
        this.degree        = _degree;
        this.sshFilename   = _sshFile;
        this.treeType      = _treeType;
        this.cacheSize     = _cacheSize;
        this.debugLevel    = _debugLevel;
    }

    private void throwIllegal(String msg) {
        throw new IllegalArgumentException("Error: " + msg + "\n\n" + USAGE);
    }

    public boolean getUseCache()      { return useCache; }
    public boolean getCreateDatabase(){ return createDatabase; }
    public int     getDegree()        { return degree; }
    public String  getSSHFilename()   { return sshFilename; }
    public String  getTreeType()      { return treeType; }
    public int     getCacheSize()     { return cacheSize; }
    public int     getDebugLevel()    { return debugLevel; }

    @Override
    public String toString() {
        return "SSHCreateBTreeArguments[" +
            "cache=" + useCache +
            ", degree=" + degree +
            ", sshFile=" + sshFilename +
            ", type=" + treeType +
            ", cacheSize=" + cacheSize +
            ", db=" + createDatabase +
            ", debug=" + debugLevel +
        "]";
    }
}
