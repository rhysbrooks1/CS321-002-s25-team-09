package cs321.search;

import java.util.Arrays;

/**
 * The argument class for parsing SSHSearchDatabase's command line arguments
 */
public class SSHSearchDatabaseArguments {
    private String treeType;
    private String database;
    private int topFrequency;

    // Valid tree types according to project spec
    private static final String[] VALID_TREE_TYPES = {
            "accepted-ip", "accepted-time", "failed-ip", "failed-time",
            "invalid-ip", "invalid-time", "reverseaddress-ip", "reverseaddress-time", "user-ip",
            // Include timestamp variants for compatibility
            "accepted-timestamp", "failed-timestamp", "invalid-timestamp", "reverseaddress-timestamp"
    };

    public SSHSearchDatabaseArguments(String[] args) throws IllegalArgumentException {
        try {
            // Process arguments
            for (String arg : args) {
                if (arg.startsWith("--type=")) {
                    treeType = arg.substring(7);
                } else if (arg.startsWith("--database=")) {
                    database = arg.substring(11);
                } else if (arg.startsWith("--top-frequency=")) {
                    topFrequency = Integer.parseInt(arg.substring(16));
                }
            }

            validateArguments();

        } catch (NumberFormatException e) {
            this.notifyInvalidArguments("Error: Failed to parse numeric argument.");
        }
    }

    /**
     * Validates the arguments for SSHSearchDatabase.
     *
     * @throws IllegalArgumentException If the arguments are invalid.
     */
    private void validateArguments() throws IllegalArgumentException {
        // Required arguments not found -> invalid arguments
        if (this.treeType == null || this.database == null || this.topFrequency == 0) {
            this.notifyInvalidArguments("Error: Missing required arguments");
        }

        // Handle timestamp vs time naming conversion
        if (treeType.endsWith("-timestamp")) {
            treeType = treeType.replace("-timestamp", "-time");
        }

        // Validate tree type
        if (!Arrays.asList(VALID_TREE_TYPES).contains(treeType)) {
            this.notifyInvalidArguments("Error: Invalid tree type: " + treeType);
        }

        // Make sure top frequency is correct
        if (topFrequency != 10 && topFrequency != 25 && topFrequency != 50) {
            this.notifyInvalidArguments("Error: Top frequency must be <10/25/50>");
        }
    }

    /**
     * Notifies the user of invalid arguments and throws an
     * IllegalArgumentException.
     *
     * @param message the message to display to the user
     *
     * @throws IllegalArgumentException
     */
    private void notifyInvalidArguments(String message) throws IllegalArgumentException {
        String formattedString = message + "\n";

        formattedString += "Usage: java -jar build/libs/SSHSearchDatabase.jar --type=<tree-type> --database=<sqlite-database-path> --top-frequency=<10/25/50>"
                + "\n";

        throw new IllegalArgumentException(formattedString);
    }

    /**
     * Returns the treeType string
     *
     * @return the treeType string
     */
    public String getTreeType() {
        return this.treeType;
    }

    /**
     * Returns the database string filename
     * 
     * @return the database string filename
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Returns the top frequency integer
     * 
     * @return the top frequency integer
     */
    public int getTopFrequency() {
        return this.topFrequency;
    }

    @Override
    public String toString() {
        return "SSHSearchDatabaseArguments{" +
                "type=" + treeType +
                ", database=" + database +
                ", top-frequency='" + topFrequency +
                '}';
    }
}