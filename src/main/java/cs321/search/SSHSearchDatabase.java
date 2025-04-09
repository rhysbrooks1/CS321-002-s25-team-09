package cs321.search;

import cs321.btree.BTree;
import cs321.common.ParseArgumentException;
import cs321.common.ParseArgumentUtils;



/**
 * The driver class for searching a Database of a B-Tree.
 * 
 * This class is used to search a B-Tree database for a specific key.
 * It takes the following command line arguments:
 * 
 *  java -jar build/libs/SSHSearchDatabase.jar \
 * --type=<tree-type> \
 * --database=<path-to-SQLite-db> \
 * --top-frequency=<10|25|50>
 * 
 * Where <tree-type> is one of the following:
 * accepted-ip
 * accepted-time
 * failed-ip
 * failed-time
 * invalid-ip
 * invalid-time
 * reverseaddress-ip
 * reverseaddress-time
 * user-ip
 * 
 * <path-to-SQLite-db> is the path to the SQLite database file.
 * --database=SSHLogDB.db
 * 
 * --top-frequency=<10|25|50> is the number of top results to return.
 * --top-frequency=10
 * --top-frequency=25
 * --top-frequency=50
 * 
 * ALternative Mode: --type=test
 * Testing without a real BTree/database.
 * 
 * Standard Output:
 * <Key> <Frequency>
 * 
 * Example: 
 * Accepted-111.222.107.90 25
 * Accepted-119.137.63.195 14
 * Accepted-137.189.88.215 12
 */
import java.util.HashMap;
import java.util.Map;

public class SSHSearchDatabase {
    public static void main(String[] args) {
        ParsedArguments parsed = parseArgs(args);

        System.out.println("Input validation passed");
        System.out.println("Type: " + parsed.type);
        System.out.println("Database: " + parsed.database);
        System.out.println("Top Frequency: " + parsed.topFrequency);

        // TODO: Add SQLite query logic here
        if (parsed.type.equals("test")) {
            runTestMode(parsed.database);
        } else {
            // TODO: implement real DB query for BTree table
            System.out.println("Real query mode not implemented yet.");
        }
        
    }

    private static ParsedArguments parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] split = arg.substring(2).split("=", 2);
                map.put(split[0], split[1]);
            }
        }

        String type = map.get("type");
        String db = map.get("database");
        String topStr = map.get("top-frequency");

        if (type == null || db == null || topStr == null) {
            exit("Usage: --type=<type> --database=<path> --top-frequency=<10|25|50>");
        }

        String[] validTypes = {
            "accepted-ip", "accepted-time", "failed-ip", "failed-time",
            "invalid-ip", "invalid-time", "reverseaddress-ip", "reverseaddress-time",
            "user-ip", "test"
        };

        boolean typeValid = java.util.Arrays.asList(validTypes).contains(type);
        if (!typeValid) exit("Invalid --type: " + type);

        int top;
        try {
            top = Integer.parseInt(topStr);
            if (top != 10 && top != 25 && top != 50) throw new Exception();
        } catch (Exception e) {
            exit("Invalid --top-frequency: must be 10, 25, or 50.");
            return null; // never reached
        }

        return new ParsedArguments(type, db, top);
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    private static class ParsedArguments {
        String type, database;
        int topFrequency;
        ParsedArguments(String type, String database, int topFrequency) {
            this.type = type;
            this.database = database;
            this.topFrequency = topFrequency;
        }
    }
    private static void runTestMode(String dbPath) {
        String tableName = "acceptedip"; // always for test mode
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            java.sql.Statement stmt = conn.createStatement();
    
            // Drop table if it already exists (clean test)
            stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
    
            // Create table
            stmt.executeUpdate(
                "CREATE TABLE " + tableName + " (" +
                "key TEXT NOT NULL, " +
                "frequency INTEGER NOT NULL)"
            );
    
            // Insert 25 test rows
            java.sql.PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO " + tableName + " (key, frequency) VALUES (?, ?)"
            );
    
            String[] keys = {
                "Accepted-111.222.107.90", "Accepted-112.96.173.55", "Accepted-112.96.33.40",
                "Accepted-113.116.236.34", "Accepted-113.118.187.34", "Accepted-113.99.127.215",
                "Accepted-119.137.60.156", "Accepted-119.137.62.123", "Accepted-119.137.62.142",
                "Accepted-119.137.63.195", "Accepted-123.255.103.142", "Accepted-123.255.103.215",
                "Accepted-137.189.204.138", "Accepted-137.189.204.155", "Accepted-137.189.204.220",
                "Accepted-137.189.204.236", "Accepted-137.189.204.246", "Accepted-137.189.204.253",
                "Accepted-137.189.205.44", "Accepted-137.189.206.152", "Accepted-137.189.206.243",
                "Accepted-137.189.207.18", "Accepted-137.189.207.28", "Accepted-137.189.240.159",
                "Accepted-137.189.241.19"
            };
            int[] frequencies = {
                25, 3, 3, 6, 2, 2, 1, 9, 1, 14,
                5, 5, 1, 1, 1, 1, 1, 3, 2, 1,
                1, 1, 1, 1, 2
            };
    
            for (int i = 0; i < 25; i++) {
                insert.setString(1, keys[i]);
                insert.setInt(2, frequencies[i]);
                insert.executeUpdate();
            }
    
            // Query all rows, sorted by frequency DESC, then key ASC
            java.sql.PreparedStatement query = conn.prepareStatement(
                "SELECT key, frequency FROM " + tableName +
                " ORDER BY frequency DESC, key ASC"
            );
            java.sql.ResultSet rs = query.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString("key") + " " + rs.getInt("frequency"));
            }
    
        } catch (Exception e) {
            System.err.println("Test mode error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
