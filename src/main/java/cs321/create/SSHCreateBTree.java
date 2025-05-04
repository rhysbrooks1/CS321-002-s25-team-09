package cs321.create;

import cs321.btree.BTree;
import cs321.btree.TreeObject;
import java.io.*;
import java.sql.*;
import java.util.Scanner;

/**
 * Create a BTree from a given wrangled SSH log file and output all unique
 * values found within the SSH log file as
 *   1) a Random-Access-File file containing the BTree,
 *   2) a dump file (if debug option is on), and
 *   3) a table into a SQL database named SSHLogDB.db
 *
 * Usage:
 *   java -jar SSHCreateBTree.jar --cache=<0|1> --degree=<btree-degree> \
 *       --sshFile=<ssh-file> --type=<tree-type> [--cache-size=<n>] \
 *       --database=<yes|no> [--debug=<0|1>]
 */
public class SSHCreateBTree {
    static final String DATABASE_URL = "jdbc:sqlite:SSHLogDB.db";

    public SSHCreateBTree(SSHCreateBTreeArguments params) throws Exception {
        // Construct BTree filename
        String btreeFileName = String.format(
            "SSH_log.txt.ssh.btree.%s.%d",
            params.getTreeType(), params.getDegree()
        );

        // Instantiate BTree
        BTree btree = new BTree(
            params.getDegree(),
            btreeFileName,
            params.getUseCache(),
            params.getCacheSize()
        );

        System.out.println("Processing SSH file: "
            + params.getSSHFilename() + " of type " + params.getTreeType());

        // Insert keys into BTree
        processSSHFile(params, btree);

        // Dump to file if debug
        if (params.getDebugLevel() == 1) {
            dumpTreeToFile(params, btree);
        }

        // Dump to database if requested
        if (params.getCreateDatabase()) {
            dumpTreeToDatabase(params, btree);
        }

        // Finalize and close
        btree.finishUp();
        System.out.println("BTree creation complete for type: "
            + params.getTreeType());
    }

    private static void processSSHFile(
        SSHCreateBTreeArguments params,
        BTree btree
    ) throws IOException {
        Scanner scanner = new Scanner(new File(params.getSSHFilename()));
        String[] parts = params.getTreeType().split("-");
        String identifier = parts[0];
        String target = parts[1];

        while (scanner.hasNextLine()) {
            String[] tokens = scanner.nextLine().split(" ");
            boolean hasUser = tokens.length == 5;

            // Skip user-ip invalid lines
            if (identifier.equals("user") &&
                (!hasUser || tokens[2].equals("reverse") || tokens[2].equals("Address"))) {
                continue;
            }
            // Skip other types
            if (!identifier.equals("user") &&
                !tokens[2].equalsIgnoreCase(identifier)) {
                continue;
            }

            // Build key
            String key = (identifier.equals("user") ? tokens[3] : tokens[2]) + "-";
            if (target.equals("ip")) {
                key += hasUser && !tokens[2].equals("Address")
                    ? tokens[4] : tokens[3];
            } else {
                key += tokens[1].substring(0, 5);
            }

            btree.insert(new TreeObject(key));
        }
        scanner.close();
    }

    private static void dumpTreeToFile(
        SSHCreateBTreeArguments params,
        BTree btree
    ) throws IOException {
        File out = new File(
            String.format("dump-%s.%d.txt",
                params.getTreeType(), params.getDegree())
        );
        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            for (TreeObject obj : btree.getSortedTreeObjects()) {
                w.write(obj.getKey() + " " + obj.getCount());
                w.newLine();
            }
        }
        System.out.println("Dump file: " + out.getAbsolutePath());
    }

    private static void dumpTreeToDatabase(
        SSHCreateBTreeArguments params,
        BTree btree
    ) throws SQLException {
        String table = params.getTreeType().replaceAll("[^a-zA-Z0-9_]", "_");
        String createSQL = String.format(
            "CREATE TABLE IF NOT EXISTS %s(key TEXT PRIMARY KEY, frequency INTEGER);",
            table
        );
        String insertSQL = String.format(
            "INSERT OR REPLACE INTO %s(key, frequency) VALUES (?,?);",
            table
        );

        try (Connection conn = DriverManager.getConnection(DATABASE_URL)) {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createSQL);
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                for (TreeObject obj : btree.getSortedTreeObjects()) {
                    ps.setString(1, obj.getKey());
                    ps.setLong(2, obj.getCount());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        System.out.println("Database updated: table '" + table + "'");
    }

    public static void main(String[] args) {
        try {
            new SSHCreateBTree(new SSHCreateBTreeArguments(args));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
