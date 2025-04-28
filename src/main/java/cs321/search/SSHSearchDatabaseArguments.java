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

    // Stores the validated value for --type
    private final String type;

    // Stores the validated value for --database
    private final String database;

    // Stores the validated value for --top-frequency
    private final int topFrequency;

    // List of valid values for the --type argument
    private static final String[] VALID_TYPES = {
        "accepted-ip", "accepted-time", "failed-ip", "failed-time",
        "invalid-ip", "invalid-time", "reverseaddress-ip", "reverseaddress-time",
        "user-ip"
    };

    /**
     * Constructor that parses and validates command-line arguments.
     *
     * @param args command-line arguments in the format --key=value
     * @throws IllegalArgumentException if required arguments are missing or invalid
     */
    public SSHSearchDatabaseArguments(String[] args) {
        // Parse arguments into a map of key-value pairs
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] split = arg.substring(2).split("=", 2); // Remove '--' and split at '='
                map.put(split[0], split[1]); // Store as key-value in map
            }
        }

        // Extract individual argument values from the map
        this.type = map.get("type");
        this.database = map.get("database");
        String topStr = map.get("top-frequency");

        // Ensure all required arguments are provided
        if (type == null || database == null || topStr == null) {
            throw new IllegalArgumentException("Usage: --type=<type> --database=<path> --top-frequency=<10|25|50>");
        }

        // Validate that the given type is one of the supported types
        if (!java.util.Arrays.asList(VALID_TYPES).contains(type)) {
            throw new IllegalArgumentException("Invalid --type: " + type);
        }

        // Validate that the top-frequency is an integer and one of the accepted values
        try {
            this.topFrequency = Integer.parseInt(topStr);
            if (topFrequency != 10 && topFrequency != 25 && topFrequency != 50) {
                throw new NumberFormatException(); // Trigger catch block
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid --top-frequency: must be 10, 25, or 50.");
        }
    }

    // Getter for the --type value
    public String getType() {
        return type;
    }

    // Getter for the --database value
    public String getDatabase() {
        return database;
    }

    // Getter for the --top-frequency value
    public int getTopFrequency() {
        return topFrequency;
    }
}