package cs321.search;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assert.*;

public class SSHSearchDatabaseTest {

    @Test
    public void testQueryDatabasePrintsTopResults() throws Exception {
        String dbPath = "test-query.db"; // temporary test database file
        File dbFile = new File(dbPath);

        try {
            // Set up the test database
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                Statement stmt = conn.createStatement();

                // Create and populate the acceptedip table
                stmt.execute("DROP TABLE IF EXISTS acceptedip");
                stmt.execute("CREATE TABLE acceptedip (key TEXT, frequency INTEGER)");
                stmt.execute("INSERT INTO acceptedip VALUES ('Accepted-1.1.1.1', 5)");
                stmt.execute("INSERT INTO acceptedip VALUES ('Accepted-2.2.2.2', 10)");
                stmt.execute("INSERT INTO acceptedip VALUES ('Accepted-3.3.3.3', 7)");
            }

            // Redirect stdout to capture query output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            // Call the query method
            SSHSearchDatabase.queryDatabase("accepted-ip", dbPath, 2);

            // Restore original stdout
            System.setOut(originalOut);

            // Parse output
            String[] lines = outContent.toString().trim().split("\n");

            // Assert expected output
            assertEquals("Expected 2 results", 2, lines.length);
            assertTrue("Top entry should be Accepted-2.2.2.2", lines[0].startsWith("Accepted-2.2.2.2"));
            assertTrue("Second entry should be Accepted-3.3.3.3", lines[1].startsWith("Accepted-3.3.3.3"));

        } finally {
            // Clean up test file
            if (dbFile.exists()) {
                boolean deleted = dbFile.delete();
                if (!deleted) {
                    System.err.println(" Warning: Failed to delete test DB file: " + dbPath);
                }
            }
        }
    }

    @Test
    public void testQueryDatabaseWithEmptyTable() throws Exception {
        String dbPath = "test-empty.db";
        File dbFile = new File(dbPath);

        try {
            // Create an empty acceptedip table
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                Statement stmt = conn.createStatement();
                stmt.execute("DROP TABLE IF EXISTS acceptedip");
                stmt.execute("CREATE TABLE acceptedip (key TEXT, frequency INTEGER)");
            }

            // Capture output
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            SSHSearchDatabase.queryDatabase("accepted-ip", dbPath, 10);

            System.setOut(System.out);
            String output = outContent.toString().trim();

            assertEquals("Expected no output for empty table", "", output);

        } finally {
            dbFile.delete();
        }
    }

    @Test
    public void testQueryDatabaseWithNonexistentTable() throws Exception {
        String dbPath = "test-missing-table.db";
        File dbFile = new File(dbPath);

        try {
            // Create empty database without creating any table
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {}

            // Capture stderr
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            System.setErr(new PrintStream(errContent));

            // This should cause a "no such table" error
            SSHSearchDatabase.queryDatabase("accepted-ip", dbPath, 10);

            System.setErr(System.err);
            String errorOutput = errContent.toString();

            assertTrue("Expected error for missing table", errorOutput.toLowerCase().contains("no such table"));

        } finally {
            dbFile.delete();
        }
    }

    @Test
    public void testQueryDatabaseWithInvalidDbPath() {
        // Non-writable or nonexistent path
        String dbPath = "/invalid/path/SSHLogDB.db";

        // Capture stderr
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        try {
            SSHSearchDatabase.queryDatabase("accepted-ip", dbPath, 10);
            fail("Expected System.exit or SQLException for invalid DB path");
        } catch (Exception ignored) {
            // Expected error
        } finally {
            System.setErr(System.err);
        }

        String output = errContent.toString().toLowerCase();
        assertTrue("Expected error for invalid DB path", output.contains("database error"));
    }
}
