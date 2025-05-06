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
            SSHSearchDatabaseArguments params = new SSHSearchDatabaseArguments(args);
            String originalType = params.getTreeType();
            final String tableName = originalType.replace('-', '_');
            String dbPath = params.getDatabase();
            int topFreq = params.getTopFrequency();

            // Generic case (time-based vs non-time)
            String sql;
            if (originalType.endsWith("-time")) {
                sql = String.format(
                    "SELECT substr(key, 1, instr(key, '-') + 5) AS key, SUM(frequency) AS freq " +
                    "FROM %s " +
                    "GROUP BY substr(key, 1, instr(key, '-') + 5) " +
                    "ORDER BY freq DESC, key ASC " +
                    "LIMIT ?;",
                    tableName
                );
            } else {
                sql = String.format(
                    "SELECT key, SUM(frequency) AS freq " +
                    "FROM %s " +
                    "GROUP BY key " +
                    "ORDER BY freq DESC, key ASC " +
                    "LIMIT ?;",
                    tableName
                );
            }

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, topFreq);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.println(rs.getString("key") + " " + rs.getLong("freq"));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}