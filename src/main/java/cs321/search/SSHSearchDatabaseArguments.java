package cs321.search;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses and validates command-line arguments for the SSHSearchDatabase program.
 *
 * This class is responsible for interpreting arguments passed to:
 *
 *   java -jar build/libs/SSHSearchDatabase.jar
 *
 * Required command-line arguments:
 *
 *   --type=<tree-type>
 *     Specifies the category of log data to query.
 *     Valid values:
 *       accepted-ip
 *       accepted-time
 *       failed-ip
 *       failed-time
 *       invalid-ip
 *       invalid-time
 *       reverseaddress-ip
 *       reverseaddress-time
 *       user-ip
 *       test         // Special mode to populate a test SQLite database
 *
 *   --database=<sqlite-database-path>
 *     The path to the SQLite database file.
 *     Example: SSHLogDB.db
 *
 *   --top-frequency=<10|25|50>
 *     The number of most frequent keys to return from the database.
 *
 * Complete Format:
 * java -jar build/libs/SSHSearchDatabase.jar --type=<tree-type> \
          --database=<sqlite-database-path> --top-frequency=<10/25/50>
 */

 public class SSHSearchDatabaseArguments {

    private final String type;
    private final String database;
    private final int topFrequency;

    private static final String[] VALID_TYPES = {
        "accepted-ip", "accepted-time", "failed-ip", "failed-time",
        "invalid-ip", "invalid-time", "reverseaddress-ip", "reverseaddress-time",
        "user-ip"
    };

    public SSHSearchDatabaseArguments(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] split = arg.substring(2).split("=", 2);
                map.put(split[0], split[1]);
            }
        }

        this.type = map.get("type");
        this.database = map.get("database");
        String topStr = map.get("top-frequency");

        if (type == null || database == null || topStr == null) {
            throw new IllegalArgumentException("Usage: --type=<type> --database=<path> --top-frequency=<10|25|50>");
        }

        if (!java.util.Arrays.asList(VALID_TYPES).contains(type)) {
            throw new IllegalArgumentException("Invalid --type: " + type);
        }

        try {
            this.topFrequency = Integer.parseInt(topStr);
            if (topFrequency != 10 && topFrequency != 25 && topFrequency != 50) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid --top-frequency: must be 10, 25, or 50.");
        }
    }

    public String getType() {
        return type;
    }

    public String getDatabase() {
        return database;
    }

    public int getTopFrequency() {
        return topFrequency;
    }
}

