package cs321.search;

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
}
