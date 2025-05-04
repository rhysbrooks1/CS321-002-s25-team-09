package cs321.search;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * The argument class for parsing SSHSearchDatabase's command line arguments
 * 
 * @author Devyn Korber
 */
public class SSHSearchDatabaseArguments {
    private String treeType;
    private String database;
    private int topFrequency;

    private static final String[] VALID_TREE_TYPES = {
            "accepted-ip", "accepted-time", "invalid-ip", "invalid-time",
            "failed-ip", "failed-time", "reverseaddress-ip", "reverseaddress-time", "user-ip"
    };

    public SSHSearchDatabaseArguments(String[] args) throws IllegalArgumentException {
        try {
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
            this.notifyInvalidArguments("Error: Failed to parse argument.");
        }
    }

    /**
     * Validates the arguments for SSHSearchDatabase.
     *
     * @throws IllegalArgumentException If the arguments are invalid.
     */
    private void validateArguments() throws IllegalArgumentException {
        // required arguments not found -> invalid arguments
        if (this.treeType == null || this.database == null || this.topFrequency == 0) {
            this.notifyInvalidArguments("Error: Missing required arguments");
        }

        // validate tree type
        if (!Arrays.asList(VALID_TREE_TYPES).contains(treeType)) {
            this.notifyInvalidArguments("Error: Invalid tree type.");
        }

        // validate database file exists
        if (!Files.exists(Paths.get(this.database))) {
            this.notifyInvalidArguments("Error: Database does not exist.");
        }

        // make sure top frequency is correct
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

        formattedString += "Usage: SSHSearchDatabase --type=<tree-type> --database=<sqlite-database-path> --top-frequency=<10/25/50>"
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
