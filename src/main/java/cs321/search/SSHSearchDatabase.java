package cs321.search;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SSHSearchDatabase: queries an existing SQLite database for the top-frequency entries
 * in a specified B-Tree table.
 *
 * Usage:
 *   java -jar SSHSearchDatabase.jar \
 *     --type=<tree-type> \
 *     --database=<sqlite-database-path> \
 *     --top-frequency=<10|25|50>
 */
public class SSHSearchDatabase {

    public static void main(String[] args) {
        try {
            // Parse and validate arguments
            SSHSearchDatabaseArguments params = new SSHSearchDatabaseArguments(args);

            String originalType = params.getTreeType();
            String tableName    = originalType.replace('-', '_');
            String dbPath       = params.getDatabase();
            int topFreq         = params.getTopFrequency();

            // Build SQL: always sum frequencies, group by key or minute
            String sql;
            if (originalType.endsWith("-time")) {
                // Truncate seconds: HH:MM from key format "Type-HH:MM:SS"
                // key_min = substr(key, 1, instr(key, '-') + 6)
                sql = String.format(
                    "SELECT substr(key, 1, instr(key, '-') + 5) AS key, SUM(frequency) AS freq " +
                    "FROM %s " +
                    "GROUP BY substr(key, 1, instr(key, '-') + 5) " +
                    "ORDER BY freq DESC, key ASC " +
                    "LIMIT ?;",
                    tableName
                );
            } else {
                // Group by full key
                sql = String.format(
                    "SELECT key, SUM(frequency) AS freq " +
                    "FROM %s " +
                    "GROUP BY key " +
                    "ORDER BY freq DESC, key ASC " +
                    "LIMIT ?;",
                    tableName
                );
            }

            // Execute query and print results
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, topFreq);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String key = rs.getString("key");
                        long freq  = rs.getLong("freq");
                        System.out.println(key + " " + freq);
                    }
                }
            }

        } catch (Exception e) {
            // Only print the error message and exit
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
