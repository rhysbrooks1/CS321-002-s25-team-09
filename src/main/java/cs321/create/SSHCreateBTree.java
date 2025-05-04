package cs321.create;

import cs321.btree.BTree;
import cs321.btree.TreeObject;
import java.io.*;
import java.sql.*;
import cs321.create.SSHFileReader;

/**
 * Driver for creating a BTree from a wrangled SSH log file.
 * Produces:
 *   1) A disk-backed BTree file,
 *   2) An optional dump file,
 *   3) An optional SQLite table.
 */
public class SSHCreateBTree {
    private static final String DATABASE_URL = "jdbc:sqlite:SSHLogDB.db";

    public SSHCreateBTree(SSHCreateBTreeArguments params) throws Exception {
        String btreeFile = String.format(
            "SSH_log.txt.ssh.btree.%s.%d",
            params.getTreeType(), params.getDegree()
        );

        BTree btree = new BTree(
            params.getDegree(),
            btreeFile,
            params.getUseCache(),
            params.getCacheSize()
        );

        System.out.println("Processing SSH file: " +
            params.getSSHFilename() + " of type " + params.getTreeType());

        // Use SSHFileReader to iterate keys
        SSHFileReader reader = new SSHFileReader(
            params.getSSHFilename(), params.getTreeType()
        );
        while (reader.hasNextKey()) {
            String key = reader.nextKey();
            btree.insert(new TreeObject(key));
        }
        reader.close();

        // Optional dump
        if (params.getDebugLevel() == 1) {
            dumpTreeToFile(params, btree);
        }

        // Optional database
        if (params.getCreateDatabase()) {
            dumpTreeToDatabase(params, btree);
        }

        // Finalize
        btree.finishUp();
        System.out.println("BTree creation complete for type: " + params.getTreeType());
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
