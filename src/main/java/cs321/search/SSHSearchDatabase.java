package cs321.search;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

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

    private static final String IPV4_REGEX =
        "(25[0-5]|2[0-4]\\d|1?\\d?\\d)\\." +
        "(25[0-5]|2[0-4]\\d|1?\\d?\\d)\\." +
        "(25[0-5]|2[0-4]\\d|1?\\d?\\d)\\." +
        "(25[0-5]|2[0-4]\\d|1?\\d?\\d)";

    public static void main(String[] args) {
        try {
            SSHSearchDatabaseArguments params = new SSHSearchDatabaseArguments(args);
            String originalType = params.getTreeType();
            final String tableName = originalType.replace('-', '_');
            String dbPath = params.getDatabase();
            int topFreq = params.getTopFrequency();

            // Special handling for reverseaddress-ip
            if ("reverseaddress-ip".equals(originalType)) {
                Map<String, Long> counts = new HashMap<>();
                String sqlRaw = String.format(
                    "SELECT key, SUM(frequency) AS freq FROM %s GROUP BY key;",
                    tableName
                );

                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                     PreparedStatement ps = conn.prepareStatement(sqlRaw);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        String rawKey = rs.getString("key");
                        long freq = rs.getLong("freq");

                        // extract the IP part (after last dash), or the whole key if no dash
                        int dashIdx = rawKey.lastIndexOf('-');
                        String ip = (dashIdx >= 0) ? rawKey.substring(dashIdx + 1) : rawKey;

                        // filter pure IPv4 only
                        if (!ip.matches(IPV4_REGEX)) {
                            continue;
                        }

                        counts.merge(rawKey, freq, Long::sum);
                    }
                }

                counts.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int cmp = Long.compare(e2.getValue(), e1.getValue());
                        return (cmp != 0) ? cmp : e1.getKey().compareTo(e2.getKey());
                    })
                    .limit(topFreq)
                    .forEach(e -> {
                        String rawKey = e.getKey();
                        int dashIdx = rawKey.lastIndexOf('-');
                        String ip = (dashIdx >= 0) ? rawKey.substring(dashIdx + 1) : rawKey;

                        // if original key was "Address-...", keep it; otherwise prefix with "reverse-"
                        String outKey = rawKey.startsWith("Address-")
                            ? rawKey
                            : "reverse-" + ip;

                        System.out.println(outKey + " " + e.getValue());
                    });
                return;
            }

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
